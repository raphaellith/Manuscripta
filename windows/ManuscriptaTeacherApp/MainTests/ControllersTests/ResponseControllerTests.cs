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
        _mockQuestionRepo.Setup(r => r.GetByIdAsync(It.IsAny<Guid>()))
            .ReturnsAsync((QuestionEntity?)null);

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
        var question = new MultipleChoiceQuestionEntity(
            _testQuestionId, _testMaterialId, "Test?", new List<string> { "A", "B" }, 0);
        
        _mockQuestionRepo.Setup(r => r.GetByIdAsync(_testQuestionId))
            .ReturnsAsync(question);
        _mockResponseService.Setup(s => s.CreateResponseAsync(It.IsAny<ResponseEntity>()))
            .ReturnsAsync((ResponseEntity r) => r);

        // Act
        var result = await _controller.SubmitResponse(request);

        // Assert
        var statusResult = Assert.IsType<ObjectResult>(result);
        Assert.Equal(201, statusResult.StatusCode);
    }

    [Fact]
    public async Task SubmitResponse_WrittenAnswer_Returns201Created()
    {
        // Arrange
        var request = CreateValidSubmitResponseDto();
        request.Answer = "My written answer";
        var question = new WrittenAnswerQuestionEntity(
            _testQuestionId, _testMaterialId, "Explain?", "Sample answer");
        
        _mockQuestionRepo.Setup(r => r.GetByIdAsync(_testQuestionId))
            .ReturnsAsync(question);
        _mockResponseService.Setup(s => s.CreateResponseAsync(It.IsAny<ResponseEntity>()))
            .ReturnsAsync((ResponseEntity r) => r);

        // Act
        var result = await _controller.SubmitResponse(request);

        // Assert
        var statusResult = Assert.IsType<ObjectResult>(result);
        Assert.Equal(201, statusResult.StatusCode);
    }

    [Fact]
    public async Task SubmitResponse_MultipleChoiceInvalidAnswerFormat_Returns400BadRequest()
    {
        // Arrange - Answer must be an integer index for MC questions
        var request = CreateValidSubmitResponseDto();
        request.Answer = "not-a-number";
        var question = new MultipleChoiceQuestionEntity(
            _testQuestionId, _testMaterialId, "Test?", new List<string> { "A", "B" }, 0);
        
        _mockQuestionRepo.Setup(r => r.GetByIdAsync(_testQuestionId))
            .ReturnsAsync(question);

        // Act
        var result = await _controller.SubmitResponse(request);

        // Assert
        var badRequest = Assert.IsType<BadRequestObjectResult>(result);
        Assert.Equal(400, badRequest.StatusCode);
    }

    [Fact]
    public async Task SubmitResponse_ServiceValidationFailure_Returns400BadRequest()
    {
        // Arrange - Per Validation Rules §1A(3): Service throws, controller returns 400
        var request = CreateValidSubmitResponseDto();
        var question = new MultipleChoiceQuestionEntity(
            _testQuestionId, _testMaterialId, "Test?", new List<string> { "A", "B" }, 0);
        
        _mockQuestionRepo.Setup(r => r.GetByIdAsync(_testQuestionId))
            .ReturnsAsync(question);
        _mockResponseService.Setup(s => s.CreateResponseAsync(It.IsAny<ResponseEntity>()))
            .ThrowsAsync(new InvalidOperationException("Device not paired"));

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
        var question = new MultipleChoiceQuestionEntity(
            _testQuestionId, _testMaterialId, "Test?", new List<string> { "A", "B" }, 0);
        
        _mockQuestionRepo.Setup(r => r.GetByIdAsync(_testQuestionId))
            .ReturnsAsync(question);
        _mockResponseService.Setup(s => s.CreateResponseAsync(It.IsAny<ResponseEntity>()))
            .ReturnsAsync((ResponseEntity r) => r);

        var request = new BatchSubmitResponsesDto
        {
            Responses = new List<SubmitResponseDto> { CreateValidSubmitResponseDto(), CreateValidSubmitResponseDto() }
        };

        // Act
        var result = await _controller.SubmitBatchResponses(request);

        // Assert
        var statusResult = Assert.IsType<ObjectResult>(result);
        Assert.Equal(201, statusResult.StatusCode);
    }

    [Fact]
    public async Task SubmitBatchResponses_OneInvalidResponse_Returns400BadRequest()
    {
        // Arrange
        var validRequest = CreateValidSubmitResponseDto();
        var invalidRequest = CreateValidSubmitResponseDto();
        invalidRequest.Id = "invalid-guid";

        var request = new BatchSubmitResponsesDto
        {
            Responses = new List<SubmitResponseDto> { validRequest, invalidRequest }
        };

        var question = new MultipleChoiceQuestionEntity(
            _testQuestionId, _testMaterialId, "Test?", new List<string> { "A", "B" }, 0);
        
        _mockQuestionRepo.Setup(r => r.GetByIdAsync(_testQuestionId))
            .ReturnsAsync(question);
        _mockResponseService.Setup(s => s.CreateResponseAsync(It.IsAny<ResponseEntity>()))
            .ReturnsAsync((ResponseEntity r) => r);

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
