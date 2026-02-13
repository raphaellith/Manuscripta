using System.Diagnostics;
using System.Net.Http;
using System.Runtime.InteropServices;

using System.IO.Compression;

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
    public async Task<bool> InstallAsync()
    {
        try
        {
            // Ensure target directory exists
            var binDir = Path.GetDirectoryName(_rmapiExecutablePath)!;
            Directory.CreateDirectory(binDir);

            // Download from GitHub releases (zip)
            // Per RemarkableIntegrationSpecification §2(4)
            var downloadUrl = "https://github.com/ddvk/rmapi/releases/latest/download/rmapi-win64.zip";
            _logger.LogInformation("Downloading rmapi from {Url}", downloadUrl);

            var tempZipPath = Path.GetTempFileName();
            var tempExtractPath = Path.Combine(Path.GetTempPath(), Path.GetRandomFileName());

            try
            {
                using (var response = await _httpClient.GetAsync(downloadUrl))
                {
                    response.EnsureSuccessStatusCode();
                    await using var fileStream = File.Create(tempZipPath);
                    await response.Content.CopyToAsync(fileStream);
                }

                _logger.LogInformation("Extracting rmapi...");
                ZipFile.ExtractToDirectory(tempZipPath, tempExtractPath);

                // Find rmapi.exe in extracted files (could be in root or subfolder)
                var rmapiFile = Directory.GetFiles(tempExtractPath, "rmapi.exe", SearchOption.AllDirectories).FirstOrDefault();
                
                if (rmapiFile == null)
                {
                    throw new FileNotFoundException("rmapi.exe not found in downloaded zip archive");
                }

                // Move to target location, overwriting if exists
                if (File.Exists(_rmapiExecutablePath))
                {
                    File.Delete(_rmapiExecutablePath);
                }
                
                File.Move(rmapiFile, _rmapiExecutablePath);

                _logger.LogInformation("rmapi installed successfully at {Path}", _rmapiExecutablePath);

                // Invalidate cache so next check picks up the new binary
                InvalidateAvailabilityCache();
                return true;
            }
            finally
            {
                // Cleanup temp files
                if (File.Exists(tempZipPath)) File.Delete(tempZipPath);
                if (Directory.Exists(tempExtractPath)) Directory.Delete(tempExtractPath, true);
            }
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Failed to install rmapi");
            return false;
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
        // mkdir may fail if folder already exists — that's acceptable
        if (result.ExitCode != 0)
        {
            _logger.LogDebug("mkdir returned exit code {ExitCode} for {Path} (may already exist): {Output}",
                result.ExitCode, remotePath, result.Output);

            if (IsAuthError(result.Output))
            {
                throw new RmapiAuthException($"Authentication invalid. Output: {result.Output}");
            }
        }
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

        var output = await process.StandardOutput.ReadToEndAsync();
        var error = await process.StandardError.ReadToEndAsync();

        await process.WaitForExitAsync();

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

    private void SetCachedAvailability(bool value)
    {
        lock (_cacheLock)
        {
            _cachedAvailability = value;
        }
    }

    private record ProcessResult(int ExitCode, string Output);
}
