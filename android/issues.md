# Android Client - Draft Issues

This file contains drafted GitHub issues for the Manuscripta Android student client. Issues are organised hierarchically by parent area and include granular subtasks where appropriate.

---

## Parent Issues

### [Android] Data Model Layer Implementation

- Labels: `android`, `enhancement`, `data-layer`

**Description:**
Implement the data model layer for the Android student client, including Room entities, DAOs, and domain models.

**Critical Specification:** Entity IDs must be persistent and consistent across both Windows and Android applications. When either client generates a new entity, it must assign a globally unique identifier (UUID) that remains constant across all services. For example:
- Materials/Questions created by Windows app: Windows assigns ID, Android preserves it
- Responses/Sessions created by Android app: Android assigns ID, Windows preserves it

This ensures data integrity and proper synchronization without ID conflicts.

**Requirements:**
- Define all entity classes for local persistence (Room) with String/UUID IDs
- Create DAO interfaces for database operations
- Implement domain model classes for business logic
- Ensure proper data validation and constraints
- All entities must use client-assigned, persistent UUIDs

**Related Requirements:** MAT1, MAT2, NET1

**Acceptance Criteria:**
- [ ] All entity classes created with proper Room annotations
- [ ] All DAO interfaces implemented with CRUD operations
- [ ] Domain models separate from entities (Clean Architecture)
- [ ] 100% unit test coverage for model classes
- [ ] Checkstyle compliant
- [ ] Javadoc for all public methods

---

### [Android] Repository Layer Implementation

- Labels: `android`, `enhancement`, `repository-layer`

**Description:**
Implement the repository layer following Clean Architecture principles, providing a single source of truth for data access.

**Requirements:**
- Create repository interfaces and implementations
- Implement data synchronization between network and local database
- Handle caching strategies
- Implement error handling and retry logic

**Related Requirements:** MAT1, NET1, NET2

**Acceptance Criteria:**
- [ ] Repository interfaces defined
- [ ] Repository implementations with caching logic
- [ ] Proper error handling and Result/State patterns
- [ ] 100% unit test coverage
- [ ] Checkstyle compliant
- [ ] Javadoc for all public methods

**Dependencies:** Data Model Layer must be completed first

---

### [Android] Network Layer Implementation

- Labels: `android`, `enhancement`, `network-layer`

**Description:**
Implement the HTTP/REST network layer using Retrofit for content transmission with the teacher's Windows application server. This layer handles material downloads, response submissions, and configuration fetching. Real-time control signals (TCP) and server discovery (UDP) are covered under Device Management (issues 6.8 and 6.9).

**Requirements:**
- Define DTOs (Data Transfer Objects) for network communication
- Create Retrofit API service interfaces
- Implement network interceptors for logging and error handling
- Configure connection timeout and retry policies
- Handle network connectivity status

**Related Requirements:** NET1, NET2

**Acceptance Criteria:**
- [ ] All HTTP API endpoints defined in ApiService
- [ ] DTOs created for all request/response types
- [ ] Network interceptors configured
- [ ] Connection error handling implemented
- [ ] 100% unit test coverage with MockWebServer
- [ ] Checkstyle compliant
- [ ] Javadoc for all public methods

**Dependencies:** Data Model Layer (for DTOs)

**Note:** TCP socket layer (6.8) and UDP discovery (6.9) are separate sub-issues under Device Management.

---

### [Android] UI & ViewModel Layer Implementation

- Labels: `android`, `enhancement`, `ui-layer`

**Description:**
Implement the presentation layer including Activities, Fragments, ViewModels, and XML layouts following MVVM architecture.

**Requirements:**
- Create ViewModels for each screen
- Implement Activities and Fragments with ViewBinding
- Design XML layouts following Material Design (monochrome)
- Implement RecyclerView adapters where needed
- Handle UI state management (loading, error, success)

**Related Requirements:** MAT1, MAT2, MAT3, MAT4, ACC1, ACC4

**Acceptance Criteria:**
- [ ] All ViewModels implemented with LiveData/StateFlow
- [ ] All Activities use ViewBinding
- [ ] Layouts follow Material Design (monochrome theme)
- [ ] Proper state management (loading, error, success)
- [ ] 100% unit test coverage for ViewModels
- [ ] UI tests for critical flows
- [ ] Checkstyle compliant

**Dependencies:** Repository Layer must be completed first

---

### [Android] Accessibility Features Implementation

- Labels: `android`, `enhancement`, `accessibility`

**Description:**
Implement accessibility features to support diverse learning needs, particularly for students with autism and other cognitive differences.

**Requirements:**
- Text-to-speech functionality
- Content simplification controls
- Content expansion controls
- Text summarization
 - Stylus and touch input optimisation
 - Minimal audiovisual stimuli (e-ink optimised)

**Related Requirements:** ACC1, ACC3, ACC4, MAT4

**Acceptance Criteria:**
- [ ] Text-to-speech service integrated
- [ ] Content transformation UI implemented
- [ ] Stylus input properly handled
- [ ] E-ink display optimizations applied
- [ ] Settings for teacher-controlled features
- [ ] 100% unit test coverage
- [ ] Accessibility tests passed

**Dependencies:** UI Layer (partial)

---

### [Android] Device Management & Kiosk Mode

- Labels: `android`, `enhancement`, `device-management`

**Description:**
Implement device management features including kiosk mode, battery monitoring, connection status, and remote control capabilities. Also includes the pairing process as specified in `Pairing Process.md`.

**Requirements:**
- Kiosk mode to prevent access to other apps
- Battery level monitoring and reporting
- Connection status tracking
- "Help needed" signal functionality
- Screen lock capability (teacher-controlled)
- Session end handling
- TCP socket layer for real-time control messages
- UDP broadcast listener for automatic server discovery
- Pairing process orchestration (UDP discovery + TCP pairing + HTTP registration)

**Related Requirements:** SYS1, CON2, CON5, NET1, NET2

**Acceptance Criteria:**
- [ ] Kiosk mode implemented and tested
- [ ] Battery monitoring service created
- [ ] Connection status tracking implemented
- [ ] Help signal functionality working
- [ ] Remote control handlers implemented
- [ ] TCP socket layer functional
- [ ] UDP discovery layer functional
- [ ] Pairing manager functional
- [ ] 100% unit test coverage
- [ ] Integration tests passed
- [ ] Checkstyle compliant

**Dependencies:** Network Layer (HTTP) must be completed first

---

## Sub-tasks: Data Model Layer

### 1.1 [Android] Create Material Entity and DAO

- Labels: `android`, `data-layer`  

**Description:**
Create the Material entity class with Room annotations and corresponding DAO interface for CRUD operations. This replaces the placeholder Lesson entity and includes support for key vocabulary terms.

**Important:** Entity IDs must be persistent across services. Materials created by the Windows teacher application will have IDs assigned by that application. The Android client must preserve these IDs when receiving materials via the network layer.

**Related Requirements:** MAT1, MAT6

**Tasks:**
- Create `MaterialEntity.java` with fields: id (String/UUID - assigned by creator, persistent across services), type (enum: QUIZ, WORKSHEET, POLL), title, content, metadata, vocabularyTerms (JSON array), timestamp
- Create `MaterialDao.java` interface with methods: getAll(), getById(), insert(), update(), delete()
- Add Material table to `ManuscriptaDatabase`
- Replace placeholder Lesson entity with Material entity
- Write unit tests for DAO operations

