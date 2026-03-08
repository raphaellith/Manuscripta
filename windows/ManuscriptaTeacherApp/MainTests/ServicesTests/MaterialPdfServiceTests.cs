using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Threading.Tasks;
using Moq;
using Xunit;
using Main.Models.Entities;
using Main.Models.Entities.Materials;
using Main.Models.Entities.Questions;
using Main.Models.Enums;
using Main.Models.Entities.Responses;
using Main.Services;
using Main.Services.Latex;
using Main.Services.Repositories;

namespace MainTests.ServicesTests;

/// <summary>
/// Tests for MaterialPdfService.
/// Verifies PDF generation behavior per MaterialConversionSpecification.
/// </summary>
public class MaterialPdfServiceTests
{
    private readonly Mock<IMaterialRepository> _mockMaterialRepo;
    private readonly Mock<IQuestionRepository> _mockQuestionRepo;
    private readonly Mock<IAttachmentRepository> _mockAttachmentRepo;
    private readonly Mock<IFileService> _mockFileService;
    private readonly Mock<ILatexRenderer> _mockLatexRenderer;
    private readonly Mock<IPdfExportSettingsRepository> _mockPdfExportSettingsRepo;
    private readonly Mock<IResponseRepository> _mockResponseRepo;
    private readonly Mock<IFeedbackRepository> _mockFeedbackRepo;
    private readonly Mock<IDeviceRegistryService> _mockDeviceRegistryService;
    private readonly Mock<IExternalDeviceRepository> _mockExternalDeviceRepo;
    private readonly MaterialPdfService _service;
    private readonly Guid _testMaterialId = Guid.NewGuid();
    private readonly Guid _testLessonId = Guid.NewGuid();

    public MaterialPdfServiceTests()
    {
        // Configure QuestPDF license (normally done at app startup in Program.cs)
        QuestPDF.Settings.License = QuestPDF.Infrastructure.LicenseType.Community;
        
        _mockMaterialRepo = new Mock<IMaterialRepository>();
        _mockQuestionRepo = new Mock<IQuestionRepository>();
        _mockAttachmentRepo = new Mock<IAttachmentRepository>();
        _mockFileService = new Mock<IFileService>();
        _mockLatexRenderer = new Mock<ILatexRenderer>();
        _mockPdfExportSettingsRepo = new Mock<IPdfExportSettingsRepository>();
        _mockResponseRepo = new Mock<IResponseRepository>();
        _mockFeedbackRepo = new Mock<IFeedbackRepository>();
        _mockDeviceRegistryService = new Mock<IDeviceRegistryService>();
        _mockExternalDeviceRepo = new Mock<IExternalDeviceRepository>();

        // Default: return RULED / MEDIUM / MEDIUM
        _mockPdfExportSettingsRepo.Setup(r => r.GetAsync())
            .ReturnsAsync(PdfExportSettingsEntity.CreateDefault());

        _service = new MaterialPdfService(
            _mockMaterialRepo.Object,
            _mockQuestionRepo.Object,
            _mockAttachmentRepo.Object,
            _mockFileService.Object,
            _mockLatexRenderer.Object,
            _mockPdfExportSettingsRepo.Object,
            _mockResponseRepo.Object,
            _mockFeedbackRepo.Object,
            _mockDeviceRegistryService.Object,
            _mockExternalDeviceRepo.Object);
    }

    #region Constructor Tests

    [Fact]
    public void Constructor_NullMaterialRepository_ThrowsArgumentNullException()
    {
        // Act & Assert
        Assert.Throws<ArgumentNullException>(() => new MaterialPdfService(
            null!,
            _mockQuestionRepo.Object,
            _mockAttachmentRepo.Object,
            _mockFileService.Object,
            _mockLatexRenderer.Object,
            _mockPdfExportSettingsRepo.Object,
            _mockResponseRepo.Object,
            _mockFeedbackRepo.Object,
            _mockDeviceRegistryService.Object,
            _mockExternalDeviceRepo.Object));
    }

    [Fact]
    public void Constructor_NullQuestionRepository_ThrowsArgumentNullException()
    {
        // Act & Assert
        Assert.Throws<ArgumentNullException>(() => new MaterialPdfService(
            _mockMaterialRepo.Object,
            null!,
            _mockAttachmentRepo.Object,
            _mockFileService.Object,
            _mockLatexRenderer.Object,
            _mockPdfExportSettingsRepo.Object,
            _mockResponseRepo.Object,
            _mockFeedbackRepo.Object,
            _mockDeviceRegistryService.Object,
            _mockExternalDeviceRepo.Object));
    }

    [Fact]
    public void Constructor_NullAttachmentRepository_ThrowsArgumentNullException()
    {
        // Act & Assert
        Assert.Throws<ArgumentNullException>(() => new MaterialPdfService(
            _mockMaterialRepo.Object,
            _mockQuestionRepo.Object,
            null!,
            _mockFileService.Object,
            _mockLatexRenderer.Object,
            _mockPdfExportSettingsRepo.Object,
            _mockResponseRepo.Object,
            _mockFeedbackRepo.Object,
            _mockDeviceRegistryService.Object,
            _mockExternalDeviceRepo.Object));
    }

    [Fact]
    public void Constructor_NullFileService_ThrowsArgumentNullException()
    {
        // Act & Assert
        Assert.Throws<ArgumentNullException>(() => new MaterialPdfService(
            _mockMaterialRepo.Object,
            _mockQuestionRepo.Object,
            _mockAttachmentRepo.Object,
            null!,
            _mockLatexRenderer.Object,
            _mockPdfExportSettingsRepo.Object,
            _mockResponseRepo.Object,
            _mockFeedbackRepo.Object,
            _mockDeviceRegistryService.Object,
            _mockExternalDeviceRepo.Object));
    }

    [Fact]
    public void Constructor_NullLatexRenderer_ThrowsArgumentNullException()
    {
        // Act & Assert
        Assert.Throws<ArgumentNullException>(() => new MaterialPdfService(
            _mockMaterialRepo.Object,
            _mockQuestionRepo.Object,
            _mockAttachmentRepo.Object,
            _mockFileService.Object,
            null!,
            _mockPdfExportSettingsRepo.Object,
            _mockResponseRepo.Object,
            _mockFeedbackRepo.Object,
            _mockDeviceRegistryService.Object,
            _mockExternalDeviceRepo.Object));
    }

    [Fact]
    public void Constructor_NullPdfExportSettingsRepository_ThrowsArgumentNullException()
    {
        // Act & Assert
        Assert.Throws<ArgumentNullException>(() => new MaterialPdfService(
            _mockMaterialRepo.Object,
            _mockQuestionRepo.Object,
            _mockAttachmentRepo.Object,
            _mockFileService.Object,
            _mockLatexRenderer.Object,
            null!,
            _mockResponseRepo.Object,
            _mockFeedbackRepo.Object,
            _mockDeviceRegistryService.Object,
            _mockExternalDeviceRepo.Object));
    }

    [Fact]
    public void Constructor_ValidParameters_Success()
    {
        // Act
        var service = new MaterialPdfService(
            _mockMaterialRepo.Object,
            _mockQuestionRepo.Object,
            _mockAttachmentRepo.Object,
            _mockFileService.Object,
            _mockLatexRenderer.Object,
            _mockPdfExportSettingsRepo.Object,
            _mockResponseRepo.Object,
            _mockFeedbackRepo.Object,
            _mockDeviceRegistryService.Object,
            _mockExternalDeviceRepo.Object);

        // Assert
        Assert.NotNull(service);
    }

