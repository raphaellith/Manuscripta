using System.Net.Http.Json;
using System.Runtime.CompilerServices;
using System.Text.Json;
using Main.Models.Dtos;
using Main.Services.RuntimeDependencies;

namespace Main.Services.GenAI;

/// <summary>
/// Provides low-level Ollama API interactions for model verification, chat completion, and embedding generation.
/// See GenAISpec.md §3(2).
/// </summary>
public class OllamaClientService : IDependencyService
{
    private readonly HttpClient _httpClient;
    private readonly string _baseUrl;

    public OllamaClientService()
    {
        _baseUrl = "http://localhost:11434";
        _httpClient = new HttpClient { 
            BaseAddress = new Uri(_baseUrl),
            Timeout = TimeSpan.FromSeconds(300) // 5 minutes for material generation
        };
    }

    /// <summary>
    /// Verifies that Ollama's daemon is running.
    /// See GenAISpec.md §1(4)(b).
    /// </summary>
    public virtual async Task<bool> IsOllamaRunningAsync()
    {
        try
        {
            var response = await _httpClient.GetAsync("/");
            return response.IsSuccessStatusCode;
        }
        catch
        {
            return false;
        }
    }

    /// <summary>
    /// Verifies that a model is locally available.
    /// See GenAISpec.md §1(4)(a).
    /// </summary>
    public virtual async Task<bool> IsModelAvailableAsync(string modelName)
    {
        try
        {
            var response = await _httpClient.GetAsync("/api/tags");
            if (!response.IsSuccessStatusCode) return false;

            var content = await response.Content.ReadAsStringAsync();
            var doc = JsonDocument.Parse(content);
            
            if (doc.RootElement.TryGetProperty("models", out var models))
            {
                foreach (var model in models.EnumerateArray()
                    .Where(m => m.TryGetProperty("name", out var name) && 
                        name.GetString()?.StartsWith(modelName) == true))
                {
                    return true;
                }
            }
            
            return false;
        }
        catch
        {
            return false;
        }
    }

    /// <summary>
    /// Pulls a model from Ollama if it's not available locally.
    /// See GenAISpec.md §1(4)(a).
    /// </summary>
    public virtual async Task PullModelAsync(string modelName)
    {
        var request = new { name = modelName };
        var response = await _httpClient.PostAsJsonAsync("/api/pull", request);
        response.EnsureSuccessStatusCode();
        // Consume the streaming response to wait for the pull operation to complete.
        _ = await response.Content.ReadAsStringAsync();
    }

    /// <summary>
    /// Generates embeddings for the given text.
    /// See GenAISpec.md §2(2).
    /// </summary>
    public virtual async Task<float[]> GenerateEmbeddingAsync(string text, string model = "nomic-embed-text")
    {
        var request = new
        {
            model = model,
            prompt = text
        };

        var response = await _httpClient.PostAsJsonAsync("/api/embeddings", request);
        response.EnsureSuccessStatusCode();

        var content = await response.Content.ReadAsStringAsync();
        var doc = JsonDocument.Parse(content);
        
        if (doc.RootElement.TryGetProperty("embedding", out var embedding))
        {
            var embeddings = new List<float>();
            foreach (var value in embedding.EnumerateArray())
            {
                embeddings.Add(value.GetSingle());
            }
            
            // §2(2): Validate embedding dimensions match specification (768)
            var embeddingArray = embeddings.ToArray();
            if (embeddingArray.Length != 768)
            {
                throw new InvalidOperationException($"Invalid embedding dimension: expected 768, got {embeddingArray.Length}");
            }
            
            return embeddingArray;
        }

        throw new InvalidOperationException("Failed to extract embedding from response");
    }

