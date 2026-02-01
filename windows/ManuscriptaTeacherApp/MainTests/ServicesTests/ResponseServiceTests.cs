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
using Main.Models.Dtos;

namespace MainTests.ServicesTests;

public class ResponseServiceTests
{
    private readonly Mock<IResponseRepository> _mockResponseRepo;
    private readonly Mock<IQuestionRepository> _mockQuestionRepo;
    private readonly Mock<IFeedbackRepository> _mockFeedbackRepo;
    private readonly Mock<DeviceIdValidator> _mockDeviceIdValidator;
    private readonly ResponseService _service;

    public ResponseServiceTests()
    {
        _mockResponseRepo = new Mock<IResponseRepository>();
        _mockQuestionRepo = new Mock<IQuestionRepository>();
        _mockFeedbackRepo = new Mock<IFeedbackRepository>();
        
        // Mock DeviceIdValidator - requires IDeviceRegistryService in constructor
        var mockDeviceRegistry = new Mock<IDeviceRegistryService>();
        mockDeviceRegistry.Setup(r => r.IsDevicePairedAsync(It.IsAny<Guid>())).ReturnsAsync(true);
        _mockDeviceIdValidator = new Mock<DeviceIdValidator>(mockDeviceRegistry.Object) { CallBase = false };
        _mockDeviceIdValidator.Setup(v => v.ValidateOrThrowAsync(It.IsAny<Guid>())).Returns(Task.CompletedTask);
        
        _service = new ResponseService(
            _mockResponseRepo.Object, 
            _mockQuestionRepo.Object, 
            _mockFeedbackRepo.Object,
            _mockDeviceIdValidator.Object);
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
    public async Task CreateResponseAsync_NullResponseEntity_ThrowsArgumentNullException()
    {
        // Act & Assert
        await Assert.ThrowsAsync<ArgumentNullException>(
            () => _service.CreateResponseAsync((ResponseEntity)null!));
    }

    [Fact]
    public async Task CreateResponseAsync_NonExistingQuestion_ThrowsInvalidOperationException()
    {
        // Arrange - Rule 2C(3)(a): Responses must reference a Question
        var questionId = Guid.NewGuid();
        var response = new MultipleChoiceResponseEntity(
            Guid.NewGuid(),
            questionId,
            Guid.NewGuid(),
            0
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
    public async Task CreateResponseAsync_MultipleChoiceWithTrueFalseOptions_Success()
    {
        // Arrange - Now uses multiple choice with True/False options
        var questionId = Guid.NewGuid();
        var question = new MultipleChoiceQuestionEntity(
            questionId,
            Guid.NewGuid(),
            "Question",
            new List<string> { "True", "False" },
            0
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
    public async Task CreateResponseAsync_WrittenResponseForMultipleChoiceQuestion_ThrowsInvalidOperationException()
    {
        // Arrange - Written response for multiple choice question
        var questionId = Guid.NewGuid();
        var question = new MultipleChoiceQuestionEntity(
            questionId,
            Guid.NewGuid(),
            "Question",
            new List<string> { "A", "B" },
            0
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
    public async Task CreateResponseAsync_IndexOutOfBounds_ThrowsInvalidOperationException()
    {
        // Arrange - Response index out of bounds
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
            5  // Invalid index
        );

        _mockQuestionRepo.Setup(r => r.GetByIdAsync(questionId))
            .ReturnsAsync(question);

        // Act & Assert
        var exception = await Assert.ThrowsAsync<InvalidOperationException>(
            () => _service.CreateResponseAsync(response));
        Assert.Contains("out of range", exception.Message, StringComparison.OrdinalIgnoreCase);
    }

    [Fact]
    public async Task UpdateResponseAsync_ValidResponse_Success()
    {
        // Arrange
        var questionId = Guid.NewGuid();
        var question = new MultipleChoiceQuestionEntity(
            questionId,
            Guid.NewGuid(),
            "Question",
            new List<string> { "True", "False" },
            0
        );
        var response = new MultipleChoiceResponseEntity(
            Guid.NewGuid(),
            questionId,
            Guid.NewGuid(),
            1
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
        var question = new MultipleChoiceQuestionEntity(questionId, Guid.NewGuid(), "Q", new List<string> { "T", "F" }, 0);
        var response = new MultipleChoiceResponseEntity(Guid.NewGuid(), questionId, Guid.NewGuid(), 0);

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
    public async Task UpdateResponseAsync_WrittenResponseForMultipleChoiceQuestion_ThrowsInvalidOperationException()
    {
        // Arrange - Written response for multiple choice question
        var questionId = Guid.NewGuid();
        var question = new MultipleChoiceQuestionEntity(
            questionId,
            Guid.NewGuid(),
            "Question",
            new List<string> { "A", "B" },
            0
        );
        var response = new WrittenAnswerResponseEntity(
            Guid.NewGuid(),
            questionId,
            Guid.NewGuid(),
            "Some answer"
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
    public async Task DeleteResponseAsync_CascadesDeleteToFeedback()
    {
        // Arrange
        var responseId = Guid.NewGuid();
        
        _mockFeedbackRepo.Setup(r => r.DeleteByResponseIdAsync(responseId))
            .Returns(Task.CompletedTask);
        _mockResponseRepo.Setup(r => r.DeleteAsync(responseId))
            .Returns(Task.CompletedTask);

        // Act
        await _service.DeleteResponseAsync(responseId);

        // Assert - Per §2(2A): Feedback must be deleted before response
        _mockFeedbackRepo.Verify(r => r.DeleteByResponseIdAsync(responseId), Times.Once);
        _mockResponseRepo.Verify(r => r.DeleteAsync(responseId), Times.Once);
    }


    #region DTO & Batch Tests

    [Fact]
    public async Task CreateResponseAsync_ValidDto_FetchesQuestionOnceAndSaves()
    {
        // Arrange
        var questionId = Guid.NewGuid();
        var deviceId = Guid.NewGuid();
        
        var dto = new SubmitResponseDto
        {
            Id = Guid.NewGuid().ToString(),
            QuestionId = questionId.ToString(),
            DeviceId = deviceId.ToString(),
            Answer = "1",
            Timestamp = DateTime.UtcNow.ToString("O"),
            IsCorrect = null
        };

        var question = new MultipleChoiceQuestionEntity(
            questionId, Guid.NewGuid(), "Q", new List<string> { "A", "B", "C" }, 0);

        _mockQuestionRepo.Setup(r => r.GetByIdAsync(questionId))
            .ReturnsAsync(question);
        _mockResponseRepo.Setup(r => r.AddAsync(It.IsAny<ResponseEntity>()))
            .Returns(Task.CompletedTask);

        // Act
        var result = await _service.CreateResponseAsync(dto);

        // Assert
        Assert.NotNull(result);
        Assert.Equal(Guid.Parse(dto.Id), result.Id);
        
        // Verify optimization: GetByIdAsync called ONCE
        _mockQuestionRepo.Verify(r => r.GetByIdAsync(questionId), Times.Once);
        
        // Verify Persistence
        _mockResponseRepo.Verify(r => r.AddAsync(It.IsAny<ResponseEntity>()), Times.Once);
    }

    [Fact]
    public async Task CreateBatchResponsesAsync_AllValid_FetchesQuestionsAndSavesAll()
    {
        // Arrange
        var questionId1 = Guid.NewGuid();
        var questionId2 = Guid.NewGuid();
        var deviceId = Guid.NewGuid();

        var dto1 = new SubmitResponseDto
        {
            Id = Guid.NewGuid().ToString(),
            QuestionId = questionId1.ToString(),
            DeviceId = deviceId.ToString(),
            Answer = "0",
            Timestamp = DateTime.UtcNow.ToString("O")
        };
        var dto2 = new SubmitResponseDto
        {
            Id = Guid.NewGuid().ToString(),
            QuestionId = questionId2.ToString(),
            DeviceId = deviceId.ToString(),
            Answer = "1",
            Timestamp = DateTime.UtcNow.ToString("O")
        };

        var question1 = new MultipleChoiceQuestionEntity(questionId1, Guid.NewGuid(), "Q1", new List<string> { "A", "B" }, 0);
        var question2 = new MultipleChoiceQuestionEntity(questionId2, Guid.NewGuid(), "Q2", new List<string> { "A", "B" }, 1);

        _mockQuestionRepo.Setup(r => r.GetByIdAsync(questionId1)).ReturnsAsync(question1);
        _mockQuestionRepo.Setup(r => r.GetByIdAsync(questionId2)).ReturnsAsync(question2);
        _mockResponseRepo.Setup(r => r.AddAsync(It.IsAny<ResponseEntity>())).Returns(Task.CompletedTask);

        // Act
        await _service.CreateBatchResponsesAsync(new List<SubmitResponseDto> { dto1, dto2 });

        // Assert
        // Verify optimizations: 1 query per DTO
        _mockQuestionRepo.Verify(r => r.GetByIdAsync(questionId1), Times.Once);
        _mockQuestionRepo.Verify(r => r.GetByIdAsync(questionId2), Times.Once);

        // Verify Persistence: 2 Adds
        _mockResponseRepo.Verify(r => r.AddAsync(It.IsAny<ResponseEntity>()), Times.Exactly(2));
    }

    [Fact]
    public async Task CreateBatchResponsesAsync_OneInvalid_ThrowsAndSavesNone()
    {
        // Arrange
        var questionId1 = Guid.NewGuid();
        var questionId2 = Guid.NewGuid(); // Will mimic invalid answer for this one

        var dto1 = new SubmitResponseDto
        {
            Id = Guid.NewGuid().ToString(),
            QuestionId = questionId1.ToString(),
            DeviceId = Guid.NewGuid().ToString(),
            Answer = "0", // Valid
            Timestamp = DateTime.UtcNow.ToString("O")
        };
        var dto2 = new SubmitResponseDto
        {
            Id = Guid.NewGuid().ToString(),
            QuestionId = questionId2.ToString(),
            DeviceId = Guid.NewGuid().ToString(),
            Answer = "99", // Invalid index
            Timestamp = DateTime.UtcNow.ToString("O")
        };

        var question1 = new MultipleChoiceQuestionEntity(questionId1, Guid.NewGuid(), "Q1", new List<string> { "A", "B" }, 0);
        var question2 = new MultipleChoiceQuestionEntity(questionId2, Guid.NewGuid(), "Q2", new List<string> { "A", "B" }, 0);

        _mockQuestionRepo.Setup(r => r.GetByIdAsync(questionId1)).ReturnsAsync(question1);
        _mockQuestionRepo.Setup(r => r.GetByIdAsync(questionId2)).ReturnsAsync(question2);

        // Act & Assert
        await Assert.ThrowsAsync<InvalidOperationException>(
            () => _service.CreateBatchResponsesAsync(new [] { dto1, dto2 }));

        // Verify Atomic Failure: No calls to AddAsync
        _mockResponseRepo.Verify(r => r.AddAsync(It.IsAny<ResponseEntity>()), Times.Never);
    }

    #endregion
}
