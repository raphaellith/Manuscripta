using Microsoft.AspNetCore.Mvc;
using Microsoft.Extensions.Logging;
using Moq;
using Main.Controllers;
using Xunit;

namespace MainTests.ControllersTests;

/// <summary>
/// Unit tests for ConfigController.
/// Verifies GET /config endpoint per API Contract.md §2.2.
/// </summary>
public class ConfigControllerTests
{
    private readonly ConfigController _controller;
    private readonly Mock<ILogger<ConfigController>> _mockLogger;

    public ConfigControllerTests()
    {
        _mockLogger = new Mock<ILogger<ConfigController>>();
        _controller = new ConfigController(_mockLogger.Object);
    }

    #region Constructor Tests

    [Fact]
    public void Constructor_NullLogger_ThrowsArgumentNullException()
    {
        // Act & Assert
        Assert.Throws<ArgumentNullException>(() => new ConfigController(null!));
    }

    #endregion

    #region GET /api/v1/config Tests

    [Fact]
    public void GetConfig_Returns200Ok()
    {
        // Act
        var result = _controller.GetConfig();

        // Assert
        Assert.IsType<OkObjectResult>(result);
    }

    [Fact]
    public void GetConfig_LogsInformation()
    {
        // Act
        _controller.GetConfig();

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

    #endregion
}
