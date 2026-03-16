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



### Section 2ZA — Backend Process Lifecycle Management

(1) **Obligation to Start Backend**

    The frontend application shall be solely responsible for starting, monitoring, and terminating the backend application process. The backend shall not be expected to run independently in production.

(2) **Backend Executable Bundling**

    (a) The backend application shall be published as a self-contained executable targeting the `win-x64` runtime.

    (b) The published backend executable and its dependencies shall be bundled with the frontend Electron application as an extra resource, outside the ASAR archive.

    (c) The frontend shall resolve the backend executable path as follows:

        (i) In packaged (production) mode, the path shall be resolved relative to `process.resourcesPath`.

        (ii) In development mode, the path shall be resolved relative to a configurable fallback location.

(3) **Backend Process Spawning**

    When the frontend application is launched —

    (a) the frontend shall first determine whether a backend process is already listening on the designated port by performing a health probe as specified in subsection (5)(a), starting with the default port and proceeding through alternative ports as specified in subsection (8)(c) if necessary.

    (b) If the frontend is launched in development mode, and if a backend process is already running and responds to the health probe —

        (i) the frontend shall not spawn a new backend process;

        (ii) the frontend shall proceed to display the main application window as specified in subsection (6).

    (c) Unless paragraph (b) applies —

        (i) the frontend shall spawn the backend executable as a child process using Node.js `child_process.spawn`.

        (ii) the backend shall be started with the command-line argument `--urls http://localhost:{PORT}` to bind to the designated port.

        (iii) the frontend shall capture the backend's standard output and standard error streams for diagnostic purposes.

        (iv) if the backend process fails to start due to the designated port being unavailable, the frontend shall retry with the next port in accordance with subsection (8)(c).
    
    (d) A frontend in deployment mode shall in no circumstances connect to a backend not spawned by itself.

(4) **Splash Screen**

    (a) Before the backend is confirmed ready, the frontend shall display a splash screen indicating that the application is starting.

    (b) The splash screen shall be a lightweight Electron `BrowserWindow` without the main React renderer.

    (c) The splash screen shall be dismissed once the backend is confirmed ready, as specified in subsection (5)(c).

(4A) The frontend shall, in no circumstances, use a wording which appears to the user that there is a frontend-backend separation[, such as "connecting"]. 

[Explanatory note: The frontend-backend separation is not a user-facing concept. The user should be informed as if there is no such separation.] 

(5) **Backend Readiness Confirmation**

    (a) The frontend shall poll the backend's health endpoint at `GET http://localhost:{PORT}/` at intervals of 500 milliseconds.

    (b) The backend shall be considered ready when the health endpoint returns an HTTP 200 response.

    (c) Once the backend is confirmed ready —

        (i) the frontend shall dismiss the splash screen;

        (ii) the frontend shall proceed to create the main application window and establish SignalR connection as specified in Section 2.

    (d) If the backend does not become ready within 30 seconds of spawning —

        (i) the frontend shall display an error message to the user indicating that the application failed to start;

        (ii) the frontend shall terminate the spawned backend process, if any;

        (iii) the frontend shall provide an option to retry or exit the application.

(6) **Backend Process Monitoring and Automatic Restart**

    (a) The frontend shall monitor the spawned backend process for unexpected termination by listening for the `exit` event on the child process handle.

    (b) If the backend process terminates unexpectedly while the frontend is running —

        (i) the frontend shall attempt to restart the backend process automatically;

        (ii) restart attempts shall use exponential backoff with initial delay of 1 second, doubling after each failed attempt, up to a maximum delay of 30 seconds;

        (iii) the backoff delay shall reset to 1 second after the backend has been running continuously for at least 60 seconds.

    (c) During backend restart —

        (i) the frontend shall display a reconnecting indicator to the user;

        (ii) the frontend shall not dismiss any unsaved user state.

    (d) If five consecutive restart attempts fail within a 5-minute window —

        (i) the frontend shall display an error message indicating persistent backend failure;

        (ii) the frontend shall provide options to retry manually or exit the application.

(7) **Graceful Shutdown**

    (a) When the user closes the frontend application, or when Electron's `will-quit` event is emitted —

        (i) the frontend shall send a termination signal (`SIGTERM` or equivalent) to the backend process;

        (ii) the frontend shall wait up to 5 seconds for the backend process to exit gracefully;

        (iii) if the backend process does not exit within 5 seconds, the frontend shall forcibly terminate the process.

    (b) The frontend shall ensure no orphaned backend processes remain after the application exits.

(8) **Port Configuration**

    (a) The designated backend port shall be defined in a single configuration constant shared across —

        (i) the backend process spawning logic;

        (ii) the Content Security Policy header;

        (iii) the SignalR connection URL.

    (b) The default designated port for SignalR communications shall be 5910.

    (c) If the default port specified in paragraph (b) is unavailable —

        (i) the frontend shall attempt to bind to alternative ports by incrementing the port number sequentially, starting from 5914 up to and including 5919.

        (ii) a port shall be considered unavailable if the backend process fails to start due to an address-in-use error, or if another process is already listening on that port as determined by a health probe returning an unexpected response.

        (iii) if all ports in the range 5914 to 5919 are unavailable, the frontend shall display an error message to the user indicating the application failed to start.

        (iv) upon successful binding to an alternative port, the frontend shall use that port for all subsequent communications within that application session.

    (d) In this section, references to "{PORT}" shall be substituted with the designated port as selected in accordance with paragraphs (b) and (c) above.


### Section 2 — Establishing a Connection and Bidirectional Communication

