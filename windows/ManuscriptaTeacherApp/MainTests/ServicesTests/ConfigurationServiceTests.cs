using System;
using System.Collections.Generic;
using System.Threading.Tasks;
using Microsoft.Extensions.Logging;
using Moq;
using Xunit;
using Main.Models.Entities;
using Main.Models.Enums;
using Main.Services;
using Main.Services.Network;
using Main.Services.Repositories;

namespace MainTests.ServicesTests;

/// <summary>
/// Tests for ConfigurationService.
/// Validates two-tier config model per ConfigurationManagementSpecification and Validation Rules §2G.
/// </summary>
public class ConfigurationServiceTests
{
    private readonly Mock<IDefaultConfigurationRepository> _mockDefaultsRepo;
    private readonly Mock<IConfigurationOverrideRepository> _mockOverrideRepo;
    private readonly Mock<ITcpPairingService> _mockTcpService;
    private readonly Mock<IDeviceRegistryService> _mockDeviceRegistry;
    private readonly Mock<ILogger<ConfigurationService>> _mockLogger;
    private readonly ConfigurationService _service;

    public ConfigurationServiceTests()
    {
        _mockDefaultsRepo = new Mock<IDefaultConfigurationRepository>();
        _mockOverrideRepo = new Mock<IConfigurationOverrideRepository>();
        _mockTcpService = new Mock<ITcpPairingService>();
        _mockDeviceRegistry = new Mock<IDeviceRegistryService>();
        _mockLogger = new Mock<ILogger<ConfigurationService>>();

        _service = new ConfigurationService(
            _mockDefaultsRepo.Object,
            _mockOverrideRepo.Object,
            _mockTcpService.Object,
            _mockDeviceRegistry.Object,
            _mockLogger.Object);
    }

    #region Constructor Tests

    [Fact]
    public void Constructor_NullDefaultsRepo_ThrowsArgumentNullException()
    {
        Assert.Throws<ArgumentNullException>(() => new ConfigurationService(
            null!, _mockOverrideRepo.Object, _mockTcpService.Object,
            _mockDeviceRegistry.Object, _mockLogger.Object));
    }

    [Fact]
    public void Constructor_NullOverrideRepo_ThrowsArgumentNullException()
    {
        Assert.Throws<ArgumentNullException>(() => new ConfigurationService(
            _mockDefaultsRepo.Object, null!, _mockTcpService.Object,
            _mockDeviceRegistry.Object, _mockLogger.Object));
    }

    [Fact]
    public void Constructor_NullTcpService_ThrowsArgumentNullException()
    {
        Assert.Throws<ArgumentNullException>(() => new ConfigurationService(
            _mockDefaultsRepo.Object, _mockOverrideRepo.Object, null!,
            _mockDeviceRegistry.Object, _mockLogger.Object));
    }

    [Fact]
    public void Constructor_NullDeviceRegistry_ThrowsArgumentNullException()
    {
        Assert.Throws<ArgumentNullException>(() => new ConfigurationService(
            _mockDefaultsRepo.Object, _mockOverrideRepo.Object, _mockTcpService.Object,
            null!, _mockLogger.Object));
    }

    [Fact]
    public void Constructor_NullLogger_ThrowsArgumentNullException()
    {
        Assert.Throws<ArgumentNullException>(() => new ConfigurationService(
            _mockDefaultsRepo.Object, _mockOverrideRepo.Object, _mockTcpService.Object,
            _mockDeviceRegistry.Object, null!));
    }

    #endregion

    #region GetDefaultsAsync Tests

    [Fact]
    public async Task GetDefaultsAsync_DelegatesToRepository()
    {
        // Arrange
        var defaults = ConfigurationEntity.CreateDefault();
        _mockDefaultsRepo.Setup(r => r.GetAsync()).ReturnsAsync(defaults);

        // Act
        var result = await _service.GetDefaultsAsync();

        // Assert
        Assert.Same(defaults, result);
        _mockDefaultsRepo.Verify(r => r.GetAsync(), Times.Once);
    }

    #endregion

    #region UpdateDefaultsAsync Tests

