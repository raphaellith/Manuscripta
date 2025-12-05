using System;
using System.Collections.Generic;
using System.Linq;
using System.Threading.Tasks;
using Moq;
using Xunit;
using Main.Models.Entities.Questions;
using Main.Models.Entities.Responses;
using Main.Services;
using Main.Services.Repositories;

namespace MainTests.ServicesTests;

public class ResponseServiceTests
{
    private readonly Mock<IResponseRepository> _mockResponseRepo;
    private readonly Mock<IQuestionRepository> _mockQuestionRepo;
    private readonly ResponseService _service;

    public ResponseServiceTests()
    {
        _mockResponseRepo = new Mock<IResponseRepository>();
        _mockQuestionRepo = new Mock<IQuestionRepository>();
        _service = new ResponseService(_mockResponseRepo.Object, _mockQuestionRepo.Object);
    }

    [Fact]
    public async Task CreateResponseAsync_ValidMultipleChoiceResponse_Success()
    {
        // Arrange
        var questionId = Guid.NewGuid();
        var question = new MultipleChoiceQuestionEntity(
            questionId,
            Guid.NewGuid(),
            "Question",
            new List<string> { "A", "B", "C" },
            1
        );
        var response = new MultipleChoiceResponseEntity(
            Guid.NewGuid(),
            questionId,
            Guid.NewGuid(),
            1
        );

        _mockQuestionRepo.Setup(r => r.GetByIdAsync(questionId))
            .ReturnsAsync(question);
        _mockResponseRepo.Setup(r => r.AddAsync(It.IsAny<ResponseEntity>()))
            .Returns(Task.CompletedTask);

        // Act
        var result = await _service.CreateResponseAsync(response);

        // Assert
        Assert.NotNull(result);
        Assert.Equal(response.Id, result.Id);
        _mockResponseRepo.Verify(r => r.AddAsync(response), Times.Once);
    }

    [Fact]
    public async Task CreateResponseAsync_NullResponse_ThrowsArgumentNullException()
    {
        // Act & Assert
        await Assert.ThrowsAsync<ArgumentNullException>(
            () => _service.CreateResponseAsync(null!));
    }

    [Fact]
    public async Task CreateResponseAsync_NonExistingQuestion_ThrowsInvalidOperationException()
    {
        // Arrange - Rule 2C(3)(a): Responses must reference a Question
        var questionId = Guid.NewGuid();
        var response = new TrueFalseResponseEntity(
            Guid.NewGuid(),
            questionId,
            Guid.NewGuid(),
            true
        );

        _mockQuestionRepo.Setup(r => r.GetByIdAsync(questionId))
            .ReturnsAsync((QuestionEntity?)null);

        // Act & Assert
        var exception = await Assert.ThrowsAsync<InvalidOperationException>(
            () => _service.CreateResponseAsync(response));
        Assert.Contains("not found", exception.Message);
    }

    [Fact]
    public async Task CreateResponseAsync_MultipleChoiceWithInvalidIndex_ThrowsInvalidOperationException()
    {
        // Arrange - Rule 2C(3)(b): Answer must be a valid index
        var questionId = Guid.NewGuid();
        var question = new MultipleChoiceQuestionEntity(
            questionId,
            Guid.NewGuid(),
            "Question",
            new List<string> { "A", "B", "C" },
            1
        );
        var response = new MultipleChoiceResponseEntity(
            Guid.NewGuid(),
            questionId,
            Guid.NewGuid(),
            5  // Invalid index - only 3 options (0-2)
        );

        _mockQuestionRepo.Setup(r => r.GetByIdAsync(questionId))
            .ReturnsAsync(question);

        // Act & Assert
        var exception = await Assert.ThrowsAsync<InvalidOperationException>(
            () => _service.CreateResponseAsync(response));
        Assert.Contains("out of range", exception.Message, StringComparison.OrdinalIgnoreCase);
    }

