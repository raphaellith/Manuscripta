# Frontend Workflow Specifications (Windows)

## Explanatory Note

This document defines the Windows application's frontend workflow, which describes the functionalities the UI components should provide, and the way in which it should interact with the Main component.

For a list of all server method and client handlers to be implemented for communication between the frontend and backend, see `NetworkingAPISpec.md`.


### Section 1 — General Principles

(1) The backend of the application, built with ASP.NET Core, must define a SignalR hub class `TeacherPortalHub`. This class must:

    (a) be exposed via ASP.NET Core at the URL endpoint `/TeacherPortalHub`.

    (b) be responsible for providing server methods that can be invoked by the frontend.

    (c) be responsible for invoking frontend client methods.


(2) The frontend of the application, built with Electron (HTML/ CSS/ JS/ React), must use a JavaScript-based SignalR client to build a connection to the hub described in (1). This client must:

    (a) be responsible for invoking backend hub methods.
    
    (b) be responsible for providing server message handlers, which handle messages received from the backend hub.



### Section 2 - Establishing a Connection and Bidirectional Communication

(1) When the backend is run,

    (a) it must expose its hub by mapping the `TeacherPortalHub` class to the endpoint `/TeacherPortalHub`.


(2) When the frontend is run,

    (a) it must start and initialise a SignalR client.

    (b) it must connect to the SignalR Hub endpoint `/TeacherPortalHub`, exposed by the backend.


(3) After a connection is established,

    (a) The frontend may send messages to the backend by invoking server methods via its SignalR client.

    (b) The backend may send messages to the frontend through its hub.


## Section 3 - Initialisation

(1) When the frontend application is first run, it must immediately call the following server methods for initialisation. All entities retrieved during initialisation must be stored on the frontend.

    (a) Methods for retrieving entities belonging to the material hierarchy.

        (i) `Task<List<UnitCollectionEntity>> GetAllUnitCollections()`

        (ii) `Task<List<UnitEntity>> GetAllUnits()`

        (iii) `Task<List<LessonEntity>> GetAllLessons()`

        (iv) `Task<List<MaterialEntity>> GetAllMaterials()`

        (v) `Task<List<SourceDocumentEntity>> GetAllSourceDocuments()`


## Section 4 - Functionalities for the "Library" tab

(1) When the "Library" tab is open on the frontend, the lesson library must show all unit collections, units, lessons and materials in accordance with the entities previously retrieved during initialisation in S3(1).

(2) When the frontend creates, updates or deletes a unit collection, unit, lesson or material, it must call the appropriate CRUD method defined in S1(1)(a)-(d) of `NetworkingAPISpec.md`.



## Section 4A - Further Specifications regarding Material Creation

(1) The front end shall prompt the user to enter a title when creating a unit collection, unit or lesson. There is no requirement for that title to be distinct.

(2) When creating a material, the front end shall -

    (a) prompt the user to enter a title for the material, and create a material entity through the CRUD method specified in S1(1)(d) of `NetworkingAPISpec.md` with empty content.

    (b) prompt the user to select the method of creation, through one of the following initial means:

        (i) AI generation based on description and templates. See Section 4B. The user should be reminded that they may not create a poll through this method.

        (ii) Manual creation. This bypasses the AI creation process specified in Section 4B, and the user should be prompted to select the type of material to create, from reading, worksheet and poll. When the material type selected is poll, the material should be initialised as a material containing one multiple choice question. For any other material type, the material should be initially empty.
    
    (c) inform the user that they will still be able to make manual edits, or invoke the AI assistant, after the initial creation.

(3) The front end shall also provide means for the user to -

    (a) attach source documents to a unit collection.

    (b) search for a material based on its title and contents.


## Section 4AA - Source Document Management

(1) The frontend shall provide the user with the ability to manage source documents within a unit collection.

(2) When the user uploads a source document —

    (a) the frontend shall prompt the user to select a file (e.g., PDF, DOCX, or plain text).

    (b) the frontend shall extract or prompt for a textual transcript of the document.

    (c) the frontend shall invoke `CreateSourceDocument` (NetworkingAPISpec §1(1)(k)(i)) via `TeacherPortalHub`, passing the `UnitCollectionId` and `Transcript`.

    (d) the backend shall index the document as specified in GenAISpec §3A(2).

    (e) the frontend may poll `GetEmbeddingStatus` (NetworkingAPISpec §1(1)(i)(v)) to display indexing progress.

