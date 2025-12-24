using Main.Models.Network;
using Xunit;

namespace MainTests.ModelsTests.Network;

/// <summary>
/// Tests for BinaryOpcodes constants.
/// Verifies opcode values match API Contract.md §3.2.
/// </summary>
public class BinaryOpcodesTests
{
    [Fact]
    public void Discovery_HasCorrectValue()
    {
        // Per API Contract §3.3: DISCOVERY = 0x00
        Assert.Equal(0x00, BinaryOpcodes.Discovery);
    }

    [Fact]
    public void LockScreen_HasCorrectValue()
    {
        // Per API Contract §3.4: LOCK_SCREEN = 0x01
        Assert.Equal(0x01, BinaryOpcodes.LockScreen);
    }

    [Fact]
    public void UnlockScreen_HasCorrectValue()
    {
        // Per API Contract §3.4: UNLOCK_SCREEN = 0x02
        Assert.Equal(0x02, BinaryOpcodes.UnlockScreen);
    }

    [Fact]
    public void RefreshConfig_HasCorrectValue()
    {
        // Per API Contract §3.4: REFRESH_CONFIG = 0x03
        Assert.Equal(0x03, BinaryOpcodes.RefreshConfig);
    }

    [Fact]
    public void Unpair_HasCorrectValue()
    {
        // Per API Contract §3.4: UNPAIR = 0x04
        Assert.Equal(0x04, BinaryOpcodes.Unpair);
    }

    [Fact]
    public void DistributeMaterial_HasCorrectValue()
    {
        // Per API Contract §3.4: DISTRIBUTE_MATERIAL = 0x05
        Assert.Equal(0x05, BinaryOpcodes.DistributeMaterial);
    }

    [Fact]
    public void StatusUpdate_HasCorrectValue()
    {
        // Per API Contract §3.6: STATUS_UPDATE = 0x10
        Assert.Equal(0x10, BinaryOpcodes.StatusUpdate);
    }

    [Fact]
    public void HandRaised_HasCorrectValue()
    {
        // Per API Contract §3.6: HAND_RAISED = 0x11
        Assert.Equal(0x11, BinaryOpcodes.HandRaised);
    }

    [Fact]
    public void PairingRequest_HasCorrectValue()
    {
        // Per API Contract §3.5: PAIRING_REQUEST = 0x20
        Assert.Equal(0x20, BinaryOpcodes.PairingRequest);
    }

    [Fact]
    public void PairingAck_HasCorrectValue()
    {
        // Per API Contract §3.5: PAIRING_ACK = 0x21
        Assert.Equal(0x21, BinaryOpcodes.PairingAck);
    }

    [Fact]
    public void HandAck_HasCorrectValue()
    {
        // Per API Contract §3.4: HAND_ACK = 0x06
        Assert.Equal(0x06, BinaryOpcodes.HandAck);
    }

    [Fact]
    public void DistributeAck_HasCorrectValue()
    {
        // Per API Contract §3.6: DISTRIBUTE_ACK = 0x12
        Assert.Equal(0x12, BinaryOpcodes.DistributeAck);
    }

    [Fact]
    public void OpcodeRanges_AreCorrect()
    {
        // Verify opcode ranges per API Contract §3.2
        // 0x00: UDP Discovery
        Assert.Equal(0x00, BinaryOpcodes.Discovery);

        // 0x01-0x0F: Server → Client Control (TCP)
        Assert.InRange(BinaryOpcodes.LockScreen, 0x01, 0x0F);
        Assert.InRange(BinaryOpcodes.UnlockScreen, 0x01, 0x0F);
        Assert.InRange(BinaryOpcodes.RefreshConfig, 0x01, 0x0F);
        Assert.InRange(BinaryOpcodes.Unpair, 0x01, 0x0F);
        Assert.InRange(BinaryOpcodes.DistributeMaterial, 0x01, 0x0F);
        Assert.InRange(BinaryOpcodes.HandAck, 0x01, 0x0F);

        // 0x10-0x1F: Client → Server Status (TCP)
        Assert.InRange(BinaryOpcodes.StatusUpdate, 0x10, 0x1F);
        Assert.InRange(BinaryOpcodes.HandRaised, 0x10, 0x1F);
        Assert.InRange(BinaryOpcodes.DistributeAck, 0x10, 0x1F);

        // 0x20-0x2F: Pairing (TCP)
        Assert.InRange(BinaryOpcodes.PairingRequest, 0x20, 0x2F);
        Assert.InRange(BinaryOpcodes.PairingAck, 0x20, 0x2F);
    }
}
