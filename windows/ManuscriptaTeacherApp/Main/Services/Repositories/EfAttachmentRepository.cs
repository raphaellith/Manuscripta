using Microsoft.EntityFrameworkCore;
using Main.Data;
using Main.Models.Entities;

namespace Main.Services.Repositories;

/// <summary>
/// Repository for managing AttachmentEntity.
/// Per PersistenceAndCascadingRules.md §1(1)(g).
/// </summary>
public class EfAttachmentRepository : IAttachmentRepository
{
    private readonly MainDbContext _ctx;

    public EfAttachmentRepository(MainDbContext ctx)
    {
        _ctx = ctx;
    }

    public async Task<AttachmentEntity?> GetByIdAsync(Guid id)
    {
        return await _ctx.Attachments.FirstOrDefaultAsync(a => a.Id == id);
    }

    public async Task<IEnumerable<AttachmentEntity>> GetAllAsync()
    {
        return await _ctx.Attachments.ToListAsync();
    }

    public async Task<IEnumerable<AttachmentEntity>> GetByMaterialIdAsync(Guid materialId)
    {
        return await _ctx.Attachments
            .Where(a => a.MaterialId == materialId)
            .ToListAsync();
    }

    public async Task AddAsync(AttachmentEntity entity)
    {
        await _ctx.Attachments.AddAsync(entity);
        await _ctx.SaveChangesAsync();
    }

    public async Task DeleteAsync(Guid id)
    {
        var entity = await _ctx.Attachments.FindAsync(id);
        if (entity != null)
        {
            _ctx.Attachments.Remove(entity);
            await _ctx.SaveChangesAsync();
        }
    }
}
