using Main.Models.Dtos;

namespace Main.Services.GenAI;

public interface IMaterialGenerationService
{
    /// <summary>
    /// Generates reading content using AI.
    /// Per GenAISpec.md §3B(1)(a) and §3H(8).
    /// </summary>
    /// <param name="request">The generation request parameters.</param>
    /// <param name="onChunk">Optional callback invoked for each streaming chunk. Per GenAISpec §3H(5)(a).</param>
    /// <param name="cancellationToken">Optional cancellation token. Per GenAISpec §3H(8).</param>
    Task<GenerationResult> GenerateReading(GenerationRequest request, Func<StreamingGenerationChunk, Task>? onChunk = null, CancellationToken cancellationToken = default);

    /// <summary>
    /// Generates worksheet content using AI.
    /// Per GenAISpec.md §3B(1)(b) and §3H(8).
    /// </summary>
    /// <param name="request">The generation request parameters.</param>
    /// <param name="onChunk">Optional callback invoked for each streaming chunk. Per GenAISpec §3H(5)(a).</param>
    /// <param name="cancellationToken">Optional cancellation token. Per GenAISpec §3H(8).</param>
    Task<GenerationResult> GenerateWorksheet(GenerationRequest request, Func<StreamingGenerationChunk, Task>? onChunk = null, CancellationToken cancellationToken = default);

    /// <summary>
    /// Checks whether the primary generation model is ready and has sufficient
    /// resources to handle a typical generation request.  This is used by the
    /// frontend availability check to avoid enabling AI UI while memory is
    /// temporarily constrained (e.g. immediately after installing ChromaDB).
    /// </summary>
    Task<bool> CanGenerateWithPrimaryModelAsync();
}
