using System.Collections.Generic;
using System.Linq;
using System.Threading.Tasks;
using Microsoft.EntityFrameworkCore;
using Main.Data;
using Main.Models.Entities;
using Main.Models.Entities.Responses;
using Main.Models.Mappings;

namespace Main.Services.Repositories;

/// <summary>
/// Repository for managing responses. Uses ResponseDataEntity for persistence
/// and ResponseEntity for business logic.
/// </summary>
public class EfResponseRepository : IResponseRepository
{
    private readonly MainDbContext _ctx;
    private readonly IQuestionRepository _questionRepository;

    public EfResponseRepository(MainDbContext ctx, IQuestionRepository questionRepository)
    {
        _ctx = ctx;
        _questionRepository = questionRepository;
    }

    public async Task<ResponseEntity?> GetByIdAsync(Guid id)
    {
        var dataEntity = await _ctx.Responses
            .FirstOrDefaultAsync(r => r.Id == id);
        
        if (dataEntity == null)
            return null;
        
        // Get the associated question to determine response type
        var question = await _questionRepository.GetByIdAsync(dataEntity.QuestionId);
        if (question == null)
            throw new InvalidOperationException($"Question with ID {dataEntity.QuestionId} not found for response {id}");
        
        return ResponseEntityMapper.ToEntity(dataEntity, question);
    }

    public async Task<IEnumerable<ResponseEntity>> GetByQuestionIdAsync(Guid questionId)
    {
        var dataEntities = await _ctx.Responses
            .Where(r => r.QuestionId == questionId)
            .ToListAsync();
        
        if (!dataEntities.Any())
            return Enumerable.Empty<ResponseEntity>();
        
        // Get the associated question to determine response type
        var question = await _questionRepository.GetByIdAsync(questionId);
        if (question == null)
            throw new InvalidOperationException($"Question with ID {questionId} not found");
        
        return dataEntities.Select(de => ResponseEntityMapper.ToEntity(de, question));
    }

    public async Task AddAsync(ResponseEntity entity)
    {
        var dataEntity = ResponseEntityMapper.ToDataEntity(entity);
        await _ctx.Responses.AddAsync(dataEntity);
        await _ctx.SaveChangesAsync();
    }

    public async Task UpdateAsync(ResponseEntity entity)
    {
        var dataEntity = ResponseEntityMapper.ToDataEntity(entity);
        _ctx.Responses.Update(dataEntity);
        await _ctx.SaveChangesAsync();
    }

    public async Task DeleteAsync(Guid id)
    {
        var dataEntity = await _ctx.Responses.FindAsync(id);
        if (dataEntity != null)
        {
            _ctx.Responses.Remove(dataEntity);
            await _ctx.SaveChangesAsync();
        }
    }
}
