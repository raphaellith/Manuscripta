using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Threading.Tasks;
using Moq;
using Xunit;
using Main.Models.Entities;
using Main.Models.Entities.Materials;
using Main.Services;
using Main.Services.Repositories;
using Microsoft.Extensions.Logging;
using MimeKit;

namespace MainTests.ServicesTests;

/// <summary>
/// Tests for ExternalDeviceDeploymentService.
/// Verifies deployment orchestration for both Kindle and ReMarkable.
/// </summary>
public class ExternalDeviceDeploymentServiceTests
{
    private readonly Mock<IMaterialPdfService> _mockPdfService;
    private readonly Mock<IRmapiService> _mockRmapiService;
    private readonly Mock<IEmailService> _mockEmailService;
    private readonly Mock<IExternalDeviceRepository> _mockDeviceRepository;
    private readonly Mock<IMaterialRepository> _mockMaterialRepository;
    private readonly Mock<IEmailCredentialRepository> _mockEmailCredRepo;
    private readonly Mock<ILogger<ExternalDeviceDeploymentService>> _mockLogger;
    private readonly ExternalDeviceDeploymentService _service;

    public ExternalDeviceDeploymentServiceTests()
    {
        _mockPdfService = new Mock<IMaterialPdfService>();
        _mockRmapiService = new Mock<IRmapiService>();
        _mockEmailService = new Mock<IEmailService>();
        _mockDeviceRepository = new Mock<IExternalDeviceRepository>();
        _mockMaterialRepository = new Mock<IMaterialRepository>();
        _mockEmailCredRepo = new Mock<IEmailCredentialRepository>();
        _mockLogger = new Mock<ILogger<ExternalDeviceDeploymentService>>();

        _mockEmailCredRepo.Setup(r => r.GetCredentialsAsync()).ReturnsAsync(new EmailCredentialEntity(
            Guid.NewGuid(),
            "test@example.com",
            "smtp.example.com",
            587,
            "password"
        ));

        _service = new ExternalDeviceDeploymentService(
            _mockPdfService.Object,
            _mockRmapiService.Object,
            _mockEmailService.Object,
            _mockDeviceRepository.Object,
            _mockEmailCredRepo.Object,
            _mockMaterialRepository.Object,
            _mockLogger.Object);
    }

    #region Constructor Tests

    [Fact]
    public void Constructor_NullPdfService_ThrowsArgumentNullException()
    {
        Assert.Throws<ArgumentNullException>(() => new ExternalDeviceDeploymentService(
            null!,
            _mockRmapiService.Object,
            _mockEmailService.Object,
            _mockDeviceRepository.Object,
            _mockEmailCredRepo.Object,
            _mockMaterialRepository.Object,
            _mockLogger.Object));
    }

    [Fact]
    public void Constructor_NullRmapiService_ThrowsArgumentNullException()
    {
        Assert.Throws<ArgumentNullException>(() => new ExternalDeviceDeploymentService(
            _mockPdfService.Object,
            null!,
            _mockEmailService.Object,
            _mockDeviceRepository.Object,
            _mockEmailCredRepo.Object,
            _mockMaterialRepository.Object,
            _mockLogger.Object));
    }

    [Fact]
    public void Constructor_NullEmailService_ThrowsArgumentNullException()
    {
        Assert.Throws<ArgumentNullException>(() => new ExternalDeviceDeploymentService(
            _mockPdfService.Object,
            _mockRmapiService.Object,
            null!,
            _mockDeviceRepository.Object,
            _mockEmailCredRepo.Object,
            _mockMaterialRepository.Object,
            _mockLogger.Object));
    }

    [Fact]
    public void Constructor_NullDeviceRepository_ThrowsArgumentNullException()
    {
        Assert.Throws<ArgumentNullException>(() => new ExternalDeviceDeploymentService(
            _mockPdfService.Object,
            _mockRmapiService.Object,
            _mockEmailService.Object,
            null!,
            _mockEmailCredRepo.Object,
            _mockMaterialRepository.Object,
            _mockLogger.Object));
    }

    [Fact]
    public void Constructor_NullEmailCredRepo_ThrowsArgumentNullException()
    {
        Assert.Throws<ArgumentNullException>(() => new ExternalDeviceDeploymentService(
            _mockPdfService.Object,
            _mockRmapiService.Object,
            _mockEmailService.Object,
            _mockDeviceRepository.Object,
            null!,
            _mockMaterialRepository.Object,
            _mockLogger.Object));
    }

    [Fact]
    public void Constructor_NullMaterialRepository_ThrowsArgumentNullException()
    {
        Assert.Throws<ArgumentNullException>(() => new ExternalDeviceDeploymentService(
            _mockPdfService.Object,
            _mockRmapiService.Object,
            _mockEmailService.Object,
            _mockDeviceRepository.Object,
            _mockEmailCredRepo.Object,
            null!,
            _mockLogger.Object));
    }

