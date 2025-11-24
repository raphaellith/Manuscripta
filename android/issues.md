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
Implement the network layer using Retrofit for HTTP communication with the teacher's Windows application server.

**Requirements:**
- Define DTOs (Data Transfer Objects) for network communication
- Create Retrofit API service interfaces
- Implement network interceptors for logging and error handling
- Configure connection timeout and retry policies
- Handle network connectivity status

**Related Requirements:** NET1, NET2

**Acceptance Criteria:**
- [ ] All API endpoints defined in ApiService
- [ ] DTOs created for all request/response types
- [ ] Network interceptors configured
- [ ] Connection error handling implemented
- [ ] 100% unit test coverage with MockWebServer
- [ ] Checkstyle compliant
- [ ] Javadoc for all public methods

**Dependencies:** Data Model Layer (for DTOs)

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
Implement device management features including kiosk mode, battery monitoring, connection status, and remote control capabilities.

**Requirements:**
- Kiosk mode to prevent access to other apps
- Battery level monitoring and reporting
- Connection status tracking
- "Help needed" signal functionality
- Screen lock capability (teacher-controlled)
- Session end handling

**Related Requirements:** SYS1, CON2, CON5

**Acceptance Criteria:**
- [ ] Kiosk mode implemented and tested
- [ ] Battery monitoring service created
- [ ] Connection status tracking implemented
- [ ] Help signal functionality working
- [ ] Remote control handlers implemented
- [ ] 100% unit test coverage
- [ ] Integration tests passed
- [ ] Checkstyle compliant

**Dependencies:** Network Layer must be completed

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
- Create `QuestionType.java` enum with values: MULTIPLE_CHOICE, TRUE_FALSE, SHORT_ANSWER, POLL
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
- Create `DeviceStatusEntity.java` with fields: deviceId, status (ON_TASK, NEEDS_HELP, DISCONNECTED, LOCKED, IDLE), batteryLevel, currentMaterialId, studentView (for teacher live view feature), lastUpdated
- Create `DeviceStatusDao.java` interface
- Write unit tests

**Acceptance Criteria:**
- [ ] DeviceStatusEntity created
- [ ] DeviceStatusDao implemented
- [ ] Status enum defined
- [ ] 100% test coverage

---

## Sub-tasks: Repository Layer

### 2.1 [Android] Create MaterialRepository

- Labels: `android`, `repository-layer`

**Description:**
Implement repository for managing material data with network and local database synchronisation. Orchestrates attachment file downloads when materials contain file references.

**Related Requirements:** MAT1, MAT8 (Teacher), MAT15

**Tasks:**
- Create `MaterialRepository.java` interface
- Create `MaterialRepositoryImpl.java` implementation
- Implement caching strategy (network-first with fallback)
- Create `FileStorageManager.java` utility:
  - Save binary attachment data to internal app storage
  - Use predictable path pattern: `/internal/attachments/{materialId}/{attachmentId}.{ext}`
  - Delete attachments when parent material is deleted (cleanup on "end lesson")
- Orchestrate attachment downloads:
  1. Fetch MaterialDto from network
  2. Parse content for `/attachments/{id}` references using ContentParser
  3. Download each attachment via ApiService.getAttachment()
  4. Save binary data to internal storage via FileStorageManager
  5. Convert MaterialDto to MaterialEntity
  6. Insert into Room database
- Handle HTTP errors appropriately (return error Result if download fails)
- Write unit tests with mocked dependencies

**Acceptance Criteria:**
- [ ] Interface and implementation created
- [ ] Attachment download orchestration implemented
- [ ] FileStorageManager handles binary file storage
- [ ] Caching logic implemented
- [ ] Error handling with Result pattern (fails gracefully on attachment errors)
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
Implement repository for reporting device status to teacher.

**Tasks:**
- Create `DeviceStatusRepository.java` interface
- Create `DeviceStatusRepositoryImpl.java` implementation
- Implement periodic status reporting
- Handle battery level monitoring
- Write unit tests

