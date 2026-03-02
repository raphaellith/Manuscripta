using System;
using System.Collections.Generic;
using System.Threading.Tasks;
using Main.Models.Entities.Sessions;
using Main.Models.Enums;
using Main.Services.Repositories;

namespace Main.Services;

/// <summary>
/// Service for managing sessions.
/// Implements Section 2(3) of ServiceSpec.md.
/// Enforces business rules and data validation per Validation Rules §2D.
/// </summary>
public class SessionService : ISessionService
{
    private readonly ISessionRepository _sessionRepository;
    private readonly IMaterialRepository _materialRepository;
    private readonly DeviceIdValidator _deviceIdValidator;

    public SessionService(
        ISessionRepository sessionRepository, 
        IMaterialRepository materialRepository,
        DeviceIdValidator deviceIdValidator)
    {
        _sessionRepository = sessionRepository ?? throw new ArgumentNullException(nameof(sessionRepository));
        _materialRepository = materialRepository ?? throw new ArgumentNullException(nameof(materialRepository));
        _deviceIdValidator = deviceIdValidator ?? throw new ArgumentNullException(nameof(deviceIdValidator));
    }

    /// <inheritdoc/>
    public async Task<SessionEntity> CreateSessionAsync(SessionEntity session)
    {
        if (session == null)
            throw new ArgumentNullException(nameof(session));

        // Validate session
        await ValidateSessionAsync(session);

        await _sessionRepository.AddAsync(session);
        return session;
    }

    /// <inheritdoc/>
    public async Task<SessionEntity> ActivateSessionAsync(Guid id)
    {
        var session = await GetSessionOrThrowAsync(id);

        session.SessionStatus = SessionStatus.ACTIVE;
        session.EndTime = null;

        await _sessionRepository.UpdateAsync(session);
        return session;
    }

    /// <inheritdoc/>
    public async Task<SessionEntity> PauseSessionAsync(Guid id)
    {
        var session = await GetSessionOrThrowAsync(id);

        session.SessionStatus = SessionStatus.PAUSED;
        session.EndTime = DateTime.UtcNow;

        await _sessionRepository.UpdateAsync(session);
        return session;
    }

    /// <inheritdoc/>
    public async Task<SessionEntity> CompleteSessionAsync(Guid id)
    {
        var session = await GetSessionOrThrowAsync(id);

        session.SessionStatus = SessionStatus.COMPLETED;
        session.EndTime = DateTime.UtcNow;

        await _sessionRepository.UpdateAsync(session);
        return session;
    }

    /// <inheritdoc/>
    public async Task<SessionEntity> CancelSessionAsync(Guid id)
    {
        var session = await GetSessionOrThrowAsync(id);

        session.SessionStatus = SessionStatus.CANCELLED;
        session.EndTime = DateTime.UtcNow;

        await _sessionRepository.UpdateAsync(session);
        return session;
    }

    #region Helpers

    /// <summary>
    /// Gets a session by ID or throws if not found.
    /// </summary>
    private async Task<SessionEntity> GetSessionOrThrowAsync(Guid id)
    {
        var session = await _sessionRepository.GetByIdAsync(id);
        if (session == null)
            throw new InvalidOperationException($"Session with ID {id} not found.");
        return session;
    }

    #endregion

    #region Validation

    /// <summary>
    /// Validates a session according to business rules:
    /// - §2D(3)(a): Non-active sessions must have EndTime
    /// - §2D(3)(b): DeviceId must correspond to a valid device
    /// - Material must exist
    /// </summary>
    private async Task ValidateSessionAsync(SessionEntity session)
    {
        // Validate §2D(3)(a): Non-active sessions must have EndTime
        if (session.SessionStatus != SessionStatus.ACTIVE && !session.EndTime.HasValue)
        {
            throw new InvalidOperationException(
                $"Sessions with status {session.SessionStatus} must have an EndTime (Validation Rules §2D(3)(a)).");
        }

        // Validate §2D(3)(b): DeviceId must correspond to a valid device
        await _deviceIdValidator.ValidateOrThrowAsync(session.DeviceId);

        // Validate that the referenced material exists
        var material = await _materialRepository.GetByIdAsync(session.MaterialId);
        if (material == null)
            throw new InvalidOperationException($"Material with ID {session.MaterialId} not found.");
    }

    #endregion
}
