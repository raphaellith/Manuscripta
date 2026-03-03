using System;
using System.IO;
using System.Net.Http;
using System.Linq;
using System.Threading.Tasks;
using Main.Models;
using Main.Services.GenAI;
using Microsoft.Extensions.Logging;

namespace Main.Services.RuntimeDependencies
{
    /// <summary>
    /// Manages the availability and installation of the OV-Ollama runtime dependency.
    /// Per GenAISpec.md §1G.
    /// </summary>
    public class OpenVinoOllamaRuntimeDependencyManager : RuntimeDependencyManagerBase
    {
        private readonly ILogger<OpenVinoOllamaRuntimeDependencyManager> _logger;
        private readonly HttpClient _httpClient;
        private static OllamaClientService? _ollamaClientServiceInstance;
        private readonly object _serviceLock = new object();

        public override string DependencyId => "ollama-openvino";

        protected virtual string GetAppDataFolder()
        {
            return Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData);
        }

        public OpenVinoOllamaRuntimeDependencyManager(
            ILogger<OpenVinoOllamaRuntimeDependencyManager> logger,
            HttpClient httpClient)
        {
            _logger = logger;
            _httpClient = httpClient;
        }

        public override async Task<bool> CheckDependencyAvailabilityAsync()
        {
            try
            {
                // OV-Ollama exposes the same API as Standard Ollama
                using var cts = new System.Threading.CancellationTokenSource(TimeSpan.FromSeconds(5));
                var response = await _httpClient.GetAsync("http://localhost:11434/api/version", cts.Token);
                return response.StatusCode == System.Net.HttpStatusCode.OK;
            }
            catch
            {
                return false;
            }
        }

