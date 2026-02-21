using System;
using System.Diagnostics;
using System.IO;
using System.Linq;
using System.Net.Http;
using System.Runtime.InteropServices;
using System.Security.Cryptography;
using System.IO.Compression;
using System.Threading.Tasks;
using Microsoft.Extensions.Logging;
using Main.Models;

namespace Main.Services;

/// <summary>
/// Service for interacting with the rmapi tool.
/// Per RemarkableIntegrationSpecification §1(4), §2, §3(2)(b-c), §4(2)(c).
/// </summary>
public class RmapiService : IRmapiService
{
    private readonly ILogger<RmapiService> _logger;
    private readonly HttpClient _httpClient;
    private readonly string _rmapiExecutablePath;
    private bool? _cachedAvailability;
    private readonly object _cacheLock = new();
    public const string RmapiReleaseVersion = "v0.0.32";
    private const string RmapiZipSha256 = "2b784d017ea19723bb75c90fa5500349a1599d2956404251b5631736de5ddf94";

    /// <summary>
    /// The path where the rmapi executable is stored.
    /// Per RemarkableIntegrationSpecification §2(2)(a).
    /// </summary>
    private static string DefaultRmapiExecutablePath => Path.Combine(
        Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData),
        "ManuscriptaTeacherApp",
        "bin",
        "rmapi.exe"
    );

    /// <summary>
    /// The base directory for rmapi configuration files.
    /// Per RemarkableIntegrationSpecification §3(2)(c).
    /// </summary>
    private static string RmapiConfigDirectory => Path.Combine(
        Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData),
        "ManuscriptaTeacherApp",
        "rmapi"
    );

    public RmapiService(ILogger<RmapiService> logger, HttpClient httpClient, string? rmapiExecutablePath = null)
    {
        _logger = logger ?? throw new ArgumentNullException(nameof(logger));
        _httpClient = httpClient ?? throw new ArgumentNullException(nameof(httpClient));
        _rmapiExecutablePath = string.IsNullOrWhiteSpace(rmapiExecutablePath)
            ? DefaultRmapiExecutablePath
            : rmapiExecutablePath;
    }

    /// <inheritdoc />
    public async Task<bool> CheckAvailabilityAsync()
    {
        // Per §2(3): return cached result if available
        lock (_cacheLock)
        {
            if (_cachedAvailability.HasValue)
            {
                return _cachedAvailability.Value;
            }
        }

        // Per §2(2)(a): check executable exists
        if (!File.Exists(_rmapiExecutablePath))
        {
            _logger.LogInformation("rmapi not found at {Path}", _rmapiExecutablePath);
            SetCachedAvailability(false);
            return false;
        }

        // Per §2(2)(b): invoke 'rmapi version'
        try
        {
            var result = await RunRmapiAsync("version", configPath: null);
            var available = result.ExitCode == 0;
            _logger.LogInformation("rmapi version check: exit code {ExitCode}, available={Available}", result.ExitCode, available);
            SetCachedAvailability(available);
            return available;
        }
        catch (Exception ex)
        {
            _logger.LogWarning(ex, "rmapi version check failed");
            SetCachedAvailability(false);
            return false;
        }
    }


    /// <inheritdoc />
    public async Task<bool> InstallAsync(IProgress<RuntimeDependencyProgress>? progress = null)
    {
        try
        {
            var binDir = Path.GetDirectoryName(_rmapiExecutablePath)!;
            Directory.CreateDirectory(binDir);

            var downloadUrl = $"https://github.com/ddvk/rmapi/releases/download/{RmapiReleaseVersion}/rmapi-win64.zip";
            var tempZipPath = Path.GetTempFileName();
            var tempExtractPath = Path.Combine(Path.GetTempPath(), Path.GetRandomFileName());

            try
            {
                await DownloadDependencyAsync(downloadUrl, tempZipPath, progress);
                await VerifyDownloadAsync(tempZipPath, progress);
                await InstallExtractedAsync(tempZipPath, tempExtractPath, progress);

                InvalidateAvailabilityCache();
                progress?.Report(new RuntimeDependencyProgress { Phase = "Completed", ProgressPercentage = null });
                return true;
            }
            finally
            {
                if (File.Exists(tempZipPath)) File.Delete(tempZipPath);
                if (Directory.Exists(tempExtractPath)) Directory.Delete(tempExtractPath, true);
            }
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Failed to install rmapi");
            progress?.Report(new RuntimeDependencyProgress { Phase = "Failed", ErrorMessage = ex.Message });
            return false;
        }
    }

    public async Task DownloadDependencyAsync(string downloadUrl, string tempZipPath, IProgress<RuntimeDependencyProgress>? progress)
    {
        progress?.Report(new RuntimeDependencyProgress { Phase = "Downloading", ProgressPercentage = 0 });
        _logger.LogInformation("Downloading rmapi from {Url}", downloadUrl);

        using var response = await _httpClient.GetAsync(downloadUrl, HttpCompletionOption.ResponseHeadersRead);
        response.EnsureSuccessStatusCode();

        var totalBytes = response.Content.Headers.ContentLength;
        using var contentStream = await response.Content.ReadAsStreamAsync();
        using var fileStream = File.Create(tempZipPath);

        var buffer = new byte[8192];
        long totalRead = 0;
        int bytesRead;

        while ((bytesRead = await contentStream.ReadAsync(buffer, 0, buffer.Length)) > 0)
        {
            await fileStream.WriteAsync(buffer, 0, bytesRead);
            totalRead += bytesRead;

            if (totalBytes.HasValue && totalBytes.Value > 0)
            {
                var percentage = (int)((double)totalRead / totalBytes.Value * 100);
                progress?.Report(new RuntimeDependencyProgress { Phase = "Downloading", ProgressPercentage = percentage });
            }
        }
    }

    public Task VerifyDownloadAsync(string tempZipPath, IProgress<RuntimeDependencyProgress>? progress)
    {
        progress?.Report(new RuntimeDependencyProgress { Phase = "Verifying", ProgressPercentage = null });

        var actualHash = ComputeFileSha256(tempZipPath);
        if (!string.Equals(actualHash, RmapiZipSha256, StringComparison.OrdinalIgnoreCase))
        {
            throw new InvalidDataException($"rmapi checksum mismatch. expected {RmapiZipSha256}, got {actualHash}");
        }

        return Task.CompletedTask;
    }

    public Task InstallExtractedAsync(string tempZipPath, string tempExtractPath, IProgress<RuntimeDependencyProgress>? progress)
    {
        progress?.Report(new RuntimeDependencyProgress { Phase = "Installing", ProgressPercentage = null });
        _logger.LogInformation("Extracting rmapi...");

        ZipFile.ExtractToDirectory(tempZipPath, tempExtractPath);

        var rmapiFile = Directory.GetFiles(tempExtractPath, "rmapi.exe", SearchOption.AllDirectories).FirstOrDefault();
        if (rmapiFile == null)
        {
            throw new FileNotFoundException("rmapi.exe not found in downloaded zip archive");
        }

        var binDir = Path.GetDirectoryName(_rmapiExecutablePath);
        if (binDir != null && !Directory.Exists(binDir))
        {
            Directory.CreateDirectory(binDir);
        }

        if (File.Exists(_rmapiExecutablePath))
        {
            File.Delete(_rmapiExecutablePath);
        }

        File.Move(rmapiFile, _rmapiExecutablePath);
        _logger.LogInformation("rmapi installed successfully at {Path}", _rmapiExecutablePath);

        return Task.CompletedTask;
    }

    /// <inheritdoc />
    public Task<bool> UninstallAsync()
    {
        try
        {
            if (File.Exists(_rmapiExecutablePath))
            {
                File.Delete(_rmapiExecutablePath);
            }
            InvalidateAvailabilityCache();
            return Task.FromResult(true);
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Failed to uninstall rmapi");
            return Task.FromResult(false);
        }
    }

    /// <inheritdoc />
    public async Task<bool> AuthenticateAsync(string oneTimeCode, string configPath)
    {
        if (string.IsNullOrWhiteSpace(oneTimeCode))
            throw new ArgumentException("One-time code cannot be empty.", nameof(oneTimeCode));
        if (string.IsNullOrWhiteSpace(configPath))
            throw new ArgumentException("Config path cannot be empty.", nameof(configPath));

        // Ensure config directory exists
        var configDir = Path.GetDirectoryName(configPath);
        if (configDir != null)
        {
            Directory.CreateDirectory(configDir);
        }

        try
        {
            // rmapi reads the one-time code from stdin via readCode() when no device token exists.
            // The -ni flag must NOT be used here — it prevents code reading and aborts immediately.
            // A post-auth command ("ls /") is passed so rmapi exits after authentication
            // instead of entering the interactive shell.
            var result = await RunRmapiAsync("ls /", configPath, stdinInput: oneTimeCode);
            if (result.ExitCode == 0)
            {
                _logger.LogInformation("reMarkable authentication succeeded for config {ConfigPath}", configPath);
                return true;
            }

            _logger.LogWarning("reMarkable authentication failed: {Output}", result.Output);
            return false;
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "reMarkable authentication process failed");
            return false;
        }
    }

    /// <inheritdoc />
    public async Task UploadFileAsync(string localPath, string remotePath, string configPath)
    {
        if (!File.Exists(localPath))
            throw new FileNotFoundException("File to upload not found.", localPath);

        var result = await RunRmapiAsync($"put \"{localPath}\" \"{remotePath}\"", configPath);

        if (result.ExitCode != 0)
        {
            if (IsAuthError(result.Output))
            {
                throw new RmapiAuthException($"Authentication invalid for config at {configPath}. Output: {result.Output}");
            }
            throw new RmapiException($"Upload failed with exit code {result.ExitCode}. Output: {result.Output}");
        }

        _logger.LogInformation("Uploaded {LocalPath} to {RemotePath}", localPath, remotePath);
    }

    /// <inheritdoc />
    public async Task EnsureFolderExistsAsync(string remotePath, string configPath)
    {
        var result = await RunRmapiAsync($"mkdir \"{remotePath}\"", configPath);

        if (result.ExitCode != 0)
        {
            if (IsAuthError(result.Output))
            {
                throw new RmapiAuthException($"Authentication invalid. Output: {result.Output}");
            }

            // mkdir may fail if folder already exists — that's acceptable; other errors are not.
            if (!IsAlreadyExistsError(result.Output))
            {
                throw new RmapiException($"mkdir failed for path {remotePath} with exit code {result.ExitCode}. Output: {result.Output}");
            }

            _logger.LogDebug("mkdir reported existing folder for {Path}: {Output}", remotePath, result.Output);
        }
    }

    private static bool IsAlreadyExistsError(string output)
    {
        if (string.IsNullOrEmpty(output))
        {
            return false;
        }

        // Heuristic: rmapi (or underlying tools) typically indicate existing folders with phrases like
        // "already exists" or "File exists".
        return output.Contains("already exists", StringComparison.OrdinalIgnoreCase)
               || output.Contains("file exists", StringComparison.OrdinalIgnoreCase);
    }
    /// <inheritdoc />
    public async Task<List<string>> ListFolderAsync(string remotePath, string configPath)
    {
        var result = await RunRmapiAsync($"ls \"{remotePath}\"", configPath);

        if (result.ExitCode != 0)
        {
            if (IsAuthError(result.Output))
            {
                throw new RmapiAuthException($"Authentication invalid. Output: {result.Output}");
            }
            // Folder may not exist yet — return empty
            return new List<string>();
        }

        // Parse ls output: each line is a file/folder entry
        var entries = result.Output
            .Split('\n', StringSplitOptions.RemoveEmptyEntries)
            .Select(line => line.Trim())
            .Where(line => !string.IsNullOrEmpty(line))
            .Select(line =>
            {
                // rmapi ls output format: "[d]\tname" or "[f]\tname"
                var tabIndex = line.IndexOf('\t');
                return tabIndex >= 0 ? line[(tabIndex + 1)..] : line;
            })
            .ToList();

        return entries;
    }

    /// <inheritdoc />
    public string GetConfigPath(Guid deviceId)
    {
        return Path.Combine(RmapiConfigDirectory, $"{deviceId}.conf");
    }

    /// <inheritdoc />
    public void InvalidateAvailabilityCache()
    {
        lock (_cacheLock)
        {
            _cachedAvailability = null;
        }
    }

    /// <summary>
    /// Runs the rmapi executable with the given arguments.
    /// </summary>
    /// <param name="arguments">Command-line arguments for rmapi.</param>
    /// <param name="configPath">Optional config path set via RMAPI_CONFIG env var.</param>
    /// <param name="stdinInput">Optional input to write to the process's standard input (e.g. one-time code).</param>
    private async Task<ProcessResult> RunRmapiAsync(string arguments, string? configPath, string? stdinInput = null)
    {
        const int timeoutMilliseconds = 30_000;
        var startInfo = new ProcessStartInfo
        {
            FileName = _rmapiExecutablePath,
            Arguments = arguments,
            UseShellExecute = false,
            RedirectStandardOutput = true,
            RedirectStandardError = true,
            RedirectStandardInput = stdinInput != null,
            CreateNoWindow = true
        };

        // Set RMAPI_CONFIG environment variable if a config path is provided
        // Per RemarkableIntegrationSpecification §3(2)(c)
        if (!string.IsNullOrEmpty(configPath))
        {
            startInfo.EnvironmentVariables["RMAPI_CONFIG"] = configPath;
        }

        using var process = new Process { StartInfo = startInfo };
        process.Start();

        // Write to stdin if input is provided (e.g. one-time auth code for -ni mode)
        if (stdinInput != null)
        {
            await process.StandardInput.WriteLineAsync(stdinInput);
            process.StandardInput.Close();
        }

        var outputTask = process.StandardOutput.ReadToEndAsync();
        var errorTask = process.StandardError.ReadToEndAsync();
        var waitForExitTask = process.WaitForExitAsync();
        var allTasks = Task.WhenAll(outputTask, errorTask, waitForExitTask);
        var timeoutTask = Task.Delay(timeoutMilliseconds);
        var completed = await Task.WhenAny(allTasks, timeoutTask);
        if (completed == timeoutTask)
        {
            try
            {
                process.Kill(entireProcessTree: true);
            }
            catch
            {
            }
            throw new TimeoutException($"rmapi command timed out after {timeoutMilliseconds}ms: {arguments}");
        }

        var output = await outputTask;
        var error = await errorTask;

        var combinedOutput = string.IsNullOrEmpty(error) ? output : $"{output}\n{error}";

        return new ProcessResult(process.ExitCode, combinedOutput);
    }

    /// <summary>
    /// Determines whether rmapi output indicates an authentication error.
    /// </summary>
    private static bool IsAuthError(string output)
    {
        // Common rmapi auth error indicators
        return output.Contains("401", StringComparison.OrdinalIgnoreCase) ||
               output.Contains("unauthorized", StringComparison.OrdinalIgnoreCase) ||
               output.Contains("token", StringComparison.OrdinalIgnoreCase) && output.Contains("expired", StringComparison.OrdinalIgnoreCase) ||
               output.Contains("authentication", StringComparison.OrdinalIgnoreCase) && output.Contains("failed", StringComparison.OrdinalIgnoreCase);
    }

    private static string ComputeFileSha256(string filePath)
    {
        using var stream = File.OpenRead(filePath);
        using var sha256 = SHA256.Create();
        var hash = sha256.ComputeHash(stream);
        return Convert.ToHexString(hash).ToLowerInvariant();
    }

    private void SetCachedAvailability(bool value)
    {
        lock (_cacheLock)
        {
            _cachedAvailability = value;
        }
    }

    private record ProcessResult(int ExitCode, string Output);
}