(1) When the backend is run —

    (a) it must expose its hub by mapping the `TeacherPortalHub` class to the endpoint `/TeacherPortalHub`.

    (b) it must expose a health endpoint at `GET /` that returns HTTP 200 when the application is ready to accept connections.

(2) When the frontend is run —

    (a) it must start and manage the backend process in accordance with Section 2ZA.

    (b) it must not proceed to initialise the SignalR client until the backend is confirmed ready per Section 2ZA(5)(b).

    (c) once the backend is confirmed ready, it must start and initialise a SignalR client.

    (d) it must connect to the SignalR Hub endpoint `/TeacherPortalHub`, exposed by the backend.

(3) After a connection is established —

    (a) The frontend may send messages to the backend by invoking server methods via its SignalR client.

    (b) The backend may send messages to the frontend through its hub.

(4) **Connection Resilience**

    (a) The SignalR client shall be configured with a custom retry policy that retries indefinitely with exponential backoff.

    (b) The retry policy shall use initial delay of 0 seconds, then 2 seconds, then 10 seconds, then 30 seconds for subsequent attempts, repeating the 30-second delay indefinitely.

    (c) When the SignalR connection is lost and subsequently restored, the frontend shall re-fetch all entities as specified in Section 3(1) to ensure state consistency.


## Section 3 - Initialisation

(1) When the frontend application is first run, it must immediately call the following server methods for initialisation. All entities retrieved during initialisation must be stored on the frontend.

    (a) Methods for retrieving entities belonging to the material hierarchy.

        (i) `Task<List<UnitCollectionEntity>> GetAllUnitCollections()`

        (ii) `Task<List<UnitEntity>> GetAllUnits()`

        (iii) `Task<List<LessonEntity>> GetAllLessons()`

        (iv) `Task<List<MaterialEntity>> GetAllMaterials()`

        (v) `Task<List<SourceDocumentEntity>> GetAllSourceDocuments()`

    (b) Methods for retrieving global settings.

        (i) `Task<PdfExportSettingsEntity> GetPdfExportSettings()`


## Section 3A - Runtime Dependency Management

(1) The backend may notify the frontend of a missing runtime dependency by invoking the `RuntimeDependencyNotInstalled` handler specified in the Networking API Specification Section 2(1)(f)(i).

(2) When such notification is received, the frontend shall —

    (a) display a modal indicating to the user that runtime dependency(ies) are missing, the name and purpose of those dependencies, and a confirmation that the user wishes to install those dependencies;

    (b) if confirmation is received, for each missing runtime dependency, sequentially —

        (i) call `Task<bool> InstallRuntimeDependency(string dependencyId)` as defined in the Networking API Specification Section 1(1)(nz)(ii);

        (ii) subscribe to the `RuntimeDependencyInstallProgress` handler specified in the Networking API Specification Section 2(1)(f)(ii);

        (iii) display a modal with —

            (A) the current phase of the installation process, being one of "Downloading", "Verifying", "Installing";

            (B) where the phase is "Downloading", a progress bar indicating the percentage of the download completed;

            (C) where the phase is "Verifying" or "Installing", an indeterminate progress indicator;

    (c) [DELETED]

    (d) if the installation fails —

        (i) display an error message with the error details received from the `RuntimeDependencyInstallProgress` handler;

        (ii) offer the user the option to install manually per subsection (3); and

        (iii) offer the user the option to cancel the operation.

(3) When manual installation is selected, or when automatic installation fails and the user selects manual installation, the frontend shall —

    (a) open the user's default browser to the download page for the dependency;

    (b) display instructions for the user to download and place the binary at the expected path; and

    (c) provide a button to re-check the availability of the dependency by calling `Task<bool> CheckRuntimeDependencyAvailability(string dependencyId)` as defined in the Networking API Specification Section 1(1)(nz)(i).

(4) [DELETED. See Section 7(1).]

## Section 3B - Email Credential Configuration

(1) The backend may notify the frontend of missing email credentials during an email-dependent operation by invoking the `EmailCredentialsNotConfigured` handler specified in the Networking API Specification Section 2.

(2) When such notification is received, the frontend shall —

    (a) display a modal indicating to the user that email credentials are required for the requested operation;

    (b) provide a button to navigate to the Settings interface to configure the credentials; and

    (c) provide an option to cancel the dependent operation.

(3) [DELETED. See Section 7(2).]

(4) [DELETED. See Section 7(3).]

## Section 4 - Functionalities for the "Library" tab

(1) When the "Library" tab is open on the frontend, the lesson library must show all unit collections, units, lessons and materials in accordance with the entities previously retrieved during initialisation in S3(1).

(2) When the frontend creates, updates or deletes a unit collection, unit, lesson or material, it must call the appropriate CRUD method defined in S1(1)(a)-(d) of `NetworkingAPISpec.md`.

(3) The "Library" tab must provide a search bar for searching materials in the lesson library, subject to the following requirements.

    (a) When one or more keywords are entered into the search bar, the frontend must filter through all materials in the lesson library and only display those matching those keywords.

    (b) Materials are said to match the keyword if its title, content, question texts or question options include the keyword.

    (c) Material encoding syntax that is not visible through the user interface must be excluded from the search. 



## Section 4A - Further Specifications regarding Material Creation

(1) The front end shall prompt the user to enter a title when creating a unit collection, unit or lesson. There is no requirement for that title to be distinct.

