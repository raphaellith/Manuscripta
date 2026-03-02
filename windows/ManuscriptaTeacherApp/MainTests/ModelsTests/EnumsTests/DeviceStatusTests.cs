using Main.Models.Enums;
using Xunit;

namespace MainTests.ModelsTests.EnumsTests;

/// <summary>
/// Tests for DeviceStatus enum.
/// Verifies enum values match Validation Rules.md §2E(1)(b).
/// </summary>
public class DeviceStatusTests
{
    [Fact]
    public void DeviceStatus_HasFourValues()
    {
        // Per Validation Rules §2E(1)(b) and API Contract §4.3: 4 possible status values
        // (ON_TASK, IDLE, LOCKED, DISCONNECTED - HAND_RAISED is not a device status)
        var values = Enum.GetValues<DeviceStatus>();
        Assert.Equal(4, values.Length);
    }

    [Fact]
    public void DeviceStatus_ContainsOnTask()
    {
        // Per §2E(1)(b)(i): ON_TASK - Student is active in the app
        Assert.True(Enum.IsDefined(typeof(DeviceStatus), DeviceStatus.ON_TASK));
    }

    [Fact]
    public void DeviceStatus_ContainsIdle()
    {
        // Per §2E(1)(b)(ii): IDLE - No activity for a threshold period
        Assert.True(Enum.IsDefined(typeof(DeviceStatus), DeviceStatus.IDLE));
    }



    [Fact]
    public void DeviceStatus_ContainsLocked()
    {
        // Per §2E(1)(b)(iv): LOCKED - Device is remotely locked
        Assert.True(Enum.IsDefined(typeof(DeviceStatus), DeviceStatus.LOCKED));
    }

    [Fact]
    public void DeviceStatus_ContainsDisconnected()
    {
        // Per §2E(1)(b)(v): DISCONNECTED - Server-side inferred status
        Assert.True(Enum.IsDefined(typeof(DeviceStatus), DeviceStatus.DISCONNECTED));
    }

    [Theory]
    [InlineData(DeviceStatus.ON_TASK, "ON_TASK")]
    [InlineData(DeviceStatus.IDLE, "IDLE")]
    [InlineData(DeviceStatus.LOCKED, "LOCKED")]
    [InlineData(DeviceStatus.DISCONNECTED, "DISCONNECTED")]
    public void DeviceStatus_NamesMatchExpected(DeviceStatus status, string expectedName)
    {
        Assert.Equal(expectedName, status.ToString());
    }
}
