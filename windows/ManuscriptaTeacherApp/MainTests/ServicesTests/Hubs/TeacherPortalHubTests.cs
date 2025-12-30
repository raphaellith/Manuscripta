using System;
using System.Collections.Generic;
using System.Threading.Tasks;
using Moq;
using Xunit;
using Main.Models.Dtos;
using Main.Models.Entities;
using Main.Models.Entities.Materials;
using Main.Models.Enums;
using Main.Services;
using Main.Services.Hubs;
using Main.Services.Repositories;

namespace MainTests.ServicesTests.Hubs;

/// <summary>
/// Tests for TeacherPortalHub.
/// Verifies CRUD operations per NetworkingAPISpec §1(1)(a)-(d).
/// </summary>
public class TeacherPortalHubTests
{
    private readonly Mock<IUnitCollectionService> _mockUnitCollectionService;
    private readonly Mock<IUnitService> _mockUnitService;
    private readonly Mock<ILessonService> _mockLessonService;
    private readonly Mock<IMaterialService> _mockMaterialService;
    private readonly Mock<IUnitCollectionRepository> _mockUnitCollectionRepository;
    private readonly Mock<IUnitRepository> _mockUnitRepository;
    private readonly Mock<ILessonRepository> _mockLessonRepository;
    private readonly Mock<IMaterialRepository> _mockMaterialRepository;
    private readonly TeacherPortalHub _hub;

    public TeacherPortalHubTests()
    {
        _mockUnitCollectionService = new Mock<IUnitCollectionService>();
        _mockUnitService = new Mock<IUnitService>();
        _mockLessonService = new Mock<ILessonService>();
        _mockMaterialService = new Mock<IMaterialService>();
        _mockUnitCollectionRepository = new Mock<IUnitCollectionRepository>();
        _mockUnitRepository = new Mock<IUnitRepository>();
        _mockLessonRepository = new Mock<ILessonRepository>();
        _mockMaterialRepository = new Mock<IMaterialRepository>();

        _hub = new TeacherPortalHub(
            _mockUnitCollectionService.Object,
            _mockUnitService.Object,
            _mockLessonService.Object,
            _mockMaterialService.Object,
            _mockUnitCollectionRepository.Object,
            _mockUnitRepository.Object,
            _mockLessonRepository.Object,
            _mockMaterialRepository.Object);
    }

    #region Constructor Tests

    [Fact]
    public void Constructor_NullUnitCollectionService_ThrowsArgumentNullException()
    {
        Assert.Throws<ArgumentNullException>(() => new TeacherPortalHub(
            null!,
            _mockUnitService.Object,
            _mockLessonService.Object,
            _mockMaterialService.Object,
            _mockUnitCollectionRepository.Object,
            _mockUnitRepository.Object,
            _mockLessonRepository.Object,
            _mockMaterialRepository.Object));
    }

    [Fact]
    public void Constructor_NullUnitService_ThrowsArgumentNullException()
    {
        Assert.Throws<ArgumentNullException>(() => new TeacherPortalHub(
            _mockUnitCollectionService.Object,
            null!,
            _mockLessonService.Object,
            _mockMaterialService.Object,
            _mockUnitCollectionRepository.Object,
            _mockUnitRepository.Object,
            _mockLessonRepository.Object,
            _mockMaterialRepository.Object));
    }

    [Fact]
    public void Constructor_NullLessonService_ThrowsArgumentNullException()
    {
        Assert.Throws<ArgumentNullException>(() => new TeacherPortalHub(
            _mockUnitCollectionService.Object,
            _mockUnitService.Object,
            null!,
            _mockMaterialService.Object,
            _mockUnitCollectionRepository.Object,
            _mockUnitRepository.Object,
            _mockLessonRepository.Object,
            _mockMaterialRepository.Object));
    }

