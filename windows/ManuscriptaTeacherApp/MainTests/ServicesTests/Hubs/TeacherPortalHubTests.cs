using System;
using System.Collections.Generic;
using System.Threading.Tasks;
using Moq;
using Xunit;
using Main.Models.Dtos;
using Main.Models.Entities;
using Main.Models.Entities.Materials;
using Main.Models.Entities.Questions;
using Main.Models.Enums;
using Main.Services;
using Main.Services.Hubs;
using Main.Services.Repositories;
using Main.Services.Network;
using Microsoft.AspNetCore.SignalR;

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
    private readonly Mock<IQuestionService> _mockQuestionService;
    private readonly Mock<ISourceDocumentService> _mockSourceDocumentService;
    private readonly Mock<IAttachmentService> _mockAttachmentService;
    private readonly Mock<IUnitCollectionRepository> _mockUnitCollectionRepository;
    private readonly Mock<IUnitRepository> _mockUnitRepository;
    private readonly Mock<ILessonRepository> _mockLessonRepository;
    private readonly Mock<IMaterialRepository> _mockMaterialRepository;
    private readonly Mock<IQuestionRepository> _mockQuestionRepository;
    private readonly Mock<ISourceDocumentRepository> _mockSourceDocumentRepository;
    private readonly Mock<IAttachmentRepository> _mockAttachmentRepository;
    private readonly Mock<IUdpBroadcastService> _mockUdpBroadcastService;
    private readonly Mock<ITcpPairingService> _mockTcpPairingService;
    private readonly Mock<IDeviceRegistryService> _mockDeviceRegistryService;
    private readonly Mock<IDeviceStatusCacheService> _mockDeviceStatusCacheService;
    private readonly TeacherPortalHub _hub;

    public TeacherPortalHubTests()
    {
        _mockUnitCollectionService = new Mock<IUnitCollectionService>();
        _mockUnitService = new Mock<IUnitService>();
        _mockLessonService = new Mock<ILessonService>();
        _mockMaterialService = new Mock<IMaterialService>();
        _mockQuestionService = new Mock<IQuestionService>();
        _mockSourceDocumentService = new Mock<ISourceDocumentService>();
        _mockAttachmentService = new Mock<IAttachmentService>();
        _mockUnitCollectionRepository = new Mock<IUnitCollectionRepository>();
        _mockUnitRepository = new Mock<IUnitRepository>();
        _mockLessonRepository = new Mock<ILessonRepository>();
        _mockMaterialRepository = new Mock<IMaterialRepository>();
        _mockQuestionRepository = new Mock<IQuestionRepository>();
        _mockSourceDocumentRepository = new Mock<ISourceDocumentRepository>();
        _mockAttachmentRepository = new Mock<IAttachmentRepository>();
        _mockUdpBroadcastService = new Mock<IUdpBroadcastService>();
        _mockTcpPairingService = new Mock<ITcpPairingService>();
        _mockDeviceRegistryService = new Mock<IDeviceRegistryService>();
        _mockDeviceStatusCacheService = new Mock<IDeviceStatusCacheService>();

        _hub = new TeacherPortalHub(
            _mockUnitCollectionService.Object,
            _mockUnitService.Object,
            _mockLessonService.Object,
            _mockMaterialService.Object,
            _mockQuestionService.Object,
            _mockSourceDocumentService.Object,
            _mockAttachmentService.Object,
            _mockUnitCollectionRepository.Object,
            _mockUnitRepository.Object,
            _mockLessonRepository.Object,
            _mockMaterialRepository.Object,
            _mockQuestionRepository.Object,
            _mockSourceDocumentRepository.Object,
            _mockAttachmentRepository.Object,
            _mockUdpBroadcastService.Object,
            _mockTcpPairingService.Object,
            _mockDeviceRegistryService.Object,
            _mockDeviceStatusCacheService.Object);
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
            _mockQuestionService.Object,
            _mockSourceDocumentService.Object,
            _mockAttachmentService.Object,
            _mockUnitCollectionRepository.Object,
            _mockUnitRepository.Object,
            _mockLessonRepository.Object,
            _mockMaterialRepository.Object,
            _mockQuestionRepository.Object,
            _mockSourceDocumentRepository.Object,
            _mockAttachmentRepository.Object,
            _mockUdpBroadcastService.Object,
            _mockTcpPairingService.Object,
            _mockDeviceRegistryService.Object,
            _mockDeviceStatusCacheService.Object));
    }

    [Fact]
    public void Constructor_NullUnitService_ThrowsArgumentNullException()
    {
        Assert.Throws<ArgumentNullException>(() => new TeacherPortalHub(
            _mockUnitCollectionService.Object,
            null!,
            _mockLessonService.Object,
            _mockMaterialService.Object,
            _mockQuestionService.Object,
            _mockSourceDocumentService.Object,
            _mockAttachmentService.Object,
            _mockUnitCollectionRepository.Object,
            _mockUnitRepository.Object,
            _mockLessonRepository.Object,
            _mockMaterialRepository.Object,
            _mockQuestionRepository.Object,
            _mockSourceDocumentRepository.Object,
            _mockAttachmentRepository.Object,
            _mockUdpBroadcastService.Object,
            _mockTcpPairingService.Object,
            _mockDeviceRegistryService.Object,
            _mockDeviceStatusCacheService.Object));
    }

    [Fact]
    public void Constructor_NullLessonService_ThrowsArgumentNullException()
    {
        Assert.Throws<ArgumentNullException>(() => new TeacherPortalHub(
            _mockUnitCollectionService.Object,
            _mockUnitService.Object,
            null!,
            _mockMaterialService.Object,
            _mockQuestionService.Object,
            _mockSourceDocumentService.Object,
            _mockAttachmentService.Object,
            _mockUnitCollectionRepository.Object,
            _mockUnitRepository.Object,
            _mockLessonRepository.Object,
            _mockMaterialRepository.Object,
            _mockQuestionRepository.Object,
            _mockSourceDocumentRepository.Object,
            _mockAttachmentRepository.Object,
            _mockUdpBroadcastService.Object,
            _mockTcpPairingService.Object,
            _mockDeviceRegistryService.Object,
            _mockDeviceStatusCacheService.Object));
    }

    [Fact]
    public void Constructor_NullMaterialService_ThrowsArgumentNullException()
    {
        Assert.Throws<ArgumentNullException>(() => new TeacherPortalHub(
            _mockUnitCollectionService.Object,
            _mockUnitService.Object,
            _mockLessonService.Object,
            null!,
            _mockQuestionService.Object,
            _mockSourceDocumentService.Object,
            _mockAttachmentService.Object,
            _mockUnitCollectionRepository.Object,
            _mockUnitRepository.Object,
            _mockLessonRepository.Object,
            _mockMaterialRepository.Object,
            _mockQuestionRepository.Object,
            _mockSourceDocumentRepository.Object,
            _mockAttachmentRepository.Object,
            _mockUdpBroadcastService.Object,
            _mockTcpPairingService.Object,
            _mockDeviceRegistryService.Object,
            _mockDeviceStatusCacheService.Object));
    }

    [Fact]
    public void Constructor_NullQuestionService_ThrowsArgumentNullException()
    {
        Assert.Throws<ArgumentNullException>(() => new TeacherPortalHub(
            _mockUnitCollectionService.Object,
            _mockUnitService.Object,
            _mockLessonService.Object,
            _mockMaterialService.Object,
            null!,
            _mockSourceDocumentService.Object,
            _mockAttachmentService.Object,
            _mockUnitCollectionRepository.Object,
            _mockUnitRepository.Object,
            _mockLessonRepository.Object,
            _mockMaterialRepository.Object,
            _mockQuestionRepository.Object,
            _mockSourceDocumentRepository.Object,
            _mockAttachmentRepository.Object,
            _mockUdpBroadcastService.Object,
            _mockTcpPairingService.Object,
            _mockDeviceRegistryService.Object,
            _mockDeviceStatusCacheService.Object));
    }

    [Fact]
    public void Constructor_NullSourceDocumentService_ThrowsArgumentNullException()
    {
        Assert.Throws<ArgumentNullException>(() => new TeacherPortalHub(
            _mockUnitCollectionService.Object,
            _mockUnitService.Object,
            _mockLessonService.Object,
            _mockMaterialService.Object,
            _mockQuestionService.Object,
            null!,
            _mockAttachmentService.Object,
            _mockUnitCollectionRepository.Object,
            _mockUnitRepository.Object,
            _mockLessonRepository.Object,
            _mockMaterialRepository.Object,
            _mockQuestionRepository.Object,
            _mockSourceDocumentRepository.Object,
            _mockAttachmentRepository.Object,
            _mockUdpBroadcastService.Object,
            _mockTcpPairingService.Object,
            _mockDeviceRegistryService.Object,
            _mockDeviceStatusCacheService.Object));
    }

    [Fact]
    public void Constructor_NullAttachmentService_ThrowsArgumentNullException()
    {
        Assert.Throws<ArgumentNullException>(() => new TeacherPortalHub(
            _mockUnitCollectionService.Object,
            _mockUnitService.Object,
            _mockLessonService.Object,
            _mockMaterialService.Object,
            _mockQuestionService.Object,
            _mockSourceDocumentService.Object,
            null!,
            _mockUnitCollectionRepository.Object,
            _mockUnitRepository.Object,
            _mockLessonRepository.Object,
            _mockMaterialRepository.Object,
            _mockQuestionRepository.Object,
            _mockSourceDocumentRepository.Object,
            _mockAttachmentRepository.Object,
            _mockUdpBroadcastService.Object,
            _mockTcpPairingService.Object,
            _mockDeviceRegistryService.Object,
            _mockDeviceStatusCacheService.Object));
    }

    [Fact]
    public void Constructor_NullUnitCollectionRepository_ThrowsArgumentNullException()
    {
        Assert.Throws<ArgumentNullException>(() => new TeacherPortalHub(
            _mockUnitCollectionService.Object,
            _mockUnitService.Object,
            _mockLessonService.Object,
            _mockMaterialService.Object,
            _mockQuestionService.Object,
            _mockSourceDocumentService.Object,
            _mockAttachmentService.Object,
            null!,
            _mockUnitRepository.Object,
            _mockLessonRepository.Object,
            _mockMaterialRepository.Object,
            _mockQuestionRepository.Object,
            _mockSourceDocumentRepository.Object,
            _mockAttachmentRepository.Object,
            _mockUdpBroadcastService.Object,
            _mockTcpPairingService.Object,
            _mockDeviceRegistryService.Object,
            _mockDeviceStatusCacheService.Object));
    }

    [Fact]
    public void Constructor_NullUnitRepository_ThrowsArgumentNullException()
    {
        Assert.Throws<ArgumentNullException>(() => new TeacherPortalHub(
            _mockUnitCollectionService.Object,
            _mockUnitService.Object,
            _mockLessonService.Object,
            _mockMaterialService.Object,
            _mockQuestionService.Object,
            _mockSourceDocumentService.Object,
            _mockAttachmentService.Object,
            _mockUnitCollectionRepository.Object,
            null!,
            _mockLessonRepository.Object,
            _mockMaterialRepository.Object,
            _mockQuestionRepository.Object,
            _mockSourceDocumentRepository.Object,
            _mockAttachmentRepository.Object,
            _mockUdpBroadcastService.Object,
            _mockTcpPairingService.Object,
            _mockDeviceRegistryService.Object,
            _mockDeviceStatusCacheService.Object));
    }

    [Fact]
    public void Constructor_NullLessonRepository_ThrowsArgumentNullException()
    {
        Assert.Throws<ArgumentNullException>(() => new TeacherPortalHub(
            _mockUnitCollectionService.Object,
            _mockUnitService.Object,
            _mockLessonService.Object,
            _mockMaterialService.Object,
            _mockQuestionService.Object,
            _mockSourceDocumentService.Object,
            _mockAttachmentService.Object,
            _mockUnitCollectionRepository.Object,
            _mockUnitRepository.Object,
            null!,
            _mockMaterialRepository.Object,
            _mockQuestionRepository.Object,
            _mockSourceDocumentRepository.Object,
            _mockAttachmentRepository.Object,
            _mockUdpBroadcastService.Object,
            _mockTcpPairingService.Object,
            _mockDeviceRegistryService.Object,
            _mockDeviceStatusCacheService.Object));
    }

    [Fact]
    public void Constructor_NullMaterialRepository_ThrowsArgumentNullException()
    {
        Assert.Throws<ArgumentNullException>(() => new TeacherPortalHub(
            _mockUnitCollectionService.Object,
            _mockUnitService.Object,
            _mockLessonService.Object,
            _mockMaterialService.Object,
            _mockQuestionService.Object,
            _mockSourceDocumentService.Object,
            _mockAttachmentService.Object,
            _mockUnitCollectionRepository.Object,
            _mockUnitRepository.Object,
            _mockLessonRepository.Object,
            null!,
            _mockQuestionRepository.Object,
            _mockSourceDocumentRepository.Object,
            _mockAttachmentRepository.Object,
            _mockUdpBroadcastService.Object,
            _mockTcpPairingService.Object,
            _mockDeviceRegistryService.Object,
            _mockDeviceStatusCacheService.Object));
    }

    [Fact]
    public void Constructor_NullQuestionRepository_ThrowsArgumentNullException()
    {
        Assert.Throws<ArgumentNullException>(() => new TeacherPortalHub(
            _mockUnitCollectionService.Object,
            _mockUnitService.Object,
            _mockLessonService.Object,
            _mockMaterialService.Object,
            _mockQuestionService.Object,
            _mockSourceDocumentService.Object,
            _mockAttachmentService.Object,
            _mockUnitCollectionRepository.Object,
            _mockUnitRepository.Object,
            _mockLessonRepository.Object,
            _mockMaterialRepository.Object,
            null!,
            _mockSourceDocumentRepository.Object,
            _mockAttachmentRepository.Object,
            _mockUdpBroadcastService.Object,
            _mockTcpPairingService.Object,
            _mockDeviceRegistryService.Object,
            _mockDeviceStatusCacheService.Object));
    }

    [Fact]
    public void Constructor_NullSourceDocumentRepository_ThrowsArgumentNullException()
    {
        Assert.Throws<ArgumentNullException>(() => new TeacherPortalHub(
            _mockUnitCollectionService.Object,
            _mockUnitService.Object,
            _mockLessonService.Object,
            _mockMaterialService.Object,
            _mockQuestionService.Object,
            _mockSourceDocumentService.Object,
            _mockAttachmentService.Object,
            _mockUnitCollectionRepository.Object,
            _mockUnitRepository.Object,
            _mockLessonRepository.Object,
            _mockMaterialRepository.Object,
            _mockQuestionRepository.Object,
            null!,
            _mockAttachmentRepository.Object,
            _mockUdpBroadcastService.Object,
            _mockTcpPairingService.Object,
            _mockDeviceRegistryService.Object,
            _mockDeviceStatusCacheService.Object));
    }

    [Fact]
    public void Constructor_NullAttachmentRepository_ThrowsArgumentNullException()
    {
        Assert.Throws<ArgumentNullException>(() => new TeacherPortalHub(
            _mockUnitCollectionService.Object,
            _mockUnitService.Object,
            _mockLessonService.Object,
            _mockMaterialService.Object,
            _mockQuestionService.Object,
            _mockSourceDocumentService.Object,
            _mockAttachmentService.Object,
            _mockUnitCollectionRepository.Object,
            _mockUnitRepository.Object,
            _mockLessonRepository.Object,
            _mockMaterialRepository.Object,
            _mockQuestionRepository.Object,
            _mockSourceDocumentRepository.Object,
            null!,
            _mockUdpBroadcastService.Object,
            _mockTcpPairingService.Object,
            _mockDeviceRegistryService.Object,
            _mockDeviceStatusCacheService.Object));
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
    public async Task UpdateMaterial_ValidDto_UpdatesViaRepository()
    {
        // Arrange
        var materialId = Guid.NewGuid();
        var lessonId = Guid.NewGuid();
        var existingMaterial = new ReadingMaterialEntity(materialId, lessonId, "Original Title", "Original Content");
        var dto = new InternalUpdateMaterialDto(
            materialId, lessonId, "Updated Title", "Updated Content", MaterialType.READING);
        
        _mockMaterialRepository.Setup(r => r.GetByIdAsync(materialId))
            .ReturnsAsync(existingMaterial);
        _mockMaterialRepository.Setup(r => r.UpdateAsync(It.IsAny<MaterialEntity>()))
            .Returns(Task.CompletedTask);

        // Act
        await _hub.UpdateMaterial(dto);

        // Assert
        _mockMaterialRepository.Verify(r => r.GetByIdAsync(materialId), Times.Once);
        _mockMaterialRepository.Verify(r => r.UpdateAsync(It.Is<MaterialEntity>(m =>
            m.Id == materialId &&
            m.Title == "Updated Title" &&
            m.Content == "Updated Content"
        )), Times.Once);
    }

    [Fact]
    public async Task UpdateMaterial_NonExistingMaterial_ThrowsHubException()
    {
        // Arrange
        var dto = new InternalUpdateMaterialDto(
            Guid.NewGuid(), Guid.NewGuid(), "Title", "Content", MaterialType.READING);
        
        _mockMaterialRepository.Setup(r => r.GetByIdAsync(dto.Id))
            .ReturnsAsync((MaterialEntity?)null);

        // Act & Assert
        await Assert.ThrowsAsync<HubException>(() => _hub.UpdateMaterial(dto));
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

    #region Question CRUD Tests - NetworkingAPISpec §1(1)(d1)

    [Fact]
    public async Task CreateQuestion_CallsServiceAndReturnsId()
    {
        // Arrange
        var materialId = Guid.NewGuid();
        var dto = new InternalCreateQuestionDto(
            materialId,
            QuestionType.MULTIPLE_CHOICE,
            "What is 2+2?",
            new List<string> { "3", "4", "5" },
            1);
        
        _mockQuestionService.Setup(s => s.CreateQuestionAsync(It.IsAny<QuestionEntity>()))
            .ReturnsAsync((QuestionEntity e) => e);

        // Act
        var result = await _hub.CreateQuestion(dto);

        // Assert
        Assert.NotEqual(Guid.Empty, result);
        _mockQuestionService.Verify(s => s.CreateQuestionAsync(It.Is<QuestionEntity>(e => 
            e.QuestionText == dto.QuestionText && e.MaterialId == materialId)), Times.Once);
    }

    [Fact]
    public async Task GetQuestionsUnderMaterial_ReturnsFromRepository()
    {
        // Arrange
        var materialId = Guid.NewGuid();
        var questions = new List<QuestionEntity>
        {
            new MultipleChoiceQuestionEntity(Guid.NewGuid(), materialId, "Q1", new List<string> { "A", "B" }, 0),
            new MultipleChoiceQuestionEntity(Guid.NewGuid(), materialId, "Q2", new List<string> { "X", "Y" }, 1)
        };
        _mockQuestionRepository.Setup(r => r.GetByMaterialIdAsync(materialId))
            .ReturnsAsync(questions);

        // Act
        var result = await _hub.GetQuestionsUnderMaterial(materialId);

        // Assert
        Assert.Equal(2, result.Count);
        _mockQuestionRepository.Verify(r => r.GetByMaterialIdAsync(materialId), Times.Once);
    }

    [Fact]
    public async Task UpdateQuestion_CallsService()
    {
        // Arrange
        var questionId = Guid.NewGuid();
        var materialId = Guid.NewGuid();
        var dto = new InternalUpdateQuestionDto(
            questionId,
            materialId,
            QuestionType.MULTIPLE_CHOICE,
            "Updated Question",
            new List<string> { "A", "B", "C" },
            2);
        
        var existingQuestion = new MultipleChoiceQuestionEntity(
            questionId, materialId, "Original", new List<string> { "A", "B" }, 0);
        
        _mockQuestionRepository.Setup(r => r.GetByIdAsync(questionId))
            .ReturnsAsync(existingQuestion);
        _mockQuestionService.Setup(s => s.UpdateQuestionAsync(It.IsAny<QuestionEntity>()))
            .ReturnsAsync((QuestionEntity e) => e);

        // Act
        await _hub.UpdateQuestion(dto);

        // Assert
        _mockQuestionRepository.Verify(r => r.GetByIdAsync(questionId), Times.Once);
        _mockQuestionService.Verify(s => s.UpdateQuestionAsync(It.Is<QuestionEntity>(e => 
            e.QuestionText == dto.QuestionText)), Times.Once);
    }

    [Fact]
    public async Task DeleteQuestion_CallsService()
    {
        // Arrange
        var id = Guid.NewGuid();

        // Act
        await _hub.DeleteQuestion(id);

        // Assert
        _mockQuestionService.Verify(s => s.DeleteQuestionAsync(id), Times.Once);
    }

    #endregion

    #region SourceDocument CRUD Tests - NetworkingAPISpec §1(1)(k)

    [Fact]
    public async Task CreateSourceDocument_CallsServiceWithAssignedId()
    {
        // Arrange
        var collectionId = Guid.NewGuid();
        var dto = new InternalCreateSourceDocumentDto { UnitCollectionId = collectionId, Transcript = "Sample transcript" };
        _mockSourceDocumentService.Setup(s => s.CreateAsync(It.IsAny<SourceDocumentEntity>()))
            .ReturnsAsync((SourceDocumentEntity e) => e);

        // Act
        var result = await _hub.CreateSourceDocument(dto);

        // Assert
        Assert.NotEqual(Guid.Empty, result.Id);
        Assert.Equal(dto.UnitCollectionId, result.UnitCollectionId);
        Assert.Equal(dto.Transcript, result.Transcript);
        _mockSourceDocumentService.Verify(s => s.CreateAsync(It.Is<SourceDocumentEntity>(e => 
            e.Transcript == dto.Transcript && e.Id != Guid.Empty)), Times.Once);
    }

    [Fact]
    public async Task GetAllSourceDocuments_ReturnsFromRepository()
    {
        // Arrange
        var collectionId = Guid.NewGuid();
        var entities = new List<SourceDocumentEntity>
        {
            new(Guid.NewGuid(), collectionId, "Transcript 1"),
            new(Guid.NewGuid(), collectionId, "Transcript 2")
        };
        _mockSourceDocumentRepository.Setup(r => r.GetAllAsync())
            .ReturnsAsync(entities);

        // Act
        var result = await _hub.GetAllSourceDocuments();

        // Assert
        Assert.Equal(2, result.Count);
        _mockSourceDocumentRepository.Verify(r => r.GetAllAsync(), Times.Once);
    }

    [Fact]
    public async Task DeleteSourceDocument_CallsService()
    {
        // Arrange
        var id = Guid.NewGuid();

        // Act
        await _hub.DeleteSourceDocument(id);

        // Assert
        _mockSourceDocumentService.Verify(s => s.DeleteAsync(id), Times.Once);
    }

    #endregion

    #region Attachment CRUD Tests - NetworkingAPISpec §1(1)(l)

    [Fact]
    public async Task CreateAttachment_CallsServiceAndReturnsId()
    {
        // Arrange
        var materialId = Guid.NewGuid();
        var dto = new InternalCreateAttachmentDto(materialId, "image", "png");
        _mockAttachmentService.Setup(s => s.CreateAsync(It.IsAny<AttachmentEntity>()))
            .ReturnsAsync((AttachmentEntity e) => e);

        // Act
        var result = await _hub.CreateAttachment(dto);

        // Assert
        Assert.NotEqual(Guid.Empty, result);
        _mockAttachmentService.Verify(s => s.CreateAsync(It.Is<AttachmentEntity>(e => 
            e.FileBaseName == dto.FileBaseName && 
            e.FileExtension == dto.FileExtension &&
            e.MaterialId == materialId)), Times.Once);
    }

    [Fact]
    public async Task GetAttachmentsUnderMaterial_ReturnsFromRepository()
    {
        // Arrange
        var materialId = Guid.NewGuid();
        var attachments = new List<AttachmentEntity>
        {
            new(Guid.NewGuid(), materialId, "image1", "png"),
            new(Guid.NewGuid(), materialId, "document", "pdf")
        };
        _mockAttachmentRepository.Setup(r => r.GetByMaterialIdAsync(materialId))
            .ReturnsAsync(attachments);

        // Act
        var result = await _hub.GetAttachmentsUnderMaterial(materialId);

        // Assert
        Assert.Equal(2, result.Count);
        _mockAttachmentRepository.Verify(r => r.GetByMaterialIdAsync(materialId), Times.Once);
    }

    [Fact]
    public async Task DeleteAttachment_CallsService()
    {
        // Arrange
        var id = Guid.NewGuid();

        // Act
        await _hub.DeleteAttachment(id);

        // Assert
        _mockAttachmentService.Verify(s => s.DeleteAsync(id), Times.Once);
    }

    #endregion
}
