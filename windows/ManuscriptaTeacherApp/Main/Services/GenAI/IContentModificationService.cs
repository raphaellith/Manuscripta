using Main.Models.Dtos;

namespace Main.Services.GenAI;

public interface IContentModificationService
{
    Task<GenerationResult> ModifyContent(string selectedContent, string instruction, Guid? unitCollectionId, string materialType, string title, int? readingAge, int? actualAge, Func<StreamingGenerationChunk, Task>? onChunk = null, CancellationToken cancellationToken = default);
}