    [Fact]
    public void Constructor_NullLogger_ThrowsArgumentNullException()
    {
        Assert.Throws<ArgumentNullException>(() => new ExternalDeviceDeploymentService(
            _mockPdfService.Object,
            _mockRmapiService.Object,
            _mockEmailService.Object,
            _mockDeviceRepository.Object,
            _mockEmailCredRepo.Object,
            _mockMaterialRepository.Object,
            null!));
    }

    #endregion

    #region DeployAsync Tests

    [Fact]
    public async Task DeployAsync_MaterialNotFound_ThrowsKeyNotFoundException()
    {
        var materialId = Guid.NewGuid();
        _mockMaterialRepository.Setup(r => r.GetByIdAsync(materialId))
            .ReturnsAsync((MaterialEntity?)null);

        await Assert.ThrowsAsync<KeyNotFoundException>(
            () => _service.DeployAsync(materialId, new List<Guid> { Guid.NewGuid() }));
    }

    [Fact]
    public async Task DeployAsync_DelegatesPdfGeneration_Remarkable()
    {
        var materialId = Guid.NewGuid();
        var deviceId = Guid.NewGuid();
        var pdfBytes = new byte[] { 0x25, 0x50, 0x44, 0x46 }; // %PDF

        _mockMaterialRepository.Setup(r => r.GetByIdAsync(materialId))
            .ReturnsAsync(new WorksheetMaterialEntity(materialId, Guid.NewGuid(), "Test Material", "Content"));
        _mockPdfService.Setup(s => s.GeneratePdfAsync(materialId, deviceId))
            .ReturnsAsync(pdfBytes);
        _mockDeviceRepository.Setup(r => r.GetByIdAsync(deviceId))
            .ReturnsAsync(new ExternalDeviceEntity(deviceId, "Test Device", ExternalDeviceType.REMARKABLE));
        _mockRmapiService.Setup(s => s.GetConfigPath(deviceId)).Returns("/tmp/test.conf");
        _mockRmapiService.Setup(s => s.ListFolderAsync(It.IsAny<string>(), It.IsAny<string>()))
            .ReturnsAsync(new List<string>());

        await _service.DeployAsync(materialId, new List<Guid> { deviceId });

        // Verify PDF generation was called exactly once with device ID
        _mockPdfService.Verify(s => s.GeneratePdfAsync(materialId, deviceId), Times.Once);
        // Verify RMAPI was called
        _mockRmapiService.Verify(s => s.UploadFileAsync(It.IsAny<string>(), It.IsAny<string>(), It.IsAny<string>()), Times.Once);
        // Verify Email was not called
        _mockEmailService.Verify(s => s.SendEmailWithAttachmentAsync(It.IsAny<EmailCredentialEntity>(), It.IsAny<string>(), It.IsAny<string>(), It.IsAny<string>(), It.IsAny<byte[]>(), It.IsAny<string>()), Times.Never);
    }

    [Fact]
    public async Task DeployAsync_DelegatesPdfGeneration_Kindle()
    {
        var materialId = Guid.NewGuid();
        var deviceId = Guid.NewGuid();
        var pdfBytes = new byte[] { 0x25, 0x50, 0x44, 0x46 }; // %PDF

        _mockMaterialRepository.Setup(r => r.GetByIdAsync(materialId))
            .ReturnsAsync(new WorksheetMaterialEntity(materialId, Guid.NewGuid(), "Test Material", "Content"));
        _mockPdfService.Setup(s => s.GeneratePdfAsync(materialId, deviceId))
            .ReturnsAsync(pdfBytes);
        _mockDeviceRepository.Setup(r => r.GetByIdAsync(deviceId))
            .ReturnsAsync(new ExternalDeviceEntity(deviceId, "Kindle Device", ExternalDeviceType.KINDLE) { ConfigurationData = "test@kindle.com" });
        
        await _service.DeployAsync(materialId, new List<Guid> { deviceId });

        // Verify PDF generation was called exactly once with device ID
        _mockPdfService.Verify(s => s.GeneratePdfAsync(materialId, deviceId), Times.Once);
        // Verify RMAPI was not called
        _mockRmapiService.Verify(s => s.UploadFileAsync(It.IsAny<string>(), It.IsAny<string>(), It.IsAny<string>()), Times.Never);
        // Verify Email was called
        _mockEmailService.Verify(s => s.SendEmailWithAttachmentAsync(It.IsAny<EmailCredentialEntity>(), "test@kindle.com", "Send to Kindle: Test Material", "Please find the attached material from Manuscripta.", pdfBytes, "Test Material.pdf"), Times.Once);
    }

