using Microsoft.AspNetCore.Mvc;
using Microsoft.Extensions.Logging;
using Moq;
using Main.Controllers;
using Main.Services.Network;
using Xunit;

namespace MainTests.ControllersTests;

/// <summary>
/// Unit tests for ConfigController.
/// Verifies GET /config/{deviceId} endpoint per API Contract.md §2.2.
/// </summary>
public class ConfigControllerTests
{
    private readonly ConfigController _controller;
    private readonly Mock<ILogger<ConfigController>> _mockLogger;
    private readonly Mock<IRefreshConfigTracker> _mockRefreshTracker;

    public ConfigControllerTests()
    {
        _mockLogger = new Mock<ILogger<ConfigController>>();
        _mockRefreshTracker = new Mock<IRefreshConfigTracker>();
        _controller = new ConfigController(_mockLogger.Object, _mockRefreshTracker.Object);
    }

    #region Constructor Tests

    [Fact]
    public void Constructor_NullLogger_ThrowsArgumentNullException()
    {
        // Act & Assert
        Assert.Throws<ArgumentNullException>(() => new ConfigController(null!, _mockRefreshTracker.Object));
    }

    [Fact]
    public void Constructor_NullRefreshTracker_ThrowsArgumentNullException()
    {
        // Act & Assert
        Assert.Throws<ArgumentNullException>(() => new ConfigController(_mockLogger.Object, null!));
    }

    #endregion

    #region GET /api/v1/config/{deviceId} Tests

    [Fact]
    public void GetConfig_Returns200Ok()
    {
        // Arrange
        var deviceId = Guid.NewGuid().ToString();

        // Act
        var result = _controller.GetConfig(deviceId);

        // Assert
        Assert.IsType<OkObjectResult>(result);
    }

    [Fact]
    public void GetConfig_LogsInformation()
    {
        // Arrange
        var deviceId = Guid.NewGuid().ToString();

        // Act
        _controller.GetConfig(deviceId);

        // Assert
        _mockLogger.Verify(
            x => x.Log(
                LogLevel.Information,
                It.IsAny<EventId>(),
                It.Is<It.IsAnyType>((v, t) => v.ToString()!.Contains("Configuration requested")),
                It.IsAny<Exception>(),
                It.IsAny<Func<It.IsAnyType, Exception?, string>>()),
            Times.Once);
    }

    [Fact]
    public void GetConfig_CallsRefreshTrackerMarkConfigReceived()
    {
        // Arrange
        var deviceId = Guid.NewGuid().ToString();

        // Act
        _controller.GetConfig(deviceId);

        // Assert - Verify that MarkConfigReceived was called with the device ID
        _mockRefreshTracker.Verify(
            t => t.MarkConfigReceived(deviceId),
            Times.Once);
    }

    #endregion
}

