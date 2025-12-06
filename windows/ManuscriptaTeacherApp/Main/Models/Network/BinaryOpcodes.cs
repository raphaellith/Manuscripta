namespace Main.Models.Network;

/// <summary>
/// Defines opcode constants for the binary protocol used in TCP and UDP communication.
/// See API Contract.md §3.2 for the opcode registry.
/// </summary>
public static class BinaryOpcodes
{
    // UDP Discovery (§3.3)
    /// <summary>
    /// Server broadcasts presence to clients.
    /// Operand: IP (4 bytes) + HTTP port (2 bytes) + TCP port (2 bytes)
    /// </summary>
    public const byte Discovery = 0x00;

    // Server → Client Control (TCP) (§3.4)
    /// <summary>Locks the student's screen.</summary>
    public const byte LockScreen = 0x01;

    /// <summary>Unlocks the student's screen.</summary>
    public const byte UnlockScreen = 0x02;

    /// <summary>Triggers tablet to re-fetch configuration via HTTP.</summary>
    public const byte RefreshConfig = 0x03;

    // Client → Server Status (TCP) (§3.6)
    /// <summary>Reports device status to teacher. Operand: JSON payload.</summary>
    public const byte StatusUpdate = 0x10;

    /// <summary>Student requests help. Operand: Device ID (UTF-8 string).</summary>
    public const byte HandRaised = 0x11;

    // Pairing (TCP) (§3.5)
    /// <summary>
    /// Client requests pairing with server.
    /// Operand: Device ID (UTF-8 string)
    /// </summary>
    public const byte PairingRequest = 0x20;

    /// <summary>
    /// Server acknowledges successful TCP pairing.
    /// Operand: None
    /// </summary>
    public const byte PairingAck = 0x21;
}
