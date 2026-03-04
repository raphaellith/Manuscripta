using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.IO;
using System.Linq;
using System.Net.Http;
using System.Text.Json;
using System.Threading.Tasks;
using Main.Models;
using Main.Services.GenAI;
using Microsoft.Extensions.Logging;

namespace Main.Services.RuntimeDependencies
{
    /// <summary>
    /// Manages the availability and installation of the IBM Granite 4.0 model
    /// for OV-Ollama. Uses `ollama pull` via the OV-Ollama binary, which
    /// handles GGUF-to-IR conversion internally.
    /// Per GenAISpec.md §1DA.
    /// </summary>
    public class GraniteOVModelRuntimeDependencyManager : RuntimeDependencyManagerBase
    {
        private readonly ILogger<GraniteOVModelRuntimeDependencyManager> _logger;
        private readonly HttpClient _httpClient;
        private readonly IInferenceRuntimeSelector _inferenceRuntimeSelector;
        private static OllamaClientService? _ollamaClientServiceInstance;
        private readonly object _serviceLock = new object();

        /// <summary>
        /// The model name used with OV-Ollama.
        /// </summary>
        private const string MODEL_TAG = "granite4";

        public override string DependencyId => "granite4-openvino";

        public GraniteOVModelRuntimeDependencyManager(
            ILogger<GraniteOVModelRuntimeDependencyManager> logger,
            HttpClient httpClient,
            IInferenceRuntimeSelector inferenceRuntimeSelector)
        {
            _logger = logger;
            _httpClient = httpClient;
            _inferenceRuntimeSelector = inferenceRuntimeSelector;
        }

        /// <summary>
        /// Gets the OV-Ollama binary directory.
        /// </summary>
        private string GetOvOllamaBinDir()
        {
            return Path.Combine(
                Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData),
                "ManuscriptaTeacherApp",
                "bin",
                "ollama-openvino");
        }

        /// <summary>
        /// Gets the isolated OLLAMA_MODELS directory for OV-Ollama.
        /// </summary>
        private string GetOvOllamaModelsDir()
        {
            return Path.Combine(GetOvOllamaBinDir(), "models");
        }

