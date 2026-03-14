namespace Main.Services.Network;

/// <summary>
/// Interface for tracking REFRESH_CONFIG acknowledgements.
/// Per Session Interaction.md §6(3): tracks GET /config requests as implicit ACKs.
/// </summary>
public interface IRefreshConfigTracker
{
    /// <summary>
    /// Registers that a REFRESH_CONFIG was sent and we expect a GET /config.
    /// </summary>
    /// <param name="deviceId">The device ID that should call GET /config.</param>
    /// <param name="tcs">TaskCompletionSource to signal when config is received.</param>
    void ExpectConfigRequest(string deviceId, TaskCompletionSource<bool> tcs);

    /// <summary>
    /// Called when a GET /config/{deviceId} request is received.
    /// Completes the pending TaskCompletionSource if one exists.
    /// </summary>
    /// <param name="deviceId">The device ID that made the request.</param>
    void MarkConfigReceived(string deviceId);
}
