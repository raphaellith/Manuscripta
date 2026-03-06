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
using Main.Services.GenAI;
using Main.Services.Hubs;
using Main.Services.Network;
using Microsoft.AspNetCore.SignalR;

namespace MainTests.ServicesTests;

public class ResponseServiceTests
{
    private readonly Mock<IResponseRepository> _mockResponseRepo;
    private readonly Mock<IQuestionRepository> _mockQuestionRepo;
    private readonly Mock<IFeedbackRepository> _mockFeedbackRepo;
    private readonly Mock<IDeviceRegistryService> _mockDeviceRegistry;
    private readonly Mock<IHubContext<TeacherPortalHub>> _mockHubContext;
    private readonly Mock<ITcpPairingService> _mockTcpPairingService;
    private readonly DeviceIdValidator _deviceIdValidator;
    private readonly ResponseService _service;

    public ResponseServiceTests()
    {
        _mockResponseRepo = new Mock<IResponseRepository>();
        _mockQuestionRepo = new Mock<IQuestionRepository>();
        _mockFeedbackRepo = new Mock<IFeedbackRepository>();
        _mockDeviceRegistry = new Mock<IDeviceRegistryService>();
        _mockHubContext = new Mock<IHubContext<TeacherPortalHub>>();
        _mockTcpPairingService = new Mock<ITcpPairingService>();
        
        // Default: all devices are valid (paired)
        _mockDeviceRegistry.Setup(r => r.IsDevicePairedAsync(It.IsAny<Guid>()))
            .ReturnsAsync(true);
        
        // Default: no duplicate responses exist
        _mockResponseRepo.Setup(r => r.ExistsForQuestionAndDeviceAsync(It.IsAny<Guid>(), It.IsAny<Guid>()))
            .ReturnsAsync(false);
        
        _deviceIdValidator = new DeviceIdValidator(_mockDeviceRegistry.Object);
        var feedbackQueueService = new FeedbackQueueService(
            _mockHubContext.Object,
            _mockTcpPairingService.Object,
            _mockResponseRepo.Object);
        var feedbackGenerationService = new StubFeedbackGenerationService();
        _service = new ResponseService(
            _mockResponseRepo.Object, 
            _mockQuestionRepo.Object, 
            _mockFeedbackRepo.Object,
            _deviceIdValidator,
            feedbackQueueService,
            feedbackGenerationService);
    }

    private sealed class StubFeedbackGenerationService : IFeedbackGenerationService
    {
        public bool ShouldGenerateFeedback(QuestionEntity question) => false;
        public Guid? GetCurrentlyGeneratingResponseId() => null;
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
    public async Task CreateResponseAsync_UnpairedDevice_ThrowsInvalidOperationException()
    {
        // Arrange - Rule 2C(3)(e): DeviceId must correspond to a valid device
        var questionId = Guid.NewGuid();
        var unpairedDeviceId = Guid.NewGuid();
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
            unpairedDeviceId,
            1
        );

        _mockQuestionRepo.Setup(r => r.GetByIdAsync(questionId))
            .ReturnsAsync(question);
        // Device is NOT paired
        _mockDeviceRegistry.Setup(r => r.IsDevicePairedAsync(unpairedDeviceId))
            .ReturnsAsync(false);

        // Act & Assert
        var exception = await Assert.ThrowsAsync<InvalidOperationException>(
            () => _service.CreateResponseAsync(response));
        Assert.Contains("valid paired device", exception.Message);
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

    #region Auto-Marking Tests

    [Fact]
    public async Task CreateResponseAsync_AndroidClientProvidesIsCorrect_PreservesValue()
    {
        // Arrange - Per Session Interaction §4(2): Android may evaluate and set IsCorrect
        var questionId = Guid.NewGuid();
        var question = new MultipleChoiceQuestionEntity(
            questionId, Guid.NewGuid(), "Question",
            new List<string> { "A", "B", "C" }, 1);
        var response = new MultipleChoiceResponseEntity(
            Guid.NewGuid(), questionId, Guid.NewGuid(), 0, null, true); // Android says correct

        _mockQuestionRepo.Setup(r => r.GetByIdAsync(questionId)).ReturnsAsync(question);
        _mockResponseRepo.Setup(r => r.AddAsync(It.IsAny<ResponseEntity>())).Returns(Task.CompletedTask);

        // Act
        var result = await _service.CreateResponseAsync(response);

        // Assert - Server should NOT override Android's evaluation
        Assert.True(result.IsCorrect);
    }

    [Fact]
    public async Task CreateResponseAsync_MultipleChoiceCorrectAnswer_SetsIsCorrectTrue()
    {
        // Arrange - MCQ with correct answer, no Android evaluation
        var questionId = Guid.NewGuid();
        var question = new MultipleChoiceQuestionEntity(
            questionId, Guid.NewGuid(), "Question",
            new List<string> { "A", "B", "C" }, 1); // Correct answer is index 1
        var response = new MultipleChoiceResponseEntity(
            Guid.NewGuid(), questionId, Guid.NewGuid(), 1); // Student chose index 1

        _mockQuestionRepo.Setup(r => r.GetByIdAsync(questionId)).ReturnsAsync(question);
        _mockResponseRepo.Setup(r => r.AddAsync(It.IsAny<ResponseEntity>())).Returns(Task.CompletedTask);

        // Act
        var result = await _service.CreateResponseAsync(response);

        // Assert
        Assert.True(result.IsCorrect);
    }

