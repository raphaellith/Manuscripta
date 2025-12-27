using Main.Models.Entities.Materials;
using Main.Models.Entities.Questions;
using Main.Models.Enums;
using Main.Services;
using Main.Services.Repositories;
using Moq;
using Xunit;

namespace MainTests.ServicesTests;

/// <summary>
/// Unit tests for DistributionService.
/// Verifies distribution bundle logic per API Contract.md §2.5.
/// </summary>
public class DistributionServiceTests
{
    private readonly Mock<IMaterialRepository> _mockMaterialRepo;
    private readonly Mock<IQuestionRepository> _mockQuestionRepo;
    private readonly Mock<IDeviceRegistryService> _mockDeviceRegistry;
    private readonly DistributionService _service;

    private readonly Guid _testDeviceId = Guid.NewGuid();
    private readonly Guid _testMaterialId = Guid.NewGuid();
    private readonly Guid _testLessonId = Guid.NewGuid();

    public DistributionServiceTests()
    {
        _mockMaterialRepo = new Mock<IMaterialRepository>();
        _mockQuestionRepo = new Mock<IQuestionRepository>();
        _mockDeviceRegistry = new Mock<IDeviceRegistryService>();

        _service = new DistributionService(
            _mockMaterialRepo.Object,
            _mockQuestionRepo.Object,
            _mockDeviceRegistry.Object);
    }

    #region Constructor Tests

    [Fact]
    public void Constructor_NullMaterialRepository_ThrowsArgumentNullException()
    {
        Assert.Throws<ArgumentNullException>(() =>
            new DistributionService(null!, _mockQuestionRepo.Object, _mockDeviceRegistry.Object));
    }

    [Fact]
    public void Constructor_NullQuestionRepository_ThrowsArgumentNullException()
    {
        Assert.Throws<ArgumentNullException>(() =>
            new DistributionService(_mockMaterialRepo.Object, null!, _mockDeviceRegistry.Object));
    }

    [Fact]
    public void Constructor_NullDeviceRegistry_ThrowsArgumentNullException()
    {
        Assert.Throws<ArgumentNullException>(() =>
            new DistributionService(_mockMaterialRepo.Object, _mockQuestionRepo.Object, null!));
    }

    #endregion

    #region GetDistributionBundleAsync Tests

    [Fact]
    public async Task GetDistributionBundleAsync_UnpairedDevice_ReturnsNull()
    {
        // Arrange
        _mockDeviceRegistry.Setup(x => x.IsDevicePairedAsync(_testDeviceId))
            .ReturnsAsync(false);

        // Act
        var result = await _service.GetDistributionBundleAsync(_testDeviceId);

        // Assert
        Assert.Null(result);
    }

    [Fact]
    public async Task GetDistributionBundleAsync_NoAssignedMaterials_ReturnsNull()
    {
        // Arrange
        _mockDeviceRegistry.Setup(x => x.IsDevicePairedAsync(_testDeviceId))
            .ReturnsAsync(true);
        // No materials assigned - default state

        // Act
        var result = await _service.GetDistributionBundleAsync(_testDeviceId);

        // Assert
        Assert.Null(result);
    }

    [Fact]
    public async Task GetDistributionBundleAsync_WithAssignedMaterials_ReturnsBundle()
    {
        // Arrange
        _mockDeviceRegistry.Setup(x => x.IsDevicePairedAsync(_testDeviceId))
            .ReturnsAsync(true);

        var material = new ReadingMaterialEntity(
            _testMaterialId,
            _testLessonId,
            "Test Material",
            "Test Content");

        _mockMaterialRepo.Setup(x => x.GetByIdAsync(_testMaterialId))
            .ReturnsAsync(material);

        var questions = new List<QuestionEntity>
        {
            new TrueFalseQuestionEntity(
                Guid.NewGuid(),
                _testMaterialId,
                "Is this a test?",
                true)
        };

        _mockQuestionRepo.Setup(x => x.GetByMaterialIdAsync(_testMaterialId))
            .ReturnsAsync(questions);

        // Assign material to device
        await _service.AssignMaterialsToDeviceAsync(_testDeviceId, new[] { _testMaterialId });

        // Act
        var result = await _service.GetDistributionBundleAsync(_testDeviceId);

        // Assert
        Assert.NotNull(result);
        Assert.Single(result.Materials);
        Assert.Single(result.Questions);
    }

