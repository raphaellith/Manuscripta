using Main.Data;
using Main.Models.Enums;
using Microsoft.EntityFrameworkCore;

namespace Main.Services.GenAI;

/// <summary>
/// Provides embedding status query functionality.
/// See GenAISpec.md §3E.
/// </summary>
public class EmbeddingStatusService : IEmbeddingStatusService
{
    private readonly MainDbContext _dbContext;

    public EmbeddingStatusService(MainDbContext dbContext)
    {
        _dbContext = dbContext;
    }

    /// <summary>
    /// Retrieves the current embedding status of a source document.
    /// See GenAISpec.md §3E(1)(a) and §3E(2).
    /// </summary>
    public async Task<EmbeddingStatus> GetEmbeddingStatus(Guid sourceDocumentId)
    {
        var document = await _dbContext.SourceDocuments.FindAsync(sourceDocumentId);
        
        if (document == null)
        {
            throw new InvalidOperationException($"Source document {sourceDocumentId} not found");
        }

        // Return the current status, or treat null as not submitted for indexing
        var status = document.EmbeddingStatus ?? EmbeddingStatus.PENDING;
        return status;
    }
}
