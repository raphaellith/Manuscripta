using System.Net;
using Main.Models.Network;
using Xunit;

namespace MainTests.ModelsTests.Network;

/// <summary>
/// Tests for DiscoveryMessage encoding and decoding.
/// Verifies message format per API Contract.md §1.1 and §3.3.
/// </summary>
public class DiscoveryMessageTests
{
    #region Constructor Tests

    [Fact]
    public void Constructor_ValidIPv4Address_CreatesMessage()
    {
        // Arrange
        var ipAddress = IPAddress.Parse("192.168.1.100");
        ushort httpPort = 5911;
        ushort tcpPort = 5912;

        // Act
        var message = new DiscoveryMessage(ipAddress, httpPort, tcpPort);

        // Assert
        Assert.Equal(ipAddress, message.IpAddress);
        Assert.Equal(httpPort, message.HttpPort);
        Assert.Equal(tcpPort, message.TcpPort);
    }

    [Fact]
    public void Constructor_IPv6Address_ThrowsArgumentException()
    {
        // Arrange
        var ipv6Address = IPAddress.Parse("::1");

        // Act & Assert
        var exception = Assert.Throws<ArgumentException>(() =>
            new DiscoveryMessage(ipv6Address, 5911, 5912));

        Assert.Contains("IPv4", exception.Message);
    }

    #endregion

    #region Encode Tests

    [Fact]
    public void Encode_ReturnsCorrectMessageSize()
    {
        // Arrange
        var message = new DiscoveryMessage(IPAddress.Parse("192.168.1.100"), 5911, 5912);

        // Act
        var encoded = message.Encode();

        // Assert - Per API Contract §1.1: Total message size = 9 bytes
        Assert.Equal(DiscoveryMessage.MessageSize, encoded.Length);
        Assert.Equal(9, encoded.Length);
    }

    [Fact]
    public void Encode_ContainsCorrectOpcode()
    {
        // Arrange
        var message = new DiscoveryMessage(IPAddress.Parse("192.168.1.100"), 5911, 5912);

        // Act
        var encoded = message.Encode();

        // Assert - Byte 0 should be DISCOVERY opcode (0x00)
        Assert.Equal(BinaryOpcodes.Discovery, encoded[0]);
    }

    [Fact]
    public void Encode_ContainsCorrectIPAddress_BigEndian()
    {
        // Arrange - 192.168.1.100
        var message = new DiscoveryMessage(IPAddress.Parse("192.168.1.100"), 5911, 5912);

        // Act
        var encoded = message.Encode();

        // Assert - Bytes 1-4: IP address in network byte order (big-endian)
        Assert.Equal(0xC0, encoded[1]); // 192
        Assert.Equal(0xA8, encoded[2]); // 168
        Assert.Equal(0x01, encoded[3]); // 1
        Assert.Equal(0x64, encoded[4]); // 100
    }

    [Fact]
    public void Encode_ContainsCorrectHttpPort_LittleEndian()
    {
        // Arrange - HTTP port 5911 = 0x1717
        var message = new DiscoveryMessage(IPAddress.Parse("192.168.1.100"), 5911, 5912);

        // Act
        var encoded = message.Encode();

        // Assert - Bytes 5-6: HTTP port in little-endian
        // 5911 = 0x1717 → little-endian: 0x17, 0x17
        Assert.Equal(0x17, encoded[5]);
        Assert.Equal(0x17, encoded[6]);
    }

    [Fact]
    public void Encode_ContainsCorrectTcpPort_LittleEndian()
    {
        // Arrange - TCP port 5912 = 0x1718
        var message = new DiscoveryMessage(IPAddress.Parse("192.168.1.100"), 5911, 5912);

        // Act
        var encoded = message.Encode();

        // Assert - Bytes 7-8: TCP port in little-endian
        // 5912 = 0x1718 → little-endian: 0x18, 0x17
        Assert.Equal(0x18, encoded[7]);
        Assert.Equal(0x17, encoded[8]);
    }

    [Fact]
    public void Encode_MatchesApiContractExample()
    {
        // Per API Contract §1.1 example:
        // 192.168.1.100, HTTP 5911, TCP 5912
        // Expected bytes:
        // Byte 0:      0x00                         (DISCOVERY opcode)
        // Bytes 1-4:   0xC0 0xA8 0x01 0x64          (192.168.1.100)
        // Bytes 5-6:   0x17 0x17                    (5911 little-endian)
        // Bytes 7-8:   0x18 0x17                    (5912 little-endian)

        // Arrange
        var message = new DiscoveryMessage(IPAddress.Parse("192.168.1.100"), 5911, 5912);

        // Act
        var encoded = message.Encode();

        // Assert
        var expected = new byte[] { 0x00, 0xC0, 0xA8, 0x01, 0x64, 0x17, 0x17, 0x18, 0x17 };
        Assert.Equal(expected, encoded);
    }

