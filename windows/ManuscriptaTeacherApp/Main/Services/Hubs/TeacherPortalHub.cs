using Microsoft.AspNetCore.SignalR;
using Main.Models.Dtos;
using Main.Models.Entities;
using Main.Models.Entities.Materials;
using Main.Models.Enums;
using Main.Services.Repositories;

namespace Main.Services.Hubs;

/// <summary>
/// SignalR hub providing CRUD operations for the UI component.
/// Per NetworkingAPISpec §1(1)(a)-(d).
/// </summary>
public class TeacherPortalHub : Hub
{
    private readonly IUnitCollectionService _unitCollectionService;
    private readonly IUnitService _unitService;
    private readonly ILessonService _lessonService;
    private readonly IMaterialService _materialService;
    private readonly IUnitCollectionRepository _unitCollectionRepository;
    private readonly IUnitRepository _unitRepository;
    private readonly ILessonRepository _lessonRepository;
    private readonly IMaterialRepository _materialRepository;

    public TeacherPortalHub(
        IUnitCollectionService unitCollectionService,
        IUnitService unitService,
        ILessonService lessonService,
        IMaterialService materialService,
        IUnitCollectionRepository unitCollectionRepository,
        IUnitRepository unitRepository,
        ILessonRepository lessonRepository,
        IMaterialRepository materialRepository)
    {
        _unitCollectionService = unitCollectionService ?? throw new ArgumentNullException(nameof(unitCollectionService));
        _unitService = unitService ?? throw new ArgumentNullException(nameof(unitService));
        _lessonService = lessonService ?? throw new ArgumentNullException(nameof(lessonService));
        _materialService = materialService ?? throw new ArgumentNullException(nameof(materialService));
        _unitCollectionRepository = unitCollectionRepository ?? throw new ArgumentNullException(nameof(unitCollectionRepository));
        _unitRepository = unitRepository ?? throw new ArgumentNullException(nameof(unitRepository));
        _lessonRepository = lessonRepository ?? throw new ArgumentNullException(nameof(lessonRepository));
        _materialRepository = materialRepository ?? throw new ArgumentNullException(nameof(materialRepository));
    }

    #region UnitCollection CRUD - NetworkingAPISpec §1(1)(a)

    /// <summary>
    /// Creates a new unit collection entity with an assigned UUID.
    /// Per NetworkingAPISpec §1(1)(a)(i).
    /// </summary>
    public async Task<UnitCollectionEntity> CreateUnitCollection(InternalCreateUnitCollectionDto dto)
    {
        var entity = new UnitCollectionEntity(Guid.NewGuid(), dto.Title);
        return await _unitCollectionService.CreateAsync(entity);
    }

    /// <summary>
    /// Retrieves all unit collection entities.
    /// Per NetworkingAPISpec §1(1)(a)(ii).
    /// </summary>
    public async Task<List<UnitCollectionEntity>> GetAllUnitCollections()
    {
        var entities = await _unitCollectionRepository.GetAllAsync();
        return entities.ToList();
    }

    /// <summary>
    /// Updates a unit collection entity.
    /// Per NetworkingAPISpec §1(1)(a)(iii).
    /// </summary>
    public async Task UpdateUnitCollection(UnitCollectionEntity updated)
    {
        await _unitCollectionService.UpdateAsync(updated);
    }

    /// <summary>
    /// Deletes a unit collection entity by ID.
    /// Per NetworkingAPISpec §1(1)(a)(iv).
    /// </summary>
    public async Task DeleteUnitCollection(Guid id)
    {
        await _unitCollectionService.DeleteAsync(id);
    }

    #endregion

    #region Unit CRUD - NetworkingAPISpec §1(1)(b)

    /// <summary>
    /// Creates a new unit entity with an assigned UUID.
    /// Per NetworkingAPISpec §1(1)(b)(i).
    /// </summary>
    public async Task<UnitEntity> CreateUnit(InternalCreateUnitDto dto)
    {
        var entity = new UnitEntity(Guid.NewGuid(), dto.UnitCollectionId, dto.Title, dto.SourceDocuments);
        return await _unitService.CreateAsync(entity);
    }

    /// <summary>
    /// Retrieves all unit entities.
    /// Per NetworkingAPISpec §1(1)(b)(ii).
    /// </summary>
    public async Task<List<UnitEntity>> GetAllUnits()
    {
        var entities = await _unitRepository.GetAllAsync();
        return entities.ToList();
    }

