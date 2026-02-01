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

    /// <summary>
    /// Checks if a response already exists for the given question and device combination.
    /// Per Validation Rules §2C(3)(f): one response per question per device.
    /// </summary>
    Task<bool> ExistsForQuestionAndDeviceAsync(Guid questionId, Guid deviceId);
}
