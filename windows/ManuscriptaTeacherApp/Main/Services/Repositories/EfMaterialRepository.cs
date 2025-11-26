using System.Collections.Generic;
using System.Threading.Tasks;
using Microsoft.EntityFrameworkCore;
using Main.Data;
using Main.Models.Entities;

namespace Main.Services.Repositories;

public class EfMaterialRepository : IMaterialRepository
{
    private readonly MainDbContext _ctx;

    public EfMaterialRepository(MainDbContext ctx)
    {
        _ctx = ctx;
    }

    public async Task<MaterialEntity?> GetByIdAsync(Guid id)
    {
        return await _ctx.Materials
            .FirstOrDefaultAsync(m => m.Id == id);
    }

    public async Task<IEnumerable<MaterialEntity>> GetAllAsync()
    {
        return await _ctx.Materials.ToListAsync();
    }

    public async Task AddAsync(MaterialEntity entity)
    {
        await _ctx.Materials.AddAsync(entity);
        await _ctx.SaveChangesAsync();
    }

    public async Task UpdateAsync(MaterialEntity entity)
    {
        _ctx.Materials.Update(entity);
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
