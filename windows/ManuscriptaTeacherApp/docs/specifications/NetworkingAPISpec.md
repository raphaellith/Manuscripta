# Networking API Specifications (Windows)

## Explanatory Note

This document lists the method signatures to be implemented by the backend `TeacherPortalHub` class and the handlers to be used by the frontend Electron app's SignalR client.

For a description of how these server methods and client handlers are expected to interact, see `NetworkingInteractionSpec.md`.


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

        (iv) `Task<List<SessionEntity>> GetAllSessions()`: Retrieves all sessions.

    (f) Methods for locking and unlocking devices.

        (i) `Task LockDevices(List<Guid> deviceIds)`: Locks the tablets with the given UUIDs.

        (ii) `Task UnlockDevices(List<Guid> deviceIds)`: Unlocks the tablets with the given UUIDs.
    
    (g) Methods for starting and ending material deployment.

        (i) `Task DeployMaterial(Guid Id)`: Deploys a material, identified by its UUID.

        (ii) `Task FinishMaterial(Guid Id)`: Stops deploying a material, identified by its UUID.
        
    (h) Methods for creating feedback.

        (i) `Task CreateFeedback(FeedbackEntity newFeedbackEntity)`: Receives data for a new feedback entity (without an assigned UUID), and creates the entity with an assigned UUID.

    (i) Methods for sending AI assistant prompts. **To be confirmed.**

    (j) Methods for updating app settings. **To be confirmed.**

    (k) Creation, retrieval and deletion methods for source documents.

        (i) `Task CreateSourceDocument(SourceDocumentEntity newSourceDocumentEntity)`: Receives data for a new source document entity (without an assigned UUID), and creates the entity with an assigned UUID.

        (ii) `Task<List<SourceDocumentEntity>> GetAllSourceDocuments()`: Retrieves all source document entities.

        (iii) `Task DeleteSourceDocument(Guid id)`: Deletes a source document entity, identified by its UUID.




### Section 2 - Frontend handlers

(1) The frontend JavaScript client must include the following handlers.

    (a) Handlers for updating device statuses and sessions.

        (i) `UpdateDeviceStatus`, with parameter `deviceStatusEntity` (DeviceStatusEntity): Updates a device status entity, identified by its deviceId.

        (ii) `UpdateSession`, with parameter `sessionEntity` (SessionEntity): Updates a session entity, identified by its `deviceId` and `materialId`.

    (b) Handlers for creating responses.
        
        (i) `CreateResponse`, with parameter `ResponseEntity` (ResponseEntity): Creates a response entity.

    (c) Handlers for retrieving AI assistant responses. **To be confirmed.**
