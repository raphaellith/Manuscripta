using System.Collections.Concurrent;
using Main.Models.Entities;

namespace Main.Services.Repositories;

/// <summary>
/// Thread-safe in-memory implementation of <see cref="IConfigurationOverrideRepository"/>.
/// Stores per-device configuration overrides using ConcurrentDictionary.
/// Per ConfigurationManagementSpecification §2(2)(a): stores only deviations from defaults.
/// Short-term persisted per PersistenceAndCascadingRules §1(2).
/// Same pattern as <see cref="Main.Services.DeviceRegistryService"/>.
/// </summary>
public class InMemoryConfigurationOverrideRepository : IConfigurationOverrideRepository
{
    private readonly ConcurrentDictionary<Guid, ConfigurationOverride> _overrides = new();

    /// <inheritdoc />
    public ConfigurationOverride? GetByDeviceId(Guid deviceId)
    {
        return _overrides.TryGetValue(deviceId, out var overrides) ? overrides : null;
    }

    /// <inheritdoc />
    public void Set(Guid deviceId, ConfigurationOverride overrides)
    {
        ArgumentNullException.ThrowIfNull(overrides);
        _overrides[deviceId] = overrides;
    }

    /// <inheritdoc />
    public void Remove(Guid deviceId)
    {
        _overrides.TryRemove(deviceId, out _);
    }
}
