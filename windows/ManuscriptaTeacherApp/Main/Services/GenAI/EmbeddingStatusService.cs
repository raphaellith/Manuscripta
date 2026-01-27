using Main.Models.Entities;
using Main.Models.Enums;

namespace Main.Services.GenAI;

/// <summary>
/// Provides embedding status query functionality.
/// See GenAISpec.md §3E.
/// </summary>
public class EmbeddingStatusService
{
    /// <summary>
    /// Retrieves the current embedding status of a source document.
    /// See GenAISpec.md §3E(1)(a) and §3E(2).
    /// </summary>
    public Task<EmbeddingStatus> GetEmbeddingStatus(SourceDocumentEntity document)
    {
        // Return the current status, or treat null as not submitted for indexing
        var status = document.EmbeddingStatus ?? EmbeddingStatus.PENDING;
        return Task.FromResult(status);
    }
}