    [Fact]
    public void Constructor_NullMaterialService_ThrowsArgumentNullException()
    {
        Assert.Throws<ArgumentNullException>(() => new TeacherPortalHub(
            _mockUnitCollectionService.Object,
            _mockUnitService.Object,
            _mockLessonService.Object,
            null!,
            _mockUnitCollectionRepository.Object,
            _mockUnitRepository.Object,
            _mockLessonRepository.Object,
            _mockMaterialRepository.Object));
    }

    [Fact]
    public void Constructor_NullUnitCollectionRepository_ThrowsArgumentNullException()
    {
        Assert.Throws<ArgumentNullException>(() => new TeacherPortalHub(
            _mockUnitCollectionService.Object,
            _mockUnitService.Object,
            _mockLessonService.Object,
            _mockMaterialService.Object,
            null!,
            _mockUnitRepository.Object,
            _mockLessonRepository.Object,
            _mockMaterialRepository.Object));
    }

    [Fact]
    public void Constructor_NullUnitRepository_ThrowsArgumentNullException()
    {
        Assert.Throws<ArgumentNullException>(() => new TeacherPortalHub(
            _mockUnitCollectionService.Object,
            _mockUnitService.Object,
            _mockLessonService.Object,
            _mockMaterialService.Object,
            _mockUnitCollectionRepository.Object,
            null!,
            _mockLessonRepository.Object,
            _mockMaterialRepository.Object));
    }

    [Fact]
    public void Constructor_NullLessonRepository_ThrowsArgumentNullException()
    {
        Assert.Throws<ArgumentNullException>(() => new TeacherPortalHub(
            _mockUnitCollectionService.Object,
            _mockUnitService.Object,
            _mockLessonService.Object,
            _mockMaterialService.Object,
            _mockUnitCollectionRepository.Object,
            _mockUnitRepository.Object,
            null!,
            _mockMaterialRepository.Object));
    }

    [Fact]
    public void Constructor_NullMaterialRepository_ThrowsArgumentNullException()
    {
        Assert.Throws<ArgumentNullException>(() => new TeacherPortalHub(
            _mockUnitCollectionService.Object,
            _mockUnitService.Object,
            _mockLessonService.Object,
            _mockMaterialService.Object,
            _mockUnitCollectionRepository.Object,
            _mockUnitRepository.Object,
            _mockLessonRepository.Object,
            null!));
    }

    #endregion

    #region UnitCollection CRUD Tests - NetworkingAPISpec §1(1)(a)

    [Fact]
    public async Task CreateUnitCollection_CallsServiceWithAssignedId()
    {
        // Arrange
        var dto = new InternalCreateUnitCollectionDto("Test Collection");
        _mockUnitCollectionService.Setup(s => s.CreateAsync(It.IsAny<UnitCollectionEntity>()))
            .ReturnsAsync((UnitCollectionEntity e) => e);

        // Act
        var result = await _hub.CreateUnitCollection(dto);

        // Assert
        Assert.NotEqual(Guid.Empty, result.Id);
        Assert.Equal(dto.Title, result.Title);
        _mockUnitCollectionService.Verify(s => s.CreateAsync(It.Is<UnitCollectionEntity>(e => 
            e.Title == dto.Title && e.Id != Guid.Empty)), Times.Once);
    }

    [Fact]
    public async Task GetAllUnitCollections_ReturnsFromRepository()
    {
        // Arrange
        var entities = new List<UnitCollectionEntity>
        {
            new(Guid.NewGuid(), "Collection 1"),
            new(Guid.NewGuid(), "Collection 2")
        };
        _mockUnitCollectionRepository.Setup(r => r.GetAllAsync())
            .ReturnsAsync(entities);

        // Act
        var result = await _hub.GetAllUnitCollections();

        // Assert
        Assert.Equal(2, result.Count);
        _mockUnitCollectionRepository.Verify(r => r.GetAllAsync(), Times.Once);
    }

    [Fact]
    public async Task UpdateUnitCollection_CallsService()
    {
        // Arrange
        var entity = new UnitCollectionEntity(Guid.NewGuid(), "Updated Title");
        _mockUnitCollectionService.Setup(s => s.UpdateAsync(entity))
            .ReturnsAsync(entity);

        // Act
        await _hub.UpdateUnitCollection(entity);

        // Assert
        _mockUnitCollectionService.Verify(s => s.UpdateAsync(entity), Times.Once);
    }

