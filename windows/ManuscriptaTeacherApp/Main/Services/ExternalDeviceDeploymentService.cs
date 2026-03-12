using Main.Services.Repositories;
using Main.Models.Entities;
using Microsoft.Extensions.Logging;
using System.IO;
using System.Linq;

namespace Main.Services;

/// <summary>
/// Implementation of IExternalDeviceDeploymentService delegating to IRmapiService or IEmailService
/// depending on the Type of the ExternalDeviceEntity.
/// Per ExternalDeviceIntegrationSpecification §3.
/// </summary>
public class ExternalDeviceDeploymentService : IExternalDeviceDeploymentService
{
    private readonly IMaterialPdfService _pdfService;
    private readonly IRmapiService _rmapiService;
    private readonly IEmailService _emailService;
    private readonly IExternalDeviceRepository _deviceRepository;
    private readonly IEmailCredentialRepository _emailCredentialRepository;
    private readonly IMaterialRepository _materialRepository;
    private readonly ILogger<ExternalDeviceDeploymentService> _logger;

    private const string DefaultRemoteFolder = "/Manuscripta";

    public ExternalDeviceDeploymentService(
        IMaterialPdfService pdfService,
        IRmapiService rmapiService,
        IEmailService emailService,
        IExternalDeviceRepository deviceRepository,
        IEmailCredentialRepository emailCredentialRepository,
        IMaterialRepository materialRepository,
        ILogger<ExternalDeviceDeploymentService> logger)
    {
        _pdfService = pdfService ?? throw new ArgumentNullException(nameof(pdfService));
        _rmapiService = rmapiService ?? throw new ArgumentNullException(nameof(rmapiService));
        _emailService = emailService ?? throw new ArgumentNullException(nameof(emailService));
        _deviceRepository = deviceRepository ?? throw new ArgumentNullException(nameof(deviceRepository));
        _emailCredentialRepository = emailCredentialRepository ?? throw new ArgumentNullException(nameof(emailCredentialRepository));
        _materialRepository = materialRepository ?? throw new ArgumentNullException(nameof(materialRepository));
        _logger = logger ?? throw new ArgumentNullException(nameof(logger));
    }

    /// <inheritdoc />
    public async Task<List<ExternalDeviceDeploymentResult>> DeployAsync(Guid materialId, List<Guid> deviceIds)
    {
        // 1. Fetch material to get the title for filename
        var material = await _materialRepository.GetByIdAsync(materialId)
            ?? throw new KeyNotFoundException($"Material {materialId} not found.");

        // 2. Name using material title
        var baseFileName = SanitiseFileName(material.Title);

        // 3. Per ExternalDeviceIntegrationSpecification §4(1)(a): generate per-device PDF
        //    passing target device ID for per-device settings resolution
        var results = new List<ExternalDeviceDeploymentResult>();
        foreach (var deviceId in deviceIds)
        {
            var pdfBytes = await _pdfService.GeneratePdfAsync(materialId, deviceId);
            results.Add(await DeployToDeviceAsync(deviceId, pdfBytes, baseFileName));
        }

        return results;
    }

    private async Task<ExternalDeviceDeploymentResult> DeployToDeviceAsync(
        Guid deviceId, byte[] pdfBytes, string baseFileName)
    {
        var device = await _deviceRepository.GetByIdAsync(deviceId);
        if (device == null)
        {
            return new ExternalDeviceDeploymentResult
            {
                DeviceId = deviceId,
                Success = false,
                ErrorMessage = $"Device {deviceId} not found."
            };
        }

        return device.Type switch
        {
            ExternalDeviceType.REMARKABLE => await DeployToReMarkableAsync(device, pdfBytes, baseFileName),
            ExternalDeviceType.KINDLE => await DeployToKindleAsync(device, pdfBytes, baseFileName),
            _ => new ExternalDeviceDeploymentResult { DeviceId = deviceId, Success = false, ErrorMessage = $"Unsupported device type: {device.Type}" }
        };
    }

