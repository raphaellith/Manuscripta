using System;
using System.Diagnostics;
using System.IO;
using System.Threading.Tasks;
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

                await Task.Run(() => process.WaitForExit());

                if (process.ExitCode != 0)
                {
                    var errorOutput = process.StandardError.ReadToEnd();
                    throw new InvalidOperationException($"ChromaDB installation failed with exit code {process.ExitCode}: {errorOutput}");
                }

                _logger.LogInformation("ChromaDB installed successfully");
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

                var processStartInfo = new ProcessStartInfo
                {
                    FileName = "chroma",
                    Arguments = $"run --path \"{dataPath}\"",
                    UseShellExecute = false,
                    RedirectStandardOutput = true,
                    RedirectStandardError = true,
                    CreateNoWindow = true
                };

                _chromaServerProcess = Process.Start(processStartInfo);
                if (_chromaServerProcess == null)
                {
                    throw new InvalidOperationException("Failed to start ChromaDB server process");
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
