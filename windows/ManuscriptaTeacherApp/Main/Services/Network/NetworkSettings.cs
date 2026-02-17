namespace Main.Services.Network;

/// <summary>
/// Configuration settings for network services.
/// Per API Contract.md §Ports and FrontendWorkflowSpecifications §2ZA(8).
/// </summary>
public class NetworkSettings
{
    /// <summary>
    /// Default/preferred port for SignalR hub and health endpoint (frontend communication).
    /// Per FrontendWorkflowSpecifications §2ZA(8)(b-c): This is the preferred port, but the
    /// frontend dynamically selects an available port (5910, then 5914-5919 as fallbacks).
    /// NOT enforced via routing - SignalR/health are accessible on any bound port.
    /// </summary>
    public int SignalRPort { get; set; } = 5910;

    /// <summary>
    /// Port for HTTP REST API (Android client communication).
    /// Per API Contract.md §Ports. This port IS enforced - REST controllers only respond here.
    /// Android clients rely on this stable port for material distribution.
    /// </summary>
    public int HttpPort { get; set; } = 5911;

    /// <summary>
    /// Port for TCP control signals.
    /// Per API Contract.md §Ports.
    /// </summary>
    public int TcpPort { get; set; } = 5912;

    /// <summary>
    /// Port for UDP broadcast discovery.
    /// Per API Contract.md §Ports.
    /// </summary>
    public int UdpBroadcastPort { get; set; } = 5913;

    /// <summary>
    /// Interval between UDP broadcast messages in milliseconds.
    /// </summary>
    public int BroadcastIntervalMs { get; set; } = 3000;

    public bool ArePortsDistinct()
    {
        return SignalRPort != HttpPort
            && SignalRPort != TcpPort
            && SignalRPort != UdpBroadcastPort
            && HttpPort != TcpPort
            && HttpPort != UdpBroadcastPort
            && TcpPort != UdpBroadcastPort;
    }
}
