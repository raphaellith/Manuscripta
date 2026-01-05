using Microsoft.AspNetCore.Mvc;
using Microsoft.Extensions.Logging;
using Moq;
using Main.Controllers;
using Main.Models.Entities;
using Main.Models.Entities.Responses;
using Main.Services.Repositories;
using Xunit;

namespace MainTests.ControllersTests;

/// <summary>
/// Unit tests for FeedbackController.
/// Verifies GET /feedback/{deviceId} endpoint per API Contract.md §2.6.
/// </summary>
public class FeedbackControllerTests
{
    private readonly Mock<IFeedbackRepository> _mockFeedbackRepository;
    private readonly Mock<IResponseRepository> _mockResponseRepository;
    private readonly Mock<ILogger<FeedbackController>> _mockLogger;
    private readonly FeedbackController _controller;

    private readonly Guid _testDeviceId = Guid.NewGuid();
    private readonly Guid _testResponseId = Guid.NewGuid();
    private readonly Guid _testQuestionId = Guid.NewGuid();

    public FeedbackControllerTests()
    {
        _mockFeedbackRepository = new Mock<IFeedbackRepository>();
        _mockResponseRepository = new Mock<IResponseRepository>();
        _mockLogger = new Mock<ILogger<FeedbackController>>();
        _controller = new FeedbackController(
            _mockFeedbackRepository.Object,
            _mockResponseRepository.Object,
            _mockLogger.Object);
    }

    #region Constructor Tests

    [Fact]
    public void Constructor_NullFeedbackRepository_ThrowsArgumentNullException()
    {
        Assert.Throws<ArgumentNullException>(() =>
            new FeedbackController(null!, _mockResponseRepository.Object, _mockLogger.Object));
    }

    [Fact]
    public void Constructor_NullResponseRepository_ThrowsArgumentNullException()
    {
        Assert.Throws<ArgumentNullException>(() =>
            new FeedbackController(_mockFeedbackRepository.Object, null!, _mockLogger.Object));
    }

    [Fact]
    public void Constructor_NullLogger_ThrowsArgumentNullException()
    {
        Assert.Throws<ArgumentNullException>(() =>
            new FeedbackController(_mockFeedbackRepository.Object, _mockResponseRepository.Object, null!));
    }

    #endregion

    #region GET /feedback/{deviceId} Tests

    [Fact]
    public async Task GetFeedback_ValidDeviceId_Returns200OK()
    {
        // Arrange
        var response = new WrittenAnswerResponseEntity(_testResponseId, _testQuestionId, _testDeviceId, "Answer");
        var feedback = new FeedbackEntity(Guid.NewGuid(), _testResponseId, text: "Good work!");

        _mockFeedbackRepository.Setup(r => r.GetAllAsync())
            .ReturnsAsync(new[] { feedback });
        _mockResponseRepository.Setup(r => r.GetByIdAsync(_testResponseId))
            .ReturnsAsync(response);

        // Act
        var result = await _controller.GetFeedback(_testDeviceId.ToString());

        // Assert
        var okResult = Assert.IsType<OkObjectResult>(result);
        Assert.Equal(200, okResult.StatusCode);
    }

    [Fact]
    public async Task GetFeedback_ValidDeviceId_ReturnsFeedbackArray()
    {
        // Arrange
        var response = new WrittenAnswerResponseEntity(_testResponseId, _testQuestionId, _testDeviceId, "Answer");
        var feedback = new FeedbackEntity(Guid.NewGuid(), _testResponseId, text: "Good work!");

        _mockFeedbackRepository.Setup(r => r.GetAllAsync())
            .ReturnsAsync(new[] { feedback });
        _mockResponseRepository.Setup(r => r.GetByIdAsync(_testResponseId))
            .ReturnsAsync(response);

        // Act
        var result = await _controller.GetFeedback(_testDeviceId.ToString());

        // Assert - Per API Contract §2.6: Response has "feedback" property
        var okResult = Assert.IsType<OkObjectResult>(result);
        Assert.NotNull(okResult.Value);

        var valueType = okResult.Value.GetType();
        var feedbackProperty = valueType.GetProperty("feedback");
        Assert.NotNull(feedbackProperty);
    }

