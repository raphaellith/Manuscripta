using Main.Models.Entities;

namespace Main.Services;

/// <summary>
/// Service interface for managing paired Android devices.
/// Implements the device registration requirements from Pairing Process.md §1(1)(a) and §2(4).
/// </summary>
public interface IDeviceRegistryService
{
    /// <summary>
    /// Registers a new device as paired.
    /// Per Pairing Process.md §2(4): "The Windows device must store the deviceId received during pairing."
    /// </summary>
    /// <param name="deviceId">The unique identifier of the device.</param>
    /// <param name="name">The user-friendly device name per Pairing Process.md §2(2)(c).</param>
    /// <returns>The created entity if this is a new pairing; null if the device was already paired.</returns>
    Task<PairedDeviceEntity?> RegisterDeviceAsync(Guid deviceId, string name);

    /// <summary>
    /// Checks whether a device is currently paired.
    /// Used for validating incoming requests per Pairing Process.md §1(3):
    /// "Requests received from unpaired Android devices should be rejected."
    /// </summary>
    /// <param name="deviceId">The device ID to check.</param>
    /// <returns>True if the device is paired; otherwise, false.</returns>
    Task<bool> IsDevicePairedAsync(Guid deviceId);

    /// <summary>
    /// Removes a device from the registry (unpairing).
    /// Per Pairing Process.md §3(2): "It should also remove the Android client from its registry."
    /// </summary>
    /// <param name="deviceId">The device ID to remove.</param>
    /// <returns>True if the device was removed; false if it wasn't registered.</returns>
    Task<bool> UnregisterDeviceAsync(Guid deviceId);

    /// <summary>
    /// Retrieves all paired devices.
    /// Per NetworkingAPISpec §1(1)(e)(ii): GetAllPairedDevices().
    /// </summary>
    /// <returns>All paired device entities.</returns>
    Task<IEnumerable<PairedDeviceEntity>> GetAllAsync();

    /// <summary>
    /// Updates a paired device entity (e.g., for renaming).
    /// Per FrontendWorkflowSpec §5B(4).
    /// </summary>
    /// <param name="entity">The entity with updated properties.</param>
    Task UpdateAsync(PairedDeviceEntity entity);

    /// <summary>
    /// Event raised when a new device is successfully paired.
    /// Per FrontendWorkflowSpecifications §5A(3).
    /// </summary>
    event EventHandler<PairedDeviceEntity>? DevicePaired;
}
