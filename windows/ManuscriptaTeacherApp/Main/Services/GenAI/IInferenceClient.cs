using Main.Services.RuntimeDependencies;

namespace Main.Services.GenAI;

/// <summary>
/// Common interface for low-level inference API interactions.
/// Per GenAISpec.md §3(2) and §3(2A).
/// Both Standard Ollama and OV-Ollama expose identical REST APIs, so a single
/// OllamaClientService implementation of this interface is sufficient for both runtimes.
/// </summary>
public interface IInferenceClient : IDependencyService
{
    /// <summary>
    /// Generates embeddings for the given text.
    /// Per GenAISpec.md §3(2A)(a).
    /// </summary>
    Task<float[]> GenerateEmbeddingAsync(string text, string model = "nomic-embed-text");

    /// <summary>
    /// Generates a chat completion using the specified model.
    /// Per GenAISpec.md §3(2A)(b).
    /// </summary>
    Task<string> GenerateChatCompletionAsync(string model, string prompt, string? systemPrompt = null);

    /// <summary>
    /// Verifies that a model is locally available.
    /// Per GenAISpec.md §3(2A)(c).
    /// </summary>
    Task<bool> IsModelAvailableAsync(string modelName, bool useStandardOllama = false);

    /// <summary>
    /// Ensures the inference server is running and the specified model is available.
    /// Per GenAISpec.md §3(2A)(d).
    /// </summary>
    Task EnsureModelReadyAsync(string modelName, bool useStandardOllama = false);

    /// <summary>
    /// Checks if the system has sufficient resources to generate with a given model.
    /// Per GenAISpec.md §3(2A)(e).
    /// </summary>
    Task<bool> CanGenerateWithModelAsync(string modelName);

    /// <summary>
    /// Unloads a model from the inference server's memory to free up system resources.
    /// </summary>
    Task UnloadModelAsync(string modelName);
}