    [Fact]
    public async Task CreateResponseAsync_MultipleChoiceIncorrectAnswer_SetsIsCorrectFalse()
    {
        // Arrange - MCQ with wrong answer, no Android evaluation
        var questionId = Guid.NewGuid();
        var question = new MultipleChoiceQuestionEntity(
            questionId, Guid.NewGuid(), "Question",
            new List<string> { "A", "B", "C" }, 1); // Correct answer is index 1
        var response = new MultipleChoiceResponseEntity(
            Guid.NewGuid(), questionId, Guid.NewGuid(), 0); // Student chose index 0

        _mockQuestionRepo.Setup(r => r.GetByIdAsync(questionId)).ReturnsAsync(question);
        _mockResponseRepo.Setup(r => r.AddAsync(It.IsAny<ResponseEntity>())).Returns(Task.CompletedTask);

        // Act
        var result = await _service.CreateResponseAsync(response);

        // Assert
        Assert.False(result.IsCorrect);
    }

    [Fact]
    public async Task CreateResponseAsync_MultipleChoiceNoCorrectAnswer_IsCorrectRemainsNull()
    {
        // Arrange - MCQ without correct answer defined (no auto-marking)
        var questionId = Guid.NewGuid();
        var question = new MultipleChoiceQuestionEntity(
            questionId, Guid.NewGuid(), "Question",
            new List<string> { "A", "B", "C" }, null); // No correct answer
        var response = new MultipleChoiceResponseEntity(
            Guid.NewGuid(), questionId, Guid.NewGuid(), 1);

        _mockQuestionRepo.Setup(r => r.GetByIdAsync(questionId)).ReturnsAsync(question);
        _mockResponseRepo.Setup(r => r.AddAsync(It.IsAny<ResponseEntity>())).Returns(Task.CompletedTask);

        // Act
        var result = await _service.CreateResponseAsync(response);

        // Assert - No auto-marking when no correct answer defined
        Assert.Null(result.IsCorrect);
    }

    [Fact]
    public async Task CreateResponseAsync_WrittenAnswerExactMatch_SetsIsCorrectTrue()
    {
        // Arrange - Written answer matching CorrectAnswer exactly
        var questionId = Guid.NewGuid();
        var question = new WrittenAnswerQuestionEntity(
            questionId, Guid.NewGuid(), "Question", "42");
        var response = new WrittenAnswerResponseEntity(
            Guid.NewGuid(), questionId, Guid.NewGuid(), "42");

        _mockQuestionRepo.Setup(r => r.GetByIdAsync(questionId)).ReturnsAsync(question);
        _mockResponseRepo.Setup(r => r.AddAsync(It.IsAny<ResponseEntity>())).Returns(Task.CompletedTask);

        // Act
        var result = await _service.CreateResponseAsync(response);

        // Assert
        Assert.True(result.IsCorrect);
    }

    [Fact]
    public async Task CreateResponseAsync_WrittenAnswerNoMatch_SetsIsCorrectFalse()
    {
        // Arrange - Written answer NOT matching CorrectAnswer
        var questionId = Guid.NewGuid();
        var question = new WrittenAnswerQuestionEntity(
            questionId, Guid.NewGuid(), "Question", "42");
        var response = new WrittenAnswerResponseEntity(
            Guid.NewGuid(), questionId, Guid.NewGuid(), "41");

        _mockQuestionRepo.Setup(r => r.GetByIdAsync(questionId)).ReturnsAsync(question);
        _mockResponseRepo.Setup(r => r.AddAsync(It.IsAny<ResponseEntity>())).Returns(Task.CompletedTask);

        // Act
        var result = await _service.CreateResponseAsync(response);

        // Assert
        Assert.False(result.IsCorrect);
    }

    [Fact]
    public async Task CreateResponseAsync_WrittenAnswerNoCorrectAnswer_IsCorrectRemainsNull()
    {
        // Arrange - Written answer question without CorrectAnswer (no auto-marking)
        var questionId = Guid.NewGuid();
        var question = new WrittenAnswerQuestionEntity(
            questionId, Guid.NewGuid(), "Question", null); // No correct answer
        var response = new WrittenAnswerResponseEntity(
            Guid.NewGuid(), questionId, Guid.NewGuid(), "Student answer");

        _mockQuestionRepo.Setup(r => r.GetByIdAsync(questionId)).ReturnsAsync(question);
        _mockResponseRepo.Setup(r => r.AddAsync(It.IsAny<ResponseEntity>())).Returns(Task.CompletedTask);

        // Act
        var result = await _service.CreateResponseAsync(response);

        // Assert - No auto-marking when no correct answer defined
        Assert.Null(result.IsCorrect);
    }

