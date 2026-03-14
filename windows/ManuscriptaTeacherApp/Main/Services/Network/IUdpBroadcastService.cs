namespace Main.Services.Network;

/// <summary>
/// Service interface for UDP broadcasting of server presence.
/// Implements Pairing Process.md §2(1) - UDP discovery message broadcasting.
/// </summary>
public interface IUdpBroadcastService
{
    /// <summary>
    /// Starts broadcasting UDP discovery messages at regular intervals.
    /// </summary>
    /// <param name="cancellationToken">Token to cancel the broadcasting.</param>
    Task StartBroadcastingAsync(CancellationToken cancellationToken);

    /// <summary>
    /// Stops the UDP broadcasting.
    /// </summary>
    void StopBroadcasting();

    /// <summary>
    /// Gets whether the service is currently broadcasting.
    /// </summary>
    bool IsBroadcasting { get; }
}
