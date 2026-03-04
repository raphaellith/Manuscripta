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
    /// Manages the availability and installation of the Qwen3 8B model
    /// in OpenVINO IR format for OV-Ollama.
    /// Per GenAISpec.md §1CA.
    /// </summary>
    public class Qwen3OVModelRuntimeDependencyManager : RuntimeDependencyManagerBase
    {
        private readonly ILogger<Qwen3OVModelRuntimeDependencyManager> _logger;
        private readonly HttpClient _httpClient;
        private readonly IInferenceRuntimeSelector _inferenceRuntimeSelector;
        private static OllamaClientService? _ollamaClientServiceInstance;
        private readonly object _serviceLock = new object();

        /// <summary>
        /// HuggingFace repository for pre-converted Qwen3 8B INT4 IR model.
        /// Per GenAISpec.md Appendix A (QWEN3_IR_HF_REPO).
        /// </summary>
        private const string HF_REPO = "OpenVINO/Qwen3-8B-int4-ov";

        /// <summary>
        /// Local model directory name under IR_MODEL_BASE_DIR.
        /// </summary>
        private const string MODEL_DIR_NAME = "qwen3-8b";

        /// <summary>
        /// The model name as registered with OV-Ollama.
        /// </summary>
        private const string MODEL_TAG = "qwen3:8b";

        public override string DependencyId => "qwen3:8b-openvino";

        public Qwen3OVModelRuntimeDependencyManager(
            ILogger<Qwen3OVModelRuntimeDependencyManager> logger,
            HttpClient httpClient,
            IInferenceRuntimeSelector inferenceRuntimeSelector)
        {
            _logger = logger;
            _httpClient = httpClient;
            _inferenceRuntimeSelector = inferenceRuntimeSelector;
        }

        /// <summary>
        /// Checks if the IR-format Qwen3 8B model is available on OV-Ollama.
        /// Per GenAISpec.md §1CA(3)(a). Always targets port 11435.
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
        /// Gets the local directory where IR model files are stored.
        /// </summary>
        private string GetModelDirectory()
        {
            return Path.Combine(
                Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData),
                "ManuscriptaTeacherApp",
                "bin",
                "models",
                MODEL_DIR_NAME);
        }

        /// <summary>
        /// Gets the path to the OV-Ollama executable.
        /// </summary>
        private string GetOvOllamaExePath()
        {
            return Path.Combine(
                Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData),
                "ManuscriptaTeacherApp",
                "bin",
                "ollama-openvino",
                "ollama.exe");
        }

        /// <summary>
        /// Downloads all files from the HuggingFace repository into the local model directory.
        /// Per GenAISpec.md §1CA(3)(b).
        /// </summary>
        protected override async Task DownloadDependencyAsync(IProgress<RuntimeDependencyProgress> progress)
        {
            _logger.LogInformation("Downloading Qwen3 8B IR model from HuggingFace: {Repo}", HF_REPO);

            progress?.Report(new RuntimeDependencyProgress
            {
                Phase = "Downloading",
                ProgressPercentage = 0
            });

            var modelDir = GetModelDirectory();
            Directory.CreateDirectory(modelDir);

            // List all files in the HF repo
            var treeUrl = $"https://huggingface.co/api/models/{HF_REPO}/tree/main";
            var treeResponse = await _httpClient.GetAsync(treeUrl);
            treeResponse.EnsureSuccessStatusCode();
            var treeJson = await treeResponse.Content.ReadAsStringAsync();
            var files = JsonDocument.Parse(treeJson);

            var fileList = new List<(string path, long size)>();
            foreach (var fileEntry in files.RootElement.EnumerateArray())
            {
                var type = fileEntry.GetProperty("type").GetString();
                if (type == "file")
                {
                    var path = fileEntry.GetProperty("path").GetString()!;
                    var size = fileEntry.TryGetProperty("size", out var sizeEl) ? sizeEl.GetInt64() : 0;
                    fileList.Add((path, size));
                }
            }

            var totalSize = fileList.Sum(f => f.size);
            long downloadedSize = 0;

            for (int i = 0; i < fileList.Count; i++)
            {
                var (filePath, fileSize) = fileList[i];
                var localPath = Path.Combine(modelDir, filePath.Replace('/', Path.DirectorySeparatorChar));
                var localDir = Path.GetDirectoryName(localPath)!;
                Directory.CreateDirectory(localDir);

                var downloadUrl = $"https://huggingface.co/{HF_REPO}/resolve/main/{filePath}";
                _logger.LogInformation("Downloading {File} ({Index}/{Total})", filePath, i + 1, fileList.Count);

                progress?.Report(new RuntimeDependencyProgress
                {
                    Phase = "Downloading",
                    ProgressPercentage = totalSize > 0 ? (int)(downloadedSize * 80 / totalSize) : (int)(i * 80.0 / fileList.Count)
                });

                using var fileResponse = await _httpClient.GetAsync(downloadUrl, HttpCompletionOption.ResponseHeadersRead);
                fileResponse.EnsureSuccessStatusCode();

                using var contentStream = await fileResponse.Content.ReadAsStreamAsync();
                using var fileStream = File.Create(localPath);
                await contentStream.CopyToAsync(fileStream);

                downloadedSize += fileSize;
            }

            progress?.Report(new RuntimeDependencyProgress
            {
                Phase = "Downloading",
                ProgressPercentage = 80
            });

            _logger.LogInformation("Qwen3 8B IR model downloaded successfully to {Dir}", modelDir);
        }

        /// <summary>
        /// Verifies that the IR model directory contains the required model files.
        /// Per GenAISpec.md §1CA(3)(c).
        /// </summary>
        protected override Task VerifyDownloadAsync(IProgress<RuntimeDependencyProgress> progress)
        {
            var modelDir = GetModelDirectory();
            var xmlPath = Path.Combine(modelDir, "openvino_model.xml");
            var binPath = Path.Combine(modelDir, "openvino_model.bin");

            if (!File.Exists(xmlPath) || !File.Exists(binPath))
            {
                throw new InvalidOperationException(
                    $"IR model verification failed: openvino_model.xml or openvino_model.bin not found in {modelDir}");
            }

            _logger.LogInformation("Qwen3 8B IR model verified successfully");
            return Task.CompletedTask;
        }

        /// <summary>
        /// Registers the IR model with OV-Ollama using a Modelfile.
        /// Per GenAISpec.md §1CA(3)(d).
        /// </summary>
        protected override async Task PerformInstallDependencyAsync(IProgress<RuntimeDependencyProgress> progress)
        {
            var modelDir = GetModelDirectory();
            var ollamaExePath = GetOvOllamaExePath();

            if (!File.Exists(ollamaExePath))
            {
                throw new InvalidOperationException(
                    $"OV-Ollama executable not found at {ollamaExePath}. "
                    + "Ensure OpenVINO Ollama runtime dependency is installed first.");
            }

            // Create Modelfile that references the IR directory
            var modelfilePath = Path.Combine(modelDir, "Modelfile");
            var modelfileContent = $"FROM {modelDir}";
            await File.WriteAllTextAsync(modelfilePath, modelfileContent);

            _logger.LogInformation("Creating OV-Ollama model from IR directory: {Dir}", modelDir);

            var psi = new ProcessStartInfo
            {
                FileName = ollamaExePath,
                Arguments = $"create {MODEL_TAG} -f \"{modelfilePath}\"",
                UseShellExecute = false,
                RedirectStandardOutput = true,
                RedirectStandardError = true,
                CreateNoWindow = true
            };

            // Set environment for OV-Ollama
            psi.EnvironmentVariables["OLLAMA_HOST"] = "127.0.0.1:11435";

            using var process = Process.Start(psi);
            if (process == null)
            {
                throw new InvalidOperationException("Failed to start ollama create process");
            }

            await Task.Run(() => process.WaitForExit());

            if (process.ExitCode != 0)
            {
                var errorOutput = await process.StandardError.ReadToEndAsync();
                throw new InvalidOperationException(
                    $"ollama create {MODEL_TAG} failed with exit code {process.ExitCode}: {errorOutput}");
            }

            _logger.LogInformation("Qwen3 8B IR model registered with OV-Ollama successfully");
        }

        /// <summary>
        /// Removes the model from OV-Ollama and deletes the local IR directory.
        /// Per GenAISpec.md §1CA(3)(e).
        /// </summary>
        public override async Task<bool> UninstallDependencyAsync()
        {
            _logger.LogInformation("Uninstalling Qwen3 8B IR model");

            try
            {
                var ollamaExePath = GetOvOllamaExePath();

                if (File.Exists(ollamaExePath))
                {
                    var psi = new ProcessStartInfo
                    {
                        FileName = ollamaExePath,
                        Arguments = $"rm {MODEL_TAG}",
                        UseShellExecute = false,
                        RedirectStandardOutput = true,
                        RedirectStandardError = true,
                        CreateNoWindow = true
                    };
                    psi.EnvironmentVariables["OLLAMA_HOST"] = "127.0.0.1:11435";

                    using var process = Process.Start(psi);
                    if (process != null)
                    {
                        await Task.Run(() => process.WaitForExit());
                        if (process.ExitCode != 0)
                        {
                            var errorOutput = await process.StandardError.ReadToEndAsync();
                            _logger.LogWarning("ollama rm {Model} exited with code {ExitCode}: {Error}",
                                MODEL_TAG, process.ExitCode, errorOutput);
                        }
                    }
                }
                else
                {
                    _logger.LogWarning("OV-Ollama executable not found during uninstall. Skipping rm.");
                }

                // Delete the IR model directory
                var modelDir = GetModelDirectory();
                if (Directory.Exists(modelDir))
                {
                    Directory.Delete(modelDir, recursive: true);
                    _logger.LogInformation("Deleted IR model directory: {Dir}", modelDir);
                }

                _logger.LogInformation("Qwen3 8B IR model uninstalled successfully");
                return true;
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, "Failed to uninstall Qwen3 8B IR model");
                return false;
            }
        }

        /// <summary>
        /// Provides a singleton instance of OllamaClientService.
        /// Per GenAISpec.md §1CA(3)(f).
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
