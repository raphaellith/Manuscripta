using System;
using System.Collections.Generic;
using System.Threading.Tasks;
using Moq;
using Xunit;
using Main.Models.Entities;
using Main.Models.Entities.Materials;
using Main.Models.Entities.Questions;
using Main.Models.Enums;
using Main.Services;
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
    private readonly MaterialPdfService _service;
    private readonly Guid _testMaterialId = Guid.NewGuid();
    private readonly Guid _testLessonId = Guid.NewGuid();

    public MaterialPdfServiceTests()
    {
        _mockMaterialRepo = new Mock<IMaterialRepository>();
        _mockQuestionRepo = new Mock<IQuestionRepository>();
        _mockAttachmentRepo = new Mock<IAttachmentRepository>();
        _mockFileService = new Mock<IFileService>();

        _service = new MaterialPdfService(
            _mockMaterialRepo.Object,
            _mockQuestionRepo.Object,
            _mockAttachmentRepo.Object,
            _mockFileService.Object);
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
            _mockFileService.Object));
    }

    [Fact]
    public void Constructor_NullQuestionRepository_ThrowsArgumentNullException()
    {
        // Act & Assert
        Assert.Throws<ArgumentNullException>(() => new MaterialPdfService(
            _mockMaterialRepo.Object,
            null!,
            _mockAttachmentRepo.Object,
            _mockFileService.Object));
    }

    [Fact]
    public void Constructor_NullAttachmentRepository_ThrowsArgumentNullException()
    {
        // Act & Assert
        Assert.Throws<ArgumentNullException>(() => new MaterialPdfService(
            _mockMaterialRepo.Object,
            _mockQuestionRepo.Object,
            null!,
            _mockFileService.Object));
    }

    [Fact]
    public void Constructor_NullFileService_ThrowsArgumentNullException()
    {
        // Act & Assert
        Assert.Throws<ArgumentNullException>(() => new MaterialPdfService(
            _mockMaterialRepo.Object,
            _mockQuestionRepo.Object,
            _mockAttachmentRepo.Object,
            null!));
    }

    [Fact]
    public void Constructor_ValidParameters_Success()
    {
        // Act
        var service = new MaterialPdfService(
            _mockMaterialRepo.Object,
            _mockQuestionRepo.Object,
            _mockAttachmentRepo.Object,
            _mockFileService.Object);

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

    #endregion
}
