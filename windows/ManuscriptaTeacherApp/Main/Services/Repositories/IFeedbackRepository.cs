using System.Collections.Generic;
using System.Threading.Tasks;
using Main.Models.Entities;

namespace Main.Services.Repositories;

public interface IFeedbackRepository
{
    Task<FeedbackEntity?> GetByIdAsync(Guid id);
    Task<FeedbackEntity?> GetByResponseIdAsync(Guid responseId);
    Task<IEnumerable<FeedbackEntity>> GetAllAsync();
    Task AddAsync(FeedbackEntity entity);
    Task UpdateAsync(FeedbackEntity entity);
    Task DeleteAsync(Guid id);
    
    /// <summary>
    /// Deletes all feedback associated with a specific response.
    /// Implements orphan removal per PersistenceAndCascadingRules.md §2(2A).
    /// </summary>
    Task DeleteByResponseIdAsync(Guid responseId);
}