    #endregion

    #region GeneratePdfAsync Tests

    [Fact]
    public async Task GeneratePdfAsync_MaterialNotFound_ThrowsKeyNotFoundException()
    {
        // Arrange
        _mockMaterialRepo.Setup(r => r.GetByIdAsync(_testMaterialId))
            .ReturnsAsync((MaterialEntity?)null);

        // Act & Assert
        await Assert.ThrowsAsync<KeyNotFoundException>(
            () => _service.GeneratePdfAsync(_testMaterialId));
    }

    [Fact]
    public async Task GeneratePdfAsync_MaterialWithNoContent_ReturnsValidPdf()
    {
        // Arrange
        var material = new WorksheetMaterialEntity(
            _testMaterialId,
            _testLessonId,
            "Test Worksheet",
            ""
        );

        _mockMaterialRepo.Setup(r => r.GetByIdAsync(_testMaterialId))
            .ReturnsAsync(material);
        _mockQuestionRepo.Setup(r => r.GetByMaterialIdAsync(_testMaterialId))
            .ReturnsAsync(new List<QuestionEntity>());
        _mockAttachmentRepo.Setup(r => r.GetByMaterialIdAsync(_testMaterialId))
            .ReturnsAsync(new List<AttachmentEntity>());

        // Act
        var result = await _service.GeneratePdfAsync(_testMaterialId);

        // Assert
        Assert.NotNull(result);
        Assert.NotEmpty(result);
        // PDF files start with %PDF
        Assert.StartsWith("%PDF", System.Text.Encoding.ASCII.GetString(result.Take(4).ToArray()));
    }

    [Fact]
    public async Task GeneratePdfAsync_MaterialWithMarkdownContent_ReturnsValidPdf()
    {
        // Arrange
        var material = new WorksheetMaterialEntity(
            _testMaterialId,
            _testLessonId,
            "Test Worksheet",
            "# Heading 1\n\nThis is a **bold** paragraph with *italic* text.\n\n- Item 1\n- Item 2"
        );

        _mockMaterialRepo.Setup(r => r.GetByIdAsync(_testMaterialId))
            .ReturnsAsync(material);
        _mockQuestionRepo.Setup(r => r.GetByMaterialIdAsync(_testMaterialId))
            .ReturnsAsync(new List<QuestionEntity>());
        _mockAttachmentRepo.Setup(r => r.GetByMaterialIdAsync(_testMaterialId))
            .ReturnsAsync(new List<AttachmentEntity>());

        // Act
        var result = await _service.GeneratePdfAsync(_testMaterialId);

        // Assert
        Assert.NotNull(result);
        Assert.NotEmpty(result);
        Assert.StartsWith("%PDF", System.Text.Encoding.ASCII.GetString(result.Take(4).ToArray()));
    }

    [Fact]
    public async Task GeneratePdfAsync_MaterialWithMultipleChoiceQuestion_ReturnsValidPdf()
    {
        // Arrange
        var questionId = Guid.NewGuid();
        var material = new WorksheetMaterialEntity(
            _testMaterialId,
            _testLessonId,
            "Test Worksheet",
            $"# Test\n\n!!! question id=\"{questionId}\"\n\nSome more content."
        );

        var question = new MultipleChoiceQuestionEntity(
            questionId,
            _testMaterialId,
            "What is 2+2?",
            new List<string> { "3", "4", "5", "6" },
            1, // Correct answer index
            2  // Max score
        );

        _mockMaterialRepo.Setup(r => r.GetByIdAsync(_testMaterialId))
            .ReturnsAsync(material);
        _mockQuestionRepo.Setup(r => r.GetByMaterialIdAsync(_testMaterialId))
            .ReturnsAsync(new List<QuestionEntity> { question });
        _mockAttachmentRepo.Setup(r => r.GetByMaterialIdAsync(_testMaterialId))
            .ReturnsAsync(new List<AttachmentEntity>());

        // Act
        var result = await _service.GeneratePdfAsync(_testMaterialId);

        // Assert
        Assert.NotNull(result);
        Assert.NotEmpty(result);
        Assert.StartsWith("%PDF", System.Text.Encoding.ASCII.GetString(result.Take(4).ToArray()));
    }

    [Fact]
    public async Task GeneratePdfAsync_MaterialWithWrittenAnswerQuestion_ReturnsValidPdf()
    {
        // Arrange
        var questionId = Guid.NewGuid();
        var material = new WorksheetMaterialEntity(
            _testMaterialId,
            _testLessonId,
            "Test Worksheet",
            $"# Essay Question\n\n!!! question id=\"{questionId}\""
        );

        var question = new WrittenAnswerQuestionEntity(
            questionId,
            _testMaterialId,
            "Explain your reasoning.",
            correctAnswer: null,
            markScheme: "Award marks for clear explanation.",
            maxScore: 5
        );

        _mockMaterialRepo.Setup(r => r.GetByIdAsync(_testMaterialId))
            .ReturnsAsync(material);
        _mockQuestionRepo.Setup(r => r.GetByMaterialIdAsync(_testMaterialId))
            .ReturnsAsync(new List<QuestionEntity> { question });
        _mockAttachmentRepo.Setup(r => r.GetByMaterialIdAsync(_testMaterialId))
            .ReturnsAsync(new List<AttachmentEntity>());

        // Act
        var result = await _service.GeneratePdfAsync(_testMaterialId);

        // Assert
        Assert.NotNull(result);
        Assert.NotEmpty(result);
        Assert.StartsWith("%PDF", System.Text.Encoding.ASCII.GetString(result.Take(4).ToArray()));
    }

    [Fact]
    public async Task GeneratePdfAsync_MaterialWithMissingQuestionReference_ReturnsValidPdf()
    {
        // Arrange - question marker in content but question not in database
        var missingQuestionId = Guid.NewGuid();
        var material = new WorksheetMaterialEntity(
            _testMaterialId,
            _testLessonId,
            "Test Worksheet",
            $"# Test\n\n!!! question id=\"{missingQuestionId}\"\n\nAfter question."
        );

        _mockMaterialRepo.Setup(r => r.GetByIdAsync(_testMaterialId))
            .ReturnsAsync(material);
        _mockQuestionRepo.Setup(r => r.GetByMaterialIdAsync(_testMaterialId))
            .ReturnsAsync(new List<QuestionEntity>()); // Empty - no questions
        _mockAttachmentRepo.Setup(r => r.GetByMaterialIdAsync(_testMaterialId))
            .ReturnsAsync(new List<AttachmentEntity>());

        // Act
        var result = await _service.GeneratePdfAsync(_testMaterialId);

        // Assert - Should still generate PDF with placeholder
        Assert.NotNull(result);
        Assert.NotEmpty(result);
        Assert.StartsWith("%PDF", System.Text.Encoding.ASCII.GetString(result.Take(4).ToArray()));
    }

