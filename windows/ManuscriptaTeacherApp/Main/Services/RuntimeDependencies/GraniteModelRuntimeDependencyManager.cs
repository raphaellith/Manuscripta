using System;
using System.Diagnostics;
using System.Net.Http;
using System.Text.Json;
using System.Threading.Tasks;
using Main.Models;
using Main.Services.GenAI;
using Main.Services.RuntimeDependencies;
using Microsoft.Extensions.Logging;

namespace Main.Services.RuntimeDependencies
{
    /// <summary>
    /// Manages the availability and installation of the IBM Granite 4.0 model for Ollama.
    /// Per GenAISpec.md §1D.
    /// </summary>
    public class GraniteModelRuntimeDependencyManager : RuntimeDependencyManagerBase
    {
        private readonly ILogger<GraniteModelRuntimeDependencyManager> _logger;
        private readonly HttpClient _httpClient;
        private readonly IInferenceRuntimeSelector _inferenceRuntimeSelector;
        private static OllamaClientService? _ollamaClientServiceInstance;
        private readonly object _serviceLock = new object();

        public override string DependencyId => "granite4";

        public GraniteModelRuntimeDependencyManager(
            ILogger<GraniteModelRuntimeDependencyManager> logger,
            HttpClient httpClient,
            IInferenceRuntimeSelector inferenceRuntimeSelector)
        {
            _logger = logger;
            _httpClient = httpClient;
            _inferenceRuntimeSelector = inferenceRuntimeSelector;
        }

        /// <summary>
        /// Starts the external process used to pull the model. Exposed
        /// virtually so tests can substitute a dummy process without touching
        /// the filesystem or requiring Ollama.
        /// </summary>
        protected virtual Task<Process> StartPullProcessAsync(string args)
        {
            // Always use Standard Ollama binary for GGUF model pull. Per §1D(3)(b).
            var dirName = "ollama";

            // Ollama extraction directory
            var binDir = Path.Combine(
                Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData),
                "ManuscriptaTeacherApp",
                "bin",
                dirName
            );

            // Check if ollama.exe exists in the expected location
            var ollamaExePath = Path.Combine(binDir, "ollama.exe");
            if (!File.Exists(ollamaExePath))
            {
                throw new InvalidOperationException(
                    $"Ollama executable not found at {ollamaExePath}. "
                    + "Ensure Ollama runtime dependency is installed before downloading models.");
            }

            var psi = new ProcessStartInfo
            {
                FileName = ollamaExePath,  // Use full path to ensure it's found
                Arguments = args,
                UseShellExecute = false,
                RedirectStandardOutput = true,
                RedirectStandardError = true,
                CreateNoWindow = true
            };

            // Ensure the Ollama extraction directory is on the PATH so that subprocess calls
            // to ollama will work correctly from within the pulled models.
            var pathEnv = Environment.GetEnvironmentVariable("PATH", EnvironmentVariableTarget.Process) ?? string.Empty;
            var pathItems = pathEnv.Split(Path.PathSeparator, StringSplitOptions.RemoveEmptyEntries).ToList();
            if (!pathItems.Any(p => string.Equals(p, binDir, StringComparison.OrdinalIgnoreCase)))
            {
                pathItems.Add(binDir);
            }
            psi.EnvironmentVariables["PATH"] = string.Join(Path.PathSeparator, pathItems);

            return Task.FromResult(
                Process.Start(psi) ?? throw new InvalidOperationException("Failed to start ollama process"));
        }

        /// <summary>
        /// Checks if the IBM Granite 4.0 GGUF model is available on Standard Ollama.
        /// Per GenAISpec.md §1D(3)(a). Always targets port 11434.
        /// </summary>
        public override async Task<bool> CheckDependencyAvailabilityAsync()
        {
            try
            {
                var baseUrl = _inferenceRuntimeSelector.GetStandardOllamaBaseUrl();
                var response = await _httpClient.GetAsync($"{baseUrl}/api/tags");
                if (!response.IsSuccessStatusCode)
                {
                    return false;
                }

                var content = await response.Content.ReadAsStringAsync();
                var doc = JsonDocument.Parse(content);

                if (doc.RootElement.TryGetProperty("models", out var models))
                {
                    foreach (var model in models.EnumerateArray())
                    {
                        if (model.TryGetProperty("name", out var name))
                        {
                            var modelName = name.GetString();
                            if (modelName == "granite4" || modelName?.StartsWith("granite4") == true)
                            {
                                return true;
                            }
                        }
                    }
                }

                return false;
            }
            catch
            {
                return false;
            }
        }