    [Fact]
    public void Encode_DifferentIPAddress_EncodesCorrectly()
    {
        // Arrange - 10.0.0.1
        var message = new DiscoveryMessage(IPAddress.Parse("10.0.0.1"), 8080, 9090);

        // Act
        var encoded = message.Encode();

        // Assert
        Assert.Equal(0x0A, encoded[1]); // 10
        Assert.Equal(0x00, encoded[2]); // 0
        Assert.Equal(0x00, encoded[3]); // 0
        Assert.Equal(0x01, encoded[4]); // 1
    }

    #endregion

    #region Decode Tests

    [Fact]
    public void Decode_ValidMessage_ReturnsCorrectValues()
    {
        // Arrange - Same as API Contract example
        var data = new byte[] { 0x00, 0xC0, 0xA8, 0x01, 0x64, 0x17, 0x17, 0x18, 0x17 };

        // Act
        var message = DiscoveryMessage.Decode(data);

        // Assert
        Assert.Equal(IPAddress.Parse("192.168.1.100"), message.IpAddress);
        Assert.Equal((ushort)5911, message.HttpPort);
        Assert.Equal((ushort)5912, message.TcpPort);
    }

    [Fact]
    public void Decode_NullData_ThrowsArgumentException()
    {
        // Act & Assert
        Assert.Throws<ArgumentException>(() => DiscoveryMessage.Decode(null!));
    }

    [Fact]
    public void Decode_TooShortData_ThrowsArgumentException()
    {
        // Arrange - Only 5 bytes, need 9
        var data = new byte[] { 0x00, 0xC0, 0xA8, 0x01, 0x64 };

        // Act & Assert
        var exception = Assert.Throws<ArgumentException>(() => DiscoveryMessage.Decode(data));
        Assert.Contains("9 bytes", exception.Message);
    }

    [Fact]
    public void Decode_InvalidOpcode_ThrowsArgumentException()
    {
        // Arrange - Wrong opcode (0x01 instead of 0x00)
        var data = new byte[] { 0x01, 0xC0, 0xA8, 0x01, 0x64, 0x17, 0x17, 0x18, 0x17 };

        // Act & Assert
        var exception = Assert.Throws<ArgumentException>(() => DiscoveryMessage.Decode(data));
        Assert.Contains("opcode", exception.Message.ToLower());
    }

    [Fact]
    public void Decode_ExtraData_IgnoresExtraBytes()
    {
        // Arrange - Valid message with extra bytes at the end
        var data = new byte[] { 0x00, 0xC0, 0xA8, 0x01, 0x64, 0x17, 0x17, 0x18, 0x17, 0xFF, 0xFF };

        // Act
        var message = DiscoveryMessage.Decode(data);

        // Assert - Should decode correctly, ignoring extra bytes
        Assert.Equal(IPAddress.Parse("192.168.1.100"), message.IpAddress);
        Assert.Equal((ushort)5911, message.HttpPort);
        Assert.Equal((ushort)5912, message.TcpPort);
    }

    #endregion

    #region Roundtrip Tests

    [Theory]
    [InlineData("192.168.1.100", 5911, 5912)]
    [InlineData("10.0.0.1", 8080, 9090)]
    [InlineData("255.255.255.255", 65535, 65535)]
    [InlineData("0.0.0.0", 0, 0)]
    [InlineData("172.16.0.1", 80, 443)]
    public void EncodeAndDecode_Roundtrip_PreservesValues(string ip, int httpPort, int tcpPort)
    {
        // Arrange
        var original = new DiscoveryMessage(
            IPAddress.Parse(ip),
            (ushort)httpPort,
            (ushort)tcpPort);

        // Act
        var encoded = original.Encode();
        var decoded = DiscoveryMessage.Decode(encoded);

        // Assert
        Assert.Equal(original.IpAddress, decoded.IpAddress);
        Assert.Equal(original.HttpPort, decoded.HttpPort);
        Assert.Equal(original.TcpPort, decoded.TcpPort);
    }

    #endregion

    #region MessageSize Tests

    [Fact]
    public void MessageSize_Is9Bytes()
    {
        // Per API Contract §1.1: Total message size = 9 bytes
        Assert.Equal(9, DiscoveryMessage.MessageSize);
    }

    #endregion
}