    [Fact]
    public async Task DeployAsync_DeviceNotFound_ReturnsFailureResult()
    {
        var materialId = Guid.NewGuid();
        var deviceId = Guid.NewGuid();

        _mockMaterialRepository.Setup(r => r.GetByIdAsync(materialId))
            .ReturnsAsync(new WorksheetMaterialEntity(materialId, Guid.NewGuid(), "Test", "Content"));
        _mockPdfService.Setup(s => s.GeneratePdfAsync(materialId, deviceId))
            .ReturnsAsync(new byte[] { 0x25 });
        _mockDeviceRepository.Setup(r => r.GetByIdAsync(deviceId))
            .ReturnsAsync((ExternalDeviceEntity?)null);

        var results = await _service.DeployAsync(materialId, new List<Guid> { deviceId });

        Assert.Single(results);
        Assert.False(results[0].Success);
        Assert.Contains("not found", results[0].ErrorMessage!);
    }

    [Fact]
    public async Task DeployAsync_AuthFailure_ReturnsAuthFailedResult()
    {
        var materialId = Guid.NewGuid();
        var deviceId = Guid.NewGuid();

        _mockMaterialRepository.Setup(r => r.GetByIdAsync(materialId))
            .ReturnsAsync(new WorksheetMaterialEntity(materialId, Guid.NewGuid(), "Test", "Content"));
        _mockPdfService.Setup(s => s.GeneratePdfAsync(materialId, deviceId))
            .ReturnsAsync(new byte[] { 0x25 });
        _mockDeviceRepository.Setup(r => r.GetByIdAsync(deviceId))
            .ReturnsAsync(new ExternalDeviceEntity(deviceId, "Device", ExternalDeviceType.REMARKABLE));
        _mockRmapiService.Setup(s => s.GetConfigPath(deviceId)).Returns("/tmp/test.conf");
        _mockRmapiService.Setup(s => s.EnsureFolderExistsAsync(It.IsAny<string>(), It.IsAny<string>()))
            .ThrowsAsync(new RmapiAuthException("Token expired"));

        var results = await _service.DeployAsync(materialId, new List<Guid> { deviceId });

        Assert.Single(results);
        Assert.False(results[0].Success);
        Assert.True(results[0].AuthFailed);
    }

    [Fact]
    public async Task DeployAsync_MissingEmailCredentials_ReturnsFailureResult()
    {
        var materialId = Guid.NewGuid();
        var deviceId = Guid.NewGuid();

        _mockMaterialRepository.Setup(r => r.GetByIdAsync(materialId))
            .ReturnsAsync(new WorksheetMaterialEntity(materialId, Guid.NewGuid(), "Test", "Content"));
        _mockPdfService.Setup(s => s.GeneratePdfAsync(materialId, deviceId))
            .ReturnsAsync(new byte[] { 0x25 });
        _mockDeviceRepository.Setup(r => r.GetByIdAsync(deviceId))
            .ReturnsAsync(new ExternalDeviceEntity(deviceId, "Kindle Device", ExternalDeviceType.KINDLE) { ConfigurationData = "test@kindle.com" });
        
        _mockEmailCredRepo.Setup(r => r.GetCredentialsAsync()).ReturnsAsync((EmailCredentialEntity?)null);

        var results = await _service.DeployAsync(materialId, new List<Guid> { deviceId });

        Assert.Single(results);
        Assert.False(results[0].Success);
        Assert.Contains("Email credentials have not been configured", results[0].ErrorMessage!);
    }

    #endregion

    #region SanitiseFileName Tests

    [Fact]
    public void SanitiseFileName_ValidTitle_ReturnsSameTitle()
    {
        Assert.Equal("My Material", ExternalDeviceDeploymentService.SanitiseFileName("My Material"));
    }

    [Fact]
    public void SanitiseFileName_EmptyTitle_ReturnsDefault()
    {
        Assert.Equal("Untitled Material", ExternalDeviceDeploymentService.SanitiseFileName(""));
    }

    [Fact]
    public void SanitiseFileName_WhitespaceTitle_ReturnsDefault()
    {
        Assert.Equal("Untitled Material", ExternalDeviceDeploymentService.SanitiseFileName("   "));
    }

    [Fact]
    public void SanitiseFileName_NullTitle_ReturnsDefault()
    {
        Assert.Equal("Untitled Material", ExternalDeviceDeploymentService.SanitiseFileName(null!));
    }

    [Fact]
    public void SanitiseFileName_InvalidChars_ReplacedWithUnderscore()
    {
        var invalidChars = Path.GetInvalidFileNameChars();
        if (invalidChars.Length > 0)
        {
            var testTitle = $"Test{invalidChars[0]}Material";
            var result = ExternalDeviceDeploymentService.SanitiseFileName(testTitle);
            Assert.DoesNotContain(result, c => invalidChars.Contains(c));
        }
    }

    [Fact]
    public void SanitiseFileName_TrailingDots_Removed()
    {
        var result = ExternalDeviceDeploymentService.SanitiseFileName("Title...");
        Assert.False(result.EndsWith('.'));
    }

    [Fact]
    public void SanitiseFileName_MultipleSpaces_Collapsed()
    {
        var result = ExternalDeviceDeploymentService.SanitiseFileName("Title    With    Spaces");
        Assert.DoesNotContain("  ", result);
    }

    #endregion
}
