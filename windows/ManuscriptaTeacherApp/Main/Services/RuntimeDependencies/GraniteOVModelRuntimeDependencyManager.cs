using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.IO;
using System.IO.Compression;
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
    /// in OpenVINO IR format for OV-Ollama.
    /// Per GenAISpec.md §1DA and zhaohb/ollama_ov import procedure.
    /// </summary>
    public class GraniteOVModelRuntimeDependencyManager : RuntimeDependencyManagerBase
    {
        private readonly ILogger<GraniteOVModelRuntimeDependencyManager> _logger;
        private readonly HttpClient _httpClient;
        private readonly IInferenceRuntimeSelector _inferenceRuntimeSelector;
        private static OllamaClientService? _ollamaClientServiceInstance;
        private readonly object _serviceLock = new object();

        /// <summary>
        /// HuggingFace repository for pre-converted Granite 4 Micro INT4 IR model.
        /// Per GenAISpec.md Appendix A (GRANITE4_IR_HF_REPO).
        /// </summary>
        private const string HF_REPO = "llmware/granite-4-micro-ov";

        /// <summary>
        /// Local model directory name.
        /// </summary>
        private const string MODEL_DIR_NAME = "granite4-micro-ov";

        /// <summary>
        /// The model tag registered with OV-Ollama.
        /// </summary>
        private const string MODEL_TAG = "granite4-micro-ov:v1";

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
        /// Gets the local directory where IR model files are downloaded.
        /// </summary>
        private string GetModelDirectory()
        {
            return Path.Combine(GetOvOllamaBinDir(), "ir-downloads", MODEL_DIR_NAME);
        }

        /// <summary>
        /// Gets the path to the OV-Ollama executable.
        /// </summary>
        private string GetOvOllamaExePath()
        {
            return Path.Combine(GetOvOllamaBinDir(), "ollama.exe");
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
                            if (modelName == MODEL_TAG || modelName?.StartsWith(MODEL_DIR_NAME) == true)
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
        /// Downloads all files from the HuggingFace repository into the local model directory.
        /// Per GenAISpec.md §1DA(3)(b).
        /// </summary>
        protected override async Task DownloadDependencyAsync(IProgress<RuntimeDependencyProgress> progress)
        {
            _logger.LogInformation("Downloading IBM Granite 4.0 IR model from HuggingFace: {Repo}", HF_REPO);

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
                    ProgressPercentage = totalSize > 0 ? (int)(downloadedSize * 70 / totalSize) : (int)(i * 70.0 / fileList.Count)
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
                ProgressPercentage = 70
            });

            _logger.LogInformation("IBM Granite 4.0 IR model downloaded successfully to {Dir}", modelDir);
        }

        /// <summary>
        /// Verifies that the IR model directory contains required files.
        /// Per GenAISpec.md §1DA(3)(c).
        /// </summary>
        protected override Task VerifyDownloadAsync(IProgress<RuntimeDependencyProgress> progress)
        {
            var modelDir = GetModelDirectory();

            // Check for key OpenVINO IR files
            var hasXml = Directory.GetFiles(modelDir, "*.xml", SearchOption.AllDirectories).Any();
            var hasBin = Directory.GetFiles(modelDir, "*.bin", SearchOption.AllDirectories).Any();

            if (!hasXml || !hasBin)
            {
                throw new InvalidOperationException(
                    $"IR model verification failed: no .xml or .bin files found in {modelDir}");
            }

            _logger.LogInformation("IBM Granite 4.0 IR model verified successfully");
            return Task.CompletedTask;
        }

        /// <summary>
        /// Packages the IR model as tar.gz, creates a Modelfile with ModelType "OpenVINO",
        /// and registers with OV-Ollama using `ollama create`.
        /// Per zhaohb/ollama_ov import procedure.
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

            // Verify OV-Ollama server is running
            try
            {
                var serverCheck = await _httpClient.GetAsync("http://localhost:11435/api/version");
                if (!serverCheck.IsSuccessStatusCode)
                {
                    throw new InvalidOperationException(
                        "OV-Ollama server is not responding on port 11435.");
                }
            }
            catch (HttpRequestException)
            {
                throw new InvalidOperationException(
                    "OV-Ollama server is not running on port 11435. "
                    + "Ensure the OpenVINO Ollama runtime dependency is installed and running.");
            }

            progress?.Report(new RuntimeDependencyProgress { Phase = "Packaging model" });

            // Step 1: Package IR model directory as tar.gz
            var tarGzPath = modelDir + ".tar.gz";
            _logger.LogInformation("Packaging IR model as tar.gz: {Path}", tarGzPath);

            await CreateTarGzAsync(modelDir, tarGzPath);

            // Step 2: Create Modelfile with ModelType "OpenVINO" (mandatory per ollama_ov docs)
            // TEMPLATE is required for chat formatting — without it the model receives
            // raw text and returns blank output.
            var modelfilePath = Path.Combine(Path.GetDirectoryName(modelDir)!, $"{MODEL_DIR_NAME}_Modelfile");
            var modelfileContent = $@"FROM {tarGzPath}
