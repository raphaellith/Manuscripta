using System.Collections.Generic;
using System.Linq;
using System.Threading.Tasks;
using Microsoft.EntityFrameworkCore;
using Main.Data;
using Main.Models.Entities;

namespace Main.Services.Repositories;

public class EfResponseRepository : IResponseRepository
{
    private readonly MainDbContext _ctx;

    public EfResponseRepository(MainDbContext ctx)
    {
        _ctx = ctx;
    }

    public async Task<ResponseEntity?> GetByIdAsync(int id)
    {
        return await _ctx.Responses
            .FirstOrDefaultAsync(r => r.Id == id);
    }

    public async Task<IEnumerable<ResponseEntity>> GetByQuestionIdAsync(int questionId)
    {
        return await _ctx.Responses
            .Where(r => r.QuestionId == questionId)
            .ToListAsync();
    }
}
