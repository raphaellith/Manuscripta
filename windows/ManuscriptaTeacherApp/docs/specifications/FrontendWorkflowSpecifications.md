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


## Section 4B - AI Generation

(1) When the user selects AI generation, the frontend should collect the following information:

    (a) A description of the material, and any other requirements regarding the material to be generated.

    (b) Any source documents, as specified in S2B(1)(c) of the Additional Validation Rules, that the AI should be made aware of, such as a syllabus or learning objectives.

    (c) The type of the material from reading and worksheet.

    (d) The reading age and the actual age group of the intended audience.

    (e) An approximate of the time, in minutes, that students would need to complete the material.

    (f) The template the material is based on. [Subject to further specification of the concept of templates]

(2) Once information in subsection (1) have been collected -

    (a) the AI module shall be called to generate a draft of the material. [Subject to further specification of the AI module] 

    (b) the draft shall be displayed to the user in the editor modal, as specified in Section 4C. 

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