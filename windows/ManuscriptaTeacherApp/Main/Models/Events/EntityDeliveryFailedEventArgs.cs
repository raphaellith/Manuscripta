namespace Main.Models.Events;

/// <summary>
/// Event arguments for entity delivery failure notifications.
/// Used for both distribution and feedback delivery failures.
/// Per NetworkingAPISpec §2(1)(d)(ii) and §2(1)(d)(v).
/// </summary>
public class EntityDeliveryFailedEventArgs : EventArgs
{
    /// <summary>
    /// Gets the device ID that the delivery failed for.
    /// </summary>
    public Guid DeviceId { get; }

    /// <summary>
    /// Gets the entity ID (material or feedback) that failed delivery.
    /// </summary>
    public Guid EntityId { get; }

    public EntityDeliveryFailedEventArgs(Guid deviceId, Guid entityId)
    {
        DeviceId = deviceId;
        EntityId = entityId;
    }
}