ModelType ""OpenVINO""
InferDevice ""GPU""
TEMPLATE """"""{{{{ if .System }}}}<|system|>
{{{{ .System }}}}
{{{{ end }}}}{{{{ if .Prompt }}}}<|user|>
{{{{ .Prompt }}}}
{{{{ end }}}}<|assistant|>
{{{{ .Response }}}}""""""
PARAMETER stop ""<|system|>""
PARAMETER stop ""<|user|>""
PARAMETER stop ""<|assistant|>""
PARAMETER temperature 0.7
PARAMETER num_ctx 4096
";
            await File.WriteAllTextAsync(modelfilePath, modelfileContent);

            _logger.LogInformation("Created Modelfile at {Path} with content:\n{Content}", modelfilePath, modelfileContent);

            progress?.Report(new RuntimeDependencyProgress { Phase = "Registering model with OV-Ollama" });

            // Step 3: ollama create with the Modelfile
            var psi = new ProcessStartInfo
            {
                FileName = ollamaExePath,
                Arguments = $"create {MODEL_TAG} -f \"{modelfilePath}\"",
                UseShellExecute = false,
                RedirectStandardOutput = true,
                RedirectStandardError = true,
                CreateNoWindow = true
            };

            psi.EnvironmentVariables["OLLAMA_HOST"] = "127.0.0.1:11435";
            psi.EnvironmentVariables["OLLAMA_MODELS"] = GetOvOllamaModelsDir();

            using var process = Process.Start(psi);
            if (process == null)
            {
                throw new InvalidOperationException("Failed to start ollama create process");
            }

            var stdOut = new List<string>();
            var stdErr = new List<string>();

            process.OutputDataReceived += (s, e) =>
            {
                if (e.Data != null)
                {
                    stdOut.Add(e.Data);
                    _logger.LogInformation("[ollama create] {Line}", e.Data);
                }
            };
            process.ErrorDataReceived += (s, e) =>
            {
                if (e.Data != null)
                {
                    stdErr.Add(e.Data);
                    _logger.LogWarning("[ollama create err] {Line}", e.Data);
                }
            };

            process.BeginOutputReadLine();
            process.BeginErrorReadLine();

            // Timeout after 10 minutes
            var exited = await Task.Run(() => process.WaitForExit(600_000));
            if (!exited)
            {
                try { process.Kill(); } catch { }
                throw new TimeoutException(
                    $"ollama create {MODEL_TAG} timed out after 10 minutes.");
            }

            if (process.ExitCode != 0)
            {
                var errorOutput = string.Join(Environment.NewLine, stdErr);
                throw new InvalidOperationException(
                    $"ollama create {MODEL_TAG} failed with exit code {process.ExitCode}: {errorOutput}");
            }

            _logger.LogInformation("IBM Granite 4.0 IR model registered with OV-Ollama successfully as {Tag}", MODEL_TAG);

            // Cleanup tar.gz and Modelfile
            try { File.Delete(tarGzPath); } catch { }
            try { File.Delete(modelfilePath); } catch { }
        }

        /// <summary>
        /// Creates a tar.gz archive from a directory using the system tar command.
        /// </summary>
        private static async Task CreateTarGzAsync(string sourceDir, string outputPath)
        {
            if (File.Exists(outputPath))
            {
                File.Delete(outputPath);
            }

            var parentDir = Path.GetDirectoryName(sourceDir)!;
            var dirName = Path.GetFileName(sourceDir);

            var psi = new ProcessStartInfo
            {
                FileName = "tar",
                Arguments = $"-zcf \"{outputPath}\" \"{dirName}\"",
                WorkingDirectory = parentDir,
                UseShellExecute = false,
                RedirectStandardOutput = true,
                RedirectStandardError = true,
                CreateNoWindow = true
            };

            using var process = Process.Start(psi);
            if (process == null)
            {
                throw new InvalidOperationException("Failed to start tar process");
            }

            await Task.Run(() => process.WaitForExit());

            if (process.ExitCode != 0)
            {
                var errorOutput = await process.StandardError.ReadToEndAsync();
                throw new InvalidOperationException(
                    $"tar failed with exit code {process.ExitCode}: {errorOutput}");
            }
        }

        /// <summary>
        /// Removes the model from OV-Ollama and deletes downloaded IR files.
        /// Per GenAISpec.md §1DA(3)(e).
        /// </summary>
        public override async Task<bool> UninstallDependencyAsync()
        {
            _logger.LogInformation("Uninstalling IBM Granite 4.0 IR model");

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
                    psi.EnvironmentVariables["OLLAMA_MODELS"] = GetOvOllamaModelsDir();

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

                // Delete the IR model download directory
                var modelDir = GetModelDirectory();
                if (Directory.Exists(modelDir))
                {
                    Directory.Delete(modelDir, recursive: true);
                    _logger.LogInformation("Deleted IR model directory: {Dir}", modelDir);
                }

                _logger.LogInformation("IBM Granite 4.0 IR model uninstalled successfully");
                return true;
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, "Failed to uninstall IBM Granite 4.0 IR model");
                return false;
            }
        }

        /// <summary>
        /// Provides a singleton instance of OllamaClientService.
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
