using Main.Models.Entities;

namespace Main.Services.Repositories;

/// <summary>
/// Repository interface for per-device configuration overrides.
/// Per ConfigurationManagementSpecification §2(2)(a): stores only deviations from defaults.
/// Short-term persisted per PersistenceAndCascadingRules §1(2).
/// </summary>
public interface IConfigurationOverrideRepository
{
    /// <summary>
    /// Gets the overrides for a specific device, or null if none set.
    /// </summary>
    /// <param name="deviceId">The device identifier.</param>
    /// <returns>The override, or null if no overrides are set for this device.</returns>
    ConfigurationOverride? GetByDeviceId(Guid deviceId);

    /// <summary>
    /// Sets overrides for a specific device.
    /// </summary>
    /// <param name="deviceId">The device identifier.</param>
    /// <param name="overrides">The override values.</param>
    void Set(Guid deviceId, ConfigurationOverride overrides);

    /// <summary>
    /// Removes all overrides for a specific device.
    /// </summary>
    /// <param name="deviceId">The device identifier.</param>
    void Remove(Guid deviceId);
}