(3) When the user edits the transcript of a source document —

    (a) the frontend shall invoke `UpdateSourceDocument` (NetworkingAPISpec §1(1)(k)(iii)) via `TeacherPortalHub` with the updated `Transcript`.

    (b) the backend shall re-index the document as specified in GenAISpec §3A(3).

(4) When the user deletes a source document —

    (a) the frontend shall invoke `DeleteSourceDocument` (NetworkingAPISpec §1(1)(k)(iv)) via `TeacherPortalHub`.

    (b) the backend shall remove associated embeddings as specified in GenAISpec §3A(4).

(5) When a source document has `EmbeddingStatus` of `FAILED` —

    (a) the frontend shall display an error indicator on the document.

    (b) the frontend shall provide a "Retry Indexing" option invoking `RetryEmbedding` (NetworkingAPISpec §1(1)(i)(vii)).

    (c) the frontend shall provide a "Delete" option.

(6) Upon receipt of `OnEmbeddingFailed` (NetworkingAPISpec §2(1)(d)(i)) —

    (a) the frontend shall display an error notification indicating which document failed.

    (b) the frontend shall update the document's status indicator.


## Section 4B - AI Generation

(1) When the user selects AI generation, the frontend should collect the following information:

    (a) A description of the material, and any other requirements regarding the material to be generated.

    (b) Optionally, specific source documents to use for context. If no documents are selected, all indexed documents in the unit collection are searched automatically.

    (c) The type of the material from reading and worksheet.

    (d) The reading age and the actual age group of the intended audience.

    (e) An approximate of the time, in minutes, that students would need to complete the material.

    (f) The template the material is based on. [Subject to further specification of the concept of templates]

(2) Once information in subsection (1) have been collected -

    (a) the frontend shall invoke `GenerateReading` (NetworkingAPISpec §1(1)(i)(i)) or `GenerateWorksheet` (NetworkingAPISpec §1(1)(i)(ii)) via `TeacherPortalHub` to generate a draft of the material.

    (b) on receiving the generation result, the frontend shall display the content in the editor modal as specified in Section 4C. If the result contains validation warnings, the frontend shall display them as specified in §4C(7).

    (c) the reading age and the actual age metadata shall be persisted, by calling the update material method specified in S1(1)(d) of this document.



## Section 4C - Editor Modal

(1) **Core Functionalities**

    The front end shall provide a uniform editor modal for editing all types of materials on a what-you-see-is-what-you-get basis. It shall support -

    (a) rendering and editing of all language features specified in the Material Encoding Specification. 

    (b) [Deleted.]

    (c) rendering, insertion, editing, relocation and deletion of embedded questions.

    (d) rendering, insertion, relocation and deletion of PDF, PNG and JPEG attachments.

(2) **Additional Functionalities**

    The editor modal shall provide means through which the user can -

    (a) invoke the AI assistant to make changes to the material, by selecting the locations of the content they want to modify, and describing the changes they want to make.

    (a1) When invoking the AI assistant —

        (i) the frontend shall capture the selected content and the user's instruction.

        (ii) the frontend shall invoke `ModifyContent` (NetworkingAPISpec §1(1)(i)(iv)) via `TeacherPortalHub`.

        (iii) on receiving the generation result, the frontend shall replace the user's selection in the editor with the content. If the result contains validation warnings, the frontend shall display them as specified in §4C(7).

    (b) modify the reading age and actual age metadata of the material.

    (c) undo and redo changes to the material.  