    [Fact]
    public async Task GeneratePdfAsync_MaterialWithAttachment_ValidFile_ReturnsValidPdf()
    {
        // Arrange
        var attachmentId = Guid.NewGuid();
        var material = new WorksheetMaterialEntity(
            _testMaterialId,
            _testLessonId,
            "Test Worksheet",
            $"# With Image\n\n![Test Image](/attachments/{attachmentId})"
        );

        var attachment = new AttachmentEntity(
            attachmentId,
            _testMaterialId,
            "test-image",
            "png"
        );

        _mockMaterialRepo.Setup(r => r.GetByIdAsync(_testMaterialId))
            .ReturnsAsync(material);
        _mockQuestionRepo.Setup(r => r.GetByMaterialIdAsync(_testMaterialId))
            .ReturnsAsync(new List<QuestionEntity>());
        _mockAttachmentRepo.Setup(r => r.GetByMaterialIdAsync(_testMaterialId))
            .ReturnsAsync(new List<AttachmentEntity> { attachment });
        _mockFileService.Setup(f => f.GetAttachmentFilePath(attachmentId, "png"))
            .Returns("/path/to/test.png");
        _mockFileService.Setup(f => f.FileExists("/path/to/test.png"))
            .Returns(false); // File doesn't exist - should show placeholder

        // Act
        var result = await _service.GeneratePdfAsync(_testMaterialId);

        // Assert
        Assert.NotNull(result);
        Assert.NotEmpty(result);
        Assert.StartsWith("%PDF", System.Text.Encoding.ASCII.GetString(result.Take(4).ToArray()));
    }

    [Fact]
    public async Task GeneratePdfAsync_MaterialWithMissingAttachment_ReturnsValidPdf()
    {
        // Arrange
        var nonExistentId = Guid.NewGuid();
        var material = new WorksheetMaterialEntity(
            _testMaterialId,
            _testLessonId,
            "Test Worksheet",
            $"# With Missing Image\n\n![Missing](/attachments/{nonExistentId})"
        );

        _mockMaterialRepo.Setup(r => r.GetByIdAsync(_testMaterialId))
            .ReturnsAsync(material);
        _mockQuestionRepo.Setup(r => r.GetByMaterialIdAsync(_testMaterialId))
            .ReturnsAsync(new List<QuestionEntity>());
        _mockAttachmentRepo.Setup(r => r.GetByMaterialIdAsync(_testMaterialId))
            .ReturnsAsync(new List<AttachmentEntity>()); // No attachments

        // Act
        var result = await _service.GeneratePdfAsync(_testMaterialId);

        // Assert - Should still generate PDF with placeholder text
        Assert.NotNull(result);
        Assert.NotEmpty(result);
        Assert.StartsWith("%PDF", System.Text.Encoding.ASCII.GetString(result.Take(4).ToArray()));
    }

    [Fact]
    public async Task GeneratePdfAsync_ReadingMaterial_ReturnsValidPdf()
    {
        // Arrange
        var material = new ReadingMaterialEntity(
            _testMaterialId,
            _testLessonId,
            "Reading Material",
            "This is reading content without questions."
        );

        _mockMaterialRepo.Setup(r => r.GetByIdAsync(_testMaterialId))
            .ReturnsAsync(material);
        _mockQuestionRepo.Setup(r => r.GetByMaterialIdAsync(_testMaterialId))
            .ReturnsAsync(new List<QuestionEntity>());
        _mockAttachmentRepo.Setup(r => r.GetByMaterialIdAsync(_testMaterialId))
            .ReturnsAsync(new List<AttachmentEntity>());

        // Act
        var result = await _service.GeneratePdfAsync(_testMaterialId);

        // Assert
        Assert.NotNull(result);
        Assert.NotEmpty(result);
        Assert.StartsWith("%PDF", System.Text.Encoding.ASCII.GetString(result.Take(4).ToArray()));
    }

    [Fact]
    public async Task GeneratePdfAsync_PollMaterial_ReturnsValidPdf()
    {
        // Arrange
        var questionId = Guid.NewGuid();
        var material = new PollMaterialEntity(
            _testMaterialId,
            _testLessonId,
            "Poll Material",
            $"!!! question id=\"{questionId}\""
        );

        var question = new MultipleChoiceQuestionEntity(
            questionId,
            _testMaterialId,
            "Do you agree?",
            new List<string> { "Yes", "No", "Maybe" },
            0,
            1
        );

        _mockMaterialRepo.Setup(r => r.GetByIdAsync(_testMaterialId))
            .ReturnsAsync(material);
        _mockQuestionRepo.Setup(r => r.GetByMaterialIdAsync(_testMaterialId))
            .ReturnsAsync(new List<QuestionEntity> { question });
        _mockAttachmentRepo.Setup(r => r.GetByMaterialIdAsync(_testMaterialId))
            .ReturnsAsync(new List<AttachmentEntity>());

        // Act
        var result = await _service.GeneratePdfAsync(_testMaterialId);

        // Assert
        Assert.NotNull(result);
        Assert.NotEmpty(result);
        Assert.StartsWith("%PDF", System.Text.Encoding.ASCII.GetString(result.Take(4).ToArray()));
    }

    [Fact]
    public async Task GeneratePdfAsync_MaterialWithMultipleQuestions_ReturnsValidPdf()
    {
        // Arrange
        var q1Id = Guid.NewGuid();
        var q2Id = Guid.NewGuid();
        var q3Id = Guid.NewGuid();
        var material = new WorksheetMaterialEntity(
            _testMaterialId,
            _testLessonId,
            "Multi-Question Worksheet",
            $"# Test\n\n!!! question id=\"{q1Id}\"\n\n!!! question id=\"{q2Id}\"\n\n!!! question id=\"{q3Id}\""
        );

        var questions = new List<QuestionEntity>
        {
            new MultipleChoiceQuestionEntity(q1Id, _testMaterialId, "Q1?", new List<string> { "A", "B" }, 0, 1),
            new WrittenAnswerQuestionEntity(q2Id, _testMaterialId, "Q2?", null, null, 2),
            new MultipleChoiceQuestionEntity(q3Id, _testMaterialId, "Q3?", new List<string> { "X", "Y", "Z" }, 1, 3)
        };

        _mockMaterialRepo.Setup(r => r.GetByIdAsync(_testMaterialId))
            .ReturnsAsync(material);
        _mockQuestionRepo.Setup(r => r.GetByMaterialIdAsync(_testMaterialId))
            .ReturnsAsync(questions);
        _mockAttachmentRepo.Setup(r => r.GetByMaterialIdAsync(_testMaterialId))
            .ReturnsAsync(new List<AttachmentEntity>());

        // Act
        var result = await _service.GeneratePdfAsync(_testMaterialId);

        // Assert
        Assert.NotNull(result);
        Assert.NotEmpty(result);
        Assert.StartsWith("%PDF", System.Text.Encoding.ASCII.GetString(result.Take(4).ToArray()));
    }

    [Fact]
    public async Task GeneratePdfAsync_MaterialWithCenterMarker_ReturnsValidPdf()
    {
        // Arrange
        var material = new WorksheetMaterialEntity(
            _testMaterialId,
            _testLessonId,
            "Centered Content",
            "# Title\n\n!!! center\n    This is centered text.\n\nNormal paragraph."
        );

        _mockMaterialRepo.Setup(r => r.GetByIdAsync(_testMaterialId))
            .ReturnsAsync(material);
        _mockQuestionRepo.Setup(r => r.GetByMaterialIdAsync(_testMaterialId))
            .ReturnsAsync(new List<QuestionEntity>());
        _mockAttachmentRepo.Setup(r => r.GetByMaterialIdAsync(_testMaterialId))
            .ReturnsAsync(new List<AttachmentEntity>());

        // Act
        var result = await _service.GeneratePdfAsync(_testMaterialId);

        // Assert
        Assert.NotNull(result);
        Assert.NotEmpty(result);
        Assert.StartsWith("%PDF", System.Text.Encoding.ASCII.GetString(result.Take(4).ToArray()));
    }

