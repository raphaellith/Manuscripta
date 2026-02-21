using Main.Models;
using Main.Services.RuntimeDependencies;
using System;
using System.Collections.Generic;
using System.Threading.Tasks;

namespace Main.Services;

/// <summary>
/// Service interface for interacting with the rmapi tool.
/// Per RemarkableIntegrationSpecification §1(4), §2, §3(2)(b-c), §4(2)(c).
/// </summary>
public interface IRmapiService : IDependencyService
{
    /// <summary>
    /// Checks whether rmapi is available and functional.
    /// Per RemarkableIntegrationSpecification §2(2): checks existence at known path + invokes 'rmapi version'.
    /// Per §2(3): result is cached for the session duration.
    /// </summary>
    /// <returns>True if rmapi is available and responds correctly.</returns>
    Task<bool> CheckAvailabilityAsync();

    /// <summary>
    /// Downloads and installs the rmapi binary from GitHub releases.
    /// Per RemarkableIntegrationSpecification §2(4).
    /// </summary>
    /// <returns>True if installation succeeded.</returns>
    Task<bool> InstallAsync(IProgress<RuntimeDependencyProgress>? progress = null);

    /// <summary>
    /// Uninstalls the rmapi binary.
    /// </summary>
    /// <returns>True if uninstallation succeeded.</returns>
    Task<bool> UninstallAsync();

    /// <summary>
    /// Downloads the rmapi zip file.
    /// </summary>
    Task DownloadDependencyAsync(string downloadUrl, string tempZipPath, IProgress<RuntimeDependencyProgress>? progress);

    /// <summary>
    /// Verifies the downloaded zip file hash.
    /// </summary>
    Task VerifyDownloadAsync(string tempZipPath, IProgress<RuntimeDependencyProgress>? progress);

    /// <summary>
    /// Extracts and installs the rmapi binary from the zip file.
    /// </summary>
    Task InstallExtractedAsync(string tempZipPath, string tempExtractPath, IProgress<RuntimeDependencyProgress>? progress);

    /// <summary>
    /// Authenticates with the reMarkable cloud using a one-time code.
    /// Per RemarkableIntegrationSpecification §3(2)(b-c).
    /// </summary>
    /// <param name="oneTimeCode">The one-time code from my.remarkable.com.</param>
    /// <param name="configPath">The path to store the rmapi configuration file.</param>
    /// <returns>True if authentication succeeded.</returns>
    Task<bool> AuthenticateAsync(string oneTimeCode, string configPath);

    /// <summary>
    /// Uploads a file to the reMarkable cloud via rmapi.
    /// Per RemarkableIntegrationSpecification §4(2)(c).
    /// </summary>
    /// <param name="localPath">The local file path to upload.</param>
    /// <param name="remotePath">The destination folder on the reMarkable cloud.</param>
    /// <param name="configPath">The rmapi configuration file path for the target device.</param>
    /// <exception cref="RmapiAuthException">Thrown when authentication has become invalid.</exception>
    /// <exception cref="RmapiException">Thrown when the upload fails for other reasons.</exception>
    Task UploadFileAsync(string localPath, string remotePath, string configPath);

    /// <summary>
    /// Creates the destination folder on the reMarkable cloud if it does not already exist.
    /// </summary>
    /// <param name="remotePath">The folder path to create.</param>
    /// <param name="configPath">The rmapi configuration file path for the target device.</param>
    Task EnsureFolderExistsAsync(string remotePath, string configPath);

    /// <summary>
    /// Lists files in a remote folder on the reMarkable cloud.
    /// Used for duplicate name detection per RemarkableIntegrationSpecification §4(3).
    /// </summary>
    /// <param name="remotePath">The folder path to list.</param>
    /// <param name="configPath">The rmapi configuration file path for the target device.</param>
    /// <returns>A list of file/folder names in the folder.</returns>
    Task<List<string>> ListFolderAsync(string remotePath, string configPath);

    /// <summary>
    /// Gets the configuration file path for a given device ID.
    /// Returns %AppData%\ManuscriptaTeacherApp\rmapi\{deviceId}.conf.
    /// Per RemarkableIntegrationSpecification §3(2)(c).
    /// </summary>
    /// <param name="deviceId">The device UUID.</param>
    /// <returns>The full configuration file path.</returns>
    string GetConfigPath(Guid deviceId);
}

/// <summary>
/// Exception thrown when rmapi reports an authentication failure.
/// </summary>
public class RmapiAuthException : Exception
{
    public RmapiAuthException(string message) : base(message) { }
    public RmapiAuthException(string message, Exception innerException) : base(message, innerException) { }
}

/// <summary>
/// Exception thrown when rmapi fails for non-authentication reasons.
/// </summary>
public class RmapiException : Exception
{
    public RmapiException(string message) : base(message) { }
    public RmapiException(string message, Exception innerException) : base(message, innerException) { }
}
