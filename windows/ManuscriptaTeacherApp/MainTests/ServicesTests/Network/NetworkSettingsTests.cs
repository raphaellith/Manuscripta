using System.Reflection;
using Microsoft.Extensions.Configuration;
using Microsoft.Extensions.DependencyInjection;
using Microsoft.Extensions.Options;
using Main.Services.Network;
using Xunit;

namespace MainTests.ServicesTests.Network;

/// <summary>
/// Tests for NetworkSettings configuration.
/// Verifies port configuration per API Contract.md §Ports and FrontendWorkflowSpecifications §2ZA(8)(b):
/// - SignalR Port: 5910 (frontend communication)
/// - HTTP Port: 5911 (Android client REST API)
/// - TCP Port: 5912
/// - UDP Broadcast Port: 5913
/// </summary>
public class NetworkSettingsTests
{
    #region Default Value Tests - Per API Contract.md §Ports

    /// <summary>
    /// Verifies SignalRPort defaults to 5910 per FrontendWorkflowSpecifications §2ZA(8)(b).
    /// Note: This is the preferred/default port; actual port is dynamically selected by frontend.
    /// </summary>
    [Fact]
    public void NetworkSettings_SignalRPort_DefaultsTo5910()
    {
        // Arrange
        var settings = new NetworkSettings();

        // Assert
        Assert.Equal(5910, settings.SignalRPort);
    }

    /// <summary>
    /// Verifies HttpPort defaults to 5911 per API Contract.md.
    /// </summary>
    [Fact]
    public void NetworkSettings_HttpPort_DefaultsTo5911()
    {
        // Arrange
        var settings = new NetworkSettings();

        // Assert
        Assert.Equal(5911, settings.HttpPort);
    }

    /// <summary>
    /// Verifies TcpPort defaults to 5912 per API Contract.md.
    /// </summary>
    [Fact]
    public void NetworkSettings_TcpPort_DefaultsTo5912()
    {
        // Arrange
        var settings = new NetworkSettings();

        // Assert
        Assert.Equal(5912, settings.TcpPort);
    }

    /// <summary>
    /// Verifies UdpBroadcastPort defaults to 5913 per API Contract.md.
    /// </summary>
    [Fact]
    public void NetworkSettings_UdpBroadcastPort_DefaultsTo5913()
    {
        // Arrange
        var settings = new NetworkSettings();

        // Assert
        Assert.Equal(5913, settings.UdpBroadcastPort);
    }

    /// <summary>
    /// Verifies BroadcastIntervalMs has a sensible default per API Contract.md §1.1.
    /// </summary>
    [Fact]
    public void NetworkSettings_BroadcastIntervalMs_HasDefaultValue()
    {
        // Arrange
        var settings = new NetworkSettings();

        // Assert - Per API Contract §1.1: "continuously broadcasts"
        Assert.True(settings.BroadcastIntervalMs > 0);
    }

    #endregion

    #region Configuration Binding Tests

    /// <summary>
    /// Verifies that NetworkSettings can be bound from configuration.
    /// </summary>
    [Fact]
    public void NetworkSettings_CanBeLoadedFromConfiguration()
    {
        // Arrange
        var configuration = new ConfigurationBuilder()
            .AddInMemoryCollection(new Dictionary<string, string?>
            {
                ["NetworkSettings:SignalRPort"] = "5910",
                ["NetworkSettings:HttpPort"] = "5911",
                ["NetworkSettings:TcpPort"] = "5912",
                ["NetworkSettings:UdpBroadcastPort"] = "5913",
                ["NetworkSettings:BroadcastIntervalMs"] = "3000"
            })
            .Build();

        var services = new ServiceCollection();
        services.Configure<NetworkSettings>(configuration.GetSection("NetworkSettings"));
        var provider = services.BuildServiceProvider();

        // Act
        var options = provider.GetRequiredService<IOptions<NetworkSettings>>();
        var settings = options.Value;

        // Assert
        Assert.Equal(5910, settings.SignalRPort);
        Assert.Equal(5911, settings.HttpPort);
        Assert.Equal(5912, settings.TcpPort);
        Assert.Equal(5913, settings.UdpBroadcastPort);
        Assert.Equal(3000, settings.BroadcastIntervalMs);
    }

    /// <summary>
    /// Verifies that misconfigurations are detectable.
    /// If someone changes the port, tests should catch it.
    /// </summary>
    [Theory]
    [InlineData(5910)] // Wrong - this is SignalR port
    [InlineData(80)]   // Wrong - standard HTTP
    [InlineData(8080)] // Wrong - alt HTTP
    [InlineData(3000)] // Wrong - dev server
    public void NetworkSettings_HttpPort_RejectsWrongPorts(int wrongPort)
    {
        // Arrange
        var settings = new NetworkSettings();

        // Assert - Default should be 5911, not any of these
        Assert.NotEqual(wrongPort, settings.HttpPort);
        Assert.Equal(5911, settings.HttpPort);
    }

    #endregion

    #region Integration with appsettings.json

    /// <summary>
    /// Verifies that the actual appsettings.json contains correct port configuration.
    /// This test reads the real appsettings.json to ensure it matches API Contract.
    /// </summary>
    [Fact]
    public void AppSettings_NetworkSettings_HasCorrectPorts()
    {
        // Arrange - Load the actual appsettings.json
        var configuration = new ConfigurationBuilder()
            .SetBasePath(GetMainProjectPath())
            .AddJsonFile("appsettings.json", optional: false)
            .Build();

        var signalRPort = configuration.GetValue<int>("NetworkSettings:SignalRPort");
        var httpPort = configuration.GetValue<int>("NetworkSettings:HttpPort");
        var tcpPort = configuration.GetValue<int>("NetworkSettings:TcpPort");
        var udpPort = configuration.GetValue<int>("NetworkSettings:UdpBroadcastPort");

        // Assert - Per FrontendWorkflowSpecifications §2ZA(8)(b) and API Contract.md §Ports
        Assert.Equal(5910, signalRPort);
        Assert.Equal(5911, httpPort);
        Assert.Equal(5912, tcpPort);
        Assert.Equal(5913, udpPort);
    }

    private static string GetMainProjectPath()
    {
        var startDirectories = new[]
        {
            AppContext.BaseDirectory,
            Directory.GetCurrentDirectory(),
            Path.GetDirectoryName(Assembly.GetExecutingAssembly().Location) ?? string.Empty
        };

        foreach (var startDir in startDirectories)
        {
            if (string.IsNullOrWhiteSpace(startDir))
            {
                continue;
            }

            var candidate = FindMainProjectDirectory(startDir);
            if (!string.IsNullOrEmpty(candidate))
            {
                return candidate;
            }
        }

        throw new DirectoryNotFoundException("Unable to locate Manuscripta.Main.csproj from test environment.");
    }

    private static string? FindMainProjectDirectory(string startDirectory)
    {
        var current = new DirectoryInfo(startDirectory);
        while (current != null)
        {
            var mainProjectPath = Path.Combine(current.FullName, "windows", "ManuscriptaTeacherApp", "Main", "Manuscripta.Main.csproj");
            if (File.Exists(mainProjectPath))
            {
                return Path.GetDirectoryName(mainProjectPath);
            }

            var altMainProjectPath = Path.Combine(current.FullName, "Main", "Manuscripta.Main.csproj");
            if (File.Exists(altMainProjectPath))
            {
                return Path.GetDirectoryName(altMainProjectPath);
            }

            current = current.Parent;
        }

        return null;
    }

    #endregion
}
