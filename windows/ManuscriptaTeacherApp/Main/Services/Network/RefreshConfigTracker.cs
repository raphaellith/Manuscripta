using System.Collections.Concurrent;

namespace Main.Services.Network;

/// <summary>
/// Implementation of REFRESH_CONFIG acknowledgement tracking.
/// Per Session Interaction.md §6(3): tracks GET /config requests as implicit ACKs.
/// </summary>
public class RefreshConfigTracker : IRefreshConfigTracker
{
    private readonly ConcurrentDictionary<string, TaskCompletionSource<bool>> _pendingRequests = new();

    /// <inheritdoc />
    public void ExpectConfigRequest(string deviceId, TaskCompletionSource<bool> tcs)
    {
        _pendingRequests[deviceId] = tcs;
    }

    /// <inheritdoc />
    public void MarkConfigReceived(string deviceId)
    {
        if (_pendingRequests.TryRemove(deviceId, out var tcs))
        {
            tcs.TrySetResult(true);
        }
    }
}