    [Fact]
    public async Task UpdateDefaultsAsync_ValidConfig_UpdatesAndReturns()
    {
        // Arrange
        var config = ConfigurationEntity.CreateDefault();
        _mockDeviceRegistry.Setup(d => d.GetAllAsync())
            .ReturnsAsync(Array.Empty<PairedDeviceEntity>());

        // Act
        var result = await _service.UpdateDefaultsAsync(config);

        // Assert
        Assert.Same(config, result);
        _mockDefaultsRepo.Verify(r => r.UpdateAsync(config), Times.Once);
    }

    [Fact]
    public async Task UpdateDefaultsAsync_NullEntity_ThrowsArgumentNullException()
    {
        await Assert.ThrowsAsync<ArgumentNullException>(() => _service.UpdateDefaultsAsync(null!));
    }

    [Theory]
    [InlineData(4)]
    [InlineData(51)]
    [InlineData(-1)]
    [InlineData(0)]
    public async Task UpdateDefaultsAsync_InvalidTextSize_ThrowsArgumentException(int textSize)
    {
        // Arrange — §2G(1)(a): TextSize must be between 5 and 50
        var config = ConfigurationEntity.CreateDefault();
        config.TextSize = textSize;

        // Act & Assert
        await Assert.ThrowsAsync<ArgumentException>(() => _service.UpdateDefaultsAsync(config));
        _mockDefaultsRepo.Verify(r => r.UpdateAsync(It.IsAny<ConfigurationEntity>()), Times.Never);
    }

    [Theory]
    [InlineData(5)]   // Lower boundary
    [InlineData(50)]  // Upper boundary
    public async Task UpdateDefaultsAsync_BoundaryTextSize_Succeeds(int textSize)
    {
        // Arrange
        var config = ConfigurationEntity.CreateDefault();
        config.TextSize = textSize;
        _mockDeviceRegistry.Setup(d => d.GetAllAsync())
            .ReturnsAsync(Array.Empty<PairedDeviceEntity>());

        // Act
        var result = await _service.UpdateDefaultsAsync(config);

        // Assert
        Assert.Equal(textSize, result.TextSize);
    }

    [Fact]
    public async Task UpdateDefaultsAsync_InvalidFeedbackStyle_ThrowsArgumentException()
    {
        // Arrange — §2G(2)(a)
        var config = ConfigurationEntity.CreateDefault();
        config.FeedbackStyle = (FeedbackStyle)99;

        // Act & Assert
        await Assert.ThrowsAsync<ArgumentException>(() => _service.UpdateDefaultsAsync(config));
    }

    [Fact]
    public async Task UpdateDefaultsAsync_InvalidMascotSelection_ThrowsArgumentException()
    {
        // Arrange — §2G(2)(a)
        var config = ConfigurationEntity.CreateDefault();
        config.MascotSelection = (MascotSelection)99;

        // Act & Assert
        await Assert.ThrowsAsync<ArgumentException>(() => _service.UpdateDefaultsAsync(config));
    }

    #endregion

    #region GetOverride Tests

    [Fact]
    public void GetOverride_ReturnsOverrideWhenExists()
    {
        // Arrange
        var deviceId = Guid.NewGuid();
        var expected = new ConfigurationOverride { TextSize = 30 };
        _mockOverrideRepo.Setup(r => r.GetByDeviceId(deviceId)).Returns(expected);

        // Act
        var result = _service.GetOverride(deviceId);

        // Assert
        Assert.Same(expected, result);
        _mockOverrideRepo.Verify(r => r.GetByDeviceId(deviceId), Times.Once);
    }

    [Fact]
    public void GetOverride_ReturnsNullWhenNoOverrideSet()
    {
        // Arrange
        var deviceId = Guid.NewGuid();
        _mockOverrideRepo.Setup(r => r.GetByDeviceId(deviceId)).Returns((ConfigurationOverride?)null);

        // Act
        var result = _service.GetOverride(deviceId);

        // Assert
        Assert.Null(result);
    }

    #endregion

    #region SetOverride Tests

