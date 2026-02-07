using Microsoft.EntityFrameworkCore;
using Microsoft.Extensions.Logging;

using Main.Data;
using Main.Models.Entities;

namespace Main.Services;

/// <summary>
/// Implementation of <see cref="IDeviceRegistryService"/> using Entity Framework Core.
/// Manages paired Android devices per Pairing Process.md §1(1)(a) and §2(4).
/// </summary>
public class DeviceRegistryService : IDeviceRegistryService
{
    // Short-term persistence per PersistenceAndCascadingRules §1(2)
    // Uses thread-safe in-memory storage
    private readonly System.Collections.Concurrent.ConcurrentDictionary<Guid, PairedDeviceEntity> _devices = new();
    private readonly ILogger<DeviceRegistryService> _logger;

    public DeviceRegistryService(ILogger<DeviceRegistryService> logger)
    {
        _logger = logger ?? throw new ArgumentNullException(nameof(logger));
    }

    /// <inheritdoc />
    public Task<PairedDeviceEntity?> RegisterDeviceAsync(Guid deviceId, string name)
    {
        if (_devices.ContainsKey(deviceId))
        {
            _logger.LogInformation("Device {DeviceId} re-paired (already existed)", deviceId);
            return Task.FromResult<PairedDeviceEntity?>(null);
        }

        var newDevice = new PairedDeviceEntity(deviceId, name);
        if (_devices.TryAdd(deviceId, newDevice))
        {
            _logger.LogInformation("Device {DeviceId} ({Name}) paired successfully", deviceId, name);
            DevicePaired?.Invoke(this, newDevice);
            return Task.FromResult<PairedDeviceEntity?>(newDevice);
        }
        
        // Race condition
        _logger.LogWarning("Device {DeviceId} registration failed due to duplicate (race condition)", deviceId);
        return Task.FromResult<PairedDeviceEntity?>(null);
    }

    /// <inheritdoc />
    public Task<bool> IsDevicePairedAsync(Guid deviceId)
    {
        return Task.FromResult(_devices.ContainsKey(deviceId));
    }

    /// <inheritdoc />
    public Task<bool> UnregisterDeviceAsync(Guid deviceId)
    {
        if (_devices.TryRemove(deviceId, out _))
        {
            _logger.LogInformation("Device {DeviceId} unpaired and removed from registry", deviceId);
            return Task.FromResult(true);
        }

        _logger.LogInformation("Device {DeviceId} not found in registry, nothing to remove", deviceId);
        return Task.FromResult(false);
    }

    /// <inheritdoc />
    public Task<IEnumerable<PairedDeviceEntity>> GetAllAsync()
    {
        return Task.FromResult<IEnumerable<PairedDeviceEntity>>(_devices.Values.ToList());
    }

    /// <inheritdoc />
    public Task UpdateAsync(PairedDeviceEntity entity)
    {
        ArgumentNullException.ThrowIfNull(entity);

        if (_devices.TryGetValue(entity.DeviceId, out var existingDevice))
        {
            // Create a new instance with updated properties and attempt an atomic update
            var updatedDevice = new PairedDeviceEntity(existingDevice.DeviceId, entity.Name);

            if (_devices.TryUpdate(entity.DeviceId, updatedDevice, existingDevice))
            {
                _logger.LogInformation("Device {DeviceId} updated (name: {Name})", entity.DeviceId, entity.Name);
                return Task.CompletedTask;
            }

            _logger.LogWarning("Device {DeviceId} update failed due to concurrent modification", entity.DeviceId);
            throw new InvalidOperationException($"Device {entity.DeviceId} update conflict due to concurrent modification");
        }

        _logger.LogWarning("Device {DeviceId} not found for update", entity.DeviceId);
        throw new InvalidOperationException($"Device {entity.DeviceId} not found");
    }

    /// <inheritdoc />
    public event EventHandler<PairedDeviceEntity>? DevicePaired;
}