    /// <summary>
    /// Generates a chat completion using the specified model.
    /// See GenAISpec.md §3(2).
    /// </summary>
    public virtual async Task<string> GenerateChatCompletionAsync(string model, string prompt, string? systemPrompt = null)
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
            stream = false
        };

        var response = await _httpClient.PostAsJsonAsync("/api/chat", request);
        if (!response.IsSuccessStatusCode)
        {
            var errorBody = await response.Content.ReadAsStringAsync();
            var errorSnippet = string.IsNullOrWhiteSpace(errorBody)
                ? "No error payload returned by Ollama."
                : errorBody.Length <= 512
                    ? errorBody
                    : errorBody[..512] + "...";

            throw new HttpRequestException(
                $"Ollama chat request failed with {(int)response.StatusCode} ({response.StatusCode}). {errorSnippet}");
        }

        var content = await response.Content.ReadAsStringAsync();
        var doc = JsonDocument.Parse(content);
        
        if (doc.RootElement.TryGetProperty("message", out var message) &&
            message.TryGetProperty("content", out var messageContent))
        {
            return messageContent.GetString() ?? string.Empty;
        }

        throw new InvalidOperationException("Failed to extract message from response");
    }

    /// <summary>
    /// Generates a streaming chat completion using the specified model.
    /// Yields tokens as they arrive from Ollama.
    /// Per GenAISpec.md §3H(2)(a).
    /// </summary>
    public virtual async IAsyncEnumerable<StreamingGenerationChunk> GenerateChatCompletionStreamingAsync(
        string model, string prompt, string? systemPrompt = null,
        [EnumeratorCancellation] CancellationToken cancellationToken = default)
    {
        var messages = new List<object>();

        if (!string.IsNullOrEmpty(systemPrompt))
        {
            messages.Add(new { role = "system", content = systemPrompt });
        }

        messages.Add(new { role = "user", content = prompt });

        // Per §3H(4): Enable thinking mode for models that support chain-of-thought
        // (e.g., qwen3). This causes the model to emit <think>...</think> tags.
        var request = new
        {
            model = model,
            messages = messages,
            stream = true,
            options = new { enable_thinking = true }
        };

        var httpRequest = new HttpRequestMessage(HttpMethod.Post, "/api/chat")
        {
            Content = JsonContent.Create(request)
        };

        using var response = await _httpClient.SendAsync(httpRequest, HttpCompletionOption.ResponseHeadersRead, cancellationToken);

        if (!response.IsSuccessStatusCode)
        {
            var errorBody = await response.Content.ReadAsStringAsync(cancellationToken);
            var errorSnippet = string.IsNullOrWhiteSpace(errorBody)
                ? "No error payload returned by Ollama."
                : errorBody.Length <= 512
                    ? errorBody
                    : errorBody[..512] + "...";

            throw new HttpRequestException(
                $"Ollama chat request failed with {(int)response.StatusCode} ({response.StatusCode}). {errorSnippet}");
        }

        await using var stream = await response.Content.ReadAsStreamAsync(cancellationToken);
        using var reader = new StreamReader(stream);

        // Per §3H(4): Track think tag state
        bool isInThinkBlock = false;
        string partialTag = "";

        while (!reader.EndOfStream)
        {
            var line = await reader.ReadLineAsync(cancellationToken);
            if (string.IsNullOrWhiteSpace(line))
                continue;

            JsonDocument? doc = null;
            try
            {
                doc = JsonDocument.Parse(line);
            }
            catch (JsonException)
            {
                // Skip malformed JSON lines
                continue;
            }

            using (doc)
            {
                var root = doc.RootElement;

                // Extract done flag
                bool done = root.TryGetProperty("done", out var doneProp) && doneProp.GetBoolean();

                // Extract content token
                string token = "";
                if (root.TryGetProperty("message", out var messageElement) &&
                    messageElement.TryGetProperty("content", out var contentElement))
                {
                    token = contentElement.GetString() ?? "";
                }

                if (string.IsNullOrEmpty(token) && !done)
                    continue;

                // Per §3H(4): Process think tags - yields segments with correct IsThinking per segment
                var (segments, newIsThinking, newPartialTag) = ProcessThinkTags(
                    partialTag + token, isInThinkBlock);

                isInThinkBlock = newIsThinking;
                partialTag = newPartialTag;

                // Yield each segment with its correct IsThinking flag
                for (int segIdx = 0; segIdx < segments.Count; segIdx++)
                {
                    var seg = segments[segIdx];
                    bool isLastSegment = segIdx == segments.Count - 1;
                    // Only the final segment of the final JSON line should have done=true
                    bool segmentDone = done && isLastSegment;
                    yield return new StreamingGenerationChunk(seg.Content, seg.IsThinking, segmentDone);
                }

                // If no segments but done flag is set, yield empty done chunk
                if (segments.Count == 0 && done)
                {
                    yield return new StreamingGenerationChunk("", isInThinkBlock, true);
                }
            }
        }
    }

    /// <summary>
    /// Processes think tags in a token, returning segments with correct IsThinking flags.
    /// Per GenAISpec.md §3H(4)(c).
    /// </summary>
    /// <returns>Tuple of (list of content segments with IsThinking flags, final think state, remaining partial tag)</returns>
    private static (List<(string Content, bool IsThinking)> Segments, bool IsThinking, string PartialTag) ProcessThinkTags(
        string input, bool wasThinking)
    {
        var segments = new List<(string Content, bool IsThinking)>();
        var currentSegment = new System.Text.StringBuilder();
        bool isThinking = wasThinking;
        string partialTag = "";
        int i = 0;

        while (i < input.Length)
        {
            // Check for potential tag start
            if (input[i] == '<')
            {
                // Check for <think> tag
                if (i + 7 <= input.Length && input.Substring(i, 7) == "<think>")
                {
                    // Flush current segment before state change
                    if (currentSegment.Length > 0)
                    {
                        segments.Add((currentSegment.ToString(), isThinking));
                        currentSegment.Clear();
                    }
                    isThinking = true;
                    i += 7;
                    continue;
                }

                // Check for </think> tag
                if (i + 8 <= input.Length && input.Substring(i, 8) == "</think>")
                {
                    // Flush current segment before state change
                    if (currentSegment.Length > 0)
                    {
                        segments.Add((currentSegment.ToString(), isThinking));
                        currentSegment.Clear();
                    }
                    isThinking = false;
                    i += 8;
                    continue;
                }

                // Check for partial tag at end of input
                string remaining = input.Substring(i);
                if (IsPartialThinkTag(remaining))
                {
                    partialTag = remaining;
                    break;
                }
            }

            currentSegment.Append(input[i]);
            i++;
        }

        // Flush any remaining content
        if (currentSegment.Length > 0)
        {
            segments.Add((currentSegment.ToString(), isThinking));
        }

        return (segments, isThinking, partialTag);
    }

    /// <summary>
    /// Checks if a string could be the start of a think tag.
    /// </summary>
    private static bool IsPartialThinkTag(string s)
    {
        const string openTag = "<think>";
        const string closeTag = "</think>";

        // Check if it's a prefix of either tag
        if (s.Length < openTag.Length && openTag.StartsWith(s))
            return true;
        if (s.Length < closeTag.Length && closeTag.StartsWith(s))
            return true;

        return false;
    }

    /// <summary>
    /// Starts Ollama daemon if it's not running.
    /// See GenAISpec.md §1(4)(b).
    /// </summary>
    public virtual async Task StartOllamaDaemonAsync()
    {
        var processStartInfo = new System.Diagnostics.ProcessStartInfo
        {
            FileName = "ollama",
            Arguments = "serve",
            UseShellExecute = false,
            CreateNoWindow = true
        };

        using var process = System.Diagnostics.Process.Start(processStartInfo);
        if (process is null)
        {
            throw new InvalidOperationException("Failed to start Ollama daemon process");
        }
        
        // Wait for daemon to start
        for (int i = 0; i < 10; i++)
        {
            await Task.Delay(1000);
            if (await IsOllamaRunningAsync())
                return;
        }

        throw new InvalidOperationException("Failed to start Ollama daemon");
    }

    /// <summary>
    /// Ensures Ollama is running and the specified model is available.
    /// See GenAISpec.md §1(4).
    /// </summary>
    public virtual async Task EnsureModelReadyAsync(string modelName)
    {
        if (!await IsOllamaRunningAsync())
        {
            throw new InvalidOperationException(
                "Ollama is not running. Please ensure Ollama is installed and started.");
        }

        if (!await IsModelAvailableAsync(modelName))
        {
            throw new InvalidOperationException(
                $"Model '{modelName}' is not available. Please install it using the Settings menu before generating content.");
        }
    }

    /// <summary>
    /// Unloads a model from Ollama's memory to free up system resources.
    /// Uses a minimal generation request with keep_alive set to 1 second to force unloading.
    /// </summary>
    public virtual async Task UnloadModelAsync(string modelName)
    {
        try
        {
            // Send a minimal request with keep_alive: 1s to force unloading
            var request = new
            {
                model = modelName,
                prompt = "",
                stream = false,
                keep_alive = "1s"
            };

            var response = await _httpClient.PostAsJsonAsync("/api/generate", request);
            // Don't check for success - unload is a best-effort operation
            _ = await response.Content.ReadAsStringAsync();
        }
        catch
        {
            // Unload failures are not critical - continue anyway
        }
    }

    /// <summary>
    /// Checks if the system has sufficient resources to generate with a given model.
    /// See GenAISpec.md §1(6) - detection of insufficient resources.
    /// </summary>
    public virtual async Task<bool> CanGenerateWithModelAsync(string modelName)
    {
        try
        {
            // Attempt a test generation with a more realistic prompt to detect resource constraints
            // Use a longer prompt that better simulates actual generation workloads
            var testPrompt = @"Generate educational content based on the following: This is a test prompt to verify sufficient system resources are available. Please provide a brief response to confirm the model can process requests of this size and complexity. The response should demonstrate the model's ability to handle typical content generation tasks.";
            var response = await _httpClient.PostAsJsonAsync("/api/chat", new
            {
                model = modelName,
                messages = new[] { new { role = "user", content = testPrompt } },
                stream = false
            });

            // If we get a 507 (Insufficient Storage) or 503 (Service Unavailable), resources insufficient
            if (response.StatusCode == System.Net.HttpStatusCode.InsufficientStorage ||
                response.StatusCode == System.Net.HttpStatusCode.ServiceUnavailable)
            {
                return false;
            }

            // Check for 500 errors with memory-related messages
            if (!response.IsSuccessStatusCode)
            {
                var errorBody = await response.Content.ReadAsStringAsync();
                if (errorBody.Contains("system memory", StringComparison.OrdinalIgnoreCase) ||
                    errorBody.Contains("insufficient", StringComparison.OrdinalIgnoreCase))
                {
                    return false;
                }
            }

            return response.IsSuccessStatusCode;
        }
        catch
        {
            // If we can't reach the API, resources are insufficient
            return false;
        }
    }
}
