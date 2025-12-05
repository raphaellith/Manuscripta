using System.Collections.Generic;
using System.Threading.Tasks;
using Main.Models.Entities.Responses;

namespace Main.Services.Repositories;

public interface IResponseRepository
{
    Task<ResponseEntity?> GetByIdAsync(Guid id);
    Task<IEnumerable<ResponseEntity>> GetByQuestionIdAsync(Guid questionId);
    Task AddAsync(ResponseEntity entity);
    Task UpdateAsync(ResponseEntity entity);
    Task DeleteAsync(Guid id);
    
    /// <summary>
    /// Deletes all responses associated with a specific question.
    /// Implements orphan removal per PersistenceAndCascadingRules.md §2(2).
    /// </summary>
    Task DeleteByQuestionIdAsync(Guid questionId);
}
