using System.Net.Http.Json;
using System.Text.Json;

namespace Main.Services.GenAI;

/// <summary>
/// Provides low-level Ollama API interactions for model verification, chat completion, and embedding generation.
/// See GenAISpec.md §3(2).
/// </summary>
public class OllamaClientService
{
    private readonly HttpClient _httpClient;
    private readonly string _baseUrl;

    public OllamaClientService()
    {
        _baseUrl = "http://localhost:11434";
        _httpClient = new HttpClient { BaseAddress = new Uri(_baseUrl) };
    }

    /// <summary>
    /// Verifies that Ollama's daemon is running.
    /// See GenAISpec.md §1(4)(b).
    /// </summary>
    public async Task<bool> IsOllamaRunningAsync()
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
    public async Task<bool> IsModelAvailableAsync(string modelName)
    {
        try
        {
            var response = await _httpClient.GetAsync("/api/tags");
            if (!response.IsSuccessStatusCode) return false;

            var content = await response.Content.ReadAsStringAsync();
            var doc = JsonDocument.Parse(content);
            
            if (doc.RootElement.TryGetProperty("models", out var models))
            {
                foreach (var model in models.EnumerateArray())
                {
                    if (model.TryGetProperty("name", out var name) && 
                        name.GetString()?.StartsWith(modelName) == true)
                    {
                        return true;
                    }
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
    public async Task PullModelAsync(string modelName)
    {
        var request = new { name = modelName };
        await _httpClient.PostAsJsonAsync("/api/pull", request);
    }

    /// <summary>
    /// Generates embeddings for the given text.
    /// See GenAISpec.md §2(2).
    /// </summary>
    public async Task<float[]> GenerateEmbeddingAsync(string text, string model = "nomic-embed-text")
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
            return embeddings.ToArray();
        }

        throw new InvalidOperationException("Failed to extract embedding from response");
    }

    /// <summary>
    /// Generates a chat completion using the specified model.
    /// See GenAISpec.md §3(2).
    /// </summary>
    public async Task<string> GenerateChatCompletionAsync(string model, string prompt, string? systemPrompt = null)
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
        response.EnsureSuccessStatusCode();

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
    public async Task StartOllamaDaemonAsync()
    {
        var processStartInfo = new System.Diagnostics.ProcessStartInfo
        {
            FileName = "ollama",
            Arguments = "serve",
            UseShellExecute = false,
            CreateNoWindow = true
        };

        System.Diagnostics.Process.Start(processStartInfo);
        
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
    public async Task EnsureModelReadyAsync(string modelName)
    {
        if (!await IsOllamaRunningAsync())
        {
            await StartOllamaDaemonAsync();
        }

        if (!await IsModelAvailableAsync(modelName))
        {
            await PullModelAsync(modelName);
        }
    }
}