(2) When creating a material, the front end shall -

    (a) prompt the user to enter a title for the material, and create a material entity through the CRUD method specified in S1(1)(d) of `NetworkingAPISpec.md` with empty content.

    (b) prompt the user to select the method of creation, through one of the following initial means:

        (i) AI generation based on description and templates. See Section 4B. The user should be reminded that they may not create a poll through this method. This option should also make clear that AI-generated materials may contain mistakes and should be reviewed before being deployed to students.

        (ii) Manual creation. This bypasses the AI creation process specified in Section 4B, and the user should be prompted to select the type of material to create, from reading, worksheet and poll. When the material type selected is poll, the material should be initialised as a material containing one multiple choice question. For any other material type, the material should be initially empty.
    
    (c) inform the user that they will still be able to make manual edits, or invoke the AI assistant, after the initial creation.

(3) The front end shall also provide means for the user to -

    (a) attach source documents to a unit collection.

    (b) search for a material based on its title and contents.


## Section 4AA - Source Document Management

(1) The frontend shall provide the user with the ability to manage source documents within a unit collection.

(2) When the user uploads a source document —

    (a) the frontend shall prompt the user to either select a file (e.g., PDF, DOCX) or enter plain text.

    (b) the frontend shall create a textual transcript of the document, if a file has been uploaded.

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

    (f) [Deleted.]

(2) Once information in subsection (1) have been collected -

    (a) the frontend shall invoke `GenerateReading` (NetworkingAPISpec §1(1)(i)(i)) or `GenerateWorksheet` (NetworkingAPISpec §1(1)(i)(ii)) via `TeacherPortalHub` to generate a draft of the material.

    (a1) Before invoking the generation method, the frontend shall subscribe to the `OnGenerationStarted` handler (NetworkingAPISpec §2(1)(h)(ii)) to receive the server-generated generation ID for cancellation support. Whilst the generation is in progress, the frontend shall also subscribe to the `OnGenerationProgress` handler (NetworkingAPISpec §2(1)(h)(i)) and display a streaming generation view, which shall —

        (i) display chain-of-thought tokens (`isThinking = true`) in a visually distinct manner (e.g., a collapsible "Thinking…" section with muted or italic styling), to give the user evidence that the AI is actively reasoning.

        (ia) when `isQueryingSourceDocuments = true` is received via `OnGenerationProgress`, display an in-progress status message indicating that source-document retrieval is underway. This status message shall be displayed if and only if `isQueryingSourceDocuments` remains true.

        (ii) display content tokens (`isThinking = false`) progressively, rendering them as they arrive, in a manner consistent with the editor's rendering capabilities. Specifically, the streaming view shall convert accumulated content Markdown to HTML using a streaming-specific conversion function (`markdownToStreamingHtml`) that —

            (A) renders standard Markdown formatting (headings, bold, italic, lists, tables, code blocks, blockquotes, horizontal rules) via the same `marked` library used by the editor;

            (B) renders inline and block LaTeX to KaTeX HTML, consistent with the editor's `InlineLatex` and `BlockLatex` extensions;

            (C) renders `question-draft` markers (GenAISpec Appendix C) as styled preview cards matching the editor's `QuestionRef` visual appearance, with a "Draft" badge, question type tag, question text, multiple-choice options with correct-answer highlighting, and mark-scheme/correct-answer displays for written-answer questions;

            (D) handles incomplete Markdown gracefully during streaming: `marked` shall be invoked tolerantly on each frame, rendering valid syntax and passing through unparsed/partial syntax as text;

            (E) renders `!!! center` blocks, `!!! pdf` embeds, and `!!! question` references as appropriate placeholders.

        (iii) display an animated indicator (e.g., a blinking cursor or pulsing dot) at the end of the streamed content to signal that generation is still in progress.

        (iv) upon receipt of a chunk with `done = true`, remove the animated indicator and transition to the final content display.

        (v) a "Cancel" control, which when activated shall invoke `CancelGeneration` (NetworkingAPISpec §1(1)(i)(x)) with the generation ID received from `OnGenerationStarted`, and close the streaming view without persisting any content.

        (vi) the frontend shall handle `OperationCanceledException` from generation methods gracefully without displaying an error notification.

        (vii) to prevent UI jank from high-frequency token streams, the frontend should buffer incoming tokens and batch state updates using `requestAnimationFrame` or a similar mechanism, rather than updating state on each individual token.

    (a2) The streaming generation view shall not permit editing of the content whilst generation is in progress. Editing shall be enabled only after the final `GenerationResult` is received per paragraph (b).

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

    (a) invoke the AI assistant to make changes to the material, by selecting the locations of the content they want to modify, and describing the changes they want to make. When invoking the AI assistant —

        (i) the frontend shall capture the selected content and the user's instruction.

        (ii) the frontend shall invoke `ModifyContent` (NetworkingAPISpec §1(1)(i)(iv)) via `TeacherPortalHub`, passing the material's `id`, `materialType`, `title`, `readingAge`, and `actualAge` alongside the selected content and instruction.

        (iii) on receiving the generation result, the frontend shall replace the user's selection in the editor with the content. If the result contains validation warnings, the frontend shall display them as specified in §4C(7).

        (iv) Whilst the modification is in progress, the frontend shall display a streaming generation view in accordance with §4B(2)(a1)(ia), showing the AI's chain-of-thought and the progressively generated replacement content.

        (v) The streaming generation view for content modification shall not replace the user's existing content until the final `GenerationResult` is received. An interim preview may be shown adjacent to or overlaid on the selected content.

    (b) modify the reading age and actual age metadata of the material.

    (b1) modify the per-material PDF export settings — line pattern type, line spacing preset, and font size preset. The frontend shall display a description informing the user that these settings override the global defaults for this material, but may in turn be overridden by per-device settings configured on external devices. This shall be provided in means of dropdowns, and each dropdown shall —

        (i) include a "Default" option that maps to null (i.e., the global default from `PdfExportSettingsEntity` is used), and display the current global default value for the teacher's reference (e.g., "Default (Ruled)", "Default (Medium)");

        (ii) include options for each enum value of the respective type (`LinePatternType`, `LineSpacingPreset`, `FontSizePreset`);

        (iii) display "Default (...)" when the per-material override is null;

        (iv) when the user selects a specific value, set it as a per-material override; and

        (v) be auto-saved with the existing 1-second debounce mechanism like other material properties.

    (c) undo and redo changes to the material.  