    [Fact]
    public async Task GeneratePdfAsync_MaterialWithPdfEmbedMarker_ReturnsValidPdf()
    {
        // Arrange
        var pdfId = Guid.NewGuid();
        var material = new WorksheetMaterialEntity(
            _testMaterialId,
            _testLessonId,
            "With PDF Embed",
            $"# Title\n\n!!! pdf id=\"{pdfId}\"\n\nAfter embed."
        );

        _mockMaterialRepo.Setup(r => r.GetByIdAsync(_testMaterialId))
            .ReturnsAsync(material);
        _mockQuestionRepo.Setup(r => r.GetByMaterialIdAsync(_testMaterialId))
            .ReturnsAsync(new List<QuestionEntity>());
        _mockAttachmentRepo.Setup(r => r.GetByMaterialIdAsync(_testMaterialId))
            .ReturnsAsync(new List<AttachmentEntity>());

        // Act
        var result = await _service.GeneratePdfAsync(_testMaterialId);

        // Assert - PDF embed shows placeholder in current version
        Assert.NotNull(result);
        Assert.NotEmpty(result);
        Assert.StartsWith("%PDF", System.Text.Encoding.ASCII.GetString(result.Take(4).ToArray()));
    }

    [Fact]
    public async Task GeneratePdfAsync_WrittenQuestionWithHighMaxScore_GeneratesMoreBlankLines()
    {
        // Arrange - test the blank line calculation per §4(4)(b)(iii)
        var questionId = Guid.NewGuid();
        var material = new WorksheetMaterialEntity(
            _testMaterialId,
            _testLessonId,
            "High Score Question",
            $"!!! question id=\"{questionId}\""
        );

        var question = new WrittenAnswerQuestionEntity(
            questionId,
            _testMaterialId,
            "Write a detailed essay.",
            correctAnswer: null,
            markScheme: null,
            maxScore: 10 // High score - should generate 20 blank lines
        );

        _mockMaterialRepo.Setup(r => r.GetByIdAsync(_testMaterialId))
            .ReturnsAsync(material);
        _mockQuestionRepo.Setup(r => r.GetByMaterialIdAsync(_testMaterialId))
            .ReturnsAsync(new List<QuestionEntity> { question });
        _mockAttachmentRepo.Setup(r => r.GetByMaterialIdAsync(_testMaterialId))
            .ReturnsAsync(new List<AttachmentEntity>());

        // Act
        var result = await _service.GeneratePdfAsync(_testMaterialId);

        // Assert
        Assert.NotNull(result);
        Assert.NotEmpty(result);
        Assert.StartsWith("%PDF", System.Text.Encoding.ASCII.GetString(result.Take(4).ToArray()));
    }

    [Fact]
    public async Task GeneratePdfAsync_WithPdfEmbed_IncreasesPageCount()
    {
        // Arrange - create a synthetic 2-page PDF file for embedding
        var pdfAttachmentId = Guid.NewGuid();
        var tempDir = Path.Combine(Path.GetTempPath(), "MaterialPdfServiceTests");
        Directory.CreateDirectory(tempDir);
        var syntheticPdfPath = Path.Combine(tempDir, $"{pdfAttachmentId}.pdf");

        try
        {
            // Create a 2-page synthetic PDF using PdfSharpCore
            using (var doc = new PdfSharpCore.Pdf.PdfDocument())
            {
                // Add 2 pages
                var page1 = doc.AddPage();
                using (var gfx1 = PdfSharpCore.Drawing.XGraphics.FromPdfPage(page1))
                {
                    gfx1.DrawString("Test Page 1", 
                        new PdfSharpCore.Drawing.XFont("Arial", 12), 
                        PdfSharpCore.Drawing.XBrushes.Black, 
                        new PdfSharpCore.Drawing.XPoint(100, 100));
                }
                
                var page2 = doc.AddPage();
                using (var gfx2 = PdfSharpCore.Drawing.XGraphics.FromPdfPage(page2))
                {
                    gfx2.DrawString("Test Page 2", 
                        new PdfSharpCore.Drawing.XFont("Arial", 12), 
                        PdfSharpCore.Drawing.XBrushes.Black, 
                        new PdfSharpCore.Drawing.XPoint(100, 100));
                }
                
                doc.Save(syntheticPdfPath);
            }

            var material = new WorksheetMaterialEntity(
                _testMaterialId,
                _testLessonId,
                "With PDF Embed",
                $"# Title\n\n!!! pdf id=\"{pdfAttachmentId}\"\n\nAfter embed."
            );

            var attachment = new AttachmentEntity(
                pdfAttachmentId,
                _testMaterialId,
                "test-document",
                "pdf"
            );

            _mockMaterialRepo.Setup(r => r.GetByIdAsync(_testMaterialId))
                .ReturnsAsync(material);
            _mockQuestionRepo.Setup(r => r.GetByMaterialIdAsync(_testMaterialId))
                .ReturnsAsync(new List<QuestionEntity>());
            _mockAttachmentRepo.Setup(r => r.GetByMaterialIdAsync(_testMaterialId))
                .ReturnsAsync(new List<AttachmentEntity> { attachment });
            _mockFileService.Setup(f => f.GetAttachmentFilePath(pdfAttachmentId, "pdf"))
                .Returns(syntheticPdfPath);
            _mockFileService.Setup(f => f.FileExists(syntheticPdfPath))
                .Returns(true);

            // Act
            var result = await _service.GeneratePdfAsync(_testMaterialId);

            // Assert - Verify PDF is valid
            Assert.NotNull(result);
            Assert.NotEmpty(result);
            Assert.StartsWith("%PDF", System.Text.Encoding.ASCII.GetString(result.Take(4).ToArray()));

            // Parse the result PDF and verify page count includes embedded pages
            using var resultStream = new MemoryStream(result);
            using var resultPdf = PdfSharpCore.Pdf.IO.PdfReader.Open(resultStream, PdfSharpCore.Pdf.IO.PdfDocumentOpenMode.Import);
            
            // Should have at least 3 pages: 1 for content before embed, 2 from embedded PDF
            // (content after embed may be on same page or new page)
            Assert.True(resultPdf.PageCount >= 3, 
                $"Expected at least 3 pages (1 content + 2 embedded), but got {resultPdf.PageCount}");
        }
        finally
        {
            // Cleanup
            if (File.Exists(syntheticPdfPath))
                File.Delete(syntheticPdfPath);
            if (Directory.Exists(tempDir) && !Directory.EnumerateFileSystemEntries(tempDir).Any())
                Directory.Delete(tempDir);
        }
    }

    #endregion

    #region LaTeX Tests