        protected override async Task DownloadDependencyAsync(IProgress<RuntimeDependencyProgress> progress)
        {
            var binDir = Path.Combine(GetAppDataFolder(), "ManuscriptaTeacherApp", "bin");
            Directory.CreateDirectory(binDir);
            
            var extractDir = Path.Combine(binDir, "ollama-openvino");
            
            try
            {
                var pathEnv = Environment.GetEnvironmentVariable("PATH", EnvironmentVariableTarget.Process) ?? string.Empty;
                var pathItems = pathEnv.Split(Path.PathSeparator, StringSplitOptions.RemoveEmptyEntries).ToList();
                if (!pathItems.Any(p => string.Equals(p, extractDir, StringComparison.OrdinalIgnoreCase)))
                {
                    pathItems.Add(extractDir);
                    var newPath = string.Join(Path.PathSeparator, pathItems);
                    Environment.SetEnvironmentVariable("PATH", newPath, EnvironmentVariableTarget.Process);
                    Environment.SetEnvironmentVariable("PATH", newPath, EnvironmentVariableTarget.User);
                }
            }
            catch (Exception ex)
            {
                _logger.LogWarning(ex, "Failed to update PATH environment variable with OV-Ollama extract directory");
            }

            var zipPath = Path.Combine(binDir, "ollama-openvino-windows-amd64.zip");
            var downloadUrl = "https://drive.google.com/file/d/1Xo3ohbfC852KtJy_4xtn_YrYaH4Y_507/view?usp=sharing";

            _logger.LogInformation("Downloading OV-Ollama from {Url}", downloadUrl);

            if (File.Exists(zipPath))
            {
                File.Delete(zipPath);
            }

            try
            {
                // Direct file download from Google Drive is often blocked by a "Virus scan warning" page.
                // We attempt to fetch the file, but if it returns HTML we throw so the frontend defaults to manual.
                var request = new HttpRequestMessage(HttpMethod.Get, downloadUrl);
                using var response = await _httpClient.SendAsync(request, HttpCompletionOption.ResponseHeadersRead);
                response.EnsureSuccessStatusCode();

                if (response.Content.Headers.ContentType?.MediaType?.Contains("text/html") == true)
                {
                    throw new InvalidOperationException("Google Drive returned an HTML page instead of the zip file. Manual installation required.");
                }

                var totalBytes = response.Content.Headers.ContentLength ?? -1;
                using var contentStream = await response.Content.ReadAsStreamAsync();
                using var fileStream = new FileStream(zipPath, FileMode.Create, FileAccess.Write, FileShare.None);

                var buffer = new byte[8192];
                var totalRead = 0L;
                int bytesRead;

                while ((bytesRead = await contentStream.ReadAsync(buffer, 0, buffer.Length)) != 0)
                {
                    await fileStream.WriteAsync(buffer, 0, bytesRead);
                    totalRead += bytesRead;

                    if (totalBytes > 0 && progress != null)
                    {
                        var progressPercentage = (int)((totalRead * 100) / totalBytes);
                        progress.Report(new RuntimeDependencyProgress
                        {
                            Phase = "Downloading",
                            ProgressPercentage = progressPercentage
                        });
                    }
                }

                _logger.LogInformation("OV-Ollama downloaded successfully to {Path}", zipPath);
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, "Failed to download OV-Ollama. Manual installation likely required.");
                throw;
            }
        }

        protected override Task VerifyDownloadAsync(IProgress<RuntimeDependencyProgress> progress)
        {
            // OV-Ollama ZIP from Google Drive currently lacks a published checksum.
            // Bypassing checksum validation per immediate requirements.
            _logger.LogInformation("Skipping checksum verification for OV-Ollama.");
            return Task.CompletedTask;
        }

        protected override async Task PerformInstallDependencyAsync(IProgress<RuntimeDependencyProgress> progress)
        {
            var binDir = Path.Combine(GetAppDataFolder(), "ManuscriptaTeacherApp", "bin");
            var zipPath = Path.Combine(binDir, "ollama-openvino-windows-amd64.zip");
            var extractDir = Path.Combine(binDir, "ollama-openvino");

            _logger.LogInformation("Extracting OV-Ollama to {Path}", extractDir);

            try
            {
                if (Directory.Exists(extractDir))
                {
                    try
                    {
                        var procs = System.Diagnostics.Process.GetProcessesByName("ollama");
                        foreach (var p in procs)
                        {
                            try { p.Kill(); p.WaitForExit(5000); } catch { }
                        }

                        var allFiles = Directory.GetFiles(extractDir, "*", SearchOption.AllDirectories);
                        foreach (var f in allFiles) { try { File.SetAttributes(f, FileAttributes.Normal); } catch { } }

                        Directory.Delete(extractDir, true);
                    }
                    catch (Exception ex)
                    {
                        _logger.LogWarning(ex, "Failed deleting existing OV-Ollama directory.");
                    }
                }

                System.IO.Compression.ZipFile.ExtractToDirectory(zipPath, extractDir);
                File.Delete(zipPath);

                try
                {
                    var pathEnv = Environment.GetEnvironmentVariable("PATH", EnvironmentVariableTarget.Process) ?? string.Empty;
                    var pathItems = pathEnv.Split(Path.PathSeparator, StringSplitOptions.RemoveEmptyEntries).ToList();
                    if (!pathItems.Any(p => string.Equals(p, extractDir, StringComparison.OrdinalIgnoreCase)))
                    {
                        pathItems.Add(extractDir);
                        var newPath = string.Join(Path.PathSeparator, pathItems);
                        Environment.SetEnvironmentVariable("PATH", newPath, EnvironmentVariableTarget.Process);
                        Environment.SetEnvironmentVariable("PATH", newPath, EnvironmentVariableTarget.User);
                    }
                }
                catch { }

                progress?.Report(new RuntimeDependencyProgress { Phase = "Starting OV-Ollama daemon" });
                await StartOllamaDaemonAsync(extractDir);

                _logger.LogInformation("OV-Ollama daemon started.");
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, "Failed to install OV-Ollama");
                throw;
            }
        }

        public override Task<bool> UninstallDependencyAsync()
        {
            _logger.LogInformation("Uninstalling OV-Ollama");

            try
            {
                var processes = System.Diagnostics.Process.GetProcessesByName("ollama");
                foreach (var process in processes)
                {
                    try { process.Kill(); process.WaitForExit(5000); } catch { }
                }

                var binDir = Path.Combine(GetAppDataFolder(), "ManuscriptaTeacherApp", "bin");
                var ollamaDir = Path.Combine(binDir, "ollama-openvino");

                if (Directory.Exists(ollamaDir))
                {
                    Directory.Delete(ollamaDir, true);
                }

                return Task.FromResult(true);
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, "Failed to uninstall OV-Ollama");
                return Task.FromResult(false);
            }
        }

        protected override Task<IDependencyService> ProvideDependencyServiceAsync()
        {
            lock (_serviceLock)
            {
                if (_ollamaClientServiceInstance == null)
                {
                    _ollamaClientServiceInstance = new OllamaClientService();
                }

                return Task.FromResult<IDependencyService>(_ollamaClientServiceInstance);
            }
        }

        private async Task StartOllamaDaemonAsync(string ollamaDir)
        {
            var ollamaExe = Path.Combine(ollamaDir, "ollama.exe");

            if (!File.Exists(ollamaExe))
            {
                throw new InvalidOperationException($"OV-Ollama executable not found at {ollamaExe}");
            }

            var processStartInfo = new System.Diagnostics.ProcessStartInfo
            {
                FileName = ollamaExe,
                Arguments = "serve",
                UseShellExecute = false,
                CreateNoWindow = true
            };

            using var process = System.Diagnostics.Process.Start(processStartInfo);
            if (process == null) throw new InvalidOperationException("Failed to start OV-Ollama daemon process");

            for (int i = 0; i < 30; i++)
            {
                await Task.Delay(500);
                try
                {
                    var response = await _httpClient.GetAsync("http://localhost:11434/api/version");
                    if (response.StatusCode == System.Net.HttpStatusCode.OK) return;
                }
                catch { }
            }

            throw new InvalidOperationException("OV-Ollama daemon failed to start within timeout period");
        }
    }
}
