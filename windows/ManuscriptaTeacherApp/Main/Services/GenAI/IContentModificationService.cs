using Main.Models.Dtos;

namespace Main.Services.GenAI;

public interface IContentModificationService
{
    Task<GenerationResult> ModifyContent(string selectedContent, string instruction, Guid? unitCollectionId);
}