**Acceptance Criteria:**
- [ ] MaterialEntity with proper Room annotations
- [ ] Vocabulary terms field included for MAT6 support
- [ ] MaterialDao with all CRUD operations
- [ ] Database migration handled (Lesson → Material)
- [ ] 100% test coverage

---

### 1.2 [Android] Create Question Entity and DAO

- Labels: `android`, `data-layer`  

**Description:**
Create the Question entity for quiz/poll questions and corresponding DAO.

**Important:** Entity IDs must be persistent across services. Questions created by the Windows teacher application will have IDs assigned by that application. The Android client must preserve these IDs when receiving questions via the network layer.

**Tasks:**
- Create `QuestionEntity.java` with fields: id (String/UUID - assigned by creator, persistent across services), materialId (foreign key), questionText, questionType, options (JSON), correctAnswer
- Create `QuestionDao.java` interface
- Establish foreign key relationship with Material
- Write unit tests

**Acceptance Criteria:**
- [ ] QuestionEntity with foreign key to Material
- [ ] QuestionDao with query methods
- [ ] Cascade delete configured
- [ ] 100% test coverage

---

### 1.3 [Android] Create Response Entity and DAO

- Labels: `android`, `data-layer`  

**Description:**
Create the Response entity for storing student responses to questions.

**Important:** Entity IDs must be persistent across services. When the Android client creates a response, it must assign a globally unique ID (UUID) that will be preserved when the response is transmitted to the Windows teacher application. This ensures proper tracking and prevents ID conflicts.

**Tasks:**
- Create `ResponseEntity.java` with fields: id (String/UUID - generated by Android client, persistent across services), questionId (foreign key), selectedAnswer, isCorrect, timestamp, synced
- Create `ResponseDao.java` interface
- Include sync status tracking
- Write unit tests

**Acceptance Criteria:**
- [ ] ResponseEntity with sync tracking
- [ ] ResponseDao with sync queries
- [ ] Foreign key to Question
- [ ] 100% test coverage

---

### 1.4 [Android] Create Session Entity and DAO

- Labels: `android`, `data-layer`

**Description:**
Create the Session entity to track active learning sessions.

**Important:** Entity IDs must be persistent across services. When the Android client creates a session, it must assign a globally unique ID (UUID) that will be preserved when session data is transmitted to the Windows teacher application.

**Tasks:**
- Create `SessionEntity.java` with fields: id (String/UUID - generated by Android client, persistent across services), materialId, startTime, endTime, status, deviceId
- Create `SessionDao.java` interface
- Track session lifecycle
- Write unit tests

**Acceptance Criteria:**
- [ ] SessionEntity with lifecycle fields
- [ ] SessionDao with status queries
- [ ] Active session tracking
- [ ] 100% test coverage

---

### 1.5 [Android] Create Domain Models

- Labels: `android`, `data-layer`

**Description:**
Create domain model classes separate from entities for business logic layer (Clean Architecture).

**Tasks:**
- Create `Material.java` domain model
- Create `Question.java` domain model
- Create `Response.java` domain model
- Create `Session.java` domain model
- Implement mappers between entities and domain models

**Acceptance Criteria:**
- [ ] All domain models created
- [ ] Mappers implemented (Entity ↔ Domain)
- [ ] No Room annotations in domain models
- [ ] 100% test coverage for mappers

---

### 1.6 [Android] Create MaterialType Enum

- Labels: `android`, `data-layer`  

**Description:**
Create enum for different material types (quiz, worksheet, poll).

**Tasks:**
- Create `MaterialType.java` enum with values: LESSON, QUIZ, WORKSHEET, POLL
- Add helper methods (fromString, getDisplayName)
- Write unit tests

**Acceptance Criteria:**
- [ ] Enum with all material types
- [ ] Helper methods implemented
- [ ] 100% test coverage

---

### 1.7 [Android] Create QuestionType Enum

- Labels: `android`, `data-layer`  

**Description:**
Create enum for different question types (multiple choice, true/false, open-ended).

**Tasks:**
- Create `QuestionType.java` enum with values: MULTIPLE_CHOICE, TRUE_FALSE, WRITTEN_ANSWER, POLL
- Add helper methods
- Write unit tests

**Acceptance Criteria:**
- [ ] Enum with all question types
- [ ] Helper methods implemented
- [ ] 100% test coverage

---

### 1.8 [Android] Create DeviceStatus Entity and DAO

- Labels: `android`, `data-layer`

**Description:**
Create entity to track device status for reporting to teacher.

**Tasks:**
- Create `DeviceStatusEntity.java` with fields: deviceId, status (ON_TASK, HAND_RAISED, DISCONNECTED, LOCKED, IDLE), batteryLevel, currentMaterialId, studentView (for teacher live view feature), lastUpdated
- Create `DeviceStatusDao.java` interface
- Write unit tests

**Acceptance Criteria:**
- [ ] DeviceStatusEntity created
- [ ] DeviceStatusDao implemented
- [ ] Status enum defined
- [ ] 100% test coverage

---

Based on our analysis, here is a draft for the GitHub issue. I have identified that this refactoring specifically applies to **`SessionEntity`**, **`ResponseEntity`**, and **`DeviceStatusEntity`**, as `MaterialEntity` and `QuestionEntity` correctly rely on server-provided IDs and do not contain this anti-pattern.

### 1.9 [Android] Refactor Entity Initialization Logic to Domain Layer

  - **Labels:** `android`, `refactor`, `data-layer`, `clean-architecture`

**Description:**
Currently, several Room entities (`SessionEntity`, `ResponseEntity`, `DeviceStatusEntity`) contain business logic within convenience constructors (annotated with `@Ignore`). This includes generating UUIDs, capturing `System.currentTimeMillis()`, and setting default enum states.

This pattern violates Clean Architecture principles by coupling business rules (like ID generation and default states) to the Data Layer. It also bypasses the Domain Layer, making it possible to create valid entities without going through the proper domain model validation.

**Affected Classes:**

  - `SessionEntity` (Generates UUID, sets `ACTIVE` status, captures start time)
  - `ResponseEntity` (Generates UUID, captures timestamp, sets `synced=false`)
  - `DeviceStatusEntity` (Captures `lastUpdated` timestamp)

**Note:** `MaterialEntity` and `QuestionEntity` are **not** affected as they correctly treat IDs as immutable fields provided by the server.

**Proposed Solution:**

1.  **Remove Convenience Constructors:** Delete the `@Ignore` annotated constructors in the affected Entity classes. Entities should only have the single, all-args constructor used by Room and Mappers.
2.  **Add Factory Methods to Domain Models:** Implement static factory methods (e.g., `create()`) in the corresponding Domain classes (`Session`, `Response`, `DeviceStatus`).
3.  **Move Logic:** Move the ID generation (`UUID.randomUUID()`), timestamp capture, and default value assignment to these new factory methods.
4.  **Update Mappers:** Ensure Mappers (`SessionMapper`, etc.) strictly map fields without generating new data.

**Example (Session):**

*Domain Model (`Session.java`):*

```java
public static Session create(String materialId, String deviceId) {
    return new Session(
        UUID.randomUUID().toString(),
        materialId,
        System.currentTimeMillis(),
        0,
        SessionStatus.ACTIVE,
        deviceId
    );
}
```

*Entity (`SessionEntity.java`):*

```java
// Remove the public SessionEntity(String materialId, String deviceId) constructor entirely.
// Keep only the full constructor used by Room.
```

