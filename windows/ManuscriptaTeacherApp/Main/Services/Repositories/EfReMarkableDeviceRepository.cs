using Microsoft.EntityFrameworkCore;
using Main.Data;
using Main.Models.Entities;

namespace Main.Services.Repositories;

/// <summary>
/// Repository for managing ReMarkableDeviceEntity.
/// Per PersistenceAndCascadingRules.md §1(1)(h).
/// </summary>
public class EfReMarkableDeviceRepository : IReMarkableDeviceRepository
{
    private readonly MainDbContext _ctx;

    public EfReMarkableDeviceRepository(MainDbContext ctx)
    {
        _ctx = ctx;
    }

    public async Task<ReMarkableDeviceEntity?> GetByIdAsync(Guid id)
    {
        return await _ctx.ReMarkableDevices.FirstOrDefaultAsync(d => d.DeviceId == id);
    }

    public async Task<IEnumerable<ReMarkableDeviceEntity>> GetAllAsync()
    {
        return await _ctx.ReMarkableDevices.ToListAsync();
    }

    public async Task AddAsync(ReMarkableDeviceEntity entity)
    {
        await _ctx.ReMarkableDevices.AddAsync(entity);
        await _ctx.SaveChangesAsync();
    }

    public async Task UpdateAsync(ReMarkableDeviceEntity entity)
    {
        var existing = await _ctx.ReMarkableDevices.FindAsync(entity.DeviceId);
        if (existing != null)
        {
            existing.Name = entity.Name;
            await _ctx.SaveChangesAsync();
        }
    }

    public async Task DeleteAsync(Guid id)
    {
        var entity = await _ctx.ReMarkableDevices.FindAsync(id);
        if (entity != null)
        {
            _ctx.ReMarkableDevices.Remove(entity);
            await _ctx.SaveChangesAsync();
        }
    }
}
