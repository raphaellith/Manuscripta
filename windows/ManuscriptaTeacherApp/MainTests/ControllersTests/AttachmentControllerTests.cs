using Microsoft.AspNetCore.Mvc;
using Microsoft.Extensions.Logging;
using Moq;
using Main.Controllers;
using Main.Models.Entities;
using Main.Services.Repositories;
using Xunit;

namespace MainTests.ControllersTests;

/// <summary>
/// Unit tests for AttachmentController.
/// Verifies GET /attachments/{id} endpoint per API Contract.md §2.1.3.
/// </summary>
public class AttachmentControllerTests
{
    private readonly Mock<IAttachmentRepository> _mockAttachmentRepo;
    private readonly Mock<ILogger<AttachmentController>> _mockLogger;
    private readonly AttachmentController _controller;

    private readonly Guid _testAttachmentId = Guid.NewGuid();
    private readonly Guid _testMaterialId = Guid.NewGuid();

    public AttachmentControllerTests()
    {
        _mockAttachmentRepo = new Mock<IAttachmentRepository>();
        _mockLogger = new Mock<ILogger<AttachmentController>>();
        _controller = new AttachmentController(
            _mockAttachmentRepo.Object,
            _mockLogger.Object);
    }

    #region Constructor Tests

    [Fact]
    public void Constructor_NullAttachmentRepository_ThrowsArgumentNullException()
    {
        Assert.Throws<ArgumentNullException>(() =>
            new AttachmentController(null!, _mockLogger.Object));
    }

    [Fact]
    public void Constructor_NullLogger_ThrowsArgumentNullException()
    {
        Assert.Throws<ArgumentNullException>(() =>
            new AttachmentController(_mockAttachmentRepo.Object, null!));
    }

    #endregion

    #region GET /attachments/{id} Tests

    [Fact]
    public async Task GetAttachment_EmptyId_Returns400BadRequest()
    {
        // Act
        var result = await _controller.GetAttachment("");

        // Assert
        var badRequest = Assert.IsType<BadRequestObjectResult>(result);
        Assert.Equal(400, badRequest.StatusCode);
    }

    [Fact]
    public async Task GetAttachment_WhitespaceId_Returns400BadRequest()
    {
        // Act
        var result = await _controller.GetAttachment("   ");

        // Assert
        var badRequest = Assert.IsType<BadRequestObjectResult>(result);
        Assert.Equal(400, badRequest.StatusCode);
    }

    [Fact]
    public async Task GetAttachment_InvalidGuidFormat_Returns400BadRequest()
    {
        // Act
        var result = await _controller.GetAttachment("not-a-guid");

        // Assert
        var badRequest = Assert.IsType<BadRequestObjectResult>(result);
        Assert.Equal(400, badRequest.StatusCode);
    }

    [Fact]
    public async Task GetAttachment_AttachmentNotFound_Returns404NotFound()
    {
        // Arrange
        _mockAttachmentRepo.Setup(r => r.GetByIdAsync(_testAttachmentId))
            .ReturnsAsync((AttachmentEntity?)null);

        // Act
        var result = await _controller.GetAttachment(_testAttachmentId.ToString());

        // Assert
        var notFound = Assert.IsType<NotFoundObjectResult>(result);
        Assert.Equal(404, notFound.StatusCode);
    }

    [Fact]
    public async Task GetAttachment_FileNotOnDisk_Returns404NotFound()
    {
        // Arrange - Entity exists but file doesn't
        var attachment = new AttachmentEntity(
            _testAttachmentId,
            _testMaterialId,
            "TestImage",
            "png");

        _mockAttachmentRepo.Setup(r => r.GetByIdAsync(_testAttachmentId))
            .ReturnsAsync(attachment);

        // Act
        var result = await _controller.GetAttachment(_testAttachmentId.ToString());

        // Assert - File won't exist on disk during unit tests
        var notFound = Assert.IsType<NotFoundObjectResult>(result);
        Assert.Equal(404, notFound.StatusCode);
    }

    [Theory]
    [InlineData("png")]
    [InlineData("jpeg")]
    [InlineData("pdf")]
    public async Task GetAttachment_ValidAttachmentId_LooksUpEntity(string extension)
    {
        // Arrange
        var attachment = new AttachmentEntity(
            _testAttachmentId,
            _testMaterialId,
            "TestFile",
            extension);

        _mockAttachmentRepo.Setup(r => r.GetByIdAsync(_testAttachmentId))
            .ReturnsAsync(attachment);

        // Act
        await _controller.GetAttachment(_testAttachmentId.ToString());

        // Assert - Verify repository was called
        _mockAttachmentRepo.Verify(r => r.GetByIdAsync(_testAttachmentId), Times.Once);
    }

    #endregion
}
