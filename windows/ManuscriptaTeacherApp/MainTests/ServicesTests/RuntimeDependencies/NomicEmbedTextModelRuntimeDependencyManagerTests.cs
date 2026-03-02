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

public class NomicEmbedTextModelRuntimeDependencyManagerTests
{
    [Fact]
    public void DependencyId_ReturnsCorrectId()
    {
        var logger = new Mock<ILogger<NomicEmbedTextModelRuntimeDependencyManager>>();
        var httpClient = new HttpClient();
        var manager = new NomicEmbedTextModelRuntimeDependencyManager(logger.Object, httpClient);

        Assert.Equal("nomic-embed-text", manager.DependencyId);
    }

    [Fact]
    public async Task DownloadDependencyAsync_ReadsProcessOutputAndCompletes()
    {
        var logger = new Mock<ILogger<NomicEmbedTextModelRuntimeDependencyManager>>();
        var httpClient = new HttpClient();
        var manager = new TestableNomicEmbedTextManager(logger.Object, httpClient);

        // act
        await manager.PublicDownload(new Progress<RuntimeDependencyProgress>());

        // assert - method finished without throwing (the process output was consumed internally)
        Assert.True(true);
    }

    /// <summary>
    /// Subclass overrides process creation to use a harmless command that prints
    /// a few lines and exits. This simulates a long-running `ollama pull` without
    /// requiring the real binary.
    /// </summary>
    private class TestableNomicEmbedTextManager : NomicEmbedTextModelRuntimeDependencyManager
    {
        public TestableNomicEmbedTextManager(ILogger<NomicEmbedTextModelRuntimeDependencyManager> logger, HttpClient httpClient)
            : base(logger, httpClient)
        {
        }

        public Task PublicDownload(IProgress<RuntimeDependencyProgress> progress)
            => DownloadDependencyAsync(progress);

        protected override Process StartPullProcess(string args)
        {
            // construct a command that prints some lines slowly
            // note that cmd /c for loops require double %% at runtime
            var psi = new ProcessStartInfo
            {
                FileName = "cmd",
                Arguments = "/c \"for /L %i in (1,1,5) do @echo line %i & ping -n 1 localhost >nul\"",
                UseShellExecute = false,
                RedirectStandardOutput = true,
                RedirectStandardError = true,
                CreateNoWindow = true
            };
            return Process.Start(psi) ?? throw new InvalidOperationException("failed to start test process");
        }
    }
}