using System.Text;
using Main.Models.Network;
using Xunit;

namespace MainTests.ModelsTests.Network;

/// <summary>
/// Tests for PairingMessage encoding and decoding.
/// Verifies message format per API Contract.md §3.5.
/// </summary>
public class PairingMessageTests
{
    #region DecodePairingRequest Tests

    [Fact]
    public void DecodePairingRequest_ValidMessage_ReturnsDeviceId()
    {
        // Arrange
        var deviceId = Guid.NewGuid();
        var deviceIdBytes = Encoding.UTF8.GetBytes(deviceId.ToString());
        var data = new byte[1 + deviceIdBytes.Length];
        data[0] = BinaryOpcodes.PairingRequest;
        Array.Copy(deviceIdBytes, 0, data, 1, deviceIdBytes.Length);

        // Act
        var result = PairingMessage.DecodePairingRequest(data);

        // Assert
        Assert.Equal(deviceId, result);
    }

    [Fact]
    public void DecodePairingRequest_NullData_ThrowsArgumentException()
    {
        // Act & Assert
        Assert.Throws<ArgumentException>(() => PairingMessage.DecodePairingRequest(null!));
    }

    [Fact]
    public void DecodePairingRequest_EmptyData_ThrowsArgumentException()
    {
        // Act & Assert
        Assert.Throws<ArgumentException>(() => PairingMessage.DecodePairingRequest(Array.Empty<byte>()));
    }

    [Fact]
    public void DecodePairingRequest_OnlyOpcode_ThrowsArgumentException()
    {
        // Arrange - Only opcode, no device ID
        var data = new byte[] { BinaryOpcodes.PairingRequest };

        // Act & Assert
        Assert.Throws<ArgumentException>(() => PairingMessage.DecodePairingRequest(data));
    }

    [Fact]
    public void DecodePairingRequest_WrongOpcode_ThrowsArgumentException()
    {
        // Arrange - Use PAIRING_ACK opcode instead of PAIRING_REQUEST
        var deviceId = Guid.NewGuid();
        var deviceIdBytes = Encoding.UTF8.GetBytes(deviceId.ToString());
        var data = new byte[1 + deviceIdBytes.Length];
        data[0] = BinaryOpcodes.PairingAck; // Wrong opcode
        Array.Copy(deviceIdBytes, 0, data, 1, deviceIdBytes.Length);

        // Act & Assert
        var exception = Assert.Throws<ArgumentException>(() => PairingMessage.DecodePairingRequest(data));
        Assert.Contains("PAIRING_REQUEST", exception.Message);
    }

    [Fact]
    public void DecodePairingRequest_InvalidGuidFormat_ThrowsArgumentException()
    {
        // Arrange - Invalid GUID string
        var invalidGuid = "not-a-valid-guid";
        var invalidGuidBytes = Encoding.UTF8.GetBytes(invalidGuid);
        var data = new byte[1 + invalidGuidBytes.Length];
        data[0] = BinaryOpcodes.PairingRequest;
        Array.Copy(invalidGuidBytes, 0, data, 1, invalidGuidBytes.Length);

        // Act & Assert
        var exception = Assert.Throws<ArgumentException>(() => PairingMessage.DecodePairingRequest(data));
        Assert.Contains("Invalid device ID format", exception.Message);
    }

    #endregion

    #region EncodePairingRequest Tests

    [Fact]
    public void EncodePairingRequest_ValidDeviceId_ReturnsCorrectFormat()
    {
        // Arrange
        var deviceId = Guid.NewGuid();

        // Act
        var encoded = PairingMessage.EncodePairingRequest(deviceId);

        // Assert
        Assert.Equal(BinaryOpcodes.PairingRequest, encoded[0]);
        var decodedDeviceIdString = Encoding.UTF8.GetString(encoded, 1, encoded.Length - 1);
        Assert.Equal(deviceId.ToString(), decodedDeviceIdString);
    }

    [Fact]
    public void EncodePairingRequest_ReturnsCorrectLength()
    {
        // Arrange
        var deviceId = Guid.NewGuid();
        var expectedLength = 1 + Encoding.UTF8.GetByteCount(deviceId.ToString());

        // Act
        var encoded = PairingMessage.EncodePairingRequest(deviceId);

        // Assert
        Assert.Equal(expectedLength, encoded.Length);
    }

    #endregion

    #region EncodePairingAck Tests

