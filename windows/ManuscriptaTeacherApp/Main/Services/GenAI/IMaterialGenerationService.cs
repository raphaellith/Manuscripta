using Main.Models.Dtos;

namespace Main.Services.GenAI;

public interface IMaterialGenerationService
{
    Task<GenerationResult> GenerateReading(GenerationRequest request);
    Task<GenerationResult> GenerateWorksheet(GenerationRequest request);
}