(3) **Saving Content**

    The editor modal shall -

    (a) automatically save any changes to the material (not including embedded questions) by calling the appropriate update endpoint, as specified in s1(1)(d)(ii) of the Networking API Specification, at most one second after each change.

    (b) provide a "save" button when creating or editing an embedded question. When it is clicked -

        (i) in the case of a question whose ID is known by a question reference defined in S4(4) of the Material Encoding Specification, update the question entity by calling the appropriate update endpoint in s1(1)(d1)(ii) of the Networking API Specification.

        (ii) in the case of a question whose ID has not been assigned, create the question entity using the appropriate create endpoint in s1(1)(d1)(i) of the Networking API Specification, obtain the Guid generated, and create an appropriate question reference using that Guid.
    
    (b1) only enable the button specified in subsection (b) when -

        (i) in the case of editing an existing question, there has been a change; and

        (ii) all mandatory fields of the question, as defined in the Validation Rules, have been filled.
    
    (c) provide a delete button for embedded questions. When it is clicked -

        (i) [DELETED]

        (ii) remove the question reference in the material.

        However, this button shall not be provided when the material is a poll. [Explanatory note: this is because polls must have exactly one multiple choice question]

    (d) not allow the user to delete questions through any other means than the delete button specified in paragraph (c).

(3A) **Editing Questions**

    The frontend shall -

    (a) collect the following information regarding the question the user wishes to edit:

        (i) the question type, per s2B(1)(b) of the Validation Rules;

        (ii) the question text, per s2B(1)(c) of the Validation Rules; and

        (iii) an optional maximum score, per s2B(2)(c) of the Validation Rules.
    
    (b) in the case of a multiple choice question, collect the following additional information:

        (i) the options, per s2B(2)(a) of the Validation Rules;

        (ii) the correct option, per s2B(2)(b) of the Validation Rules;

        and clearly indicate that the correct field is optional, and by selecting an option, the user indicates that automarking should be enabled for that question.
    
    (c) in the case of a short answer question, collect whether the user wishes to enable automarking for that question, and if so, collect the means by which the answer should be marked -

        (i) in the case of exact match, collect the expected answer, and subsequently store that as `CorrectAnswer` per s2B(2)(b) of the Validation Rules when saved; or

        (ii) in the case of AI-marking, collect a mark scheme, and subsequently store that as `MarkScheme` per s2E(1)(a) of the Additional Validation Rules when saved. 

(4) **Handling attachments**

    The editor modal shall -

    (a) provide an "attach" button adding attachments to the material. When it is clicked -

        (i) prompt the user to upload an attachment file.

        (ii) create an attachment entity using the uploaded file's base name and extension, by calling `Task CreateAttachment(AttachmentEntity newAttachmentEntity)`, as defined in S1(1)(l)(i) of the Networking API Specification.

        (iii) create and save a copy of the uploaded attachment file to the directory `%AppData%\ManuscriptaTeacherApp\Attachments`. This copy's file base name must match the UUID of the attachment entity created in (ii). Its file extension must match that of the attachment file originally uploaded in (i).

        (iv) insert the attachment into the material at the point indicated by the caret's current position.
    
    (a1) when the attachment entity is successfully created by the virtue of subparagraph (a)(ii), but the copy of the file cannot be saved as suggested by subparagraph (a)(iii), remove the attachment entity created in (ii) by calling the deletion endpoint specified in s1(1)(l)(iii) of the Networking API Specification.

    (b) provide a "delete" button for attachments. When it is clicked -

        (i) [DELETED]

        (ii) remove the attachment reference in the material.
    
    (c) not allow the user to delete attachments through any other means than the delete button specified in paragraph (b).

(5) **Initiation of Orphan Removal on Entry or Exit**

    The frontend shall, when the editor modal is entered or exited -

    (a) retrieve all attachments and questions associated with the material, by calling `Task<List<AttachmentEntity>> GetAttachmentsUnderMaterial(Guid materialId)` and `Task<List<QuestionEntity>> GetQuestionsUnderMaterial(Guid materialId)`, as defined in S1(1)(l)(ii) and S1(1)(d)(ii) of the Networking API Specification.

    (b) identify all attachments and questions that are not referenced in the material, and -

        (i) delete such attachment entity(ies) using the deletion endpoint specified in s1(1)(l)(iii) of the Networking API Specification, and delete the copy(ies) of corresponding attachment file(s) from the data directory.

        (ii) delete such question(s) using the deletion endpoint specified in s1(1)(d1)(iv) of the Networking API Specification.