(2A) **LaTeX Formatting**

    The frontend shall disable rich text formatting (bold, italic, underline) in paragraphs containing inline or block LaTeX nodes. When a LaTeX node is inserted into a paragraph, any existing formatting marks in that paragraph shall be removed.

    [Explanatory Note: Pursuitant to Material Conversion Specifications, Section 3A(6)(a), LaTeX and markdown formatting may not coexist in the same paragraph.]

(3) **Saving Content**

    The editor modal shall -

    (a) automatically save any changes to the material (not including embedded questions) by calling the appropriate update endpoint, as specified in s1(1)(d)(ii) of the Networking API Specification, at most one second after each change.

    (a1) strip any syntax or tags that is not permitted per Material Encoding Specification §1(4) from the material content before saving, preserving only the visible text content within such tags. This includes -

        (i) hyperlinks.

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

    (d) support - 
        
        (i) drag-and-drop of attachments, of all supported types, into the editor modal.

        (ii) copy-paste of images into the editor modal. These images may be introduced along with text.

        Attachments added in a manner specified in this paragraph shall be handled in the same manner as attachments added through the "attach" button, as specified in paragraphs (a)(ii-iv) and (a1).

(5) **Initiation of Orphan Removal on Entry or Exit**

    The frontend shall, when the editor modal is entered or exited -

    (a) retrieve all attachments and questions associated with the material, by calling `Task<List<AttachmentEntity>> GetAttachmentsUnderMaterial(Guid materialId)` and `Task<List<QuestionEntity>> GetQuestionsUnderMaterial(Guid materialId)`, as defined in S1(1)(l)(ii) and S1(1)(d)(ii) of the Networking API Specification.

    (b) identify all attachments and questions that are not referenced in the material, and -

        (i) delete such attachment entity(ies) using the deletion endpoint specified in s1(1)(l)(iii) of the Networking API Specification, and delete the copy(ies) of corresponding attachment file(s) from the data directory.

        (ii) delete such question(s) using the deletion endpoint specified in s1(1)(d1)(iv) of the Networking API Specification.

(6) **Initiation of Orphan Removal on Discovery of Orphaned Attachment Entities**

    The frontend shall, on discovery of an attachment entity whose corresponding attachment file does not exist in the data directory when attempting to render the attachment -

    (a) remove any attachment reference which references such attachment entity.

    (b) delete such attachment entity using the deletion endpoint specified in s1(1)(l)(iii) of the Networking API Specification.

(7) **Displaying Validation Warnings**

    When the editor modal receives content with validation warnings (per GenAISpec §3G) —

    (a) the frontend shall display a warning banner indicating issues require attention.

    (b) the frontend shall highlight or annotate the affected lines where line numbers are available.

    (c) the frontend shall provide a list view of all warnings with descriptions.


## Section 4D — PDF Export

(1) **Export Button**

    The editor modal shall provide an "Export to PDF" button. When activated —

    (a) the frontend shall invoke `GenerateMaterialPdf(Guid materialId)`, as defined in s1(1)(m)(i) of the Networking API Specification, passing the current material's ID;

    (b) the frontend shall display a loading indicator while awaiting the response;

    (c) the server method shall return the PDF content as a byte array upon successful generation;

    (d) upon receipt of the byte array, the frontend shall prompt the user to save the PDF file using the system file save dialogue;

    (e) the suggested filename shall be the material title with `.pdf` extension, with any invalid filename characters removed.

    [Explanatory Note: The export uses the effective PDF settings resolved per MaterialConversionSpecification §1(5)(h) (device → material → global). The same resolution is applied when PDFs are generated as part of deployment to external devices.]

(2) **Error Handling**

    If the server method throws an exception, the frontend shall display an error message to the user.


## Section 5 - Functionalities for the "Classroom" tab

(1) [DELETED]

(2) On entry to the Classroom page, the frontend client must call the following methods to retrieve all devices and device statuses. All retrieved entities must be stored on the frontend.

    (i) `Task<List<PairedDeviceEntity>> GetAllPairedDevices()`.

    (ii) `Task<List<DeviceStatusEntity>> GetAllDeviceStatuses()`

    (iii) [DELETED]

(3) [DELETED]

(4) [DELETED]

(5) [DELETED]

(6) [DELETED]

(7) [DELETED]

[Explanatory note: See Sections 5A-5D for detailed specifications regarding the Classroom tab.]

## Section 5A — Device Pairing

(1) **Initiating Pairing**

    The frontend shall provide a button or other control to initiate device pairing.

(2) **Calling the Pairing Endpoint**

    When the user initiates pairing —

    (a) the frontend shall call `Task PairDevices()`, as defined in s1(1)(e)(i) of the Networking API Specification;

    (b) the frontend shall indicate to the user that pairing is in progress.

