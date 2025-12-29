using System;
using System.Collections.Generic;
using System.Linq;
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

public class MaterialServiceTests
{
    private readonly Mock<IMaterialRepository> _mockMaterialRepo;
    private readonly Mock<IQuestionRepository> _mockQuestionRepo;
    private readonly Mock<ILessonRepository> _mockLessonRepo;
    private readonly MaterialService _service;
    private readonly Guid _testLessonId = Guid.NewGuid();

    public MaterialServiceTests()
    {
        _mockMaterialRepo = new Mock<IMaterialRepository>();
        _mockQuestionRepo = new Mock<IQuestionRepository>();
        _mockLessonRepo = new Mock<ILessonRepository>();
        
        // Setup default lesson validation to pass
        _mockLessonRepo.Setup(r => r.GetByIdAsync(It.IsAny<Guid>()))
            .ReturnsAsync(new LessonEntity(Guid.NewGuid(), Guid.NewGuid(), "Test Lesson", "Description"));
        
        _service = new MaterialService(_mockMaterialRepo.Object, _mockQuestionRepo.Object, _mockLessonRepo.Object);
    }

    #region Material Tests

    [Fact]
    public async Task CreateMaterialAsync_ValidMaterial_Success()
    {
        // Arrange
        var material = new WorksheetMaterialEntity(
            Guid.NewGuid(),
            _testLessonId,
            "Test Worksheet",
            "Test Content"
        );

        _mockMaterialRepo.Setup(r => r.AddAsync(It.IsAny<MaterialEntity>()))
            .Returns(Task.CompletedTask);

        // Act
        var result = await _service.CreateMaterialAsync(material);

        // Assert
        Assert.NotNull(result);
        Assert.Equal(material.Id, result.Id);
        _mockMaterialRepo.Verify(r => r.AddAsync(material), Times.Once);
    }

    [Fact]
    public async Task CreateMaterialAsync_NullMaterial_ThrowsArgumentNullException()
    {
        // Act & Assert
        await Assert.ThrowsAsync<ArgumentNullException>(
            () => _service.CreateMaterialAsync(null!));
    }

    [Fact]
    public async Task CreateMaterialAsync_EmptyTitle_ThrowsArgumentException()
    {
        // Arrange
        var material = new QuizMaterialEntity(
            Guid.NewGuid(),
            _testLessonId,
            "",
            "Content"
        );

        // Act & Assert
        await Assert.ThrowsAsync<ArgumentException>(
            () => _service.CreateMaterialAsync(material));
    }

    [Fact]
    public async Task CreateMaterialAsync_EmptyContent_ThrowsArgumentException()
    {
        // Arrange
        var material = new PollMaterialEntity(
            Guid.NewGuid(),
            _testLessonId,
            "Title",
            ""
        );

        // Act & Assert
        await Assert.ThrowsAsync<ArgumentException>(
            () => _service.CreateMaterialAsync(material));
    }

    [Fact]
    public async Task CreateMaterialAsync_InvalidLessonId_ThrowsInvalidOperationException()
    {
        // Arrange
        var invalidLessonId = Guid.NewGuid();
        var material = new WorksheetMaterialEntity(
            Guid.NewGuid(),
            invalidLessonId,
            "Test Material",
            "Test Content"
        );

        _mockLessonRepo.Setup(r => r.GetByIdAsync(invalidLessonId))
            .ReturnsAsync((LessonEntity?)null);

        // Act & Assert
        await Assert.ThrowsAsync<InvalidOperationException>(
            () => _service.CreateMaterialAsync(material));
    }

    [Fact]
    public async Task UpdateMaterialAsync_ValidMaterial_Success()
    {
        // Arrange
        var material = new WorksheetMaterialEntity(
            Guid.NewGuid(),
            _testLessonId,
            "Updated Material",
            "Updated Content"
        );

        _mockMaterialRepo.Setup(r => r.GetByIdAsync(material.Id))
            .ReturnsAsync(material);
        _mockMaterialRepo.Setup(r => r.UpdateAsync(It.IsAny<MaterialEntity>()))
            .Returns(Task.CompletedTask);

        // Act
        var result = await _service.UpdateMaterialAsync(material);

        // Assert
        Assert.NotNull(result);
        Assert.Equal(material.Id, result.Id);
        _mockMaterialRepo.Verify(r => r.UpdateAsync(material), Times.Once);
    }

    [Fact]
    public async Task UpdateMaterialAsync_NullMaterial_ThrowsArgumentNullException()
    {
        // Act & Assert
        await Assert.ThrowsAsync<ArgumentNullException>(
            () => _service.UpdateMaterialAsync(null!));
    }

    [Fact]
    public async Task UpdateMaterialAsync_EmptyTitle_ThrowsArgumentException()
    {
        // Arrange
        var material = new QuizMaterialEntity(
            Guid.NewGuid(),
            _testLessonId,
            "",
            "Content"
        );

        // Act & Assert
        await Assert.ThrowsAsync<ArgumentException>(
            () => _service.UpdateMaterialAsync(material));
    }

    [Fact]
    public async Task UpdateMaterialAsync_EmptyContent_ThrowsArgumentException()
    {
        // Arrange
        var material = new PollMaterialEntity(
            Guid.NewGuid(),
            _testLessonId,
            "Title",
            ""
        );

        // Act & Assert
        await Assert.ThrowsAsync<ArgumentException>(
            () => _service.UpdateMaterialAsync(material));
    }

    [Fact]
    public async Task UpdateMaterialAsync_NonExistingMaterial_ThrowsInvalidOperationException()
    {
        // Arrange
        var material = new WorksheetMaterialEntity(
            Guid.NewGuid(),
            _testLessonId,
            "Material",
            "Content"
        );

        _mockMaterialRepo.Setup(r => r.GetByIdAsync(material.Id))
            .ReturnsAsync((MaterialEntity?)null);

        // Act & Assert
        await Assert.ThrowsAsync<InvalidOperationException>(
            () => _service.UpdateMaterialAsync(material));
    }

    [Fact]
    public async Task DeleteMaterialAsync_WithQuestions_DeletesMaterialAndQuestions()
    {
        // Arrange
        var materialId = Guid.NewGuid();
        var questions = new List<QuestionEntity>
        {
            new MultipleChoiceQuestionEntity(
                Guid.NewGuid(),
                materialId,
                "Question 1",
                new List<string> { "A", "B", "C" },
                0
            ),
            new TrueFalseQuestionEntity(
                Guid.NewGuid(),
                materialId,
                "Question 2",
                true
            )
        };

        _mockQuestionRepo.Setup(r => r.GetByMaterialIdAsync(materialId))
            .ReturnsAsync(questions);
        _mockQuestionRepo.Setup(r => r.DeleteAsync(It.IsAny<Guid>()))
            .Returns(Task.CompletedTask);
        _mockMaterialRepo.Setup(r => r.DeleteAsync(materialId))
            .Returns(Task.CompletedTask);

        // Act
        await _service.DeleteMaterialAsync(materialId);

        // Assert
        _mockQuestionRepo.Verify(r => r.DeleteAsync(It.IsAny<Guid>()), Times.Exactly(2));
        _mockMaterialRepo.Verify(r => r.DeleteAsync(materialId), Times.Once);
    }


    #endregion
}