    #endregion

    #region Batch Tests

    [Fact]
    public async Task CreateResponseBatchAsync_ValidBatch_Success()
    {
        // Arrange
        var questionId = Guid.NewGuid();
        var deviceId = Guid.NewGuid();
        var question = new MultipleChoiceQuestionEntity(
            questionId,
            Guid.NewGuid(),
            "Question",
            new List<string> { "A", "B", "C" },
            1
        );

        var responses = new List<ResponseEntity>
        {
            new MultipleChoiceResponseEntity(Guid.NewGuid(), questionId, deviceId, 0),
            new MultipleChoiceResponseEntity(Guid.NewGuid(), questionId, deviceId, 1),
            new MultipleChoiceResponseEntity(Guid.NewGuid(), questionId, deviceId, 2)
        };

        _mockQuestionRepo.Setup(r => r.GetByIdAsync(questionId))
            .ReturnsAsync(question);

        // Act
        await _service.CreateResponseBatchAsync(responses);

        // Assert - all responses added
        _mockResponseRepo.Verify(r => r.AddAsync(It.IsAny<ResponseEntity>()), Times.Exactly(3));
    }

    [Fact]
    public async Task CreateResponseBatchAsync_OptimizesDeviceValidation_OnlyChecksUniqueDevices()
    {
        // Arrange - 3 responses from same device, should only validate device once
        var questionId = Guid.NewGuid();
        var deviceId = Guid.NewGuid();
        var question = new MultipleChoiceQuestionEntity(
            questionId, Guid.NewGuid(), "Q", new List<string> { "A", "B" }, 0);

        var responses = new List<ResponseEntity>
        {
            new MultipleChoiceResponseEntity(Guid.NewGuid(), questionId, deviceId, 0),
            new MultipleChoiceResponseEntity(Guid.NewGuid(), questionId, deviceId, 1),
            new MultipleChoiceResponseEntity(Guid.NewGuid(), questionId, deviceId, 0)
        };

        _mockQuestionRepo.Setup(r => r.GetByIdAsync(questionId)).ReturnsAsync(question);

        // Act
        await _service.CreateResponseBatchAsync(responses);

        // Assert - device validation called only once for the single unique device
        _mockDeviceRegistry.Verify(
            r => r.IsDevicePairedAsync(deviceId), 
            Times.Once);
    }

    [Fact]
    public async Task CreateResponseBatchAsync_InvalidDevice_ThrowsAndNothingStored()
    {
        // Arrange
        var questionId = Guid.NewGuid();
        var invalidDeviceId = Guid.NewGuid();
        var question = new MultipleChoiceQuestionEntity(
            questionId, Guid.NewGuid(), "Q", new List<string> { "A", "B" }, 0);

        var responses = new List<ResponseEntity>
        {
            new MultipleChoiceResponseEntity(Guid.NewGuid(), questionId, invalidDeviceId, 0)
        };

        _mockQuestionRepo.Setup(r => r.GetByIdAsync(questionId)).ReturnsAsync(question);
        _mockDeviceRegistry.Setup(r => r.IsDevicePairedAsync(invalidDeviceId))
            .ReturnsAsync(false);

        // Act & Assert
        await Assert.ThrowsAsync<InvalidOperationException>(
            () => _service.CreateResponseBatchAsync(responses));
        
        _mockResponseRepo.Verify(r => r.AddAsync(It.IsAny<ResponseEntity>()), Times.Never);
    }

    [Fact]
    public async Task CreateResponseBatchAsync_EmptyBatch_ThrowsArgumentException()
    {
        // Act & Assert
        await Assert.ThrowsAsync<ArgumentException>(
            () => _service.CreateResponseBatchAsync(new List<ResponseEntity>()));
    }

    [Fact]
    public async Task CreateResponseAsync_DuplicateQuestionDeviceCombination_ThrowsInvalidOperationException()
    {
        // Arrange - Rule 2C(3)(f): One response per question per device
        var questionId = Guid.NewGuid();
        var deviceId = Guid.NewGuid();
        var question = new MultipleChoiceQuestionEntity(
            questionId, Guid.NewGuid(), "Q", new List<string> { "A", "B" }, 0);
        var response = new MultipleChoiceResponseEntity(
            Guid.NewGuid(), questionId, deviceId, 1);

        _mockQuestionRepo.Setup(r => r.GetByIdAsync(questionId)).ReturnsAsync(question);
        // Response already exists for this question-device combination
        _mockResponseRepo.Setup(r => r.ExistsForQuestionAndDeviceAsync(questionId, deviceId))
            .ReturnsAsync(true);

        // Act & Assert
        var exception = await Assert.ThrowsAsync<InvalidOperationException>(
            () => _service.CreateResponseAsync(response));
        Assert.Contains("already exists", exception.Message);
        _mockResponseRepo.Verify(r => r.AddAsync(It.IsAny<ResponseEntity>()), Times.Never);
    }

    #endregion
}
