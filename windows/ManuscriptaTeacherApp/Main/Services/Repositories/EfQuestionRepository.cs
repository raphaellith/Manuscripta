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
        // Fetch existing data entity to avoid tracking conflicts
        var existingDataEntity = await _ctx.Questions.FindAsync(entity.Id);
        if (existingDataEntity == null)
            throw new InvalidOperationException($"Question with ID {entity.Id} not found.");

        // Update properties from the domain entity
        var updatedDataEntity = QuestionEntityMapper.ToDataEntity(entity);
        existingDataEntity.MaterialId = updatedDataEntity.MaterialId;
        existingDataEntity.QuestionText = updatedDataEntity.QuestionText;
        existingDataEntity.QuestionType = updatedDataEntity.QuestionType;
        existingDataEntity.Options = updatedDataEntity.Options;
        existingDataEntity.CorrectAnswer = updatedDataEntity.CorrectAnswer;
        existingDataEntity.MaxScore = updatedDataEntity.MaxScore;
        
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