        /// <summary>
        /// Downloads the IBM Granite 4.0 model via `ollama pull granite4`.
        /// Per GenAISpec.md §1D(3)(b).
        /// </summary>
        protected override async Task DownloadDependencyAsync(IProgress<RuntimeDependencyProgress> progress)
        {
            _logger.LogInformation("Downloading IBM Granite 4.0 model");

            try
            {
                using var process = await StartPullProcessAsync("pull granite4");

                // read output and error asynchronously to avoid blocking when buffers fill
                var stdOut = new List<string>();
                var stdErr = new List<string>();

                process.OutputDataReceived += (s, e) =>
                {
                    if (e.Data != null)
                    {
                        stdOut.Add(e.Data);
                        _logger.LogDebug("[ollama] {Line}", e.Data);
                    }
                };
                process.ErrorDataReceived += (s, e) =>
                {
                    if (e.Data != null)
                    {
                        stdErr.Add(e.Data);
                        _logger.LogWarning("[ollama err] {Line}", e.Data);
                    }
                };

                process.BeginOutputReadLine();
                process.BeginErrorReadLine();

                await Task.Run(() => process.WaitForExit());

                if (process.ExitCode != 0)
                {
                    var errorOutput = string.Join(Environment.NewLine, stdErr);
                    throw new InvalidOperationException($"ollama pull granite4 failed with exit code {process.ExitCode}: {errorOutput}");
                }

                _logger.LogInformation("IBM Granite 4.0 model downloaded successfully");
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, "Failed to download IBM Granite 4.0 model");
                throw;
            }
        }

        /// <summary>
        /// Verifies the model download. This is a no-op as Ollama verifies internally.
        /// Per GenAISpec.md §1D(3)(c). [Deleted per specification.]
        /// </summary>
        protected override Task VerifyDownloadAsync(IProgress<RuntimeDependencyProgress> progress)
        {
            // No-op: Ollama verifies model integrity internally during pull
            _logger.LogInformation("IBM Granite 4.0 model verification skipped (Ollama verifies internally)");
            return Task.CompletedTask;
        }

        /// <summary>
        /// Verifies the model is available after download.
        /// Per GenAISpec.md §1D(3)(d).
        /// </summary>
        protected override async Task PerformInstallDependencyAsync(IProgress<RuntimeDependencyProgress> progress)
        {
            _logger.LogInformation("Verifying IBM Granite 4.0 model installation");

            try
            {
                progress?.Report(new RuntimeDependencyProgress { Phase = "Verifying model" });

                var isAvailable = await CheckDependencyAvailabilityAsync();
                if (!isAvailable)
                {
                    throw new InvalidOperationException("IBM Granite 4.0 model is still not available after download");
                }

                _logger.LogInformation("IBM Granite 4.0 model installation verified");
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, "Failed to verify IBM Granite 4.0 model installation");
                throw;
            }
        }

        /// <summary>
        /// Uninstalls the IBM Granite 4.0 model via `ollama rm granite4`.
        /// Per GenAISpec.md §1D(3)(e).
        /// </summary>
        public override async Task<bool> UninstallDependencyAsync()
        {
            _logger.LogInformation("Uninstalling IBM Granite 4.0 model");

            try
            {
                // Always use Standard Ollama binary for GGUF model removal. Per §1D(3)(e).
                var dirName = "ollama";

                var binDir = Path.Combine(
                    Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData),
                    "ManuscriptaTeacherApp",
                    "bin",
                    dirName
                );
                
                var ollamaExePath = Path.Combine(binDir, "ollama.exe");
                if (!File.Exists(ollamaExePath))
                {
                    _logger.LogWarning("ollama.exe not found during uninstall. Skipping rm.");
                    return true;
                }

                var processStartInfo = new ProcessStartInfo
                {
                    FileName = ollamaExePath,
                    Arguments = "rm granite4",
                    UseShellExecute = false,
                    RedirectStandardOutput = true,
                    RedirectStandardError = true,
                    CreateNoWindow = true
                };

                using var process = Process.Start(processStartInfo);
                if (process == null)
                {
                    throw new InvalidOperationException("Failed to start ollama rm process");
                }

                await Task.Run(() => process.WaitForExit());

                if (process.ExitCode != 0)
                {
                    var errorOutput = await process.StandardError.ReadToEndAsync();
                    _logger.LogWarning("ollama rm granite4 exited with code {ExitCode}: {Error}", process.ExitCode, errorOutput);
                }

                _logger.LogInformation("IBM Granite 4.0 model uninstalled successfully");
                return true;
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, "Failed to uninstall IBM Granite 4.0 model");
                return false;
            }
        }

        /// <summary>
        /// Provides a singleton instance of OllamaClientService.
        /// Per GenAISpec.md §1D(3)(f) and BackendRuntimeDependencyManagementSpecification §2(3A).
        /// </summary>
        protected override Task<IDependencyService> ProvideDependencyServiceAsync()
        {
            lock (_serviceLock)
            {
                if (_ollamaClientServiceInstance == null)
                {
                    _ollamaClientServiceInstance = new OllamaClientService(_inferenceRuntimeSelector);
                }

                return Task.FromResult<IDependencyService>(_ollamaClientServiceInstance);
            }
        }
    }
}
