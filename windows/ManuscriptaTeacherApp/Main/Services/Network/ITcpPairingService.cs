namespace Main.Services.Network;

/// <summary>
/// Service interface for TCP pairing operations.
/// Implements Pairing Process.md §2(3)(a) - TCP PAIRING_ACK response.
/// </summary>
public interface ITcpPairingService
{
    /// <summary>
    /// Starts listening for TCP pairing requests.
    /// </summary>
    /// <param name="cancellationToken">Token to cancel the listening.</param>
    Task StartListeningAsync(CancellationToken cancellationToken);

    /// <summary>
    /// Stops listening for TCP connections.
    /// </summary>
    void StopListening();

    /// <summary>
    /// Gets whether the service is currently listening.
    /// </summary>
    bool IsListening { get; }

    /// <summary>
    /// Sends a LOCK_SCREEN (0x01) command to the specified device.
    /// </summary>
    Task SendLockScreenAsync(string deviceId);

    /// <summary>
    /// Sends an UNLOCK_SCREEN (0x02) command to the specified device.
    /// </summary>
    Task SendUnlockScreenAsync(string deviceId);

    /// <summary>
    /// Sends a DISTRIBUTE_MATERIAL (0x05) command to the specified device and waits for DISTRIBUTE_ACK.
    /// </summary>
    Task SendDistributeMaterialAsync(string deviceId);
}
