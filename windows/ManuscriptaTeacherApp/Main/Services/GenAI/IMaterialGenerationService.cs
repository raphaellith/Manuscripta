using Main.Models.Dtos;

namespace Main.Services.GenAI;

public interface IMaterialGenerationService
{
    Task<GenerationResult> GenerateReading(GenerationRequest request);
    Task<GenerationResult> GenerateWorksheet(GenerationRequest request);

    /// <summary>
    /// Checks whether the primary generation model is ready and has sufficient
    /// resources to handle a typical generation request.  This is used by the
    /// frontend availability check to avoid enabling AI UI while memory is
    /// temporarily constrained (e.g. immediately after installing ChromaDB).
    /// </summary>
    Task<bool> CanGenerateWithPrimaryModelAsync();
}
