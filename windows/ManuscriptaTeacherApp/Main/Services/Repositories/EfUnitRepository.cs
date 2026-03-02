using System;
using System.Collections.Generic;
using System.Linq;
using System.Threading.Tasks;
using Microsoft.EntityFrameworkCore;
using Main.Data;
using Main.Models.Entities;

namespace Main.Services.Repositories;

/// <summary>
/// Repository for managing UnitEntity.
/// Per PersistenceAndCascadingRules.md §1(1)(d).
/// </summary>
public class EfUnitRepository : IUnitRepository
{
    private readonly MainDbContext _ctx;

    public EfUnitRepository(MainDbContext ctx)
    {
        _ctx = ctx;
    }

    public async Task<UnitEntity?> GetByIdAsync(Guid id)
    {
        return await _ctx.Units
            .FirstOrDefaultAsync(u => u.Id == id);
    }

    public async Task<IEnumerable<UnitEntity>> GetByUnitCollectionIdAsync(Guid unitCollectionId)
    {
        return await _ctx.Units
            .Where(u => u.UnitCollectionId == unitCollectionId)
            .ToListAsync();
    }

    public async Task<IEnumerable<UnitEntity>> GetAllAsync()
    {
        return await _ctx.Units.ToListAsync();
    }

    public async Task AddAsync(UnitEntity entity)
    {
        await _ctx.Units.AddAsync(entity);
        await _ctx.SaveChangesAsync();
    }

    public async Task UpdateAsync(UnitEntity entity)
    {
        var tracked = _ctx.Units.Local.FirstOrDefault(u => u.Id == entity.Id);
        if (tracked != null)
        {
            _ctx.Entry(tracked).State = EntityState.Detached;
        }
        
        _ctx.Units.Update(entity);
        await _ctx.SaveChangesAsync();
    }

    public async Task DeleteAsync(Guid id)
    {
        var entity = await _ctx.Units.FindAsync(id);
        if (entity != null)
        {
            _ctx.Units.Remove(entity);
            await _ctx.SaveChangesAsync();
        }
    }
}