**Acceptance Criteria:**
- [ ] Interface and implementation created
- [ ] Periodic reporting implemented
- [ ] Battery monitoring integrated
- [ ] 100% test coverage

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
Define all Retrofit API endpoints for communication with teacher server, including binary attachment downloads.

**Related Requirements:** NET1, NET2, MAT7, MAT8 (Teacher), MAT15

**Tasks:**
- Add `@GET` method for fetching materials: `getMaterials()` - Returns a list of material IDs in presentation order
- Add `@GET` method for material details: `getMaterialById(@Path id)`
- Add `@GET` method for attachments: `getAttachment(@Path id)` - Returns `ResponseBody` for binary data (PDF, images)
- Add `@POST` method for submitting response: `submitResponse(@Body)`
- Add `@POST` method for batch responses: `submitBatchResponses(@Body)`
- Add `@POST` method for device status: `reportDeviceStatus(@Body)`
- Add `@POST` method for raise hand request: `raiseHand(@Body)` (MAT7)
- Write tests with MockWebServer (including binary response mocking for attachments)

**Acceptance Criteria:**
- [ ] All endpoints defined
- [ ] Attachment endpoint returns `ResponseBody` for binary data
- [ ] Proper HTTP methods used
- [ ] Path/query parameters configured
- [ ] Raise hand endpoint included for MAT7
- [ ] 100% test coverage

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
Track connection status to teacher server and report disconnections.

**Tasks:**
- Create `ConnectionMonitorService.java`
- Implement heartbeat mechanism (periodic ping)
- Detect disconnections
- Attempt reconnection with backoff
- Update device status accordingly
- Write tests

**Acceptance Criteria:**
- [ ] Service created
- [ ] Heartbeat mechanism working
- [ ] Reconnection logic implemented
- [ ] Status updates sent
- [ ] 100% test coverage

---

### 6.4 [Android] Implement Raise Hand Feature

- Labels: `android`, `device-management`  

**Description:**
Create "Raise Hand" button and signal functionality to request teacher assistance.

**Related Requirements:** MAT7, CON12

**Tasks:**
- Add Raise Hand button to status bar
- Create `RaiseHandManager.java`
- Send help request to teacher via API (triggers visual alert on teacher dashboard)
- Show confirmation to student
- Handle teacher response/acknowledgment
- Write tests

**Acceptance Criteria:**
- [ ] Raise Hand button functional
- [ ] Request sent to teacher dashboard
- [ ] Confirmation shown to student
- [ ] Teacher acknowledgment handled
- [ ] 100% test coverage

---

### 6.5 [Android] Implement Remote Screen Lock

- Labels: `android`, `device-management`

**Description:**
Implement ability for teacher to remotely lock student screen.

**Tasks:**
- Create `RemoteControlService.java`
- Add screen lock endpoint to ApiService
- Listen for lock commands via polling/push
- Display lock overlay when commanded
- Handle unlock command
- Write tests

**Acceptance Criteria:**
- [ ] Service created
- [ ] Lock command received and handled
- [ ] Lock overlay displayed
- [ ] Unlock command working
- [ ] 100% test coverage

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
Implement device registration/identification with teacher server on first launch.

**Tasks:**
- Create device ID generation (UUID)
- Create registration UI
- Implement server discovery (mDNS or manual IP)
- Send device info to server
- Store registration status
- Write tests

**Acceptance Criteria:**
- [ ] Device ID generated
- [ ] Registration UI created
- [ ] Server discovery working
- [ ] Registration persisted
- [ ] 100% test coverage

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

**Dependencies:** Issue 1.1 (Material Entity with vocabulary field), Issue 3.1 (Material DTOs), Issue 4 (UI screens)

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

**Dependencies:** Issue 4.3 (Worksheet Screen), Issue 2 (Repository Layer), Issue 3 (Network Layer), Issue 5.5 (Stylus Optimization)

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

## Next steps

- Review this file and tell me if you want changes to granularity, wording, or labels.
- When you're happy, I can format these into GitHub-friendly issue bodies and provide them for copy/paste or (with your permission) create them via the GitHub API.

File created: `android/issues.md`
