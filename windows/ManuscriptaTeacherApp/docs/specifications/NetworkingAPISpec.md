# Networking API Specifications (Windows)

## Explanatory Note

This document lists the method signatures to be implemented by the backend `TeacherPortalHub` class and the handlers to be used by the frontend Electron app's SignalR client.

For a description of how these server methods and client handlers are expected to interact, see `FrontendWorkflowSpecifications.md`.


### Section 1 - Backend methods

(1) The backend SignalR Hub must provide the following public methods.

    (a) CRUD methods for unit collections.

        (i) `Task CreateUnitCollection(UnitCollectionEntity newUnitCollectionEntity)`: Receives data for a new unit collection entity (without an assigned UUID), and creates the entity with an assigned UUID.

        (ii) `Task<List<UnitCollectionEntity>> GetAllUnitCollections()`: Retrieves all unit collection entities.

        (iii) `Task UpdateUnitCollection(UnitCollectionEntity updated)`: Updates a unit collection entity, identified by its UUID.

        (iv) `Task DeleteUnitCollection(Guid id)`: Deletes a unit collection entity, identified by its UUID.
    

    (b) CRUD methods for units.

        (i) `Task CreateUnit(UnitEntity newUnitEntity)`: Receives data for a new unit entity (without an assigned UUID), and creates the entity with an assigned UUID.

        (ii) `Task<List<UnitEntity>> GetAllUnits()`: Retrieves all unit entities.

        (iii) `Task UpdateUnit(UnitEntity updated)`: Updates a unit entity, identified by its UUID.

        (iv) `Task DeleteUnit(Guid id)`: Deletes a unit entity, identified by its UUID.

    (c) CRUD methods for lessons.

        (i) `Task CreateLesson(LessonEntity newLessonEntity)`: Receives data for a new lesson entity (without an assigned UUID), and creates the entity with an assigned UUID.

        (ii) `Task<List<LessonEntity>> GetAllLessons()`: Retrieves all lesson entities.

        (iii) `Task UpdateLesson(LessonEntity updated)`: Updates a lesson entity, identified by its UUID.

        (iv) `Task DeleteLesson(Guid id)`: Deletes a lesson entity, identified by its UUID.


    (d) CRUD methods for materials.

        (i) `Task CreateMaterial(MaterialEntity newMaterialEntity)`: Receives data for a new material entity (without an assigned UUID), and creates the entity with an assigned UUID.

        (ii) `Task<List<MaterialEntity>> GetAllMaterials()`: Retrieves all material entities.

        (iii) `Task UpdateMaterial(MaterialEntity updated)`: Updates a material entity, identified by its UUID.

        (iv) `Task DeleteMaterial(Guid id)`: Deletes a Material entity, identified by its UUID.
    
    (d1) CRUD methods for questions.

        (i) `Task<Guid> CreateQuestion(QuestionEntity newQuestionEntity)`: Receives data for a new material entity (without an assigned UUID), and creates the entity with an assigned UUID, which is returned to the client 

        (ii) `Task<List<QuestionEntity>> GetQuestionsUnderMaterial(Guid materialId)`: Retrieves all questions associated with the material with the materialId.

        (iii) `Task UpdateQuestion(QuestionEntity updated)`: Updates a question entity, identified by its UUID.

        (iv) `Task DeleteQuestion(Guid id)`: Deletes a Question entity, identified by its UUID.

    (e) Method for retrieving devices, device statuses and sessions.

        (i) `Task PairDevices()`: Initiates pairing with devices.

        (ii) `Task<List<PairedDeviceEntity>> GetAllPairedDevices()`: Retrieves all paired devices.

        (iii) `Task<List<DeviceStatusEntity>> GetAllDeviceStatuses()`: Retrieves all device statuses.

        (iv) [DELETED]

        (v) `Task StopPairing()`: Terminates the pairing process.

    (f) Methods for locking and unlocking devices.

        (i) `Task LockDevices(List<Guid> deviceIds)`: Locks the tablets with the given UUIDs.

        (ii) `Task UnlockDevices(List<Guid> deviceIds)`: Unlocks the tablets with the given UUIDs.
    
    (g) Methods for starting and ending material deployment.

        (i) `Task DeployMaterial(Guid materialId, List<Guid> deviceIds)`: Deploys a material to the specified devices, by adding the material and all its embedded questions to the distribution bundle related to all related devices, and sending a TCP `DISTRIBUTE_MATERIAL` message to all related devices.

        (ii) [DELETED]
        
    (h) Methods for feedback.

        (i) `Task CreateFeedback(FeedbackEntity newFeedbackEntity)`: Receives data for a new feedback entity (without an assigned UUID), and creates the entity with an assigned UUID.

        (ii) `Task ApproveFeedback(Guid feedbackId)`: Approves the specified feedback and triggers dispatch to the student device. See GenAISpec §3DA(2).

        (iii) `Task RetryFeedbackDispatch(Guid feedbackId)`: Retries dispatch of feedback in `READY` status.
        
        (iv) `Task<List<FeedbackEntity>> GetAllFeedbacks()`: Retrieves all feedback entities.

        (v) `Task UpdateFeedback(FeedbackEntity entity)`: Updates an existing feedback entity (marks and text only; status unchanged). This method shall reject updates if the feedback's Status is not `PROVISIONAL`.

        (vi) `Task DeleteFeedback(Guid feedbackId)`: Deletes an existing feedback entity. Per FrontendWorkflowSpecifications §6A(7)(a)(ii), this is invoked when the teacher clears both Text and Marks on a `PROVISIONAL` feedback.

    (i) Methods for GenAI functionalities, as specified in GenAISpec.

        (i) `Task<GenerationResult> GenerateReading(GenerationRequest request)`: Generates reading material content. Returns `GenerationResult` (AdditionalValidationRules §3AC). See GenAISpec §3B.

        (ii) `Task<GenerationResult> GenerateWorksheet(GenerationRequest request)`: Generates worksheet material content. Returns `GenerationResult`. See GenAISpec §3B.

        (iii) `Task<string> GenerateFeedback(Guid questionId, Guid responseId)`: Generates feedback for a student response. See GenAISpec §3D(9).

        (iv) `Task<GenerationResult> ModifyContent(string selectedContent, string instruction, Guid? unitCollectionId)`: Modifies selected content based on the instruction. Returns `GenerationResult`. See GenAISpec §3C.

        (v) `Task<EmbeddingStatus> GetEmbeddingStatus(Guid sourceDocumentId)`: Returns the embedding status of a source document. See GenAISpec §3E.

        (vi) `Task QueueForAiGeneration(Guid responseId)`: Adds or re-adds the specified response to the AI feedback generation queue. See GenAISpec §3D(5).

        (vii) `Task RetryEmbedding(Guid sourceDocumentId)`: Re-queues a source document with `FAILED` status for indexing. See GenAISpec §3A(7).

        (viii) `Task PrioritiseFeedbackGeneration(Guid responseId)`: Moves the specified response to the front of the AI feedback generation queue. See GenAISpec §3D(8A).

        (ix) `Task RemoveFromAiGenerationQueue(Guid responseId)`: Removes the specified response from the AI feedback generation queue. See GenAISpec §3D(6)(a).
        
    (j) Methods for retrieving responses.

        (i) `Task<List<ResponseEntity>> GetAllResponses()`: Retrieves all responses.

        (ii) `Task<List<ResponseEntity>> GetResponsesUnderQuestion(Guid questionId)`: Retrieves all responses associated with the question with the questionId.

    (k) CRUD methods for source documents.

        (i) `Task CreateSourceDocument(SourceDocumentEntity newSourceDocumentEntity)`: Receives data for a new source document entity (without an assigned UUID), and creates the entity with an assigned UUID. Triggers embedding indexing per GenAISpec §3A.

        (ii) `Task<List<SourceDocumentEntity>> GetAllSourceDocuments()`: Retrieves all source document entities.

        (iii) `Task UpdateSourceDocument(SourceDocumentEntity updated)`: Updates a source document entity, including its `Transcript` field. Triggers re-indexing per GenAISpec §3A(3).

        (iv) `Task DeleteSourceDocument(Guid id)`: Deletes a source document entity, identified by its UUID. Removes associated embeddings per GenAISpec §3A(4).
    
    (l) Creation, retrieval and deletion methods for attachments.

        (i) `Task<Guid> CreateAttachment(AttachmentEntity newAttachmentEntity)`: Receives data for a new attachment entity (without an assigned UUID), and creates the entity with an assigned UUID. The assigned UUID is then returned to the client.

        (ii) `Task<List<AttachmentEntity>> GetAttachmentsUnderMaterial(Guid materialId)`: Retrieves all attachments associated with the material with the materialId.

        (iii) `Task DeleteAttachment(Guid id)`: Deletes an attachment entity, identified by its UUID.

    (m) Methods for PDF generation.

        (i) `Task<byte[]> GenerateMaterialPdf(Guid materialId)`: Generates a PDF document for the specified material and returns the PDF content as a byte array. The PDF shall be generated in accordance with Material Conversion Specification.

    (nz) Methods for runtime dependency management.

        (i) `Task<bool> CheckRuntimeDependencyAvailability(string dependencyId)`: Checks whether the runtime dependency with the specified dependencyId is available and functional per Runtime Dependency Management Specification §2(2). Returns `true` if available, `false` otherwise.

        (ii) `Task<bool> InstallRuntimeDependency(string dependencyId)`: Installs the runtime dependency with the specified dependencyId per Runtime Dependency Management Specification §2(2). Returns `true` on success, `false` on failure.

    (n) Methods for reMarkable device management.

        (i) `Task<Guid> PairReMarkableDevice(string name, string oneTimeCode)`: Pairs a reMarkable device by authenticating with the one-time code, creating the configuration file, and persisting the `ReMarkableDeviceEntity`. Returns the UUID of the newly created device entity.

        (ii) `Task UnpairReMarkableDevice(Guid deviceId)`: Unpairs a reMarkable device by deleting the `ReMarkableDeviceEntity` and the corresponding rmapi configuration file.

        (iii) `Task<List<ReMarkableDeviceEntity>> GetAllReMarkableDevices()`: Retrieves all paired reMarkable devices.

        (iv) `Task UpdateReMarkableDevice(ReMarkableDeviceEntity entity)`: Updates a reMarkable device entity, identified by its UUID.

        (v) [DELETED]

        (vi) [DELETED]

        (vii) `Task DeployMaterialToReMarkable(Guid materialId, List<Guid> deviceIds)`: Deploys a material to the specified reMarkable devices by generating a PDF and uploading it to each device's reMarkable cloud via rmapi. Returns when all uploads are complete or have failed.
    
    (o) Methods for base configuration and device-specific overrides.

        (i) `Task<ConfigurationEntity> GetBaseConfiguration()`: Retrieves the base configuration assumed by all Android devices.

        (ii) `Task UpdateBaseConfiguration(ConfigurationEntity newBaseConfiguration)`: Updates the base configuration.

        (iii) `Task<ConfigurationEntity> GetDeviceConfiguration(Guid DeviceId)`: Retrieves the configuration used by an Android device, identified by its UUID.

        (iv) `Task UpdateDeviceConfiguration(Guid DeviceId, ConfigurationEntity newDeviceConfiguration)`: Updates the overrides associated with an Android device, identified by its UUID. The overrides are determined by comparing the new device configuration with the base configuration.