(3) **Notification of Newly Paired Devices**

    When a device is deemed paired by virtue of Pairing Process Specification s2(4) —

    (a) the backend shall invoke the `DevicePaired` client handler with the newly created `PairedDeviceEntity`;

    (b) the frontend shall refresh the device grid in accordance with subsection (3A).

(3A) **Device Grid Refresh**

    The frontend shall refresh the device grid by —

    (a) calling `GetAllPairedDevices` and `GetAllDeviceStatuses` to retrieve the current state from the backend;

    (b) replacing the locally stored device list and status map with the retrieved data.

    (c) This refresh shall be triggered when —

        (i) the frontend receives a `DevicePaired` notification per subsection (3)(a);

        (ii) the frontend receives notification that devices have been unpaired;

        (iii) the user navigates to the Classroom page (as already specified in §5(2)).

(4) **Terminating the Pairing Process**

    The frontend shall provide a button or other control to terminate the pairing process. When activated —

    (a) the frontend shall call `Task StopPairing()`, as defined in s1(1)(e)(v) of the Networking API Specification;

    (b) the frontend shall remove the pairing-in-progress indicator.

(5) **Unpairing Devices**

    The frontend shall provide a means for the user to unpair one or more selected devices by calling `Task UnpairDevices(List<Guid> deviceIds)`.


## Section 5B — Device Display, Status and Configuration

(1) The frontend shall display all paired devices in a grid layout.

(2) For each device, the frontend shall display —

    (a) the user-friendly device name, as provided during pairing per Pairing Process Specification s2(2)(c);

    (b) the current status of the device, using distinct visual treatments for each `DeviceStatus` value defined in s2E(1)(b) of the Validation Rules;

    (c) a distinct visual indicator in the grid, when the device has an unacknowledged help request. For the purpose of this paragraph —

        (i) a help request shall be considered pending from the time the backend receives a `HAND_RAISED` (0x11) message from the device, per Session Interaction Specification s4A(1).

        (ii) a help request shall be considered acknowledged by the user when the user triggers acknowledgement by dismissing the relevant alert per s5D(2)(c) of this specification.

        (iii) the frontend shall provide button, within the grid, to acknowledge a help request. When the user clicks this button, the help request shall also be considered acknowledged.
    
    (d) [Deleted]

(3) The frontend shall update the displayed status when the backend invokes the `UpdateDeviceStatus` client handler, as defined in s2(1)(a) of the Networking API Specification.


(4) **Device Renaming**

    The frontend shall provide a means for the user to rename a paired device —

    (a) by calling `Task UpdatePairedDevice(PairedDeviceEntity entity)` as defined in the Networking API Specification;

    (b) the new name shall be persisted on the Windows device and shall not affect the Android device.


(5) For each Android device in the grid display, the frontend shall provide a settings button which allows the user to view and modify the device's configurations in a configuration modal as outlined in s5H of this specification.

(5A) For each external device in the grid display, the frontend shall provide a settings button which allows the user to view and modify the device's PDF export setting overrides in a configuration modal as outlined in s5H(2) of this specification.


## Section 5C — Device Control

(1) **Device Selection**

    The frontend shall provide a means for the user to select one or more devices —

    (a) by clicking on individual device cards to toggle selection;

    (b) by using a "Select All" button to select all devices;

    (c) by using a "Deselect All" button to clear selection.

    (d) The frontend shall display the current selection count (e.g., "X / Y devices selected").

(2) **Locking and Unlocking Devices**

    The frontend shall provide controls to lock and unlock devices. These actions shall apply only to the currently selected devices.

    The action shall call `Task LockDevices(List<Guid> deviceIds)` or `Task UnlockDevices(List<Guid> deviceIds)` as defined in s1(1)(f) of the Networking API Specification.

(3) **Material Deployment**

    The frontend shall provide a means for the user to deploy materials to the currently selected devices by —

    (a) selecting a material from the lesson library, using cascading selection controls for unit collection, unit, lesson, and material;

    (b) calling `Task DeployMaterial(Guid materialId, List<Guid> deviceIds)` as defined in s1(1)(g)(i) of the Networking API Specification;

    (c) indicating to the user when deployment is in progress, and when it completes. For this purpose, deployment to a device shall be considered complete when `DISTRIBUTE_ACK` (0x12) message(s) for all deployed material(s) are received from that device, per Session Interaction Specification s3(5).

(4) **Unpairing Devices**

    The frontend shall provide a means for the user to unpair the currently selected devices. When activated —

    (a) the frontend shall call `Task UnpairDevices(List<Guid> deviceIds)` as defined in s5A(5) of this specification;

    (b) the unpairing shall proceed per Pairing Process Specification s3.


## Section 5D — Alerts

(1) **Definition**

    For the purpose of this section, an "alert" shall be displayed as an appropriate banner. Alerts shall be displayed regardless of the tab the user is currently on.

(2) **Help Request Alerts**

    The frontend shall —

    (a) display an alert when the backend invokes the `HandRaised` client handler, as defined in s2(1)(e)(i) of the Networking API Specification;

    (b) display a persistent alert when one or more devices have unacknowledged help requests, showing the count of such requests;

    (c) provide an "Acknowledge" button for each help request. Activating this button shall dismiss the alert on the frontend.

    (d) automatically update the alert if help requests are acknowledged through other means than those defined in this subsection.

(3) **Device Disconnection Alerts**

    The frontend shall display an alert when the backend invokes the `UpdateDeviceStatus` client handler, as defined in s2(1)(a)(i) of the Networking API Specification, with a `Status` of `DISCONNECTED`. The frontend shall also visually distinguish disconnected devices in the device grid.

