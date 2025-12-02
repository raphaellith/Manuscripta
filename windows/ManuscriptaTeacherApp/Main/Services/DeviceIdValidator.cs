namespace Main.Services;

/// <summary>
/// Provides validation for device IDs.
/// Used to validate §2C(3)(e) and §2D(3)(b) from Validation Rules.
/// </summary>
public static class DeviceIdValidator
{
    /// <summary>
    /// Validates whether a device ID corresponds to a valid, paired device.
    /// </summary>
    /// <param name="deviceId">The device ID to validate.</param>
    /// <returns>True if the device ID is valid and paired; otherwise, false.</returns>
    /// <remarks>
    /// TODO: Implement device ID validation once device registration/pairing is implemented.
    /// This should check against a registry of paired devices maintained by the pairing service.
    /// See Pairing Process.md §1(1)(a) - "The Windows device has recorded the Android device's deviceId."
    /// 
    /// Required validations per Validation Rules:
    /// - §2C(3)(e): A DeviceId must correspond to a valid device (for ResponseEntity)
    /// - §2D(3)(b): A DeviceId must correspond to a valid device (for SessionEntity)
    /// </remarks>
    public static bool IsValidDeviceId(Guid deviceId)
    {
        // TODO: Implement actual validation against paired device registry
        // For now, return true to allow development to proceed
        // This should query the pairing service or device registry
        return true;
    }

    /// <summary>
    /// Validates a device ID and throws if invalid.
    /// </summary>
    /// <param name="deviceId">The device ID to validate.</param>
    /// <exception cref="ArgumentException">Thrown when the device ID is not valid.</exception>
    public static void ValidateOrThrow(Guid deviceId)
    {
        if (!IsValidDeviceId(deviceId))
        {
            throw new ArgumentException(
                $"Device ID '{deviceId}' does not correspond to a valid paired device.",
                nameof(deviceId));
        }
    }
}
