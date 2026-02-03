using Microsoft.AspNetCore.SignalR;
using Main.Models.Dtos;
using Main.Models.Entities;
using Main.Models.Entities.Materials;
using Main.Models.Entities.Questions;
using Main.Models.Enums;
using Main.Services.Repositories;
using Main.Services.Network;

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
    private readonly IQuestionService _questionService;
    private readonly ISourceDocumentService _sourceDocumentService;
    private readonly IAttachmentService _attachmentService;
    private readonly IUnitCollectionRepository _unitCollectionRepository;
    private readonly IUnitRepository _unitRepository;
    private readonly ILessonRepository _lessonRepository;
    private readonly IMaterialRepository _materialRepository;
    private readonly IQuestionRepository _questionRepository;
    private readonly ISourceDocumentRepository _sourceDocumentRepository;
    private readonly IAttachmentRepository _attachmentRepository;
    
    // Classroom dependencies - NetworkingAPISpec §1(1)(e)-(g)
    private readonly IUdpBroadcastService _udpBroadcastService;
    private readonly ITcpPairingService _tcpPairingService;
    private readonly IDeviceRegistryService _deviceRegistryService;
    private readonly IDeviceStatusCacheService _deviceStatusCacheService;
    private readonly IDistributionService _distributionService;

    public TeacherPortalHub(
        IUnitCollectionService unitCollectionService,
        IUnitService unitService,
        ILessonService lessonService,
        IMaterialService materialService,
        IQuestionService questionService,
        ISourceDocumentService sourceDocumentService,
        IAttachmentService attachmentService,
        IUnitCollectionRepository unitCollectionRepository,
        IUnitRepository unitRepository,
        ILessonRepository lessonRepository,
        IMaterialRepository materialRepository,
        IQuestionRepository questionRepository,
        ISourceDocumentRepository sourceDocumentRepository,
        IAttachmentRepository attachmentRepository,
        IUdpBroadcastService udpBroadcastService,
        ITcpPairingService tcpPairingService,
        IDeviceRegistryService deviceRegistryService,
        IDeviceStatusCacheService deviceStatusCacheService,
        IDistributionService distributionService)
    {
        _unitCollectionService = unitCollectionService ?? throw new ArgumentNullException(nameof(unitCollectionService));
        _unitService = unitService ?? throw new ArgumentNullException(nameof(unitService));
        _lessonService = lessonService ?? throw new ArgumentNullException(nameof(lessonService));
        _materialService = materialService ?? throw new ArgumentNullException(nameof(materialService));
        _questionService = questionService ?? throw new ArgumentNullException(nameof(questionService));
        _sourceDocumentService = sourceDocumentService ?? throw new ArgumentNullException(nameof(sourceDocumentService));
        _attachmentService = attachmentService ?? throw new ArgumentNullException(nameof(attachmentService));
        _unitCollectionRepository = unitCollectionRepository ?? throw new ArgumentNullException(nameof(unitCollectionRepository));
        _unitRepository = unitRepository ?? throw new ArgumentNullException(nameof(unitRepository));
        _lessonRepository = lessonRepository ?? throw new ArgumentNullException(nameof(lessonRepository));
        _materialRepository = materialRepository ?? throw new ArgumentNullException(nameof(materialRepository));
        _questionRepository = questionRepository ?? throw new ArgumentNullException(nameof(questionRepository));
        _sourceDocumentRepository = sourceDocumentRepository ?? throw new ArgumentNullException(nameof(sourceDocumentRepository));
        _attachmentRepository = attachmentRepository ?? throw new ArgumentNullException(nameof(attachmentRepository));
        _udpBroadcastService = udpBroadcastService ?? throw new ArgumentNullException(nameof(udpBroadcastService));
        _tcpPairingService = tcpPairingService ?? throw new ArgumentNullException(nameof(tcpPairingService));
        _deviceRegistryService = deviceRegistryService ?? throw new ArgumentNullException(nameof(deviceRegistryService));
        _deviceStatusCacheService = deviceStatusCacheService ?? throw new ArgumentNullException(nameof(deviceStatusCacheService));
        _distributionService = distributionService ?? throw new ArgumentNullException(nameof(distributionService));
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
    public async Task UpdateMaterial(InternalUpdateMaterialDto dto)
    {
        // Get existing material to determine concrete type (using repository directly)
        var existing = await _materialRepository.GetByIdAsync(dto.Id);
        if (existing == null)
            throw new HubException($"Material with ID {dto.Id} not found.");

        // Update properties
        existing.Title = dto.Title;
        existing.Content = dto.Content;
        existing.Metadata = dto.Metadata;
        existing.VocabularyTerms = dto.VocabularyTerms;
        existing.ReadingAge = dto.ReadingAge;
        existing.ActualAge = dto.ActualAge;
        existing.Timestamp = DateTime.UtcNow;

        await _materialRepository.UpdateAsync(existing);
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

    #region Question CRUD - NetworkingAPISpec §1(1)(d1)

    /// <summary>
    /// Creates a new question entity with an assigned UUID.
    /// Per NetworkingAPISpec §1(1)(d1)(i).
    /// </summary>
    public async Task<Guid> CreateQuestion(InternalCreateQuestionDto dto)
    {
        var id = Guid.NewGuid();
        var entity = CreateQuestionEntity(id, dto);
        var created = await _questionService.CreateQuestionAsync(entity);
        return created.Id;
    }

    /// <summary>
    /// Retrieves all questions associated with a material.
    /// Per NetworkingAPISpec §1(1)(d1)(ii).
    /// </summary>
    public async Task<List<InternalQuestionResponseDto>> GetQuestionsUnderMaterial(Guid materialId)
    {
        var questions = await _questionRepository.GetByMaterialIdAsync(materialId);
        return questions.Select(InternalQuestionResponseDto.FromEntity).ToList();
    }

    /// <summary>
    /// Updates a question entity.
    /// Per NetworkingAPISpec §1(1)(d1)(iii).
    /// </summary>
    public async Task UpdateQuestion(InternalUpdateQuestionDto dto)
    {
        var existing = await _questionRepository.GetByIdAsync(dto.Id);
        if (existing == null)
            throw new HubException($"Question with ID {dto.Id} not found.");

        // Check if the question type is changing - this requires delete/recreate
        if (existing.QuestionType != dto.QuestionType)
        {
            // Delete old question and create new one with same ID
            try
            {
                await _questionService.DeleteQuestionAsync(dto.Id);
                var newEntity = CreateQuestionEntity(dto.Id, new InternalCreateQuestionDto(
                    dto.MaterialId,
                    dto.QuestionType,
                    dto.QuestionText,
                    dto.Options,
                    dto.CorrectAnswerIndex,
                    dto.CorrectAnswer,
                    dto.MarkScheme,
                    dto.MaxScore));
                await _questionService.CreateQuestionAsync(newEntity);
            }
            catch
            {
                // Attempt to restore original question to avoid leaving the system in an inconsistent state
                await _questionService.CreateQuestionAsync(existing);
                throw;
            }
            return;
        }

        // Prevent changing the owning material for an existing question
        if (dto.MaterialId != existing.MaterialId)
            throw new HubException("MaterialId cannot be changed for an existing question. Create a new question for a different material.");

        // Update properties on existing entity
        existing.QuestionText = dto.QuestionText;
        existing.MaxScore = dto.MaxScore;
        
        // Update type-specific properties
        if (existing is MultipleChoiceQuestionEntity mcq && dto.QuestionType == QuestionType.MULTIPLE_CHOICE)
        {
            mcq.Options = dto.Options ?? new List<string>();
            mcq.CorrectAnswerIndex = dto.CorrectAnswerIndex;  // null = no correct answer
        }
        else if (existing is WrittenAnswerQuestionEntity waq && dto.QuestionType == QuestionType.WRITTEN_ANSWER)
        {
            waq.CorrectAnswer = dto.CorrectAnswer;  // null = auto-marking disabled
            waq.MarkScheme = dto.MarkScheme;  // null = no AI-marking
        }
        
        await _questionService.UpdateQuestionAsync(existing);
    }

    /// <summary>
    /// Deletes a question entity by ID.
    /// Per NetworkingAPISpec §1(1)(d1)(iv).
    /// </summary>
    public async Task DeleteQuestion(Guid id)
    {
        await _questionService.DeleteQuestionAsync(id);
    }

    /// <summary>
    /// Factory method to create concrete QuestionEntity based on type.
    /// </summary>
    private static QuestionEntity CreateQuestionEntity(Guid id, InternalCreateQuestionDto dto)
    {
        return dto.QuestionType switch
        {
            QuestionType.MULTIPLE_CHOICE => new MultipleChoiceQuestionEntity(
                id,
                dto.MaterialId,
                dto.QuestionText,
                dto.Options ?? new List<string>(),
                dto.CorrectAnswerIndex,  // null = no correct answer (auto-marking disabled)
                dto.MaxScore),
            QuestionType.WRITTEN_ANSWER => new WrittenAnswerQuestionEntity(
                id,
                dto.MaterialId,
                dto.QuestionText,
                dto.CorrectAnswer,  // null = auto-marking disabled
                dto.MarkScheme,     // Per §2E(1)(a) - for AI-marking
                dto.MaxScore),
            _ => throw new ArgumentException($"Unknown question type: {dto.QuestionType}", nameof(dto))
        };
    }

    #endregion

    #region SourceDocument CRUD - NetworkingAPISpec §1(1)(k)

    /// <summary>
    /// Creates a new source document entity with an assigned UUID.
    /// Per NetworkingAPISpec §1(1)(k)(i).
    /// </summary>
    public async Task<SourceDocumentEntity> CreateSourceDocument(InternalCreateSourceDocumentDto dto)
    {
        var entity = new SourceDocumentEntity(Guid.NewGuid(), dto.UnitCollectionId, dto.Transcript);
        return await _sourceDocumentService.CreateAsync(entity);
    }

    /// <summary>
    /// Retrieves all source document entities.
    /// Per NetworkingAPISpec §1(1)(k)(ii).
    /// </summary>
    public async Task<List<SourceDocumentEntity>> GetAllSourceDocuments()
    {
        var entities = await _sourceDocumentRepository.GetAllAsync();
        return entities.ToList();
    }

    /// <summary>
    /// Deletes a source document entity by ID.
    /// Per NetworkingAPISpec §1(1)(k)(iii).
    /// </summary>
    public async Task DeleteSourceDocument(Guid id)
    {
        await _sourceDocumentService.DeleteAsync(id);
    }

    #endregion

    #region Attachment CRUD - NetworkingAPISpec §1(1)(l)

    /// <summary>
    /// Creates a new attachment entity with an assigned UUID.
    /// Per NetworkingAPISpec §1(1)(l)(i).
    /// </summary>
    public async Task<Guid> CreateAttachment(InternalCreateAttachmentDto dto)
    {
        var entity = new AttachmentEntity(Guid.NewGuid(), dto.MaterialId, dto.FileBaseName, dto.FileExtension);
        var created = await _attachmentService.CreateAsync(entity);
        return created.Id;
    }

    /// <summary>
    /// Retrieves all attachments associated with a material.
    /// Per NetworkingAPISpec §1(1)(l)(ii).
    /// </summary>
    public async Task<List<AttachmentEntity>> GetAttachmentsUnderMaterial(Guid materialId)
    {
        var attachments = await _attachmentRepository.GetByMaterialIdAsync(materialId);
        return attachments.ToList();
    }

    /// <summary>
    /// Deletes an attachment entity by ID.
    /// Per NetworkingAPISpec §1(1)(l)(iii).
    /// </summary>
    public async Task DeleteAttachment(Guid id)
    {
        await _attachmentService.DeleteAsync(id);
    }

    #endregion

    #region Classroom Methods - NetworkingAPISpec §1(1)(e)-(g)

    /// <summary>
    /// Initiates device pairing by starting UDP broadcast and TCP listener.
    /// Per Pairing Process §2(1): Windows broadcasts UDP discovery message.
    /// Per Pairing Process §2(2)(b): Android responds via TCP PAIRING_REQUEST.
    /// </summary>
    public async Task PairDevices()
    {
        // 1. Start UDP broadcast per Pairing Process §2(1)
        await _udpBroadcastService.StartBroadcastingAsync(CancellationToken.None);
        
        // 2. Start TCP listener for PAIRING_REQUEST per Pairing Process §2(2)(b)
        await _tcpPairingService.StartListeningAsync(CancellationToken.None);
    }

    /// <summary>
    /// Stops the pairing process by stopping both UDP broadcast and TCP listener.
    /// </summary>
    public Task StopPairing()
    {
        _udpBroadcastService.StopBroadcasting();
        _tcpPairingService.StopListening();
        return Task.CompletedTask;
    }

    /// <summary>
    /// Retrieves all paired devices.
    /// Per NetworkingAPISpec §1(1)(e)(ii).
    /// </summary>
    public async Task<List<PairedDeviceEntity>> GetAllPairedDevices()
    {
        var devices = await _deviceRegistryService.GetAllAsync();
        return devices.ToList();
    }

    /// <summary>
    /// Retrieves all device statuses.
    /// Per NetworkingAPISpec §1(1)(e)(iii).
    /// </summary>
    public Task<List<DeviceStatusEntity>> GetAllDeviceStatuses()
    {
        var statuses = _deviceStatusCacheService.GetAllStatuses();
        return Task.FromResult(statuses.ToList());
    }

    /// <summary>
    /// Locks the specified devices.
    /// Per NetworkingAPISpec §1(1)(f)(i).
    /// </summary>
    public async Task LockDevices(List<Guid> deviceIds)
    {
        foreach (var deviceId in deviceIds)
        {
            await _tcpPairingService.SendLockScreenAsync(deviceId.ToString());
        }
    }

    /// <summary>
    /// Unlocks the specified devices.
    /// Per NetworkingAPISpec §1(1)(f)(ii).
    /// </summary>
    public async Task UnlockDevices(List<Guid> deviceIds)
    {
        foreach (var deviceId in deviceIds)
        {
            await _tcpPairingService.SendUnlockScreenAsync(deviceId.ToString());
        }
    }

    /// <summary>
    /// Deploys a material to the specified devices.
    /// Per NetworkingAPISpec §1(1)(g)(i).
    /// </summary>
    public async Task DeployMaterial(Guid materialId, List<Guid> deviceIds)
    {
        // Step 1: Assign material to each target device per Session Interaction §3
        foreach (var deviceId in deviceIds)
        {
            await _distributionService.AssignMaterialsToDeviceAsync(deviceId, new[] { materialId });
        }

        // Step 2: Send TCP trigger to each device
        foreach (var deviceId in deviceIds)
        {
            await _tcpPairingService.SendDistributeMaterialAsync(deviceId.ToString());
        }
    }

    /// <summary>
    /// Unpairs the specified devices.
    /// Per FrontendWorkflowSpecifications §5A(5).
    /// </summary>
    public async Task UnpairDevices(List<Guid> deviceIds)
    {
        foreach (var deviceId in deviceIds)
        {
            await _tcpPairingService.SendUnpairAsync(deviceId.ToString());
            // Registry removal is handled by TcpPairingService.SendUnpairAsync per Pairing Process §3(2)
            _deviceStatusCacheService.RemoveDevice(deviceId);
        }
    }

    /// <summary>
    /// Updates a paired device entity (e.g., for renaming).
    /// Per FrontendWorkflowSpec §5B(4).
    /// </summary>
    public async Task UpdatePairedDevice(PairedDeviceEntity entity)
    {
        await _deviceRegistryService.UpdateAsync(entity);
    }

    #endregion
}

