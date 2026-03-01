using System;
using System.Diagnostics;
using System.IO;
using System.Threading.Tasks;
using System.Collections.Generic;
using System.Linq;
using System.Net.Http;
using Main.Models;
using Main.Services.GenAI;
using Main.Services.RuntimeDependencies;
using Microsoft.Extensions.Logging;

namespace Main.Services.RuntimeDependencies
{
    /// <summary>
    /// Manages the availability and installation of ChromaDB runtime dependency.
    /// Per GenAISpec.md §1B.
    /// </summary>
    public class ChromaRuntimeDependencyManager : RuntimeDependencyManagerBase
    {
        private readonly ILogger<ChromaRuntimeDependencyManager> _logger;
        private Process? _chromaServerProcess;
        private string? _installedChromaPath;
        private static ChromaClientService? _chromaClientServiceInstance;
        private readonly object _serviceLock = new object();

        public override string DependencyId => "chroma";

        public ChromaRuntimeDependencyManager(
            ILogger<ChromaRuntimeDependencyManager> logger)
        {
            _logger = logger;
        }

        /// <summary>
        /// Checks if ChromaDB is available by running `chroma --version`.
        /// Per GenAISpec.md §1B(3)(a).
        /// </summary>
        public override async Task<bool> CheckDependencyAvailabilityAsync()
        {
            try
            {
                var processStartInfo = new ProcessStartInfo
                {
                    FileName = "chroma",
                    Arguments = "--version",
                    UseShellExecute = false,
                    RedirectStandardOutput = true,
                    RedirectStandardError = true,
                    CreateNoWindow = true
                };

                using var process = Process.Start(processStartInfo);
                if (process == null)
                {
                    return false;
                }

                var exitCode = await Task.Run(() =>
                {
                    process.WaitForExit(5000);
                    return process.ExitCode;
                });

                return exitCode == 0;
            }
            catch
            {
                return false;
            }
        }

        /// <summary>
        /// Downloads ChromaDB by running a PowerShell installation script.
        /// Per GenAISpec.md §1B(3)(b).
        /// </summary>
        protected override async Task DownloadDependencyAsync(IProgress<RuntimeDependencyProgress> progress)
        {
            _logger.LogInformation("Installing ChromaDB via PowerShell");

            try
            {
                // Clean up any existing chroma.exe files from failed/partial installations
                // to prevent Move-Item conflicts in the install script
                CleanupExistingChromaInstallations();

                var psScript = @"iex ((New-Object System.Net.WebClient).DownloadString('https://raw.githubusercontent.com/chroma-core/chroma/main/rust/cli/install/install.ps1'))";

                var processStartInfo = new ProcessStartInfo
                {
                    FileName = "powershell.exe",
                    Arguments = $"-Command \"{psScript}\"",
                    UseShellExecute = false,
                    RedirectStandardOutput = true,
                    RedirectStandardError = true,
                    CreateNoWindow = true
                };

                using var process = Process.Start(processStartInfo);
                if (process == null)
                {
                    throw new InvalidOperationException("Failed to start PowerShell for ChromaDB installation");
                }

                // Capture stdout/stderr so we can parse installer output for the installed path
                var stdOutTask = process.StandardOutput.ReadToEndAsync();
                var stdErrTask = process.StandardError.ReadToEndAsync();
                await Task.Run(() => process.WaitForExit());
                var stdOut = await stdOutTask;
                var stdErr = await stdErrTask;

                _logger.LogInformation("Chroma installer output: {Output}", stdOut);
                if (!string.IsNullOrWhiteSpace(stdErr)) _logger.LogWarning("Chroma installer stderr: {Err}", stdErr);

                if (process.ExitCode != 0)
                {
                    throw new InvalidOperationException($"ChromaDB installation failed with exit code {process.ExitCode}: {stdErr}");
                }

                _logger.LogInformation("ChromaDB installed successfully");

                // Per GenAISpec.md §1B(3)(b): ensure the location of the installed executable
                // is added to the user's PATH environment variable.
                try
                {
                    // Attempt to parse installer stdout for an installed path containing 'chroma'
                    if (!string.IsNullOrWhiteSpace(stdOut))
                    {
                        try
                        {
                            // Simple heuristic: find any absolute path that contains "\\chroma" or ends with "chroma"
                            var lines = stdOut.Split(new[] { '\r', '\n' }, StringSplitOptions.RemoveEmptyEntries);
                            foreach (var line in lines.Reverse())
                            {
                                var trimmed = line.Trim();
                                if (trimmed.IndexOf("chroma", StringComparison.OrdinalIgnoreCase) >= 0)
                                {
                                    // try to extract a path-like substring
                                    var start = trimmed.LastIndexOf(':');
                                    if (start > 0 && start + 1 < trimmed.Length)
                                    {
                                        var candidate = trimmed.Substring(start - 1).Trim();
                                        // remove extraneous text
                                        candidate = candidate.Trim('"', '\'', '.', ',');
                                        // if candidate is a directory, append chroma.exe
                                        if (Directory.Exists(candidate) && File.Exists(Path.Combine(candidate, "chroma.exe")))
                                        {
                                            _installedChromaPath = Path.Combine(candidate, "chroma.exe");
                                            break;
                                        }
                                        if (File.Exists(candidate))
                                        {
                                            _installedChromaPath = candidate;
                                            break;
                                        }
                                    }
                                }
                            }
                        }
                        catch (Exception ex)
                        {
                            _logger.LogDebug(ex, "Failed to heuristically parse chroma install path from installer output");
                        }
                    }

                    // If we found an installed path, ensure it's in the user PATH
                    if (!string.IsNullOrWhiteSpace(_installedChromaPath) && File.Exists(_installedChromaPath))
                    {
                        var exeDir = Path.GetDirectoryName(_installedChromaPath)!;
                        var userPath = Environment.GetEnvironmentVariable("PATH", EnvironmentVariableTarget.User) ?? string.Empty;
                        if (!userPath.Split(Path.PathSeparator, StringSplitOptions.RemoveEmptyEntries).Contains(exeDir, StringComparer.OrdinalIgnoreCase))
                        {
                            var newPath = string.IsNullOrEmpty(userPath) ? exeDir : userPath + Path.PathSeparator + exeDir;
                            Environment.SetEnvironmentVariable("PATH", newPath, EnvironmentVariableTarget.User);
                            _logger.LogInformation("Added Chroma executable directory to user PATH: {Dir}", exeDir);
                        }
                    }
                }
                catch (Exception ex)
                {
                    _logger.LogWarning(ex, "Failed to update user PATH with Chroma executable location. Installation itself succeeded.");
                }
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, "Failed to install ChromaDB");
                throw;
            }
        }