**Acceptance Criteria:**

  - [ ] `SessionEntity`, `ResponseEntity`, and `DeviceStatusEntity` contain **only** the primary constructor required by Room.
  - [ ] `Session`, `Response`, and `DeviceStatus` domain models contain static `create()` factory methods.
  - [ ] All business logic (UUIDs, timestamps, defaults) is moved to the Domain models.
  - [ ] Unit tests for Mappers are updated to reflect these changes.
  - [ ] Unit tests for Entities are updated to use the full constructor only.

---

## Sub-tasks: Repository Layer

### 2.1 [Android] Create MaterialRepository

- Labels: `android`, `repository-layer`

**Description:**
Implement repository for managing material data with network and local database synchronisation. Orchestrates attachment file downloads when materials contain file references, delegating file storage to `FileStorageManager`.

**Integration with TCP Layer (Heartbeat-Triggered Fetch):**
The MaterialRepository must integrate with the TCP socket layer to receive notifications when new materials are available. When TcpSocketManager receives a `DISTRIBUTE_MATERIAL` (0x05) signal from the server (in response to a heartbeat), it notifies the MaterialRepository to initiate an HTTP material fetch. This is the primary mechanism for material distribution since the server cannot push HTTP requests.

**Related Requirements:** MAT1, MAT8 (Teacher), MAT15, NET1

**Tasks:**
- Create `MaterialRepository.java` interface
- Create `MaterialRepositoryImpl.java` implementation
- Implement caching strategy (network-first with fallback)
- **Register as listener for DISTRIBUTE_MATERIAL signal from TcpSocketManager (issue 6.8)**
- **Implement `onFetchMaterialsSignal()` callback to trigger HTTP material sync**
- Orchestrate attachment downloads:
  1. Fetch MaterialDto from network via HTTP GET /materials
  2. Parse content for `/attachments/{id}` references using ContentParser
  3. Download each attachment via ApiService.getAttachment()
  4. Save binary data to internal storage via FileStorageManager (from issue 2.1a)
  5. Convert MaterialDto to MaterialEntity
  6. Insert into Room database
- Handle HTTP errors appropriately (return error Result if download fails)
- Write unit tests with mocked dependencies

**Acceptance Criteria:**
- [ ] Interface and implementation created
- [ ] DISTRIBUTE_MATERIAL signal listener registered with TcpSocketManager
- [ ] Attachment download orchestration implemented
- [ ] Caching logic implemented
- [ ] Error handling with Result pattern (fails gracefully on attachment errors)
- [ ] 100% test coverage

**Dependencies:** Issue 2.1a (FileStorageManager), Issue 6.8 (TCP Socket Layer - for DISTRIBUTE_MATERIAL signal) from the issues.md document.

---

### 2.1a [Android] Create FileStorageManager Utility

- Labels: `android`, `repository-layer`, `utility`

**Description:**
Create a utility class for managing binary file storage (attachments such as PDFs and images) in the app's internal storage. This separates file system concerns from repository logic.

**Related Requirements:** MAT8 (Teacher), MAT15

**Tasks:**
- Create `FileStorageManager.java` utility class
- Implement `saveAttachment(materialId, attachmentId, extension, bytes)` method
  - Use predictable path pattern: `/internal/attachments/{materialId}/{attachmentId}.{ext}`
- Implement `getAttachmentFile(materialId, attachmentId)` to retrieve saved files
- Implement `deleteAttachmentsForMaterial(materialId)` for cleanup on "end lesson"
- Implement `clearAllAttachments()` for full cache clear
- Handle file I/O exceptions gracefully
- Ensure thread-safety for concurrent access
- Write unit tests with mocked file system or temporary directories

**Acceptance Criteria:**
- [ ] FileStorageManager created with clear API
- [ ] Save, retrieve, and delete operations functional
- [ ] Predictable file path structure
- [ ] Exception handling implemented
- [ ] Thread-safe implementation
- [ ] 100% test coverage

---

### 2.2 [Android] Create ResponseRepository

- Labels: `android`, `repository-layer`

**Description:**
Implement repository for managing student responses with sync queue.

**Tasks:**
- Create `ResponseRepository.java` interface
- Create `ResponseRepositoryImpl.java` implementation
- Implement sync queue for offline responses
- Handle network retry logic
- Write unit tests

**Acceptance Criteria:**
- [ ] Interface and implementation created
- [ ] Sync queue implemented
- [ ] Retry logic with exponential backoff
- [ ] 100% test coverage

---

### 2.3 [Android] Create SessionRepository

- Labels: `android`, `repository-layer`

**Description:**
Implement repository for managing learning sessions.

**Tasks:**
- Create `SessionRepository.java` interface
- Create `SessionRepositoryImpl.java` implementation
- Track active session state
- Handle session lifecycle events
- Write unit tests

**Acceptance Criteria:**
- [ ] Interface and implementation created
- [ ] Session lifecycle managed
- [ ] Active session tracking
- [ ] 100% test coverage

---

### 2.4 [Android] Create DeviceStatusRepository

- Labels: `android`, `repository-layer`

**Description:**
Implement repository for managing and reporting device status to teacher. Acts as a facade over the TCP socket layer for status updates and the local database for status persistence.

**Related Requirements:** CON2A, CON5

**Tasks:**
- Create `DeviceStatusRepository.java` interface
- Create `DeviceStatusRepositoryImpl.java` implementation
- Implement periodic status reporting via TCP socket (using TcpSocketManager from issue 6.8 in the issues.md document.)
- Implement local status persistence for offline resilience
- Handle battery level monitoring integration
- Provide observable status state for UI (LiveData/StateFlow)
- Write unit tests

**Acceptance Criteria:**
- [ ] Interface and implementation created
- [ ] Status reporting via TCP socket implemented
- [ ] Local status persistence working
- [ ] Battery monitoring integrated
- [ ] Observable status for UI
- [ ] 100% test coverage

**Dependencies:** Issue 6.8 (TCP Socket Layer) from the issues.md document.

---

### 2.5 [Android] Create Result/State Wrapper Classes

- Labels: `android`, `repository-layer`  

**Description:**
Create generic Result and UiState wrapper classes for error handling and UI state management.

**Tasks:**
- Create `Result.java` sealed class with Success/Error states
- Create `UiState.java` class with Loading/Success/Error states
- Add helper factory methods
- Write unit tests

**Acceptance Criteria:**
- [ ] Result class with proper pattern
- [ ] UiState class created
- [ ] Factory methods implemented
- [ ] 100% test coverage

---

### 2.6 [Android] Implement Repository Module (DI)

- Labels: `android`, `repository-layer`, `dependency-injection`

**Description:**
Create Hilt module for providing repository instances.

**Tasks:**
- Create `RepositoryModule.java`
- Add @Provides methods for all repositories
- Ensure proper scoping (@Singleton)
- Write integration tests

**Acceptance Criteria:**
- [ ] RepositoryModule created
- [ ] All repositories provided
- [ ] Proper dependency injection
- [ ] Integration tests passed

---

## Sub-tasks: Network Layer

### 3.1 [Android] Create Material DTOs

- Labels: `android`, `network-layer`  

**Description:**
Create Data Transfer Objects for material-related API communication. Materials may reference attachment files (PDFs, images) via URLs in the content field.

**Important:** DTOs must include the entity ID field (String/UUID) assigned by the Windows teacher application. The Android client must preserve these IDs exactly as received, without modification or regeneration.

**Related Requirements:** MAT1, MAT6, MAT8 (Teacher), MAT15

**Tasks:**
- Create `MaterialDto.java` with @SerializedName annotations, including:
  - id field (String/UUID)
  - content field (may contain `/attachments/{id}` URL references)
  - vocabularyTerms field for MAT6 support
  - metadata field
