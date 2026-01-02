using System;
using System.Collections.Generic;
using System.Linq;
using System.Threading.Tasks;
using Moq;
using Xunit;
using Main.Models.Entities.Materials;
using Main.Models.Entities.Questions;
using Main.Models.Enums;
using Main.Services;
using Main.Services.Repositories;

namespace MainTests.ServicesTests;

public class QuestionServiceTests
{
    private readonly Mock<IQuestionRepository> _mockQuestionRepo;
    private readonly Mock<IMaterialRepository> _mockMaterialRepo;
    private readonly Mock<IResponseRepository> _mockResponseRepo;
    private readonly QuestionService _service;
    private readonly Guid _testLessonId = Guid.NewGuid();

    public QuestionServiceTests()
    {
        _mockQuestionRepo = new Mock<IQuestionRepository>();
        _mockMaterialRepo = new Mock<IMaterialRepository>();
        _mockResponseRepo = new Mock<IResponseRepository>();
        _service = new QuestionService(_mockQuestionRepo.Object, _mockMaterialRepo.Object, _mockResponseRepo.Object);
    }

    [Fact]
    public async Task CreateQuestionAsync_ValidQuestion_Success()
    {
        // Arrange
        var materialId = Guid.NewGuid();
        var material = new WorksheetMaterialEntity(
            materialId,
            _testLessonId,
            "Worksheet",
            "Content"
        );
        var question = new MultipleChoiceQuestionEntity(
            Guid.NewGuid(),
            materialId,
            "What is 2+2?",
            new List<string> { "3", "4", "5" },
            1
        );

        _mockMaterialRepo.Setup(r => r.GetByIdAsync(materialId))
            .ReturnsAsync(material);
        _mockQuestionRepo.Setup(r => r.AddAsync(It.IsAny<QuestionEntity>()))
            .Returns(Task.CompletedTask);

        // Act
        var result = await _service.CreateQuestionAsync(question);

        // Assert
        Assert.NotNull(result);
        Assert.Equal(question.Id, result.Id);
        _mockQuestionRepo.Verify(r => r.AddAsync(question), Times.Once);
    }

    [Fact]
    public async Task CreateQuestionAsync_NullQuestion_ThrowsArgumentNullException()
    {
        // Act & Assert
        await Assert.ThrowsAsync<ArgumentNullException>(
            () => _service.CreateQuestionAsync(null!));
    }

    [Fact]
    public async Task CreateQuestionAsync_EmptyQuestionText_ThrowsArgumentException()
    {
        // Arrange
        var materialId = Guid.NewGuid();
        var material = new WorksheetMaterialEntity(materialId, _testLessonId, "Material", "Content");
        var question = new MultipleChoiceQuestionEntity(
            Guid.NewGuid(),
            materialId,
            "   ",
            new List<string> { "True", "False" },
            0
        );

        _mockMaterialRepo.Setup(r => r.GetByIdAsync(materialId))
            .ReturnsAsync(material);

        // Act & Assert
        await Assert.ThrowsAsync<ArgumentException>(
            () => _service.CreateQuestionAsync(question));
    }

    [Fact]
    public async Task CreateQuestionAsync_NonExistingMaterial_ThrowsInvalidOperationException()
    {
        // Arrange
        var materialId = Guid.NewGuid();
        var question = new MultipleChoiceQuestionEntity(
            Guid.NewGuid(),
            materialId,
            "Question",
            new List<string> { "True", "False" },
            0
        );

        _mockMaterialRepo.Setup(r => r.GetByIdAsync(materialId))
            .ReturnsAsync((MaterialEntity?)null);

        // Act & Assert
        var exception = await Assert.ThrowsAsync<InvalidOperationException>(
            () => _service.CreateQuestionAsync(question));
        Assert.Contains("not found", exception.Message);
    }

    [Fact]
    public async Task CreateQuestionAsync_ReadingMaterial_ThrowsInvalidOperationException()
    {
        // Arrange - Rule 2B(3)(a): Questions cannot reference reading materials
        var materialId = Guid.NewGuid();
        var material = new ReadingMaterialEntity(
            materialId,
            _testLessonId,
            "Reading Material",
            "Content"
        );
        var question = new MultipleChoiceQuestionEntity(
            Guid.NewGuid(),
            materialId,
            "Question",
            new List<string> { "A", "B" },
            0
        );

        _mockMaterialRepo.Setup(r => r.GetByIdAsync(materialId))
            .ReturnsAsync(material);

        // Act & Assert
        var exception = await Assert.ThrowsAsync<InvalidOperationException>(
            () => _service.CreateQuestionAsync(question));
        Assert.Contains("reading material", exception.Message, StringComparison.OrdinalIgnoreCase);
    }

