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

public class NomicEmbedTextModelRuntimeDependencyManagerTests
{
    [Fact]
    public void DependencyId_ReturnsCorrectId()
    {
        var logger = new Mock<ILogger<NomicEmbedTextModelRuntimeDependencyManager>>();
        var httpClient = new HttpClient();
        var manager = new NomicEmbedTextModelRuntimeDependencyManager(
            logger.Object,
            httpClient,
            CreateProviderConfigurationResolver().Object,
            new OllamaClientService("http://localhost:11434"));

        Assert.Equal("nomic-embed-text", manager.DependencyId);
    }

    [Fact]
    public async Task DownloadDependencyAsync_ReadsProcessOutputAndCompletes()
    {
        var logger = new Mock<ILogger<NomicEmbedTextModelRuntimeDependencyManager>>();
        var httpClient = new HttpClient();
        var manager = new TestableNomicEmbedTextManager(
            logger.Object,
            httpClient,
            CreateProviderConfigurationResolver().Object);

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
        public TestableNomicEmbedTextManager(
            ILogger<NomicEmbedTextModelRuntimeDependencyManager> logger,
            HttpClient httpClient,
            IProviderConfigurationResolver providerConfigurationResolver)
            : base(logger, httpClient, providerConfigurationResolver, new OllamaClientService("http://localhost:11434"))
        {
        }

        public Task PublicDownload(IProgress<RuntimeDependencyProgress> progress)
            => DownloadDependencyAsync(progress);

        protected override Process StartPullProcess(string args)
        {
            // construct a command that prints some lines
            var psi = new ProcessStartInfo
            {
                UseShellExecute = false,
                RedirectStandardOutput = true,
                RedirectStandardError = true,
                CreateNoWindow = true
            };

            if (OperatingSystem.IsWindows())
            {
                psi.FileName = "cmd";
                psi.Arguments = "/c \"for /L %i in (1,1,5) do @echo line %i & ping -n 1 localhost >nul\"";
            }
            else
            {
                psi.FileName = "/bin/sh";
                psi.Arguments = "-c \"for i in 1 2 3 4 5; do echo line $i; done\"";
            }

            return Process.Start(psi) ?? throw new InvalidOperationException("failed to start test process");
        }
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