using Main.Models.Entities;

namespace Main.Services.GenAI;

/// <summary>
/// Service for selecting and managing the active inference runtime.
/// Per GenAISpec.md §1F.
/// </summary>
public interface IInferenceRuntimeSelector
{
    /// <summary>
    /// Retrieves the currently active inference runtime.
    /// Checks DB preference first, falls back to hardware detection if no preference exists.
    /// Per GenAISpec.md §1F(3).
    /// </summary>
    Task<InferenceRuntime> GetActiveRuntimeAsync();

    /// <summary>
    /// Sets the preferred inference runtime and persists it.
    /// Per GenAISpec.md §1F(3).
    /// </summary>
    Task SwitchRuntimeAsync(InferenceRuntime newRuntime);

    /// <summary>
    /// Detects the presence of an NPU (or stubs it to true for immediate implementation).
    /// Per GenAISpec.md §1F(3).
    /// </summary>
    Task<bool> DetectNpuHardwareAsync();

    /// <summary>
    /// Returns the base URL for the active inference runtime.
    /// Standard: http://localhost:11434, OpenVINO: http://localhost:11435.
    /// </summary>
    Task<string> GetActiveRuntimeBaseUrlAsync();

    /// <summary>
    /// Returns the base URL for Standard Ollama (always http://localhost:11434).
    /// Per GenAISpec.md §1F(7)(a): embedding operations always use this URL.
    /// </summary>
    string GetStandardOllamaBaseUrl();
}