- Create `MaterialListResponseDto.java` - Contains a `materials` field with a list of material IDs in presentation order
- Create `VocabularyTermDto.java` for key vocabulary
- Create `ContentParser.java` utility to extract attachment IDs from content URLs (e.g., `/attachments/abc-123` → `abc-123`)
- Create mapper methods (DTO → Domain) that preserve entity IDs
- Write unit tests

**Acceptance Criteria:**
- [ ] DTOs with proper JSON annotations
- [ ] Content parser extracts attachment references
- [ ] Vocabulary terms field included
- [ ] Mappers implemented
- [ ] 100% test coverage

---

### 3.2 [Android] Create Question DTOs

- Labels: `android`, `network-layer`  

**Description:**
Create DTOs for question-related API communication.

**Tasks:**
- Create `QuestionDto.java`
- Create `QuestionListDto.java`
- Create mapper methods
- Write unit tests

**Acceptance Criteria:**
- [ ] DTOs created
- [ ] Mappers implemented
- [ ] 100% test coverage

---

### 3.3 [Android] Create Response DTOs

- Labels: `android`, `network-layer`  

**Description:**
Create DTOs for submitting responses to teacher.

**Important:** Response DTOs must include the entity ID (String/UUID) generated by the Android client. This ID must be created when the response is first recorded and must be included in the network transmission so the Windows teacher application can preserve it.

**Tasks:**
- Create `ResponseDto.java` (request) with id field (String/UUID generated by Android)
- Create `ResponseResultDto.java` (response with feedback)
- Create `BatchResponseDto.java` for bulk submissions
- Create mapper methods that include client-generated IDs
- Write unit tests

**Acceptance Criteria:**
- [ ] Request/response DTOs created
- [ ] Batch submission DTO created
- [ ] Mappers implemented
- [ ] 100% test coverage

---

### 3.4 [Android] Create Device Status DTOs

- Labels: `android`, `network-layer`  

**Description:**
Create DTOs for device status reporting.

**Tasks:**
- Create `DeviceStatusDto.java` with fields: deviceId, status, batteryLevel, currentMaterialId, placeholderStudentView
- Create `DeviceInfoDto.java` (device metadata)
- Create mapper methods
- Write unit tests

**Acceptance Criteria:**
- [ ] DTOs created
- [ ] Device info included
- [ ] Mappers implemented
- [ ] 100% test coverage

---

### 3.5 [Android] Define API Endpoints in ApiService

- Labels: `android`, `network-layer`

**Description:**
Define all Retrofit API endpoints for HTTP communication with teacher server, including binary attachment downloads. Note: Real-time control signals (lock/unlock, status updates, raise hand) are handled via TCP socket (issue 6.8), not HTTP.

**Related Requirements:** NET1, NET2, MAT8 (Teacher), MAT15

**Tasks:**
- Add `@GET` method for fetching materials: `getMaterials()` - Returns a list of material IDs in presentation order
- Add `@GET` method for material details: `getMaterialById(@Path id)`
- Add `@GET` method for attachments: `getAttachment(@Path id)` - Returns `ResponseBody` for binary data (PDF, images)
- Add `@GET` method for configuration: `getConfig()` - Fetches tablet configuration
- Add `@POST` method for submitting response: `submitResponse(@Body)`
- Add `@POST` method for batch responses: `submitBatchResponses(@Body)`
- Add `@POST` method for device registration: `registerDevice(@Body)` - Initial device pairing
- Write tests with MockWebServer (including binary response mocking for attachments)

**Acceptance Criteria:**
- [ ] All HTTP endpoints defined
- [ ] Attachment endpoint returns `ResponseBody` for binary data
- [ ] Proper HTTP methods used
- [ ] Path/query parameters configured
- [ ] Registration endpoint included
- [ ] 100% test coverage

**Note:** Device status updates, raise hand, and lock/unlock commands use TCP socket (issue 6.8), not HTTP.

---

### 3.6 [Android] Implement Network Interceptors

- Labels: `android`, `network-layer`

**Description:**
Create interceptors for logging, error handling, and authentication.

**Tasks:**
- Create `LoggingInterceptor.java` for request/response logging
- Create `ErrorInterceptor.java` for standardized error handling
- Create `AuthInterceptor.java` for device identification
- Add interceptors to OkHttpClient in NetworkModule
- Write unit tests

**Acceptance Criteria:**
- [ ] All interceptors created
- [ ] Properly configured in NetworkModule
- [ ] Error handling standardized
- [ ] 100% test coverage

---

### 3.7 [Android] Implement Connection Manager

- Labels: `android`, `network-layer`

**Description:**
Create utility class for monitoring network connectivity and server reachability.

**Tasks:**
- Create `ConnectionManager.java`
- Monitor WiFi/network connectivity
- Implement server reachability check (ping)
- Provide connection state LiveData
- Write unit tests

**Acceptance Criteria:**
- [ ] ConnectionManager created
- [ ] Network state monitoring working
- [ ] Server reachability check implemented
- [ ] 100% test coverage

---

### 3.8 [Android] Implement Retry Policy

- Labels: `android`, `network-layer`

**Description:**
Create retry logic for failed network requests with exponential backoff.

**Tasks:**
- Create `RetryInterceptor.java`
- Implement exponential backoff algorithm
- Configure max retry attempts
- Handle specific error codes (5xx retry, 4xx fail)
- Write unit tests

**Acceptance Criteria:**
- [ ] Retry interceptor created
- [ ] Exponential backoff implemented
- [ ] Configurable retry policy
- [ ] 100% test coverage

---

## Sub-tasks: UI & ViewModel Layer

### 4.1 [Android] Create Material List Screen

- Labels: `android`, `ui-layer`

**Description:**
Implement screen to display list of available materials.

**Tasks:**
- Create `MaterialListViewModel.java`
- Create `MaterialListActivity.java` with ViewBinding
- Create `activity_material_list.xml` layout
- Create `item_material.xml` for RecyclerView
- Create `MaterialListAdapter.java`
- Handle loading/error/empty states
- Write unit tests for ViewModel, UI tests for Activity

**Acceptance Criteria:**
- [ ] ViewModel with LiveData
- [ ] Activity with ViewBinding
- [ ] RecyclerView with adapter
- [ ] State management implemented
- [ ] 100% ViewModel test coverage
- [ ] UI tests passed

---

### 4.2 [Android] Create Material Detail Screen (Quiz)

- Labels: `android`, `ui-layer`

**Description:**
Implement screen for displaying and answering quiz questions.

**Tasks:**
- Create `QuizViewModel.java`
- Create `QuizActivity.java`
- Create `activity_quiz.xml` layout
- Create question display UI with options
- Implement answer selection
- Show immediate feedback (✓ or ✗ with explanation)
- Implement "Try Again" for incorrect answers
- Write tests

**Acceptance Criteria:**
- [ ] ViewModel with question navigation
- [ ] Answer submission logic
- [ ] Feedback UI (correct/incorrect)
- [ ] Try Again functionality
- [ ] 100% test coverage

---

### 4.3 [Android] Create Material Detail Screen (Worksheet)

- Labels: `android`, `ui-layer`

**Description:**
Implement screen for displaying and working through worksheets with handwriting annotation support.

**Related Requirements:** MAT1, MAT4, MAT8, ACC1