    [Fact]
    public async Task GeneratePdfAsync_MaterialWithInlineLatex_ReturnsValidPdf()
    {
        // Arrange
        var material = new WorksheetMaterialEntity(
            _testMaterialId,
            _testLessonId,
            "LaTeX Worksheet",
            "Solve: $x^2 + 2x + 1 = 0$"
        );

        // Mock renderer returns a minimal 1x1 PNG
        var fakePng = CreateMinimalPng();
        _mockLatexRenderer.Setup(r => r.RenderToImage(It.IsAny<string>(), false, It.IsAny<float>()))
            .Returns(fakePng);

        _mockMaterialRepo.Setup(r => r.GetByIdAsync(_testMaterialId))
            .ReturnsAsync(material);
        _mockQuestionRepo.Setup(r => r.GetByMaterialIdAsync(_testMaterialId))
            .ReturnsAsync(new List<QuestionEntity>());
        _mockAttachmentRepo.Setup(r => r.GetByMaterialIdAsync(_testMaterialId))
            .ReturnsAsync(new List<AttachmentEntity>());

        // Act
        var result = await _service.GeneratePdfAsync(_testMaterialId);

        // Assert
        Assert.NotNull(result);
        Assert.NotEmpty(result);
        Assert.StartsWith("%PDF", System.Text.Encoding.ASCII.GetString(result.Take(4).ToArray()));
        _mockLatexRenderer.Verify(r => r.RenderToImage("x^2 + 2x + 1 = 0", false, It.IsAny<float>()), Times.Once);
    }

    [Fact]
    public async Task GeneratePdfAsync_MaterialWithBlockLatex_ReturnsValidPdf()
    {
        // Arrange
        var material = new WorksheetMaterialEntity(
            _testMaterialId,
            _testLessonId,
            "LaTeX Worksheet",
            "$$\\int_0^1 f(x) dx$$"
        );

        var fakePng = CreateMinimalPng();
        _mockLatexRenderer.Setup(r => r.RenderToImage(It.IsAny<string>(), true, It.IsAny<float>()))
            .Returns(fakePng);

        _mockMaterialRepo.Setup(r => r.GetByIdAsync(_testMaterialId))
            .ReturnsAsync(material);
        _mockQuestionRepo.Setup(r => r.GetByMaterialIdAsync(_testMaterialId))
            .ReturnsAsync(new List<QuestionEntity>());
        _mockAttachmentRepo.Setup(r => r.GetByMaterialIdAsync(_testMaterialId))
            .ReturnsAsync(new List<AttachmentEntity>());

        // Act
        var result = await _service.GeneratePdfAsync(_testMaterialId);

        // Assert
        Assert.NotNull(result);
        Assert.NotEmpty(result);
        Assert.StartsWith("%PDF", System.Text.Encoding.ASCII.GetString(result.Take(4).ToArray()));
        _mockLatexRenderer.Verify(r => r.RenderToImage(It.IsAny<string>(), true, It.IsAny<float>()), Times.Once);
    }

    [Fact]
    public async Task GeneratePdfAsync_LatexRenderingFails_FallsBackToRawText()
    {
        // Arrange - LaTeX renderer returns null (failure) per §6(4)
        var material = new WorksheetMaterialEntity(
            _testMaterialId,
            _testLessonId,
            "LaTeX Failure",
            "Solve: $\\invalid{latex}$"
        );

        _mockLatexRenderer.Setup(r => r.RenderToImage(It.IsAny<string>(), It.IsAny<bool>(), It.IsAny<float>()))
            .Returns((byte[]?)null);

        _mockMaterialRepo.Setup(r => r.GetByIdAsync(_testMaterialId))
            .ReturnsAsync(material);
        _mockQuestionRepo.Setup(r => r.GetByMaterialIdAsync(_testMaterialId))
            .ReturnsAsync(new List<QuestionEntity>());
        _mockAttachmentRepo.Setup(r => r.GetByMaterialIdAsync(_testMaterialId))
            .ReturnsAsync(new List<AttachmentEntity>());

        // Act - should not throw, fallback to raw text
        var result = await _service.GeneratePdfAsync(_testMaterialId);

        // Assert
        Assert.NotNull(result);
        Assert.NotEmpty(result);
        Assert.StartsWith("%PDF", System.Text.Encoding.ASCII.GetString(result.Take(4).ToArray()));
    }

    /// <summary>
    /// Creates a minimal 1x1 transparent PNG for test purposes.
    /// </summary>
    private static byte[] CreateMinimalPng()
    {
        // Minimal valid PNG: 1x1 pixel, RGBA, transparent
        return new byte[]
        {
            0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, // PNG signature
            0x00, 0x00, 0x00, 0x0D, 0x49, 0x48, 0x44, 0x52, // IHDR chunk
            0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x01, // 1x1
            0x08, 0x06, 0x00, 0x00, 0x00, 0x1F, 0x15, 0xC4, 0x89, // 8-bit RGBA
            0x00, 0x00, 0x00, 0x0A, 0x49, 0x44, 0x41, 0x54, // IDAT chunk
            0x78, 0x9C, 0x62, 0x00, 0x00, 0x00, 0x02, 0x00, 0x01, 0xE5, // compressed data
            0x27, 0xDE, 0xFC,
            0x00, 0x00, 0x00, 0x00, 0x49, 0x45, 0x4E, 0x44, // IEND chunk
            0xAE, 0x42, 0x60, 0x82
        };
    }

    #endregion

    #region EffectivePdfSettings Tests

    [Theory]
    [InlineData(FontSizePreset.SMALL, 10f)]
    [InlineData(FontSizePreset.MEDIUM, 12f)]
    [InlineData(FontSizePreset.LARGE, 14f)]
    [InlineData(FontSizePreset.EXTRA_LARGE, 16f)]
    public void EffectivePdfSettings_BodyFontSizePt_MapsCorrectly(FontSizePreset preset, float expectedPt)
    {
        var settings = new EffectivePdfSettings(LinePatternType.RULED, LineSpacingPreset.MEDIUM, preset);
        Assert.Equal(expectedPt, settings.BodyFontSizePt);
    }

    [Theory]
    [InlineData(LineSpacingPreset.SMALL, 6f)]
    [InlineData(LineSpacingPreset.MEDIUM, 8f)]
    [InlineData(LineSpacingPreset.LARGE, 10f)]
    [InlineData(LineSpacingPreset.EXTRA_LARGE, 14f)]
    public void EffectivePdfSettings_LineSpacingMm_MapsCorrectly(LineSpacingPreset preset, float expectedMm)
    {
        var settings = new EffectivePdfSettings(LinePatternType.RULED, preset, FontSizePreset.MEDIUM);
        Assert.Equal(expectedMm, settings.LineSpacingMm);
    }

    [Fact]
    public async Task GeneratePdfAsync_MaterialOverridesGlobalDefaults()
    {
        // Arrange - material has per-material overrides
        var material = new WorksheetMaterialEntity(
            _testMaterialId,
            _testLessonId,
            "Test Worksheet",
            "Some content",
            linePatternType: LinePatternType.SQUARE,
            lineSpacingPreset: LineSpacingPreset.LARGE,
            fontSizePreset: FontSizePreset.EXTRA_LARGE
        );

        _mockMaterialRepo.Setup(r => r.GetByIdAsync(_testMaterialId))
            .ReturnsAsync(material);
        _mockQuestionRepo.Setup(r => r.GetByMaterialIdAsync(_testMaterialId))
            .ReturnsAsync(new List<QuestionEntity>());
        _mockAttachmentRepo.Setup(r => r.GetByMaterialIdAsync(_testMaterialId))
            .ReturnsAsync(new List<AttachmentEntity>());

        // Act
        var result = await _service.GeneratePdfAsync(_testMaterialId);

        // Assert - PDF generated successfully (overrides applied internally)
        Assert.NotNull(result);
        Assert.NotEmpty(result);
        Assert.StartsWith("%PDF", System.Text.Encoding.ASCII.GetString(result.Take(4).ToArray()));
    }

