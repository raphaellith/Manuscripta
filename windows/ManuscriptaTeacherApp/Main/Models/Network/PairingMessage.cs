using System.Text;

namespace Main.Models.Network;

/// <summary>
/// Handles encoding and decoding of TCP pairing messages.
/// See API Contract.md §3.5 for message format specification.
/// </summary>
public static class PairingMessage
{
    /// <summary>
    /// Decodes a PAIRING_REQUEST message to extract the device ID.
    /// 
    /// Message Format:
    /// | Byte 0    | Bytes 1-N            |
    /// |-----------|----------------------|
    /// | 0x20      | Device ID (UTF-8)    |
    /// </summary>
    /// <param name="data">The raw message bytes.</param>
    /// <returns>The device ID as a GUID.</returns>
    /// <exception cref="ArgumentException">Thrown if the data is invalid.</exception>
    public static Guid DecodePairingRequest(byte[] data)
    {
        if (data == null || data.Length < 2)
        {
            throw new ArgumentException("Data must contain at least opcode and device ID.", nameof(data));
        }

        if (data[0] != BinaryOpcodes.PairingRequest)
        {
            throw new ArgumentException(
                $"Invalid opcode. Expected {BinaryOpcodes.PairingRequest:X2} (PAIRING_REQUEST), got {data[0]:X2}.",
                nameof(data));
        }

        // Extract device ID from bytes 1 to end
        var deviceIdString = Encoding.UTF8.GetString(data, 1, data.Length - 1);

        if (!Guid.TryParse(deviceIdString, out var deviceId))
        {
            throw new ArgumentException($"Invalid device ID format: '{deviceIdString}'.", nameof(data));
        }

        return deviceId;
    }

    /// <summary>
    /// Encodes a PAIRING_REQUEST message with the specified device ID.
    /// </summary>
    /// <param name="deviceId">The device ID to include in the message.</param>
    /// <returns>The encoded message bytes.</returns>
    public static byte[] EncodePairingRequest(Guid deviceId)
    {
        var deviceIdBytes = Encoding.UTF8.GetBytes(deviceId.ToString());
        var buffer = new byte[1 + deviceIdBytes.Length];

        buffer[0] = BinaryOpcodes.PairingRequest;
        Array.Copy(deviceIdBytes, 0, buffer, 1, deviceIdBytes.Length);

        return buffer;
    }

    /// <summary>
    /// Encodes a PAIRING_ACK message.
    /// 
    /// Message Format:
    /// | Byte 0    |
    /// |-----------|
    /// | 0x21      |
    /// </summary>
    /// <returns>A single-byte array containing the PAIRING_ACK opcode.</returns>
    public static byte[] EncodePairingAck()
    {
        return new[] { BinaryOpcodes.PairingAck };
    }

    /// <summary>
    /// Validates that a message is a PAIRING_ACK.
    /// </summary>
    /// <param name="data">The raw message bytes.</param>
    /// <returns>True if the message is a valid PAIRING_ACK; otherwise, false.</returns>
    public static bool IsPairingAck(byte[] data)
    {
        return data != null && data.Length >= 1 && data[0] == BinaryOpcodes.PairingAck;
    }

    /// <summary>
    /// Gets the opcode from a raw message.
    /// </summary>
    /// <param name="data">The raw message bytes.</param>
    /// <returns>The opcode byte, or null if the data is empty.</returns>
    public static byte? GetOpcode(byte[] data)
    {
        return data != null && data.Length >= 1 ? data[0] : null;
    }
}
