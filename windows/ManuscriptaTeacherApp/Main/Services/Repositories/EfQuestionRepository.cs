using System.Collections.Generic;
using System.Linq;
using System.Threading.Tasks;
using Microsoft.EntityFrameworkCore;
using Main.Data;
using Main.Models.Entities;
using Main.Models.Entities.Questions;
using Main.Models.Mappings;

namespace Main.Services.Repositories;

/// <summary>
/// Repository for managing questions. Uses QuestionDataEntity for persistence
/// and QuestionEntity for business logic.
/// </summary>
public class EfQuestionRepository : IQuestionRepository
{
    private readonly MainDbContext _ctx;

    public EfQuestionRepository(MainDbContext ctx)
    {
        _ctx = ctx;
    }

    public async Task<QuestionEntity?> GetByIdAsync(Guid id)
    {
        var dataEntity = await _ctx.Questions
            .FirstOrDefaultAsync(q => q.Id == id);
        
        return dataEntity == null ? null : QuestionEntityMapper.ToEntity(dataEntity);
    }

    public async Task<IEnumerable<QuestionEntity>> GetByMaterialIdAsync(Guid materialId)
    {
        var dataEntities = await _ctx.Questions
            .Where(q => q.MaterialId == materialId)
            .ToListAsync();
        
        return dataEntities.Select(QuestionEntityMapper.ToEntity);
    }

    public async Task AddAsync(QuestionEntity entity)
    {
        var dataEntity = QuestionEntityMapper.ToDataEntity(entity);
        await _ctx.Questions.AddAsync(dataEntity);
        await _ctx.SaveChangesAsync();
    }

    public async Task UpdateAsync(QuestionEntity entity)
    {
        var dataEntity = QuestionEntityMapper.ToDataEntity(entity);
        _ctx.Questions.Update(dataEntity);
        await _ctx.SaveChangesAsync();
    }

    public async Task DeleteAsync(Guid id)
    {
        var dataEntity = await _ctx.Questions.FindAsync(id);
        if (dataEntity != null)
        {
            _ctx.Questions.Remove(dataEntity);
            await _ctx.SaveChangesAsync();
        }
    }
}
