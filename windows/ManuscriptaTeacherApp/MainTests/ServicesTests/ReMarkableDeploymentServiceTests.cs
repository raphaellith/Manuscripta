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

namespace MainTests.ServicesTests;

/// <summary>
/// Tests for ReMarkableDeploymentService.
/// Verifies deployment orchestration per RemarkableIntegrationSpecification §4.
/// </summary>
public class ReMarkableDeploymentServiceTests
{
    private readonly Mock<IMaterialPdfService> _mockPdfService;
    private readonly Mock<IRmapiService> _mockRmapiService;
    private readonly Mock<IReMarkableDeviceRepository> _mockDeviceRepository;
    private readonly Mock<IMaterialRepository> _mockMaterialRepository;
    private readonly Mock<ILogger<ReMarkableDeploymentService>> _mockLogger;
    private readonly ReMarkableDeploymentService _service;

    public ReMarkableDeploymentServiceTests()
    {
        _mockPdfService = new Mock<IMaterialPdfService>();
        _mockRmapiService = new Mock<IRmapiService>();
        _mockDeviceRepository = new Mock<IReMarkableDeviceRepository>();
        _mockMaterialRepository = new Mock<IMaterialRepository>();
        _mockLogger = new Mock<ILogger<ReMarkableDeploymentService>>();

        _service = new ReMarkableDeploymentService(
            _mockPdfService.Object,
            _mockRmapiService.Object,
            _mockDeviceRepository.Object,
            _mockMaterialRepository.Object,
            _mockLogger.Object);
    }

    #region Constructor Tests

    [Fact]
    public void Constructor_NullPdfService_ThrowsArgumentNullException()
    {
        Assert.Throws<ArgumentNullException>(() => new ReMarkableDeploymentService(
            null!,
            _mockRmapiService.Object,
            _mockDeviceRepository.Object,
            _mockMaterialRepository.Object,
            _mockLogger.Object));
    }

    [Fact]
    public void Constructor_NullRmapiService_ThrowsArgumentNullException()
    {
        Assert.Throws<ArgumentNullException>(() => new ReMarkableDeploymentService(
            _mockPdfService.Object,
            null!,
            _mockDeviceRepository.Object,
            _mockMaterialRepository.Object,
            _mockLogger.Object));
    }

    [Fact]
    public void Constructor_NullDeviceRepository_ThrowsArgumentNullException()
    {
        Assert.Throws<ArgumentNullException>(() => new ReMarkableDeploymentService(
            _mockPdfService.Object,
            _mockRmapiService.Object,
            null!,
            _mockMaterialRepository.Object,
            _mockLogger.Object));
    }

    [Fact]
    public void Constructor_NullMaterialRepository_ThrowsArgumentNullException()
    {
        Assert.Throws<ArgumentNullException>(() => new ReMarkableDeploymentService(
            _mockPdfService.Object,
            _mockRmapiService.Object,
            _mockDeviceRepository.Object,
            null!,
            _mockLogger.Object));
    }

