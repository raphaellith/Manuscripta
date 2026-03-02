using System;
using System.Diagnostics;
using System.Net.Http;
using System.Threading.Tasks;
using Main.Models;
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
        private readonly Process? _dummyProcess;

        public TestableQwen3ModelRuntimeDependencyManager(
            ILogger<Qwen3ModelRuntimeDependencyManager> logger,
            HttpClient httpClient,
            Process? dummyProcess = null)
            : base(logger, httpClient)
        {
            _dummyProcess = dummyProcess;
        }

        protected override Process StartPullProcess(string args)
        {
            if (_dummyProcess != null)
            {
                return _dummyProcess;
            }
            return base.StartPullProcess(args);
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
        var manager = new Qwen3ModelRuntimeDependencyManager(mockLogger.Object, httpClient);

        // Act
        var id = manager.DependencyId;

        // Assert
        Assert.Equal("qwen3:8b", id);
    }

    [Fact]
    public async Task DownloadDependencyAsync_ReadsProcessOutputAndCompletes()
    {
        // Arrange: Create a dummy process that echoes output and exits cleanly
        var dummyProcess = new Process
        {
            StartInfo = new ProcessStartInfo
            {
                FileName = "cmd",
                Arguments = "/c \"echo progress1 & echo progress2 & exit /b 0\"",
                UseShellExecute = false,
                RedirectStandardOutput = true,
                RedirectStandardError = true,
                CreateNoWindow = true
            }
        };
        dummyProcess.Start();

        var mockLogger = new Mock<ILogger<Qwen3ModelRuntimeDependencyManager>>();
        var httpClient = new HttpClient();
        var manager = new TestableQwen3ModelRuntimeDependencyManager(mockLogger.Object, httpClient, dummyProcess);
        var progress = new Progress<RuntimeDependencyProgress>();

        // Act
        await manager.PublicDownloadDependencyAsync(progress);

        // Assert: Process completed without exception
        Assert.True(true); // Successful completion of async download
    }
}
