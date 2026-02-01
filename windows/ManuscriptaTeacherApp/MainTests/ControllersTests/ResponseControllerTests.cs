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
    private readonly Mock<IQuestionRepository> _mockQuestionRepo;
    private readonly Mock<ILogger<ResponseController>> _mockLogger;
    private readonly ResponseController _controller;

    private readonly Guid _testQuestionId = Guid.NewGuid();
    private readonly Guid _testDeviceId = Guid.NewGuid();
    private readonly Guid _testMaterialId = Guid.NewGuid();

    public ResponseControllerTests()
    {
        _mockResponseService = new Mock<IResponseService>();
        _mockQuestionRepo = new Mock<IQuestionRepository>();
        _mockLogger = new Mock<ILogger<ResponseController>>();
        _controller = new ResponseController(
            _mockResponseService.Object,
            _mockQuestionRepo.Object,
            _mockLogger.Object);
    }

    #region Constructor Tests

    [Fact]
    public void Constructor_NullResponseService_ThrowsArgumentNullException()
    {
        Assert.Throws<ArgumentNullException>(() =>
            new ResponseController(null!, _mockQuestionRepo.Object, _mockLogger.Object));
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
            new ResponseController(_mockResponseService.Object, _mockQuestionRepo.Object, null!));
    }

    #endregion

    #region POST /responses Tests

    [Fact]
    public async Task SubmitResponse_NullRequest_Returns400BadRequest()
    {
        // Act
        var result = await _controller.SubmitResponse(null!);

        // Assert
        var badRequest = Assert.IsType<BadRequestObjectResult>(result);
        Assert.Equal(400, badRequest.StatusCode);
    }

    [Fact]
    public async Task SubmitResponse_InvalidIdFormat_Returns400BadRequest()
    {
        // Arrange
        var request = CreateValidSubmitResponseDto();
        request.Id = "not-a-guid";
        
        _mockResponseService.Setup(s => s.CreateResponseAsync(request))
            .ThrowsAsync(new FormatException("Id must be a valid GUID"));

        // Act
        var result = await _controller.SubmitResponse(request);

        // Assert
        var badRequest = Assert.IsType<BadRequestObjectResult>(result);
        Assert.Equal(400, badRequest.StatusCode);
    }

    [Fact]
    public async Task SubmitResponse_InvalidQuestionIdFormat_Returns400BadRequest()
    {
        // Arrange
        var request = CreateValidSubmitResponseDto();
        request.QuestionId = "invalid";
        
        _mockResponseService.Setup(s => s.CreateResponseAsync(request))
            .ThrowsAsync(new FormatException("QuestionId must be a valid GUID"));

        // Act
        var result = await _controller.SubmitResponse(request);

        // Assert
        var badRequest = Assert.IsType<BadRequestObjectResult>(result);
        Assert.Equal(400, badRequest.StatusCode);
    }

    [Fact]
    public async Task SubmitResponse_InvalidDeviceIdFormat_Returns400BadRequest()
    {
        // Arrange
        var request = CreateValidSubmitResponseDto();
        request.DeviceId = "invalid";
        
        _mockResponseService.Setup(s => s.CreateResponseAsync(request))
            .ThrowsAsync(new FormatException("DeviceId must be a valid GUID"));

        // Act
        var result = await _controller.SubmitResponse(request);

        // Assert
        var badRequest = Assert.IsType<BadRequestObjectResult>(result);
        Assert.Equal(400, badRequest.StatusCode);
    }

    [Fact]
    public async Task SubmitResponse_InvalidTimestampFormat_Returns400BadRequest()
    {
        // Arrange
        var request = CreateValidSubmitResponseDto();
        request.Timestamp = "not-a-timestamp";
        
        _mockResponseService.Setup(s => s.CreateResponseAsync(request))
            .ThrowsAsync(new FormatException("Timestamp must be a valid ISO 8601"));

        // Act
        var result = await _controller.SubmitResponse(request);

        // Assert
        var badRequest = Assert.IsType<BadRequestObjectResult>(result);
        Assert.Equal(400, badRequest.StatusCode);
    }

    [Fact]
    public async Task SubmitResponse_QuestionNotFound_Returns400BadRequest()
    {
        // Arrange
        var request = CreateValidSubmitResponseDto();
        _mockResponseService.Setup(s => s.CreateResponseAsync(request))
            .ThrowsAsync(new InvalidOperationException("Question with ID ... not found"));

        // Act
        var result = await _controller.SubmitResponse(request);

        // Assert
        var badRequest = Assert.IsType<BadRequestObjectResult>(result);
        Assert.Equal(400, badRequest.StatusCode);
    }

    [Fact]
    public async Task SubmitResponse_MultipleChoiceWithValidIndex_Returns201Created()
    {
        // Arrange
        var request = CreateValidSubmitResponseDto();
        var responseEntity = new MultipleChoiceResponseEntity(Guid.Parse(request.Id), _testQuestionId, _testDeviceId, 0, DateTime.UtcNow, null);

        _mockResponseService.Setup(s => s.CreateResponseAsync(request))
            .ReturnsAsync(responseEntity);
 
        // Act
        var result = await _controller.SubmitResponse(request);
 
        // Assert
        var statusResult = Assert.IsType<ObjectResult>(result);
        Assert.Equal(201, statusResult.StatusCode);
        _mockResponseService.Verify(s => s.CreateResponseAsync(request), Times.Once);
    }
 
    [Fact]
    public async Task SubmitResponse_ServiceValidationFailure_Returns400BadRequest()
    {
        // Arrange
        var request = CreateValidSubmitResponseDto();
        _mockResponseService.Setup(s => s.CreateResponseAsync(request))
            .ThrowsAsync(new InvalidOperationException("Validation failed"));
 
        // Act
        var result = await _controller.SubmitResponse(request);
 
        // Assert
        var badRequest = Assert.IsType<BadRequestObjectResult>(result);
        Assert.Equal(400, badRequest.StatusCode);
    }

    #endregion
 
    #region POST /responses/batch Tests
 
    [Fact]
    public async Task SubmitBatchResponses_NullRequest_Returns400BadRequest()
    {
        // Act
        var result = await _controller.SubmitBatchResponses(null!);
 
        // Assert
        var badRequest = Assert.IsType<BadRequestObjectResult>(result);
        Assert.Equal(400, badRequest.StatusCode);
    }
 
    [Fact]
    public async Task SubmitBatchResponses_EmptyResponses_Returns400BadRequest()
    {
        // Arrange
        var request = new BatchSubmitResponsesDto { Responses = new List<SubmitResponseDto>() };
 
        // Act
        var result = await _controller.SubmitBatchResponses(request);
 
        // Assert
        var badRequest = Assert.IsType<BadRequestObjectResult>(result);
        Assert.Equal(400, badRequest.StatusCode);
    }
 
    [Fact]
    public async Task SubmitBatchResponses_ValidResponses_Returns201Created()
    {
        // Arrange
        var request = new BatchSubmitResponsesDto
        {
            Responses = new List<SubmitResponseDto> { CreateValidSubmitResponseDto(), CreateValidSubmitResponseDto() }
        };

        _mockResponseService.Setup(s => s.CreateBatchResponsesAsync(request.Responses))
            .Returns(Task.CompletedTask);
 
        // Act
        var result = await _controller.SubmitBatchResponses(request);
 
        // Assert
        var statusResult = Assert.IsType<ObjectResult>(result);
        Assert.Equal(201, statusResult.StatusCode);
        _mockResponseService.Verify(s => s.CreateBatchResponsesAsync(request.Responses), Times.Once);
    }
 
    [Fact]
    public async Task SubmitBatchResponses_ServiceThrowsInvalidOperation_Returns400BadRequest()
    {
        // Arrange
        var request = new BatchSubmitResponsesDto
        {
            Responses = new List<SubmitResponseDto> { CreateValidSubmitResponseDto() }
        };

        _mockResponseService.Setup(s => s.CreateBatchResponsesAsync(request.Responses))
            .ThrowsAsync(new InvalidOperationException("Atomic validation failed"));
 
        // Act
        var result = await _controller.SubmitBatchResponses(request);
 
        // Assert
        var badRequest = Assert.IsType<BadRequestObjectResult>(result);
        Assert.Equal(400, badRequest.StatusCode);
    }

    #endregion

    #region Helpers

    private SubmitResponseDto CreateValidSubmitResponseDto()
    {
        return new SubmitResponseDto
        {
            Id = Guid.NewGuid().ToString(),
            QuestionId = _testQuestionId.ToString(),
            DeviceId = _testDeviceId.ToString(),
            Answer = "0",
            Timestamp = DateTime.UtcNow.ToString("O"),
            IsCorrect = null
        };
    }

    #endregion
}