    [Fact]
    public async Task GetFeedback_NoFeedbackAvailable_Returns404NotFound()
    {
        // Arrange - Per API Contract §2.6: 404 if no feedback available
        _mockFeedbackRepository.Setup(r => r.GetAllAsync())
            .ReturnsAsync(Array.Empty<FeedbackEntity>());

        // Act
        var result = await _controller.GetFeedback(_testDeviceId.ToString());

        // Assert
        Assert.IsType<NotFoundObjectResult>(result);
    }

    [Fact]
    public async Task GetFeedback_FeedbackForOtherDevice_Returns404NotFound()
    {
        // Arrange - Feedback exists but for different device
        var otherDeviceId = Guid.NewGuid();
        var response = new WrittenAnswerResponseEntity(_testResponseId, _testQuestionId, otherDeviceId, "Answer");
        var feedback = new FeedbackEntity(Guid.NewGuid(), _testResponseId, text: "Good work!");

        _mockFeedbackRepository.Setup(r => r.GetAllAsync())
            .ReturnsAsync(new[] { feedback });
        _mockResponseRepository.Setup(r => r.GetByIdAsync(_testResponseId))
            .ReturnsAsync(response);

        // Act
        var result = await _controller.GetFeedback(_testDeviceId.ToString());

        // Assert
        Assert.IsType<NotFoundObjectResult>(result);
    }

    [Fact]
    public async Task GetFeedback_EmptyDeviceId_Returns400BadRequest()
    {
        // Act
        var result = await _controller.GetFeedback("");

        // Assert
        Assert.IsType<BadRequestObjectResult>(result);
    }

    [Fact]
    public async Task GetFeedback_WhitespaceDeviceId_Returns400BadRequest()
    {
        // Act
        var result = await _controller.GetFeedback("   ");

        // Assert
        Assert.IsType<BadRequestObjectResult>(result);
    }

    [Fact]
    public async Task GetFeedback_InvalidGuidFormat_Returns400BadRequest()
    {
        // Act
        var result = await _controller.GetFeedback("not-a-valid-guid");

        // Assert
        Assert.IsType<BadRequestObjectResult>(result);
    }

    [Fact]
    public async Task GetFeedback_InvalidGuidFormat_ReturnsErrorMessage()
    {
        // Act
        var result = await _controller.GetFeedback("invalid-guid") as BadRequestObjectResult;

        // Assert
        Assert.NotNull(result);
        var value = result.Value?.ToString();
        Assert.Contains("GUID", value, StringComparison.OrdinalIgnoreCase);
    }

    [Fact]
    public async Task GetFeedback_RepositoryThrowsException_Returns500()
    {
        // Arrange
        _mockFeedbackRepository.Setup(r => r.GetAllAsync())
            .ThrowsAsync(new Exception("Test exception"));

        // Act
        var result = await _controller.GetFeedback(_testDeviceId.ToString());

        // Assert
        var statusResult = Assert.IsType<ObjectResult>(result);
        Assert.Equal(500, statusResult.StatusCode);
    }

    [Fact]
    public async Task GetFeedback_MultipleFeedback_ReturnsAll()
    {
        // Arrange
        var response1 = new WrittenAnswerResponseEntity(Guid.NewGuid(), _testQuestionId, _testDeviceId, "Answer 1");
        var response2 = new WrittenAnswerResponseEntity(Guid.NewGuid(), _testQuestionId, _testDeviceId, "Answer 2");
        
        var feedback1 = new FeedbackEntity(Guid.NewGuid(), response1.Id, text: "Good!");
        var feedback2 = new FeedbackEntity(Guid.NewGuid(), response2.Id, marks: 8);

        _mockFeedbackRepository.Setup(r => r.GetAllAsync())
            .ReturnsAsync(new[] { feedback1, feedback2 });
        _mockResponseRepository.Setup(r => r.GetByIdAsync(response1.Id))
            .ReturnsAsync(response1);
        _mockResponseRepository.Setup(r => r.GetByIdAsync(response2.Id))
            .ReturnsAsync(response2);

        // Act
        var result = await _controller.GetFeedback(_testDeviceId.ToString());

        // Assert
        var okResult = Assert.IsType<OkObjectResult>(result);
        Assert.NotNull(okResult.Value);
    }

    #endregion
}
