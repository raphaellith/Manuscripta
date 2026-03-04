using Main.Models.Dtos;

namespace Main.Services.GenAI;

public interface IMaterialGenerationService
{
    /// <summary>
    /// Generates reading content using AI.
    /// Per GenAISpec.md §3B(1)(a).
    /// </summary>
    /// <param name="request">The generation request parameters.</param>
    /// <param name="onChunk">Optional callback invoked for each streaming chunk. Per GenAISpec §3H(5)(a).</param>
    Task<GenerationResult> GenerateReading(GenerationRequest request, Func<StreamingGenerationChunk, Task>? onChunk = null);

    /// <summary>
    /// Generates worksheet content using AI.
    /// Per GenAISpec.md §3B(1)(b).
    /// </summary>
    /// <param name="request">The generation request parameters.</param>
    /// <param name="onChunk">Optional callback invoked for each streaming chunk. Per GenAISpec §3H(5)(a).</param>
    Task<GenerationResult> GenerateWorksheet(GenerationRequest request, Func<StreamingGenerationChunk, Task>? onChunk = null);

    /// <summary>
    /// Checks whether the primary generation model is ready and has sufficient
    /// resources to handle a typical generation request.  This is used by the
    /// frontend availability check to avoid enabling AI UI while memory is
    /// temporarily constrained (e.g. immediately after installing ChromaDB).
    /// </summary>
    Task<bool> CanGenerateWithPrimaryModelAsync();
}
