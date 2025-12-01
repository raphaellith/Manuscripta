using System.Collections.Generic;
using System.Linq;
using System.Threading.Tasks;
using Microsoft.EntityFrameworkCore;
using Main.Data;
using Main.Models.Entities;
using Main.Models.Entities.Materials;
using Main.Models.Mappings;

namespace Main.Services.Repositories;

/// <summary>
/// Repository for managing materials. Uses MaterialDataEntity for persistence
/// and MaterialEntity for business logic.
/// </summary>
public class EfMaterialRepository : IMaterialRepository
{
    private readonly MainDbContext _ctx;

    public EfMaterialRepository(MainDbContext ctx)
    {
        _ctx = ctx;
    }

    public async Task<MaterialEntity?> GetByIdAsync(Guid id)
    {
        var dataEntity = await _ctx.Materials
            .FirstOrDefaultAsync(m => m.Id == id);
        
        return dataEntity == null ? null : MaterialEntityMapper.ToEntity(dataEntity);
    }

    public async Task<IEnumerable<MaterialEntity>> GetAllAsync()
    {
        var dataEntities = await _ctx.Materials.ToListAsync();
        return dataEntities.Select(de => MaterialEntityMapper.ToEntity(de));
    }

    public async Task AddAsync(MaterialEntity entity)
    {
        var dataEntity = MaterialEntityMapper.ToDataEntity(entity);
        await _ctx.Materials.AddAsync(dataEntity);
        await _ctx.SaveChangesAsync();
    }

    public async Task UpdateAsync(MaterialEntity entity)
    {
        // Find and detach any existing tracked entity
        var tracked = _ctx.Materials.Local.FirstOrDefault(m => m.Id == entity.Id);
        if (tracked != null)
        {
            _ctx.Entry(tracked).State = EntityState.Detached;
        }
        
        var dataEntity = MaterialEntityMapper.ToDataEntity(entity);
        _ctx.Materials.Update(dataEntity);
        await _ctx.SaveChangesAsync();
    }

    public async Task DeleteAsync(Guid id)
    {
        var entity = await _ctx.Materials.FindAsync(id);
        if (entity != null)
        {
            _ctx.Materials.Remove(entity);
            await _ctx.SaveChangesAsync();
        }
    }
}