    [Fact]
    public void SetOverride_ValidOverride_DelegatesToRepository()
    {
        // Arrange
        var deviceId = Guid.NewGuid();
        var overrides = new ConfigurationOverride { TextSize = 20 };

        // Act
        _service.SetOverride(deviceId, overrides);

        // Assert
        _mockOverrideRepo.Verify(r => r.Set(deviceId, overrides), Times.Once);
    }

    [Fact]
    public void SetOverride_NullOverride_ThrowsArgumentNullException()
    {
        Assert.Throws<ArgumentNullException>(() => _service.SetOverride(Guid.NewGuid(), null!));
    }

    [Fact]
    public void SetOverride_InvalidTextSize_ThrowsArgumentException()
    {
        // Arrange — §2G(1)(a)
        var overrides = new ConfigurationOverride { TextSize = 51 };

        // Act & Assert
        Assert.Throws<ArgumentException>(() => _service.SetOverride(Guid.NewGuid(), overrides));
        _mockOverrideRepo.Verify(r => r.Set(It.IsAny<Guid>(), It.IsAny<ConfigurationOverride>()), Times.Never);
    }

    [Fact]
    public void SetOverride_InvalidFeedbackStyle_ThrowsArgumentException()
    {
        // Arrange — §2G(2)(a)
        var overrides = new ConfigurationOverride { FeedbackStyle = (FeedbackStyle)99 };

        // Act & Assert
        Assert.Throws<ArgumentException>(() => _service.SetOverride(Guid.NewGuid(), overrides));
    }

    [Fact]
    public void SetOverride_InvalidMascotSelection_ThrowsArgumentException()
    {
        // Arrange — §2G(2)(a)
        var overrides = new ConfigurationOverride { MascotSelection = (MascotSelection)99 };

        // Act & Assert
        Assert.Throws<ArgumentException>(() => _service.SetOverride(Guid.NewGuid(), overrides));
    }

    [Fact]
    public void SetOverride_NullFieldsAreValid()
    {
        // Arrange — null fields mean "use default", not validated
        var deviceId = Guid.NewGuid();
        var overrides = new ConfigurationOverride(); // All null

        // Act — should not throw
        _service.SetOverride(deviceId, overrides);

        // Assert
        _mockOverrideRepo.Verify(r => r.Set(deviceId, overrides), Times.Once);
    }

    #endregion

    #region CompileConfigAsync Tests — §2(2)(b) merge logic

    [Fact]
    public async Task CompileConfigAsync_NoOverrides_ReturnsDefaults()
    {
        // Arrange
        var deviceId = Guid.NewGuid();
        var defaults = ConfigurationEntity.CreateDefault();
        _mockDefaultsRepo.Setup(r => r.GetAsync()).ReturnsAsync(defaults);
        _mockOverrideRepo.Setup(r => r.GetByDeviceId(deviceId)).Returns((ConfigurationOverride?)null);

        // Act
        var result = await _service.CompileConfigAsync(deviceId);

        // Assert — same object since no overrides
        Assert.Same(defaults, result);
    }

    [Fact]
    public async Task CompileConfigAsync_EmptyOverrides_ReturnsDefaults()
    {
        // Arrange
        var deviceId = Guid.NewGuid();
        var defaults = ConfigurationEntity.CreateDefault();
        _mockDefaultsRepo.Setup(r => r.GetAsync()).ReturnsAsync(defaults);
        _mockOverrideRepo.Setup(r => r.GetByDeviceId(deviceId))
            .Returns(new ConfigurationOverride()); // All null = IsEmpty

        // Act
        var result = await _service.CompileConfigAsync(deviceId);

        // Assert — same object since overrides are empty
        Assert.Same(defaults, result);
    }

