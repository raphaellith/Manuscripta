using System;
using System.Diagnostics;
using System.IO;
using System.Threading;
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
                var chromaExecutable = await ResolveChromaExecutablePathAsync();
                var processStartInfo = new ProcessStartInfo
                {
                    FileName = chromaExecutable,
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

                if (exitCode != 0)
                {
                    return false;
                }

                // Dependency is considered available only when executable exists and API is reachable.
                // This avoids false positives where chroma is installed but a non-Chroma service
                // is bound to localhost:8000.
                return await CheckChromaApiHealthAsync();
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

                var chromaExecutable = await ResolveChromaExecutablePathAsync();

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

                if (_chromaServerProcess == null)
                {
                    throw new InvalidOperationException("Failed to start ChromaDB server process");
                }

                // Track startup completion for logging purposes
                var startupComplete = false;
                var chromaSaidReady = false;
                var chromaReadyTime = DateTime.MinValue;

                // Start async tasks to continuously read stdout/stderr to prevent buffer deadlock
                var stdoutReader = Task.Run(async () =>
                {
                    try
                    {
                        string? line;
                        while ((line = await _chromaServerProcess.StandardOutput.ReadLineAsync()) != null)
                        {
                            if (!string.IsNullOrWhiteSpace(line))
                            {
                                // Detect when Chroma says it's ready
                                if (line.Contains("Connect to Chroma at:", StringComparison.OrdinalIgnoreCase))
                                {
                                    chromaSaidReady = true;
                                    chromaReadyTime = DateTime.UtcNow;
                                    _logger.LogInformation("Chroma: {Line} [Server claims ready, waiting for API to respond...]", line);
                                }
                                // Log at Info level during startup to diagnose issues, Debug after
                                else if (!startupComplete)
                                {
                                    _logger.LogInformation("Chroma: {Line}", line);
                                }
                                else
                                {
                                    _logger.LogDebug("Chroma stdout: {Line}", line);
                                }
                            }
                        }
                    }
                    catch (Exception ex)
                    {
                        _logger.LogDebug(ex, "Error reading Chroma stdout");
                    }
                });

                var stderrReader = Task.Run(async () =>
                {
                    try
                    {
                        string? line;
                        while ((line = await _chromaServerProcess.StandardError.ReadLineAsync()) != null)
                        {
                            if (!string.IsNullOrWhiteSpace(line))
                            {
                                // Downgrade OpenTelemetry warning to Info - it's not a problem
                                if (line.Contains("OpenTelemetry", StringComparison.OrdinalIgnoreCase))
                                {
                                    _logger.LogInformation("Chroma: {Line}", line);
                                }
                                else
                                {
                                    _logger.LogWarning("Chroma stderr: {Line}", line);
                                }
                            }
                        }
                    }
                    catch (Exception ex)
                    {
                        _logger.LogDebug(ex, "Error reading Chroma stderr");
                    }
                });

                // Wait for server to become responsive
                var startTime = DateTime.UtcNow;
                var timeout = TimeSpan.FromSeconds(90); // Extended timeout for first-run initialization
                var serverStarted = false;
                var lastProgressLog = DateTime.UtcNow;
                var attemptCount = 0;

                _logger.LogInformation("Waiting for ChromaDB server to become responsive (timeout: {Timeout}s)...", timeout.TotalSeconds);

                while (DateTime.UtcNow - startTime < timeout)
                {
                    attemptCount++;
                    
                    // Log progress every 10 seconds
                    var elapsed = DateTime.UtcNow - startTime;
                    if ((DateTime.UtcNow - lastProgressLog).TotalSeconds >= 10)
                    {
                        _logger.LogInformation("Still waiting for ChromaDB server... ({Elapsed:F0}s elapsed, {Attempts} attempts)", 
                            elapsed.TotalSeconds, attemptCount);
                        lastProgressLog = DateTime.UtcNow;
                    }

                    // Check if process has exited prematurely
                    if (_chromaServerProcess.HasExited)
                    {
                        var exitCode = _chromaServerProcess.ExitCode;
                        _logger.LogError("ChromaDB process exited prematurely with exit code {ExitCode}", exitCode);
                        throw new InvalidOperationException($"ChromaDB server process exited with code {exitCode} before becoming ready");
                    }

                    try
                    {
                        using var httpClient = new HttpClient { Timeout = TimeSpan.FromSeconds(3) };
                        
                        // Try multiple endpoints - the CLI version may have different endpoint availability
                        var endpoints = new[] 
                        { 
                            ("http://localhost:8000/api/v2/", "base API path"),
                            ("http://localhost:8000/", "root"),
                            ("http://127.0.0.1:8000/api/v2/", "base API path (IP)"),
                            ("http://localhost:8000/api/v2/heartbeat", "heartbeat endpoint")
                        };
                        
                        HttpResponseMessage? response = null;
                        Exception? lastException = null;
                        string? successfulEndpoint = null;
                        
                        foreach (var (endpoint, description) in endpoints)
                        {
                            try
                            {
                                response = await httpClient.GetAsync(endpoint);
                                
                                // Accept 200-299 (success), 404 (Not Found), and 410 (Gone) as signs the server is responding
                                // 410 Gone means the endpoint existed but is deprecated - server is definitely alive
                                // 404 means routing works but endpoint doesn't exist - server is alive
                                if (response.IsSuccessStatusCode || 
                                    response.StatusCode == System.Net.HttpStatusCode.NotFound ||
                                    response.StatusCode == System.Net.HttpStatusCode.Gone)
                                {
                                    serverStarted = true;
                                    startupComplete = true;
                                    _logger.LogInformation(
                                        "ChromaDB server is responding on {Endpoint} ({Description}) with status {StatusCode}. Server is ready.",
                                        endpoint, description, response.StatusCode);
                                    successfulEndpoint = endpoint;
                                    break;
                                }
                                else if (attemptCount <= 5)
                                {
                                    _logger.LogInformation("{Description} at {Endpoint} returned status code {StatusCode}", 
                                        description, endpoint, response.StatusCode);
                                }
                            }
                            catch (Exception ex)
                            {
                                lastException = ex;
                                // Continue to try next endpoint
                            }
                        }
                        
                        if (serverStarted)
                        {
                            break; // Exit the while loop
                        }
                        
                        // If all endpoints failed but we got a response, log it
                        if (response != null && attemptCount <= 5)
                        {
                            _logger.LogInformation("Server responding but no endpoint confirmed readiness. Last status: {StatusCode}", 
                                response.StatusCode);
                        }
                        
                        // If all endpoints threw exceptions, throw the last one to be caught below
                        if (lastException != null && response == null)
                        {
                            throw lastException;
                        }
                    }
                    catch (HttpRequestException ex)
                    {
                        // Log at Info for first few attempts to diagnose connectivity issues
                        if (attemptCount <= 5)
                        {
                            _logger.LogInformation("HTTP connection failed (attempt {Attempt}): {Type} - {Message}", 
                                attemptCount, ex.GetType().Name, ex.Message);
                            if (ex.InnerException != null)
                            {
                                _logger.LogInformation("  Inner exception: {Type} - {Message}", 
                                    ex.InnerException.GetType().Name, ex.InnerException.Message);
                            }
                        }
                        else
                        {
                            _logger.LogDebug("Connection failed: {Message}", ex.Message);
                        }
                    }
                    catch (TaskCanceledException ex)
                    {
                        // Log at Info for first few attempts
                        if (attemptCount <= 5)
                        {
                            _logger.LogInformation("HTTP request timed out (attempt {Attempt}): {Message}", attemptCount, ex.Message);
                        }
                        else
                        {
                            _logger.LogDebug("HTTP request timed out");
                        }
                    }
                    catch (Exception ex)
                    {
                        // Unexpected errors should always be logged
                        _logger.LogWarning(ex, "Unexpected error during health check (attempt {Attempt}): {Type} - {Message}", 
                            attemptCount, ex.GetType().Name, ex.Message);
                    }

                    await Task.Delay(1000); // Check every second
                }

                if (!serverStarted)
                {
                    var elapsed = DateTime.UtcNow - startTime;
                    var processStatus = _chromaServerProcess.HasExited 
                        ? $"Process exited with code {_chromaServerProcess.ExitCode}" 
                        : "Process still running but not responding to HTTP requests";
                    
                    var chromaReadyNote = chromaSaidReady 
                        ? $" Chroma reported 'Connect to Chroma at: http://localhost:8000' {(DateTime.UtcNow - chromaReadyTime).TotalSeconds:F1}s ago, but API heartbeat still failing." 
                        : " Chroma never reported being ready.";
                    
                    _logger.LogError(
                        "ChromaDB server failed to start within {Timeout} seconds (waited {Elapsed:F1}s). Status: {Status}.{ChromaNote} " +
                        "Review Chroma stdout logs above for startup messages. This may indicate a Windows firewall/networking issue preventing localhost connections.",
                        timeout.TotalSeconds, elapsed.TotalSeconds, processStatus, chromaReadyNote);
                    
                    throw new InvalidOperationException(
                        $"ChromaDB server failed to start within {timeout.TotalSeconds} seconds. {processStatus}.{chromaReadyNote} " +
                        "Check startup logs above for Chroma output. If Chroma reported being ready but API is unreachable, " +
                        "check Windows Firewall settings for localhost blocking. You can also try manually running: " +
                        $"chroma run --path \"{dataPath}\" to diagnose the issue.");
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
            // First, kill any running Chroma processes that might lock the executable
            try
            {
                var chromaProcesses = Process.GetProcessesByName("chroma")
                    .Concat(Process.GetProcessesByName("chroma.exe"))
                    .Concat(Process.GetProcessesByName("chroma-windows"))
                    .ToList();

                foreach (var proc in chromaProcesses)
                {
                    try
                    {
                        _logger.LogInformation("Terminating running Chroma process (PID: {Pid})", proc.Id);
                        proc.Kill();
                        proc.WaitForExit(5000); // Wait up to 5 seconds for graceful termination
                        proc.Dispose();
                    }
                    catch (Exception ex)
                    {
                        _logger.LogWarning(ex, "Failed to terminate Chroma process (PID: {Pid})", proc.Id);
                    }
                }

                // Give the OS a moment to release file handles
                if (chromaProcesses.Any())
                {
                    Thread.Sleep(500);
                }
            }
            catch (Exception ex)
            {
                _logger.LogWarning(ex, "Failed to enumerate/kill Chroma processes");
            }

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
                        
                        // Clear read-only and other restrictive attributes before deletion
                        try
                        {
                            File.SetAttributes(location, FileAttributes.Normal);
                        }
                        catch (Exception attrEx)
                        {
                            _logger.LogDebug(attrEx, "Failed to clear attributes on {Location}", location);
                        }

                        File.Delete(location);
                        _logger.LogInformation("Successfully removed existing chroma.exe at {Location}", location);
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

        /// <summary>
        /// Resolves the Chroma executable path from known installation locations and PATH entries.
        /// Falls back to the command name "chroma" if no absolute path can be identified.
        /// </summary>
        protected virtual async Task<string> ResolveChromaExecutablePathAsync()
        {
            try
            {
                if (!string.IsNullOrWhiteSpace(_installedChromaPath) && File.Exists(_installedChromaPath))
                {
                    _logger.LogInformation("Using installer-reported chroma path: {Path}", _installedChromaPath);
                    return _installedChromaPath;
                }

                var candidateDirs = GetChromaCandidateDirectories();
                foreach (var dir in candidateDirs)
                {
                    try
                    {
                        var exePath = Path.Combine(dir, "chroma.exe");
                        if (File.Exists(exePath))
                        {
                            _logger.LogInformation("Found chroma executable at {Path}", exePath);
                            return exePath;
                        }
                    }
                    catch
                    {
                        // Ignore malformed candidate directories.
                    }
                }

                var whereResolvedPath = await ResolveChromaPathViaWhereAsync();
                if (!string.IsNullOrWhiteSpace(whereResolvedPath))
                {
                    return whereResolvedPath;
                }
            }
            catch (Exception ex)
            {
                _logger.LogDebug(ex, "Failed to resolve chroma executable path; falling back to command name");
            }

            return "chroma";
        }

        private IEnumerable<string> GetChromaCandidateDirectories()
        {
            var candidateDirs = new List<string>();

            string? userPath = Environment.GetEnvironmentVariable("PATH", EnvironmentVariableTarget.User);
            if (!string.IsNullOrWhiteSpace(userPath))
            {
                candidateDirs.AddRange(userPath.Split(Path.PathSeparator, StringSplitOptions.RemoveEmptyEntries));
            }

            string? machinePath = Environment.GetEnvironmentVariable("PATH", EnvironmentVariableTarget.Machine);
            if (!string.IsNullOrWhiteSpace(machinePath))
            {
                candidateDirs.AddRange(machinePath.Split(Path.PathSeparator, StringSplitOptions.RemoveEmptyEntries));
            }

            string? processPath = Environment.GetEnvironmentVariable("PATH");
            if (!string.IsNullOrWhiteSpace(processPath))
            {
                candidateDirs.AddRange(processPath.Split(Path.PathSeparator, StringSplitOptions.RemoveEmptyEntries));
            }

            var userProfile = Environment.GetFolderPath(Environment.SpecialFolder.UserProfile);
            if (!string.IsNullOrWhiteSpace(userProfile))
            {
                candidateDirs.Add(Path.Combine(userProfile, "bin"));
                candidateDirs.Add(Path.Combine(userProfile, ".cargo", "bin"));
            }

            var localAppData = Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData);
            if (!string.IsNullOrWhiteSpace(localAppData))
            {
                candidateDirs.Add(Path.Combine(localAppData, "Programs", "chroma"));
            }

            var programFiles = Environment.GetFolderPath(Environment.SpecialFolder.ProgramFiles);
            if (!string.IsNullOrWhiteSpace(programFiles))
            {
                candidateDirs.Add(Path.Combine(programFiles, "chroma"));
            }

            return candidateDirs
                .Where(d => !string.IsNullOrWhiteSpace(d))
                .Distinct(StringComparer.OrdinalIgnoreCase);
        }

        private async Task<string?> ResolveChromaPathViaWhereAsync()
        {
            try
            {
                var whereInfo = new ProcessStartInfo
                {
                    FileName = "where.exe",
                    Arguments = "chroma",
                    UseShellExecute = false,
                    RedirectStandardOutput = true,
                    RedirectStandardError = true,
                    CreateNoWindow = true
                };

                using var whereProcess = Process.Start(whereInfo);
                if (whereProcess == null)
                {
                    return null;
                }

                var whereOut = await whereProcess.StandardOutput.ReadToEndAsync();
                whereProcess.WaitForExit(2000);
                var firstLine = whereOut
                    .Split(new[] { '\r', '\n' }, StringSplitOptions.RemoveEmptyEntries)
                    .Select(line => line.Trim())
                    .FirstOrDefault(line => !string.IsNullOrWhiteSpace(line));

                if (!string.IsNullOrWhiteSpace(firstLine) && File.Exists(firstLine))
                {
                    _logger.LogInformation("Resolved chroma path via 'where.exe': {Path}", firstLine);
                    return firstLine;
                }
            }
            catch (Exception ex)
            {
                _logger.LogDebug(ex, "'where.exe chroma' failed while resolving executable path");
            }

            return null;
        }

        protected virtual async Task<bool> CheckChromaApiHealthAsync()
        {
            try
            {
                using var httpClient = new HttpClient { Timeout = TimeSpan.FromSeconds(1) };
                var response = await httpClient.GetAsync("http://localhost:8000/api/v2/heartbeat");
                return response.IsSuccessStatusCode;
            }
            catch
            {
                return false;
            }
        }
    }
}