    [Fact]
    public async Task CreateResponseAsync_MultipleChoiceWithNegativeIndex_ThrowsInvalidOperationException()
    {
        // Arrange - Rule 2C(3)(b): Answer must be a valid index
        var questionId = Guid.NewGuid();
        var question = new MultipleChoiceQuestionEntity(
            questionId,
            Guid.NewGuid(),
            "Question",
            new List<string> { "A", "B" },
            0
        );
        var response = new MultipleChoiceResponseEntity(
            Guid.NewGuid(),
            questionId,
            Guid.NewGuid(),
            -1  // Invalid negative index
        );

        _mockQuestionRepo.Setup(r => r.GetByIdAsync(questionId))
            .ReturnsAsync(question);

        // Act & Assert
        var exception = await Assert.ThrowsAsync<InvalidOperationException>(
            () => _service.CreateResponseAsync(response));
        Assert.Contains("out of range", exception.Message, StringComparison.OrdinalIgnoreCase);
    }

    [Fact]
    public async Task CreateResponseAsync_MultipleChoiceWithBoundaryIndex_Success()
    {
        // Arrange - Test boundary condition (last valid index)
        var questionId = Guid.NewGuid();
        var question = new MultipleChoiceQuestionEntity(
            questionId,
            Guid.NewGuid(),
            "Question",
            new List<string> { "A", "B", "C" },
            0
        );
        var response = new MultipleChoiceResponseEntity(
            Guid.NewGuid(),
            questionId,
            Guid.NewGuid(),
            2  // Last valid index for 3 options
        );

        _mockQuestionRepo.Setup(r => r.GetByIdAsync(questionId))
            .ReturnsAsync(question);
        _mockResponseRepo.Setup(r => r.AddAsync(It.IsAny<ResponseEntity>()))
            .Returns(Task.CompletedTask);

        // Act
        var result = await _service.CreateResponseAsync(response);

        // Assert
        Assert.NotNull(result);
        _mockResponseRepo.Verify(r => r.AddAsync(response), Times.Once);
    }

    [Fact]
    public async Task CreateResponseAsync_TrueFalseResponse_Success()
    {
        // Arrange
        var questionId = Guid.NewGuid();
        var question = new TrueFalseQuestionEntity(
            questionId,
            Guid.NewGuid(),
            "Question",
            true
        );
        var response = new TrueFalseResponseEntity(
            Guid.NewGuid(),
            questionId,
            Guid.NewGuid(),
            false
        );

        _mockQuestionRepo.Setup(r => r.GetByIdAsync(questionId))
            .ReturnsAsync(question);
        _mockResponseRepo.Setup(r => r.AddAsync(It.IsAny<ResponseEntity>()))
            .Returns(Task.CompletedTask);

        // Act
        var result = await _service.CreateResponseAsync(response);

        // Assert
        Assert.NotNull(result);
        _mockResponseRepo.Verify(r => r.AddAsync(response), Times.Once);
    }

    [Fact]
    public async Task CreateResponseAsync_WrittenAnswerResponse_Success()
    {
        // Arrange
        var questionId = Guid.NewGuid();
        var question = new WrittenAnswerQuestionEntity(
            questionId,
            Guid.NewGuid(),
            "Question",
            "Correct Answer"
        );
        var response = new WrittenAnswerResponseEntity(
            Guid.NewGuid(),
            questionId,
            Guid.NewGuid(),
            "Student Answer"
        );

        _mockQuestionRepo.Setup(r => r.GetByIdAsync(questionId))
            .ReturnsAsync(question);
        _mockResponseRepo.Setup(r => r.AddAsync(It.IsAny<ResponseEntity>()))
            .Returns(Task.CompletedTask);

        // Act
        var result = await _service.CreateResponseAsync(response);

        // Assert
        Assert.NotNull(result);
        _mockResponseRepo.Verify(r => r.AddAsync(response), Times.Once);
    }

    [Fact]
    public async Task CreateResponseAsync_ResponseTypeMismatch_ThrowsInvalidOperationException()
    {
        // Arrange - Multiple choice response for true/false question
        var questionId = Guid.NewGuid();
        var question = new TrueFalseQuestionEntity(
            questionId,
            Guid.NewGuid(),
            "Question",
            true
        );
        var response = new MultipleChoiceResponseEntity(
            Guid.NewGuid(),
            questionId,
            Guid.NewGuid(),
            0
        );

        _mockQuestionRepo.Setup(r => r.GetByIdAsync(questionId))
            .ReturnsAsync(question);

        // Act & Assert
        var exception = await Assert.ThrowsAsync<InvalidOperationException>(
            () => _service.CreateResponseAsync(response));
        Assert.Contains("does not match", exception.Message, StringComparison.OrdinalIgnoreCase);
    }

