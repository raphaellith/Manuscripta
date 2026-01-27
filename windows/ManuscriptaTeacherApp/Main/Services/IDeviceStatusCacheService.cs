using Main.Models.Entities;

namespace Main.Services;

/// <summary>
/// Service interface for caching device status updates in memory.
/// Per FrontendWorkflowSpecifications §5B(3): frontend updates status on backend events.
/// </summary>
public interface IDeviceStatusCacheService
{
    /// <summary>
    /// Updates or adds a device status in the cache.
    /// </summary>
    /// <param name="status">The device status entity to cache.</param>
    void UpdateStatus(DeviceStatusEntity status);

    /// <summary>
    /// Gets the current status for a specific device.
    /// </summary>
    /// <param name="deviceId">The device ID.</param>
    /// <returns>The cached status, or null if not found.</returns>
    DeviceStatusEntity? GetStatus(Guid deviceId);

    /// <summary>
    /// Gets all cached device statuses.
    /// Per NetworkingAPISpec §1(1)(e)(iii): GetAllDeviceStatuses().
    /// </summary>
    /// <returns>All currently cached device statuses.</returns>
    IEnumerable<DeviceStatusEntity> GetAllStatuses();

    /// <summary>
    /// Marks a device as disconnected.
    /// Per Session Interaction.md §2(3): after 10 seconds of no heartbeat.
    /// </summary>
    /// <param name="deviceId">The device ID to mark as disconnected.</param>
    void MarkDisconnected(Guid deviceId);

    /// <summary>
    /// Removes a device from the cache (e.g., on unpair).
    /// </summary>
    /// <param name="deviceId">The device ID to remove.</param>
    void RemoveDevice(Guid deviceId);
}
