using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;
using Microsoft.Extensions.Logging;
using Moq;
using Main.Controllers;
using Main.Data;
using Main.Models.Dtos;
using Main.Models.Entities;
using Main.Services;
using Xunit;

namespace MainTests.ControllersTests;

/// <summary>
/// Unit tests for PairingController.
/// Verifies HTTP POST /pair endpoint per API Contract.md §2.4 and Pairing Process.md §2(3)(b).
/// </summary>
public class PairingControllerTests : IDisposable
{
    private readonly MainDbContext _context;
    private readonly DeviceRegistryService _deviceRegistry;
    private readonly PairingController _controller;
    private readonly Mock<ILogger<DeviceRegistryService>> _mockRegistryLogger;
    private readonly Mock<ILogger<PairingController>> _mockControllerLogger;

    public PairingControllerTests()
    {
        var options = new DbContextOptionsBuilder<MainDbContext>()
            .UseInMemoryDatabase(databaseName: $"TestDb_{Guid.NewGuid()}")
            .Options;

        _context = new MainDbContext(options);
        _mockRegistryLogger = new Mock<ILogger<DeviceRegistryService>>();
        _mockControllerLogger = new Mock<ILogger<PairingController>>();
        
        _deviceRegistry = new DeviceRegistryService(_context, _mockRegistryLogger.Object);
        _controller = new PairingController(_deviceRegistry, _mockControllerLogger.Object);
    }

    public void Dispose()
    {
        _context.Database.EnsureDeleted();
        _context.Dispose();
    }

    #region POST /api/v1/pair Tests

    [Fact]
    public async Task Pair_ValidDeviceId_Returns201StatusCode()
    {
        // Arrange
        var deviceId = Guid.NewGuid();
        var request = new PairRequest { DeviceId = deviceId.ToString() };

        // Act
        var result = await _controller.Pair(request);

        // Assert
        var objectResult = Assert.IsType<ObjectResult>(result);
        Assert.Equal(201, objectResult.StatusCode);
    }

    [Fact]
    public async Task Pair_ValidDeviceId_RegistersDevice()
    {
        // Arrange
        var deviceId = Guid.NewGuid();
        var request = new PairRequest { DeviceId = deviceId.ToString() };

        // Act
        await _controller.Pair(request);

        // Assert
        var device = await _context.PairedDevices.FindAsync(deviceId);
        Assert.NotNull(device);
        Assert.Equal(deviceId, device.DeviceId);
    }

    [Fact]
    public async Task Pair_AlreadyPairedDevice_Returns409Conflict()
    {
        // Arrange - First, pair the device
        var deviceId = Guid.NewGuid();
        var request = new PairRequest { DeviceId = deviceId.ToString() };
        await _controller.Pair(request);

        // Act - Try to pair again
        var result = await _controller.Pair(request);

        // Assert
        Assert.IsType<ConflictObjectResult>(result);
    }

    [Fact]
    public async Task Pair_AlreadyPairedDevice_ReturnsErrorMessage()
    {
        // Arrange
        var deviceId = Guid.NewGuid();
        var request = new PairRequest { DeviceId = deviceId.ToString() };
        await _controller.Pair(request);

        // Act
        var result = await _controller.Pair(request) as ConflictObjectResult;

        // Assert
        Assert.NotNull(result);
        var value = result.Value?.ToString();
        Assert.Contains("already paired", value, StringComparison.OrdinalIgnoreCase);
    }

    [Fact]
    public async Task Pair_MissingDeviceId_Returns400BadRequest()
    {
        // Arrange
        var request = new PairRequest { DeviceId = null! };

        // Act
        var result = await _controller.Pair(request);

        // Assert
        Assert.IsType<BadRequestObjectResult>(result);
    }

    [Fact]
    public async Task Pair_EmptyDeviceId_Returns400BadRequest()
    {
        // Arrange
        var request = new PairRequest { DeviceId = "" };

        // Act
        var result = await _controller.Pair(request);

        // Assert
        Assert.IsType<BadRequestObjectResult>(result);
    }

    [Fact]
    public async Task Pair_WhitespaceDeviceId_Returns400BadRequest()
    {
        // Arrange
        var request = new PairRequest { DeviceId = "   " };

        // Act
        var result = await _controller.Pair(request);

        // Assert
        Assert.IsType<BadRequestObjectResult>(result);
    }

    [Fact]
    public async Task Pair_InvalidGuidFormat_Returns400BadRequest()
    {
        // Arrange
        var request = new PairRequest { DeviceId = "not-a-valid-guid" };

        // Act
        var result = await _controller.Pair(request);

        // Assert
        Assert.IsType<BadRequestObjectResult>(result);
    }

    [Fact]
    public async Task Pair_InvalidGuidFormat_ReturnsErrorMessage()
    {
        // Arrange
        var request = new PairRequest { DeviceId = "invalid-guid-format" };

        // Act
        var result = await _controller.Pair(request) as BadRequestObjectResult;

        // Assert
        Assert.NotNull(result);
        var value = result.Value?.ToString();
        Assert.Contains("GUID", value, StringComparison.OrdinalIgnoreCase);
    }

    [Fact]
    public async Task Pair_MultipleDifferentDevices_AllSucceed()
    {
        // Arrange
        var deviceId1 = Guid.NewGuid();
        var deviceId2 = Guid.NewGuid();
        var deviceId3 = Guid.NewGuid();

        // Act
        var result1 = await _controller.Pair(new PairRequest { DeviceId = deviceId1.ToString() });
        var result2 = await _controller.Pair(new PairRequest { DeviceId = deviceId2.ToString() });
        var result3 = await _controller.Pair(new PairRequest { DeviceId = deviceId3.ToString() });

        // Assert
        Assert.IsType<ObjectResult>(result1);
        Assert.IsType<ObjectResult>(result2);
        Assert.IsType<ObjectResult>(result3);

        var count = await _context.PairedDevices.CountAsync();
        Assert.Equal(3, count);
    }

    [Fact]
    public async Task Pair_SameDeviceTwice_OnlyFirstSucceeds()
    {
        // Arrange
        var deviceId = Guid.NewGuid();

        // Act
        var result1 = await _controller.Pair(new PairRequest { DeviceId = deviceId.ToString() });
        var result2 = await _controller.Pair(new PairRequest { DeviceId = deviceId.ToString() });

        // Assert
        var objectResult1 = Assert.IsType<ObjectResult>(result1);
        Assert.Equal(201, objectResult1.StatusCode);
        Assert.IsType<ConflictObjectResult>(result2);
    }

    #endregion
}
