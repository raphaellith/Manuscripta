using Microsoft.Extensions.DependencyInjection;
using Microsoft.Extensions.Options;
using Main.Services;
using Main.Services.Network;
using Xunit;

namespace MainTests.ControllersTests;

/// <summary>
/// Integration tests for API port configuration.
/// Verifies HTTP REST API is configured per API Contract.md §Ports.
/// 
/// Per FrontendWorkflowSpecifications §2ZA(8)(b) and API Contract.md:
/// - SignalR Port: 5910 (frontend communication)
/// - HTTP Port: 5911 (REST API for material distribution and responses)
/// - TCP Port: 5912 (Control signals)
/// - UDP Port: 5913 (Device discovery)
/// </summary>
public class ApiPortConfigurationTests : IClassFixture<TestWebApplicationFactory>
{
    private readonly TestWebApplicationFactory _factory;

    public ApiPortConfigurationTests(TestWebApplicationFactory factory)
    {
        _factory = factory;
    }

    #region Network Settings Integration Tests

    /// <summary>
    /// Verifies NetworkSettings is registered in DI container with correct SignalRPort.
    /// Per FrontendWorkflowSpecifications §2ZA(8)(b): SignalR Port = 5910 (default/preferred).
    /// Note: Actual port may differ due to dynamic port selection; this verifies the default.
    /// </summary>
    [Fact]
    public void NetworkSettings_SignalRPort_Is5910()
    {
        // Arrange
        using var scope = _factory.Services.CreateScope();
        var options = scope.ServiceProvider.GetRequiredService<IOptions<NetworkSettings>>();

        // Assert
        Assert.Equal(5910, options.Value.SignalRPort);
    }

    /// <summary>
    /// Verifies NetworkSettings is registered in DI container with correct HttpPort.
    /// Per API Contract.md §Ports: HTTP Port = 5911.
    /// </summary>
    [Fact]
    public void NetworkSettings_HttpPort_Is5911()
    {
        // Arrange
        using var scope = _factory.Services.CreateScope();
        var options = scope.ServiceProvider.GetRequiredService<IOptions<NetworkSettings>>();

        // Assert
        Assert.Equal(5911, options.Value.HttpPort);
    }

    /// <summary>
    /// Verifies NetworkSettings is registered in DI container with correct TcpPort.
    /// Per API Contract.md §Ports: TCP Port = 5912.
    /// </summary>
    [Fact]
    public void NetworkSettings_TcpPort_Is5912()
    {
        // Arrange
        using var scope = _factory.Services.CreateScope();
        var options = scope.ServiceProvider.GetRequiredService<IOptions<NetworkSettings>>();

        // Assert
        Assert.Equal(5912, options.Value.TcpPort);
    }

    /// <summary>
    /// Verifies NetworkSettings is registered in DI container with correct UdpBroadcastPort.
    /// Per API Contract.md §Ports: UDP Port = 5913.
    /// </summary>
    [Fact]
    public void NetworkSettings_UdpBroadcastPort_Is5913()
    {
        // Arrange
        using var scope = _factory.Services.CreateScope();
        var options = scope.ServiceProvider.GetRequiredService<IOptions<NetworkSettings>>();

        // Assert
        Assert.Equal(5913, options.Value.UdpBroadcastPort);
    }

    #endregion

    #region REST API Endpoint Accessibility Tests

    /// <summary>
    /// Verifies the health endpoint is accessible.
    /// This confirms REST API routing is working.
    /// </summary>
    [Fact]
    public async Task HealthEndpoint_ReturnsOk()
    {
        // Arrange
        using var client = _factory.CreateClient();

        // Act
        var response = await client.GetAsync("/");

        // Assert
        response.EnsureSuccessStatusCode();
    }

    /// <summary>
    /// Verifies the config endpoint returns proper response.
    /// Per API Contract.md §2.2: GET /api/v2/config/{deviceId}
    /// </summary>
    [Fact]
    public async Task ConfigEndpoint_ReturnsOk()
    {
        // Arrange
        using var client = _factory.CreateClient();
        var deviceId = Guid.NewGuid();

        // Register the device so the request is not rejected
        using (var scope = _factory.Services.CreateScope())
        {
            var deviceRegistry = scope.ServiceProvider.GetRequiredService<IDeviceRegistryService>();
            await deviceRegistry.RegisterDeviceAsync(deviceId, "Test Device");
        }

        // Act
        var response = await client.GetAsync($"/api/v1/config/{deviceId}");

        // Assert
        response.EnsureSuccessStatusCode();
    }

    /// <summary>
    /// Verifies the pairing endpoint is accessible.
    /// Per API Contract.md §2.4: POST /api/v1/pair
    /// </summary>
    [Fact]
    public async Task PairingEndpoint_IsAccessible()
    {
        // Arrange
        using var client = _factory.CreateClient();

        // Act
        var response = await client.PostAsync("/api/v1/pair",
            new StringContent("{}", System.Text.Encoding.UTF8, "application/json"));

        // Assert - May return 400 due to invalid body, but endpoint should be accessible
        // 400 = endpoint found, bad request; 404 = endpoint not found
        Assert.NotEqual(System.Net.HttpStatusCode.NotFound, response.StatusCode);
    }

    #endregion

    #region Port Separation Verification

    /// <summary>
    /// Verifies SignalRPort (default) differs from HttpPort (enforced).
    /// Per FrontendWorkflowSpecifications §2ZA(8): SignalR on dynamic port (default 5910)
    /// Per API Contract.md §Ports: REST API on fixed 5911
    /// </summary>
    [Fact]
    public void PortConfiguration_HttpApiSeparateFromSignalR()
    {
        // Arrange
        using var scope = _factory.Services.CreateScope();
        var options = scope.ServiceProvider.GetRequiredService<IOptions<NetworkSettings>>();

        // Assert - HTTP API port should NOT be SignalR port
        Assert.NotEqual(options.Value.SignalRPort, options.Value.HttpPort);
        Assert.Equal(5910, options.Value.SignalRPort);
        Assert.Equal(5911, options.Value.HttpPort);
    }

    /// <summary>
    /// Verifies all four ports (SignalR, HTTP, TCP, UDP) are distinct.
    /// </summary>
    [Fact]
    public void PortConfiguration_AllPortsAreDistinct()
    {
        // Arrange
        using var scope = _factory.Services.CreateScope();
        var options = scope.ServiceProvider.GetRequiredService<IOptions<NetworkSettings>>();
        var settings = options.Value;

        // Assert - All ports must be different
        var ports = new[] { settings.SignalRPort, settings.HttpPort, settings.TcpPort, settings.UdpBroadcastPort };
        Assert.Equal(4, ports.Distinct().Count());
    }

    #endregion
}
