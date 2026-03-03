using System;
using System.IO;
using System.IO.Compression;
using System.Net.Http;
using System.Linq;
using System.Text.RegularExpressions;
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

        private readonly IInferenceRuntimeSelector _runtimeSelector;

        public OpenVinoOllamaRuntimeDependencyManager(
            ILogger<OpenVinoOllamaRuntimeDependencyManager> logger,
            HttpClient httpClient,
            IInferenceRuntimeSelector runtimeSelector)
        {
            _logger = logger;
            _httpClient = httpClient;
            _runtimeSelector = runtimeSelector;
        }

        public override async Task<bool> CheckDependencyAvailabilityAsync()
        {
            try
            {
                // OV-Ollama exposes the same API as Standard Ollama
                using var cts = new System.Threading.CancellationTokenSource(TimeSpan.FromSeconds(5));
                var response = await _httpClient.GetAsync("http://localhost:11435/api/version", cts.Token);
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

            var tempExePath = Path.Combine(binDir, "ollama-openvino-windows-amd64.exe");
            var downloadUrl = "https://drive.google.com/uc?export=download&id=1Xo3ohbfC852KtJy_4xtn_YrYaH4Y_507";

            _logger.LogInformation("Downloading OV-Ollama as executable from {Url}", downloadUrl);

            if (File.Exists(tempExePath))
            {
                File.Delete(tempExePath);
            }

            try
            {
                // Direct file download from Google Drive is often blocked by a "Virus scan warning" page.
                // We attempt to fetch the file, but if it returns HTML we throw so the frontend defaults to manual.
                var request = new HttpRequestMessage(HttpMethod.Get, downloadUrl);
                var activeResponse = await _httpClient.SendAsync(request, HttpCompletionOption.ResponseHeadersRead);
                activeResponse.EnsureSuccessStatusCode();

                if (activeResponse.Content.Headers.ContentType?.MediaType?.Contains("text/html") == true)
                {
                    _logger.LogInformation("Google Drive returned a virus scan warning page. Attempting to bypass...");
                    var html = await activeResponse.Content.ReadAsStringAsync();

                    var confirmMatch = Regex.Match(html, @"name=""confirm""\s+value=""([^""]+)""");
                    var uuidMatch = Regex.Match(html, @"name=""uuid""\s+value=""([^""]+)""");

                    if (confirmMatch.Success)
                    {
                        var confirmToken = confirmMatch.Groups[1].Value;
                        var uuidParam = uuidMatch.Success ? $"&uuid={uuidMatch.Groups[1].Value}" : "";

                        var targetId = Regex.Match(downloadUrl, @"id=([^&]+)").Groups[1].Value;
                        if (string.IsNullOrEmpty(targetId)) targetId = "1Xo3ohbfC852KtJy_4xtn_YrYaH4Y_507"; // fallback if regex fails

                        var bypassUrl = $"https://drive.usercontent.google.com/download?id={targetId}&export=download&confirm={confirmToken}{uuidParam}";

                        _logger.LogInformation("Bypass URL generated. Submitting confirmation request...");

                        // Dispose previous response as we are replacing it
                        activeResponse.Dispose();

                        // Using same HttpClient alongside explicit cookie forwarding
                        var bypassRequest = new HttpRequestMessage(HttpMethod.Get, bypassUrl);
                        if (activeResponse.Headers.TryGetValues("Set-Cookie", out var cookies))
                        {
                            var cookieString = string.Join("; ", cookies.Select(c => c.Split(';')[0]));
                            bypassRequest.Headers.Add("Cookie", cookieString);
                        }
                        
                        activeResponse = await _httpClient.SendAsync(bypassRequest, HttpCompletionOption.ResponseHeadersRead);
                        activeResponse.EnsureSuccessStatusCode();

                        if (activeResponse.Content.Headers.ContentType?.MediaType?.Contains("text/html") == true)
                        {
                            activeResponse.Dispose();
                            throw new InvalidOperationException("Google Drive bypass failed (returned HTML again). Manual installation required.");
                        }
                    }
                    else
                    {
                        activeResponse.Dispose();
                        throw new InvalidOperationException("Google Drive returned an HTML page but bypass tokens could not be found. Manual installation required.");
                    }
                }

                using var finalResponse = activeResponse;
                var totalBytes = finalResponse.Content.Headers.ContentLength ?? -1;
                using var contentStream = await finalResponse.Content.ReadAsStreamAsync();
                using var fileStream = new FileStream(tempExePath, FileMode.Create, FileAccess.Write, FileShare.None);

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

                if (totalBytes > 0 && totalRead != totalBytes)
                {
                    throw new IOException($"Download truncated. Received {totalRead} bytes out of {totalBytes}.");
                }
                
                if (totalRead < 50_000_000)
                {
                    // OV-Ollama is 118MB; if it's less than 50MB, it's garbage/HTML
                    throw new InvalidDataException($"Downloaded file is too small ({totalRead} bytes). Google Drive download likely failed.");
                }

                _logger.LogInformation("OV-Ollama downloaded successfully to {Path}", tempExePath);
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
            var tempExePath = Path.Combine(binDir, "ollama-openvino-windows-amd64.exe");
            var extractDir = Path.Combine(binDir, "ollama-openvino");
            var finalExePath = Path.Combine(extractDir, "ollama.exe");

            _logger.LogInformation("Installing OV-Ollama executable to {Path}", extractDir);

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

                Directory.CreateDirectory(extractDir);
                if (File.Exists(tempExePath))
                {
                    File.Move(tempExePath, finalExePath, overwrite: true);
                }
                else
                {
                    throw new FileNotFoundException("Downloaded executable not found.", tempExePath);
                }

                // Download missing OpenVINO GenAI runtime dependencies from official PyPI wheels
                progress?.Report(new RuntimeDependencyProgress { Phase = "Downloading OpenVINO GenAI DLLs" });
                
                var pypiWheelUrls = new[] {
                    "https://files.pythonhosted.org/packages/79/93/f352dfcf7a405e75369853ce835f74e224c1d7d9aa40ca569ec6ac5b53ca/openvino_genai-2024.5.0.0-cp310-cp310-win_amd64.whl",
                    "https://files.pythonhosted.org/packages/fd/51/5428a4e208f71a7a97ad58415475d8b08e297dfc1cfeea836d468cce7bec/openvino-2024.5.0-17288-cp310-cp310-win_amd64.whl",
                    "https://files.pythonhosted.org/packages/aa/c1/a7207947755a2903de352f2f8519d8d99d875d1a54ddf321993f95a36af3/openvino_tokenizers-2024.5.0.0-py3-none-win_amd64.whl"
                };

                foreach (var url in pypiWheelUrls)
                {
                    var wheelBytes = await _httpClient.GetByteArrayAsync(url);
                    var tempWheelPath = Path.Combine(binDir, Path.GetFileName(url));
                    await File.WriteAllBytesAsync(tempWheelPath, wheelBytes);

                    using var archive = System.IO.Compression.ZipFile.OpenRead(tempWheelPath);
                    foreach (var entry in archive.Entries)
                    {
                        if (entry.FullName.EndsWith(".dll", StringComparison.OrdinalIgnoreCase))
                        {
                            var destPath = Path.Combine(extractDir, entry.Name);
                            entry.ExtractToFile(destPath, overwrite: true);
                        }
                    }

                    File.Delete(tempWheelPath);
                }

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
                    _ollamaClientServiceInstance = new OllamaClientService(_runtimeSelector);
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
            processStartInfo.EnvironmentVariables["OLLAMA_HOST"] = "127.0.0.1:11435";

            using var process = System.Diagnostics.Process.Start(processStartInfo);
            if (process == null) throw new InvalidOperationException("Failed to start OV-Ollama daemon process");

            for (int i = 0; i < 30; i++)
            {
                await Task.Delay(500);
                try
                {
                    var response = await _httpClient.GetAsync("http://localhost:11435/api/version");
                    if (response.StatusCode == System.Net.HttpStatusCode.OK) return;
                }
                catch { }
            }

            throw new InvalidOperationException("OV-Ollama daemon failed to start within timeout period");
        }
    }
}