    [Fact]
    public async Task GetDistributionBundleAsync_MaterialNotFound_ReturnsNull()
    {
        // Arrange
        _mockDeviceRegistry.Setup(x => x.IsDevicePairedAsync(_testDeviceId))
            .ReturnsAsync(true);

        _mockMaterialRepo.Setup(x => x.GetByIdAsync(_testMaterialId))
            .ReturnsAsync((MaterialEntity?)null);

        await _service.AssignMaterialsToDeviceAsync(_testDeviceId, new[] { _testMaterialId });

        // Act
        var result = await _service.GetDistributionBundleAsync(_testDeviceId);

        // Assert
        Assert.Null(result);
    }

    #endregion

    #region AssignMaterialsToDeviceAsync Tests

    [Fact]
    public async Task AssignMaterialsToDeviceAsync_NullMaterialIds_ThrowsArgumentNullException()
    {
        await Assert.ThrowsAsync<ArgumentNullException>(() =>
            _service.AssignMaterialsToDeviceAsync(_testDeviceId, null!));
    }

    [Fact]
    public async Task AssignMaterialsToDeviceAsync_AddsMaterialsToDevice()
    {
        // Arrange
        var materialId1 = Guid.NewGuid();
        var materialId2 = Guid.NewGuid();

        _mockDeviceRegistry.Setup(x => x.IsDevicePairedAsync(_testDeviceId))
            .ReturnsAsync(true);

        var material1 = new ReadingMaterialEntity(materialId1, _testLessonId, "Material 1", "Content 1");
        var material2 = new ReadingMaterialEntity(materialId2, _testLessonId, "Material 2", "Content 2");

        _mockMaterialRepo.Setup(x => x.GetByIdAsync(materialId1)).ReturnsAsync(material1);
        _mockMaterialRepo.Setup(x => x.GetByIdAsync(materialId2)).ReturnsAsync(material2);
        _mockQuestionRepo.Setup(x => x.GetByMaterialIdAsync(It.IsAny<Guid>()))
            .ReturnsAsync(new List<QuestionEntity>());

        // Act
        await _service.AssignMaterialsToDeviceAsync(_testDeviceId, new[] { materialId1, materialId2 });
        var bundle = await _service.GetDistributionBundleAsync(_testDeviceId);

        // Assert
        Assert.NotNull(bundle);
        Assert.Equal(2, bundle.Materials.Count());
    }

    #endregion

    #region ClearDeviceAssignmentsAsync Tests

    [Fact]
    public async Task ClearDeviceAssignmentsAsync_ClearsAllAssignments()
    {
        // Arrange
        _mockDeviceRegistry.Setup(x => x.IsDevicePairedAsync(_testDeviceId))
            .ReturnsAsync(true);

        var material = new ReadingMaterialEntity(_testMaterialId, _testLessonId, "Test", "Test");

        _mockMaterialRepo.Setup(x => x.GetByIdAsync(_testMaterialId)).ReturnsAsync(material);
        _mockQuestionRepo.Setup(x => x.GetByMaterialIdAsync(_testMaterialId))
            .ReturnsAsync(new List<QuestionEntity>());

        await _service.AssignMaterialsToDeviceAsync(_testDeviceId, new[] { _testMaterialId });

        // Verify assignment exists
        var bundleBefore = await _service.GetDistributionBundleAsync(_testDeviceId);
        Assert.NotNull(bundleBefore);

        // Act
        await _service.ClearDeviceAssignmentsAsync(_testDeviceId);

        // Assert
        var bundleAfter = await _service.GetDistributionBundleAsync(_testDeviceId);
        Assert.Null(bundleAfter);
    }

    #endregion
}