    /// <summary>
    /// Updates a unit entity.
    /// Per NetworkingAPISpec §1(1)(b)(iii).
    /// </summary>
    public async Task UpdateUnit(UnitEntity updated)
    {
        await _unitService.UpdateAsync(updated);
    }

    /// <summary>
    /// Deletes a unit entity by ID.
    /// Per NetworkingAPISpec §1(1)(b)(iv).
    /// </summary>
    public async Task DeleteUnit(Guid id)
    {
        await _unitService.DeleteAsync(id);
    }

    #endregion

    #region Lesson CRUD - NetworkingAPISpec §1(1)(c)

    /// <summary>
    /// Creates a new lesson entity with an assigned UUID.
    /// Per NetworkingAPISpec §1(1)(c)(i).
    /// </summary>
    public async Task<LessonEntity> CreateLesson(InternalCreateLessonDto dto)
    {
        var entity = new LessonEntity(Guid.NewGuid(), dto.UnitId, dto.Title, dto.Description);
        return await _lessonService.CreateAsync(entity);
    }

    /// <summary>
    /// Retrieves all lesson entities.
    /// Per NetworkingAPISpec §1(1)(c)(ii).
    /// </summary>
    public async Task<List<LessonEntity>> GetAllLessons()
    {
        var entities = await _lessonRepository.GetAllAsync();
        return entities.ToList();
    }

    /// <summary>
    /// Updates a lesson entity.
    /// Per NetworkingAPISpec §1(1)(c)(iii).
    /// </summary>
    public async Task UpdateLesson(LessonEntity updated)
    {
        await _lessonService.UpdateAsync(updated);
    }

    /// <summary>
    /// Deletes a lesson entity by ID.
    /// Per NetworkingAPISpec §1(1)(c)(iv).
    /// </summary>
    public async Task DeleteLesson(Guid id)
    {
        await _lessonService.DeleteAsync(id);
    }

    #endregion

    #region Material CRUD - NetworkingAPISpec §1(1)(d)

    /// <summary>
    /// Creates a new material entity with an assigned UUID.
    /// Per NetworkingAPISpec §1(1)(d)(i).
    /// </summary>
    public async Task<MaterialEntity> CreateMaterial(InternalCreateMaterialDto dto)
    {
        var id = Guid.NewGuid();
        var entity = CreateMaterialEntity(id, dto);
        return await _materialService.CreateMaterialAsync(entity);
    }

    /// <summary>
    /// Retrieves all material entities.
    /// Per NetworkingAPISpec §1(1)(d)(ii).
    /// </summary>
    public async Task<List<MaterialEntity>> GetAllMaterials()
    {
        var entities = await _materialRepository.GetAllAsync();
        return entities.ToList();
    }

    /// <summary>
    /// Updates a material entity.
    /// Per NetworkingAPISpec §1(1)(d)(iii).
    /// </summary>
    public async Task UpdateMaterial(MaterialEntity updated)
    {
        await _materialService.UpdateMaterialAsync(updated);
    }

    /// <summary>
    /// Deletes a material entity by ID.
    /// Per NetworkingAPISpec §1(1)(d)(iv).
    /// </summary>
    public async Task DeleteMaterial(Guid id)
    {
        await _materialService.DeleteMaterialAsync(id);
    }

    /// <summary>
    /// Factory method to create the appropriate MaterialEntity subtype.
    /// </summary>
    private static MaterialEntity CreateMaterialEntity(Guid id, InternalCreateMaterialDto dto)
    {
        return dto.MaterialType switch
        {
            MaterialType.READING => new ReadingMaterialEntity(
                id, dto.LessonId, dto.Title, dto.Content, null, dto.Metadata, dto.VocabularyTerms, dto.ReadingAge, dto.ActualAge),
            MaterialType.WORKSHEET => new WorksheetMaterialEntity(
                id, dto.LessonId, dto.Title, dto.Content, null, dto.Metadata, dto.VocabularyTerms, dto.ReadingAge, dto.ActualAge),
            MaterialType.POLL => new PollMaterialEntity(
                id, dto.LessonId, dto.Title, dto.Content, null, dto.Metadata, dto.VocabularyTerms, dto.ReadingAge, dto.ActualAge),
            _ => throw new ArgumentException($"Unknown material type: {dto.MaterialType}", nameof(dto))
        };
    }

    #endregion
}