(4) **Distribution Failure Alerts**

    The frontend shall display an alert when the backend invokes the `DistributionFailed` client handler, as defined in s2(1)(e)(ii) of the Networking API Specification. The alert shall identify the specific device(s) for which distribution has failed.

(5) **Remote Control Failure Alerts**

    The frontend shall display an alert when the backend invokes the `RemoteControlFailed` client handler, as defined in s2(1)(e)(iii) of the Networking API Specification.

(6) **Configuration Refresh Failure Alerts**

    The frontend shall display an alert when the backend invokes the `ConfigRefreshFailed` client handler, as defined in s2(1)(e)(iv) of the Networking API Specification.

(7) **Feedback Delivery Failure Alerts**

    The frontend shall display an alert when the backend invokes the `FeedbackDeliveryFailed` client handler, as defined in s2(1)(e)(v) of the Networking API Specification. The alert shall identify the specific device(s) for which feedback delivery has failed.


## Section 5E — rmapi Availability and Installation

[DELETED. See Section 3A for generalised runtime dependency management.]


## Section 5F — External Device Pairing

(1) The frontend shall provide a button or other control to initiate external device pairing (reMarkable or Kindle), distinct from Android device pairing.

(2) When the user initiates external device pairing —

    (a) the frontend shall prompt the user to select the device type (`REMARKABLE` or `KINDLE`).

    (b) the frontend shall prompt the user to enter a user-friendly device name.

    (c) if the selected type is `REMARKABLE`, the frontend shall open `https://my.remarkable.com/device/desktop/connect` and prompt for the one-time code.

    (d) if the selected type is `KINDLE`, the frontend shall prompt for the "Send to Kindle" email address by providing a text input field for the local part and pre-filling/displaying `@kindle.com` as a fixed suffix. It shall also display a reminder to whitelist the sending address.

    (e) the frontend shall call `Task<Guid> PairExternalDevice()`, passing the collected data as the `configurationData` parameter.

    (f) the frontend shall indicate to the user that pairing is in progress.

    (g) upon success, the frontend shall refresh the device grid in accordance with Subsection 5A(3A).

    (h) upon failure, the frontend shall display an error message and allow the user to retry or cancel.

(3) When the backend invokes the `ExternalDeviceAuthInvalid` client handler —

    (a) the frontend shall display a notification indicating that the specified device requires re-authentication.

    (b) the frontend shall provide an option to initiate re-authentication, following the workflow in Subsection (2).

(4) The frontend shall provide a means for the user to unpair an external device by calling `Task UnpairExternalDevice(Guid deviceId)`.

(5) External devices shall be visually distinguished in the device grid. Classroom control and response collection UI elements shall be disabled when an external device is selected.


## Section 5G — Material Deployment to External Devices

(1) The frontend shall provide a means to deploy materials to external devices using the same material selection workflow as Android devices in Subsection 5C(3).

(2) When the user deploys a material to one or more external devices —

    (a) the frontend shall perform capability checks for the selected device types:
        (i) If any `KINDLE` devices are selected, it shall call `CheckEmailCredentialAvailability()`. If false, display the configuration modal in accordance with Subsection 3B(2).
        (ii) If any `REMARKABLE` devices are selected, the application shall verify the availability of `rmapi`. If the backend invokes the `RuntimeDependencyNotInstalled` handler, the frontend shall display the installation modal and halt deployment in accordance with Section 3A.

    (b) the frontend shall call `Task DeployMaterialToExternalDevices(Guid materialId, List<Guid> deviceIds)`.

    (c) the frontend shall indicate that deployment is in progress.

    (d) upon completion, the frontend shall display a success message indicating dispatch.

(3) When the user selects a mix of Android and External devices —

    (a) the frontend shall call `DeployMaterial` for Android devices;

    (b) the frontend shall call `DeployMaterialToExternalDevices` for External devices;

    (c) the frontend shall clearly indicate the status of deployment to each device class separately.

## Section 5H - Configuration modal

(1) When the settings button for a displayed Android device is pressed, the frontend shall display a configuration modal which -

    (a) shows the configuration currently associated with the selected device, retrieved via `Task<ConfigurationEntity> GetDeviceConfiguration(Guid DeviceId)`.

    (b) allows the user to modify any value(s) in the selected device's configuration.
    
    (c) includes a "Save" button to allow users to save the changes made via `Task UpdateDeviceConfiguration(Guid DeviceId, ConfigurationEntity newDeviceConfiguration)`.

(2) When the settings button for a displayed external device is pressed, the frontend shall display a configuration modal which —

    (a) shows the per-device PDF export setting overrides currently associated with the selected device, as defined in AdditionalValidationRules §3D(1)(e–g).

    (a1) The modal shall display a description informing the user that per-device settings take the highest priority, overriding both per-material and global default settings when a PDF is generated for this device.

    (b) displays three dropdown controls — line pattern type, line spacing preset, and font size preset — each of which shall —
        (i) include a "Default" option that maps to null (i.e., the per-material or global default is used);
        (ii) include options for each enum value of the respective type (`LinePatternType`, `LineSpacingPreset`, `FontSizePreset`); and
        (iii) display "Default" when the per-device override is null.

    (c) includes a "Save" button to allow users to save the changes made via `Task UpdateExternalDevice(ExternalDeviceEntity entity)`.



## Section 6 - Functionalities for the "Responses" tab


(1) [DELETED]

(2) When the frontend creates a new feedback entity, it must call the server method `Task CreateFeedback(FeedbackEntity newFeedbackEntity)` with the newly created feedback entity as the argument.