    [Fact]
    public async Task CompileConfigAsync_PartialOverrides_MergesCorrectly()
    {
        // Arrange — only TextSize and TtsEnabled overridden
        var deviceId = Guid.NewGuid();
        var defaults = ConfigurationEntity.CreateDefault();
        _mockDefaultsRepo.Setup(r => r.GetAsync()).ReturnsAsync(defaults);
        _mockOverrideRepo.Setup(r => r.GetByDeviceId(deviceId))
            .Returns(new ConfigurationOverride { TextSize = 30, TtsEnabled = false });

        // Act
        var result = await _service.CompileConfigAsync(deviceId);

        // Assert — overridden fields
        Assert.Equal(30, result.TextSize);
        Assert.False(result.TtsEnabled);
        // Assert — non-overridden fields use defaults
        Assert.Equal(defaults.FeedbackStyle, result.FeedbackStyle);
        Assert.Equal(defaults.AiScaffoldingEnabled, result.AiScaffoldingEnabled);
        Assert.Equal(defaults.SummarisationEnabled, result.SummarisationEnabled);
        Assert.Equal(defaults.MascotSelection, result.MascotSelection);
    }

    [Fact]
    public async Task CompileConfigAsync_FullOverrides_AllFieldsOverridden()
    {
        // Arrange — all fields overridden
        var deviceId = Guid.NewGuid();
        var defaults = ConfigurationEntity.CreateDefault();
        _mockDefaultsRepo.Setup(r => r.GetAsync()).ReturnsAsync(defaults);
        _mockOverrideRepo.Setup(r => r.GetByDeviceId(deviceId))
            .Returns(new ConfigurationOverride
            {
                TextSize = 40,
                FeedbackStyle = FeedbackStyle.NEUTRAL,
                TtsEnabled = false,
                AiScaffoldingEnabled = false,
                SummarisationEnabled = false,
                MascotSelection = MascotSelection.MASCOT5
            });

        // Act
        var result = await _service.CompileConfigAsync(deviceId);

        // Assert
        Assert.Equal(40, result.TextSize);
        Assert.Equal(FeedbackStyle.NEUTRAL, result.FeedbackStyle);
        Assert.False(result.TtsEnabled);
        Assert.False(result.AiScaffoldingEnabled);
        Assert.False(result.SummarisationEnabled);
        Assert.Equal(MascotSelection.MASCOT5, result.MascotSelection);
    }

    #endregion

    #region §3 Config Refresh Trigger Tests

    [Fact]
    public async Task UpdateDefaultsAsync_RefreshesAllPairedDevices()
    {
        // Arrange — §3(1)(b): refresh all devices on default change
        var device1 = new PairedDeviceEntity(Guid.NewGuid(), "Device1");
        var device2 = new PairedDeviceEntity(Guid.NewGuid(), "Device2");
        _mockDeviceRegistry.Setup(d => d.GetAllAsync())
            .ReturnsAsync(new[] { device1, device2 });

        var config = ConfigurationEntity.CreateDefault();

        // Act
        await _service.UpdateDefaultsAsync(config);

        // Assert — refresh sent to both devices
        _mockTcpService.Verify(
            t => t.SendRefreshConfigAsync(device1.DeviceId.ToString()), Times.Once);
        _mockTcpService.Verify(
            t => t.SendRefreshConfigAsync(device2.DeviceId.ToString()), Times.Once);
    }

    [Fact]
    public void SetOverride_TriggersRefreshForTargetDevice()
    {
        // Arrange — §3(1)(c): refresh target device on override change
        var deviceId = Guid.NewGuid();
        var overrides = new ConfigurationOverride { TextSize = 20 };

        // Act
        _service.SetOverride(deviceId, overrides);

        // Assert
        _mockTcpService.Verify(
            t => t.SendRefreshConfigAsync(deviceId.ToString()), Times.Once);
    }

    #endregion

    #region RemoveOverride Tests

    [Fact]
    public void RemoveOverride_DelegatesToRepository()
    {
        // Arrange
        var deviceId = Guid.NewGuid();

        // Act
        _service.RemoveOverride(deviceId);

        // Assert
        _mockOverrideRepo.Verify(r => r.Remove(deviceId), Times.Once);
    }

    [Fact]
    public void RemoveOverride_TriggersRefreshForDevice()
    {
        // Arrange — §3(1)(c): removing overrides changes effective config
        var deviceId = Guid.NewGuid();

        // Act
        _service.RemoveOverride(deviceId);

        // Assert
        _mockTcpService.Verify(
            t => t.SendRefreshConfigAsync(deviceId.ToString()), Times.Once);
    }

    #endregion
}
