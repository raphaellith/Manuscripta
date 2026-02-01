using Microsoft.AspNetCore.Mvc;
using Main.Services.Repositories;

namespace Main.Controllers;

/// <summary>
/// Controller for serving attachment files to Android clients.
/// Implements API Contract.md §2.1.3.
/// </summary>
[ApiController]
[Route("api/v1")]
public class AttachmentController : ControllerBase
{
    private readonly IAttachmentRepository _attachmentRepository;
    private readonly ILogger<AttachmentController> _logger;
    private readonly string _attachmentsDirectory;

    public AttachmentController(
        IAttachmentRepository attachmentRepository,
        ILogger<AttachmentController> logger)
    {
        _attachmentRepository = attachmentRepository ?? throw new ArgumentNullException(nameof(attachmentRepository));
        _logger = logger ?? throw new ArgumentNullException(nameof(logger));
        
        // Attachments stored in %APPDATA%/ManuscriptaTeacherApp/Attachments
        _attachmentsDirectory = Path.Combine(
            Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData),
            "ManuscriptaTeacherApp",
            "Attachments");
    }

    /// <summary>
    /// Downloads an attachment file by ID.
    /// Per API Contract.md §2.1.3.
    /// </summary>
    /// <param name="id">The attachment UUID</param>
    /// <returns>The file content with appropriate Content-Type header</returns>
    [HttpGet("attachments/{id}")]
    [ProducesResponseType(StatusCodes.Status200OK)]
    [ProducesResponseType(StatusCodes.Status400BadRequest)]
    [ProducesResponseType(StatusCodes.Status404NotFound)]
    public async Task<IActionResult> GetAttachment(string id)
    {
        if (string.IsNullOrWhiteSpace(id))
        {
            _logger.LogWarning("Attachment request with empty ID");
            return BadRequest(new { error = "Attachment ID is required" });
        }

        if (!Guid.TryParse(id, out var attachmentId))
        {
            _logger.LogWarning("Attachment request with invalid ID format: {Id}", id);
            return BadRequest(new { error = "Attachment ID must be a valid GUID" });
        }

        // Look up attachment entity
        var attachment = await _attachmentRepository.GetByIdAsync(attachmentId);
        if (attachment == null)
        {
            _logger.LogWarning("Attachment not found: {Id}", attachmentId);
            return NotFound(new { error = "Attachment not found" });
        }

        // Construct file path: {uuid}.{extension}
        var fileName = $"{attachmentId}.{attachment.FileExtension}";
        var filePath = Path.Combine(_attachmentsDirectory, fileName);

        if (!System.IO.File.Exists(filePath))
        {
            _logger.LogError("Attachment file missing on disk: {FilePath}", filePath);
            return NotFound(new { error = "Attachment file not found" });
        }

        // Get content type based on extension
        var contentType = GetContentType(attachment.FileExtension);
        
        _logger.LogInformation("Serving attachment {Id} as {ContentType}", attachmentId, contentType);

        // Return file with original filename for download
        var downloadName = $"{attachment.FileBaseName}.{attachment.FileExtension}";
        return PhysicalFile(filePath, contentType, downloadName);
    }

    /// <summary>
    /// Maps file extension to MIME content type.
    /// Per AdditionalValidationRules.md §3B(2)(b) - only png, jpeg, pdf are allowed.
    /// </summary>
    private static string GetContentType(string extension)
    {
        return extension.ToLowerInvariant() switch
        {
            "png" => "image/png",
            "jpeg" => "image/jpeg",
            "jpg" => "image/jpeg",
            "pdf" => "application/pdf",
            _ => "application/octet-stream"
        };
    }
}