(3) **Refresh of Responses**

    (a) Responses shall be refreshed by calling `Task GetAllResponses()` as defined in s1(1)(i)(i) of the Networking API Specification.

    (b) The frontend shall refresh responses -

        (i) on entry to the responses tab; or
        
        (ii) when the `RefreshResponses` client handler is invoked by the backend, as defined in s2(1)(b)(i) of the Networking API Specification.

(3A) **Refresh of Feedback**

    (a) Feedback shall be refreshed by calling `GetAllFeedbacks()` s1(1)(h)(iv) of the Networking API Specification.

    (b) The frontend shall refresh feedback —

        (i) when responses are refreshed, by the virtue of paragraph (3)(b); or

        (ii) after each `UpdateFeedback` call [to ascertain that feedback has been appropriately submitted].

(4) **Display of responses on class-level**

    The frontend shall -

    (a) provide a material selection dropdown menu containing only materials with corresponding response entities.

    (b) when a material is selected, display the list of questions in the material in the manner specified in paragraphs (c)-(d).

    (c) in the case of a multiple choice question, display —

        (i) the question text and maximum score;

        (ii) the list of choices, and the distribution of responses for each choice, both in percentage and absolute number;

        (iii) if the question contains a `CorrectAnswer` field, highlight of the correct answer in green; and

        (iv) the "display name" of tablets which selected each choice, in a pop-up upon clicking each choice.

    (d) in the case of a written answer question, display -

        (i) the question text and maximum score;

        (ii) the list of responses and the "display name" of tablets which submitted each response; 

        (iii) if the question has a `MarkScheme` field, the mark scheme;

        (iv) for each response to a question without a  `CorrectAnswer` field, means to enter the mark awarded for that response, and means to add a textual feedback , automatically saving the mark and/or textual feedback by creating or updating the feedback entity, at most 1 second after each change. Editing is subject to the rules in §6A(7). The feedback entity shall be in PROVISIONAL state before dispatch in paragraph (v) takes place; 

        (v) means to add the marks and feedbacks under the feedback entities for dispatch, setting the status to `READY` (AdditionalValidationRules §3AE(1)(a)(ii));

        (vi) an indicator if a response's feedback has status `READY` but not yet `DELIVERED` (per AdditionalValidationRules §3AE).

        (vii) for each response with a CorrectAnswer field, whether that answer is correct.
    
(5) **Display of Responses on Device-level**

    The frontend shall —

    (a) provide a device selection dropdown menu containing only devices with corresponding response entities, using their display names.

    (b) when a device is selected, provide a dropdown menu of materials with corresponding response entities from that device.

    (c) when a material is selected, display the list of questions in the material in the manner specified in paragraphs (d)-(e).

    (d) in the case of a multiple choice question, display —

        (i) the question text and maximum score;

        (ii) the list of choices, and the response selected for that device; and

        (iii) if the question contains a `CorrectAnswer` field, highlight of the correct answer in green.

    (e) in the case of a written answer question, display -

        (i) the question text and maximum score;

        (ii) the response from the selected device;

        (iii) if the question has a `MarkScheme` field, the mark scheme;

        (iv) for a response to a question without a `CorrectAnswer` field, means to enter the mark awarded for that response, and means to add a textual feedback, automatically saving the mark and/or textual feedback by creating or updating the feedback entity, at most 1 second after each change. Editing is subject to the rules in §6A(7). The feedback entity shall be in PROVISIONAL state before dispatch in paragraph (v) takes place; 

        (v) means to add the mark and feedback under the feedback entity for dispatch, setting the status to `READY` (AdditionalValidationRules §3AE(1)(a)(ii));

        (vi) an indicator if a response's feedback has status `READY` but not yet `DELIVERED` (per AdditionalValidationRules §3AE);

        (vii) for a response to a question with a `CorrectAnswer` field, the correct answer, and whether that response is correct.

## Section 6A — Response Review and Feedback Workflow

(1) If no `FeedbackEntity` corresponding to a response (R) exists and the question R is related to satisfies GenAISpec §3D(1), the frontend shall —

    (a) if R is deemed queued or generating (per GenAISpec §3D(3)–(4)), display a "pending" indicator, and disable edits for that response.

    (b) provide a "Write Manually" option, enabled only when R is queued and not actively generating. Upon selecting manual feedback, R shall be removed from the queue by invoking `RemoveFromAiGenerationQueue` (NetworkingAPISpec §1(1)(i)(ix)) per GenAISpec §3D(6)(a), and a feedback in `PROVISIONAL` state shall be created. The option shall be disabled while R is being generated.

    (c) provide a "Prioritise" option if R is queued. Upon selection, the frontend shall invoke `PrioritiseFeedbackGeneration(Guid responseId)` (NetworkingAPISpec §1(1)(i)(viii)) to move R to the front of the generation queue.

(1A) If a response (R) corresponds to an existing feedback (F) in `PROVISIONAL` state, the frontend shall provide means for the teacher to send R to the generation queue. R shall be added to the AI generation queue by invoking `QueueForAiGeneration` (NetworkingAPISpec §1(1)(i)(vi)). The teacher shall be warned that F will be overwritten, and be asked for confirmation.

(2) When the teacher clicks "Release Feedback", the frontend shall invoke `ApproveFeedback(feedbackId)` (NetworkingAPISpec §1(1)(h)(ii)).

