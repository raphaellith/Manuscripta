using System.Net;

namespace Main.Models.Network;

/// <summary>
/// Represents a UDP discovery message broadcast by the server.
/// See API Contract.md §1.1 and §3.3 for message format specification.
/// 
/// Message Format (9 bytes total):
/// | Field       | Offset | Size    | Description                              |
/// |-------------|--------|---------|------------------------------------------|
/// | Opcode      | 0      | 1 byte  | 0x00 = DISCOVERY                         |
/// | IP Address  | 1      | 4 bytes | IPv4 address (network byte order, big-endian) |
/// | HTTP Port   | 5      | 2 bytes | Unsigned, little-endian                  |
/// | TCP Port    | 7      | 2 bytes | Unsigned, little-endian                  |
/// </summary>
public class DiscoveryMessage
{
    /// <summary>
    /// Total size of the discovery message in bytes.
    /// </summary>
    public const int MessageSize = 9;

    /// <summary>
    /// The IPv4 address of the server.
    /// </summary>
    public IPAddress IpAddress { get; }

    /// <summary>
    /// The HTTP port the server is listening on.
    /// </summary>
    public ushort HttpPort { get; }

    /// <summary>
    /// The TCP port the server is listening on.
    /// </summary>
    public ushort TcpPort { get; }

    /// <summary>
    /// Creates a new discovery message.
    /// </summary>
    /// <param name="ipAddress">The server's IPv4 address.</param>
    /// <param name="httpPort">The HTTP port.</param>
    /// <param name="tcpPort">The TCP port.</param>
    /// <exception cref="ArgumentException">Thrown if the IP address is not IPv4.</exception>
    public DiscoveryMessage(IPAddress ipAddress, ushort httpPort, ushort tcpPort)
    {
        if (ipAddress.AddressFamily != System.Net.Sockets.AddressFamily.InterNetwork)
        {
            throw new ArgumentException("Only IPv4 addresses are supported.", nameof(ipAddress));
        }

        IpAddress = ipAddress;
        HttpPort = httpPort;
        TcpPort = tcpPort;
    }

    /// <summary>
    /// Encodes the discovery message to a byte array for UDP transmission.
    /// </summary>
    /// <returns>A 9-byte array containing the encoded message.</returns>
    public byte[] Encode()
    {
        var buffer = new byte[MessageSize];

        // Byte 0: Opcode
        buffer[0] = BinaryOpcodes.Discovery;

        // Bytes 1-4: IP address (network byte order / big-endian)
        var ipBytes = IpAddress.GetAddressBytes();
        buffer[1] = ipBytes[0];
        buffer[2] = ipBytes[1];
        buffer[3] = ipBytes[2];
        buffer[4] = ipBytes[3];

        // Bytes 5-6: HTTP port (little-endian)
        buffer[5] = (byte)(HttpPort & 0xFF);
        buffer[6] = (byte)((HttpPort >> 8) & 0xFF);

        // Bytes 7-8: TCP port (little-endian)
        buffer[7] = (byte)(TcpPort & 0xFF);
        buffer[8] = (byte)((TcpPort >> 8) & 0xFF);

        return buffer;
    }

    /// <summary>
    /// Decodes a discovery message from a byte array.
    /// </summary>
    /// <param name="data">The byte array containing the encoded message.</param>
    /// <returns>The decoded discovery message.</returns>
    /// <exception cref="ArgumentException">Thrown if the data is invalid.</exception>
    public static DiscoveryMessage Decode(byte[] data)
    {
        if (data == null || data.Length < MessageSize)
        {
            throw new ArgumentException($"Data must be at least {MessageSize} bytes.", nameof(data));
        }

        if (data[0] != BinaryOpcodes.Discovery)
        {
            throw new ArgumentException($"Invalid opcode. Expected {BinaryOpcodes.Discovery:X2}, got {data[0]:X2}.", nameof(data));
        }

        // Bytes 1-4: IP address (network byte order / big-endian)
        var ipAddress = new IPAddress(new[] { data[1], data[2], data[3], data[4] });

        // Bytes 5-6: HTTP port (little-endian)
        var httpPort = (ushort)(data[5] | (data[6] << 8));

        // Bytes 7-8: TCP port (little-endian)
        var tcpPort = (ushort)(data[7] | (data[8] << 8));

        return new DiscoveryMessage(ipAddress, httpPort, tcpPort);
    }
}
