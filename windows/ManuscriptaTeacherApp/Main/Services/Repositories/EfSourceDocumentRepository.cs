using System;
using System.Collections.Generic;
using System.Threading.Tasks;
using Microsoft.EntityFrameworkCore;
using Main.Data;
using Main.Models.Entities;

namespace Main.Services.Repositories;

/// <summary>
/// Repository for managing SourceDocumentEntity.
/// Per PersistenceAndCascadingRules.md §1(1)(f).
/// </summary>
public class EfSourceDocumentRepository : ISourceDocumentRepository
{
    private readonly MainDbContext _ctx;

    public EfSourceDocumentRepository(MainDbContext ctx)
    {
        _ctx = ctx;
    }

    public async Task<SourceDocumentEntity?> GetByIdAsync(Guid id)
    {
        return await _ctx.SourceDocuments
            .FirstOrDefaultAsync(sd => sd.Id == id);
    }

    public async Task<IEnumerable<SourceDocumentEntity>> GetAllAsync()
    {
        return await _ctx.SourceDocuments.ToListAsync();
    }

    public async Task AddAsync(SourceDocumentEntity entity)
    {
        await _ctx.SourceDocuments.AddAsync(entity);
        await _ctx.SaveChangesAsync();
    }

    public async Task UpdateAsync(SourceDocumentEntity entity)
    {
        var existing = await _ctx.SourceDocuments.FindAsync(entity.Id);
        if (existing == null)
        {
            throw new InvalidOperationException($"SourceDocument with ID {entity.Id} not found.");
        }

        // Copy only scalar properties to avoid EF tracking conflicts
        // when the incoming entity carries a populated UnitCollection
        // navigation property whose key is already tracked.
        existing.UnitCollectionId = entity.UnitCollectionId;
        existing.Transcript = entity.Transcript;
        existing.EmbeddingStatus = entity.EmbeddingStatus;

        await _ctx.SaveChangesAsync();
    }

    public async Task DeleteAsync(Guid id)
    {
        var entity = await _ctx.SourceDocuments.FindAsync(id);
        if (entity != null)
        {
            _ctx.SourceDocuments.Remove(entity);
            await _ctx.SaveChangesAsync();
        }
    }
}
