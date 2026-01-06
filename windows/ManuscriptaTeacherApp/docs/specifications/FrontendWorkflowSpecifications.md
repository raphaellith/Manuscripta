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

(2) When creating a material, the front-end should -

    (a) prompt the user to enter a title for the material, and create a material entity through the CRUD method specified in S1(1)(d) of this document with empty content.

    (b) prompt the user to select the method of creation, through one of the following initial means:

        (i) AI generation based on description and templates. See Section 4B. The user should be reminded that they may not create a poll through this method.

        (ii) Manual creation. This bypasses the AI creation process specified in Section 4B, and the user should be prompted to select the type of material to create, from reading, worksheet and poll.
    
    (c) inform the user that they will still be able to make manual edits, or invoke the AI assistant, after the initial creation.



## Section 4B - AI Generation

(1) When the user selects AI generation, the frontend should collect the following information:

    (a) A description of the material, and any other requirements regarding the material to be generated.

    (b) Any source documents, as specified in S2B(1)(c) of the Additional Validation Rules, that the AI should be made aware of, such as a syllabus or learning objectives.

    (c) The type of the material from reading and worksheet.

    (d) The reading age and the actual age group of the intended audience.

    (e) An approximate of the time, in minutes, that students would need to complete the material.

    (f) The template the material is based on. [Subject to further specification of the concept of templates]

(2) Once information in subsection (1) have been collected -

    (i) the AI module shall be called to generate a draft of the material. [Subject to further specification of the AI module] 

    (ii) the draft shall be displayed to the user in the editor modal, as specified in Section 4C. 

    (iii) the reading age and the actual age metadata shall be persisted, by calling the update material method specified in S1(1)(d) of this document.



## Section 4C - Editor Modal

(1) The front end shall provide a uniform editor modal for editing all types of materials on a what-you-see-is-what-you-get basis. It shall support -

    (i) rendering and editing of all language features specified in the Material Encoding Specification. 

    (ii) rendering, insertion, relocation and deletion of embedded questions, as well as pictures and PDFs.

(2) The editor modal shall provide means through which the user can -

    (i) invoke the AI assistant to make changes to the material, by selecting the locations of the content they want to modify, and describing the changes they want to make.

    (ii) modify the reading age and actual age metadata of the material.

(3) The editor modal shall auto-save its contents at regular intervals, using the appropriate update methods.



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