    [Fact]
    public async Task CreateQuestionAsync_WrittenQuestionInPoll_ThrowsInvalidOperationException()
    {
        // Arrange - Rule 2B(3)(b): Written questions cannot be in polls
        var materialId = Guid.NewGuid();
        var material = new PollMaterialEntity(
            materialId,
            _testLessonId,
            "Poll",
            "Content"
        );
        var question = new WrittenAnswerQuestionEntity(
            Guid.NewGuid(),
            materialId,
            "Written Question",
            "Answer"
        );

        _mockMaterialRepo.Setup(r => r.GetByIdAsync(materialId))
            .ReturnsAsync(material);

        // Act & Assert
        var exception = await Assert.ThrowsAsync<InvalidOperationException>(
            () => _service.CreateQuestionAsync(question));
        Assert.Contains("poll", exception.Message, StringComparison.OrdinalIgnoreCase);
    }

    [Fact]
    public async Task CreateQuestionAsync_WrittenQuestionInWorksheet_Success()
    {
        // Arrange - Written questions ARE allowed in worksheets
        var materialId = Guid.NewGuid();
        var material = new WorksheetMaterialEntity(
            materialId,
            _testLessonId,
            "Worksheet",
            "Content"
        );
        var question = new WrittenAnswerQuestionEntity(
            Guid.NewGuid(),
            materialId,
            "Written Question",
            "Answer"
        );

        _mockMaterialRepo.Setup(r => r.GetByIdAsync(materialId))
            .ReturnsAsync(material);
        _mockQuestionRepo.Setup(r => r.AddAsync(It.IsAny<QuestionEntity>()))
            .Returns(Task.CompletedTask);

        // Act
        var result = await _service.CreateQuestionAsync(question);

        // Assert
        Assert.NotNull(result);
        _mockQuestionRepo.Verify(r => r.AddAsync(question), Times.Once);
    }

    [Fact]
    public async Task CreateQuestionAsync_MultipleChoiceInPoll_Success()
    {
        // Arrange - Multiple choice questions ARE allowed in polls
        var materialId = Guid.NewGuid();
        var material = new PollMaterialEntity(
            materialId,
            _testLessonId,
            "Poll",
            "Content"
        );
        var question = new MultipleChoiceQuestionEntity(
            Guid.NewGuid(),
            materialId,
            "Poll Question",
            new List<string> { "Option 1", "Option 2" },
            0
        );

        _mockMaterialRepo.Setup(r => r.GetByIdAsync(materialId))
            .ReturnsAsync(material);
        _mockQuestionRepo.Setup(r => r.AddAsync(It.IsAny<QuestionEntity>()))
            .Returns(Task.CompletedTask);

        // Act
        var result = await _service.CreateQuestionAsync(question);

        // Assert
        Assert.NotNull(result);
        _mockQuestionRepo.Verify(r => r.AddAsync(question), Times.Once);
    }

    [Fact]
    public async Task CreateQuestionAsync_MultipleChoiceInQuiz_Success()
    {
        // Arrange - Multiple choice questions ARE allowed in quizzes
        var materialId = Guid.NewGuid();
        var material = new WorksheetMaterialEntity(
            materialId,
            _testLessonId,
            "Quiz",
            "Content"
        );
        var question = new MultipleChoiceQuestionEntity(
            Guid.NewGuid(),
            materialId,
            "Quiz Question",
            new List<string> { "A", "B", "C" },
            1
        );

        _mockMaterialRepo.Setup(r => r.GetByIdAsync(materialId))
            .ReturnsAsync(material);
        _mockQuestionRepo.Setup(r => r.AddAsync(It.IsAny<QuestionEntity>()))
            .Returns(Task.CompletedTask);

        // Act
        var result = await _service.CreateQuestionAsync(question);

        // Assert
        Assert.NotNull(result);
        _mockQuestionRepo.Verify(r => r.AddAsync(question), Times.Once);
    }

    [Fact]
    public async Task UpdateQuestionAsync_ValidQuestion_Success()
    {
        // Arrange
        var materialId = Guid.NewGuid();
        var material = new WorksheetMaterialEntity(materialId, _testLessonId, "Material", "Content");
        var question = new MultipleChoiceQuestionEntity(
            Guid.NewGuid(),
            materialId,
            "Updated Question",
            new List<string> { "True", "False" },
            1
        );

        _mockMaterialRepo.Setup(r => r.GetByIdAsync(materialId))
            .ReturnsAsync(material);
        _mockQuestionRepo.Setup(r => r.GetByIdAsync(question.Id))
            .ReturnsAsync(question);
        _mockQuestionRepo.Setup(r => r.UpdateAsync(It.IsAny<QuestionEntity>()))
            .Returns(Task.CompletedTask);

        // Act
        var result = await _service.UpdateQuestionAsync(question);

        // Assert
        Assert.NotNull(result);
        Assert.Equal(question.Id, result.Id);
        _mockQuestionRepo.Verify(r => r.UpdateAsync(question), Times.Once);
    }

    [Fact]
    public async Task UpdateQuestionAsync_NullQuestion_ThrowsArgumentNullException()
    {
        // Act & Assert
        await Assert.ThrowsAsync<ArgumentNullException>(
            () => _service.UpdateQuestionAsync(null!));
    }

