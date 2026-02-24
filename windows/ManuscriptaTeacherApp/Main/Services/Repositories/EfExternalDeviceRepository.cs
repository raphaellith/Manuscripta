using Microsoft.EntityFrameworkCore;
using Main.Data;
using Main.Models.Entities;

namespace Main.Services.Repositories;

/// <summary>
/// EF Core implementation of IExternalDeviceRepository.
/// </summary>
public class EfExternalDeviceRepository : IExternalDeviceRepository
{
    private readonly MainDbContext _context;

    public EfExternalDeviceRepository(MainDbContext context)
    {
        _context = context ?? throw new ArgumentNullException(nameof(context));
    }

    public async Task<IEnumerable<ExternalDeviceEntity>> GetAllAsync()
    {
        return await _context.ExternalDevices.ToListAsync();
    }

    public async Task<ExternalDeviceEntity?> GetByIdAsync(Guid id)
    {
        return await _context.ExternalDevices.FindAsync(id);
    }

    public async Task AddAsync(ExternalDeviceEntity entity)
    {
        _context.ExternalDevices.Add(entity);
        await _context.SaveChangesAsync();
    }

    public async Task UpdateAsync(ExternalDeviceEntity entity)
    {
        var existing = await _context.ExternalDevices.FindAsync(entity.Id);
        if (existing == null)
            throw new InvalidOperationException($"External device {entity.Id} not found");

        existing.Name = entity.Name;
        existing.ConfigurationData = entity.ConfigurationData;
        // note: type is readonly
        await _context.SaveChangesAsync();
    }

    public async Task DeleteAsync(Guid id)
    {
        var entity = await _context.ExternalDevices.FindAsync(id);
        if (entity != null)
        {
            _context.ExternalDevices.Remove(entity);
            await _context.SaveChangesAsync();
        }
    }
}
