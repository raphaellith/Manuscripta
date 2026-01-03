using Microsoft.AspNetCore.Mvc;
using Microsoft.Extensions.Logging;
using Moq;
using Main.Controllers;
using Main.Models.Dtos;
using Main.Models.Entities.Questions;
using Main.Models.Entities.Responses;
using Main.Services;
using Main.Services.Repositories;
using Xunit;

namespace MainTests.ControllersTests;

/// <summary>
/// Unit tests for ResponseController.
/// Verifies POST /responses and POST /responses/batch endpoints per API Contract.md §2.3.
/// </summary>
public class ResponseControllerTests
{
    private readonly Mock<IResponseService> _mockResponseService;
    private readonly Mock<IQuestionRepository> _mockQuestionRepository;
    private readonly Mock<ILogger<ResponseController>> _mockLogger;
    private readonly ResponseController _controller;

    private readonly Guid _testQuestionId = Guid.NewGuid();
    private readonly Guid _testMaterialId = Guid.NewGuid();
    private readonly Guid _testDeviceId = Guid.NewGuid();
    private readonly Guid _testResponseId = Guid.NewGuid();

    public ResponseControllerTests()
    {
        _mockResponseService = new Mock<IResponseService>();
        _mockQuestionRepository = new Mock<IQuestionRepository>();
        _mockLogger = new Mock<ILogger<ResponseController>>();
        _controller = new ResponseController(
            _mockResponseService.Object,
            _mockQuestionRepository.Object,
            _mockLogger.Object);
    }

    #region Constructor Tests

    [Fact]
    public void Constructor_NullResponseService_ThrowsArgumentNullException()
    {
        Assert.Throws<ArgumentNullException>(() =>
            new ResponseController(null!, _mockQuestionRepository.Object, _mockLogger.Object));
    }

    [Fact]
    public void Constructor_NullQuestionRepository_ThrowsArgumentNullException()
    {
        Assert.Throws<ArgumentNullException>(() =>
            new ResponseController(_mockResponseService.Object, null!, _mockLogger.Object));
    }

    [Fact]
    public void Constructor_NullLogger_ThrowsArgumentNullException()
    {
        Assert.Throws<ArgumentNullException>(() =>
            new ResponseController(_mockResponseService.Object, _mockQuestionRepository.Object, null!));
    }

    #endregion

    #region POST /responses Tests

    [Fact]
    public async Task SubmitResponse_ValidMultipleChoiceResponse_Returns201Created()
    {
        // Arrange
        var question = new MultipleChoiceQuestionEntity(
            _testQuestionId, _testMaterialId, "Test Question", 
            new List<string> { "A", "B", "C" }, 0);

        _mockQuestionRepository.Setup(r => r.GetByIdAsync(_testQuestionId))
            .ReturnsAsync(question);
        _mockResponseService.Setup(s => s.CreateResponseAsync(It.IsAny<ResponseEntity>()))
            .ReturnsAsync((ResponseEntity r) => r);

        var request = CreateValidMultipleChoiceRequest();

        // Act
        var result = await _controller.SubmitResponse(request);

        // Assert
        var statusResult = Assert.IsType<ObjectResult>(result);
        Assert.Equal(201, statusResult.StatusCode);
    }

    [Fact]
    public async Task SubmitResponse_ValidWrittenAnswerResponse_Returns201Created()
    {
        // Arrange
        var question = new WrittenAnswerQuestionEntity(
            _testQuestionId, _testMaterialId, "Test Question", "Expected Answer");

        _mockQuestionRepository.Setup(r => r.GetByIdAsync(_testQuestionId))
            .ReturnsAsync(question);
        _mockResponseService.Setup(s => s.CreateResponseAsync(It.IsAny<ResponseEntity>()))
            .ReturnsAsync((ResponseEntity r) => r);

        var request = CreateValidWrittenAnswerRequest();

        // Act
        var result = await _controller.SubmitResponse(request);

        // Assert
        var statusResult = Assert.IsType<ObjectResult>(result);
        Assert.Equal(201, statusResult.StatusCode);
    }

    [Fact]
    public async Task SubmitResponse_NullRequest_Returns400BadRequest()
    {
        // Act
        var result = await _controller.SubmitResponse(null!);

        // Assert
        Assert.IsType<BadRequestObjectResult>(result);
    }

    [Fact]
    public async Task SubmitResponse_MissingId_Returns400BadRequest()
    {
        // Arrange
        var request = CreateValidMultipleChoiceRequest();
        request.Id = "";

        // Act
        var result = await _controller.SubmitResponse(request);

        // Assert
        var badRequest = Assert.IsType<BadRequestObjectResult>(result);
        Assert.Contains("Id", badRequest.Value?.ToString());
    }

