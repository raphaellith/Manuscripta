using System;
using System.IO;
using System.Net.Http;
using System.Security.Cryptography;
using System.Threading.Tasks;
using Main.Models;
using Main.Services.GenAI;
using Main.Services.RuntimeDependencies;
using Microsoft.Extensions.Logging;

namespace Main.Services.RuntimeDependencies
{
    /// <summary>
    /// Manages the availability and installation of Ollama runtime dependency.
    /// Per GenAISpec.md §1A.
    /// </summary>
    public class OllamaRuntimeDependencyManager : RuntimeDependencyManagerBase
    {
        private readonly ILogger<OllamaRuntimeDependencyManager> _logger;
        private readonly HttpClient _httpClient;
        private static OllamaClientService? _ollamaClientServiceInstance;
        private readonly object _serviceLock = new object();

        public override string DependencyId => "ollama";

        /// <summary>
        /// Returns the base directory used for storing runtime dependency data.
        /// By default this is the user's ApplicationData special folder.  Exposed
        /// as a virtual method so unit tests can override it without relying on
        /// environment variable hacks (Path.GetFolderPath caches values on some
        /// platforms).
        /// </summary>
        protected virtual string GetAppDataFolder()
        {
            return Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData);
        }

        public OllamaRuntimeDependencyManager(
            ILogger<OllamaRuntimeDependencyManager> logger,
            HttpClient httpClient)
        {
            _logger = logger;
            _httpClient = httpClient;
        }

        /// <summary>
        /// Checks if Ollama is available by calling http://localhost:11434/api/version.
        /// Per GenAISpec.md §1A(3)(a).
        /// </summary>
        public override async Task<bool> CheckDependencyAvailabilityAsync()
        {
            try
            {
                var response = await _httpClient.GetAsync("http://localhost:11434/api/version");
                return response.StatusCode == System.Net.HttpStatusCode.OK;
            }
            catch
            {
                return false;
            }
        }

        /// <summary>
        /// Downloads Ollama from GitHub releases.
        /// Per GenAISpec.md §1A(3)(b).
        /// </summary>
        protected override async Task DownloadDependencyAsync(IProgress<RuntimeDependencyProgress> progress)
        {
            var binDir = Path.Combine(
                GetAppDataFolder(),
                "ManuscriptaTeacherApp",
                "bin"
            );
            Directory.CreateDirectory(binDir);

            // Path to the directory where Ollama will be extracted, used later during
            // PerformInstallDependencyAsync.  Add this directory to PATH as well so
            // that commands like "ollama pull" work immediately after installation.
            var extractDir = Path.Combine(binDir, "ollama");

            // Ensure the extraction directory is on the PATH for both the current process and the user.
            // This satisfies GenAISpec §1A(3)(b) and ensures that spawned "ollama" commands
            // can be resolved even when the application is launched without a pre-existing
            // PATH entry.
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
                _logger.LogWarning(ex, "Failed to update PATH environment variable with Ollama extract directory");
            }

            var zipPath = Path.Combine(binDir, "ollama-windows-amd64.zip");
            var downloadUrl = "https://github.com/ollama/ollama/releases/latest/download/ollama-windows-amd64.zip";

            _logger.LogInformation("Downloading Ollama from {Url}", downloadUrl);

            // Delete existing file if present
            if (File.Exists(zipPath))
            {
                File.Delete(zipPath);
            }

            try
            {
                using var response = await _httpClient.GetAsync(downloadUrl, HttpCompletionOption.ResponseHeadersRead);
                response.EnsureSuccessStatusCode();

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

                    if (totalBytes > 0)
                    {
                        var progressPercentage = (int)((totalRead * 100) / totalBytes);
                        progress?.Report(new RuntimeDependencyProgress
                        {
                            Phase = "Downloading",
                            ProgressPercentage = progressPercentage
                        });
                    }
                }