**Tasks:**
- Create `WorksheetViewModel.java`
- Create `WorksheetActivity.java`
- Create `activity_worksheet.xml` layout
- Implement scrollable content view
- Add simplify/expand/summarize buttons
- **Implement handwriting annotation layer (MAT8):**
  - Create `AnnotationCanvasView.java` for drawing with stylus
  - Add annotation toolbar (pen, eraser, color selection)
  - Implement stroke capture and rendering
  - Add annotation persistence to local database
  - Sync annotations to teacher server
- Write tests

**Acceptance Criteria:**
- [ ] ViewModel created
- [ ] Worksheet display implemented
- [ ] Content transformation buttons working
- [ ] Annotation layer functional with stylus input
- [ ] Annotations persist and sync to teacher
- [ ] 100% test coverage

---

### 4.4 [Android] Create Material Detail Screen (Poll)

- Labels: `android`, `ui-layer`

**Description:**
Implement screen for displaying and responding to polls.

**Tasks:**
- Create `PollViewModel.java`
- Create `PollActivity.java`
- Create `activity_poll.xml` layout
- Implement option selection UI
- Submit poll response
- Show confirmation after submission
- Write tests

**Acceptance Criteria:**
- [ ] ViewModel created
- [ ] Poll UI implemented
- [ ] Response submission working
- [ ] 100% test coverage

---

### 4.5 [Android] Create Session Status Bar Component

- Labels: `android`, `ui-layer`  

**Description:**
Create a status bar component showing connection status, battery level, and raise hand button.

**Related Requirements:** MAT7, CON2, CON5, CON12

**Tasks:**
- Create `SessionStatusView.java` custom view
- Create `layout_session_status.xml`
- Show connection indicator
- Show battery level
- Add "Raise Hand" button (MAT7)
- Write tests

**Acceptance Criteria:**
- [ ] Custom view created
- [ ] All indicators functional
- [ ] Raise Hand button working
- [ ] 100% test coverage

---

### 4.6 [Android] Create Feedback Dialog

- Labels: `android`, `ui-layer`  

**Description:**
Create dialog for showing immediate feedback on quiz/poll responses.

**Tasks:**
- Create `FeedbackDialogFragment.java`
- Create `dialog_feedback.xml` layout
- Show ✓ or ✗ icon
- Display explanation text
- Add "Try Again" or "Continue" button
- Write tests

**Acceptance Criteria:**
- [ ] DialogFragment created
- [ ] Correct/incorrect variants
- [ ] Action buttons working
- [ ] UI tests passed

---

### 4.7 [Android] Create Loading States

- Labels: `android`, `ui-layer`  

**Description:**
Implement loading indicators and empty state views.

**Tasks:**
- Create `LoadingView.java` custom view
- Create `EmptyStateView.java` custom view
- Create corresponding XML layouts
- Add to all list/detail screens
- Write tests

**Acceptance Criteria:**
- [ ] Loading view created
- [ ] Empty state view created
- [ ] Integrated in all screens
- [ ] UI tests passed

---

### 4.8 [Android] Create Monochrome Theme

- Labels: `android`, `ui-layer`  

**Description:**
Create Material Design theme optimised for e-ink displays (monochrome, high contrast).

**Tasks:**
**Tasks:**
- Create `themes.xml` with monochrome colour palette
- Define black/white/grey colour resources
- Remove all colour images/icons (use vector drawables)
- Optimise for e-ink refresh rates
- Test on actual e-ink device if possible

**Acceptance Criteria:**
- [ ] Theme created in themes.xml
- [ ] All colours monochrome
- [ ] High contrast for readability
- [ ] E-ink optimised

---

### 4.9 [Android] Implement Navigation Flow

- Labels: `android`, `ui-layer`

**Description:**
Implement navigation between screens and handle back stack.

**Tasks:**
- Define navigation intents
- Handle deep linking to specific materials
- Manage back stack properly
- Implement session exit confirmation
- Write navigation tests

**Acceptance Criteria:**
- [ ] Navigation working between all screens
- [ ] Back stack managed properly
- [ ] Exit confirmation dialog
- [ ] Navigation tests passed

---

## Sub-tasks: Accessibility Features

### 5.1 [Android] Implement Text-to-Speech Service

- Labels: `android`, `accessibility`

**Description:**
Integrate Android TextToSpeech API for reading content aloud.

**Tasks:**
- Create `TextToSpeechManager.java`
- Initialize TTS engine
- Implement read-aloud functionality
- Add TTS button to material screens
- Handle TTS lifecycle (start/stop/pause)
- Respect teacher-enabled setting
- Write tests

**Acceptance Criteria:**
- [ ] TTS service implemented
- [ ] Read-aloud working for all content
- [ ] Teacher control setting respected
- [ ] 100% test coverage

---

### 5.2 [Android] Implement Content Simplification

- Labels: `android`, `accessibility`

**Description:**
Implement API integration for simplifying text content for different reading levels.

**Tasks:**
- Add simplification endpoint to ApiService
- Create `ContentTransformationService.java`
- Implement UI for simplify button
- Handle loading state during transformation
- Cache simplified versions locally
- Write tests

**Acceptance Criteria:**
- [ ] Simplification working
- [ ] UI button functional
- [ ] Caching implemented
- [ ] 100% test coverage

---

### 5.3 [Android] Implement Content Expansion

- Labels: `android`, `accessibility`

**Description:**
Implement API integration for expanding text content with more details.

**Tasks:**
- Add expansion endpoint to ApiService
- Add expansion logic to `ContentTransformationService.java`
- Implement UI for expand button
- Handle loading state
- Cache expanded versions
- Write tests

**Acceptance Criteria:**
- [ ] Expansion working
- [ ] UI button functional
- [ ] Caching implemented
- [ ] 100% test coverage

---

### 5.4 [Android] Implement Content Summarization

- Labels: `android`, `accessibility`

**Description:**
Implement API integration for summarizing text content.

**Tasks:**
- Add summarization endpoint to ApiService
- Add summarization logic to `ContentTransformationService.java`
- Implement UI for summarize button
- Handle loading state
- Cache summaries
- Write tests

**Acceptance Criteria:**
- [ ] Summarization working
- [ ] UI button functional
- [ ] Caching implemented
- [ ] 100% test coverage

---

### 5.5 [Android] Optimise for Stylus Input

- Labels: `android`, `accessibility`  

**Description:**
Optimise touch handling for stylus input on worksheets and annotation gestures.

**Related Requirements:** ACC1, MAT8

**Tasks:**
- Configure touch event handling for stylus
- Optimise for annotation gestures (MAT8 - drawing, erasing)
- Increase touch target sizes for buttons and UI elements
- Implement palm rejection if possible
- Test with stylus on e-ink device
- Write tests

**Acceptance Criteria:**
- [ ] Stylus input optimised for annotations
- [ ] Touch targets appropriately sized
- [ ] Palm rejection implemented
- [ ] Tested on actual e-ink device
- [ ] Tests passed

---

### 5.6 [Android] Create Accessibility Settings

- Labels: `android`, `accessibility`

**Description:**
Create settings screen for accessibility features (teacher-controlled via sync).

**Tasks:**
- Create `AccessibilitySettings.java` data class
- Create settings sync endpoint
- Store settings in SharedPreferences
- Apply settings across app
- Write tests

**Acceptance Criteria:**
- [ ] Settings model created
- [ ] Sync from teacher working
- [ ] Settings applied correctly
- [ ] 100% test coverage

---

## Sub-tasks: Device Management & Kiosk Mode

### 6.1 [Android] Implement Kiosk Mode

- Labels: `android`, `device-management`

**Description:**
Implement kiosk mode to lock device into the Manuscripta app.

