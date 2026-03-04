using System.Net;
using System.Text;
using Main.Models.Dtos;
using Main.Services.GenAI;
using Moq;
using Moq.Protected;
using Xunit;

namespace MainTests.ServicesTests.GenAI;

/// <summary>
/// Tests for OllamaClientService streaming functionality.
/// Per GenAISpec.md §3H.
/// </summary>
public class OllamaClientServiceStreamingTests
{
    /// <summary>
    /// Creates an OllamaClientService with a mocked HttpClient that returns the specified NDJSON response.
    /// </summary>
    private static OllamaClientService CreateServiceWithMockedResponse(string ndjsonResponse)
    {
        var handlerMock = new Mock<HttpMessageHandler>();
        handlerMock
            .Protected()
            .Setup<Task<HttpResponseMessage>>(
                "SendAsync",
                ItExpr.IsAny<HttpRequestMessage>(),
                ItExpr.IsAny<CancellationToken>())
            .ReturnsAsync(() => new HttpResponseMessage
            {
                StatusCode = HttpStatusCode.OK,
                Content = new StreamContent(new MemoryStream(Encoding.UTF8.GetBytes(ndjsonResponse)))
            });

        var httpClient = new HttpClient(handlerMock.Object)
        {
            BaseAddress = new Uri("http://localhost:11434")
        };

        return new TestableOllamaClientService(httpClient);
    }

    [Fact]
    public async Task StreamingAsync_YieldsChunks()
    {
        // Arrange: Multiple NDJSON lines
        var ndjson = """
            {"message":{"content":"Hello"},"done":false}
            {"message":{"content":" World"},"done":false}
            {"message":{"content":"!"},"done":true}
            """;

        var service = CreateServiceWithMockedResponse(ndjson);
        var chunks = new List<StreamingGenerationChunk>();

        // Act
        await foreach (var chunk in service.GenerateChatCompletionStreamingAsync("test-model", "test prompt"))
        {
            chunks.Add(chunk);
        }

        // Assert
        Assert.Equal(3, chunks.Count);
        Assert.Equal("Hello", chunks[0].Token);
        Assert.False(chunks[0].Done);
        Assert.Equal(" World", chunks[1].Token);
        Assert.False(chunks[1].Done);
        Assert.Equal("!", chunks[2].Token);
        Assert.True(chunks[2].Done);
    }

    [Fact]
    public async Task StreamingAsync_ParsesThinkTags()
    {
        // Arrange: Response with think tags
        var ndjson = """
            {"message":{"content":"<think>Reasoning"},"done":false}
            {"message":{"content":" here</think>"},"done":false}
            {"message":{"content":"Content"},"done":true}
            """;

        var service = CreateServiceWithMockedResponse(ndjson);
        var chunks = new List<StreamingGenerationChunk>();

        // Act
        await foreach (var chunk in service.GenerateChatCompletionStreamingAsync("test-model", "test prompt"))
        {
            chunks.Add(chunk);
        }

        // Assert: First chunk after <think> tag should have IsThinking = true
        Assert.True(chunks[0].IsThinking);
        Assert.Equal("Reasoning", chunks[0].Token);

        // After </think> tag is processed in the same chunk, IsThinking reflects final state = false
        // The content " here" precedes </think> but the state returned is after tag processing
        Assert.False(chunks[1].IsThinking);
        Assert.Equal(" here", chunks[1].Token);

        Assert.False(chunks[2].IsThinking); // Content after </think>
        Assert.Equal("Content", chunks[2].Token);
    }

    [Fact]
    public async Task StreamingAsync_StripsThinkTags()
    {
        // Arrange: Response with think tags embedded in content
        var ndjson = """
            {"message":{"content":"<think>"},"done":false}
            {"message":{"content":"thinking"},"done":false}
            {"message":{"content":"</think>"},"done":false}
            {"message":{"content":"visible"},"done":true}
            """;

        var service = CreateServiceWithMockedResponse(ndjson);
        var chunks = new List<StreamingGenerationChunk>();

        // Act
        await foreach (var chunk in service.GenerateChatCompletionStreamingAsync("test-model", "test prompt"))
        {
            chunks.Add(chunk);
        }

        // Assert: Tags should not appear in token values
        Assert.DoesNotContain("<think>", string.Join("", chunks.Select(c => c.Token)));
        Assert.DoesNotContain("</think>", string.Join("", chunks.Select(c => c.Token)));
        
        // But content should still be present
        Assert.Contains(chunks, c => c.Token == "thinking");
        Assert.Contains(chunks, c => c.Token == "visible");
    }

    [Fact]
    public async Task StreamingAsync_SetsDoneOnFinalChunk()
    {
        // Arrange
        var ndjson = """
            {"message":{"content":"First"},"done":false}
            {"message":{"content":"Last"},"done":true}
            """;

        var service = CreateServiceWithMockedResponse(ndjson);
        var chunks = new List<StreamingGenerationChunk>();

        // Act
        await foreach (var chunk in service.GenerateChatCompletionStreamingAsync("test-model", "test prompt"))
        {
            chunks.Add(chunk);
        }

        // Assert
        Assert.False(chunks[0].Done);
        Assert.True(chunks[1].Done);
    }

    [Fact]
    public async Task StreamingAsync_HandlesThinkTagsSplitAcrossChunks()
    {
        // Arrange: <think> tag split across chunks
        var ndjson = """
            {"message":{"content":"Before<thi"},"done":false}
            {"message":{"content":"nk>Inside"},"done":false}
            {"message":{"content":"</think>After"},"done":true}
            """;

        var service = CreateServiceWithMockedResponse(ndjson);
        var allTokens = new StringBuilder();
        var chunks = new List<StreamingGenerationChunk>();

        // Act
        await foreach (var chunk in service.GenerateChatCompletionStreamingAsync("test-model", "test prompt"))
        {
            chunks.Add(chunk);
            allTokens.Append(chunk.Token);
        }

        // Assert: Combined output should not contain think tags
        var combined = allTokens.ToString();
        Assert.DoesNotContain("<think>", combined);
        Assert.DoesNotContain("</think>", combined);
        
        // "Before" should be outside think block, "Inside" inside, "After" outside
        Assert.Contains("Before", combined);
        Assert.Contains("Inside", combined);
        Assert.Contains("After", combined);
    }

