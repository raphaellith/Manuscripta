using System.Collections.Concurrent;
using Microsoft.Extensions.Logging;

using Main.Models.Entities;
using Main.Models.Enums;

namespace Main.Services;

/// <summary>
/// In-memory cache for device status updates.
/// Thread-safe implementation using ConcurrentDictionary.
/// Per FrontendWorkflowSpecifications §5B: provides real-time device statuses.
/// </summary>
public class DeviceStatusCacheService : IDeviceStatusCacheService
{
    private readonly ConcurrentDictionary<Guid, DeviceStatusEntity> _statusCache = new();
    private readonly ILogger<DeviceStatusCacheService> _logger;

    public DeviceStatusCacheService(ILogger<DeviceStatusCacheService> logger)
    {
        _logger = logger ?? throw new ArgumentNullException(nameof(logger));
    }

    /// <inheritdoc />
    public void UpdateStatus(DeviceStatusEntity status)
    {
        ArgumentNullException.ThrowIfNull(status);

        _statusCache.AddOrUpdate(
            status.DeviceId,
            status,
            (_, _) => status);

        _logger.LogDebug(
            "Updated status for device {DeviceId}: {Status}",
            status.DeviceId,
            status.Status);
    }

    /// <inheritdoc />
    public DeviceStatusEntity? GetStatus(Guid deviceId)
    {
        return _statusCache.TryGetValue(deviceId, out var status) ? status : null;
    }

    /// <inheritdoc />
    public IEnumerable<DeviceStatusEntity> GetAllStatuses()
    {
        return _statusCache.Values.ToList();
    }

    /// <inheritdoc />
    public void MarkDisconnected(Guid deviceId)
    {
        if (_statusCache.TryGetValue(deviceId, out var existingStatus))
        {
            var disconnectedStatus = new DeviceStatusEntity(
                deviceId,
                DeviceStatus.DISCONNECTED,
                existingStatus.BatteryLevel,
                existingStatus.CurrentMaterialId,
                existingStatus.StudentView,
                DateTimeOffset.UtcNow.ToUnixTimeSeconds());

            _statusCache[deviceId] = disconnectedStatus;

            _logger.LogInformation(
                "Device {DeviceId} marked as DISCONNECTED",
                deviceId);
        }
        else
        {
            // Device not in cache - add a disconnected entry
            var disconnectedStatus = new DeviceStatusEntity(
                deviceId,
                DeviceStatus.DISCONNECTED,
                0,
                Guid.Empty,
                string.Empty,
                DateTimeOffset.UtcNow.ToUnixTimeSeconds());

            _statusCache.TryAdd(deviceId, disconnectedStatus);

            _logger.LogInformation(
                "Device {DeviceId} added to cache as DISCONNECTED",
                deviceId);
        }
    }

    /// <inheritdoc />
    public void RemoveDevice(Guid deviceId)
    {
        if (_statusCache.TryRemove(deviceId, out _))
        {
            _logger.LogDebug("Removed device {DeviceId} from status cache", deviceId);
        }
    }
}
