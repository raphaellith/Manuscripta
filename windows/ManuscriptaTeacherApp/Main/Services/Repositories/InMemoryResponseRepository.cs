using System;
using System.Collections.Concurrent;
using System.Collections.Generic;
using System.Linq;
using System.Threading.Tasks;
using Main.Models.Entities.Responses;

namespace Main.Services.Repositories;

/// <summary>
/// In-memory repository for managing responses.
/// Implements short-term persistence per PersistenceAndCascadingRules.md §1(2)(a).
/// Data is only kept during the application run and not persisted to the database.
/// </summary>
public class InMemoryResponseRepository : IResponseRepository
{
    private readonly ConcurrentDictionary<Guid, ResponseEntity> _responses = new();

    public Task<ResponseEntity?> GetByIdAsync(Guid id)
    {
        _responses.TryGetValue(id, out var response);
        return Task.FromResult(response);
    }

    public Task<IEnumerable<ResponseEntity>> GetByQuestionIdAsync(Guid questionId)
    {
        var responses = _responses.Values
            .Where(r => r.QuestionId == questionId)
            .ToList();
        return Task.FromResult<IEnumerable<ResponseEntity>>(responses);
    }

    public Task AddAsync(ResponseEntity entity)
    {
        if (entity == null)
            throw new ArgumentNullException(nameof(entity));

        _responses.TryAdd(entity.Id, entity);
        return Task.CompletedTask;
    }

    public Task UpdateAsync(ResponseEntity entity)
    {
        if (entity == null)
            throw new ArgumentNullException(nameof(entity));

        _responses[entity.Id] = entity;
        return Task.CompletedTask;
    }

    public Task DeleteAsync(Guid id)
    {
        _responses.TryRemove(id, out _);
        return Task.CompletedTask;
    }

    /// <summary>
    /// Deletes all responses associated with a specific question.
    /// Implements orphan removal per PersistenceAndCascadingRules.md §2(2).
    /// </summary>
    public Task DeleteByQuestionIdAsync(Guid questionId)
    {
        var responsesToRemove = _responses.Values
            .Where(r => r.QuestionId == questionId)
            .Select(r => r.Id)
            .ToList();

        foreach (var id in responsesToRemove)
        {
            _responses.TryRemove(id, out _);
        }

        return Task.CompletedTask;
    }
}