### Section 2 - Frontend handlers

(1) The frontend JavaScript client must include the following handlers.

    (a) Handlers for updating device statuses and sessions.

        (i) `UpdateDeviceStatus`, with parameter `deviceStatusEntity` (DeviceStatusEntity): Updates a device status entity, identified by its deviceId.

        (ii) `UpdateSession`, with parameter `sessionEntity` (SessionEntity): Updates a session entity, identified by its `deviceId` and `materialId`.

        (iii) `DevicePaired`, with parameter `pairedDeviceEntity` (PairedDeviceEntity): Notifies the frontend that a new device has been paired. The frontend shall use this notification as a signal to refresh the device grid per FrontendWorkflowSpec §5A(3A), and shall not directly use the payload to modify its local state.

    (b) Handlers updating the responses page.
        
        (i) `RefreshResponses`. Signals that the frontend should refresh the responses page. The backend shall invoke this handler when a response is received, or a feedback changes state.

    (c) Handlers for AI feedback notifications.

        (i) `OnFeedbackGenerationFailed`, with parameters `responseId` (Guid) and `error` (string): Notifies the frontend that AI feedback generation has failed for the specified response. See GenAISpec §3D(7)(b).

        (ii) `OnFeedbackDispatchFailed`, with parameters `feedbackId` (Guid) and `deviceId` (Guid): Notifies the frontend that feedback dispatch has failed for the specified device. See GenAISpec §3DA(4)(a).

    (d) Handlers for embedding notifications.

        (i) `OnEmbeddingFailed`, with parameters `sourceDocumentId` (Guid) and `error` (string): Notifies the frontend that source document indexing has failed after all retries. See GenAISpec §3A(6)(b)(ii).

    (e) Handlers for alerts.

        (i) `HandRaised`, with parameter `deviceId` (Guid): Notifies the frontend that a device has raised a hand. The backend shall invoke this handler on receipt of a TCP `HAND_RAISED` message from the device.

        (ii) `DistributionFailed`, with parameter `deviceId` (Guid) and `materialId` (Guid): Notifies the frontend that material distribution to a device has failed. The backend shall invoke this handler if material distribution times out by the virtue of Session Interaction Specification s3(6), or when the target device is not connected.

        (iii) `RemoteControlFailed`, with parameters `deviceId` (Guid) and `command` (string): Notifies the frontend that a remote control command has failed. The backend shall invoke this handler in case of a timeout under s6(2)(c) of the Session Interaction Specification.

        (iv) `ConfigRefreshFailed`, with parameter `deviceId` (Guid): Notifies the frontend that a configuration refresh has failed. The backend shall invoke this handler in case of a timeout under s6(3)(b) of the Session Interaction Specification.

        (v) `FeedbackDeliveryFailed`, with parameters `deviceId` (Guid) and `feedbackId` (Guid): Notifies the frontend that feedback delivery to a device has failed. The backend shall invoke this handler when a timeout occurs under s7(5) of the Session Interaction Specification, or when the target device is not connected.
    
    (f) Handlers for runtime dependency management.

        (i) `RuntimeDependencyNotInstalled`, with parameter `dependencyIds` (List<String>): Notifies the frontend that the list of runtime dependencies specified by `dependencyIds` have not been installed properly.

        (ii) `RuntimeDependencyInstallProgress`, with parameters `dependencyId` (string), `phase` (string), `progressPercentage` (int?) and `errorMessage` (string?): Notifies the frontend of the progress of a runtime dependency installation. The `phase` parameter shall be one of "Downloading", "Verifying", "Installing", "Completed" or "Failed". The `progressPercentage` parameter shall be an integer between 0 and 100 inclusive when `phase` is "Downloading", and null otherwise. The `errorMessage` parameter shall contain the error description when `phase` is "Failed", and null otherwise.