    [Fact]
    public async Task UpdateQuestionAsync_NonExistingQuestion_ThrowsInvalidOperationException()
    {
        // Arrange
        var materialId = Guid.NewGuid();
        var material = new WorksheetMaterialEntity(materialId, _testLessonId, "Material", "Content");
        var question = new MultipleChoiceQuestionEntity(
            Guid.NewGuid(),
            materialId,
            "Question",
            new List<string> { "True", "False" },
            0
        );

        _mockMaterialRepo.Setup(r => r.GetByIdAsync(materialId))
            .ReturnsAsync(material);
        _mockQuestionRepo.Setup(r => r.GetByIdAsync(question.Id))
            .ReturnsAsync((QuestionEntity?)null);

        // Act & Assert
        var exception = await Assert.ThrowsAsync<InvalidOperationException>(
            () => _service.UpdateQuestionAsync(question));
        Assert.Contains("not found", exception.Message);
    }

    [Fact]
    public async Task UpdateQuestionAsync_ViolatesReadingMaterialRule_ThrowsInvalidOperationException()
    {
        // Arrange
        var materialId = Guid.NewGuid();
        var material = new ReadingMaterialEntity(materialId, _testLessonId, "Reading", "Content");
        var question = new MultipleChoiceQuestionEntity(
            Guid.NewGuid(),
            materialId,
            "Question",
            new List<string> { "True", "False" },
            0
        );

        _mockMaterialRepo.Setup(r => r.GetByIdAsync(materialId))
            .ReturnsAsync(material);
        _mockQuestionRepo.Setup(r => r.GetByIdAsync(question.Id))
            .ReturnsAsync(question);

        // Act & Assert
        await Assert.ThrowsAsync<InvalidOperationException>(
            () => _service.UpdateQuestionAsync(question));
    }

    [Fact]
    public async Task UpdateQuestionAsync_EmptyQuestionText_ThrowsArgumentException()
    {
        // Arrange
        var materialId = Guid.NewGuid();
        var material = new WorksheetMaterialEntity(materialId, _testLessonId, "Material", "Content");
        var question = new MultipleChoiceQuestionEntity(
            Guid.NewGuid(),
            materialId,
            "   ",
            new List<string> { "True", "False" },
            0
        );

        _mockMaterialRepo.Setup(r => r.GetByIdAsync(materialId))
            .ReturnsAsync(material);
        _mockQuestionRepo.Setup(r => r.GetByIdAsync(question.Id))
            .ReturnsAsync(question);

        // Act & Assert
        await Assert.ThrowsAsync<ArgumentException>(
            () => _service.UpdateQuestionAsync(question));
    }

    [Fact]
    public async Task UpdateQuestionAsync_WrittenQuestionInPoll_ThrowsInvalidOperationException()
    {
        // Arrange - Rule 2B(3)(b): Written question cannot be in a poll
        var materialId = Guid.NewGuid();
        var material = new PollMaterialEntity(
            materialId,
            _testLessonId,
            "Poll",
            "Content"
        );
        var question = new WrittenAnswerQuestionEntity(
            Guid.NewGuid(),
            materialId,
            "Written Question",
            "Answer"
        );

        _mockMaterialRepo.Setup(r => r.GetByIdAsync(materialId))
            .ReturnsAsync(material);
        _mockQuestionRepo.Setup(r => r.GetByIdAsync(question.Id))
            .ReturnsAsync(question);

        // Act & Assert
        await Assert.ThrowsAsync<InvalidOperationException>(
            () => _service.UpdateQuestionAsync(question));
    }

    [Fact]
    public async Task DeleteQuestionAsync_DeletesResponsesFirst_ThenDeletesQuestion()
    {
        // Arrange - Per PersistenceAndCascadingRules.md §2(2): 
        // Deletion of a question must delete any responses associated with it
        var questionId = Guid.NewGuid();
        
        // Use a sequence to verify the order of calls
        var callOrder = new List<string>();
        
        _mockResponseRepo.Setup(r => r.DeleteByQuestionIdAsync(questionId))
            .Callback(() => callOrder.Add("DeleteResponses"))
            .Returns(Task.CompletedTask);
        _mockQuestionRepo.Setup(r => r.DeleteAsync(questionId))
            .Callback(() => callOrder.Add("DeleteQuestion"))
            .Returns(Task.CompletedTask);

        // Act
        await _service.DeleteQuestionAsync(questionId);

        // Assert - verify both methods were called
        _mockResponseRepo.Verify(r => r.DeleteByQuestionIdAsync(questionId), Times.Once);
        _mockQuestionRepo.Verify(r => r.DeleteAsync(questionId), Times.Once);
        
        // Assert - verify responses were deleted BEFORE the question (order matters for orphan removal)
        Assert.Equal(2, callOrder.Count);
        Assert.Equal("DeleteResponses", callOrder[0]);
        Assert.Equal("DeleteQuestion", callOrder[1]);
    }
}
