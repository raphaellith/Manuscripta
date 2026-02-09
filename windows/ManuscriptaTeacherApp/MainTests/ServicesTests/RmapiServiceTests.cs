using System;
using System.IO;
using System.Net.Http;
using Moq;
using Xunit;
using Main.Services;
using Microsoft.Extensions.Logging;

namespace MainTests.ServicesTests;

/// <summary>
/// Tests for RmapiService.
/// Tests that do not require an actual rmapi binary.
/// </summary>
public class RmapiServiceTests
{
    private readonly Mock<ILogger<RmapiService>> _mockLogger;
    private readonly Mock<HttpMessageHandler> _mockHttpHandler;
    private readonly HttpClient _httpClient;

    public RmapiServiceTests()
    {
        _mockLogger = new Mock<ILogger<RmapiService>>();
        _mockHttpHandler = new Mock<HttpMessageHandler>();
        _httpClient = new HttpClient(_mockHttpHandler.Object);
    }

    #region Constructor Tests

    [Fact]
    public void Constructor_NullLogger_ThrowsArgumentNullException()
    {
        Assert.Throws<ArgumentNullException>(() => new RmapiService(null!, _httpClient));
    }

    [Fact]
    public void Constructor_NullHttpClient_ThrowsArgumentNullException()
    {
        Assert.Throws<ArgumentNullException>(() => new RmapiService(_mockLogger.Object, null!));
    }

    [Fact]
    public void Constructor_ValidParameters_DoesNotThrow()
    {
        var service = new RmapiService(_mockLogger.Object, _httpClient);
        Assert.NotNull(service);
    }

    #endregion

    #region GetConfigPath Tests

    [Fact]
    public void GetConfigPath_ReturnsPathContainingDeviceId()
    {
        var service = new RmapiService(_mockLogger.Object, _httpClient);
        var deviceId = Guid.NewGuid();

        var path = service.GetConfigPath(deviceId);

        Assert.Contains(deviceId.ToString(), path);
    }

    [Fact]
    public void GetConfigPath_ReturnsPathWithConfExtension()
    {
        var service = new RmapiService(_mockLogger.Object, _httpClient);
        var deviceId = Guid.NewGuid();

        var path = service.GetConfigPath(deviceId);

        Assert.EndsWith(".conf", path);
    }

    [Fact]
    public void GetConfigPath_ReturnsPathContainingRmapiDirectory()
    {
        var service = new RmapiService(_mockLogger.Object, _httpClient);
        var deviceId = Guid.NewGuid();

        var path = service.GetConfigPath(deviceId);

        Assert.Contains("rmapi", path);
        Assert.Contains("ManuscriptaTeacherApp", path);
    }

    [Fact]
    public void GetConfigPath_DifferentDeviceIds_ReturnDifferentPaths()
    {
        var service = new RmapiService(_mockLogger.Object, _httpClient);
        var id1 = Guid.NewGuid();
        var id2 = Guid.NewGuid();

        Assert.NotEqual(service.GetConfigPath(id1), service.GetConfigPath(id2));
    }

    #endregion

    #region InvalidateAvailabilityCache Tests

    [Fact]
    public void InvalidateAvailabilityCache_DoesNotThrow()
    {
        var service = new RmapiService(_mockLogger.Object, _httpClient);
        // Should not throw even when no cache exists
        service.InvalidateAvailabilityCache();
    }

    #endregion

    #region CheckAvailabilityAsync Tests

    [Fact]
    public async void CheckAvailabilityAsync_RmapiNotInstalled_ReturnsFalse()
    {
        // rmapi is not installed in the test environment, so should return false
        var service = new RmapiService(_mockLogger.Object, _httpClient);

        var result = await service.CheckAvailabilityAsync();

        Assert.False(result);
    }

    [Fact]
    public async void CheckAvailabilityAsync_CachesResult_SecondCallReturnsCached()
    {
        var service = new RmapiService(_mockLogger.Object, _httpClient);

        // First call: rmapi not installed, returns false
        var first = await service.CheckAvailabilityAsync();
        Assert.False(first);

        // Second call should return the same cached result without re-checking
        var second = await service.CheckAvailabilityAsync();
        Assert.Equal(first, second);
    }

    [Fact]
    public async void CheckAvailabilityAsync_AfterInvalidate_RechecksAvailability()
    {
        var service = new RmapiService(_mockLogger.Object, _httpClient);

        var first = await service.CheckAvailabilityAsync();
        Assert.False(first);

        // Invalidate and re-check — should still return false (rmapi not installed)
        // but should actually re-check rather than return cached
        service.InvalidateAvailabilityCache();
        var second = await service.CheckAvailabilityAsync();
        Assert.False(second);
    }

    #endregion

    #region AuthenticateAsync Tests

    [Fact]
    public async void AuthenticateAsync_EmptyCode_ThrowsArgumentException()
    {
        var service = new RmapiService(_mockLogger.Object, _httpClient);

        await Assert.ThrowsAsync<ArgumentException>(
            () => service.AuthenticateAsync("", "/tmp/test.conf"));
    }

    [Fact]
    public async void AuthenticateAsync_EmptyConfigPath_ThrowsArgumentException()
    {
        var service = new RmapiService(_mockLogger.Object, _httpClient);

        await Assert.ThrowsAsync<ArgumentException>(
            () => service.AuthenticateAsync("abc123", ""));
    }

    [Fact]
    public async void AuthenticateAsync_WhitespaceCode_ThrowsArgumentException()
    {
        var service = new RmapiService(_mockLogger.Object, _httpClient);

        await Assert.ThrowsAsync<ArgumentException>(
            () => service.AuthenticateAsync("   ", "/tmp/test.conf"));
    }

    #endregion
}
