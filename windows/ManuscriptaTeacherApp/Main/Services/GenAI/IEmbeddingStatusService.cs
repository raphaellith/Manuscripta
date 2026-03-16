using Main.Models.Enums;

namespace Main.Services.GenAI;

public interface IEmbeddingStatusService
{
    Task<EmbeddingStatus> GetEmbeddingStatus(Guid sourceDocumentId);
}