                _logger.LogInformation("Ollama downloaded successfully to {Path}", zipPath);
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, "Failed to download Ollama");
                throw;
            }
        }

        /// <summary>
        /// Verifies the downloaded Ollama ZIP file against published SHA256 checksum.
        /// Per GenAISpec.md §1A(3)(c).
        /// </summary>
        protected override async Task VerifyDownloadAsync(IProgress<RuntimeDependencyProgress> progress)
        {
            var binDir = Path.Combine(
                GetAppDataFolder(),
                "ManuscriptaTeacherApp",
                "bin"
            );
            var zipPath = Path.Combine(binDir, "ollama-windows-amd64.zip");

            if (!File.Exists(zipPath))
            {
                throw new InvalidOperationException("Downloaded file not found");
            }

            _logger.LogInformation("Verifying Ollama checksum");

            try
            {
                // Download the checksum file
                var checksumUrl = "https://github.com/ollama/ollama/releases/latest/download/sha256sum.txt";
                var checksumContent = await _httpClient.GetStringAsync(checksumUrl);

                // Parse the checksum file to find the entry for ollama-windows-amd64.zip
                var lines = checksumContent.Split(new[] { "\r\n", "\r", "\n" }, StringSplitOptions.None);
                string expectedChecksum = "";

                foreach (var line in lines)
                {
                    if (line.Contains("ollama-windows-amd64.zip"))
                    {
                        // Parse "CHECKSUM  ./ollama-windows-amd64.zip" or "CHECKSUM  ollama-windows-amd64.zip"
                        var parts = line.Split(new[] { ' ', '\t' }, StringSplitOptions.RemoveEmptyEntries);
                        if (parts.Length > 0)
                        {
                            expectedChecksum = parts[0].ToLowerInvariant();
                            break;
                        }
                    }
                }

                if (string.IsNullOrEmpty(expectedChecksum))
                {
                    throw new InvalidOperationException("Could not find checksum for ollama-windows-amd64.zip in remote checksum file");
                }

                // Calculate the checksum of the downloaded file
                using var sha256 = SHA256.Create();
                using var fileStream = File.OpenRead(zipPath);
                var hashBytes = await Task.Run(() => sha256.ComputeHash(fileStream));
                var actualChecksum = BitConverter.ToString(hashBytes).Replace("-", "").ToLowerInvariant();

                if (actualChecksum != expectedChecksum)
                {
                    throw new InvalidOperationException(
                        $"Checksum verification failed. Expected: {expectedChecksum}, Actual: {actualChecksum}"
                    );
                }

                _logger.LogInformation("Ollama checksum verified successfully");
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, "Failed to verify Ollama checksum");
                throw;
            }
        }

        /// <summary>
        /// Extracts Ollama and starts the daemon.
        /// Per GenAISpec.md §1A(3)(d).
        /// </summary>
        protected override async Task PerformInstallDependencyAsync(IProgress<RuntimeDependencyProgress> progress)
        {
            var binDir = Path.Combine(
                GetAppDataFolder(),
                "ManuscriptaTeacherApp",
                "bin"
            );
            var zipPath = Path.Combine(binDir, "ollama-windows-amd64.zip");
            var extractDir = Path.Combine(binDir, "ollama");

            _logger.LogInformation("Extracting Ollama to {Path}", extractDir);

            try
            {
                // If an existing installation directory exists, try to remove it safely.
                if (Directory.Exists(extractDir))
                {
                    try
                    {
                        // Kill any running ollama processes that may lock files
                        var procs = System.Diagnostics.Process.GetProcessesByName("ollama");
                        foreach (var p in procs)
                        {
                            try
                            {
                                p.Kill();
                                p.WaitForExit(5000);
                            }
                            catch (Exception ex)
                            {
                                _logger.LogWarning(ex, "Failed to stop ollama process {Id}", p.Id);
                            }
                        }

                        // Ensure files are writable (clear read-only attributes) before deletion
                        var allFiles = Directory.GetFiles(extractDir, "*", SearchOption.AllDirectories);
                        foreach (var f in allFiles)
                        {
                            try
                            {
                                File.SetAttributes(f, FileAttributes.Normal);
                            }
                            catch (Exception ex)
                            {
                                _logger.LogDebug(ex, "Failed to clear attributes on {File}", f);
                            }
                        }

                        Directory.Delete(extractDir, true);
                    }
                    catch (UnauthorizedAccessException uaEx)
                    {
                        _logger.LogWarning(uaEx, "Unauthorized access deleting existing Ollama directory, retrying with attribute cleanup");
                        // Second attempt: clear file attributes recursively then delete
                        try
                        {
                            var files = Directory.GetFiles(extractDir, "*", SearchOption.AllDirectories);
                            foreach (var f in files)
                            {
                                try { File.SetAttributes(f, FileAttributes.Normal); } catch { }
                            }

                            Directory.Delete(extractDir, true);
                        }
                        catch (Exception ex2)
                        {
                            _logger.LogError(ex2, "Failed to remove existing Ollama directory after retry");
                            throw;
                        }
                    }
                }

                System.IO.Compression.ZipFile.ExtractToDirectory(zipPath, extractDir);

                // Delete the ZIP file
                File.Delete(zipPath);

                // After extraction ensure the directory is on PATH (in case it wasn't picked up earlier)
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
                    _logger.LogWarning(ex, "Failed to update PATH after Ollama extraction");
                }

                _logger.LogInformation("Ollama extracted successfully");

                // Start ollama serve
                progress?.Report(new RuntimeDependencyProgress { Phase = "Starting Ollama daemon" });
                await StartOllamaDaemonAsync(extractDir);

                _logger.LogInformation("Ollama daemon started");
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, "Failed to install Ollama");
                throw;
            }
        }

        /// <summary>
        /// Uninstalls Ollama by killing processes and removing files.
        /// Per GenAISpec.md §1A(3)(e).
        /// </summary>
        public override async Task<bool> UninstallDependencyAsync()
        {
            _logger.LogInformation("Uninstalling Ollama");

            try
            {
                // Kill any running ollama.exe processes
                var processes = System.Diagnostics.Process.GetProcessesByName("ollama");
                foreach (var process in processes)
                {
                    try
                    {
                        process.Kill();
                        process.WaitForExit(5000);
                    }
                    catch (Exception ex)
                    {
                        _logger.LogWarning(ex, "Failed to kill Ollama process");
                    }
                }

                // Delete the installation directory
                var binDir = Path.Combine(
                    GetAppDataFolder(),
                    "ManuscriptaTeacherApp",
                    "bin"
                );
                var ollamaDir = Path.Combine(binDir, "ollama");

                if (Directory.Exists(ollamaDir))
                {
                    Directory.Delete(ollamaDir, true);
                }

                _logger.LogInformation("Ollama uninstalled successfully");
                return true;
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, "Failed to uninstall Ollama");
                return false;
            }
        }

        /// <summary>
        /// Provides a singleton instance of OllamaClientService.
        /// Per GenAISpec.md §1A(3)(f) and BackendRuntimeDependencyManagementSpecification §2(3A).
        /// </summary>
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

        /// <summary>
        /// Starts the Ollama daemon process.
        /// </summary>
        private async Task StartOllamaDaemonAsync(string ollamaDir)
        {
            var ollamaExe = Path.Combine(ollamaDir, "ollama.exe");

            if (!File.Exists(ollamaExe))
            {
                throw new InvalidOperationException($"Ollama executable not found at {ollamaExe}");
            }

            var processStartInfo = new System.Diagnostics.ProcessStartInfo
            {
                FileName = ollamaExe,
                Arguments = "serve",
                UseShellExecute = false,
                CreateNoWindow = true
            };

            using var process = System.Diagnostics.Process.Start(processStartInfo);
            if (process == null)
            {
                throw new InvalidOperationException("Failed to start Ollama daemon process");
            }

            // Wait for daemon to start
            for (int i = 0; i < 30; i++)
            {
                await Task.Delay(500);
                try
                {
                    var response = await _httpClient.GetAsync("http://localhost:11434/api/version");
                    if (response.StatusCode == System.Net.HttpStatusCode.OK)
                    {
                        return;
                    }
                }
                catch
                {
                    // Daemon not ready yet
                }
            }

            throw new InvalidOperationException("Ollama daemon failed to start within timeout period");
        }
    }
}