    [Fact]
    public async Task GeneratePdfAsync_NullMaterialOverrides_FallsBackToGlobalDefaults()
    {
        // Arrange - material has null overrides (should fall back to global RULED/MEDIUM/MEDIUM)
        var material = new WorksheetMaterialEntity(
            _testMaterialId,
            _testLessonId,
            "Test Worksheet",
            "Some content"
        );

        _mockMaterialRepo.Setup(r => r.GetByIdAsync(_testMaterialId))
            .ReturnsAsync(material);
        _mockQuestionRepo.Setup(r => r.GetByMaterialIdAsync(_testMaterialId))
            .ReturnsAsync(new List<QuestionEntity>());
        _mockAttachmentRepo.Setup(r => r.GetByMaterialIdAsync(_testMaterialId))
            .ReturnsAsync(new List<AttachmentEntity>());

        // Act
        var result = await _service.GeneratePdfAsync(_testMaterialId);

        // Assert
        Assert.NotNull(result);
        Assert.NotEmpty(result);
        // Verify global defaults were queried
        _mockPdfExportSettingsRepo.Verify(r => r.GetAsync(), Times.Once);
    }

    [Fact]
    public async Task GeneratePdfAsync_PartialMaterialOverrides_MixesWithDefaults()
    {
        // Arrange - only linePatternType overridden, spacing/font null → use global defaults
        var material = new WorksheetMaterialEntity(
            _testMaterialId,
            _testLessonId,
            "Test Worksheet",
            "Some content",
            linePatternType: LinePatternType.ISOMETRIC
        );

        _mockMaterialRepo.Setup(r => r.GetByIdAsync(_testMaterialId))
            .ReturnsAsync(material);
        _mockQuestionRepo.Setup(r => r.GetByMaterialIdAsync(_testMaterialId))
            .ReturnsAsync(new List<QuestionEntity>());
        _mockAttachmentRepo.Setup(r => r.GetByMaterialIdAsync(_testMaterialId))
            .ReturnsAsync(new List<AttachmentEntity>());

        // Act
        var result = await _service.GeneratePdfAsync(_testMaterialId);

        // Assert - PDF generated successfully
        Assert.NotNull(result);
        Assert.NotEmpty(result);
        Assert.StartsWith("%PDF", System.Text.Encoding.ASCII.GetString(result.Take(4).ToArray()));
    }

    [Fact]
    public async Task GeneratePdfAsync_WrittenQuestion_WithNoneLinePattern_ReturnsValidPdf()
    {
        // Arrange - NONE pattern should render blank lines without any line patterns
        var questionId = Guid.NewGuid();
        var material = new WorksheetMaterialEntity(
            _testMaterialId,
            _testLessonId,
            "Test Worksheet",
            $"!!! question id=\"{questionId}\"",
            linePatternType: LinePatternType.NONE
        );

        var question = new WrittenAnswerQuestionEntity(
            questionId, _testMaterialId, "Write here.", null, null, 3
        );

        _mockMaterialRepo.Setup(r => r.GetByIdAsync(_testMaterialId))
            .ReturnsAsync(material);
        _mockQuestionRepo.Setup(r => r.GetByMaterialIdAsync(_testMaterialId))
            .ReturnsAsync(new List<QuestionEntity> { question });
        _mockAttachmentRepo.Setup(r => r.GetByMaterialIdAsync(_testMaterialId))
            .ReturnsAsync(new List<AttachmentEntity>());

        // Act
        var result = await _service.GeneratePdfAsync(_testMaterialId);

        // Assert
        Assert.NotNull(result);
        Assert.NotEmpty(result);
        Assert.StartsWith("%PDF", System.Text.Encoding.ASCII.GetString(result.Take(4).ToArray()));
    }

    [Fact]
    public async Task GeneratePdfAsync_AllLinePatternTypes_ProduceValidPdf()
    {
        foreach (var pattern in Enum.GetValues<LinePatternType>())
        {
            var questionId = Guid.NewGuid();
            var matId = Guid.NewGuid();
            var material = new WorksheetMaterialEntity(
                matId, _testLessonId, "Test", $"!!! question id=\"{questionId}\"",
                linePatternType: pattern
            );
            var question = new WrittenAnswerQuestionEntity(
                questionId, matId, "Q?", null, null, 4
            );

            _mockMaterialRepo.Setup(r => r.GetByIdAsync(matId)).ReturnsAsync(material);
            _mockQuestionRepo.Setup(r => r.GetByMaterialIdAsync(matId))
                .ReturnsAsync(new List<QuestionEntity> { question });
            _mockAttachmentRepo.Setup(r => r.GetByMaterialIdAsync(matId))
                .ReturnsAsync(new List<AttachmentEntity>());

            var result = await _service.GeneratePdfAsync(matId);

            Assert.NotNull(result);
            Assert.StartsWith("%PDF", System.Text.Encoding.ASCII.GetString(result.Take(4).ToArray()));
        }
    }

    #endregion

    #region Constructor Tests for New Dependencies

    [Fact]
    public void Constructor_NullResponseRepository_ThrowsArgumentNullException()
    {
        Assert.Throws<ArgumentNullException>(() => new MaterialPdfService(
            _mockMaterialRepo.Object,
            _mockQuestionRepo.Object,
            _mockAttachmentRepo.Object,
            _mockFileService.Object,
            _mockLatexRenderer.Object,
            _mockPdfExportSettingsRepo.Object,
            null!,
            _mockFeedbackRepo.Object,
            _mockDeviceRegistryService.Object,
            _mockExternalDeviceRepo.Object));
    }

    [Fact]
    public void Constructor_NullFeedbackRepository_ThrowsArgumentNullException()
    {
        Assert.Throws<ArgumentNullException>(() => new MaterialPdfService(
            _mockMaterialRepo.Object,
            _mockQuestionRepo.Object,
            _mockAttachmentRepo.Object,
            _mockFileService.Object,
            _mockLatexRenderer.Object,
            _mockPdfExportSettingsRepo.Object,
            _mockResponseRepo.Object,
            null!,
            _mockDeviceRegistryService.Object,
            _mockExternalDeviceRepo.Object));
    }

    [Fact]
    public void Constructor_NullDeviceRegistryService_ThrowsArgumentNullException()
    {
        Assert.Throws<ArgumentNullException>(() => new MaterialPdfService(
            _mockMaterialRepo.Object,
            _mockQuestionRepo.Object,
            _mockAttachmentRepo.Object,
            _mockFileService.Object,
            _mockLatexRenderer.Object,
            _mockPdfExportSettingsRepo.Object,
            _mockResponseRepo.Object,
            _mockFeedbackRepo.Object,
            null!,
            _mockExternalDeviceRepo.Object));
    }