    [Fact]
    public async Task SubmitResponse_InvalidIdFormat_Returns400BadRequest()
    {
        // Arrange
        var request = CreateValidMultipleChoiceRequest();
        request.Id = "not-a-guid";

        // Act
        var result = await _controller.SubmitResponse(request);

        // Assert
        var badRequest = Assert.IsType<BadRequestObjectResult>(result);
        Assert.Contains("GUID", badRequest.Value?.ToString());
    }

    [Fact]
    public async Task SubmitResponse_MissingQuestionId_Returns400BadRequest()
    {
        // Arrange
        var request = CreateValidMultipleChoiceRequest();
        request.QuestionId = "";

        // Act
        var result = await _controller.SubmitResponse(request);

        // Assert
        var badRequest = Assert.IsType<BadRequestObjectResult>(result);
        Assert.Contains("QuestionId", badRequest.Value?.ToString());
    }

    [Fact]
    public async Task SubmitResponse_MissingDeviceId_Returns400BadRequest()
    {
        // Arrange
        var request = CreateValidMultipleChoiceRequest();
        request.DeviceId = "";

        // Act
        var result = await _controller.SubmitResponse(request);

        // Assert
        var badRequest = Assert.IsType<BadRequestObjectResult>(result);
        Assert.Contains("DeviceId", badRequest.Value?.ToString());
    }

    [Fact]
    public async Task SubmitResponse_MissingAnswer_Returns400BadRequest()
    {
        // Arrange
        var request = CreateValidMultipleChoiceRequest();
        request.Answer = "";

        // Act
        var result = await _controller.SubmitResponse(request);

        // Assert
        var badRequest = Assert.IsType<BadRequestObjectResult>(result);
        Assert.Contains("Answer", badRequest.Value?.ToString());
    }

    [Fact]
    public async Task SubmitResponse_InvalidTimestamp_Returns400BadRequest()
    {
        // Arrange
        var request = CreateValidMultipleChoiceRequest();
        request.Timestamp = "not-a-date";

        // Act
        var result = await _controller.SubmitResponse(request);

        // Assert
        var badRequest = Assert.IsType<BadRequestObjectResult>(result);
        Assert.Contains("Timestamp", badRequest.Value?.ToString());
    }

    [Fact]
    public async Task SubmitResponse_QuestionNotFound_Returns400BadRequest()
    {
        // Arrange - question doesn't exist
        _mockQuestionRepository.Setup(r => r.GetByIdAsync(_testQuestionId))
            .ReturnsAsync((QuestionEntity?)null);

        var request = CreateValidMultipleChoiceRequest();

        // Act
        var result = await _controller.SubmitResponse(request);

        // Assert - controller catches InvalidOperationException and returns 400
        Assert.IsType<BadRequestObjectResult>(result);
    }

    [Fact]
    public async Task SubmitResponse_InvalidAnswerForMultipleChoice_Returns400BadRequest()
    {
        // Arrange - answer is not a valid integer for MC question
        var question = new MultipleChoiceQuestionEntity(
            _testQuestionId, _testMaterialId, "Test Question", 
            new List<string> { "A", "B" }, 0);

        _mockQuestionRepository.Setup(r => r.GetByIdAsync(_testQuestionId))
            .ReturnsAsync(question);

        var request = CreateValidMultipleChoiceRequest();
        request.Answer = "not-an-integer";

        // Act
        var result = await _controller.SubmitResponse(request);

        // Assert
        Assert.IsType<BadRequestObjectResult>(result);
    }

    [Fact]
    public async Task SubmitResponse_ServiceValidationFailure_Returns400BadRequest()
    {
        // Arrange
        var question = new MultipleChoiceQuestionEntity(
            _testQuestionId, _testMaterialId, "Test Question", 
            new List<string> { "A", "B" }, 0);

        _mockQuestionRepository.Setup(r => r.GetByIdAsync(_testQuestionId))
            .ReturnsAsync(question);
        _mockResponseService.Setup(s => s.CreateResponseAsync(It.IsAny<ResponseEntity>()))
            .ThrowsAsync(new InvalidOperationException("Index out of range"));

        var request = CreateValidMultipleChoiceRequest();
        request.Answer = "10"; // Out of range but valid integer

        // Act
        var result = await _controller.SubmitResponse(request);

        // Assert
        Assert.IsType<BadRequestObjectResult>(result);
    }

    #endregion

    #region POST /responses/batch Tests

