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

    /// <summary>
    /// Sends an UNPAIR (0x04) command to the specified device and removes it from registry.
    /// Per Pairing Process.md §3(2).
    /// </summary>
    Task SendUnpairAsync(string deviceId);

    /// <summary>
    /// Sends a REFRESH_CONFIG (0x03) command to the specified device.
    /// Per Session Interaction.md §6(3).
    /// </summary>
    Task SendRefreshConfigAsync(string deviceId);

    /// <summary>
    /// Event raised when a device status update (STATUS_UPDATE 0x10) is received.
    /// Per Session Interaction.md §2.
    /// </summary>
    event EventHandler<Models.Events.DeviceStatusEventArgs>? StatusUpdateReceived;

    /// <summary>
    /// Event raised when a device is deemed disconnected due to heartbeat silence.
    /// Per Session Interaction.md §2(3): after 10 seconds of no heartbeat.
    /// </summary>
    event EventHandler<Models.Events.DeviceStatusEventArgs>? DeviceDisconnected;

    /// <summary>
    /// Event raised when a control command times out waiting for implicit acknowledgement.
    /// Per Session Interaction.md §6(2) and §6(3).
    /// </summary>
    event EventHandler<Models.Events.ControlTimeoutEventArgs>? ControlCommandTimedOut;
}