(3) Upon receipt of `OnFeedbackGenerationFailed` (NetworkingAPISpec §2(1)(c)(i)) —

    (a) the frontend shall display an error message indicating that AI feedback generation has failed.

    (b) the frontend shall provide a "Retry" option invoking `QueueForAiGeneration` (NetworkingAPISpec §1(1)(i)(vi)).

(4) [DELETED]

(5) [DELETED]

(6) The frontend shall, upon receipt of `FeedbackDeliveryFailed` (NetworkingAPISpec §2(1)(e)(v)) —

    (a) display a message indicating that feedback delivery to the specified device has failed, and an indicator for the specific response near the indicator specified in Section 6(5)(e)(vi) above.

    (b) provide a "Retry" option invoking `RetryFeedbackDispatch`, near the indicator specified in paragraph (a) (NetworkingAPISpec §1(1)(h)(iii)).

(7) **Feedback Editing and Deletion Rules**

    Subject to subsection (1) above —

    (a) A feedback entity whose Status is `PROVISIONAL` may be edited by the teacher in the following manner —

        (i) The teacher may modify the `Text` and/or `Marks` fields via `UpdateFeedback` (NetworkingAPISpec §1(1)(h)(v)).

        (ii) If the teacher clears both `Text` and `Marks` fields (both become null/empty), the frontend shall invoke `DeleteFeedback(Guid feedbackId)` (NetworkingAPISpec §1(1)(h)(vi)) rather than `UpdateFeedback`. This action deletes the provisional feedback.

    (b) A feedback entity whose Status is `READY` or `DELIVERED` shall not be editable. The frontend shall not display editing controls for feedback in these states.

    [Explanatory Note: Once feedback has been approved (`READY`) or dispatched (`DELIVERED`), it represents a finalised assessment that the student may have already seen. Editing would create inconsistency between what the teacher approved and what the student received.]


## Section 6B — Response Export PDF

(1) **Export Button**

    When the frontend is displaying device-level responses (§6(5)), the frontend shall provide an "Export to PDF" button.

(2) **Export Options**

    When the export button is activated, the frontend shall display a popover or modal presenting the following options:

    (a) "Include Feedback" — a tickbox, defaulting to unticked.

    (b) "Include Mark Scheme" — a tickbox, defaulting to unticked.
    
    (c) A "Download" button to confirm the export.

(3) **Export Invocation**

    When the "Download" button is activated —

    (a) the frontend shall invoke `GenerateResponsePdf(Guid materialId, string deviceId, bool includeFeedback, bool includeMarkScheme)`, as defined in s1(1)(m)(ii) of the Networking API Specification, passing the currently selected material ID, the currently selected device ID, and the toggle values;

    (b) the frontend shall display a loading indicator while awaiting the response;

    (c) upon receipt of the byte array, the frontend shall prompt the user to save the PDF file using the system file save dialogue;

    (d) the suggested filename shall be `{materialTitle} - {deviceDisplayName} Responses.pdf`, with invalid filename characters removed;

    (e) if the server method throws an exception, the frontend shall display an error message to the user.

(4) **Availability**

    The export button shall be available only when both a device and a material are selected and at least one response exists from that device for that material.


## Section 7 - Functionalities for the "Settings" tab

(1) **Device Base Configuration**

    (a) On entry of the "Settings" tab, the frontend must call the server method `Task<ConfigurationEntity> GetBaseConfiguration()` to retrieve and display the base configuration assumed by all Android devices.

    (b) The frontend shall -

        (i) when there are no paired Android devices, provide means to modify any value in the base configuration; and

        (ii) when there are paired Android devices, prevent the user from modifying the base configuration.

(2) **Runtime Dependency Management**

    The frontend shall provide an option in the Settings interface to —

    (a) re-check the availability of each runtime dependency by calling `Task<bool> CheckRuntimeDependencyAvailability(string dependencyId)` per Section 3A(3)(c); and

    (b) reinstall each runtime dependency by calling `Task<bool> InstallRuntimeDependency(string dependencyId)` per Section 3A(2)(b)(i).

(3) **Email Credential Configuration**

    The frontend shall provide an interface in the Settings view to configure email credentials. This interface shall —

    (a) collect the `EmailAddress`, `SmtpHost`, `SmtpPort`, and `Password` (or app-specific password) from the user;

    (b) provide a "Save" button which calls `Task SaveEmailCredentials(EmailCredentialEntity credentials)` per the Networking API Specification Section 1;

    (c) display a loading indicator while the backend tests the connection;

    (d) upon success, display a success message; and

    (e) upon failure, display the error message preventing the save operation.

(4) The Settings interface shall also provide a means to view the currently configured `EmailAddress` (retrieved via `GetEmailCredentials()`) and a button to delete the configuration via `DeleteEmailCredentials()`.

(5) **PDF Export Defaults**

    (a) On entry of the "Settings" tab, the frontend shall call `GetPdfExportSettings()` to retrieve the current global PDF export defaults.

    (a1) The frontend shall display a description informing the user that these are the global defaults, and that they may be overridden on a per-material basis (in the editor) or on a per-device basis (in the external device configuration modal).

    (b) The frontend shall display three dropdown controls:

        (i) Line Pattern Type — with options `RULED`, `SQUARE`, `ISOMETRIC`, `NONE`.

        (ii) Line Spacing Preset — with options `SMALL`, `MEDIUM`, `LARGE`, `EXTRA_LARGE`.

        (iii) Font Size Preset — with options `SMALL`, `MEDIUM`, `LARGE`, `EXTRA_LARGE`.

    (c) The frontend shall provide a "Save PDF Defaults" button. When activated, the frontend shall call `UpdatePdfExportSettings(entity)` to persist the updated defaults.
