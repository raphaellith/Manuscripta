using Microsoft.AspNetCore.SignalR;
using Main.Models.Dtos;
using Main.Models.Entities;
using Main.Models.Entities.Materials;
using Main.Models.Entities.Questions;
using Main.Models.Entities.Responses;
using Main.Models.Enums;
using Main.Services.Repositories;
using Main.Services.GenAI;
using Main.Services.Network;
using Main.Services.RuntimeDependencies;
using Main.Models;

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

    // Feedback dependencies - NetworkingAPISpec §1(1)(h)
    private readonly IFeedbackRepository _feedbackRepository;
    private readonly IResponseRepository _responseRepository;
    private readonly ILogger<TeacherPortalHub> _logger;
    private readonly IMaterialPdfService _materialPdfService;

    // reMarkable dependencies - NetworkingAPISpec §1(1)(n)
    private readonly IRmapiService _rmapiService;
    private readonly IReMarkableDeviceRepository _remarkableDeviceRepository;
    private readonly IReMarkableDeploymentService _remarkableDeploymentService;
    private readonly IRuntimeDependencyRegistry _runtimeDependencyRegistry;

    // Configuration dependencies - NetworkingAPISpec §1(1)(o)
    private readonly IConfigurationService _configurationService;

    private readonly IMaterialGenerationService _materialGenerationService;
    private readonly IContentModificationService _contentModificationService;
    private readonly IEmbeddingStatusService _embeddingStatusService;
    private readonly FeedbackQueueService _feedbackQueueService;
    private readonly IEmbeddingService _documentEmbeddingService;
    private readonly OllamaClientService _ollamaClient;

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
        IDistributionService distributionService,
        IFeedbackRepository feedbackRepository,
        IResponseRepository responseRepository,
        ILogger<TeacherPortalHub> logger,
        IMaterialPdfService materialPdfService,
        IRmapiService rmapiService,
        IReMarkableDeviceRepository remarkableDeviceRepository,
        IReMarkableDeploymentService remarkableDeploymentService,
        IRuntimeDependencyRegistry runtimeDependencyRegistry,
        IConfigurationService configurationService,
        IMaterialGenerationService materialGenerationService,
        IContentModificationService contentModificationService,
        IEmbeddingStatusService embeddingStatusService,
        FeedbackQueueService feedbackQueueService,
        IEmbeddingService documentEmbeddingService,
        OllamaClientService ollamaClient)
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
        _feedbackRepository = feedbackRepository ?? throw new ArgumentNullException(nameof(feedbackRepository));
        _responseRepository = responseRepository ?? throw new ArgumentNullException(nameof(responseRepository));
        _materialGenerationService = materialGenerationService ?? throw new ArgumentNullException(nameof(materialGenerationService));
        _contentModificationService = contentModificationService ?? throw new ArgumentNullException(nameof(contentModificationService));
        _embeddingStatusService = embeddingStatusService ?? throw new ArgumentNullException(nameof(embeddingStatusService));
        _feedbackQueueService = feedbackQueueService ?? throw new ArgumentNullException(nameof(feedbackQueueService));
        _documentEmbeddingService = documentEmbeddingService ?? throw new ArgumentNullException(nameof(documentEmbeddingService));
        _tcpPairingService = tcpPairingService ?? throw new ArgumentNullException(nameof(tcpPairingService));
        _udpBroadcastService = udpBroadcastService ?? throw new ArgumentNullException(nameof(udpBroadcastService));
        _tcpPairingService = tcpPairingService ?? throw new ArgumentNullException(nameof(tcpPairingService));
        _deviceRegistryService = deviceRegistryService ?? throw new ArgumentNullException(nameof(deviceRegistryService));
        _deviceStatusCacheService = deviceStatusCacheService ?? throw new ArgumentNullException(nameof(deviceStatusCacheService));
        _distributionService = distributionService ?? throw new ArgumentNullException(nameof(distributionService));
        _feedbackRepository = feedbackRepository ?? throw new ArgumentNullException(nameof(feedbackRepository));
        _responseRepository = responseRepository ?? throw new ArgumentNullException(nameof(responseRepository));
        _logger = logger ?? throw new ArgumentNullException(nameof(logger));
        _materialPdfService = materialPdfService ?? throw new ArgumentNullException(nameof(materialPdfService));
        _rmapiService = rmapiService ?? throw new ArgumentNullException(nameof(rmapiService));
        _remarkableDeviceRepository = remarkableDeviceRepository ?? throw new ArgumentNullException(nameof(remarkableDeviceRepository));
        _remarkableDeploymentService = remarkableDeploymentService ?? throw new ArgumentNullException(nameof(remarkableDeploymentService));
        _runtimeDependencyRegistry = runtimeDependencyRegistry ?? throw new ArgumentNullException(nameof(runtimeDependencyRegistry));
        _configurationService = configurationService ?? throw new ArgumentNullException(nameof(configurationService));
        _ollamaClient = ollamaClient ?? throw new ArgumentNullException(nameof(ollamaClient));
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
        var entity = new UnitEntity(Guid.NewGuid(), dto.UnitCollectionId, dto.Title);
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
    /// Updates a source document entity.
    /// Per GenAISpec.md §3A(3) - triggers re-indexing.
    /// </summary>
    public async Task UpdateSourceDocument(SourceDocumentEntity updated)
    {
        await _sourceDocumentService.UpdateAsync(updated);
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

    #region PDF Generation - NetworkingAPISpec §1(1)(m)

    /// <summary>
    /// Generates a PDF document for the specified material.
    /// Per NetworkingAPISpec §1(1)(m)(i).
    /// </summary>
    public async Task<byte[]> GenerateMaterialPdf(Guid materialId)
    {
        return await _materialPdfService.GeneratePdfAsync(materialId);
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

        // Step 2: Send TCP trigger to each device with material IDs for per-entity ACK tracking
        // Per API Contract.md §3.6.2 and Session Interaction §3(5)-(7)
        foreach (var deviceId in deviceIds)
        {
            await _tcpPairingService.SendDistributeMaterialAsync(deviceId.ToString(), new[] { materialId });
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

    #region Feedback Methods - NetworkingAPISpec §1(1)(h)

    /// <summary>
    /// Retrieves all responses.
    /// Per NetworkingAPISpec §1(1)(i)(i).
    /// </summary>
    public async Task<List<InternalResponseDto>> GetAllResponses()
    {
        var responses = await _responseRepository.GetAllAsync();
        return responses.Select(MapToDto).ToList();
    }

    /// <summary>
    /// Retrieves all responses associated with a question.
    /// Per NetworkingAPISpec §1(1)(i)(ii).
    /// </summary>
    public async Task<List<InternalResponseDto>> GetResponsesUnderQuestion(Guid questionId)
    {
        var responses = await _responseRepository.GetByQuestionIdAsync(questionId);
        return responses.Select(MapToDto).ToList();
    }

    private InternalResponseDto MapToDto(ResponseEntity entity)
    {
        return new InternalResponseDto
        {
            Id = entity.Id,
            QuestionId = entity.QuestionId,
            DeviceId = entity.DeviceId,
            Timestamp = entity.Timestamp,
            IsCorrect = entity.IsCorrect,
            ResponseContent = entity switch
            {
                MultipleChoiceResponseEntity mc => mc.AnswerIndex.ToString(),
                WrittenAnswerResponseEntity wa => wa.Answer,
                _ => string.Empty
            }
        };
    }

    /// <summary>
    /// Retrieves all feedback entities.
    /// Per NetworkingAPISpec §1(1)(h)(iv).
    /// </summary>
    public async Task<List<FeedbackEntity>> GetAllFeedbacks()
    {
        var feedbacks = await _feedbackRepository.GetAllAsync();
        return feedbacks.ToList();
    }

    /// <summary>
    /// Creates a new feedback entity.
    /// Per NetworkingAPISpec §1(1)(h)(i).
    /// </summary>
    public async Task<FeedbackEntity> CreateFeedback(InternalCreateFeedbackDto dto)
    {
        _logger.LogInformation("CreateFeedback called with content: Text={Text}, Marks={Marks}, ResponseId={ResponseId}", dto.Text, dto.Marks, dto.ResponseId);
        
        try
        {
            // 1. Validate target response exists
            var response = await _responseRepository.GetByIdAsync(dto.ResponseId);
            if (response == null)
            {
                _logger.LogWarning("CreateFeedback failed: Response {ResponseId} not found", dto.ResponseId);
                throw new HubException($"Response {dto.ResponseId} not found");
            }

            // 2. Validate content per §2F(1)(b)
            if (string.IsNullOrWhiteSpace(dto.Text) && dto.Marks == null)
            {
                _logger.LogWarning("CreateFeedback failed: Validation error (no text/marks)");
                throw new HubException("At least one of Text or Marks must be provided.");
            }

            // 3. Create fresh entity with new ID per §1(1)(h)(i)
            // Backend assigns UUID per spec.
            var entity = new FeedbackEntity(
                Guid.NewGuid(), 
                dto.ResponseId, 
                dto.Text, 
                dto.Marks
            );
            // Status defaults to PROVISIONAL in constructor

            await _feedbackRepository.AddAsync(entity);
            _logger.LogInformation("CreateFeedback success: Created Feedback {FeedbackId}", entity.Id);
            return entity;
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "CreateFeedback threw exception");
            throw;
        }
    }

    /// <summary>
    /// Updates an existing feedback entity.
    /// Per NetworkingAPISpec §1(1)(h)(v).
    /// Per FrontendWorkflowSpecifications §6A(7)(b)(ii): rejects updates if status is not PROVISIONAL.
    /// Per Validation Rules §2F(1)(b): at least one of Text or Marks must be provided.
    /// </summary>
    public async Task UpdateFeedback(FeedbackEntity entity)
    {
        var existing = await _feedbackRepository.GetByIdAsync(entity.Id);
        if (existing == null)
            throw new HubException($"Feedback {entity.Id} not found");

        // Per §6A(7)(b)(ii): Only PROVISIONAL feedback can be edited
        if (existing.Status != FeedbackStatus.PROVISIONAL)
            throw new HubException($"Feedback {entity.Id} cannot be edited: status is {existing.Status}, not PROVISIONAL");

        // Per Validation Rules §2F(1)(b): At least one of Text or Marks must be provided
        var hasText = !string.IsNullOrWhiteSpace(entity.Text);
        var hasMarks = entity.Marks.HasValue;
        if (!hasText && !hasMarks)
            throw new HubException($"Feedback {entity.Id} update rejected: at least one of Text or Marks must be provided (Validation Rules §2F(1)(b))");

        // Update allowed fields (marks and text), preserve status
        existing.Marks = entity.Marks;
        existing.Text = entity.Text;
        await _feedbackRepository.UpdateAsync(existing);
    }

    /// <summary>
    /// Deletes an existing feedback entity.
    /// Per NetworkingAPISpec §1(1)(h)(vi) and FrontendWorkflowSpecifications §6A(7)(a)(ii):
    /// Invoked when teacher clears both Text and Marks on a PROVISIONAL feedback.
    /// </summary>
    public async Task DeleteFeedback(Guid feedbackId)
    {
        var existing = await _feedbackRepository.GetByIdAsync(feedbackId);
        if (existing == null)
            throw new HubException($"Feedback {feedbackId} not found");

        // Only PROVISIONAL feedback can be deleted
        if (existing.Status != FeedbackStatus.PROVISIONAL)
            throw new HubException($"Feedback {feedbackId} cannot be deleted: status is {existing.Status}, not PROVISIONAL");

        await _feedbackRepository.DeleteAsync(feedbackId);
    }

    #endregion

    #region reMarkable Methods - NetworkingAPISpec §1(1)(n)

    /// <summary>
    /// Pairs a reMarkable device using a one-time code.
    /// Per NetworkingAPISpec §1(1)(n)(i) and RemarkableIntegrationSpecification §3(2).
    /// </summary>
    public async Task<Guid> PairReMarkableDevice(string name, string oneTimeCode)
    {
        // Per §3(2)(a): validate name
        if (string.IsNullOrWhiteSpace(name))
            throw new HubException("Device name cannot be empty.");
        if (string.IsNullOrWhiteSpace(oneTimeCode))
            throw new HubException("One-time code cannot be empty.");

        // Per §2(1): check rmapi availability first
        if (!await _rmapiService.CheckAvailabilityAsync())
        {
            await Clients.Caller.SendAsync("RuntimeDependencyNotInstalled", new List<string> { "rmapi" });
            throw new HubException("rmapi is not available. Please install it first.");
        }

        // Generate UUID for the device
        var deviceId = Guid.NewGuid();
        var configPath = _rmapiService.GetConfigPath(deviceId);

        // Per §3(2)(b-c): authenticate with one-time code
        var authSuccess = await _rmapiService.AuthenticateAsync(oneTimeCode, configPath);
        if (!authSuccess)
            throw new HubException("reMarkable authentication failed. Please check the one-time code and try again.");

        // Per §3(2)(d): persist entity
        var entity = new ReMarkableDeviceEntity(deviceId, name);
        await _remarkableDeviceRepository.AddAsync(entity);

        _logger.LogInformation("Paired reMarkable device {DeviceId} with name '{Name}'", deviceId, name);
        return deviceId;
    }

    /// <summary>
    /// Unpairs a reMarkable device.
    /// Per NetworkingAPISpec §1(1)(n)(ii) and RemarkableIntegrationSpecification §3(4).
    /// </summary>
    public async Task UnpairReMarkableDevice(Guid deviceId)
    {
        // Per §3(4)(a): delete entity
        await _remarkableDeviceRepository.DeleteAsync(deviceId);

        // Per §3(4)(b): delete config file
        var configPath = _rmapiService.GetConfigPath(deviceId);
        if (File.Exists(configPath))
        {
            File.Delete(configPath);
            _logger.LogInformation("Deleted rmapi config file for device {DeviceId}", deviceId);
        }
    }

    /// <summary>
    /// Retrieves all paired reMarkable devices.
    /// Per NetworkingAPISpec §1(1)(n)(iii).
    /// </summary>
    public async Task<List<ReMarkableDeviceEntity>> GetAllReMarkableDevices()
    {
        var devices = await _remarkableDeviceRepository.GetAllAsync();
        return devices.ToList();
    }

    /// <summary>
    /// Updates a reMarkable device entity.
    /// Per NetworkingAPISpec §1(1)(n)(iv).
    /// </summary>
    public async Task UpdateReMarkableDevice(ReMarkableDeviceEntity entity)
    {
        await _remarkableDeviceRepository.UpdateAsync(entity);
    }

    #region Runtime Dependency Management - NetworkingAPISpec §1(1)(nz)

    /// <summary>
    /// Checks whether the runtime dependency with the specified dependencyId is available and functional.
    /// Per NetworkingAPISpec §1(1)(nz)(i) and BackendRuntimeDependencyManagementSpecification §2(2) and §3(2).
    /// </summary>
    public async Task<bool> CheckRuntimeDependencyAvailability(string dependencyId)
    {
        var manager = _runtimeDependencyRegistry.GetManager(dependencyId);
        if (manager == null)
            throw new HubException($"Dependency {dependencyId} not found");

        var available = await manager.CheckDependencyAvailabilityAsync();
        if (!available)
            return false;

        // Special case: even when the Ollama daemon is running, we must also
        // verify that the primary model can actually generate without running
        // out of memory.  This prevents the UI from enabling AI immediately
        // after a heavy memory event such as starting ChromaDB.
        if (dependencyId == "ollama")
        {
            try
            {
                return await _materialGenerationService.CanGenerateWithPrimaryModelAsync();
            }
            catch
            {
                return false;
            }
        }

        return true;
    }

    /// <summary>
    /// Installs the runtime dependency with the specified dependencyId.
    /// Per NetworkingAPISpec §1(1)(nz)(ii) and BackendRuntimeDependencyManagementSpecification §2(2) and §3(2).
    /// </summary>
    public async Task<bool> InstallRuntimeDependency(string dependencyId)
    {
        var manager = _runtimeDependencyRegistry.GetManager(dependencyId);
        if (manager == null)
            throw new HubException($"Dependency {dependencyId} not found");

        var progress = new Progress<RuntimeDependencyProgress>(p =>
        {
            Clients.Caller.SendAsync("RuntimeDependencyInstallProgress", 
                dependencyId, p.Phase, p.ProgressPercentage, p.ErrorMessage);
        });

        return await manager.InstallDependencyAsync(progress);
    }
    
    #endregion

    /// <summary>
    /// Deploys a material to the specified reMarkable devices.
    /// Per NetworkingAPISpec §1(1)(n)(vii) and RemarkableIntegrationSpecification §4.
    /// </summary>
    public async Task DeployMaterialToReMarkable(Guid materialId, List<Guid> deviceIds)
    {
        // Per §4(1): check rmapi availability
        if (!await _rmapiService.CheckAvailabilityAsync())
        {
            await Clients.Caller.SendAsync("RuntimeDependencyNotInstalled", new List<string> { "rmapi" });
            throw new HubException("rmapi is not available. Please install it first.");
        }

        var results = await _remarkableDeploymentService.DeployAsync(materialId, deviceIds);

        // Per §4(5): invoke ReMarkableAuthInvalid client handler for auth failures
        foreach (var result in results.Where(r => r.AuthFailed))
        {
            await Clients.Caller.SendAsync("ReMarkableAuthInvalid", result.DeviceId);
        }

        // Per §4(6): report other failures
        var failures = results.Where(r => !r.Success && !r.AuthFailed).ToList();
        if (failures.Any())
        {
            var messages = string.Join("; ", failures.Select(f => $"Device {f.DeviceId}: {f.ErrorMessage}"));
            throw new HubException($"Deployment failed for some devices: {messages}");
        }
    }

    #endregion

    #region Configuration Methods - NetworkingAPISpec §1(1)(o)

    /// <summary>
    /// Retrieves the base configuration assumed by all devices.
    /// Per NetworkingAPISpec §1(1)(o)(i) and ConfigurationManagementSpecification §1(3)(a).
    /// </summary>
    public async Task<ConfigurationEntity> GetBaseConfiguration()
    {
        _logger.LogInformation("GetBaseConfiguration called");
        return await _configurationService.GetDefaultsAsync();
    }

    /// <summary>
    /// Updates the base configuration and removes device overrides that match the new defaults.
    /// Per NetworkingAPISpec §1(1)(o)(ii) and ConfigurationManagementSpecification §1(6).
    /// </summary>
    /// <param name="newBaseConfiguration">The new base configuration.</param>
    public async Task UpdateBaseConfiguration(ConfigurationEntity newBaseConfiguration)
    {
        _logger.LogInformation("UpdateBaseConfiguration called");
        
        if (newBaseConfiguration == null)
            throw new HubException("Base configuration cannot be null");

        // Update the base configuration (per ConfigurationManagementSpecification §1(3)(a) and §1(6))
        var updated = await _configurationService.UpdateDefaultsAsync(newBaseConfiguration);
        
        _logger.LogInformation("Base configuration updated");
    }

    /// <summary>
    /// Retrieves the configuration used by a specific device (defaults merged with overrides).
    /// Per NetworkingAPISpec §1(1)(o)(iii) and ConfigurationManagementSpecification §2(2)(b).
    /// Note: Configurations are only applicable to Android devices, not reMarkable devices.
    /// Per ConfigurationManagementSpecification: "This document is applicable only to Android devices."
    /// </summary>
    /// <param name="deviceId">The device identifier.</param>
    /// <returns>The compiled configuration for the device.</returns>
    public async Task<ConfigurationEntity> GetDeviceConfiguration(Guid deviceId)
    {
        _logger.LogInformation("GetDeviceConfiguration called for device {DeviceId}", deviceId);
        
        // Validate device is an Android device (per ConfigurationManagementSpecification)
        try
        {
            await _configurationService.ValidateAndroidDeviceAsync(deviceId);
        }
        catch (ArgumentException ex)
        {
            throw new HubException(ex.Message);
        }

        // Compile and return the configuration (defaults merged with overrides per §2(2)(b))
        return await _configurationService.CompileConfigAsync(deviceId);
    }

    /// <summary>
    /// Updates the configuration overrides for a specific device.
    /// Per NetworkingAPISpec §1(1)(o)(iv) and ConfigurationManagementSpecification §2(1) and §3(1)(c).
    /// The overrides are determined by comparing the new device configuration with the base configuration.
    /// Note: Configuration updates are only applicable to Android devices, not reMarkable devices.
    /// Per ConfigurationManagementSpecification: "This document is applicable only to Android devices."
    /// </summary>
    /// <param name="deviceId">The device identifier.</param>
    /// <param name="newDeviceConfiguration">The new device configuration (full, not just overrides).</param>
    public async Task UpdateDeviceConfiguration(Guid deviceId, ConfigurationEntity newDeviceConfiguration)
    {
        _logger.LogInformation("UpdateDeviceConfiguration called for device {DeviceId}", deviceId);
        
        if (newDeviceConfiguration == null)
            throw new HubException("Device configuration cannot be null");

        // Validate device is an Android device (per ConfigurationManagementSpecification)
        try
        {
            await _configurationService.ValidateAndroidDeviceAsync(deviceId);
        }
        catch (ArgumentException ex)
        {
            throw new HubException(ex.Message);
        }

        // Get the base configuration
        var baseConfig = await _configurationService.GetDefaultsAsync();

        // Determine which values differ from the base and create override
        // Per NetworkingAPISpec §1(1)(o)(iv): "The overrides are determined by comparing 
        // the new device configuration with the base configuration."
        var newOverride = new ConfigurationOverride
        {
            TextSize = (newDeviceConfiguration.TextSize != baseConfig.TextSize) ? newDeviceConfiguration.TextSize : null,
            FeedbackStyle = (newDeviceConfiguration.FeedbackStyle != baseConfig.FeedbackStyle) ? newDeviceConfiguration.FeedbackStyle : null,
            TtsEnabled = (newDeviceConfiguration.TtsEnabled != baseConfig.TtsEnabled) ? newDeviceConfiguration.TtsEnabled : null,
            AiScaffoldingEnabled = (newDeviceConfiguration.AiScaffoldingEnabled != baseConfig.AiScaffoldingEnabled) ? newDeviceConfiguration.AiScaffoldingEnabled : null,
            SummarisationEnabled = (newDeviceConfiguration.SummarisationEnabled != baseConfig.SummarisationEnabled) ? newDeviceConfiguration.SummarisationEnabled : null,
            MascotSelection = (newDeviceConfiguration.MascotSelection != baseConfig.MascotSelection) ? newDeviceConfiguration.MascotSelection : null
        };

        // Set (or remove if empty) the override
        if (newOverride.IsEmpty)
        {
            _configurationService.RemoveOverride(deviceId);
        }
        else
        {
            _configurationService.SetOverride(deviceId, newOverride);
        }

        _logger.LogInformation("Device configuration updated for device {DeviceId}", deviceId);
    }

    #endregion

    #region GenAI Operations - NetworkingAPISpec §1(1)(i)

    private async Task NotifyMissingAiDependenciesIfApplicable(Exception ex)
    {
        if (Clients == null)
            return;

        var message = (ex.Message ?? string.Empty).ToLowerInvariant();
        var missingDependencyIds = new List<string>();

        if (message.Contains("ollama"))
            missingDependencyIds.Add("ollama");

        if (message.Contains("chroma"))
            missingDependencyIds.Add("chroma");

        if (message.Contains("qwen3"))
            missingDependencyIds.Add("qwen3:8b");

        if (message.Contains("granite"))
            missingDependencyIds.Add("granite4");

        if (message.Contains("nomic-embed-text") || message.Contains("nomic embed"))
            missingDependencyIds.Add("nomic-embed-text");

        if (missingDependencyIds.Count == 0)
            return;

        await Clients.Caller.SendAsync("RuntimeDependencyNotInstalled", missingDependencyIds);
    }

    /// <summary>
    /// Generates reading content using AI.
    /// Per NetworkingAPISpec §1(1)(i)(i) and GenAISpec.md §3B(1)(a).
    /// </summary>
    public async Task<GenerationResult> GenerateReading(GenerationRequest request)
    {
        try
        {
            return await _materialGenerationService.GenerateReading(request);
        }
        catch (Exception ex)
        {
            await NotifyMissingAiDependenciesIfApplicable(ex);
            throw new HubException($"Failed to generate reading: {ex.Message}", ex);
        }
    }

    /// <summary>
    /// Generates worksheet content using AI.
    /// Per NetworkingAPISpec §1(1)(i)(ii) and GenAISpec.md §3B(1)(b).
    /// </summary>
    public async Task<GenerationResult> GenerateWorksheet(GenerationRequest request)
    {
        try
        {
            return await _materialGenerationService.GenerateWorksheet(request);
        }
        catch (Exception ex)
        {
            await NotifyMissingAiDependenciesIfApplicable(ex);
            throw new HubException($"Failed to generate worksheet: {ex.Message}", ex);
        }
    }

    /// <summary>
    /// Modifies selected content using the AI assistant.
    /// Per NetworkingAPISpec §1(1)(i)(iv) and GenAISpec.md §3C(1)(a).
    /// </summary>
    public async Task<GenerationResult> ModifyContent(string selectedContent, string instruction, Guid? unitCollectionId)
    {
        try
        {
            return await _contentModificationService.ModifyContent(selectedContent, instruction, unitCollectionId);
        }
        catch (Exception ex)
        {
            await NotifyMissingAiDependenciesIfApplicable(ex);
            throw new HubException($"Failed to modify content: {ex.Message}", ex);
        }
    }

    /// <summary>
    /// Retrieves the current embedding status of a source document.
    /// Per NetworkingAPISpec §1(1)(i)(v) and GenAISpec.md §3E(1)(a).
    /// </summary>
    public async Task<EmbeddingStatus> GetEmbeddingStatus(Guid sourceDocumentId)
    {
        try
        {
            return await _embeddingStatusService.GetEmbeddingStatus(sourceDocumentId);
        }
        catch (Exception ex)
        {
            throw new HubException($"Failed to get embedding status: {ex.Message}", ex);
        }
    }

    /// <summary>
    /// Queues a response for AI feedback generation.
    /// Per NetworkingAPISpec §1(1)(i)(vi) and GenAISpec.md §3D(5).
    /// </summary>
    public Task QueueForAiGeneration(Guid responseId)
    {
        try
        {
            _feedbackQueueService.QueueForAiGeneration(responseId);
            return Task.CompletedTask;
        }
        catch (Exception ex)
        {
            throw new HubException($"Failed to queue response for AI generation: {ex.Message}", ex);
        }
    }

    /// <summary>
    /// Removes a response from the AI feedback generation queue.
    /// Per NetworkingAPISpec §1(1)(i)(ix) and GenAISpec.md §3D(6)(a).
    /// </summary>
    public Task RemoveFromAiGenerationQueue(Guid responseId)
    {
        try
        {
            _feedbackQueueService.RemoveFromQueue(responseId);
            return Task.CompletedTask;
        }
        catch (Exception ex)
        {
            throw new HubException($"Failed to remove response from AI generation queue: {ex.Message}", ex);
        }
    }

    /// <summary>
    /// Moves a queued response to the front of the generation queue.
    /// Per NetworkingAPISpec §1(1)(i)(viii) and GenAISpec.md §3D(8A).
    /// </summary>
    public Task PrioritiseFeedbackGeneration(Guid responseId)
    {
        try
        {
            var prioritized = _feedbackQueueService.PrioritizeResponse(responseId);
            if (!prioritized)
            {
                throw new HubException($"Response {responseId} is not currently queued or is being generated");
            }
            return Task.CompletedTask;
        }
        catch (HubException)
        {
            throw;
        }
        catch (Exception ex)
        {
            throw new HubException($"Failed to prioritise feedback generation: {ex.Message}", ex);
        }
    }

    /// <summary>
    /// Retries embedding for a failed source document.
    /// Per NetworkingAPISpec §1(1)(i)(vii) and GenAISpec.md §3A(7).
    /// </summary>
    public async Task RetryEmbedding(Guid sourceDocumentId)
    {
        try
        {
            await _documentEmbeddingService.RetryEmbeddingAsync(sourceDocumentId);
        }
        catch (Exception ex)
        {
            throw new HubException($"Failed to retry embedding: {ex.Message}", ex);
        }
    }

    /// <summary>
    /// Generates feedback for a student response.
    /// Per NetworkingAPISpec §1(1)(i)(iii) and GenAISpec.md §3D(9).
    /// Note: This is for direct/manual feedback generation. Automatic feedback generation is handled via QueueForAiGeneration.
    /// </summary>
    public async Task<string> GenerateFeedback(Guid questionId, Guid responseId)
    {
        try
        {
            var response = await _responseRepository.GetByIdAsync(responseId);
            if (response == null)
            {
                throw new HubException($"Response {responseId} not found");
            }

            var question = await _questionRepository.GetByIdAsync(questionId);
            if (question == null)
            {
                throw new HubException($"Question {questionId} not found");
            }

            if (response.QuestionId != questionId)
            {
                throw new HubException($"Response {responseId} does not belong to question {questionId}");
            }

            // Generate feedback via the feedback generation service
            // Note: This returns the entire feedback entity, but spec asks for just the text
            var feedbackText = await _ollamaClient.GenerateChatCompletionAsync(
                "granite4",
                ConstructFeedbackPrompt(question, response)
            );

            return feedbackText;
        }
        catch (HubException)
        {
            throw;
        }
        catch (Exception ex)
        {
            throw new HubException($"Failed to generate feedback: {ex.Message}", ex);
        }
    }

    /// <summary>
    /// Constructs the feedback generation prompt.
    /// See GenAISpec.md §3D(9)(b).
    /// </summary>
    private string ConstructFeedbackPrompt(QuestionEntity question, ResponseEntity response)
    {
        var maxScoreSection = question.MaxScore.HasValue
            ? $"Maximum Score:\n{question.MaxScore.Value}"
            : "";

        return $@"
TASK:
Generate constructive feedback for a student's response to the question given below.
Include a score justification explaining how well the response meets the mark scheme.
Include specific strengths in the response.
Include improvement suggestions for areas that could be enhanced.
Format the feedback in a clear, constructive manner suitable for the student to understand and learn from.

QUESTION:
{question.QuestionText}

STUDENT'S RESPONSE:
{response.ResponseText}

MARK SCHEME:
{question.MarkScheme}

{maxScoreSection}
";
    }

    #endregion

    #region Feedback Operations - NetworkingAPISpec §1(1)(h)

    /// <summary>
    /// Approves feedback and triggers dispatch to the student device.
    /// Per NetworkingAPISpec §1(1)(h)(ii) and GenAISpec.md §3DA(2).
    /// </summary>
    public async Task ApproveFeedback(Guid feedbackId)
    {
        try
        {
            var feedback = await _feedbackRepository.GetByIdAsync(feedbackId);
            if (feedback == null)
            {
                throw new HubException($"Feedback {feedbackId} not found");
            }

            if (feedback.Status != FeedbackStatus.PROVISIONAL)
            {
                throw new HubException($"Feedback {feedbackId} cannot be approved: status is {feedback.Status}, not PROVISIONAL");
            }

            // Approve feedback (transitions PROVISIONAL → READY and triggers dispatch)
            await _feedbackQueueService.ApproveFeedbackAsync(feedback);

            // Persist the status change
            await _feedbackRepository.UpdateAsync(feedback);
        }
        catch (Exception ex)
        {
            throw new HubException($"Failed to approve feedback: {ex.Message}");
        }
    }

    /// <summary>
    /// Retries dispatch of feedback in READY status.
    /// Per NetworkingAPISpec §1(1)(h)(iii) and GenAISpec.md §3DA(4).
    /// </summary>
    public async Task RetryFeedbackDispatch(Guid feedbackId)
    {
        try
        {
            var feedback = await _feedbackRepository.GetByIdAsync(feedbackId);
            if (feedback == null)
            {
                throw new HubException($"Feedback {feedbackId} not found");
            }

            if (feedback.Status != FeedbackStatus.READY)
            {
                throw new HubException($"Cannot retry dispatch for feedback with status {feedback.Status}. Only READY feedback can be retried.");
            }

            // Retrieve the response to get the device ID
            var response = await _responseRepository.GetByIdAsync(feedback.ResponseId);
            if (response == null)
            {
                throw new HubException($"Response for feedback {feedbackId} not found");
            }

            // Retry dispatch via TCP
            await _tcpPairingService.SendReturnFeedbackAsync(
                response.DeviceId.ToString(),
                new[] { feedback.Id });
        }
        catch (Exception ex)
        {
            throw new HubException($"Failed to retry feedback dispatch: {ex.Message}");
        }
    }

    #endregion

}