    [Fact]
    public async Task CreateResponseAsync_TrueFalseResponseForMultipleChoice_ThrowsInvalidOperationException()
    {
        // Arrange
        var questionId = Guid.NewGuid();
        var question = new MultipleChoiceQuestionEntity(
            questionId,
            Guid.NewGuid(),
            "Question",
            new List<string> { "A", "B" },
            0
        );
        var response = new TrueFalseResponseEntity(
            Guid.NewGuid(),
            questionId,
            Guid.NewGuid(),
            true
        );

        _mockQuestionRepo.Setup(r => r.GetByIdAsync(questionId))
            .ReturnsAsync(question);

        // Act & Assert
        var exception = await Assert.ThrowsAsync<InvalidOperationException>(
            () => _service.CreateResponseAsync(response));
        Assert.Contains("does not match", exception.Message, StringComparison.OrdinalIgnoreCase);
    }

    [Fact]
    public async Task CreateResponseAsync_WrittenResponseForTrueFalse_ThrowsInvalidOperationException()
    {
        // Arrange
        var questionId = Guid.NewGuid();
        var question = new TrueFalseQuestionEntity(
            questionId,
            Guid.NewGuid(),
            "Question",
            true
        );
        var response = new WrittenAnswerResponseEntity(
            Guid.NewGuid(),
            questionId,
            Guid.NewGuid(),
            "Some answer"
        );

        _mockQuestionRepo.Setup(r => r.GetByIdAsync(questionId))
            .ReturnsAsync(question);

        // Act & Assert
        var exception = await Assert.ThrowsAsync<InvalidOperationException>(
            () => _service.CreateResponseAsync(response));
        Assert.Contains("does not match", exception.Message, StringComparison.OrdinalIgnoreCase);
    }

    [Fact]
    public async Task GetResponseByIdAsync_ExistingResponse_ReturnsResponse()
    {
        // Arrange
        var responseId = Guid.NewGuid();
        var response = new TrueFalseResponseEntity(
            responseId,
            Guid.NewGuid(),
            Guid.NewGuid(),
            true
        );

        _mockResponseRepo.Setup(r => r.GetByIdAsync(responseId))
            .ReturnsAsync(response);

        // Act
        var result = await _service.GetResponseByIdAsync(responseId);

        // Assert
        Assert.NotNull(result);
        Assert.Equal(responseId, result!.Id);
        _mockResponseRepo.Verify(r => r.GetByIdAsync(responseId), Times.Once);
    }

    [Fact]
    public async Task GetResponseByIdAsync_NonExistingResponse_ReturnsNull()
    {
        // Arrange
        var responseId = Guid.NewGuid();
        _mockResponseRepo.Setup(r => r.GetByIdAsync(responseId))
            .ReturnsAsync((ResponseEntity?)null);

        // Act
        var result = await _service.GetResponseByIdAsync(responseId);

        // Assert
        Assert.Null(result);
    }

    [Fact]
    public async Task GetResponsesByQuestionIdAsync_ReturnsResponses()
    {
        // Arrange
        var questionId = Guid.NewGuid();
        var responses = new List<ResponseEntity>
        {
            new TrueFalseResponseEntity(Guid.NewGuid(), questionId, Guid.NewGuid(), true),
            new TrueFalseResponseEntity(Guid.NewGuid(), questionId, Guid.NewGuid(), false)
        };

        _mockResponseRepo.Setup(r => r.GetByQuestionIdAsync(questionId))
            .ReturnsAsync(responses);

        // Act
        var result = await _service.GetResponsesByQuestionIdAsync(questionId);

        // Assert
        var resultList = result.ToList();
        Assert.Equal(2, resultList.Count);
        _mockResponseRepo.Verify(r => r.GetByQuestionIdAsync(questionId), Times.Once);
    }