**Tasks:**
- Create `KioskManager.java`
- Implement lock task mode (Android Enterprise)
- Handle home button override
- Implement unlock mechanism (teacher-triggered)
- Request necessary permissions
- Write tests

**Acceptance Criteria:**
- [ ] Kiosk mode functional
- [ ] Device locked to app
- [ ] Teacher unlock working
- [ ] 100% test coverage

---

### 6.2 [Android] Implement Battery Monitoring Service

- Labels: `android`, `device-management`

**Description:**
Create service to monitor battery level and report to teacher.

**Tasks:**
- Create `BatteryMonitorService.java`
- Register BatteryManager listener
- Track battery level and charging status
- Send periodic updates to teacher
- Trigger alert when battery low (<20%)
- Write tests

**Acceptance Criteria:**
- [ ] Service created
- [ ] Battery monitoring working
- [ ] Periodic reporting implemented
- [ ] Low battery alerts sent
- [ ] 100% test coverage

---

### 6.3 [Android] Implement Connection Status Tracking

- Labels: `android`, `device-management`

**Description:**
Track connection status to teacher server and report disconnections. Uses TCP socket connection state as the primary connection indicator.

**Related Requirements:** CON2A (device status grid)

**Tasks:**
- Create `ConnectionMonitorService.java`
- Monitor TCP socket connection state (from TcpSocketManager, issue 6.8)
- Implement heartbeat mechanism via TCP keep-alive
- Detect disconnections (socket closed/timeout)
- Trigger reconnection via TcpSocketManager with exponential backoff
- Update device status (CONNECTED/DISCONNECTED) and notify UI
- Write tests

**Acceptance Criteria:**
- [ ] Service created
- [ ] TCP connection state monitoring working
- [ ] Heartbeat mechanism working
- [ ] Reconnection logic delegated to TcpSocketManager
- [ ] Status updates sent
- [ ] 100% test coverage

**Dependencies:** Issue 6.8 (TCP Socket Layer) from the issues.md document.

---

### 6.4 [Android] Implement Raise Hand Feature

- Labels: `android`, `device-management`  

**Description:**
Create "Raise Hand" button and signal functionality to request teacher assistance. The hand raised signal is sent via TCP socket (opcode 0x11) for immediate delivery, and the server responds with HAND_ACK (opcode 0x06) to confirm receipt.

**Related Requirements:** MAT7, CON12

**Tasks:**
- Add Raise Hand button to status bar (issue 4.5 in issues.md document.)
- Create `RaiseHandManager.java`
- Send HAND_RAISED message (opcode 0x11) via TCP socket (TcpSocketManager, issue 6.8 in the issues.md document.)
- Listen for HAND_ACK (opcode 0x06) response from server via TcpSocketManager listener
- Show confirmation to student only after receiving HAND_ACK ("Help requested")
- Handle timeout if HAND_ACK not received (show error/retry option)
- Write tests

**Acceptance Criteria:**
- [ ] Raise Hand button functional
- [ ] HAND_RAISED (0x11) sent via TCP socket
- [ ] HAND_ACK (0x06) received and handled
- [ ] Confirmation shown only after ACK received
- [ ] Timeout handling implemented
- [ ] 100% test coverage

**Dependencies:** Issue 6.8 (TCP Socket Layer ), Issue 4.5 (Session Status Bar) from the issues.md document.

---

### 6.5 [Android] Implement Remote Screen Lock

- Labels: `android`, `device-management`

**Description:**
Implement ability for teacher to remotely lock student screen. Lock/unlock commands are received via TCP socket (opcodes 0x01 and 0x02).

**Related Requirements:** CON6

**Tasks:**
- Create `RemoteControlService.java`
- Register as listener for TCP messages (via TcpSocketManager, issue 6.8)
- Handle LOCK_SCREEN (0x01) command - display lock overlay
- Handle UNLOCK_SCREEN (0x02) command - remove lock overlay
- Handle REFRESH_CONFIG (0x03) command - trigger HTTP config refresh
- Create lock overlay UI (full-screen, blocks input)
- Write tests

**Acceptance Criteria:**
- [ ] Service created
- [ ] TCP message listener registered
- [ ] Lock command received and handled via TCP
- [ ] Unlock command working via TCP
- [ ] Config refresh command working
- [ ] Lock overlay displayed correctly
- [ ] 100% test coverage

**Dependencies:** Issue 6.8 (TCP Socket Layer) from the issues.md document. 

---

### 6.6 [Android] Implement Session End Handling

- Labels: `android`, `device-management`

**Description:**
Handle teacher-initiated session end command.

**Tasks:**
- Add session end endpoint
- Listen for end commands
- Save unsaved work
- Clear session data
- Return to home/locked state
- Show confirmation message
- Write tests

**Acceptance Criteria:**
- [ ] End command received
- [ ] Data saved properly
- [ ] Session cleared
- [ ] Confirmation shown
- [ ] 100% test coverage

---

### 6.7 [Android] Create Device Registration Flow

- Labels: `android`, `device-management`

**Description:**
Implement device registration/identification with teacher server on first launch. This issue covers the UI flow for device registration; the actual pairing logic is orchestrated by PairingManager (issue 6.10) following the process defined in `Pairing Process.md` §2.

