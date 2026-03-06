using System;
using System.Collections.Generic;
using System.Threading.Tasks;
using Main.Models.Entities;

namespace Main.Services.GenAI;

public interface IEmbeddingService
{
    Task IndexSourceDocumentAsync(SourceDocumentEntity document);
    /// <summary>
    /// Indexes a source document by ID, fetching it from the database.
    /// Intended for use from background tasks with their own DI scope.
    /// See GenAISpec.md §3A(1) and §3A(5)(a).
    /// </summary>
    Task IndexSourceDocumentByIdAsync(Guid sourceDocumentId);
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
