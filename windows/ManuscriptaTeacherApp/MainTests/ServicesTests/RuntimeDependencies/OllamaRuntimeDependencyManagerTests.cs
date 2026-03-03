using System;
using System.IO;
using System.IO.Compression;
using System.Net.Http;
using System.Threading.Tasks;
using Main.Models;
using Main.Services.RuntimeDependencies;
using Microsoft.Extensions.Logging;
using Moq;
using Xunit;

namespace MainTests.ServicesTests.RuntimeDependencies;

public class OllamaRuntimeDependencyManagerTests
{
    [Fact]
    public void DependencyId_ReturnsOllama()
    {
        var logger = new Mock<ILogger<OllamaRuntimeDependencyManager>>();
        var httpClient = new HttpClient();
        var manager = new OllamaRuntimeDependencyManager(logger.Object, httpClient, new Mock<Main.Services.GenAI.IInferenceRuntimeSelector>().Object);

        Assert.Equal("ollama", manager.DependencyId);
    }

    [Fact]
    public async Task DownloadDependencyAsync_WritesZipAndAddsToPath()
    {
        // Arrange: use a temporary AppData folder so bin path is isolated
        var tempAppData = Path.Combine(Path.GetTempPath(), "ollama-test-" + Guid.NewGuid().ToString("N"));
        Directory.CreateDirectory(tempAppData);

        // create a dummy zip stream containing a single file so extraction will work later
        var memoryZip = new MemoryStream();
        using (var archive = new ZipArchive(memoryZip, ZipArchiveMode.Create, true))
        {
            var entry = archive.CreateEntry("ollama.exe");
            using var entryStream = entry.Open();
            var data = System.Text.Encoding.UTF8.GetBytes("dummy");
            entryStream.Write(data, 0, data.Length);
        }
        memoryZip.Seek(0, SeekOrigin.Begin);

        // prepare HttpClient that returns the dummy zip
        var zipBytes = memoryZip.ToArray();
        var handler = new DelegatingHandlerStub(request =>
        {
            // create a fresh stream copy each time so previous readers don't interfere
            var responseStream = new MemoryStream(zipBytes, writable: false);
            var response = new HttpResponseMessage(System.Net.HttpStatusCode.OK)
            {
                Content = new StreamContent(responseStream)
            };
            return Task.FromResult(response);
        });
        var httpClient = new HttpClient(handler);

        var logger = new Mock<ILogger<OllamaRuntimeDependencyManager>>();
        var manager = new TestableOllamaRuntimeDependencyManager(logger.Object, httpClient, tempAppData);

        try
        {
            // Act
            await manager.PublicDownload(new Progress<RuntimeDependencyProgress>());

            // Assert
            var binDir = Path.Combine(tempAppData, "ManuscriptaTeacherApp", "bin");
            var expectedZip = Path.Combine(binDir, "ollama-windows-amd64.zip");
            Assert.True(File.Exists(expectedZip), "Zip file should be downloaded to bin directory");

            // PATH should contain the extract directory (even though extraction hasn't run yet)
            var extractDir = Path.Combine(binDir, "ollama");
            var pathEnv = Environment.GetEnvironmentVariable("PATH", EnvironmentVariableTarget.Process) ?? string.Empty;
            Assert.Contains(extractDir, pathEnv.Split(Path.PathSeparator, StringSplitOptions.RemoveEmptyEntries), StringComparer.OrdinalIgnoreCase);
        }
        finally
        {
            // cleanup
            try { Directory.Delete(tempAppData, true); } catch { }
        }
    }

    private class DelegatingHandlerStub : DelegatingHandler
    {
        private readonly Func<HttpRequestMessage, Task<HttpResponseMessage>> _send;
        public DelegatingHandlerStub(Func<HttpRequestMessage, Task<HttpResponseMessage>> send)
        {
            _send = send;
        }
        protected override Task<HttpResponseMessage> SendAsync(HttpRequestMessage request, System.Threading.CancellationToken cancellationToken)
        {
            return _send(request);
        }
    }

    // Subclass used to expose protected methods for testing purposes.
    private class TestableOllamaRuntimeDependencyManager : OllamaRuntimeDependencyManager
    {
        private readonly string _overrideAppData;

        public TestableOllamaRuntimeDependencyManager(ILogger<OllamaRuntimeDependencyManager> logger, HttpClient httpClient, string overrideAppData)
            : base(logger, httpClient, new Mock<Main.Services.GenAI.IInferenceRuntimeSelector>().Object)
        {
            _overrideAppData = overrideAppData;
        }

        public Task PublicDownload(IProgress<RuntimeDependencyProgress> progress)
            => DownloadDependencyAsync(progress);

        protected override string GetAppDataFolder()
        {
            return _overrideAppData;
        }
    }
}