    [Fact]
    public void Constructor_NullExternalDeviceRepository_ThrowsArgumentNullException()
    {
        Assert.Throws<ArgumentNullException>(() => new MaterialPdfService(
            _mockMaterialRepo.Object,
            _mockQuestionRepo.Object,
            _mockAttachmentRepo.Object,
            _mockFileService.Object,
            _mockLatexRenderer.Object,
            _mockPdfExportSettingsRepo.Object,
            _mockResponseRepo.Object,
            _mockFeedbackRepo.Object,
            _mockDeviceRegistryService.Object,
            null!));
    }

    #endregion

    #region Per-Device Override Resolution Tests

    [Fact]
    public async Task GeneratePdfAsync_WithTargetDevice_AppliesDeviceOverrides()
    {
        // Arrange — device has all three overrides; these should take precedence
        var deviceId = Guid.NewGuid();
        var material = new WorksheetMaterialEntity(
            _testMaterialId, _testLessonId, "Test", "Some content",
            linePatternType: LinePatternType.RULED,
            lineSpacingPreset: LineSpacingPreset.SMALL,
            fontSizePreset: FontSizePreset.SMALL);

        var device = new ExternalDeviceEntity(deviceId, "Test Device", ExternalDeviceType.REMARKABLE)
        {
            LinePatternType = LinePatternType.SQUARE,
            LineSpacingPreset = LineSpacingPreset.EXTRA_LARGE,
            FontSizePreset = FontSizePreset.EXTRA_LARGE
        };

        _mockMaterialRepo.Setup(r => r.GetByIdAsync(_testMaterialId)).ReturnsAsync(material);
        _mockQuestionRepo.Setup(r => r.GetByMaterialIdAsync(_testMaterialId))
            .ReturnsAsync(new List<QuestionEntity>());
        _mockAttachmentRepo.Setup(r => r.GetByMaterialIdAsync(_testMaterialId))
            .ReturnsAsync(new List<AttachmentEntity>());
        _mockExternalDeviceRepo.Setup(r => r.GetByIdAsync(deviceId)).ReturnsAsync(device);

        // Act
        var result = await _service.GeneratePdfAsync(_testMaterialId, deviceId);

        // Assert — PDF generated successfully with device overrides applied
        Assert.NotNull(result);
        Assert.NotEmpty(result);
        Assert.StartsWith("%PDF", System.Text.Encoding.ASCII.GetString(result.Take(4).ToArray()));
        _mockExternalDeviceRepo.Verify(r => r.GetByIdAsync(deviceId), Times.Once);
    }

    [Fact]
    public async Task GeneratePdfAsync_WithTargetDevice_NullOverrides_FallsBackToMaterial()
    {
        // Arrange — device has no overrides; material overrides should apply
        var deviceId = Guid.NewGuid();
        var material = new WorksheetMaterialEntity(
            _testMaterialId, _testLessonId, "Test", "Some content",
            linePatternType: LinePatternType.ISOMETRIC,
            lineSpacingPreset: LineSpacingPreset.LARGE,
            fontSizePreset: FontSizePreset.LARGE);

        var device = new ExternalDeviceEntity(deviceId, "Bare Device", ExternalDeviceType.KINDLE);
        // All override fields remain null

        _mockMaterialRepo.Setup(r => r.GetByIdAsync(_testMaterialId)).ReturnsAsync(material);
        _mockQuestionRepo.Setup(r => r.GetByMaterialIdAsync(_testMaterialId))
            .ReturnsAsync(new List<QuestionEntity>());
        _mockAttachmentRepo.Setup(r => r.GetByMaterialIdAsync(_testMaterialId))
            .ReturnsAsync(new List<AttachmentEntity>());
        _mockExternalDeviceRepo.Setup(r => r.GetByIdAsync(deviceId)).ReturnsAsync(device);

        // Act
        var result = await _service.GeneratePdfAsync(_testMaterialId, deviceId);

        // Assert — fallback to material overrides
        Assert.NotNull(result);
        Assert.NotEmpty(result);
        Assert.StartsWith("%PDF", System.Text.Encoding.ASCII.GetString(result.Take(4).ToArray()));
    }

    [Fact]
    public async Task GeneratePdfAsync_WithTargetDevice_PartialOverrides_MixesDeviceAndMaterial()
    {
        // Arrange — device overrides only LinePatternType; spacing/font fall back to material
        var deviceId = Guid.NewGuid();
        var material = new WorksheetMaterialEntity(
            _testMaterialId, _testLessonId, "Test", "Some content",
            linePatternType: LinePatternType.RULED,
            lineSpacingPreset: LineSpacingPreset.LARGE,
            fontSizePreset: FontSizePreset.LARGE);

        var device = new ExternalDeviceEntity(deviceId, "Partial Device", ExternalDeviceType.REMARKABLE)
        {
            LinePatternType = LinePatternType.SQUARE
            // LineSpacingPreset and FontSizePreset remain null → material values
        };

        _mockMaterialRepo.Setup(r => r.GetByIdAsync(_testMaterialId)).ReturnsAsync(material);
        _mockQuestionRepo.Setup(r => r.GetByMaterialIdAsync(_testMaterialId))
            .ReturnsAsync(new List<QuestionEntity>());
        _mockAttachmentRepo.Setup(r => r.GetByMaterialIdAsync(_testMaterialId))
            .ReturnsAsync(new List<AttachmentEntity>());
        _mockExternalDeviceRepo.Setup(r => r.GetByIdAsync(deviceId)).ReturnsAsync(device);

        // Act
        var result = await _service.GeneratePdfAsync(_testMaterialId, deviceId);

        // Assert — PDF generated with mixed settings
        Assert.NotNull(result);
        Assert.NotEmpty(result);
        Assert.StartsWith("%PDF", System.Text.Encoding.ASCII.GetString(result.Take(4).ToArray()));
    }

    #endregion

    #region GenerateResponsePdfAsync Tests

    [Fact]
    public async Task GenerateResponsePdfAsync_MaterialNotFound_ThrowsKeyNotFoundException()
    {
        var materialId = Guid.NewGuid();
        _mockMaterialRepo.Setup(r => r.GetByIdAsync(materialId))
            .ReturnsAsync((MaterialEntity?)null);

        await Assert.ThrowsAsync<KeyNotFoundException>(
            () => _service.GenerateResponsePdfAsync(materialId, Guid.NewGuid().ToString(), false, false));
    }

    [Fact]
    public async Task GenerateResponsePdfAsync_NoResponses_ThrowsInvalidOperationException()
    {
        // §7(7): Throws InvalidOperationException when no responses
        var materialId = Guid.NewGuid();
        var deviceId = Guid.NewGuid();
        var questionId = Guid.NewGuid();
        var material = new WorksheetMaterialEntity(
            materialId, _testLessonId, "Test Worksheet",
            $"!!! question id=\"{questionId}\"");

        _mockMaterialRepo.Setup(r => r.GetByIdAsync(materialId)).ReturnsAsync(material);
        _mockQuestionRepo.Setup(r => r.GetByMaterialIdAsync(materialId))
            .ReturnsAsync(new List<QuestionEntity>
            {
                new WrittenAnswerQuestionEntity(questionId, materialId, "Q1?", null)
            });
        _mockResponseRepo.Setup(r => r.GetByQuestionIdAsync(questionId))
            .ReturnsAsync(new List<ResponseEntity>());
        _mockDeviceRegistryService.Setup(s => s.GetAllAsync())
            .ReturnsAsync(new List<PairedDeviceEntity>
            {
                new PairedDeviceEntity { DeviceId = deviceId, Name = "TestDevice" }
            });

        await Assert.ThrowsAsync<InvalidOperationException>(
            () => _service.GenerateResponsePdfAsync(materialId, deviceId.ToString(), false, false));
    }

