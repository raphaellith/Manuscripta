using System;
using System.Collections.Concurrent;
using System.Collections.Generic;
using System.Linq;
using System.Threading.Tasks;
using Main.Models.Entities.Sessions;

namespace Main.Services.Repositories;

/// <summary>
/// In-memory repository for managing sessions.
/// Implements short-term persistence per PersistenceAndCascadingRules.md §1(2)(b).
/// Data is only kept during the application run and not persisted to the database.
/// </summary>
public class InMemorySessionRepository : ISessionRepository
{
    private readonly ConcurrentDictionary<Guid, SessionEntity> _sessions = new();

    public Task<SessionEntity?> GetByIdAsync(Guid id)
    {
        _sessions.TryGetValue(id, out var session);
        return Task.FromResult(session);
    }

    public Task<IEnumerable<SessionEntity>> GetAllAsync()
    {
        return Task.FromResult<IEnumerable<SessionEntity>>(_sessions.Values.ToList());
    }

    public Task AddAsync(SessionEntity entity)
    {
        if (entity == null)
            throw new ArgumentNullException(nameof(entity));

        _sessions.TryAdd(entity.Id, entity);
        return Task.CompletedTask;
    }

    public Task UpdateAsync(SessionEntity entity)
    {
        if (entity == null)
            throw new ArgumentNullException(nameof(entity));

        _sessions[entity.Id] = entity;
        return Task.CompletedTask;
    }

    public Task DeleteAsync(Guid id)
    {
        _sessions.TryRemove(id, out _);
        return Task.CompletedTask;
    }
}

