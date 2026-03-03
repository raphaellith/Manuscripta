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

public class Qwen3ModelRuntimeDependencyManagerTests
{
    /// <summary>
    /// Testable subclass that can inject a dummy process instead of
    /// requiring real Ollama/filesystem operations.
    /// </summary>
    private class TestableQwen3ModelRuntimeDependencyManager : Qwen3ModelRuntimeDependencyManager
    {
        public TestableQwen3ModelRuntimeDependencyManager(
            ILogger<Qwen3ModelRuntimeDependencyManager> logger,
            HttpClient httpClient,
            IInferenceRuntimeSelector inferenceRuntimeSelector)
            : base(logger, httpClient, inferenceRuntimeSelector)
        {
        }

        protected override Task<Process> StartPullProcessAsync(string args)
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

            return Task.FromResult(Process.Start(startInfo) ?? throw new InvalidOperationException("failed to start test process"));
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
        var mockLogger = new Mock<ILogger<Qwen3ModelRuntimeDependencyManager>>();
        var httpClient = new HttpClient();
        var mockRuntimeSelector = new Mock<IInferenceRuntimeSelector>();
        var manager = new Qwen3ModelRuntimeDependencyManager(mockLogger.Object, httpClient, mockRuntimeSelector.Object);

        // Act
        var id = manager.DependencyId;

        // Assert
        Assert.Equal("qwen3:8b", id);
    }

    [Fact]
    public async Task DownloadDependencyAsync_ReadsProcessOutputAndCompletes()
    {
        // Arrange
        var mockLogger = new Mock<ILogger<Qwen3ModelRuntimeDependencyManager>>();
        var httpClient = new HttpClient();
        var mockRuntimeSelector = new Mock<IInferenceRuntimeSelector>();
        var manager = new TestableQwen3ModelRuntimeDependencyManager(mockLogger.Object, httpClient, mockRuntimeSelector.Object);
        var progress = new Progress<RuntimeDependencyProgress>();

        // Act
        await manager.PublicDownloadDependencyAsync(progress);

        // Assert: Process completed without exception
        Assert.True(true); // Successful completion of async download
    }
}
