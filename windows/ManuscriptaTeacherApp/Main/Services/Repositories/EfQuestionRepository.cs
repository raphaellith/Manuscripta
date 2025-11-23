using System.Collections.Generic;
using System.Linq;
using System.Threading.Tasks;
using Microsoft.EntityFrameworkCore;
using Main.Data;
using Main.Models.Entities;

namespace Main.Services.Repositories;

public class EfQuestionRepository : IQuestionRepository
{
    private readonly MainDbContext _ctx;

    public EfQuestionRepository(MainDbContext ctx)
    {
        _ctx = ctx;
    }

    public async Task<QuestionEntity?> GetByIdAsync(int id)
    {
        return await _ctx.Questions
            .FirstOrDefaultAsync(q => q.Id == id);
    }

    public async Task<IEnumerable<QuestionEntity>> GetByMaterialIdAsync(int materialId)
    {
        return await _ctx.Questions
            .Where(q => q.MaterialId == materialId)
            .ToListAsync();
    }
}
