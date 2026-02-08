using System;
using System.Collections.Generic;
using System.Threading.Tasks;
using Main.Models.Entities;

namespace Main.Services.GenAI;

public interface IEmbeddingService
{
    Task IndexSourceDocumentAsync(SourceDocumentEntity document);
    Task RemoveSourceDocumentAsync(Guid sourceDocumentId);
    Task<List<string>> RetrieveRelevantChunksAsync(
        float[] queryEmbedding,
        Guid unitCollectionId,
        List<Guid>? sourceDocumentIds = null,
        int topK = 5);
    Task RetryEmbeddingAsync(Guid sourceDocumentId);
    Task ReIndexSourceDocumentAsync(SourceDocumentEntity document);
    Task InitializeFailedEmbeddingsAsync();
}
