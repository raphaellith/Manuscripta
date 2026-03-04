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

                // Download official OpenVINO GenAI runtime archive from Intel storage
                progress?.Report(new RuntimeDependencyProgress { Phase = "Downloading OpenVINO GenAI runtime" });
                
                var openVinoGenAiUrl = "https://storage.openvinotoolkit.org/repositories/openvino_genai/packages/pre-release/2025.4.0.0rc3/openvino_genai_windows_2025.4.0.0rc3_x86_64.zip";
                var tempArchivePath = Path.Combine(binDir, "openvino_genai_runtime.zip");

                _logger.LogInformation("Downloading OpenVINO GenAI runtime from {Url}", openVinoGenAiUrl);
                var archiveBytes = await _httpClient.GetByteArrayAsync(openVinoGenAiUrl);
                await File.WriteAllBytesAsync(tempArchivePath, archiveBytes);

                progress?.Report(new RuntimeDependencyProgress { Phase = "Extracting OpenVINO GenAI DLLs" });

                using (var archive = ZipFile.OpenRead(tempArchivePath))
                {
                    foreach (var entry in archive.Entries)
                    {
                        // Extract only Release DLLs (skip Debug variants)
                        if (entry.FullName.EndsWith(".dll", StringComparison.OrdinalIgnoreCase)
                            && !entry.FullName.Contains("/Debug/", StringComparison.OrdinalIgnoreCase)
                            && !entry.Name.EndsWith("d.dll", StringComparison.OrdinalIgnoreCase))
                        {
                            var destPath = Path.Combine(extractDir, entry.Name);
                            entry.ExtractToFile(destPath, overwrite: true);
                            _logger.LogDebug("Extracted {DllName}", entry.Name);
                        }
                    }
                }

                File.Delete(tempArchivePath);
                _logger.LogInformation("OpenVINO GenAI runtime DLLs extracted to {Path}", extractDir);

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

                progress?.Report(new RuntimeDependencyProgress { Phase = "Ensuring Standard Ollama is running" });
                await EnsureStandardOllamaRunningAsync();

                progress?.Report(new RuntimeDependencyProgress { Phase = "Starting OV-Ollama daemon" });
                await StartOllamaDaemonAsync(extractDir);

                _logger.LogInformation("Both Standard Ollama and OV-Ollama daemons started.");
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

        /// <summary>
        /// Ensures that Standard Ollama is running on port 11434 for embedding operations.
        /// Per GenAISpec.md §1G(3)(d)(iv) and §1F(7)(d): Standard Ollama must be running
        /// whenever OV-Ollama is the active runtime.
        /// </summary>
        private async Task EnsureStandardOllamaRunningAsync()
        {
            // Check if Standard Ollama is already running
            try
            {
                using var cts = new System.Threading.CancellationTokenSource(TimeSpan.FromSeconds(3));
                var response = await _httpClient.GetAsync("http://localhost:11434/api/version", cts.Token);
                if (response.StatusCode == System.Net.HttpStatusCode.OK)
                {
                    _logger.LogInformation("Standard Ollama is already running on port 11434.");
                    return;
                }
            }
            catch
            {
                // Standard Ollama is not running — start it
            }

            _logger.LogInformation("Standard Ollama is not running. Starting it for embedding operations...");

            var standardOllamaDir = Path.Combine(GetAppDataFolder(), "ManuscriptaTeacherApp", "bin", "ollama");
            var standardOllamaExe = Path.Combine(standardOllamaDir, "ollama.exe");

            if (!File.Exists(standardOllamaExe))
            {
                throw new InvalidOperationException(
                    $"Standard Ollama executable not found at {standardOllamaExe}. " +
                    "Standard Ollama must be installed before OV-Ollama can be used. " +
                    "Per GenAISpec.md §1F(7)(d).");
            }

            var processStartInfo = new System.Diagnostics.ProcessStartInfo
            {
                FileName = standardOllamaExe,
                Arguments = "serve",
                UseShellExecute = false,
                CreateNoWindow = true
            };

            var process = System.Diagnostics.Process.Start(processStartInfo);
            if (process == null)
            {
                throw new InvalidOperationException("Failed to start Standard Ollama daemon process");
            }

            // Wait for Standard Ollama daemon to become ready on port 11434
            for (int i = 0; i < 30; i++)
            {
                await Task.Delay(500);
                try
                {
                    var response = await _httpClient.GetAsync("http://localhost:11434/api/version");
                    if (response.StatusCode == System.Net.HttpStatusCode.OK)
                    {
                        _logger.LogInformation("Standard Ollama daemon started successfully on port 11434.");
                        return;
                    }
                }
                catch
                {
                    // Daemon not ready yet
                }
            }

            throw new InvalidOperationException(
                "Standard Ollama daemon failed to start on port 11434 within timeout period");
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
                CreateNoWindow = true,
                WorkingDirectory = ollamaDir
            };
            processStartInfo.EnvironmentVariables["OLLAMA_HOST"] = "127.0.0.1:11435";
            processStartInfo.EnvironmentVariables["GODEBUG"] = "cgocheck=0";

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