    [Fact]
    public async Task DeleteUnitCollection_CallsService()
    {
        // Arrange
        var id = Guid.NewGuid();

        // Act
        await _hub.DeleteUnitCollection(id);

        // Assert
        _mockUnitCollectionService.Verify(s => s.DeleteAsync(id), Times.Once);
    }

    #endregion

    #region Unit CRUD Tests - NetworkingAPISpec §1(1)(b)

    [Fact]
    public async Task CreateUnit_CallsServiceWithAssignedId()
    {
        // Arrange
        var collectionId = Guid.NewGuid();
        var dto = new InternalCreateUnitDto(collectionId, "Test Unit");
        _mockUnitService.Setup(s => s.CreateAsync(It.IsAny<UnitEntity>()))
            .ReturnsAsync((UnitEntity e) => e);

        // Act
        var result = await _hub.CreateUnit(dto);

        // Assert
        Assert.NotEqual(Guid.Empty, result.Id);
        Assert.Equal(dto.Title, result.Title);
        Assert.Equal(collectionId, result.UnitCollectionId);
        _mockUnitService.Verify(s => s.CreateAsync(It.Is<UnitEntity>(e => 
            e.Title == dto.Title && e.Id != Guid.Empty)), Times.Once);
    }

    [Fact]
    public async Task GetAllUnits_ReturnsFromRepository()
    {
        // Arrange
        var collectionId = Guid.NewGuid();
        var entities = new List<UnitEntity>
        {
            new(Guid.NewGuid(), collectionId, "Unit 1"),
            new(Guid.NewGuid(), collectionId, "Unit 2")
        };
        _mockUnitRepository.Setup(r => r.GetAllAsync())
            .ReturnsAsync(entities);

        // Act
        var result = await _hub.GetAllUnits();

        // Assert
        Assert.Equal(2, result.Count);
        _mockUnitRepository.Verify(r => r.GetAllAsync(), Times.Once);
    }

    [Fact]
    public async Task UpdateUnit_CallsService()
    {
        // Arrange
        var entity = new UnitEntity(Guid.NewGuid(), Guid.NewGuid(), "Updated Unit");
        _mockUnitService.Setup(s => s.UpdateAsync(entity))
            .ReturnsAsync(entity);

        // Act
        await _hub.UpdateUnit(entity);

        // Assert
        _mockUnitService.Verify(s => s.UpdateAsync(entity), Times.Once);
    }

    [Fact]
    public async Task DeleteUnit_CallsService()
    {
        // Arrange
        var id = Guid.NewGuid();

        // Act
        await _hub.DeleteUnit(id);

        // Assert
        _mockUnitService.Verify(s => s.DeleteAsync(id), Times.Once);
    }

    #endregion

    #region Lesson CRUD Tests - NetworkingAPISpec §1(1)(c)

    [Fact]
    public async Task CreateLesson_CallsServiceWithAssignedId()
    {
        // Arrange
        var unitId = Guid.NewGuid();
        var dto = new InternalCreateLessonDto(unitId, "Test Lesson", "Description");
        _mockLessonService.Setup(s => s.CreateAsync(It.IsAny<LessonEntity>()))
            .ReturnsAsync((LessonEntity e) => e);

        // Act
        var result = await _hub.CreateLesson(dto);

        // Assert
        Assert.NotEqual(Guid.Empty, result.Id);
        Assert.Equal(dto.Title, result.Title);
        Assert.Equal(unitId, result.UnitId);
        _mockLessonService.Verify(s => s.CreateAsync(It.Is<LessonEntity>(e => 
            e.Title == dto.Title && e.Id != Guid.Empty)), Times.Once);
    }