    [Fact]
    public void EncodePairingAck_ReturnsSingleByteWithCorrectOpcode()
    {
        // Act
        var encoded = PairingMessage.EncodePairingAck();

        // Assert - Per API Contract §3.5: PAIRING_ACK has no operand
        Assert.Single(encoded);
        Assert.Equal(BinaryOpcodes.PairingAck, encoded[0]);
    }

    #endregion

    #region IsPairingAck Tests

    [Fact]
    public void IsPairingAck_ValidAckMessage_ReturnsTrue()
    {
        // Arrange
        var data = new byte[] { BinaryOpcodes.PairingAck };

        // Act & Assert
        Assert.True(PairingMessage.IsPairingAck(data));
    }

    [Fact]
    public void IsPairingAck_NullData_ReturnsFalse()
    {
        // Act & Assert
        Assert.False(PairingMessage.IsPairingAck(null!));
    }

    [Fact]
    public void IsPairingAck_EmptyData_ReturnsFalse()
    {
        // Act & Assert
        Assert.False(PairingMessage.IsPairingAck(Array.Empty<byte>()));
    }

    [Fact]
    public void IsPairingAck_WrongOpcode_ReturnsFalse()
    {
        // Arrange
        var data = new byte[] { BinaryOpcodes.PairingRequest };

        // Act & Assert
        Assert.False(PairingMessage.IsPairingAck(data));
    }

    [Fact]
    public void IsPairingAck_AckWithExtraBytes_ReturnsTrue()
    {
        // Arrange - ACK with extra bytes (should still be valid)
        var data = new byte[] { BinaryOpcodes.PairingAck, 0xFF, 0xFF };

        // Act & Assert
        Assert.True(PairingMessage.IsPairingAck(data));
    }

    #endregion

    #region GetOpcode Tests

    [Fact]
    public void GetOpcode_ValidData_ReturnsOpcode()
    {
        // Arrange
        var data = new byte[] { BinaryOpcodes.PairingRequest, 0x01, 0x02 };

        // Act
        var opcode = PairingMessage.GetOpcode(data);

        // Assert
        Assert.Equal(BinaryOpcodes.PairingRequest, opcode);
    }

    [Fact]
    public void GetOpcode_NullData_ReturnsNull()
    {
        // Act & Assert
        Assert.Null(PairingMessage.GetOpcode(null!));
    }

    [Fact]
    public void GetOpcode_EmptyData_ReturnsNull()
    {
        // Act & Assert
        Assert.Null(PairingMessage.GetOpcode(Array.Empty<byte>()));
    }

    [Theory]
    [InlineData(BinaryOpcodes.Discovery)]
    [InlineData(BinaryOpcodes.PairingRequest)]
    [InlineData(BinaryOpcodes.PairingAck)]
    [InlineData(BinaryOpcodes.LockScreen)]
    [InlineData(BinaryOpcodes.StatusUpdate)]
    public void GetOpcode_VariousOpcodes_ReturnsCorrectValue(byte expectedOpcode)
    {
        // Arrange
        var data = new byte[] { expectedOpcode };

        // Act
        var opcode = PairingMessage.GetOpcode(data);

        // Assert
        Assert.Equal(expectedOpcode, opcode);
    }

    #endregion

    #region Roundtrip Tests

    [Fact]
    public void EncodePairingRequest_ThenDecode_PreservesDeviceId()
    {
        // Arrange
        var originalDeviceId = Guid.NewGuid();

        // Act
        var encoded = PairingMessage.EncodePairingRequest(originalDeviceId);
        var decoded = PairingMessage.DecodePairingRequest(encoded);

        // Assert
        Assert.Equal(originalDeviceId, decoded);
    }

    [Theory]
    [InlineData("00000000-0000-0000-0000-000000000000")]
    [InlineData("ffffffff-ffff-ffff-ffff-ffffffffffff")]
    [InlineData("12345678-1234-1234-1234-123456789012")]
    public void EncodePairingRequest_ThenDecode_VariousGuids_PreservesDeviceId(string guidString)
    {
        // Arrange
        var originalDeviceId = Guid.Parse(guidString);

        // Act
        var encoded = PairingMessage.EncodePairingRequest(originalDeviceId);
        var decoded = PairingMessage.DecodePairingRequest(encoded);

        // Assert
        Assert.Equal(originalDeviceId, decoded);
    }

    #endregion
}
