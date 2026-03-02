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
    private readonly string _testRmapiPath;

    public RmapiServiceTests()
    {
        _mockLogger = new Mock<ILogger<RmapiService>>();
        _mockHttpHandler = new Mock<HttpMessageHandler>();
        _httpClient = new HttpClient(_mockHttpHandler.Object);
        _testRmapiPath = Path.Combine(Path.GetTempPath(), $"rmapi-test-{Guid.NewGuid():N}.exe");
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
        var service = new RmapiService(_mockLogger.Object, _httpClient, _testRmapiPath);
        Assert.NotNull(service);
    }

    #endregion

    #region GetConfigPath Tests

    [Fact]
    public void GetConfigPath_ReturnsPathContainingDeviceId()
    {
        var service = new RmapiService(_mockLogger.Object, _httpClient, _testRmapiPath);
        var deviceId = Guid.NewGuid();

        var path = service.GetConfigPath(deviceId);

        Assert.Contains(deviceId.ToString(), path);
    }

    [Fact]
    public void GetConfigPath_ReturnsPathWithConfExtension()
    {
        var service = new RmapiService(_mockLogger.Object, _httpClient, _testRmapiPath);
        var deviceId = Guid.NewGuid();

        var path = service.GetConfigPath(deviceId);

        Assert.EndsWith(".conf", path);
    }

    [Fact]
    public void GetConfigPath_ReturnsPathContainingRmapiDirectory()
    {
        var service = new RmapiService(_mockLogger.Object, _httpClient, _testRmapiPath);
        var deviceId = Guid.NewGuid();

        var path = service.GetConfigPath(deviceId);

        Assert.Contains("rmapi", path);
        Assert.Contains("ManuscriptaTeacherApp", path);
    }

    [Fact]
    public void GetConfigPath_DifferentDeviceIds_ReturnDifferentPaths()
    {
        var service = new RmapiService(_mockLogger.Object, _httpClient, _testRmapiPath);
        var id1 = Guid.NewGuid();
        var id2 = Guid.NewGuid();

        Assert.NotEqual(service.GetConfigPath(id1), service.GetConfigPath(id2));
    }

    #endregion

    #region CheckAvailabilityAsync Tests

    [Fact]
    public async Task CheckAvailabilityAsync_RmapiNotInstalled_ReturnsFalse()
    {
        // rmapi is not installed in the test environment, so should return false
        if (File.Exists(_testRmapiPath))
        {
            File.Delete(_testRmapiPath);
        }
        var service = new RmapiService(_mockLogger.Object, _httpClient, _testRmapiPath);

        var result = await service.CheckAvailabilityAsync();

        Assert.False(result);
    }

    #endregion

    #region AuthenticateAsync Tests

    [Fact]
    public async Task AuthenticateAsync_EmptyCode_ThrowsArgumentException()
    {
        var service = new RmapiService(_mockLogger.Object, _httpClient, _testRmapiPath);

        await Assert.ThrowsAsync<ArgumentException>(
            () => service.AuthenticateAsync("", "/tmp/test.conf"));
    }

    [Fact]
    public async Task AuthenticateAsync_EmptyConfigPath_ThrowsArgumentException()
    {
        var service = new RmapiService(_mockLogger.Object, _httpClient, _testRmapiPath);

        await Assert.ThrowsAsync<ArgumentException>(
            () => service.AuthenticateAsync("abc123", ""));
    }

    [Fact]
    public async Task AuthenticateAsync_WhitespaceCode_ThrowsArgumentException()
    {
        var service = new RmapiService(_mockLogger.Object, _httpClient, _testRmapiPath);

        await Assert.ThrowsAsync<ArgumentException>(
            () => service.AuthenticateAsync("   ", "/tmp/test.conf"));
    }

    #endregion
}