(6) **Initiation of Orphan Removal on Discovery of Orphaned Attachment Entities**

    The frontend shall, on discovery of an attachment entity whose corresponding attachment file does not exist in the data directory -

    (a) remove any attachment reference which references such attachment entity.

    (b) delete such attachment entity using the deletion endpoint specified in s1(1)(l)(iii) of the Networking API Specification.

(7) **Displaying Validation Warnings**

    When the editor modal receives content with validation warnings (per GenAISpec §3G) —

    (a) the frontend shall display a warning banner indicating issues require attention.

    (b) the frontend shall highlight or annotate the affected lines where line numbers are available.

    (c) the frontend shall provide a list view of all warnings with descriptions.


## Section 4D — Response Review and Feedback Workflow

(1) When the teacher selects a response for review, the frontend shall —

    (a) if a `FeedbackEntity` exists with status `PROVISIONAL` —

        (i) display the feedback content for review and editing.

        (ii) provide a "Release Feedback" button.

    (b) if no `FeedbackEntity` exists and the question satisfies GenAISpec §3D(1) —

        (i) if the response is deemed queued or generating (per GenAISpec §3D(3)–(4)), display a "pending" indicator.

        (ii) provide a "Write Manually" option. Upon saving manual feedback, the response shall be removed from the queue per GenAISpec §3D(6).

        (iii) provide a "Prioritise" option if the response is queued.

    (c) if the question does not have a `MarkScheme`, provide a manual feedback entry interface.

(2) When the teacher clicks "Release Feedback", the frontend shall invoke `ApproveFeedback(feedbackId)` (NetworkingAPISpec §1(1)(h)(ii)).

(3) Upon receipt of `OnFeedbackGenerationFailed` (NetworkingAPISpec §2(1)(c)(i)) —

    (a) the frontend shall display an error message indicating that AI feedback generation has failed.

    (b) the frontend shall provide a "Retry" option invoking `QueueForAiGeneration`.

    (c) the frontend shall provide a "Write Manually" option.

(4) Upon receipt of `OnFeedbackDispatchFailed` (NetworkingAPISpec §2(1)(c)(ii)) —

    (a) the frontend shall display a message indicating that feedback delivery to the specified device has failed.

    (b) the frontend shall provide a "Retry" option invoking `RetryFeedbackDispatch` (NetworkingAPISpec §1(1)(h)(iii)).


## Section 5 - Functionalities for the "Classroom" tab

(1) When prompted by the user, the frontend client must call `Task PairDevices()` to initiate device pairing.

(2) Once pairing is complete, the frontend client must call the following methods to retrieve all devices, device statuses and sessions. All retrieved entities must be stored on the frontend.
        
    (i) `Task<List<PairedDeviceEntity>> GetAllPairedDevices()`.

    (ii) `Task<List<DeviceStatusEntity>> GetAllDeviceStatuses()`

    (iii) `Task<List<SessionEntity>> GetAllSessions()`.


(3) When the backend updates a device status or session, it must call the appropriate client handler, as defined in S2(1)(a) of `NetworkingAPISpec.md`, to update the frontend data.

(4) When one or more tablets are locked on the frontend, it must call the server method `Task LockDevices(List<Guid> deviceIds)` with the corresponding tablet UUIDs.

(5) When one or more tablets are unlocked on the frontend, it must call the server method `Task UnlockDevices(List<Guid> deviceIds)` with the corresponding tablet UUIDs.

(6) When a material is deployed on the frontend, it must call the server method `Task DeployMaterial(Guid Id)` with the corresponding material UUID.

(7) When a material deployment is ended on the frontend, it must call the server method `Task FinishMaterial(Guid Id)` with the corresponding material UUID.



## Section 6 - Functionalities for the "Responses" tab

(1) When the backend creates a response entity, it must call the frontend handler `CreateResponse` with the newly created `ResponseEntity` as the argument.

(2) When the frontend creates a new feedback entity, it must call the server method `Task CreateFeedback(FeedbackEntity newFeedbackEntity)` with the newly created feedback entity as the argument.



## Section 7 - Functionalities for the "AI Assistant" tab

**To be confirmed.**


## Section 8 - Functionalities for the "Settings" tab

**To be confirmed.**