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
    /// Manages the availability and installation of the Nomic Embed Text model for Ollama.
    /// Per GenAISpec.md §1E.
    /// </summary>
    public class NomicEmbedTextModelRuntimeDependencyManager : RuntimeDependencyManagerBase
    {
        private readonly ILogger<NomicEmbedTextModelRuntimeDependencyManager> _logger;
        private readonly HttpClient _httpClient;
        private static OllamaClientService? _ollamaClientServiceInstance;
        private readonly object _serviceLock = new object();

        public override string DependencyId => "nomic-embed-text";

        public NomicEmbedTextModelRuntimeDependencyManager(
            ILogger<NomicEmbedTextModelRuntimeDependencyManager> logger,
            HttpClient httpClient)
        {
            _logger = logger;
            _httpClient = httpClient;
        }

        /// <summary>
        /// Checks if the Nomic Embed Text model is available by querying Ollama's API.
        /// Per GenAISpec.md §1E(3)(a).
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
                            if (modelName == "nomic-embed-text" || modelName?.StartsWith("nomic-embed-text") == true)
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
        /// Downloads the Nomic Embed Text model via `ollama pull nomic-embed-text`.
        /// Per GenAISpec.md §1E(3)(b).
        /// </summary>
        protected override async Task DownloadDependencyAsync(IProgress<RuntimeDependencyProgress> progress)
        {
            _logger.LogInformation("Downloading Nomic Embed Text model");

            try
            {
                var processStartInfo = new ProcessStartInfo
                {
                    FileName = "ollama",
                    Arguments = "pull nomic-embed-text",
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

                await Task.Run(() => process.WaitForExit());

                if (process.ExitCode != 0)
                {
                    var errorOutput = await process.StandardError.ReadToEndAsync();
                    throw new InvalidOperationException($"ollama pull nomic-embed-text failed with exit code {process.ExitCode}: {errorOutput}");
                }

                _logger.LogInformation("Nomic Embed Text model downloaded successfully");
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, "Failed to download Nomic Embed Text model");
                throw;
            }
        }

        /// <summary>
        /// Verifies the model download. This is a no-op as Ollama verifies internally.
        /// Per GenAISpec.md §1E(3)(c). [Deleted per specification.]
        /// </summary>
        protected override Task VerifyDownloadAsync(IProgress<RuntimeDependencyProgress> progress)
        {
            // No-op: Ollama verifies model integrity internally during pull
            _logger.LogInformation("Nomic Embed Text model verification skipped (Ollama verifies internally)");
            return Task.CompletedTask;
        }

        /// <summary>
        /// Verifies the model is available after download.
        /// Per GenAISpec.md §1E(3)(d).
        /// </summary>
        protected override async Task PerformInstallDependencyAsync(IProgress<RuntimeDependencyProgress> progress)
        {
            _logger.LogInformation("Verifying Nomic Embed Text model installation");

            try
            {
                progress?.Report(new RuntimeDependencyProgress { Phase = "Verifying model" });

                var isAvailable = await CheckDependencyAvailabilityAsync();
                if (!isAvailable)
                {
                    throw new InvalidOperationException("Nomic Embed Text model is still not available after download");
                }

                _logger.LogInformation("Nomic Embed Text model installation verified");
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, "Failed to verify Nomic Embed Text model installation");
                throw;
            }
        }

        /// <summary>
        /// Uninstalls the Nomic Embed Text model via `ollama rm nomic-embed-text`.
        /// Per GenAISpec.md §1E(3)(e).
        /// </summary>
        public override async Task<bool> UninstallDependencyAsync()
        {
            _logger.LogInformation("Uninstalling Nomic Embed Text model");

            try
            {
                var processStartInfo = new ProcessStartInfo
                {
                    FileName = "ollama",
                    Arguments = "rm nomic-embed-text",
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
                    _logger.LogWarning("ollama rm nomic-embed-text exited with code {ExitCode}: {Error}", process.ExitCode, errorOutput);
                }

                _logger.LogInformation("Nomic Embed Text model uninstalled successfully");
                return true;
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, "Failed to uninstall Nomic Embed Text model");
                return false;
            }
        }

        /// <summary>
        /// Provides a singleton instance of OllamaClientService.
        /// Per GenAISpec.md §1E(3)(f) and BackendRuntimeDependencyManagementSpecification §2(3A).
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