    private async Task<ExternalDeviceDeploymentResult> DeployToReMarkableAsync(
        ExternalDeviceEntity device, byte[] pdfBytes, string baseFileName)
    {
        try
        {
            var configPath = _rmapiService.GetConfigPath(device.DeviceId);

            // Ensure /Manuscripta folder exists
            await _rmapiService.EnsureFolderExistsAsync(DefaultRemoteFolder, configPath);

            // Handle duplicate naming with numerical suffixes
            var existingFiles = await _rmapiService.ListFolderAsync(DefaultRemoteFolder, configPath);
            var fileName = GetUniqueFileName(baseFileName, existingFiles);

            // Write PDF to temp file
            var tempFilePath = Path.Combine(Path.GetTempPath(), fileName);
            try
            {
                await File.WriteAllBytesAsync(tempFilePath, pdfBytes);

                // Upload via rmapi put
                await _rmapiService.UploadFileAsync(tempFilePath, DefaultRemoteFolder, configPath);

                _logger.LogInformation("Deployed material to reMarkable device {DeviceId} as {FileName}", device.DeviceId, fileName);

                return new ExternalDeviceDeploymentResult { DeviceId = device.DeviceId, Success = true };
            }
            finally
            {
                if (File.Exists(tempFilePath))
                    File.Delete(tempFilePath);
            }
        }
        catch (RmapiAuthException ex)
        {
            _logger.LogWarning(ex, "Auth failure deploying to reMarkable device {DeviceId}", device.DeviceId);
            return new ExternalDeviceDeploymentResult { DeviceId = device.DeviceId, Success = false, AuthFailed = true, ErrorMessage = ex.Message };
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Failed to deploy to reMarkable {DeviceId}", device.DeviceId);
            return new ExternalDeviceDeploymentResult { DeviceId = device.DeviceId, Success = false, ErrorMessage = ex.Message };
        }
    }

    private async Task<ExternalDeviceDeploymentResult> DeployToKindleAsync(
        ExternalDeviceEntity device, byte[] pdfBytes, string baseFileName)
    {
        try
        {
            var credentials = await _emailCredentialRepository.GetCredentialsAsync();
            if (credentials == null)
            {
                throw new InvalidOperationException("Email credentials have not been configured.");
            }

            // Kindle endpoint expects to receive PDFs as attachments. We just use the name for the subject.
            var pdfName = $"{baseFileName}.pdf";
            var subject = $"Send to Kindle: {baseFileName}";
            var body = "Please find the attached material from Manuscripta.";

            var kindleEmail = device.ConfigurationData;
            if (string.IsNullOrWhiteSpace(kindleEmail) || !kindleEmail.Contains('@'))
            {
                throw new InvalidOperationException($"Invalid Kindle email address configured for device '{device.Name}'.");
            }

            await _emailService.SendEmailWithAttachmentAsync(
                credentials,
                kindleEmail,
                subject,
                body,
                pdfBytes,
                pdfName);

            _logger.LogInformation("Deployed material to Kindle {DeviceId} ({Email})", device.DeviceId, kindleEmail);

            return new ExternalDeviceDeploymentResult { DeviceId = device.DeviceId, Success = true };
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Failed to deploy to Kindle {DeviceId}", device.DeviceId);
            // Email errors don't count as "AuthFailed" for the *device* (it's for the backend email config), 
            // so we just return it as a general error. The frontend shouldn't prompt for device re-auth.
            // If the SMTP credentials failed, it throws an error which the TeacherPortalHub will catch and
            // dispatch an `EmailCredentialsNotConfigured` handler.
            return new ExternalDeviceDeploymentResult { DeviceId = device.DeviceId, Success = false, ErrorMessage = ex.Message };
        }
    }

    public static string GetUniqueFileName(string baseFileName, List<string> existingFiles)
    {
        var pdfName = $"{baseFileName}.pdf";
        if (!existingFiles.Contains(baseFileName) && !existingFiles.Contains(pdfName))
            return pdfName;

        for (int i = 1; ; i++)
        {
            var candidate = $"{baseFileName} ({i}).pdf";
            var candidateNoExt = $"{baseFileName} ({i})";
            if (!existingFiles.Contains(candidateNoExt) && !existingFiles.Contains(candidate))
                return candidate;
        }
    }

    public static string SanitiseFileName(string title)
    {
        if (string.IsNullOrWhiteSpace(title)) return "Untitled Material";

        var invalidChars = Path.GetInvalidFileNameChars();
        var sanitised = new string(title.Select(c => invalidChars.Contains(c) ? '_' : c).ToArray());
        
        sanitised = sanitised.Trim();
        while (sanitised.Contains("  ")) sanitised = sanitised.Replace("  ", " ");
        sanitised = sanitised.TrimEnd('.', ' ');

        return string.IsNullOrWhiteSpace(sanitised) ? "Untitled Material" : sanitised;
    }
}
