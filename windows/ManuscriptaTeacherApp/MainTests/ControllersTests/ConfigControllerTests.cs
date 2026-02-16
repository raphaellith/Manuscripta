using System;
using System.Threading.Tasks;
using Microsoft.AspNetCore.Mvc;
using Microsoft.Extensions.Logging;
using Moq;
using Main.Controllers;
using Main.Models.Entities;
using Main.Models.Enums;
using Main.Services;
using Main.Services.Network;
using Xunit;

namespace MainTests.ControllersTests;

/// <summary>
/// Unit tests for ConfigController.
/// Verifies GET /config/{deviceId} endpoint per API Contract.md §2.2.
/// Per ConfigurationManagementSpecification §2(2)(b): returns compiled config.
/// </summary>
public class ConfigControllerTests
{
    private readonly ConfigController _controller;
    private readonly Mock<ILogger<ConfigController>> _mockLogger;
    private readonly Mock<IRefreshConfigTracker> _mockRefreshTracker;
    private readonly Mock<IConfigurationService> _mockConfigService;
    private readonly Mock<IDeviceRegistryService> _mockDeviceRegistry;

    public ConfigControllerTests()
    {
        _mockLogger = new Mock<ILogger<ConfigController>>();
        _mockRefreshTracker = new Mock<IRefreshConfigTracker>();
        _mockConfigService = new Mock<IConfigurationService>();
        _mockDeviceRegistry = new Mock<IDeviceRegistryService>();
        _controller = new ConfigController(
            _mockLogger.Object,
            _mockRefreshTracker.Object,
            _mockConfigService.Object,
            _mockDeviceRegistry.Object);
    }

    #region Constructor Tests

    [Fact]
    public void Constructor_NullLogger_ThrowsArgumentNullException()
    {
        Assert.Throws<ArgumentNullException>(() => new ConfigController(
            null!, _mockRefreshTracker.Object, _mockConfigService.Object, _mockDeviceRegistry.Object));
    }

    [Fact]
    public void Constructor_NullRefreshTracker_ThrowsArgumentNullException()
    {
        Assert.Throws<ArgumentNullException>(() => new ConfigController(
            _mockLogger.Object, null!, _mockConfigService.Object, _mockDeviceRegistry.Object));
    }

    [Fact]
    public void Constructor_NullConfigurationService_ThrowsArgumentNullException()
    {
        Assert.Throws<ArgumentNullException>(() => new ConfigController(
            _mockLogger.Object, _mockRefreshTracker.Object, null!, _mockDeviceRegistry.Object));
    }

    [Fact]
    public void Constructor_NullDeviceRegistryService_ThrowsArgumentNullException()
    {
        Assert.Throws<ArgumentNullException>(() => new ConfigController(
            _mockLogger.Object, _mockRefreshTracker.Object, _mockConfigService.Object, null!));
    }

    #endregion

    #region GET /api/v1/config/{deviceId} Tests

    [Fact]
    public async Task GetConfig_ValidPairedDevice_ReturnsCompiledConfig()
    {
        // Arrange
        var deviceId = Guid.NewGuid();
        var compiledConfig = new ConfigurationEntity(
            id: ConfigurationEntity.DefaultId,
            textSize: 20,
            feedbackStyle: FeedbackStyle.NEUTRAL,
            ttsEnabled: false,
            aiScaffoldingEnabled: true,
            summarisationEnabled: false,
            mascotSelection: MascotSelection.MASCOT3);

        _mockDeviceRegistry.Setup(d => d.IsDevicePairedAsync(deviceId)).ReturnsAsync(true);
        _mockConfigService.Setup(s => s.CompileConfigAsync(deviceId))
            .ReturnsAsync(compiledConfig);

        // Act
        var result = await _controller.GetConfig(deviceId.ToString());

        // Assert
        var okResult = Assert.IsType<OkObjectResult>(result);
        Assert.Same(compiledConfig, okResult.Value);
        _mockConfigService.Verify(s => s.CompileConfigAsync(deviceId), Times.Once);
    }

    [Fact]
    public async Task GetConfig_InvalidGuidFormat_Returns400BadRequest()
    {
        // Act
        var result = await _controller.GetConfig("not-a-guid");

        // Assert — invalid device IDs are rejected without revealing config
        Assert.IsType<BadRequestObjectResult>(result);
        _mockConfigService.Verify(s => s.CompileConfigAsync(It.IsAny<Guid>()), Times.Never);
        _mockDeviceRegistry.Verify(d => d.IsDevicePairedAsync(It.IsAny<Guid>()), Times.Never);
    }

    [Fact]
    public async Task GetConfig_UnpairedDevice_Returns403Forbidden()
    {
        // Arrange — per Pairing Process §1(3): reject unpaired devices
        var deviceId = Guid.NewGuid();
        _mockDeviceRegistry.Setup(d => d.IsDevicePairedAsync(deviceId)).ReturnsAsync(false);

        // Act
        var result = await _controller.GetConfig(deviceId.ToString());

        // Assert
        var objectResult = Assert.IsType<ObjectResult>(result);
        Assert.Equal(403, objectResult.StatusCode);
        _mockConfigService.Verify(s => s.CompileConfigAsync(It.IsAny<Guid>()), Times.Never);
        _mockRefreshTracker.Verify(t => t.MarkConfigReceived(It.IsAny<string>()), Times.Never);
    }

    [Fact]
    public async Task GetConfig_PairedDevice_CallsRefreshTracker()
    {
        // Arrange
        var deviceId = Guid.NewGuid();
        _mockDeviceRegistry.Setup(d => d.IsDevicePairedAsync(deviceId)).ReturnsAsync(true);
        _mockConfigService.Setup(s => s.CompileConfigAsync(deviceId))
            .ReturnsAsync(ConfigurationEntity.CreateDefault());

        // Act
        await _controller.GetConfig(deviceId.ToString());

        // Assert
        _mockRefreshTracker.Verify(
            t => t.MarkConfigReceived(deviceId.ToString()),
            Times.Once);
    }

    [Fact]
    public async Task GetConfig_LogsInformation()
    {
        // Arrange
        var deviceId = Guid.NewGuid();
        _mockDeviceRegistry.Setup(d => d.IsDevicePairedAsync(deviceId)).ReturnsAsync(true);
        _mockConfigService.Setup(s => s.CompileConfigAsync(deviceId))
            .ReturnsAsync(ConfigurationEntity.CreateDefault());

        // Act
        await _controller.GetConfig(deviceId.ToString());

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
