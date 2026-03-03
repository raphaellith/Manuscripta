using System.Net.Http.Json;
using System.Text.Json;
using Main.Services.RuntimeDependencies;

namespace Main.Services.GenAI;

/// <summary>
/// Provides low-level Ollama API interactions for model verification, chat completion, and embedding generation.
/// See GenAISpec.md §3(2). Implements IInferenceClient per §3(2A).
/// </summary>
public class OllamaClientService : IInferenceClient
{
    private readonly HttpClient _httpClient;
    private readonly IInferenceRuntimeSelector _runtimeSelector;

    public OllamaClientService(IInferenceRuntimeSelector runtimeSelector)
    {
        _runtimeSelector = runtimeSelector;
        _httpClient = new HttpClient { 
            Timeout = TimeSpan.FromSeconds(300) // 5 minutes for material generation
        };
    }

    private async Task<string> GetBaseUrlAsync()
    {
        return await _runtimeSelector.GetActiveRuntimeBaseUrlAsync();
    }

    /// <summary>
    /// Verifies that Ollama's daemon is running.
    /// See GenAISpec.md §1(4)(b).
    /// </summary>
    public virtual async Task<bool> IsOllamaRunningAsync()
    {
        try
        {
            var response = await _httpClient.GetAsync($"{await GetBaseUrlAsync()}/");
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
            var response = await _httpClient.GetAsync($"{await GetBaseUrlAsync()}/api/tags");
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
        var response = await _httpClient.PostAsJsonAsync($"{await GetBaseUrlAsync()}/api/pull", request);
        response.EnsureSuccessStatusCode();
        // Consume the streaming response to wait for the pull operation to complete.
        _ = await response.Content.ReadAsStringAsync();
    }

    /// <summary>
    /// Generates embeddings for the given text.
    /// See GenAISpec.md §2(2).
    /// Per GenAISpec.md §1F(7)(a): always routes to Standard Ollama (port 11434).
    /// </summary>
    public virtual async Task<float[]> GenerateEmbeddingAsync(string text, string model = "nomic-embed-text")
    {
        var request = new
        {
            model = model,
            prompt = text
        };

        var response = await _httpClient.PostAsJsonAsync($"{_runtimeSelector.GetStandardOllamaBaseUrl()}/api/embeddings", request);
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

        var response = await _httpClient.PostAsJsonAsync($"{await GetBaseUrlAsync()}/api/chat", request);
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

            var response = await _httpClient.PostAsJsonAsync($"{await GetBaseUrlAsync()}/api/generate", request);
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
            var response = await _httpClient.PostAsJsonAsync($"{await GetBaseUrlAsync()}/api/chat", new
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
