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

    public async Task<MaterialEntity?> GetByIdAsync(int id)
    {
        return await _ctx.Materials
            .FirstOrDefaultAsync(m => m.Id == id);
    }
}