    [Fact]
    public async Task GetAllLessons_ReturnsFromRepository()
    {
        // Arrange
        var unitId = Guid.NewGuid();
        var entities = new List<LessonEntity>
        {
            new(Guid.NewGuid(), unitId, "Lesson 1", "Description 1"),
            new(Guid.NewGuid(), unitId, "Lesson 2", "Description 2")
        };
        _mockLessonRepository.Setup(r => r.GetAllAsync())
            .ReturnsAsync(entities);

        // Act
        var result = await _hub.GetAllLessons();

        // Assert
        Assert.Equal(2, result.Count);
        _mockLessonRepository.Verify(r => r.GetAllAsync(), Times.Once);
    }

    [Fact]
    public async Task UpdateLesson_CallsService()
    {
        // Arrange
        var entity = new LessonEntity(Guid.NewGuid(), Guid.NewGuid(), "Updated Lesson", "Description");
        _mockLessonService.Setup(s => s.UpdateAsync(entity))
            .ReturnsAsync(entity);

        // Act
        await _hub.UpdateLesson(entity);

        // Assert
        _mockLessonService.Verify(s => s.UpdateAsync(entity), Times.Once);
    }

    [Fact]
    public async Task DeleteLesson_CallsService()
    {
        // Arrange
        var id = Guid.NewGuid();

        // Act
        await _hub.DeleteLesson(id);

        // Assert
        _mockLessonService.Verify(s => s.DeleteAsync(id), Times.Once);
    }

    #endregion

    #region Material CRUD Tests - NetworkingAPISpec §1(1)(d)

    [Fact]
    public async Task CreateMaterial_CallsServiceWithAssignedId()
    {
        // Arrange
        var lessonId = Guid.NewGuid();
        var dto = new InternalCreateMaterialDto(lessonId, "Test Material", "Content", MaterialType.READING);
        _mockMaterialService.Setup(s => s.CreateMaterialAsync(It.IsAny<MaterialEntity>()))
            .ReturnsAsync((MaterialEntity e) => e);

        // Act
        var result = await _hub.CreateMaterial(dto);

        // Assert
        Assert.NotEqual(Guid.Empty, result.Id);
        Assert.Equal(dto.Title, result.Title);
        Assert.Equal(lessonId, result.LessonId);
        Assert.IsType<ReadingMaterialEntity>(result);
        _mockMaterialService.Verify(s => s.CreateMaterialAsync(It.Is<MaterialEntity>(e => 
            e.Title == dto.Title && e.Id != Guid.Empty)), Times.Once);
    }

    [Fact]
    public async Task GetAllMaterials_ReturnsFromRepository()
    {
        // Arrange
        var lessonId = Guid.NewGuid();
        var entities = new List<MaterialEntity>
        {
            new ReadingMaterialEntity(Guid.NewGuid(), lessonId, "Material 1", "Content 1"),
            new ReadingMaterialEntity(Guid.NewGuid(), lessonId, "Material 2", "Content 2")
        };
        _mockMaterialRepository.Setup(r => r.GetAllAsync())
            .ReturnsAsync(entities);

        // Act
        var result = await _hub.GetAllMaterials();

        // Assert
        Assert.Equal(2, result.Count);
        _mockMaterialRepository.Verify(r => r.GetAllAsync(), Times.Once);
    }

    [Fact]
    public async Task UpdateMaterial_CallsService()
    {
        // Arrange
        var entity = new ReadingMaterialEntity(Guid.NewGuid(), Guid.NewGuid(), "Updated Material", "Content");
        _mockMaterialService.Setup(s => s.UpdateMaterialAsync(entity))
            .ReturnsAsync(entity);

        // Act
        await _hub.UpdateMaterial(entity);

        // Assert
        _mockMaterialService.Verify(s => s.UpdateMaterialAsync(entity), Times.Once);
    }

    [Fact]
    public async Task DeleteMaterial_CallsService()
    {
        // Arrange
        var id = Guid.NewGuid();

        // Act
        await _hub.DeleteMaterial(id);

        // Assert
        _mockMaterialService.Verify(s => s.DeleteMaterialAsync(id), Times.Once);
    }

    #endregion
}