    [Fact]
    public async Task SubmitBatchResponses_ValidBatch_Returns201Created()
    {
        // Arrange
        var question = new MultipleChoiceQuestionEntity(
            _testQuestionId, _testMaterialId, "Test Question", 
            new List<string> { "A", "B", "C" }, 0);

        _mockQuestionRepository.Setup(r => r.GetByIdAsync(_testQuestionId))
            .ReturnsAsync(question);
        _mockResponseService.Setup(s => s.CreateResponseAsync(It.IsAny<ResponseEntity>()))
            .ReturnsAsync((ResponseEntity r) => r);

        var request = new BatchResponseRequest
        {
            Responses = new List<ResponseRequest>
            {
                CreateValidMultipleChoiceRequest(),
                CreateValidMultipleChoiceRequest(Guid.NewGuid())
            }
        };

        // Act
        var result = await _controller.SubmitBatchResponses(request);

        // Assert
        var statusResult = Assert.IsType<ObjectResult>(result);
        Assert.Equal(201, statusResult.StatusCode);
        _mockResponseService.Verify(s => s.CreateResponseAsync(It.IsAny<ResponseEntity>()), Times.Exactly(2));
    }

    [Fact]
    public async Task SubmitBatchResponses_NullRequest_Returns400BadRequest()
    {
        // Act
        var result = await _controller.SubmitBatchResponses(null!);

        // Assert
        Assert.IsType<BadRequestObjectResult>(result);
    }

    [Fact]
    public async Task SubmitBatchResponses_EmptyBatch_Returns400BadRequest()
    {
        // Arrange
        var request = new BatchResponseRequest { Responses = new List<ResponseRequest>() };

        // Act
        var result = await _controller.SubmitBatchResponses(request);

        // Assert
        Assert.IsType<BadRequestObjectResult>(result);
    }

    [Fact]
    public async Task SubmitBatchResponses_OneInvalidResponse_RejectsEntireBatch()
    {
        // Arrange - second response has invalid ID format
        var request = new BatchResponseRequest
        {
            Responses = new List<ResponseRequest>
            {
                CreateValidMultipleChoiceRequest(),
                CreateValidMultipleChoiceRequest() // Will have invalid ID set below
            }
        };
        request.Responses[1].Id = "not-a-valid-guid";

        // Act
        var result = await _controller.SubmitBatchResponses(request);

        // Assert
        Assert.IsType<BadRequestObjectResult>(result);
        // Verify no responses were created
        _mockResponseService.Verify(
            s => s.CreateResponseAsync(It.IsAny<ResponseEntity>()), Times.Never);
    }

    [Fact]
    public async Task SubmitBatchResponses_InvalidResponseInMiddle_NothingStored()
    {
        // Arrange - per approved design: any invalid = entire batch rejected
        var question = new MultipleChoiceQuestionEntity(
            _testQuestionId, _testMaterialId, "Test Question", 
            new List<string> { "A", "B" }, 0);

        _mockQuestionRepository.Setup(r => r.GetByIdAsync(_testQuestionId))
            .ReturnsAsync(question);

        var request = new BatchResponseRequest
        {
            Responses = new List<ResponseRequest>
            {
                CreateValidMultipleChoiceRequest(),
                CreateValidMultipleChoiceRequest(Guid.NewGuid()),
                CreateValidMultipleChoiceRequest(Guid.NewGuid())
            }
        };
        // Invalidate middle response
        request.Responses[1].QuestionId = ""; 

        // Act
        var result = await _controller.SubmitBatchResponses(request);

        // Assert
        Assert.IsType<BadRequestObjectResult>(result);
        _mockResponseService.Verify(
            s => s.CreateResponseAsync(It.IsAny<ResponseEntity>()), Times.Never);
    }

    #endregion

    #region Helper Methods

    private ResponseRequest CreateValidMultipleChoiceRequest(Guid? responseId = null)
    {
        return new ResponseRequest
        {
            Id = (responseId ?? _testResponseId).ToString(),
            QuestionId = _testQuestionId.ToString(),
            MaterialId = _testMaterialId.ToString(),
            DeviceId = _testDeviceId.ToString(),
            Answer = "1",
            Timestamp = DateTime.UtcNow.ToString("O"),
            IsCorrect = null
        };
    }

    private ResponseRequest CreateValidWrittenAnswerRequest(Guid? responseId = null)
    {
        return new ResponseRequest
        {
            Id = (responseId ?? _testResponseId).ToString(),
            QuestionId = _testQuestionId.ToString(),
            MaterialId = _testMaterialId.ToString(),
            DeviceId = _testDeviceId.ToString(),
            Answer = "Sample written answer",
            Timestamp = DateTime.UtcNow.ToString("O"),
            IsCorrect = null
        };
    }

    #endregion
}