**Note:** Device registration is part of the broader pairing process which requires:
1. UDP discovery of teacher device (issue 6.9)
2. TCP pairing handshake (issue 6.8)
3. HTTP registration POST /pair (this issue's UI flow, orchestrated by issue 6.10)

**Tasks:**
- Create device ID generation (UUID) - stored persistently
- Create registration UI showing pairing progress:
  - Discovering teacher... (UDP phase)
  - Connecting... (TCP + HTTP phase)
  - Paired successfully / Pairing failed
- Integrate with PairingManager (issue 6.10) for orchestration
- Display pairing state from PairingManager's observable state
- Handle user-initiated retry on failure
- Store registration status
- Write tests

**Acceptance Criteria:**
- [ ] Device ID generated and persisted
- [ ] Registration UI created with progress indicators
- [ ] Pairing state displayed from PairingManager
- [ ] Retry functionality on failure
- [ ] Registration persisted
- [ ] 100% test coverage

**Dependencies:** Issue 6.9 (UDP Discovery Layer), Issue 6.10 (Pairing Manager) from the issues.md document. 

---

### 6.8 [Android] Implement TCP Socket Layer

> **GitHub Parent Issue:** [#60](https://github.com/raphaellith/Manuscripta/issues/60)

- Labels: `android`, `device-management`, `network-layer`

**Description:**
Implement TCP socket communication for low-latency, real-time control signals between the Android client and teacher server. This handles bidirectional messaging using a binary protocol with opcodes as defined in the API Contract. This layer also handles the TCP portion of the pairing handshake as specified in `Pairing Process.md` §2.

**Critical Design Pattern - Heartbeat-Triggered Material Fetch:**
Since the Windows server cannot initiate HTTP requests to Android clients, material distribution uses a heartbeat-triggered pattern:
1. Android sends periodic `STATUS_UPDATE` (0x10) heartbeat via TCP
2. Server checks if new materials are available for this device
3. If materials pending, server responds with `DISTRIBUTE_MATERIAL` (0x05)
4. Android receives signal and initiates HTTP `GET /materials` to download content

This pattern applies to all server-initiated content delivery (materials, config changes, etc.).

**Related Requirements:** CON2A, CON6, CON12, NET1, SYS (30 device support)

**Protocol Reference (API Contract Section 3):**
- **Port:** 5912 (TCP_PORT)
- **Message Structure:** 1-byte opcode + variable-length operand
- **Pairing Messages (Section 3.5):**
  - **Client → Server:** PAIRING_REQUEST (0x20, Device ID as UTF-8 string operand)
  - **Server → Client:** PAIRING_ACK (0x21, no operand)
- **Control Messages (Server → Client):** LOCK_SCREEN (0x01), UNLOCK_SCREEN (0x02), REFRESH_CONFIG (0x03), UNPAIR (0x04), DISTRIBUTE_MATERIAL (0x05), HAND_ACK (0x06)
- **Status Messages (Client → Server):** STATUS_UPDATE (0x10), HAND_RAISED (0x11), DISTRIBUTE_ACK (0x12)

**Acceptance Criteria:**
- [ ] TcpSocketManager created and manages connection lifecycle
- [ ] Binary message encoding/decoding functional
- [ ] TCP pairing handshake implemented (PAIRING_REQUEST/PAIRING_ACK)
- [ ] All message types implemented per API Contract (including DISTRIBUTE_MATERIAL 0x05)
- [ ] Heartbeat-triggered material fetch pattern implemented
- [ ] Listener interface for incoming server commands
- [ ] Reconnection with exponential backoff
- [ ] Heartbeat mechanism functional
- [ ] Thread-safe implementation
- [ ] Unknown opcodes handled gracefully
- [ ] 100% test coverage
- [ ] Checkstyle compliant
- [ ] Javadoc for all public methods

**Dependencies:** Issue 6.9 (UDP Discovery Layer - for server IP), Issue 6.10 (Pairing Manager)

---

#### Sub-Issues Overview
| # | Title | Status | Issue |
|---|-------|--------|-------|
| 60.1 | Message Protocol Classes and Opcode Enum | Starter / Parallel | [#91](https://github.com/raphaellith/Manuscripta/issues/91) |
| 60.2 | Message Encoder and Decoder | Depends on: #91 | [#92](https://github.com/raphaellith/Manuscripta/issues/92) |
| 60.3 | TcpSocketManager Skeleton and Connection Lifecycle | Depends on: #92 | [#93](https://github.com/raphaellith/Manuscripta/issues/93) |
| 60.4 | Message Listener System | Depends on: #93 | [#94](https://github.com/raphaellith/Manuscripta/issues/94) |
| 60.5 | Heartbeat Mechanism | Depends on: #93, #94 | [#95](https://github.com/raphaellith/Manuscripta/issues/95) |
| 60.6 | Pairing Handshake Integration | Depends on: #91, #93, #94, #59 | [#96](https://github.com/raphaellith/Manuscripta/issues/96) |

---

### 6.9 [Android] Implement UDP Discovery Layer

> **GitHub Parent Issue:** [#59](https://github.com/raphaellith/Manuscripta/issues/59)

- Labels: `android`, `device-management`, `network-layer`

**Description:**
Implement UDP broadcast listener for automatic teacher server discovery on the local network. This allows student tablets to discover the teacher laptop without manual IP configuration. This is Phase 1 of the pairing process as specified in `Pairing Process.md` §2(1).

**Related Requirements:** NET1 (LAN communication)

**Protocol Reference (API Contract Section 1.1 and Section 3.3):**
- **Port:** 5913 (UDP_PORT)
- **Opcode:** 0x00 (DISCOVERY)
- **Binary Message Format (9 bytes total):**

  | Field | Offset | Size | Description |
  |-------|--------|------|-------------|
  | Opcode | 0 | 1 byte | `0x00` = DISCOVERY |
  | IP Address | 1 | 4 bytes | IPv4 address (network byte order, big-endian) |
  | HTTP Port | 5 | 2 bytes | Unsigned, little-endian |
  | TCP Port | 7 | 2 bytes | Unsigned, little-endian |

- **Example:** For 192.168.1.100, HTTP 5911, TCP 5912:
  ```
  Byte 0:      0x00                         (DISCOVERY opcode)
  Bytes 1-4:   0xC0 0xA8 0x01 0x64          (192.168.1.100)
  Bytes 5-6:   0x17 0x17                    (5911 little-endian)
  Bytes 7-8:   0x18 0x17                    (5912 little-endian)
  ```
- Teacher broadcasts every 3 seconds

**Acceptance Criteria:**
- [ ] UdpDiscoveryManager created and listens on UDP port
- [ ] Binary message parsing functional (9-byte format)
- [ ] Opcode validation (0x00)
- [ ] IPv4 address parsing (big-endian)
- [ ] Port parsing (little-endian, unsigned)
- [ ] Server info stored and accessible
- [ ] Observable discovery state for UI
- [ ] Multiple teacher handling (show selection or use most recent)
- [ ] Timeout handling implemented
- [ ] Network permissions handled
- [ ] 100% test coverage
- [ ] Checkstyle compliant
- [ ] Javadoc for all public methods

**Technical Notes:**
- Use `DatagramSocket` for UDP listening
- Run listener on background thread (ExecutorService or Coroutine)
- Use `ByteBuffer` with appropriate byte order for parsing
- Consider Android 12+ restrictions on broadcasts
- May need to request CHANGE_WIFI_MULTICAST_STATE permission for some devices

**Dependencies:** None (this is the first step in pairing)

---

#### Sub-Issues Overview
| # | Title | Status | Issue |
|---|-------|--------|-------|
| 59.1 | DiscoveryMessage Data Class and Binary Parser | Starter / Parallel | [#88](https://github.com/raphaellith/Manuscripta/issues/88) |
| 59.2 | UdpDiscoveryManager Implementation | Depends on: #88 | [#89](https://github.com/raphaellith/Manuscripta/issues/89) |
| 59.3 | Discovery State and Error Handling | Depends on: #89 | [#90](https://github.com/raphaellith/Manuscripta/issues/90) |

---

### 6.10 [Android] Implement Pairing Manager

- Labels: `android`, `device-management`, `network-layer`

**Description:**
Implement a coordinator class that orchestrates the full pairing process as specified in `Pairing Process.md` §2. This manager coordinates UDP discovery, TCP pairing handshake, and HTTP device registration to establish a complete connection with the teacher's Windows application.

**Related Requirements:** NET1 (LAN communication)

**Pairing Process Reference (`Pairing Process.md` §2):**
1. **Phase 1 - Discovery:** Listen for UDP broadcast from Windows device (issue 6.9)
2. **Phase 2a - TCP Pairing:** Send PAIRING_REQUEST (0x20) via TCP, await PAIRING_ACK (0x21) (issue 6.8)
3. **Phase 2b - HTTP Registration:** POST to `/pair` endpoint with deviceId, await 201 Created
4. **Completion:** Both TCP and HTTP handshakes must succeed for pairing to be complete

**Tasks:**
- Create `PairingManager.java` singleton/service class
- Generate and persist device ID (UUID) on first launch
- Create `PairingState.java` enum: IDLE, DISCOVERING, TCP_PAIRING, HTTP_PAIRING, PAIRED, FAILED
- Implement state machine for pairing process
- **Orchestrate pairing phases:**
  1. Receive discovery info from UdpDiscoveryManager (issue 6.9)
  2. Initiate TCP connection and pairing handshake via TcpSocketManager (issue 6.8)
  3. Initiate HTTP registration via ApiService POST /pair
  4. Track completion of both channels
- Provide observable pairing state for UI (LiveData/StateFlow)
- Handle partial failures (one channel succeeds, other fails):
  - Per `Pairing Process.md` §1(4): If any phase fails, restart entire process
- Implement retry logic with user feedback
- Store pairing status persistently (SharedPreferences)
- Handle re-pairing scenarios (reconnection after disconnect)
- Write comprehensive unit tests

**Acceptance Criteria:**
- [ ] PairingManager created with state machine
- [ ] Device ID generated and persisted
- [ ] UDP discovery integrated
- [ ] TCP pairing handshake triggered and tracked
- [ ] HTTP registration triggered and tracked
- [ ] Both channels must succeed for PAIRED state
- [ ] Partial failure handling (restart process)
- [ ] Observable pairing state for UI
- [ ] Pairing status persisted
- [ ] Reconnection handling
- [ ] 100% test coverage
- [ ] Checkstyle compliant
- [ ] Javadoc for all public methods

**Dependencies:** Issue 6.8 (TCP Socket Layer), Issue 6.9 (UDP Discovery Layer), Issue 3.5 (ApiService - POST /pair endpoint)

---

## Sub-tasks: New Requirements (Client Feedback)

### 11.1 [Android] Key Vocabulary Display Component

- Labels: `android`, `ui-layer`, `accessibility`, `new-requirement`

**Description:**
Create dedicated UI component to display teacher-defined Key Vocabulary terms with highlighting mechanism for each lesson.

**Related Requirements:** MAT6 (Student), MAT15 (Teacher)

**Tasks:**
- Create `VocabularyTerm.java` data model with fields: term, definition, context
- Create `VocabularyDisplayView.java` custom component for dedicated display area
- Implement text highlighting for vocabulary terms within material content
- Add vocabulary panel/sidebar to material display screens
- Sync vocabulary terms from teacher via network layer
- Store vocabulary terms in local database
- Implement "tap to define" interaction for highlighted terms
- Write unit tests and UI tests

**Acceptance Criteria:**
- [ ] VocabularyTerm model created
- [ ] VocabularyDisplayView component functional
- [ ] Text highlighting mechanism working
- [ ] Vocabulary synced from teacher
- [ ] Integrated with Quiz, Worksheet, and Poll screens
- [ ] Tap-to-define interaction implemented
- [ ] 100% test coverage
- [ ] Checkstyle compliant
- [ ] Javadoc for all public methods

**Dependencies:** Issue 1.1 (Material Entity with vocabulary field), Issue 3.1 (Material DTOs), Issue 4 (UI screens) from the issues.md document.

---

### 11.2 [Android] Handwriting Annotation Feature

- Labels: `android`, `ui-layer`, `stylus`, `new-requirement`

**Description:**
Implement comprehensive handwriting annotation capability for worksheets and PDFs with stylus support, including stroke capture, rendering, persistence, and teacher synchronization.

**Related Requirements:** MAT8, ACC1

**Tasks:**
- Create `AnnotationLayer.java` custom view extending View/SurfaceView
- Implement stroke capture with MotionEvent handling (ACTION_DOWN, ACTION_MOVE, ACTION_UP)
- Implement stroke rendering using Canvas and Path
- Create annotation toolbar with tools: pen, eraser, color selector, undo/redo
- Create `Annotation.java` data model with fields: strokes, color, timestamp, worksheetId
- Create `AnnotationEntity.java` and `AnnotationDao.java` for local persistence
- Implement serialization/deserialization of stroke data (JSON or binary)
- Create `AnnotationDto.java` for network transmission
- Add annotation sync endpoint to ApiService
- Implement sync to teacher (real-time or on save)
- Integrate AnnotationLayer into WorksheetActivity (overlay on content)
- Handle PDF annotation overlay positioning
- Optimize for e-ink display (stroke smoothing, refresh optimization)
- Write comprehensive tests

**Acceptance Criteria:**
- [ ] AnnotationLayer view captures stylus input accurately
- [ ] Stroke rendering smooth and responsive
- [ ] Annotation toolbar functional with all tools
- [ ] Annotations persist to local database
- [ ] Annotations sync to teacher server
- [ ] Integrated with worksheet screen (Issue 4.3)
- [ ] PDF overlay positioning correct
- [ ] E-ink optimization applied
- [ ] Palm rejection working (from Issue 5.5)
- [ ] 100% test coverage
- [ ] Checkstyle compliant

**Dependencies:** Issue 4.3 (Worksheet Screen), Issue 2 (Repository Layer), Issue 3 (Network Layer), Issue 5.5 (Stylus Optimisation) from the issues.md document.

**Technical Notes:**
- Consider third-party libraries: MyScript SDK, Google ML Kit (handwriting recognition - future), or custom Canvas implementation
- Stroke data structure: List of points with pressure, timestamp, and color
- Sync strategy: Batch sync on save or periodic background sync
- E-ink consideration: Minimize partial refreshes, bundle strokes before rendering

---

## Additional Cross-Cutting Issues

### 7. [Android] Error Handling & User Feedback

- Labels: `android`, `enhancement`, `ux`

**Description:**
Implement comprehensive error handling with user-friendly messages.

**Tasks:**
- Define error types and messages
- Create error dialog/snackbar components
- Implement retry mechanisms
- Add error logging
- Write tests

---

### 8. [Android] Offline Mode Support

- Labels: `android`, `enhancement`, `networking`

**Description:**
Ensure app works in offline mode with sync when connection restored.

**Tasks:**
- Implement offline detection
- Queue actions for later sync
- Show offline indicator
- Sync when connection restored
- Write tests

---

### 9. [Android] Integration Testing Suite

- Labels: `android`, `testing`

**Description:**
Create comprehensive integration tests covering main user flows.

**Tasks:**
- Set up Espresso/UI tests
- Test material viewing flow
- Test quiz answering flow
- Test help request flow
- Test offline scenarios

---

### 10. [Android] Performance Optimisation for E-Ink

- Labels: `android`, `performance`, `e-ink`

**Description:**
Optimise app performance for e-ink display characteristics.

**Tasks:**
- Minimise screen refreshes
- Optimise animations (reduce/remove)
- Implement partial screen updates where possible
- Test on actual e-ink devices
- Profile performance

---

### [Android/Windows] Align Clients with Session Lifecycle Specification

- Labels: `android`, `windows`, `enhancement`

**Background:**
Session state transitions have been formally defined with 5 states: `RECEIVED`, `ACTIVE`, `PAUSED`, `COMPLETED`, `CANCELLED`.

**Android Client Tasks:**
- [ ] Add `RECEIVED` value to `SessionStatus.java` enum
- [ ] Update `SessionEntity` to make `StartTime` optional (null for RECEIVED)
- [ ] Update session creation logic to use `RECEIVED` as initial state
- [ ] Implement `RECEIVED` → `ACTIVE` transition on first interaction
- [ ] Implement `PAUSED` state toggle (student-initiated)
- [ ] Implement `ACTIVE/PAUSED` → `COMPLETED` on work submission
- [ ] Make `StartTime` nullable in entity/domain models
- [ ] Update mappers to handle new state

**Windows Client Tasks:**
- [ ] Update endpoint from `/session/{deviceId}` to `/distribution/{deviceId}`
- [ ] Handle new `RECEIVED` state in device status display

**Reference:**
- `docs/Session Interaction.md` §5
- `docs/Validation Rules.md` §2D
- `docs/API Contract.md` §2.5

---

## Next steps

- Review this file and tell me if you want changes to granularity, wording, or labels.
- When you're happy, I can format these into GitHub-friendly issue bodies and provide them for copy/paste or (with your permission) create them via the GitHub API.

File created: `android/issues.md`
