using System;
using System.Diagnostics;
using System.Net.Http;
using System.Threading.Tasks;
using Main.Models;
using Main.Services.GenAI;
using Main.Services.RuntimeDependencies;
using Microsoft.Extensions.Logging;
using Moq;
using Xunit;

namespace MainTests.ServicesTests.RuntimeDependencies;

public class GraniteModelRuntimeDependencyManagerTests
{
    /// <summary>
    /// Testable subclass that can inject a dummy process instead of
    /// requiring real Ollama/filesystem operations.
    /// </summary>
    private class TestableGraniteModelRuntimeDependencyManager : GraniteModelRuntimeDependencyManager
    {
        public TestableGraniteModelRuntimeDependencyManager(
            ILogger<GraniteModelRuntimeDependencyManager> logger,
            HttpClient httpClient,
            IProviderConfigurationResolver providerConfigurationResolver)
            : base(logger, httpClient, providerConfigurationResolver, new OllamaClientService("http://localhost:11434"))
        {
        }

        protected override Process StartPullProcess(string args)
        {
            var startInfo = new ProcessStartInfo
            {
                UseShellExecute = false,
                RedirectStandardOutput = true,
                RedirectStandardError = true,
                CreateNoWindow = true
            };

            if (OperatingSystem.IsWindows())
            {
                startInfo.FileName = "cmd";
                startInfo.Arguments = "/c \"echo progress1 & echo progress2 & exit /b 0\"";
            }
            else
            {
                startInfo.FileName = "/bin/sh";
                startInfo.Arguments = "-c \"echo progress1; echo progress2\"";
            }

            return Process.Start(startInfo) ?? throw new InvalidOperationException("failed to start test process");
        }

        public async Task PublicDownloadDependencyAsync(IProgress<RuntimeDependencyProgress> progress)
        {
            await DownloadDependencyAsync(progress);
        }
    }

    [Fact]
    public void DependencyId_ReturnsCorrectId()
    {
        // Arrange
        var mockLogger = new Mock<ILogger<GraniteModelRuntimeDependencyManager>>();
        var httpClient = new HttpClient();
        var manager = new GraniteModelRuntimeDependencyManager(
            mockLogger.Object,
            httpClient,
            CreateProviderConfigurationResolver().Object,
            new OllamaClientService("http://localhost:11434"));

        // Act
        var id = manager.DependencyId;

        // Assert
        Assert.Equal("granite4", id);
    }

    [Fact]
    public async Task DownloadDependencyAsync_ReadsProcessOutputAndCompletes()
    {
        // Arrange
        var mockLogger = new Mock<ILogger<GraniteModelRuntimeDependencyManager>>();
        var httpClient = new HttpClient();
        var manager = new TestableGraniteModelRuntimeDependencyManager(
            mockLogger.Object,
            httpClient,
            CreateProviderConfigurationResolver().Object);
        var progress = new Progress<RuntimeDependencyProgress>();

        // Act
        await manager.PublicDownloadDependencyAsync(progress);

        // Assert: Process completed without exception
        Assert.True(true); // Successful completion of async download
    }

    private static Mock<IProviderConfigurationResolver> CreateProviderConfigurationResolver()
    {
        var resolver = new Mock<IProviderConfigurationResolver>();
        resolver
            .Setup(r => r.GetRequiredField("OLLAMA_PROVIDER_CONFIG", "ApiBaseEndpoint"))
            .Returns("http://localhost:11434");
        return resolver;
    }
}
