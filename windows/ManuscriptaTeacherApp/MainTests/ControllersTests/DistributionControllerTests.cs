using Microsoft.AspNetCore.Mvc;
using Microsoft.Extensions.Logging;
using Moq;
using Main.Controllers;
using Main.Models.Entities.Materials;
using Main.Models.Entities.Questions;
using Main.Services;
using Xunit;

namespace MainTests.ControllersTests;

/// <summary>
/// Unit tests for DistributionController.
/// Verifies GET /distribution/{deviceId} endpoint per API Contract.md §2.5.
/// </summary>
public class DistributionControllerTests
{
    private readonly Mock<IDistributionService> _mockDistributionService;
    private readonly Mock<ILogger<DistributionController>> _mockLogger;
    private readonly DistributionController _controller;

    private readonly Guid _testDeviceId = Guid.NewGuid();
    private readonly Guid _testMaterialId = Guid.NewGuid();
    private readonly Guid _testLessonId = Guid.NewGuid();

    public DistributionControllerTests()
    {
        _mockDistributionService = new Mock<IDistributionService>();
        _mockLogger = new Mock<ILogger<DistributionController>>();
        _controller = new DistributionController(_mockDistributionService.Object, _mockLogger.Object);
    }

    #region Constructor Tests

    [Fact]
    public void Constructor_NullDistributionService_ThrowsArgumentNullException()
    {
        Assert.Throws<ArgumentNullException>(() =>
            new DistributionController(null!, _mockLogger.Object));
    }

    [Fact]
    public void Constructor_NullLogger_ThrowsArgumentNullException()
    {
        Assert.Throws<ArgumentNullException>(() =>
            new DistributionController(_mockDistributionService.Object, null!));
    }

    #endregion

    #region GET /distribution/{deviceId} Tests

    [Fact]
    public async Task GetDistribution_ValidDeviceId_Returns200OK()
    {
        // Arrange
        var material = new ReadingMaterialEntity(_testMaterialId, _testLessonId, "Test Material", "Test Content");

        var questions = new List<QuestionEntity>
        {
            new MultipleChoiceQuestionEntity(Guid.NewGuid(), _testMaterialId, "Is this a test?", new List<string> { "Yes", "No" }, 0)
        };

        var bundle = new DistributionBundle(new[] { material }, questions);
        _mockDistributionService.Setup(x => x.GetDistributionBundleAsync(_testDeviceId))
            .ReturnsAsync(bundle);

        // Act
        var result = await _controller.GetDistribution(_testDeviceId.ToString());

        // Assert
        var okResult = Assert.IsType<OkObjectResult>(result);
        Assert.Equal(200, okResult.StatusCode);
    }

    [Fact]
    public async Task GetDistribution_ValidDeviceId_ReturnsMaterialsAndQuestions()
    {
        // Arrange
        var material = new ReadingMaterialEntity(_testMaterialId, _testLessonId, "Test Material", "Test Content");

        var questions = new List<QuestionEntity>
        {
            new MultipleChoiceQuestionEntity(Guid.NewGuid(), _testMaterialId, "Is this a test?", new List<string> { "Yes", "No" }, 0)
        };

        var bundle = new DistributionBundle(new[] { material }, questions);
        _mockDistributionService.Setup(x => x.GetDistributionBundleAsync(_testDeviceId))
            .ReturnsAsync(bundle);

        // Act
        var result = await _controller.GetDistribution(_testDeviceId.ToString());

        // Assert
        var okResult = Assert.IsType<OkObjectResult>(result);
        Assert.NotNull(okResult.Value);
        
        // Verify the response has materials and questions properties
        var valueType = okResult.Value.GetType();
        var materialsProperty = valueType.GetProperty("materials");
        var questionsProperty = valueType.GetProperty("questions");
        
        Assert.NotNull(materialsProperty);
        Assert.NotNull(questionsProperty);
    }

    [Fact]
    public async Task GetDistribution_NoMaterialsAvailable_Returns404NotFound()
    {
        // Arrange - Per API Contract §2.5: 404 if no materials available
        _mockDistributionService.Setup(x => x.GetDistributionBundleAsync(_testDeviceId))
            .ReturnsAsync((DistributionBundle?)null);

        // Act
        var result = await _controller.GetDistribution(_testDeviceId.ToString());

        // Assert
        Assert.IsType<NotFoundObjectResult>(result);
    }

    [Fact]
    public async Task GetDistribution_EmptyDeviceId_Returns400BadRequest()
    {
        // Act
        var result = await _controller.GetDistribution("");

        // Assert
        Assert.IsType<BadRequestObjectResult>(result);
    }

    [Fact]
    public async Task GetDistribution_WhitespaceDeviceId_Returns400BadRequest()
    {
        // Act
        var result = await _controller.GetDistribution("   ");

        // Assert
        Assert.IsType<BadRequestObjectResult>(result);
    }

    [Fact]
    public async Task GetDistribution_InvalidGuidFormat_Returns400BadRequest()
    {
        // Act
        var result = await _controller.GetDistribution("not-a-valid-guid");

        // Assert
        Assert.IsType<BadRequestObjectResult>(result);
    }

    [Fact]
    public async Task GetDistribution_InvalidGuidFormat_ReturnsErrorMessage()
    {
        // Act
        var result = await _controller.GetDistribution("invalid-guid") as BadRequestObjectResult;

        // Assert
        Assert.NotNull(result);
        var value = result.Value?.ToString();
        Assert.Contains("GUID", value, StringComparison.OrdinalIgnoreCase);
    }

    [Fact]
    public async Task GetDistribution_ServiceThrowsException_Returns500()
    {
        // Arrange
        _mockDistributionService.Setup(x => x.GetDistributionBundleAsync(_testDeviceId))
            .ThrowsAsync(new Exception("Test exception"));

        // Act
        var result = await _controller.GetDistribution(_testDeviceId.ToString());

        // Assert
        var statusResult = Assert.IsType<ObjectResult>(result);
        Assert.Equal(500, statusResult.StatusCode);
    }

    [Fact]
    public async Task GetDistribution_MultipleMaterials_ReturnsAll()
    {
        // Arrange
        var material1 = new ReadingMaterialEntity(Guid.NewGuid(), _testLessonId, "Material 1", "Content 1");
        var material2 = new ReadingMaterialEntity(Guid.NewGuid(), _testLessonId, "Material 2", "Content 2");

        var bundle = new DistributionBundle(new[] { material1, material2 }, new List<QuestionEntity>());
        _mockDistributionService.Setup(x => x.GetDistributionBundleAsync(_testDeviceId))
            .ReturnsAsync(bundle);

        // Act
        var result = await _controller.GetDistribution(_testDeviceId.ToString());

        // Assert
        var okResult = Assert.IsType<OkObjectResult>(result);
        Assert.NotNull(okResult.Value);
    }

    #endregion
}
