using System;
using System.Collections.Generic;
using System.Threading.Tasks;
using Main.Models.Entities.Sessions;
using Main.Models.Enums;

namespace Main.Services;

/// <summary>
/// Service interface for managing sessions.
/// Implements Section 2(3) of ServiceSpec.md.
/// </summary>
public interface ISessionService
{
    /// <summary>
    /// Creates a new session.
    /// §2(3)(a)
    /// </summary>
    Task<SessionEntity> CreateSessionAsync(SessionEntity session);

    /// <summary>
    /// Activates a session by setting its status to ACTIVE and clearing the end time.
    /// §2(3)(b)
    /// </summary>
    Task<SessionEntity> ActivateSessionAsync(Guid id);

    /// <summary>
    /// Pauses a session by setting its status to PAUSED and recording the end time.
    /// §2(3)(b)
    /// </summary>
    Task<SessionEntity> PauseSessionAsync(Guid id);

    /// <summary>
    /// Completes a session by setting its status to COMPLETED and recording the end time.
    /// §2(3)(b)
    /// </summary>
    Task<SessionEntity> CompleteSessionAsync(Guid id);

    /// <summary>
    /// Cancels a session by setting its status to CANCELLED and recording the end time.
    /// §2(3)(b)
    /// </summary>
    Task<SessionEntity> CancelSessionAsync(Guid id);

    /// <summary>
    /// Retrieves all sessions.
    /// §2(3)(c)
    /// </summary>
    Task<IEnumerable<SessionEntity>> GetAllSessionsAsync();

    /// <summary>
    /// Retrieves a session by its ID.
    /// §2(3)(d)
    /// </summary>
    Task<SessionEntity?> GetSessionByIdAsync(Guid id);

    /// <summary>
    /// Deletes a session.
    /// §2(3)(e)
    /// </summary>
    Task DeleteSessionAsync(Guid id);
}
