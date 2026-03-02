using System;
using System.Collections.Generic;
using System.Linq;
using System.Threading.Tasks;
using Microsoft.EntityFrameworkCore;
using Main.Data;
using Main.Models.Entities;

namespace Main.Services.Repositories;

/// <summary>
/// Repository for managing UnitCollectionEntity.
/// Per PersistenceAndCascadingRules.md §1(1)(c).
/// </summary>
public class EfUnitCollectionRepository : IUnitCollectionRepository
{
    private readonly MainDbContext _ctx;

    public EfUnitCollectionRepository(MainDbContext ctx)
    {
        _ctx = ctx;
    }

    public async Task<UnitCollectionEntity?> GetByIdAsync(Guid id)
    {
        return await _ctx.UnitCollections
            .FirstOrDefaultAsync(uc => uc.Id == id);
    }

    public async Task<IEnumerable<UnitCollectionEntity>> GetAllAsync()
    {
        return await _ctx.UnitCollections.ToListAsync();
    }

    public async Task AddAsync(UnitCollectionEntity entity)
    {
        await _ctx.UnitCollections.AddAsync(entity);
        await _ctx.SaveChangesAsync();
    }

    public async Task UpdateAsync(UnitCollectionEntity entity)
    {
        var tracked = _ctx.UnitCollections.Local.FirstOrDefault(uc => uc.Id == entity.Id);
        if (tracked != null)
        {
            _ctx.Entry(tracked).State = EntityState.Detached;
        }
        
        _ctx.UnitCollections.Update(entity);
        await _ctx.SaveChangesAsync();
    }

    public async Task DeleteAsync(Guid id)
    {
        var entity = await _ctx.UnitCollections.FindAsync(id);
        if (entity != null)
        {
            _ctx.UnitCollections.Remove(entity);
            await _ctx.SaveChangesAsync();
        }
    }
}
