using Main.Services.Repositories;

namespace Main.Services;

/// <summary>
/// Service for deploying materials to reMarkable devices.
/// Per RemarkableIntegrationSpecification §4.
/// </summary>
public class ReMarkableDeploymentService : IReMarkableDeploymentService
{
    private readonly IMaterialPdfService _pdfService;
    private readonly IRmapiService _rmapiService;
    private readonly IReMarkableDeviceRepository _deviceRepository;
    private readonly IMaterialRepository _materialRepository;
    private readonly ILogger<ReMarkableDeploymentService> _logger;

    /// <summary>
    /// Default folder on reMarkable cloud for uploaded materials.
    /// Per RemarkableIntegrationSpecification §4(2)(c).
    /// </summary>
    private const string DefaultRemoteFolder = "/Manuscripta";

    public ReMarkableDeploymentService(
        IMaterialPdfService pdfService,
        IRmapiService rmapiService,
        IReMarkableDeviceRepository deviceRepository,
        IMaterialRepository materialRepository,
        ILogger<ReMarkableDeploymentService> logger)
    {
        _pdfService = pdfService ?? throw new ArgumentNullException(nameof(pdfService));
        _rmapiService = rmapiService ?? throw new ArgumentNullException(nameof(rmapiService));
        _deviceRepository = deviceRepository ?? throw new ArgumentNullException(nameof(deviceRepository));
        _materialRepository = materialRepository ?? throw new ArgumentNullException(nameof(materialRepository));
        _logger = logger ?? throw new ArgumentNullException(nameof(logger));
    }

    /// <inheritdoc />
    public async Task<List<ReMarkableDeploymentResult>> DeployAsync(Guid materialId, List<Guid> deviceIds)
    {
        // Fetch material to get the title for filename
        var material = await _materialRepository.GetByIdAsync(materialId)
            ?? throw new KeyNotFoundException($"Material {materialId} not found.");

        // Per §4(2)(a): generate PDF
        var pdfBytes = await _pdfService.GeneratePdfAsync(materialId);

        // Per §4(2)(b): name using material title
        var baseFileName = SanitiseFileName(material.Title);

        var results = new List<ReMarkableDeploymentResult>();

        foreach (var deviceId in deviceIds)
        {
            results.Add(await DeployToDeviceAsync(deviceId, pdfBytes, baseFileName));
        }

        return results;
    }

    /// <summary>
    /// Deploys a PDF to a single reMarkable device.
    /// </summary>
    private async Task<ReMarkableDeploymentResult> DeployToDeviceAsync(
        Guid deviceId, byte[] pdfBytes, string baseFileName)
    {
        try
        {
            var device = await _deviceRepository.GetByIdAsync(deviceId);
            if (device == null)
            {
                return new ReMarkableDeploymentResult
                {
                    DeviceId = deviceId,
                    Success = false,
                    ErrorMessage = $"Device {deviceId} not found."
                };
            }

            var configPath = _rmapiService.GetConfigPath(deviceId);

            // Ensure /Manuscripta folder exists
            await _rmapiService.EnsureFolderExistsAsync(DefaultRemoteFolder, configPath);

            // Per §4(3): handle duplicate naming with numerical suffixes
            var existingFiles = await _rmapiService.ListFolderAsync(DefaultRemoteFolder, configPath);
            var fileName = GetUniqueFileName(baseFileName, existingFiles);

            // Write PDF to temp file
            var tempFilePath = Path.Combine(Path.GetTempPath(), fileName);
            try
            {
                await File.WriteAllBytesAsync(tempFilePath, pdfBytes);

                // Per §4(2)(c): upload via rmapi put
                await _rmapiService.UploadFileAsync(tempFilePath, DefaultRemoteFolder, configPath);

                _logger.LogInformation("Deployed material to reMarkable device {DeviceId} as {FileName}",
                    deviceId, fileName);

                return new ReMarkableDeploymentResult
                {
                    DeviceId = deviceId,
                    Success = true
                };
            }
            finally
            {
                // Clean up temp file
                if (File.Exists(tempFilePath))
                {
                    File.Delete(tempFilePath);
                }
            }
        }
        catch (RmapiAuthException ex)
        {
            // Per §4(5): authentication error
            _logger.LogWarning(ex, "Auth failure deploying to device {DeviceId}", deviceId);
            return new ReMarkableDeploymentResult
            {
                DeviceId = deviceId,
                Success = false,
                AuthFailed = true,
                ErrorMessage = ex.Message
            };
        }
        catch (Exception ex)
        {
            // Per §4(6): other errors
            _logger.LogError(ex, "Failed to deploy to device {DeviceId}", deviceId);
            return new ReMarkableDeploymentResult
            {
                DeviceId = deviceId,
                Success = false,
                ErrorMessage = ex.Message
            };
        }
    }

    /// <summary>
    /// Generates a unique filename with numerical suffix if needed.
    /// Per RemarkableIntegrationSpecification §4(3).
    /// </summary>
    public static string GetUniqueFileName(string baseFileName, List<string> existingFiles)
    {
        var pdfName = $"{baseFileName}.pdf";
        if (!existingFiles.Contains(baseFileName) && !existingFiles.Contains(pdfName))
        {
            return pdfName;
        }

        for (int i = 1; ; i++)
        {
            var candidate = $"{baseFileName} ({i}).pdf";
            var candidateNoExt = $"{baseFileName} ({i})";
            if (!existingFiles.Contains(candidateNoExt) && !existingFiles.Contains(candidate))
            {
                return candidate;
            }
        }
    }

    /// <summary>
    /// Sanitises a material title for use as a Windows-safe filename.
    /// </summary>
    public static string SanitiseFileName(string title)
    {
        if (string.IsNullOrWhiteSpace(title))
        {
            return "Untitled Material";
        }

        // Remove invalid filename characters
        var invalidChars = Path.GetInvalidFileNameChars();
        var sanitised = new string(title
            .Select(c => invalidChars.Contains(c) ? '_' : c)
            .ToArray());

        // Trim and collapse whitespace
        sanitised = sanitised.Trim();
        while (sanitised.Contains("  "))
        {
            sanitised = sanitised.Replace("  ", " ");
        }

        // Remove trailing dots/spaces (invalid on Windows)
        sanitised = sanitised.TrimEnd('.', ' ');

        if (string.IsNullOrWhiteSpace(sanitised))
        {
            return "Untitled Material";
        }

        return sanitised;
    }
}
