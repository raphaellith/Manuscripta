using System;
using System.Collections.Generic;
using System.Threading.Tasks;
using Moq;
using Xunit;
using Main.Models.Dtos;
using Main.Models.Entities;
using Main.Models.Entities.Materials;
using Main.Models.Entities.Questions;
using Main.Models.Entities.Responses;
using Main.Models.Enums;
using Main.Services;
using Main.Services.Hubs;
using Main.Services.Repositories;
using Main.Services.Network;
using Main.Services.RuntimeDependencies;
using Microsoft.AspNetCore.SignalR;
using Microsoft.Extensions.Logging;
using System.Threading;

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
    private readonly Mock<IDistributionService> _mockDistributionService;
    private readonly Mock<IFeedbackRepository> _mockFeedbackRepository;
    private readonly Mock<IResponseRepository> _mockResponseRepository;
    private readonly Mock<ILogger<TeacherPortalHub>> _mockLogger;
    private readonly Mock<IMaterialPdfService> _mockMaterialPdfService;
    private readonly Mock<IRmapiService> _mockRmapiService;
    private readonly Mock<IReMarkableDeviceRepository> _mockReMarkableDeviceRepository;
    private readonly Mock<IReMarkableDeploymentService> _mockReMarkableDeploymentService;
    private readonly Mock<IRuntimeDependencyRegistry> _mockRuntimeDependencyRegistry;
    private readonly Mock<IConfigurationService> _mockConfigurationService;
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
        _mockDistributionService = new Mock<IDistributionService>();
        _mockFeedbackRepository = new Mock<IFeedbackRepository>();
        _mockResponseRepository = new Mock<IResponseRepository>();
        _mockLogger = new Mock<ILogger<TeacherPortalHub>>();
        _mockMaterialPdfService = new Mock<IMaterialPdfService>();
        _mockRmapiService = new Mock<IRmapiService>();
        _mockReMarkableDeviceRepository = new Mock<IReMarkableDeviceRepository>();
        _mockReMarkableDeploymentService = new Mock<IReMarkableDeploymentService>();
        _mockRuntimeDependencyRegistry = new Mock<IRuntimeDependencyRegistry>();
        _mockConfigurationService = new Mock<IConfigurationService>();

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
            _mockDeviceStatusCacheService.Object,
            _mockDistributionService.Object,
            _mockFeedbackRepository.Object,
            _mockResponseRepository.Object,
            _mockLogger.Object,
            _mockMaterialPdfService.Object,
            _mockRmapiService.Object,
            _mockReMarkableDeviceRepository.Object,
            _mockReMarkableDeploymentService.Object,
            _mockRuntimeDependencyRegistry.Object,
            _mockConfigurationService.Object);
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
            _mockDeviceStatusCacheService.Object,
            _mockDistributionService.Object,
            _mockFeedbackRepository.Object,
            _mockResponseRepository.Object,
            _mockLogger.Object,
            _mockMaterialPdfService.Object,
            _mockRmapiService.Object,
            _mockReMarkableDeviceRepository.Object,
            _mockReMarkableDeploymentService.Object,
            _mockRuntimeDependencyRegistry.Object,
            _mockConfigurationService.Object));
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
            _mockDeviceStatusCacheService.Object,
            _mockDistributionService.Object,
            _mockFeedbackRepository.Object,
            _mockResponseRepository.Object,
            _mockLogger.Object,
            _mockMaterialPdfService.Object,
            _mockRmapiService.Object,
            _mockReMarkableDeviceRepository.Object,
            _mockReMarkableDeploymentService.Object,
            _mockRuntimeDependencyRegistry.Object,
            _mockConfigurationService.Object));
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
            _mockDeviceStatusCacheService.Object,
            _mockDistributionService.Object,
            _mockFeedbackRepository.Object,
            _mockResponseRepository.Object,
            _mockLogger.Object,
            _mockMaterialPdfService.Object,
            _mockRmapiService.Object,
            _mockReMarkableDeviceRepository.Object,
            _mockReMarkableDeploymentService.Object,
            _mockRuntimeDependencyRegistry.Object,
            _mockConfigurationService.Object));
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
            _mockDeviceStatusCacheService.Object,
            _mockDistributionService.Object,
            _mockFeedbackRepository.Object,
            _mockResponseRepository.Object,
            _mockLogger.Object,
            _mockMaterialPdfService.Object,
            _mockRmapiService.Object,
            _mockReMarkableDeviceRepository.Object,
            _mockReMarkableDeploymentService.Object,
            _mockRuntimeDependencyRegistry.Object,
            _mockConfigurationService.Object));
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
            _mockDeviceStatusCacheService.Object,
            _mockDistributionService.Object,
            _mockFeedbackRepository.Object,
            _mockResponseRepository.Object,
            _mockLogger.Object,
            _mockMaterialPdfService.Object,
            _mockRmapiService.Object,
            _mockReMarkableDeviceRepository.Object,
            _mockReMarkableDeploymentService.Object,
            _mockRuntimeDependencyRegistry.Object,
            _mockConfigurationService.Object));
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
            _mockDeviceStatusCacheService.Object,
            _mockDistributionService.Object,
            _mockFeedbackRepository.Object,
            _mockResponseRepository.Object,
            _mockLogger.Object,
            _mockMaterialPdfService.Object,
            _mockRmapiService.Object,
            _mockReMarkableDeviceRepository.Object,
            _mockReMarkableDeploymentService.Object,
            _mockRuntimeDependencyRegistry.Object,
            _mockConfigurationService.Object));
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
            _mockDeviceStatusCacheService.Object,
            _mockDistributionService.Object,
            _mockFeedbackRepository.Object,
            _mockResponseRepository.Object,
            _mockLogger.Object,
            _mockMaterialPdfService.Object,
            _mockRmapiService.Object,
            _mockReMarkableDeviceRepository.Object,
            _mockReMarkableDeploymentService.Object,
            _mockRuntimeDependencyRegistry.Object,
            _mockConfigurationService.Object));
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
            _mockDeviceStatusCacheService.Object,
            _mockDistributionService.Object,
            _mockFeedbackRepository.Object,
            _mockResponseRepository.Object,
            _mockLogger.Object,
            _mockMaterialPdfService.Object,
            _mockRmapiService.Object,
            _mockReMarkableDeviceRepository.Object,
            _mockReMarkableDeploymentService.Object,
            _mockRuntimeDependencyRegistry.Object,
            _mockConfigurationService.Object));
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
            _mockDeviceStatusCacheService.Object,
            _mockDistributionService.Object,
            _mockFeedbackRepository.Object,
            _mockResponseRepository.Object,
            _mockLogger.Object,
            _mockMaterialPdfService.Object,
            _mockRmapiService.Object,
            _mockReMarkableDeviceRepository.Object,
            _mockReMarkableDeploymentService.Object,
            _mockRuntimeDependencyRegistry.Object,
            _mockConfigurationService.Object));
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
            _mockDeviceStatusCacheService.Object,
            _mockDistributionService.Object,
            _mockFeedbackRepository.Object,
            _mockResponseRepository.Object,
            _mockLogger.Object,
            _mockMaterialPdfService.Object,
            _mockRmapiService.Object,
            _mockReMarkableDeviceRepository.Object,
            _mockReMarkableDeploymentService.Object,
            _mockRuntimeDependencyRegistry.Object,
            _mockConfigurationService.Object));
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
            _mockDeviceStatusCacheService.Object,
            _mockDistributionService.Object,
            _mockFeedbackRepository.Object,
            _mockResponseRepository.Object,
            _mockLogger.Object,
            _mockMaterialPdfService.Object,
            _mockRmapiService.Object,
            _mockReMarkableDeviceRepository.Object,
            _mockReMarkableDeploymentService.Object,
            _mockRuntimeDependencyRegistry.Object,
            _mockConfigurationService.Object));
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
            _mockDeviceStatusCacheService.Object,
            _mockDistributionService.Object,
            _mockFeedbackRepository.Object,
            _mockResponseRepository.Object,
            _mockLogger.Object,
            _mockMaterialPdfService.Object,
            _mockRmapiService.Object,
            _mockReMarkableDeviceRepository.Object,
            _mockReMarkableDeploymentService.Object,
            _mockRuntimeDependencyRegistry.Object,
            _mockConfigurationService.Object));
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
            _mockDeviceStatusCacheService.Object,
            _mockDistributionService.Object,
            _mockFeedbackRepository.Object,
            _mockResponseRepository.Object,
            _mockLogger.Object,
            _mockMaterialPdfService.Object,
            _mockRmapiService.Object,
            _mockReMarkableDeviceRepository.Object,
            _mockReMarkableDeploymentService.Object,
            _mockRuntimeDependencyRegistry.Object,
            _mockConfigurationService.Object));
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
            _mockDeviceStatusCacheService.Object,
            _mockDistributionService.Object,
            _mockFeedbackRepository.Object,
            _mockResponseRepository.Object,
            _mockLogger.Object,
            _mockMaterialPdfService.Object,
            _mockRmapiService.Object,
            _mockReMarkableDeviceRepository.Object,
            _mockReMarkableDeploymentService.Object,
            _mockRuntimeDependencyRegistry.Object,
            _mockConfigurationService.Object));
    }

    [Fact]
    public void Constructor_NullMaterialPdfService_ThrowsArgumentNullException()
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
            _mockAttachmentRepository.Object,
            _mockUdpBroadcastService.Object,
            _mockTcpPairingService.Object,
            _mockDeviceRegistryService.Object,
            _mockDeviceStatusCacheService.Object,
            _mockDistributionService.Object,
            _mockFeedbackRepository.Object,
            _mockResponseRepository.Object,
            _mockLogger.Object,
            null!,
            _mockRmapiService.Object,
            _mockReMarkableDeviceRepository.Object,
            _mockReMarkableDeploymentService.Object,
            _mockRuntimeDependencyRegistry.Object,
            _mockConfigurationService.Object));
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

    #region Device Operations Tests

    [Fact]
    public async Task PairDevices_StartsUdpAndTcpServices()
    {
        // Act
        await _hub.PairDevices();

        // Assert
        _mockUdpBroadcastService.Verify(s => s.StartBroadcastingAsync(It.IsAny<CancellationToken>()), Times.Once);
        _mockTcpPairingService.Verify(s => s.StartListeningAsync(It.IsAny<CancellationToken>()), Times.Once);
    }

    [Fact]
    public async Task StopPairing_StopsUdpAndTcpServices()
    {
        // Act
        await _hub.StopPairing();

        // Assert
        _mockUdpBroadcastService.Verify(s => s.StopBroadcasting(), Times.Once);
        _mockTcpPairingService.Verify(s => s.StopListening(), Times.Once);
    }

    [Fact]
    public async Task GetAllPairedDevices_ReturnsFromRegistry()
    {
        // Arrange
        var devices = new List<PairedDeviceEntity>
        {
            new(Guid.NewGuid(), "Device 1"),
            new(Guid.NewGuid(), "Device 2")
        };
        _mockDeviceRegistryService.Setup(s => s.GetAllAsync())
            .ReturnsAsync(devices);

        // Act
        var result = await _hub.GetAllPairedDevices();

        // Assert
        Assert.Equal(2, result.Count);
        _mockDeviceRegistryService.Verify(s => s.GetAllAsync(), Times.Once);
    }

    [Fact]
    public async Task GetAllDeviceStatuses_ReturnsFromCache()
    {
        // Arrange
        var statuses = new List<DeviceStatusEntity>
        {
            new DeviceStatusEntity()
        };
        _mockDeviceStatusCacheService.Setup(s => s.GetAllStatuses())
            .Returns(statuses);

        // Act
        var result = await _hub.GetAllDeviceStatuses();

        // Assert
        Assert.Single(result);
        _mockDeviceStatusCacheService.Verify(s => s.GetAllStatuses(), Times.Once);
    }

    [Fact]
    public async Task LockDevices_CallsSendLockScreenForEachDevice()
    {
        // Arrange
        var deviceIds = new List<Guid> { Guid.NewGuid(), Guid.NewGuid() };

        // Act
        await _hub.LockDevices(deviceIds);

        // Assert
        foreach (var id in deviceIds)
        {
            _mockTcpPairingService.Verify(s => s.SendLockScreenAsync(id.ToString()), Times.Once);
        }
    }

    [Fact]
    public async Task UnlockDevices_CallsSendUnlockScreenForEachDevice()
    {
        // Arrange
        var deviceIds = new List<Guid> { Guid.NewGuid(), Guid.NewGuid() };

        // Act
        await _hub.UnlockDevices(deviceIds);

        // Assert
        foreach (var id in deviceIds)
        {
            _mockTcpPairingService.Verify(s => s.SendUnlockScreenAsync(id.ToString()), Times.Once);
        }
    }

    [Fact]
    public async Task DeployMaterial_CallsDistributionServiceAndSendsToDevices()
    {
        // Arrange
        var materialId = Guid.NewGuid();
        var deviceIds = new List<Guid> { Guid.NewGuid(), Guid.NewGuid() };

        // Act
        await _hub.DeployMaterial(materialId, deviceIds);

        // Assert - verifies each device gets material assigned and TCP notification
        // Per API Contract.md §3.6.2: SendDistributeMaterialAsync now includes materialIds for per-entity ACK tracking
        foreach (var id in deviceIds)
        {
            _mockDistributionService.Verify(s => s.AssignMaterialsToDeviceAsync(id, It.IsAny<IEnumerable<Guid>>()), Times.Once);
            _mockTcpPairingService.Verify(s => s.SendDistributeMaterialAsync(
                id.ToString(), 
                It.Is<IEnumerable<Guid>>(m => m.Contains(materialId))), Times.Once);
        }
    }

    [Fact]
    public async Task UnpairDevices_CallsSendUnpairForEachDevice()
    {
        // Arrange
        var deviceIds = new List<Guid> { Guid.NewGuid(), Guid.NewGuid() };

        // Act
        await _hub.UnpairDevices(deviceIds);

        // Assert
        foreach (var id in deviceIds)
        {
            _mockTcpPairingService.Verify(s => s.SendUnpairAsync(id.ToString()), Times.Once);
        }
    }

    [Fact]
    public async Task UpdatePairedDevice_CallsRegistryUpdate()
    {
        // Arrange
        var device = new PairedDeviceEntity(Guid.NewGuid(), "Updated Name");

        // Act
        await _hub.UpdatePairedDevice(device);

        // Assert
        _mockDeviceRegistryService.Verify(s => s.UpdateAsync(device), Times.Once);
    }

    #endregion

    #region Feedback Operations Tests

    [Fact]
    public async Task CreateFeedback_ValidDto_ReturnsWithAssignedId()
    {
        // Arrange
        var responseId = Guid.NewGuid();
        var dto = new InternalCreateFeedbackDto
        {
            ResponseId = responseId,
            Text = "Good work!",
            Marks = 8
        };
        var response = new WrittenAnswerResponseEntity(Guid.NewGuid(), Guid.NewGuid(), responseId, "Answer");
        _mockResponseRepository.Setup(r => r.GetByIdAsync(responseId)).ReturnsAsync(response);

        // Act
        var result = await _hub.CreateFeedback(dto);

        // Assert
        Assert.NotEqual(Guid.Empty, result.Id);
        Assert.Equal(FeedbackStatus.PROVISIONAL, result.Status);
        _mockFeedbackRepository.Verify(r => r.AddAsync(It.IsAny<FeedbackEntity>()), Times.Once);
    }

    [Fact]
    public async Task CreateFeedback_NonExistentResponse_ThrowsHubException()
    {
        // Arrange
        var dto = new InternalCreateFeedbackDto
        {
            ResponseId = Guid.NewGuid(),
            Text = "Feedback"
        };
        _mockResponseRepository.Setup(r => r.GetByIdAsync(dto.ResponseId)).ReturnsAsync((ResponseEntity?)null);

        // Act & Assert
        await Assert.ThrowsAsync<HubException>(() => _hub.CreateFeedback(dto));
    }

    [Fact]
    public async Task CreateFeedback_NoTextOrMarks_ThrowsHubException()
    {
        // Arrange
        var responseId = Guid.NewGuid();
        var dto = new InternalCreateFeedbackDto
        {
            ResponseId = responseId,
            Text = null,
            Marks = null
        };
        var response = new WrittenAnswerResponseEntity(Guid.NewGuid(), Guid.NewGuid(), responseId, "Answer");
        _mockResponseRepository.Setup(r => r.GetByIdAsync(responseId)).ReturnsAsync(response);

        // Act & Assert
        await Assert.ThrowsAsync<HubException>(() => _hub.CreateFeedback(dto));
    }

    [Fact]
    public async Task GetAllFeedbacks_ReturnsFromRepository()
    {
        // Arrange
        var feedbacks = new List<FeedbackEntity>
        {
            new(Guid.NewGuid(), Guid.NewGuid(), "Good", 5),
            new(Guid.NewGuid(), Guid.NewGuid(), "Excellent", 10)
        };
        _mockFeedbackRepository.Setup(r => r.GetAllAsync()).ReturnsAsync(feedbacks);

        // Act
        var result = await _hub.GetAllFeedbacks();

        // Assert
        Assert.Equal(2, result.Count);
        _mockFeedbackRepository.Verify(r => r.GetAllAsync(), Times.Once);
    }

    [Fact]
    public async Task UpdateFeedback_CallsRepository()
    {
        // Arrange
        var feedbackId = Guid.NewGuid();
        var existingFeedback = new FeedbackEntity(feedbackId, Guid.NewGuid(), "Original", 5) { Status = FeedbackStatus.PROVISIONAL };
        var updatedFeedback = new FeedbackEntity(feedbackId, Guid.NewGuid(), "Updated", 7);
        
        _mockFeedbackRepository.Setup(r => r.GetByIdAsync(feedbackId)).ReturnsAsync(existingFeedback);

        // Act
        await _hub.UpdateFeedback(updatedFeedback);

        // Assert
        _mockFeedbackRepository.Verify(r => r.UpdateAsync(existingFeedback), Times.Once);
        Assert.Equal("Updated", existingFeedback.Text);
        Assert.Equal(7, existingFeedback.Marks);
    }

    [Fact]
    public async Task ApproveFeedback_ValidProvisional_SetsReadyAndDispatches()
    {
        // Arrange
        var feedbackId = Guid.NewGuid();
        var responseId = Guid.NewGuid();
        var deviceId = Guid.NewGuid();
        var feedback = new FeedbackEntity(feedbackId, responseId, "Text", 5) { Status = FeedbackStatus.PROVISIONAL };
        var response = new WrittenAnswerResponseEntity(Guid.NewGuid(), Guid.NewGuid(), deviceId, "Answer");

        _mockFeedbackRepository.Setup(r => r.GetByIdAsync(feedbackId)).ReturnsAsync(feedback);
        _mockResponseRepository.Setup(r => r.GetByIdAsync(responseId)).ReturnsAsync(response);

        // Act
        await _hub.ApproveFeedback(feedbackId);

        // Assert
        Assert.Equal(FeedbackStatus.READY, feedback.Status);
        _mockFeedbackRepository.Verify(r => r.UpdateAsync(feedback), Times.Once);
        _mockTcpPairingService.Verify(s => s.SendReturnFeedbackAsync(deviceId.ToString(), It.IsAny<IEnumerable<Guid>>()), Times.Once);
    }

    [Fact]
    public async Task ApproveFeedback_NotFound_ThrowsHubException()
    {
        // Arrange
        var feedbackId = Guid.NewGuid();
        _mockFeedbackRepository.Setup(r => r.GetByIdAsync(feedbackId)).ReturnsAsync((FeedbackEntity?)null);

        // Act & Assert
        await Assert.ThrowsAsync<HubException>(() => _hub.ApproveFeedback(feedbackId));
    }

    [Fact]
    public async Task ApproveFeedback_NotProvisional_ThrowsHubException()
    {
        // Arrange
        var feedbackId = Guid.NewGuid();
        var feedback = new FeedbackEntity(feedbackId, Guid.NewGuid(), "Text", 5) { Status = FeedbackStatus.READY };
        _mockFeedbackRepository.Setup(r => r.GetByIdAsync(feedbackId)).ReturnsAsync(feedback);

        // Act & Assert
        await Assert.ThrowsAsync<HubException>(() => _hub.ApproveFeedback(feedbackId));
    }

    [Fact]
    public async Task RetryFeedbackDispatch_ValidReady_Dispatches()
    {
        // Arrange
        var feedbackId = Guid.NewGuid();
        var responseId = Guid.NewGuid();
        var deviceId = Guid.NewGuid();
        var feedback = new FeedbackEntity(feedbackId, responseId, "Text", 5) { Status = FeedbackStatus.READY };
        var response = new WrittenAnswerResponseEntity(Guid.NewGuid(), Guid.NewGuid(), deviceId, "Answer");

        _mockFeedbackRepository.Setup(r => r.GetByIdAsync(feedbackId)).ReturnsAsync(feedback);
        _mockResponseRepository.Setup(r => r.GetByIdAsync(responseId)).ReturnsAsync(response);

        // Act
        await _hub.RetryFeedbackDispatch(feedbackId);

        // Assert
        _mockTcpPairingService.Verify(s => s.SendReturnFeedbackAsync(deviceId.ToString(), It.IsAny<IEnumerable<Guid>>()), Times.Once);
    }

    [Fact]
    public async Task RetryFeedbackDispatch_NotReady_ThrowsHubException()
    {
        // Arrange
        var feedbackId = Guid.NewGuid();
        var feedback = new FeedbackEntity(feedbackId, Guid.NewGuid(), "Text", 5) { Status = FeedbackStatus.PROVISIONAL };
        _mockFeedbackRepository.Setup(r => r.GetByIdAsync(feedbackId)).ReturnsAsync(feedback);

        // Act & Assert
        await Assert.ThrowsAsync<HubException>(() => _hub.RetryFeedbackDispatch(feedbackId));
    }

    [Fact]
    public async Task UpdateFeedback_NonProvisionalStatus_ThrowsHubException()
    {
        // Arrange - Per §6A(7)(b)(ii): Only PROVISIONAL feedback can be edited
        var feedbackId = Guid.NewGuid();
        var existingFeedback = new FeedbackEntity(feedbackId, Guid.NewGuid(), "Text", 5) { Status = FeedbackStatus.READY };
        var updatedFeedback = new FeedbackEntity(feedbackId, Guid.NewGuid(), "Updated", 7);
        
        _mockFeedbackRepository.Setup(r => r.GetByIdAsync(feedbackId)).ReturnsAsync(existingFeedback);

        // Act & Assert
        var exception = await Assert.ThrowsAsync<HubException>(() => _hub.UpdateFeedback(updatedFeedback));
        Assert.Contains("PROVISIONAL", exception.Message);
        _mockFeedbackRepository.Verify(r => r.UpdateAsync(It.IsAny<FeedbackEntity>()), Times.Never);
    }

    [Fact]
    public async Task UpdateFeedback_DeliveredStatus_ThrowsHubException()
    {
        // Arrange - Per §6A(7)(b)(ii): DELIVERED feedback cannot be edited
        var feedbackId = Guid.NewGuid();
        var existingFeedback = new FeedbackEntity(feedbackId, Guid.NewGuid(), "Text", 5) { Status = FeedbackStatus.DELIVERED };
        var updatedFeedback = new FeedbackEntity(feedbackId, Guid.NewGuid(), "Updated", 7);
        
        _mockFeedbackRepository.Setup(r => r.GetByIdAsync(feedbackId)).ReturnsAsync(existingFeedback);

        // Act & Assert
        await Assert.ThrowsAsync<HubException>(() => _hub.UpdateFeedback(updatedFeedback));
    }

    [Fact]
    public async Task UpdateFeedback_BothTextAndMarksNull_ThrowsHubException()
    {
        // Arrange - Per Validation Rules §2F(1)(b): at least one of Text or Marks must be provided
        var feedbackId = Guid.NewGuid();
        var existingFeedback = new FeedbackEntity(feedbackId, Guid.NewGuid(), "Text", 5) { Status = FeedbackStatus.PROVISIONAL };
        
        // Use parameterless constructor and object initializer to simulate deserialized entity that bypassed validation
        var updatedFeedback = new FeedbackEntity { Id = feedbackId, Text = null, Marks = null };
        
        _mockFeedbackRepository.Setup(r => r.GetByIdAsync(feedbackId)).ReturnsAsync(existingFeedback);

        // Act & Assert
        var exception = await Assert.ThrowsAsync<HubException>(() => _hub.UpdateFeedback(updatedFeedback));
        Assert.Contains("2F(1)(b)", exception.Message);
        _mockFeedbackRepository.Verify(r => r.UpdateAsync(It.IsAny<FeedbackEntity>()), Times.Never);
    }

    [Fact]
    public async Task UpdateFeedback_EmptyTextAndNullMarks_ThrowsHubException()
    {
        // Arrange - Empty/whitespace text should also be rejected
        var feedbackId = Guid.NewGuid();
        var existingFeedback = new FeedbackEntity(feedbackId, Guid.NewGuid(), "Text", 5) { Status = FeedbackStatus.PROVISIONAL };
        
        // Use parameterless constructor and object initializer to simulate deserialized entity that bypassed validation
        var updatedFeedback = new FeedbackEntity { Id = feedbackId, Text = "   ", Marks = null };
        
        _mockFeedbackRepository.Setup(r => r.GetByIdAsync(feedbackId)).ReturnsAsync(existingFeedback);

        // Act & Assert
        await Assert.ThrowsAsync<HubException>(() => _hub.UpdateFeedback(updatedFeedback));
    }

    [Fact]
    public async Task UpdateFeedback_TextOnlyProvided_Succeeds()
    {
        // Arrange - Text only is valid per §2F(1)(b)
        var feedbackId = Guid.NewGuid();
        var existingFeedback = new FeedbackEntity(feedbackId, Guid.NewGuid(), "Original", 5) { Status = FeedbackStatus.PROVISIONAL };
        var updatedFeedback = new FeedbackEntity(feedbackId, Guid.NewGuid(), "Updated text", null);
        
        _mockFeedbackRepository.Setup(r => r.GetByIdAsync(feedbackId)).ReturnsAsync(existingFeedback);

        // Act
        await _hub.UpdateFeedback(updatedFeedback);

        // Assert
        _mockFeedbackRepository.Verify(r => r.UpdateAsync(existingFeedback), Times.Once);
        Assert.Equal("Updated text", existingFeedback.Text);
        Assert.Null(existingFeedback.Marks);
    }

    [Fact]
    public async Task UpdateFeedback_MarksOnlyProvided_Succeeds()
    {
        // Arrange - Marks only is valid per §2F(1)(b)
        var feedbackId = Guid.NewGuid();
        var existingFeedback = new FeedbackEntity(feedbackId, Guid.NewGuid(), "Original", 5) { Status = FeedbackStatus.PROVISIONAL };
        var updatedFeedback = new FeedbackEntity(feedbackId, Guid.NewGuid(), null, 10);
        
        _mockFeedbackRepository.Setup(r => r.GetByIdAsync(feedbackId)).ReturnsAsync(existingFeedback);

        // Act
        await _hub.UpdateFeedback(updatedFeedback);

        // Assert
        _mockFeedbackRepository.Verify(r => r.UpdateAsync(existingFeedback), Times.Once);
        Assert.Null(existingFeedback.Text);
        Assert.Equal(10, existingFeedback.Marks);
    }
    [Fact]
    public async Task DeleteFeedback_ProvisionalStatus_DeletesSuccessfully()
    {
        // Arrange - Per §6A(7)(a)(ii): PROVISIONAL feedback can be deleted
        var feedbackId = Guid.NewGuid();
        var feedback = new FeedbackEntity(feedbackId, Guid.NewGuid(), "Text", 5) { Status = FeedbackStatus.PROVISIONAL };
        
        _mockFeedbackRepository.Setup(r => r.GetByIdAsync(feedbackId)).ReturnsAsync(feedback);
        _mockFeedbackRepository.Setup(r => r.DeleteAsync(feedbackId)).Returns(Task.CompletedTask);

        // Act
        await _hub.DeleteFeedback(feedbackId);

        // Assert
        _mockFeedbackRepository.Verify(r => r.DeleteAsync(feedbackId), Times.Once);
    }

    [Fact]
    public async Task DeleteFeedback_ReadyStatus_ThrowsHubException()
    {
        // Arrange - Non-PROVISIONAL feedback cannot be deleted
        var feedbackId = Guid.NewGuid();
        var feedback = new FeedbackEntity(feedbackId, Guid.NewGuid(), "Text", 5) { Status = FeedbackStatus.READY };
        
        _mockFeedbackRepository.Setup(r => r.GetByIdAsync(feedbackId)).ReturnsAsync(feedback);

        // Act & Assert
        var exception = await Assert.ThrowsAsync<HubException>(() => _hub.DeleteFeedback(feedbackId));
        Assert.Contains("PROVISIONAL", exception.Message);
        _mockFeedbackRepository.Verify(r => r.DeleteAsync(It.IsAny<Guid>()), Times.Never);
    }

    [Fact]
    public async Task DeleteFeedback_DeliveredStatus_ThrowsHubException()
    {
        // Arrange - DELIVERED feedback cannot be deleted
        var feedbackId = Guid.NewGuid();
        var feedback = new FeedbackEntity(feedbackId, Guid.NewGuid(), "Text", 5) { Status = FeedbackStatus.DELIVERED };
        
        _mockFeedbackRepository.Setup(r => r.GetByIdAsync(feedbackId)).ReturnsAsync(feedback);

        // Act & Assert
        await Assert.ThrowsAsync<HubException>(() => _hub.DeleteFeedback(feedbackId));
        _mockFeedbackRepository.Verify(r => r.DeleteAsync(It.IsAny<Guid>()), Times.Never);
    }

    [Fact]
    public async Task DeleteFeedback_NotFound_ThrowsHubException()
    {
        // Arrange
        var feedbackId = Guid.NewGuid();
        _mockFeedbackRepository.Setup(r => r.GetByIdAsync(feedbackId)).ReturnsAsync((FeedbackEntity?)null);

        // Act & Assert
        var exception = await Assert.ThrowsAsync<HubException>(() => _hub.DeleteFeedback(feedbackId));
        Assert.Contains("not found", exception.Message);
    }

    #endregion

    #region Response Operations Tests

    [Fact]
    public async Task GetAllResponses_ReturnsFromRepository()
    {
        // Arrange
        var responses = new List<ResponseEntity>
        {
            new WrittenAnswerResponseEntity(Guid.NewGuid(), Guid.NewGuid(), Guid.NewGuid(), "Answer 1"),
            new MultipleChoiceResponseEntity(Guid.NewGuid(), Guid.NewGuid(), Guid.NewGuid(), 2)
        };
        _mockResponseRepository.Setup(r => r.GetAllAsync()).ReturnsAsync(responses);

        // Act
        var result = await _hub.GetAllResponses();

        // Assert
        Assert.Equal(2, result.Count);
        _mockResponseRepository.Verify(r => r.GetAllAsync(), Times.Once);
    }

    [Fact]
    public async Task GetResponsesUnderQuestion_ReturnsFromRepository()
    {
        // Arrange
        var questionId = Guid.NewGuid();
        var responses = new List<ResponseEntity>
        {
            new WrittenAnswerResponseEntity(Guid.NewGuid(), questionId, Guid.NewGuid(), "Answer")
        };
        _mockResponseRepository.Setup(r => r.GetByQuestionIdAsync(questionId)).ReturnsAsync(responses);

        // Act
        var result = await _hub.GetResponsesUnderQuestion(questionId);

        // Assert
        Assert.Single(result);
        _mockResponseRepository.Verify(r => r.GetByQuestionIdAsync(questionId), Times.Once);
    }

    #endregion

    #region Generic Runtime Dependency Tests

    [Fact]
    public async Task CheckRuntimeDependencyAvailability_CallsRegistryAndManager()
    {
        // Arrange
        var dependencyId = "test-dep";
        var mockManager = new Mock<RuntimeDependencyManagerBase>();
        mockManager.Setup(m => m.CheckDependencyAvailabilityAsync()).ReturnsAsync(true);
        _mockRuntimeDependencyRegistry.Setup(r => r.GetManager(dependencyId)).Returns(mockManager.Object);

        // Act
        var result = await _hub.CheckRuntimeDependencyAvailability(dependencyId);

        // Assert
        Assert.True(result);
        _mockRuntimeDependencyRegistry.Verify(r => r.GetManager(dependencyId), Times.Once);
        mockManager.Verify(m => m.CheckDependencyAvailabilityAsync(), Times.Once);
    }

    [Fact]
    public async Task InstallRuntimeDependency_CallsRegistryAndManager()
    {
        // Arrange
        var dependencyId = "test-dep";
        var fakeManager = new FakeRuntimeDependencyManager();
        _mockRuntimeDependencyRegistry.Setup(r => r.GetManager(dependencyId)).Returns(fakeManager);
        
        var mockClientProxy = new Mock<ISingleClientProxy>();
        var mockClients = new Mock<IHubCallerClients>();
        mockClients.Setup(c => c.Caller).Returns(mockClientProxy.Object);
        _hub.Clients = mockClients.Object;

        // Act
        var result = await _hub.InstallRuntimeDependency(dependencyId);

        // Assert
        Assert.True(result);
        _mockRuntimeDependencyRegistry.Verify(r => r.GetManager(dependencyId), Times.Once);
    }

    private class FakeRuntimeDependencyManager : RuntimeDependencyManagerBase
    {
        public override string DependencyId => "test-dep";
        public override Task<bool> CheckDependencyAvailabilityAsync() => Task.FromResult(true);
        protected override Task DownloadDependencyAsync(IProgress<Main.Models.RuntimeDependencyProgress> progress) => Task.CompletedTask;
        protected override Task VerifyDownloadAsync(IProgress<Main.Models.RuntimeDependencyProgress> progress) => Task.CompletedTask;
        protected override Task PerformInstallDependencyAsync(IProgress<Main.Models.RuntimeDependencyProgress> progress) => Task.CompletedTask;
        public override Task<bool> UninstallDependencyAsync() => Task.FromResult(true);
        protected override Task<IDependencyService> ProvideDependencyServiceAsync() => Task.FromResult<IDependencyService>(null!);
    }

    #endregion

    #region Configuration Methods Tests - NetworkingAPISpec §1(1)(o)

    [Fact]
    public async Task GetBaseConfiguration_ReturnsBaseConfig()
    {
        // Arrange
        var expectedConfig = new ConfigurationEntity(
            id: Guid.NewGuid(),
            textSize: 12,
            feedbackStyle: FeedbackStyle.IMMEDIATE,
            ttsEnabled: true,
            aiScaffoldingEnabled: true,
            summarisationEnabled: true,
            mascotSelection: MascotSelection.MASCOT1);

        _mockConfigurationService
            .Setup(s => s.GetDefaultsAsync())
            .ReturnsAsync(expectedConfig);

        // Act
        var result = await _hub.GetBaseConfiguration();

        // Assert
        Assert.NotNull(result);
        Assert.Equal(expectedConfig.TextSize, result.TextSize);
        Assert.Equal(expectedConfig.FeedbackStyle, result.FeedbackStyle);
        Assert.Equal(expectedConfig.TtsEnabled, result.TtsEnabled);
        _mockConfigurationService.Verify(s => s.GetDefaultsAsync(), Times.Once);
    }

    [Fact]
    public async Task UpdateBaseConfiguration_NullConfig_ThrowsHubException()
    {
        // Act & Assert
        var ex = await Assert.ThrowsAsync<HubException>(() => _hub.UpdateBaseConfiguration(null!));
        Assert.Contains("cannot be null", ex.Message);
    }

    [Fact]
    public async Task UpdateBaseConfiguration_UpdatesConfigAndRemovesMatchingOverrides()
    {
        // Arrange
        var deviceId1 = Guid.NewGuid();
        var deviceId2 = Guid.NewGuid();

        var newBaseConfig = new ConfigurationEntity(
            id: Guid.NewGuid(),
            textSize: 14,
            feedbackStyle: FeedbackStyle.NEUTRAL,
            ttsEnabled: false,
            aiScaffoldingEnabled: true,
            summarisationEnabled: true,
            mascotSelection: MascotSelection.MASCOT2);

        var pairedDevices = new List<PairedDeviceEntity>
        {
            new PairedDeviceEntity { DeviceId = deviceId1 },
            new PairedDeviceEntity { DeviceId = deviceId2 }
        };

        // Device 1 override: textSize=14 (matches new base), ttsEnabled=false (matches new base)
        var device1Override = new ConfigurationOverride
        {
            TextSize = 14,
            FeedbackStyle = FeedbackStyle.IMMEDIATE,
            TtsEnabled = false,
            AiScaffoldingEnabled = null,
            SummarisationEnabled = null,
            MascotSelection = null
        };

        // Device 2 override: all match new base (should be removed entirely)
        var device2Override = new ConfigurationOverride
        {
            TextSize = 14,
            FeedbackStyle = FeedbackStyle.NEUTRAL,
            TtsEnabled = false,
            AiScaffoldingEnabled = true,
            SummarisationEnabled = true,
            MascotSelection = MascotSelection.MASCOT2
        };

        _mockConfigurationService
            .Setup(s => s.UpdateDefaultsAsync(It.IsAny<ConfigurationEntity>()))
            .ReturnsAsync(newBaseConfig);

        _mockDeviceRegistryService
            .Setup(s => s.GetAllAsync())
            .ReturnsAsync(pairedDevices);

        _mockConfigurationService
            .Setup(s => s.GetOverride(deviceId1))
            .Returns(device1Override);

        _mockConfigurationService
            .Setup(s => s.GetOverride(deviceId2))
            .Returns(device2Override);

        // Act
        await _hub.UpdateBaseConfiguration(newBaseConfig);

        // Assert
        _mockConfigurationService.Verify(s => s.UpdateDefaultsAsync(newBaseConfig), Times.Once);
        _mockDeviceRegistryService.Verify(s => s.GetAllAsync(), Times.Once);

        // Device 1: SetOverride should be called with only FeedbackStyle override remaining
        _mockConfigurationService.Verify(s => s.SetOverride(deviceId1, It.Is<ConfigurationOverride>(o =>
            o.TextSize == null &&
            o.FeedbackStyle == FeedbackStyle.IMMEDIATE &&
            o.TtsEnabled == null)), Times.Once);

        // Device 2: RemoveOverride should be called (all values match)
        _mockConfigurationService.Verify(s => s.RemoveOverride(deviceId2), Times.Once);
    }

    [Fact]
    public async Task UpdateBaseConfiguration_NoDevicesOverrides_UpdatesSuccessfully()
    {
        // Arrange
        var newBaseConfig = new ConfigurationEntity(
            id: Guid.NewGuid(),
            textSize: 15,
            feedbackStyle: FeedbackStyle.IMMEDIATE,
            ttsEnabled: true,
            aiScaffoldingEnabled: true,
            summarisationEnabled: true,
            mascotSelection: MascotSelection.MASCOT1);

        _mockConfigurationService
            .Setup(s => s.UpdateDefaultsAsync(It.IsAny<ConfigurationEntity>()))
            .ReturnsAsync(newBaseConfig);

        _mockDeviceRegistryService
            .Setup(s => s.GetAllAsync())
            .ReturnsAsync(new List<PairedDeviceEntity>());

        // Act
        await _hub.UpdateBaseConfiguration(newBaseConfig);

        // Assert
        _mockConfigurationService.Verify(s => s.UpdateDefaultsAsync(newBaseConfig), Times.Once);
        _mockDeviceRegistryService.Verify(s => s.GetAllAsync(), Times.Once);
    }

    [Fact]
    public async Task GetDeviceConfiguration_UnpairedDevice_ThrowsHubException()
    {
        // Arrange
        var deviceId = Guid.NewGuid();
        _mockDeviceRegistryService
            .Setup(s => s.IsDevicePairedAsync(deviceId))
            .ReturnsAsync(false);

        // Act & Assert
        var ex = await Assert.ThrowsAsync<HubException>(() => _hub.GetDeviceConfiguration(deviceId));
        Assert.Contains("not paired", ex.Message);
    }

    [Fact]
    public async Task GetDeviceConfiguration_PairedDevice_ReturnsCompiledConfig()
    {
        // Arrange
        var deviceId = Guid.NewGuid();

        var compiledConfig = new ConfigurationEntity(
            id: Guid.NewGuid(),
            textSize: 16,
            feedbackStyle: FeedbackStyle.NEUTRAL,
            ttsEnabled: false,
            aiScaffoldingEnabled: false,
            summarisationEnabled: true,
            mascotSelection: MascotSelection.MASCOT3);

        _mockDeviceRegistryService
            .Setup(s => s.IsDevicePairedAsync(deviceId))
            .ReturnsAsync(true);

        _mockConfigurationService
            .Setup(s => s.CompileConfigAsync(deviceId))
            .ReturnsAsync(compiledConfig);

        // Act
        var result = await _hub.GetDeviceConfiguration(deviceId);

        // Assert
        Assert.NotNull(result);
        Assert.Equal(compiledConfig.TextSize, result.TextSize);
        Assert.Equal(compiledConfig.FeedbackStyle, result.FeedbackStyle);
        Assert.False(result.TtsEnabled);
        _mockDeviceRegistryService.Verify(s => s.IsDevicePairedAsync(deviceId), Times.Once);
        _mockConfigurationService.Verify(s => s.CompileConfigAsync(deviceId), Times.Once);
    }

    [Fact]
    public async Task UpdateDeviceConfiguration_NullConfig_ThrowsHubException()
    {
        // Arrange
        var deviceId = Guid.NewGuid();

        // Act & Assert
        var ex = await Assert.ThrowsAsync<HubException>(() => 
            _hub.UpdateDeviceConfiguration(deviceId, null!));
        Assert.Contains("cannot be null", ex.Message);
    }

    [Fact]
    public async Task UpdateDeviceConfiguration_UnpairedDevice_ThrowsHubException()
    {
        // Arrange
        var deviceId = Guid.NewGuid();
        var config = new ConfigurationEntity(
            id: Guid.NewGuid(),
            textSize: 12,
            feedbackStyle: FeedbackStyle.IMMEDIATE,
            ttsEnabled: true,
            aiScaffoldingEnabled: true,
            summarisationEnabled: true,
            mascotSelection: MascotSelection.MASCOT1);

        _mockDeviceRegistryService
            .Setup(s => s.IsDevicePairedAsync(deviceId))
            .ReturnsAsync(false);

        // Act & Assert
        var ex = await Assert.ThrowsAsync<HubException>(() => 
            _hub.UpdateDeviceConfiguration(deviceId, config));
        Assert.Contains("not paired", ex.Message);
    }

    [Fact]
    public async Task UpdateDeviceConfiguration_AllValuesDifferFromBase_SetsOverrides()
    {
        // Arrange
        var deviceId = Guid.NewGuid();

        var baseConfig = new ConfigurationEntity(
            id: Guid.NewGuid(),
            textSize: 12,
            feedbackStyle: FeedbackStyle.IMMEDIATE,
            ttsEnabled: true,
            aiScaffoldingEnabled: true,
            summarisationEnabled: true,
            mascotSelection: MascotSelection.MASCOT1);

        var deviceConfig = new ConfigurationEntity(
            id: Guid.NewGuid(),
            textSize: 18,
            feedbackStyle: FeedbackStyle.NEUTRAL,
            ttsEnabled: false,
            aiScaffoldingEnabled: false,
            summarisationEnabled: false,
            mascotSelection: MascotSelection.MASCOT3);

        _mockDeviceRegistryService
            .Setup(s => s.IsDevicePairedAsync(deviceId))
            .ReturnsAsync(true);

        _mockConfigurationService
            .Setup(s => s.GetDefaultsAsync())
            .ReturnsAsync(baseConfig);

        // Act
        await _hub.UpdateDeviceConfiguration(deviceId, deviceConfig);

        // Assert
        _mockConfigurationService.Verify(s => s.SetOverride(deviceId, It.Is<ConfigurationOverride>(o =>
            o.TextSize == 18 &&
            o.FeedbackStyle == FeedbackStyle.NEUTRAL &&
            o.TtsEnabled == false &&
            o.AiScaffoldingEnabled == false &&
            o.SummarisationEnabled == false &&
            o.MascotSelection == MascotSelection.MASCOT3)), Times.Once);
    }

    [Fact]
    public async Task UpdateDeviceConfiguration_SomeValuesDiffer_SetsPartialOverrides()
    {
        // Arrange
        var deviceId = Guid.NewGuid();

        var baseConfig = new ConfigurationEntity(
            id: Guid.NewGuid(),
            textSize: 12,
            feedbackStyle: FeedbackStyle.IMMEDIATE,
            ttsEnabled: true,
            aiScaffoldingEnabled: true,
            summarisationEnabled: true,
            mascotSelection: MascotSelection.MASCOT1);

        // Device config: only textSize and feedbackStyle differ
        var deviceConfig = new ConfigurationEntity(
            id: Guid.NewGuid(),
            textSize: 20,
            feedbackStyle: FeedbackStyle.NEUTRAL,
            ttsEnabled: true,  // Same as base
            aiScaffoldingEnabled: true,  // Same as base
            summarisationEnabled: true,  // Same as base
            mascotSelection: MascotSelection.MASCOT1);  // Same as base

        _mockDeviceRegistryService
            .Setup(s => s.IsDevicePairedAsync(deviceId))
            .ReturnsAsync(true);

        _mockConfigurationService
            .Setup(s => s.GetDefaultsAsync())
            .ReturnsAsync(baseConfig);

        // Act
        await _hub.UpdateDeviceConfiguration(deviceId, deviceConfig);

        // Assert
        _mockConfigurationService.Verify(s => s.SetOverride(deviceId, It.Is<ConfigurationOverride>(o =>
            o.TextSize == 20 &&
            o.FeedbackStyle == FeedbackStyle.NEUTRAL &&
            o.TtsEnabled == null &&
            o.AiScaffoldingEnabled == null &&
            o.SummarisationEnabled == null &&
            o.MascotSelection == null)), Times.Once);
    }

    [Fact]
    public async Task UpdateDeviceConfiguration_NoValuesDiffer_RemovesOverrides()
    {
        // Arrange
        var deviceId = Guid.NewGuid();

        var baseConfig = new ConfigurationEntity(
            id: Guid.NewGuid(),
            textSize: 12,
            feedbackStyle: FeedbackStyle.IMMEDIATE,
            ttsEnabled: true,
            aiScaffoldingEnabled: true,
            summarisationEnabled: true,
            mascotSelection: MascotSelection.MASCOT1);

        // Device config: identical to base (no overrides)
        var deviceConfig = new ConfigurationEntity(
            id: Guid.NewGuid(),
            textSize: 12,
            feedbackStyle: FeedbackStyle.IMMEDIATE,
            ttsEnabled: true,
            aiScaffoldingEnabled: true,
            summarisationEnabled: true,
            mascotSelection: MascotSelection.MASCOT1);

        _mockDeviceRegistryService
            .Setup(s => s.IsDevicePairedAsync(deviceId))
            .ReturnsAsync(true);

        _mockConfigurationService
            .Setup(s => s.GetDefaultsAsync())
            .ReturnsAsync(baseConfig);

        // Act
        await _hub.UpdateDeviceConfiguration(deviceId, deviceConfig);

        // Assert
        _mockConfigurationService.Verify(s => s.RemoveOverride(deviceId), Times.Once);
        _mockConfigurationService.Verify(s => s.SetOverride(It.IsAny<Guid>(), It.IsAny<ConfigurationOverride>()), 
            Times.Never);
    }

    #endregion
}