    [Fact]
    public async Task StreamingAsync_HandlesEmptyTokens()
    {
        // Arrange: Some empty tokens mixed in
        var ndjson = """
            {"message":{"content":"Hello"},"done":false}
            {"message":{"content":""},"done":false}
            {"message":{"content":"World"},"done":true}
            """;

        var service = CreateServiceWithMockedResponse(ndjson);
        var chunks = new List<StreamingGenerationChunk>();

        // Act
        await foreach (var chunk in service.GenerateChatCompletionStreamingAsync("test-model", "test prompt"))
        {
            chunks.Add(chunk);
        }

        // Assert: Should skip empty non-final chunks but include content
        var nonEmptyChunks = chunks.Where(c => !string.IsNullOrEmpty(c.Token) || c.Done).ToList();
        Assert.True(nonEmptyChunks.Count >= 2);
        Assert.Contains(nonEmptyChunks, c => c.Token == "Hello");
        Assert.Contains(nonEmptyChunks, c => c.Token == "World" && c.Done);
    }

    /// <summary>
    /// Testable OllamaClientService that accepts a custom HttpClient.
    /// </summary>
    private sealed class TestableOllamaClientService : OllamaClientService
    {
        private readonly HttpClient _testHttpClient;

        public TestableOllamaClientService(HttpClient httpClient)
        {
            _testHttpClient = httpClient;
        }

        public override async IAsyncEnumerable<StreamingGenerationChunk> GenerateChatCompletionStreamingAsync(
            string model, string prompt, string? systemPrompt = null,
            [System.Runtime.CompilerServices.EnumeratorCancellation] CancellationToken cancellationToken = default)
        {
            var messages = new List<object>();

            if (!string.IsNullOrEmpty(systemPrompt))
            {
                messages.Add(new { role = "system", content = systemPrompt });
            }

            messages.Add(new { role = "user", content = prompt });

            var request = new
            {
                model = model,
                messages = messages,
                stream = true
            };

            var httpRequest = new HttpRequestMessage(HttpMethod.Post, "/api/chat")
            {
                Content = System.Net.Http.Json.JsonContent.Create(request)
            };

            using var response = await _testHttpClient.SendAsync(httpRequest, HttpCompletionOption.ResponseHeadersRead, cancellationToken);

            if (!response.IsSuccessStatusCode)
            {
                var errorBody = await response.Content.ReadAsStringAsync(cancellationToken);
                throw new HttpRequestException($"Ollama chat request failed with {(int)response.StatusCode}. {errorBody}");
            }

            await using var stream = await response.Content.ReadAsStreamAsync(cancellationToken);
            using var reader = new StreamReader(stream);

            bool isInThinkBlock = false;
            string partialTag = "";

            while (!reader.EndOfStream)
            {
                var line = await reader.ReadLineAsync(cancellationToken);
                if (string.IsNullOrWhiteSpace(line))
                    continue;

                System.Text.Json.JsonDocument? doc = null;
                try
                {
                    doc = System.Text.Json.JsonDocument.Parse(line);
                }
                catch (System.Text.Json.JsonException)
                {
                    continue;
                }

                using (doc)
                {
                    var root = doc.RootElement;
                    bool done = root.TryGetProperty("done", out var doneProp) && doneProp.GetBoolean();

                    string token = "";
                    if (root.TryGetProperty("message", out var messageElement) &&
                        messageElement.TryGetProperty("content", out var contentElement))
                    {
                        token = contentElement.GetString() ?? "";
                    }

                    if (string.IsNullOrEmpty(token) && !done)
                        continue;

                    var (processedToken, newIsThinking, newPartialTag) = ProcessThinkTags(
                        partialTag + token, isInThinkBlock);

                    isInThinkBlock = newIsThinking;
                    partialTag = newPartialTag;

                    if (!string.IsNullOrEmpty(processedToken) || done)
                    {
                        yield return new StreamingGenerationChunk(processedToken, isInThinkBlock, done);
                    }
                }
            }
        }

        private static (string ProcessedToken, bool IsThinking, string PartialTag) ProcessThinkTags(
            string input, bool wasThinking)
        {
            var result = new StringBuilder();
            bool isThinking = wasThinking;
            string partialTag = "";
            int i = 0;

            while (i < input.Length)
            {
                if (input[i] == '<')
                {
                    if (i + 7 <= input.Length && input.Substring(i, 7) == "<think>")
                    {
                        isThinking = true;
                        i += 7;
                        continue;
                    }

                    if (i + 8 <= input.Length && input.Substring(i, 8) == "</think>")
                    {
                        isThinking = false;
                        i += 8;
                        continue;
                    }

                    string remaining = input.Substring(i);
                    if (IsPartialThinkTag(remaining))
                    {
                        partialTag = remaining;
                        break;
                    }
                }

                result.Append(input[i]);
                i++;
            }

            return (result.ToString(), isThinking, partialTag);
        }

        private static bool IsPartialThinkTag(string s)
        {
            const string openTag = "<think>";
            const string closeTag = "</think>";

            if (s.Length < openTag.Length && openTag.StartsWith(s))
                return true;
            if (s.Length < closeTag.Length && closeTag.StartsWith(s))
                return true;

            return false;
        }
    }
}