        /// <summary>
        /// Checks if the IBM Granite 4.0 model is available on OV-Ollama.
        /// Per GenAISpec.md §1DA(3)(a). Always targets port 11435.
        /// </summary>
        public override async Task<bool> CheckDependencyAvailabilityAsync()
        {
            try
            {
                const string ovOllamaBaseUrl = "http://localhost:11435";
                var response = await _httpClient.GetAsync($"{ovOllamaBaseUrl}/api/tags");
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
                            if (modelName == MODEL_TAG || modelName?.StartsWith(MODEL_TAG) == true)
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
        /// Starts an OV-Ollama process with the given arguments.
        /// Sets OLLAMA_HOST to port 11435 and OLLAMA_MODELS to the isolated directory.
        /// </summary>
        protected virtual Task<Process> StartOvPullProcessAsync(string args)
        {
            var binDir = GetOvOllamaBinDir();
            var ollamaExePath = Path.Combine(binDir, "ollama.exe");

            if (!File.Exists(ollamaExePath))
            {
                throw new InvalidOperationException(
                    $"OV-Ollama executable not found at {ollamaExePath}. "
                    + "Ensure OpenVINO Ollama runtime dependency is installed first.");
            }

            var modelsDir = GetOvOllamaModelsDir();
            Directory.CreateDirectory(modelsDir);

            var psi = new ProcessStartInfo
            {
                FileName = ollamaExePath,
                Arguments = args,
                UseShellExecute = false,
                RedirectStandardOutput = true,
                RedirectStandardError = true,
                CreateNoWindow = true
            };

            psi.EnvironmentVariables["OLLAMA_HOST"] = "127.0.0.1:11435";
            psi.EnvironmentVariables["OLLAMA_MODELS"] = modelsDir;

            // Ensure the OV-Ollama directory is on the PATH
            var pathEnv = Environment.GetEnvironmentVariable("PATH", EnvironmentVariableTarget.Process) ?? string.Empty;
            var pathItems = pathEnv.Split(Path.PathSeparator, StringSplitOptions.RemoveEmptyEntries).ToList();
            if (!pathItems.Any(p => string.Equals(p, binDir, StringComparison.OrdinalIgnoreCase)))
            {
                pathItems.Add(binDir);
            }
            psi.EnvironmentVariables["PATH"] = string.Join(Path.PathSeparator, pathItems);

            return Task.FromResult(
                Process.Start(psi) ?? throw new InvalidOperationException("Failed to start OV-Ollama process"));
        }

        /// <summary>
        /// Downloads the IBM Granite 4.0 model via OV-Ollama's `ollama pull`.
        /// OV-Ollama handles GGUF-to-IR conversion internally.
        /// Per GenAISpec.md §1DA(3)(b).
        /// </summary>
        protected override async Task DownloadDependencyAsync(IProgress<RuntimeDependencyProgress> progress)
        {
            _logger.LogInformation("Pulling IBM Granite 4.0 model via OV-Ollama");

            // Verify OV-Ollama server is running before attempting pull
            try
            {
                var serverCheck = await _httpClient.GetAsync("http://localhost:11435/api/version");
                if (!serverCheck.IsSuccessStatusCode)
                {
                    throw new InvalidOperationException(
                        "OV-Ollama server is not responding on port 11435. "
                        + "Ensure the OpenVINO Ollama runtime dependency is installed and running.");
                }
            }
            catch (HttpRequestException)
            {
                throw new InvalidOperationException(
                    "OV-Ollama server is not running on port 11435. "
                    + "Ensure the OpenVINO Ollama runtime dependency is installed and running.");
            }

            progress?.Report(new RuntimeDependencyProgress
            {
                Phase = "Downloading",
                ProgressPercentage = 0
            });

            try
            {
                using var process = await StartOvPullProcessAsync($"pull {MODEL_TAG}");

                var stdOut = new List<string>();
                var stdErr = new List<string>();

                process.OutputDataReceived += (s, e) =>
                {
                    if (e.Data != null)
                    {
                        stdOut.Add(e.Data);
                        _logger.LogDebug("[ov-ollama] {Line}", e.Data);
                    }
                };
                process.ErrorDataReceived += (s, e) =>
                {
                    if (e.Data != null)
                    {
                        stdErr.Add(e.Data);
                        _logger.LogWarning("[ov-ollama err] {Line}", e.Data);
                    }
                };

                process.BeginOutputReadLine();
                process.BeginErrorReadLine();

                // Timeout after 10 minutes to prevent infinite hanging
                var exited = await Task.Run(() => process.WaitForExit(600_000));
                if (!exited)
                {
                    try { process.Kill(); } catch { }
                    throw new TimeoutException(
                        $"OV-Ollama pull {MODEL_TAG} timed out after 10 minutes. "
                        + "The OV-Ollama server may not be functioning correctly.");
                }

                if (process.ExitCode != 0)
                {
                    var errorOutput = string.Join(Environment.NewLine, stdErr);
                    throw new InvalidOperationException(
                        $"OV-Ollama pull {MODEL_TAG} failed with exit code {process.ExitCode}: {errorOutput}");
                }

                _logger.LogInformation("IBM Granite 4.0 model pulled via OV-Ollama successfully");
            }
            catch (Exception ex) when (ex is not TimeoutException && ex is not InvalidOperationException)
            {
                _logger.LogError(ex, "Failed to pull IBM Granite 4.0 model via OV-Ollama");
                throw;
            }
        }

        /// <summary>
        /// Verification is a no-op — OV-Ollama verifies model integrity internally during pull.
        /// Per GenAISpec.md §1DA(3)(c).
        /// </summary>
        protected override Task VerifyDownloadAsync(IProgress<RuntimeDependencyProgress> progress)
        {
            _logger.LogInformation("IBM Granite 4.0 OV model verification skipped (OV-Ollama verifies internally)");
            return Task.CompletedTask;
        }

        /// <summary>
        /// Verifies the model is available on OV-Ollama after pull.
        /// Per GenAISpec.md §1DA(3)(d).
        /// </summary>
        protected override async Task PerformInstallDependencyAsync(IProgress<RuntimeDependencyProgress> progress)
        {
            _logger.LogInformation("Verifying IBM Granite 4.0 OV model installation");

            try
            {
                progress?.Report(new RuntimeDependencyProgress { Phase = "Verifying model" });

                var isAvailable = await CheckDependencyAvailabilityAsync();
                if (!isAvailable)
                {
                    throw new InvalidOperationException("IBM Granite 4.0 model is still not available on OV-Ollama after pull");
                }

                _logger.LogInformation("IBM Granite 4.0 OV model installation verified");
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, "Failed to verify IBM Granite 4.0 OV model installation");
                throw;
            }
        }

        /// <summary>
        /// Removes the model from OV-Ollama.
        /// Per GenAISpec.md §1DA(3)(e).
        /// </summary>
        public override async Task<bool> UninstallDependencyAsync()
        {
            _logger.LogInformation("Uninstalling IBM Granite 4.0 OV model");

            try
            {
                using var process = await StartOvPullProcessAsync($"rm {MODEL_TAG}");

                await Task.Run(() => process.WaitForExit());

                if (process.ExitCode != 0)
                {
                    var errorOutput = await process.StandardError.ReadToEndAsync();
                    _logger.LogWarning("OV-Ollama rm {Model} exited with code {ExitCode}: {Error}",
                        MODEL_TAG, process.ExitCode, errorOutput);
                }

                _logger.LogInformation("IBM Granite 4.0 OV model uninstalled successfully");
                return true;
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, "Failed to uninstall IBM Granite 4.0 OV model");
                return false;
            }
        }

        /// <summary>
        /// Provides a singleton instance of OllamaClientService.
        /// Per GenAISpec.md §1DA(3)(f).
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