    [Fact]
    public void Constructor_NullLogger_ThrowsArgumentNullException()
    {
        Assert.Throws<ArgumentNullException>(() => new ReMarkableDeploymentService(
            _mockPdfService.Object,
            _mockRmapiService.Object,
            _mockDeviceRepository.Object,
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
    public async Task DeployAsync_DelegatesPdfGeneration()
    {
        var materialId = Guid.NewGuid();
        var deviceId = Guid.NewGuid();
        var pdfBytes = new byte[] { 0x25, 0x50, 0x44, 0x46 }; // %PDF

        _mockMaterialRepository.Setup(r => r.GetByIdAsync(materialId))
            .ReturnsAsync(new WorksheetMaterialEntity(materialId, Guid.NewGuid(), "Test Material", "Content"));
        _mockPdfService.Setup(s => s.GeneratePdfAsync(materialId))
            .ReturnsAsync(pdfBytes);
        _mockDeviceRepository.Setup(r => r.GetByIdAsync(deviceId))
            .ReturnsAsync(new ReMarkableDeviceEntity(deviceId, "Test Device"));
        _mockRmapiService.Setup(s => s.GetConfigPath(deviceId)).Returns("/tmp/test.conf");
        _mockRmapiService.Setup(s => s.ListFolderAsync(It.IsAny<string>(), It.IsAny<string>()))
            .ReturnsAsync(new List<string>());

        await _service.DeployAsync(materialId, new List<Guid> { deviceId });

        // Verify PDF generation was called exactly once
        _mockPdfService.Verify(s => s.GeneratePdfAsync(materialId), Times.Once);
    }

    [Fact]
    public async Task DeployAsync_DeviceNotFound_ReturnsFailureResult()
    {
        var materialId = Guid.NewGuid();
        var deviceId = Guid.NewGuid();

        _mockMaterialRepository.Setup(r => r.GetByIdAsync(materialId))
            .ReturnsAsync(new WorksheetMaterialEntity(materialId, Guid.NewGuid(), "Test", "Content"));
        _mockPdfService.Setup(s => s.GeneratePdfAsync(materialId))
            .ReturnsAsync(new byte[] { 0x25 });
        _mockDeviceRepository.Setup(r => r.GetByIdAsync(deviceId))
            .ReturnsAsync((ReMarkableDeviceEntity?)null);

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
        _mockPdfService.Setup(s => s.GeneratePdfAsync(materialId))
            .ReturnsAsync(new byte[] { 0x25 });
        _mockDeviceRepository.Setup(r => r.GetByIdAsync(deviceId))
            .ReturnsAsync(new ReMarkableDeviceEntity(deviceId, "Device"));
        _mockRmapiService.Setup(s => s.GetConfigPath(deviceId)).Returns("/tmp/test.conf");
        _mockRmapiService.Setup(s => s.EnsureFolderExistsAsync(It.IsAny<string>(), It.IsAny<string>()))
            .ThrowsAsync(new RmapiAuthException("Token expired"));

        var results = await _service.DeployAsync(materialId, new List<Guid> { deviceId });

        Assert.Single(results);
        Assert.False(results[0].Success);
        Assert.True(results[0].AuthFailed);
    }

    [Fact]
    public async Task DeployAsync_MultipleDevices_ReturnsResultForEach()
    {
        var materialId = Guid.NewGuid();
        var device1 = Guid.NewGuid();
        var device2 = Guid.NewGuid();

        _mockMaterialRepository.Setup(r => r.GetByIdAsync(materialId))
            .ReturnsAsync(new WorksheetMaterialEntity(materialId, Guid.NewGuid(), "Test", "Content"));
        _mockPdfService.Setup(s => s.GeneratePdfAsync(materialId))
            .ReturnsAsync(new byte[] { 0x25, 0x50, 0x44, 0x46 });
        // Device 1 exists, Device 2 doesn't
        _mockDeviceRepository.Setup(r => r.GetByIdAsync(device1))
            .ReturnsAsync(new ReMarkableDeviceEntity(device1, "Device 1"));
        _mockDeviceRepository.Setup(r => r.GetByIdAsync(device2))
            .ReturnsAsync((ReMarkableDeviceEntity?)null);
        _mockRmapiService.Setup(s => s.GetConfigPath(device1)).Returns("/tmp/d1.conf");
        _mockRmapiService.Setup(s => s.ListFolderAsync(It.IsAny<string>(), It.IsAny<string>()))
            .ReturnsAsync(new List<string>());

        var results = await _service.DeployAsync(materialId, new List<Guid> { device1, device2 });

        Assert.Equal(2, results.Count);
        // One should succeed (device1 exists), one should fail (device2 not found)
        Assert.Contains(results, r => r.DeviceId == device2 && !r.Success);
    }

    #endregion

    #region GetUniqueFileName Tests

    [Fact]
    public void GetUniqueFileName_NoConflict_ReturnsBaseWithPdf()
    {
        var result = ReMarkableDeploymentService.GetUniqueFileName("My Material", new List<string>());
        Assert.Equal("My Material.pdf", result);
    }

    [Fact]
    public void GetUniqueFileName_ConflictWithExistingName_ReturnsSuffixed()
    {
        var existing = new List<string> { "My Material" };
        var result = ReMarkableDeploymentService.GetUniqueFileName("My Material", existing);
        Assert.Equal("My Material (1).pdf", result);
    }

    [Fact]
    public void GetUniqueFileName_ConflictWithExistingPdf_ReturnsSuffixed()
    {
        var existing = new List<string> { "My Material.pdf" };
        var result = ReMarkableDeploymentService.GetUniqueFileName("My Material", existing);
        Assert.Equal("My Material (1).pdf", result);
    }

    [Fact]
    public void GetUniqueFileName_MultipleDuplicates_IncrementsCorrectly()
    {
        var existing = new List<string> { "My Material", "My Material (1)", "My Material (2)" };
        var result = ReMarkableDeploymentService.GetUniqueFileName("My Material", existing);
        Assert.Equal("My Material (3).pdf", result);
    }

    #endregion

    #region SanitiseFileName Tests

    [Fact]
    public void SanitiseFileName_ValidTitle_ReturnsSameTitle()
    {
        Assert.Equal("My Material", ReMarkableDeploymentService.SanitiseFileName("My Material"));
    }

    [Fact]
    public void SanitiseFileName_EmptyTitle_ReturnsDefault()
    {
        Assert.Equal("Untitled Material", ReMarkableDeploymentService.SanitiseFileName(""));
    }

    [Fact]
    public void SanitiseFileName_WhitespaceTitle_ReturnsDefault()
    {
        Assert.Equal("Untitled Material", ReMarkableDeploymentService.SanitiseFileName("   "));
    }

    [Fact]
    public void SanitiseFileName_NullTitle_ReturnsDefault()
    {
        Assert.Equal("Untitled Material", ReMarkableDeploymentService.SanitiseFileName(null!));
    }

    [Fact]
    public void SanitiseFileName_InvalidChars_ReplacedWithUnderscore()
    {
        // Use chars that are invalid on ALL platforms (null char is universally invalid)
        var invalidChars = Path.GetInvalidFileNameChars();
        if (invalidChars.Length > 0)
        {
            var testTitle = $"Test{invalidChars[0]}Material";
            var result = ReMarkableDeploymentService.SanitiseFileName(testTitle);
            // Result should not contain any invalid filename characters
            Assert.DoesNotContain(result, c => invalidChars.Contains(c));
        }
    }

    [Fact]
    public void SanitiseFileName_TrailingDots_Removed()
    {
        var result = ReMarkableDeploymentService.SanitiseFileName("Title...");
        Assert.False(result.EndsWith('.'));
    }

    [Fact]
    public void SanitiseFileName_MultipleSpaces_Collapsed()
    {
        var result = ReMarkableDeploymentService.SanitiseFileName("Title    With    Spaces");
        Assert.DoesNotContain("  ", result);
    }

    #endregion
}