    [Fact]
    public async Task UpdateResponseAsync_ValidResponse_Success()
    {
        // Arrange
        var questionId = Guid.NewGuid();
        var question = new TrueFalseQuestionEntity(
            questionId,
            Guid.NewGuid(),
            "Question",
            true
        );
        var response = new TrueFalseResponseEntity(
            Guid.NewGuid(),
            questionId,
            Guid.NewGuid(),
            false
        );

        _mockQuestionRepo.Setup(r => r.GetByIdAsync(questionId))
            .ReturnsAsync(question);
        _mockResponseRepo.Setup(r => r.GetByIdAsync(response.Id))
            .ReturnsAsync(response);
        _mockResponseRepo.Setup(r => r.UpdateAsync(It.IsAny<ResponseEntity>()))
            .Returns(Task.CompletedTask);

        // Act
        var result = await _service.UpdateResponseAsync(response);

        // Assert
        Assert.NotNull(result);
        Assert.Equal(response.Id, result.Id);
        _mockResponseRepo.Verify(r => r.UpdateAsync(response), Times.Once);
    }

    [Fact]
    public async Task UpdateResponseAsync_NullResponse_ThrowsArgumentNullException()
    {
        // Act & Assert
        await Assert.ThrowsAsync<ArgumentNullException>(
            () => _service.UpdateResponseAsync(null!));
    }

    [Fact]
    public async Task UpdateResponseAsync_NonExistingResponse_ThrowsInvalidOperationException()
    {
        // Arrange
        var questionId = Guid.NewGuid();
        var question = new TrueFalseQuestionEntity(questionId, Guid.NewGuid(), "Q", true);
        var response = new TrueFalseResponseEntity(Guid.NewGuid(), questionId, Guid.NewGuid(), true);

        _mockQuestionRepo.Setup(r => r.GetByIdAsync(questionId))
            .ReturnsAsync(question);
        _mockResponseRepo.Setup(r => r.GetByIdAsync(response.Id))
            .ReturnsAsync((ResponseEntity?)null);

        // Act & Assert
        var exception = await Assert.ThrowsAsync<InvalidOperationException>(
            () => _service.UpdateResponseAsync(response));
        Assert.Contains("not found", exception.Message);
    }

    [Fact]
    public async Task UpdateResponseAsync_ViolatesIndexRule_ThrowsInvalidOperationException()
    {
        // Arrange
        var questionId = Guid.NewGuid();
        var question = new MultipleChoiceQuestionEntity(
            questionId,
            Guid.NewGuid(),
            "Question",
            new List<string> { "A", "B" },
            0
        );
        var response = new MultipleChoiceResponseEntity(
            Guid.NewGuid(),
            questionId,
            Guid.NewGuid(),
            10  // Invalid index
        );

        _mockQuestionRepo.Setup(r => r.GetByIdAsync(questionId))
            .ReturnsAsync(question);
        _mockResponseRepo.Setup(r => r.GetByIdAsync(response.Id))
            .ReturnsAsync(response);

        // Act & Assert
        await Assert.ThrowsAsync<InvalidOperationException>(
            () => _service.UpdateResponseAsync(response));
    }

    [Fact]
    public async Task UpdateResponseAsync_ResponseTypeMismatch_ThrowsInvalidOperationException()
    {
        // Arrange - Multiple choice response for true/false question
        var questionId = Guid.NewGuid();
        var question = new TrueFalseQuestionEntity(
            questionId,
            Guid.NewGuid(),
            "Question",
            true
        );
        var response = new MultipleChoiceResponseEntity(
            Guid.NewGuid(),
            questionId,
            Guid.NewGuid(),
            0
        );

        _mockQuestionRepo.Setup(r => r.GetByIdAsync(questionId))
            .ReturnsAsync(question);
        _mockResponseRepo.Setup(r => r.GetByIdAsync(response.Id))
            .ReturnsAsync(response);

        // Act & Assert
        var exception = await Assert.ThrowsAsync<InvalidOperationException>(
            () => _service.UpdateResponseAsync(response));
        Assert.Contains("does not match", exception.Message, StringComparison.OrdinalIgnoreCase);
    }

    [Fact]
    public async Task DeleteResponseAsync_CallsRepository()
    {
        // Arrange
        var responseId = Guid.NewGuid();
        _mockResponseRepo.Setup(r => r.DeleteAsync(responseId))
            .Returns(Task.CompletedTask);

        // Act
        await _service.DeleteResponseAsync(responseId);

        // Assert
        _mockResponseRepo.Verify(r => r.DeleteAsync(responseId), Times.Once);
    }
}