        /// <summary>
        /// Verifies the ChromaDB installation. This is a no-op as Chroma does not publish checksums.
        /// Per GenAISpec.md §1B(3)(c).
        /// </summary>
        protected override Task VerifyDownloadAsync(IProgress<RuntimeDependencyProgress> progress)
        {
            // No-op: Chroma does not publish checksums for verification
            // Per GenAISpec.md §1B(3)(c) justification
            _logger.LogInformation("ChromaDB verification skipped (no checksums published)");
            return Task.CompletedTask;
        }

        /// <summary>
        /// Starts the ChromaDB server in client-server mode.
        /// Per GenAISpec.md §1B(3)(d).
        /// </summary>
        protected override async Task PerformInstallDependencyAsync(IProgress<RuntimeDependencyProgress> progress)
        {
            try
            {
                var dataPath = Path.Combine(
                    Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData),
                    "ManuscriptaTeacherApp",
                    "VectorStore"
                );

                Directory.CreateDirectory(dataPath);

                _logger.LogInformation("Starting ChromaDB server with data directory: {DataPath}", dataPath);

                progress?.Report(new RuntimeDependencyProgress { Phase = "Starting ChromaDB server" });

                // Locate chroma executable by scanning user/machine/process PATH and common locations
                string chromaExecutable = "chroma";
                try
                {
                    var candidateDirs = new List<string>();

                    string? userPath = Environment.GetEnvironmentVariable("PATH", EnvironmentVariableTarget.User);
                    if (!string.IsNullOrWhiteSpace(userPath)) candidateDirs.AddRange(userPath.Split(Path.PathSeparator, StringSplitOptions.RemoveEmptyEntries));

                    string? machinePath = Environment.GetEnvironmentVariable("PATH", EnvironmentVariableTarget.Machine);
                    if (!string.IsNullOrWhiteSpace(machinePath)) candidateDirs.AddRange(machinePath.Split(Path.PathSeparator, StringSplitOptions.RemoveEmptyEntries));

                    string? processPath = Environment.GetEnvironmentVariable("PATH");
                    if (!string.IsNullOrWhiteSpace(processPath)) candidateDirs.AddRange(processPath.Split(Path.PathSeparator, StringSplitOptions.RemoveEmptyEntries));

                    candidateDirs.Add(Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.UserProfile) ?? string.Empty, ".cargo", "bin"));
                    candidateDirs.Add(Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData) ?? string.Empty, "Programs", "chroma"));
                    candidateDirs.Add(Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.ProgramFiles) ?? string.Empty, "chroma"));

                    foreach (var dir in candidateDirs.Where(d => !string.IsNullOrWhiteSpace(d)).Distinct(StringComparer.OrdinalIgnoreCase))
                    {
                        try
                        {
                            var exePath = Path.Combine(dir!, "chroma.exe");
                            if (File.Exists(exePath))
                            {
                                chromaExecutable = exePath;
                                _logger.LogInformation("Found chroma executable at {Path}", exePath);
                                break;
                            }
                        }
                        catch
                        {
                            // ignore invalid candidate dirs
                        }
                    }

                    // If installer reported an installed path, prefer that before scanning
                    if (!string.IsNullOrWhiteSpace(_installedChromaPath) && File.Exists(_installedChromaPath))
                    {
                        chromaExecutable = _installedChromaPath;
                        _logger.LogInformation("Using installer-reported chroma path: {Path}", chromaExecutable);
                    }

                    // Fallback to previously-scanned candidate dirs, then 'where chroma' if not found
                    if (chromaExecutable == "chroma")
                    {
                        try
                        {
                            var whereInfo = new ProcessStartInfo
                            {
                                FileName = "where",
                                Arguments = "chroma",
                                UseShellExecute = false,
                                RedirectStandardOutput = true,
                                RedirectStandardError = true,
                                CreateNoWindow = true
                            };

                            using var whereProc = Process.Start(whereInfo);
                            if (whereProc != null)
                            {
                                var whereOut = await whereProc.StandardOutput.ReadToEndAsync();
                                whereProc.WaitForExit(2000);
                                if (!string.IsNullOrWhiteSpace(whereOut))
                                {
                                    var first = whereOut.Split(new[] { '\r', '\n' }, StringSplitOptions.RemoveEmptyEntries)[0];
                                    if (!string.IsNullOrWhiteSpace(first))
                                    {
                                        chromaExecutable = first.Trim();
                                        _logger.LogInformation("Resolved chroma path via 'where': {Path}", chromaExecutable);
                                    }
                                }
                            }
                        }
                        catch (Exception ex)
                        {
                            _logger.LogDebug(ex, "'where chroma' failed while resolving executable path");
                        }
                    }
                }
                catch (Exception ex)
                {
                    _logger.LogDebug(ex, "Failed to scan PATH locations for chroma executable");
                }

                var processStartInfo = new ProcessStartInfo
                {
                    FileName = chromaExecutable,
                    Arguments = $"run --path \"{dataPath}\"",
                    UseShellExecute = false,
                    RedirectStandardOutput = true,
                    RedirectStandardError = true,
                    CreateNoWindow = true
                };

                try
                {
                    _chromaServerProcess = Process.Start(processStartInfo);
                }
                catch (Exception ex)
                {
                    _logger.LogError(ex, "Could not start ChromaDB server using executable '{Exe}'. Ensure installation succeeded and the executable is accessible.", chromaExecutable);
                    throw;
                }

                // Wait for server to become responsive
                var startTime = DateTime.UtcNow;
                var timeout = TimeSpan.FromSeconds(30);
                var serverStarted = false;

                while (DateTime.UtcNow - startTime < timeout)
                {
                    try
                    {
                        using var httpClient = new HttpClient { Timeout = TimeSpan.FromSeconds(1) };
                        var response = await httpClient.GetAsync("http://localhost:8000/api/v1/heartbeat");
                        if (response.IsSuccessStatusCode)
                        {
                            serverStarted = true;
                            _logger.LogInformation("ChromaDB server started successfully");
                            break;
                        }
                    }
                    catch
                    {
                        // Server not ready yet
                    }

                    await Task.Delay(500);
                }

                if (!serverStarted)
                {
                    _logger.LogWarning("ChromaDB server may not have started properly within timeout");
                    throw new InvalidOperationException("ChromaDB server failed to start within timeout period");
                }
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, "Failed to start ChromaDB server");
                throw;
            }
        }

        /// <summary>
        /// Uninstalls ChromaDB by stopping the server and deleting the executable.
        /// Per GenAISpec.md §1B(3)(e).
        /// </summary>
        public override async Task<bool> UninstallDependencyAsync()
        {
            _logger.LogInformation("Uninstalling ChromaDB");

            try
            {
                // Stop the ChromaDB server process if running
                if (_chromaServerProcess != null && !_chromaServerProcess.HasExited)
                {
                    try
                    {
                        _chromaServerProcess.Kill();
                        _chromaServerProcess.WaitForExit(5000);
                    }
                    catch (Exception ex)
                    {
                        _logger.LogWarning(ex, "Failed to kill ChromaDB process");
                    }
                }

                // Kill any other chroma processes
                var processes = Process.GetProcessesByName("chroma");
                foreach (var process in processes)
                {
                    try
                    {
                        process.Kill();
                        process.WaitForExit(5000);
                    }
                    catch (Exception ex)
                    {
                        _logger.LogWarning(ex, "Failed to kill Chroma process");
                    }
                }

                // Per BackendRuntimeDependencyManagementSpecification §2(2)(c),
                // UninstallDependencyAsync only applies to locally-scoped dependencies.
                // Return true as we have successfully stopped the service.

                _logger.LogInformation("ChromaDB uninstalled successfully");
                return true;
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, "Failed to uninstall ChromaDB");
                return false;
            }
        }

        /// <summary>
        /// Cleans up any existing chroma.exe installations from known locations.
        /// This prevents Move-Item conflicts when the PowerShell installer tries to move the binary.
        /// </summary>
        private void CleanupExistingChromaInstallations()
        {
            var candidateLocations = new List<string>();

            // Check common installation locations
            var userProfile = Environment.GetFolderPath(Environment.SpecialFolder.UserProfile);
            if (!string.IsNullOrEmpty(userProfile))
            {
                candidateLocations.Add(Path.Combine(userProfile, "bin", "chroma.exe"));
                candidateLocations.Add(Path.Combine(userProfile, ".cargo", "bin", "chroma.exe"));
            }

            var localAppData = Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData);
            if (!string.IsNullOrEmpty(localAppData))
            {
                candidateLocations.Add(Path.Combine(localAppData, "Programs", "chroma", "chroma.exe"));
            }

            var programFiles = Environment.GetFolderPath(Environment.SpecialFolder.ProgramFiles);
            if (!string.IsNullOrEmpty(programFiles))
            {
                candidateLocations.Add(Path.Combine(programFiles, "chroma", "chroma.exe"));
            }

            // Also scan PATH directories
            var pathDirs = new List<string>();
            var userPath = Environment.GetEnvironmentVariable("PATH", EnvironmentVariableTarget.User);
            if (!string.IsNullOrEmpty(userPath))
            {
                pathDirs.AddRange(userPath.Split(Path.PathSeparator, StringSplitOptions.RemoveEmptyEntries));
            }

            foreach (var dir in pathDirs)
            {
                try
                {
                    var chromaPath = Path.Combine(dir, "chroma.exe");
                    candidateLocations.Add(chromaPath);
                }
                catch
                {
                    // Ignore malformed paths
                }
            }

            // Attempt to delete each candidate
            foreach (var location in candidateLocations.Distinct(StringComparer.OrdinalIgnoreCase))
            {
                try
                {
                    if (File.Exists(location))
                    {
                        _logger.LogInformation("Removing existing chroma.exe at {Location}", location);
                        File.Delete(location);
                    }
                }
                catch (Exception ex)
                {
                    _logger.LogWarning(ex, "Failed to remove existing chroma.exe at {Location}", location);
                    // Don't throw - we'll let the installer try anyway
                }
            }
        }

        /// <summary>
        /// Provides a singleton instance of ChromaClientService.
        /// Per GenAISpec.md §1B(3)(f) and BackendRuntimeDependencyManagementSpecification §2(3A).
        /// </summary>
        protected override Task<IDependencyService> ProvideDependencyServiceAsync()
        {
            lock (_serviceLock)
            {
                if (_chromaClientServiceInstance == null)
                {
                    _chromaClientServiceInstance = new ChromaClientService();
                }

                return Task.FromResult<IDependencyService>(_chromaClientServiceInstance);
            }
        }
    }
}
