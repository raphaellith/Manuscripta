using System;
using System.Net;
using System.Net.Http;
using System.Threading;
using System.Threading.Tasks;
using Main.Services.GenAI;
using Moq;
using Moq.Protected;
using Xunit;

namespace MainTests.ServicesTests.GenAI;

/// <summary>
/// Tests for OllamaClientService per-method routing.
/// Per GenAISpec.md §1F(7): embedding operations always route to Standard Ollama (port 11434),
/// while generation operations route to the active runtime's URL.
/// </summary>
public class OllamaClientServiceTests
{
    /// <summary>
    /// Verifies that GenerateEmbeddingAsync always calls the Standard Ollama URL (port 11434),
    /// even when the active runtime is OpenVINO (port 11435).
    /// Per GenAISpec.md §1F(7)(a).
    /// </summary>
    [Fact]
    public async Task GenerateEmbeddingAsync_AlwaysUsesStandardOllamaUrl()
    {
        // Arrange
        var mockSelector = new Mock<IInferenceRuntimeSelector>();
        // GetActiveRuntimeBaseUrlAsync returns OV-Ollama URL (port 11435)
        mockSelector.Setup(s => s.GetActiveRuntimeBaseUrlAsync())
            .ReturnsAsync("http://localhost:11435");
        // GetStandardOllamaBaseUrl always returns Standard Ollama URL (port 11434)
        mockSelector.Setup(s => s.GetStandardOllamaBaseUrl())
            .Returns("http://localhost:11434");

        Uri? capturedUri = null;

        var mockHandler = new Mock<HttpMessageHandler>();
        mockHandler.Protected()
            .Setup<Task<HttpResponseMessage>>(
                "SendAsync",
                ItExpr.IsAny<HttpRequestMessage>(),
                ItExpr.IsAny<CancellationToken>())
            .Callback<HttpRequestMessage, CancellationToken>((request, _) =>
            {
                capturedUri = request.RequestUri;
            })
            .ReturnsAsync(new HttpResponseMessage
            {
                StatusCode = HttpStatusCode.OK,
                Content = new StringContent("{\"embedding\": [" + string.Join(",", new float[768].Select(_ => "0.1")) + "]}")
            });

        var service = new TestableOllamaClientService(mockSelector.Object, mockHandler.Object);

        // Act
        await service.GenerateEmbeddingAsync("test text");

        // Assert — the request should have gone to port 11434 (Standard Ollama), NOT 11435
        Assert.NotNull(capturedUri);
        Assert.Equal(11434, capturedUri!.Port);
        Assert.Equal("http://localhost:11434/api/embeddings", capturedUri.ToString());
    }

    /// <summary>
    /// Verifies that GenerateChatCompletionAsync uses the active runtime URL (dynamic),
    /// which should be port 11435 when OpenVINO is active.
    /// Per GenAISpec.md §1F(7)(b).
    /// </summary>
    [Fact]
    public async Task GenerateChatCompletionAsync_UsesActiveRuntimeUrl()
    {
        // Arrange
        var mockSelector = new Mock<IInferenceRuntimeSelector>();
        mockSelector.Setup(s => s.GetActiveRuntimeBaseUrlAsync())
            .ReturnsAsync("http://localhost:11435");
        mockSelector.Setup(s => s.GetStandardOllamaBaseUrl())
            .Returns("http://localhost:11434");

        Uri? capturedUri = null;

        var mockHandler = new Mock<HttpMessageHandler>();
        mockHandler.Protected()
            .Setup<Task<HttpResponseMessage>>(
                "SendAsync",
                ItExpr.IsAny<HttpRequestMessage>(),
                ItExpr.IsAny<CancellationToken>())
            .Callback<HttpRequestMessage, CancellationToken>((request, _) =>
            {
                capturedUri = request.RequestUri;
            })
            .ReturnsAsync(new HttpResponseMessage
            {
                StatusCode = HttpStatusCode.OK,
                Content = new StringContent("{\"message\": {\"content\": \"response\"}}")
            });

        var service = new TestableOllamaClientService(mockSelector.Object, mockHandler.Object);

        // Act
        await service.GenerateChatCompletionAsync("qwen3:8b", "test prompt");

        // Assert — the request should have gone to port 11435 (OV-Ollama)
        Assert.NotNull(capturedUri);
        Assert.Equal(11435, capturedUri!.Port);
        Assert.Contains("/api/chat", capturedUri.ToString());
    }

    /// <summary>
    /// Subclass that injects a custom HttpMessageHandler for request interception.
    /// </summary>
    private sealed class TestableOllamaClientService : OllamaClientService
    {
        public TestableOllamaClientService(IInferenceRuntimeSelector runtimeSelector, HttpMessageHandler handler)
            : base(runtimeSelector)
        {
            // Replace the default HttpClient with one using the mock handler
            var field = typeof(OllamaClientService).GetField("_httpClient",
                System.Reflection.BindingFlags.NonPublic | System.Reflection.BindingFlags.Instance);
            field!.SetValue(this, new HttpClient(handler) { Timeout = TimeSpan.FromSeconds(300) });
        }
    }
}
