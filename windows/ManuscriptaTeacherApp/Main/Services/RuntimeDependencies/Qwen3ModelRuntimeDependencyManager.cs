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
    /// Manages the availability and installation of the Qwen3 8B model for Ollama.
    /// Per GenAISpec.md §1C.
    /// </summary>
    public class Qwen3ModelRuntimeDependencyManager : RuntimeDependencyManagerBase
    {
        private readonly ILogger<Qwen3ModelRuntimeDependencyManager> _logger;
        private readonly HttpClient _httpClient;
        private static OllamaClientService? _ollamaClientServiceInstance;
        private readonly object _serviceLock = new object();

        public override string DependencyId => "qwen3:8b";

        public Qwen3ModelRuntimeDependencyManager(
            ILogger<Qwen3ModelRuntimeDependencyManager> logger,
            HttpClient httpClient)
        {
            _logger = logger;
            _httpClient = httpClient;
        }

        /// <summary>
        /// Checks if the Qwen3 8B model is available by querying Ollama's API.
        /// Per GenAISpec.md §1C(3)(a).
        /// </summary>
        public override async Task<bool> CheckDependencyAvailabilityAsync()
        {
            try
            {
                var response = await _httpClient.GetAsync("http://localhost:11434/api/tags");
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
                            if (modelName == "qwen3:8b" || modelName?.StartsWith("qwen3:8b") == true)
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
        /// Downloads the Qwen3 8B model via `ollama pull qwen3:8b`.
        /// Per GenAISpec.md §1C(3)(b).
        /// </summary>
        protected override async Task DownloadDependencyAsync(IProgress<RuntimeDependencyProgress> progress)
        {
            _logger.LogInformation("Downloading Qwen3 8B model");

            try
            {
                var processStartInfo = new ProcessStartInfo
                {
                    FileName = "ollama",
                    Arguments = "pull qwen3:8b",
                    UseShellExecute = false,
                    RedirectStandardOutput = true,
                    RedirectStandardError = true,
                    CreateNoWindow = true
                };

                using var process = Process.Start(processStartInfo);
                if (process == null)
                {
                    throw new InvalidOperationException("Failed to start ollama pull process");
                }

                // Read output to track progress
                var outputTask = process.StandardOutput.ReadToEndAsync();
                await Task.Run(() => process.WaitForExit());

                if (process.ExitCode != 0)
                {
                    var errorOutput = await process.StandardError.ReadToEndAsync();
                    throw new InvalidOperationException($"ollama pull qwen3:8b failed with exit code {process.ExitCode}: {errorOutput}");
                }

                _logger.LogInformation("Qwen3 8B model downloaded successfully");
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, "Failed to download Qwen3 8B model");
                throw;
            }
        }

        /// <summary>
        /// Verifies the model download. This is a no-op as Ollama verifies internally.
        /// Per GenAISpec.md §1C(3)(c).
        /// </summary>
        protected override Task VerifyDownloadAsync(IProgress<RuntimeDependencyProgress> progress)
        {
            // No-op: Ollama verifies model integrity internally during pull
            // Per GenAISpec.md §1C(3)(c) justification
            _logger.LogInformation("Qwen3 8B model verification skipped (Ollama verifies internally)");
            return Task.CompletedTask;
        }

        /// <summary>
        /// Verifies the model is available after download.
        /// Per GenAISpec.md §1C(3)(d).
        /// </summary>
        protected override async Task PerformInstallDependencyAsync(IProgress<RuntimeDependencyProgress> progress)
        {
            _logger.LogInformation("Verifying Qwen3 8B model installation");

            try
            {
                progress?.Report(new RuntimeDependencyProgress { Phase = "Verifying model" });

                var isAvailable = await CheckDependencyAvailabilityAsync();
                if (!isAvailable)
                {
                    throw new InvalidOperationException("Qwen3 8B model is still not available after download");
                }

                _logger.LogInformation("Qwen3 8B model installation verified");
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, "Failed to verify Qwen3 8B model installation");
                throw;
            }
        }

        /// <summary>
        /// Uninstalls the Qwen3 8B model via `ollama rm qwen3:8b`.
        /// Per GenAISpec.md §1C(3)(e).
        /// </summary>
        public override async Task<bool> UninstallDependencyAsync()
        {
            _logger.LogInformation("Uninstalling Qwen3 8B model");

            try
            {
                var processStartInfo = new ProcessStartInfo
                {
                    FileName = "ollama",
                    Arguments = "rm qwen3:8b",
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
                    _logger.LogWarning("ollama rm qwen3:8b exited with code {ExitCode}: {Error}", process.ExitCode, errorOutput);
                }

                _logger.LogInformation("Qwen3 8B model uninstalled successfully");
                return true;
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, "Failed to uninstall Qwen3 8B model");
                return false;
            }
        }

        /// <summary>
        /// Provides a singleton instance of OllamaClientService.
        /// Per GenAISpec.md §1C(3)(f) and BackendRuntimeDependencyManagementSpecification §2(3A).
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
    }
}
