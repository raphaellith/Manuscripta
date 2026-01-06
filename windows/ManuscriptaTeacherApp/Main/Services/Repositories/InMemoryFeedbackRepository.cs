using System;
using System.Collections.Concurrent;
using System.Collections.Generic;
using System.Linq;
using System.Threading.Tasks;
using Main.Models.Entities;

namespace Main.Services.Repositories;

/// <summary>
/// In-memory repository for managing feedback.
/// Implements short-term persistence per PersistenceAndCascadingRules.md §1(2)(c).
/// Data is only kept during the application run and not persisted to the database.
/// </summary>
public class InMemoryFeedbackRepository : IFeedbackRepository
{
    private readonly ConcurrentDictionary<Guid, FeedbackEntity> _feedback = new();

    public Task<FeedbackEntity?> GetByIdAsync(Guid id)
    {
        _feedback.TryGetValue(id, out var feedback);
        return Task.FromResult(feedback);
    }

    public Task<FeedbackEntity?> GetByResponseIdAsync(Guid responseId)
    {
        var feedback = _feedback.Values.FirstOrDefault(f => f.ResponseId == responseId);
        return Task.FromResult(feedback);
    }

    public Task<IEnumerable<FeedbackEntity>> GetAllAsync()
    {
        return Task.FromResult<IEnumerable<FeedbackEntity>>(_feedback.Values.ToList());
    }

    public Task AddAsync(FeedbackEntity entity)
    {
        if (entity == null)
            throw new ArgumentNullException(nameof(entity));

        if (!_feedback.TryAdd(entity.Id, entity))
            throw new InvalidOperationException($"Feedback with ID {entity.Id} already exists.");
        
        return Task.CompletedTask;
    }

    public Task UpdateAsync(FeedbackEntity entity)
    {
        if (entity == null)
            throw new ArgumentNullException(nameof(entity));

        _feedback.AddOrUpdate(entity.Id, entity, (key, oldValue) => entity);
        return Task.CompletedTask;
    }

    public Task DeleteAsync(Guid id)
    {
        _feedback.TryRemove(id, out _);
        return Task.CompletedTask;
    }

    /// <summary>
    /// Deletes all feedback associated with a specific response.
    /// Implements orphan removal per PersistenceAndCascadingRules.md §2(2A).
    /// </summary>
    public Task DeleteByResponseIdAsync(Guid responseId)
    {
        var feedbackToRemove = _feedback.Values
            .Where(f => f.ResponseId == responseId)
            .Select(f => f.Id)
            .ToList();

        foreach (var id in feedbackToRemove)
        {
            _feedback.TryRemove(id, out _);
        }

        return Task.CompletedTask;
    }
}