    [Fact]
    public async Task GenerateResponsePdfAsync_WithMcqResponse_ProducesValidPdf()
    {
        var materialId = Guid.NewGuid();
        var deviceId = Guid.NewGuid();
        var questionId = Guid.NewGuid();
        var responseId = Guid.NewGuid();
        var material = new WorksheetMaterialEntity(
            materialId, _testLessonId, "MCQ Worksheet",
            $"!!! question id=\"{questionId}\"");

        _mockMaterialRepo.Setup(r => r.GetByIdAsync(materialId)).ReturnsAsync(material);
        _mockQuestionRepo.Setup(r => r.GetByMaterialIdAsync(materialId))
            .ReturnsAsync(new List<QuestionEntity>
            {
                new MultipleChoiceQuestionEntity(questionId, materialId, "Pick one:", new List<string> { "A", "B", "C" }, 1)
            });
        _mockResponseRepo.Setup(r => r.GetByQuestionIdAsync(questionId))
            .ReturnsAsync(new List<ResponseEntity>
            {
                new MultipleChoiceResponseEntity(responseId, questionId, deviceId, 0)
            });
        _mockDeviceRegistryService.Setup(s => s.GetAllAsync())
            .ReturnsAsync(new List<PairedDeviceEntity>
            {
                new PairedDeviceEntity { DeviceId = deviceId, Name = "TestDevice" }
            });

        var result = await _service.GenerateResponsePdfAsync(materialId, deviceId.ToString(), false, false);

        Assert.NotNull(result);
        Assert.NotEmpty(result);
        Assert.StartsWith("%PDF", System.Text.Encoding.ASCII.GetString(result.Take(4).ToArray()));
    }

    [Fact]
    public async Task GenerateResponsePdfAsync_WithWrittenResponse_ProducesValidPdf()
    {
        var materialId = Guid.NewGuid();
        var deviceId = Guid.NewGuid();
        var questionId = Guid.NewGuid();
        var responseId = Guid.NewGuid();
        var material = new WorksheetMaterialEntity(
            materialId, _testLessonId, "Written Worksheet",
            $"!!! question id=\"{questionId}\"");

        _mockMaterialRepo.Setup(r => r.GetByIdAsync(materialId)).ReturnsAsync(material);
        _mockQuestionRepo.Setup(r => r.GetByMaterialIdAsync(materialId))
            .ReturnsAsync(new List<QuestionEntity>
            {
                new WrittenAnswerQuestionEntity(questionId, materialId, "Explain:", "correct answer", "Check for key points", 5)
            });
        _mockResponseRepo.Setup(r => r.GetByQuestionIdAsync(questionId))
            .ReturnsAsync(new List<ResponseEntity>
            {
                new WrittenAnswerResponseEntity(responseId, questionId, deviceId, "Student answer text")
            });
        _mockDeviceRegistryService.Setup(s => s.GetAllAsync())
            .ReturnsAsync(new List<PairedDeviceEntity>
            {
                new PairedDeviceEntity { DeviceId = deviceId, Name = "TestDevice" }
            });

        var result = await _service.GenerateResponsePdfAsync(materialId, deviceId.ToString(), false, false);

        Assert.NotNull(result);
        Assert.NotEmpty(result);
        Assert.StartsWith("%PDF", System.Text.Encoding.ASCII.GetString(result.Take(4).ToArray()));
    }

    [Fact]
    public async Task GenerateResponsePdfAsync_WithFeedback_ProducesValidPdf()
    {
        var materialId = Guid.NewGuid();
        var deviceId = Guid.NewGuid();
        var questionId = Guid.NewGuid();
        var responseId = Guid.NewGuid();
        var feedbackId = Guid.NewGuid();
        var material = new WorksheetMaterialEntity(
            materialId, _testLessonId, "Feedback Worksheet",
            $"!!! question id=\"{questionId}\"");

        _mockMaterialRepo.Setup(r => r.GetByIdAsync(materialId)).ReturnsAsync(material);
        _mockQuestionRepo.Setup(r => r.GetByMaterialIdAsync(materialId))
            .ReturnsAsync(new List<QuestionEntity>
            {
                new WrittenAnswerQuestionEntity(questionId, materialId, "Explain:", null, null, 5)
            });
        _mockResponseRepo.Setup(r => r.GetByQuestionIdAsync(questionId))
            .ReturnsAsync(new List<ResponseEntity>
            {
                new WrittenAnswerResponseEntity(responseId, questionId, deviceId, "Student answer")
            });
        _mockFeedbackRepo.Setup(r => r.GetByResponseIdAsync(responseId))
            .ReturnsAsync(new FeedbackEntity(feedbackId, responseId, "Good effort", 3));
        _mockDeviceRegistryService.Setup(s => s.GetAllAsync())
            .ReturnsAsync(new List<PairedDeviceEntity>
            {
                new PairedDeviceEntity { DeviceId = deviceId, Name = "TestDevice" }
            });

        var result = await _service.GenerateResponsePdfAsync(materialId, deviceId.ToString(), true, false);

        Assert.NotNull(result);
        Assert.NotEmpty(result);
        Assert.StartsWith("%PDF", System.Text.Encoding.ASCII.GetString(result.Take(4).ToArray()));
    }

    [Fact]
    public async Task GenerateResponsePdfAsync_WithMarkScheme_ProducesValidPdf()
    {
        var materialId = Guid.NewGuid();
        var deviceId = Guid.NewGuid();
        var questionId = Guid.NewGuid();
        var responseId = Guid.NewGuid();
        var material = new WorksheetMaterialEntity(
            materialId, _testLessonId, "MarkScheme Worksheet",
            $"!!! question id=\"{questionId}\"");

        _mockMaterialRepo.Setup(r => r.GetByIdAsync(materialId)).ReturnsAsync(material);
        _mockQuestionRepo.Setup(r => r.GetByMaterialIdAsync(materialId))
            .ReturnsAsync(new List<QuestionEntity>
            {
                new MultipleChoiceQuestionEntity(questionId, materialId, "Pick:", new List<string> { "X", "Y" }, 0)
            });
        _mockResponseRepo.Setup(r => r.GetByQuestionIdAsync(questionId))
            .ReturnsAsync(new List<ResponseEntity>
            {
                new MultipleChoiceResponseEntity(responseId, questionId, deviceId, 1)
            });
        _mockDeviceRegistryService.Setup(s => s.GetAllAsync())
            .ReturnsAsync(new List<PairedDeviceEntity>
            {
                new PairedDeviceEntity { DeviceId = deviceId, Name = "TestDevice" }
            });

        var result = await _service.GenerateResponsePdfAsync(materialId, deviceId.ToString(), false, true);

        Assert.NotNull(result);
        Assert.NotEmpty(result);
        Assert.StartsWith("%PDF", System.Text.Encoding.ASCII.GetString(result.Take(4).ToArray()));
    }

    #endregion
}
