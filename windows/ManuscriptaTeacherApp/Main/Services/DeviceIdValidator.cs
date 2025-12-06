namespace Main.Services;

/// <summary>
/// Provides validation for device IDs.
/// Used to validate §2C(3)(e) and §2D(3)(b) from Validation Rules.
/// See also Pairing Process.md §1(3): "Requests received from unpaired Android devices should be rejected."
/// </summary>
public class DeviceIdValidator
{
    private readonly IDeviceRegistryService _deviceRegistry;

    /// <summary>
    /// Creates a new instance of the DeviceIdValidator.
    /// </summary>
    /// <param name="deviceRegistry">The device registry service for checking paired devices.</param>
    public DeviceIdValidator(IDeviceRegistryService deviceRegistry)
    {
        _deviceRegistry = deviceRegistry ?? throw new ArgumentNullException(nameof(deviceRegistry));
    }

    /// <summary>
    /// Validates whether a device ID corresponds to a valid, paired device.
    /// </summary>
    /// <param name="deviceId">The device ID to validate.</param>
    /// <returns>True if the device ID is valid and paired; otherwise, false.</returns>
    /// <remarks>
    /// Per Pairing Process.md §1(1)(a) - "The Windows device has recorded the Android device's deviceId."
    /// 
    /// Required validations per Validation Rules:
    /// - §2C(3)(e): A DeviceId must correspond to a valid device (for ResponseEntity)
    /// - §2D(3)(b): A DeviceId must correspond to a valid device (for SessionEntity)
    /// </remarks>
    public async Task<bool> IsValidDeviceIdAsync(Guid deviceId)
    {
        return await _deviceRegistry.IsDevicePairedAsync(deviceId);
    }

    /// <summary>
    /// Validates a device ID and throws if invalid.
    /// </summary>
    /// <param name="deviceId">The device ID to validate.</param>
    /// <exception cref="ArgumentException">Thrown when the device ID is not valid.</exception>
    public async Task ValidateOrThrowAsync(Guid deviceId)
    {
        if (!await IsValidDeviceIdAsync(deviceId))
        {
            throw new ArgumentException(
                $"Device ID '{deviceId}' does not correspond to a valid paired device.",
                nameof(deviceId));
        }
    }

    /// <summary>
    /// Synchronous validation for scenarios where async is not practical.
    /// Note: This blocks the calling thread. Prefer async methods when possible.
    /// </summary>
    /// <param name="deviceId">The device ID to validate.</param>
    /// <returns>True if the device ID is valid and paired; otherwise, false.</returns>
    public bool IsValidDeviceId(Guid deviceId)
    {
        return _deviceRegistry.IsDevicePairedAsync(deviceId).GetAwaiter().GetResult();
    }

    /// <summary>
    /// Synchronous validation that throws if invalid.
    /// Note: This blocks the calling thread. Prefer async methods when possible.
    /// </summary>
    /// <param name="deviceId">The device ID to validate.</param>
    /// <exception cref="ArgumentException">Thrown when the device ID is not valid.</exception>
    public void ValidateOrThrow(Guid deviceId)
    {
        if (!IsValidDeviceId(deviceId))
        {
            throw new ArgumentException(
                $"Device ID '{deviceId}' does not correspond to a valid paired device.",
                nameof(deviceId));
        }
    }
}
