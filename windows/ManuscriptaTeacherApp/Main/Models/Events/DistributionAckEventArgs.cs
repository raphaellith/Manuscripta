namespace Main.Models.Events;

/// <summary>
/// Event arguments for distribution acknowledgement per material.
/// Per API Contract.md §3.6.2: DISTRIBUTE_ACK uses per-entity acknowledgement.
/// </summary>
public class DistributionAckEventArgs : EventArgs
{
    /// <summary>
    /// Gets the device ID that acknowledged receipt.
    /// </summary>
    public Guid DeviceId { get; }

    /// <summary>
    /// Gets the material ID that was acknowledged.
    /// </summary>
    public Guid MaterialId { get; }

    public DistributionAckEventArgs(Guid deviceId, Guid materialId)
    {
        DeviceId = deviceId;
        MaterialId = materialId;
    }
}
