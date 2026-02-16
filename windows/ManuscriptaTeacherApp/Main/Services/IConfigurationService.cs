using Main.Models.Entities;

namespace Main.Services;

/// <summary>
/// Service interface for managing configuration per ConfigurationManagementSpecification.
/// Provides default config CRUD, per-device override management, and compiled config for devices.
/// </summary>
public interface IConfigurationService
{
    /// <summary>
    /// Gets the default configuration.
    /// Per ConfigurationManagementSpecification §1(3)(a).
    /// </summary>
    Task<ConfigurationEntity> GetDefaultsAsync();

    /// <summary>
    /// Validates and updates the default configuration.
    /// Per Validation Rules §2G. Triggers config refresh to all devices per §3(1)(b).
    /// </summary>
    /// <param name="entity">The updated default configuration.</param>
    /// <returns>The updated default configuration.</returns>
    Task<ConfigurationEntity> UpdateDefaultsAsync(ConfigurationEntity entity);

    /// <summary>
    /// Gets the overrides for a specific device, or null if none set.
    /// Per ConfigurationManagementSpecification §2(2)(a).
    /// </summary>
    /// <param name="deviceId">The device identifier.</param>
    ConfigurationOverride? GetOverride(Guid deviceId);

    /// <summary>
    /// Validates and sets overrides for a specific device.
    /// Per Validation Rules §2G. Triggers config refresh per §3(1)(c).
    /// </summary>
    /// <param name="deviceId">The device identifier.</param>
    /// <param name="overrides">The override values.</param>
    void SetOverride(Guid deviceId, ConfigurationOverride overrides);

    /// <summary>
    /// Removes all overrides for a specific device.
    /// </summary>
    /// <param name="deviceId">The device identifier.</param>
    void RemoveOverride(Guid deviceId);

    /// <summary>
    /// Compiles the full configuration for a device by merging defaults with overrides.
    /// Per ConfigurationManagementSpecification §2(2)(b).
    /// </summary>
    /// <param name="deviceId">The device identifier.</param>
    /// <returns>A compiled ConfigurationEntity conformant to Validation Rules §2G.</returns>
    Task<ConfigurationEntity> CompileConfigAsync(Guid deviceId);
}
