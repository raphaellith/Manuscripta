# Manuscripta Android Client - System Design Audit

This document provides a comprehensive system design audit for the Android student client of the Manuscripta platform, based on the complete implementation requirements defined in `issues.md` and related documentation.

---

## Executive Summary

The Manuscripta Android client is a student-facing application designed for e-ink tablet devices. It operates as the **client** in a client-server architecture, communicating with a Windows-based teacher application over LAN using a hybrid multi-channel networking approach (HTTP, TCP, UDP).

**Key Design Principles:**
- **Clean Architecture** with clear separation between Data, Domain, and Presentation layers
- **Offline-first** with local Room database and sync queues
- **E-ink optimised** with monochrome theme and minimal refreshes
- **Heartbeat-triggered content delivery** since Windows server cannot initiate HTTP requests

---



## 1. Data Model Layer (Entity-Relationship Diagram)

```mermaid
erDiagram
    MATERIAL {
        string id PK "UUID (assigned by Windows)"
        enum materialType "READING|QUIZ|WORKSHEET|POLL"
        string title "max 500 chars"
        string content "HTML or text"
        string metadata "optional JSON"
        json vocabularyTerms "optional array"
        long timestamp "unix timestamp"
    }
    
    QUESTION {
        string id PK "UUID (assigned by Windows)"
        string materialId FK
        enum questionType "MULTIPLE_CHOICE|TRUE_FALSE|WRITTEN_ANSWER"
        string questionText
        json options "list for MULTIPLE_CHOICE"
        string correctAnswer "optional"
    }
    
    RESPONSE {
        string id PK "UUID (assigned by Android)"
        string questionId FK
        string deviceId FK
        string answer
        boolean isCorrect "optional"
        long timestamp
        boolean synced "sync status"
    }
    
    SESSION {
        string id PK "UUID (assigned by Android)"
        string materialId FK
        long startTime
        long endTime "optional"
        enum sessionStatus "RECEIVED|ACTIVE|PAUSED|COMPLETED|CANCELLED"
        string deviceId
    }
    
    DEVICE_STATUS {
        string deviceId PK
        enum status "ON_TASK|IDLE|LOCKED|DISCONNECTED"
        int batteryLevel
        string currentMaterialId FK
        string studentView "placeholder"
        long lastUpdated
    }
    
    ANNOTATION {
        string id PK
        string worksheetId FK
        json strokes "stroke data"
        string color
        long timestamp
        boolean synced
    }
    
    MATERIAL ||--o{ QUESTION : "contains"
    MATERIAL ||--o{ SESSION : "used in"
    QUESTION ||--o{ RESPONSE : "answered by"
    MATERIAL ||--o{ ANNOTATION : "annotated on"
    DEVICE_STATUS }o--|| MATERIAL : "viewing"
```

---



## 2. Network Communication Sequence Diagrams

### 2.1 Pairing Process

```mermaid
sequenceDiagram
    participant A as Android Client
    participant UDP as UDP Layer
    participant TCP as TCP Layer
    participant HTTP as HTTP Layer
    participant W as Windows Server
    
    Note over A,W: Phase 1: Discovery
    W->>UDP: Broadcast DISCOVERY (0x00)<br/>[IP, HTTP Port, TCP Port]
    UDP->>A: Discovery Message Received
    A->>A: Extract server info
    
    Note over A,W: Phase 2a: TCP Pairing
    A->>TCP: Initiate connection
    TCP->>W: TCP Connect to :5912
    A->>W: PAIRING_REQUEST (0x20)<br/>[Device UUID]
    W->>A: PAIRING_ACK (0x21)
    
    Note over A,W: Phase 2b: HTTP Registration
    A->>W: POST /pair<br/>{"deviceId": "uuid"}
    W->>A: 201 Created
    
    Note over A,W: Pairing Complete
    A->>A: Store pairing status<br/>PAIRED state
```

### 2.2 Material Distribution (Heartbeat-Triggered Fetch)

```mermaid
sequenceDiagram
    participant A as Android Client
    participant MR as MaterialRepository
    participant TCP as TcpSocketManager
    participant HTTP as ApiService
    participant W as Windows Server
    
    Note over A,W: Heartbeat Loop (periodic)
    loop Every N seconds
        A->>TCP: Send STATUS_UPDATE (0x10)<br/>[deviceId, status, battery, ...]
        TCP->>W: TCP Message
        
        alt New materials available
            W->>TCP: DISTRIBUTE_MATERIAL (0x05)
            TCP->>MR: onFetchMaterialsSignal()
            MR->>HTTP: GET /distribution/{deviceId}
            HTTP->>W: HTTP Request
            W->>HTTP: 200 OK [Distribution Bundle]
            HTTP->>MR: Return materials & questions
            MR->>TCP: Send DISTRIBUTE_ACK (0x12)
            TCP->>W: DISTRIBUTE_ACK
            
            loop For each material in bundle
                MR->>MR: Process Material JSON
                
                alt Content has attachments
                    loop For each attachment reference
                        MR->>HTTP: GET /attachments/{id}
                        HTTP->>W: HTTP Request
                        W->>HTTP: 200 OK [Binary data]
                        HTTP->>MR: Return Binary data
                        MR->>MR: Save to FileStorage
                    end
                end
            end
            
            MR->>MR: Save to Room DB
        else No new materials
            Note over W: No response needed
        end
    end
```

### 2.3 Student Response Submission

```mermaid
sequenceDiagram
    participant UI as QuizActivity
    participant VM as QuizViewModel
    participant RR as ResponseRepository
    participant HTTP as ApiService
    participant W as Windows Server
    
    UI->>VM: Submit answer
    VM->>VM: Create Response (generate UUID)
    VM->>RR: saveResponse(response)
    
    RR->>RR: Insert to Room DB<br/>(synced = false)
    
    alt Online
        RR->>HTTP: POST /responses<br/>{id, questionId, answer, ...}
        HTTP->>W: HTTP Request
        W->>HTTP: 201 Created
        HTTP->>RR: Return success
        RR->>RR: Update synced = true
        RR->>VM: Result.Success
    else Offline
        RR->>RR: Add to sync queue
        RR->>VM: Result.Success (queued)
        
        Note over RR,W: Later, when connection restored
        RR->>HTTP: POST /responses/batch
        HTTP->>W: HTTP Request
        W->>HTTP: 201 Created
        HTTP->>RR: Return success
        RR->>RR: Update all synced = true
    end
    
    VM->>UI: Update UI State
```

### 2.4 Teacher Control Commands

```mermaid
sequenceDiagram
    participant W as Windows Server
    participant TCP as TcpSocketManager
    participant RCS as RemoteControlService
    participant KM as KioskManager
    participant UI as Current Activity
    
    alt Lock Screen
        W->>TCP: LOCK_SCREEN (0x01)
        TCP->>RCS: onMessage(LOCK_SCREEN)
        RCS->>KM: lockScreen()
        KM->>UI: Show lock overlay
    else Unlock Screen
        W->>TCP: UNLOCK_SCREEN (0x02)
        TCP->>RCS: onMessage(UNLOCK_SCREEN)
        RCS->>KM: unlockScreen()
        KM->>UI: Remove lock overlay
    else Refresh Config
        W->>TCP: REFRESH_CONFIG (0x03)
        TCP->>RCS: onMessage(REFRESH_CONFIG)
        RCS->>UI: Trigger HTTP config fetch
    end
```

### 2.5 Raise Hand Flow

```mermaid
sequenceDiagram
    participant UI as SessionStatusView
    participant RHM as RaiseHandManager
    participant TCP as TcpSocketManager
    participant W as Windows Server
    
    UI->>RHM: raiseHand()
    RHM->>TCP: sendMessage(HAND_RAISED, deviceId)
    TCP->>W: HAND_RAISED (0x11)<br/>[Device ID UTF-8]
    
    Note over W: Teacher sees alert<br/>on dashboard (CON12)
    
    W->>TCP: HAND_ACK (0x06)<br/>[Device ID UTF-8]
    TCP->>RHM: onHandAcknowledged(deviceId)
    RHM->>UI: Show confirmation
    UI->>UI: Display "Help requested"
```

---

## 3. State Machine Diagrams

### 3.1 Pairing State Machine

```mermaid
stateDiagram-v2
    [*] --> IDLE
    
    IDLE --> DISCOVERING: startPairing()
    
    DISCOVERING --> TCP_PAIRING: UDP discovery received
    DISCOVERING --> FAILED: timeout / error
    
    TCP_PAIRING --> HTTP_PAIRING: PAIRING_ACK received
    TCP_PAIRING --> FAILED: timeout / rejected
    
    HTTP_PAIRING --> PAIRED: 201 Created received
    HTTP_PAIRING --> FAILED: error / 409 Conflict
    
    FAILED --> DISCOVERING: retry()
    FAILED --> IDLE: cancel()
    
    PAIRED --> IDLE: unpair() / disconnect
    PAIRED --> [*]
    
    note right of FAILED
        Per Pairing Process §1(4):
        If any phase fails, restart entire process
    end note
```

### 3.2 Session State Machine

```mermaid
stateDiagram-v2
    [*] --> RECEIVED: Session.create()
    
    RECEIVED --> ACTIVE: activate()
    RECEIVED --> CANCELLED: cancel() / teacher_end
    
    ACTIVE --> PAUSED: pause() / switch_material
    ACTIVE --> COMPLETED: complete()
    ACTIVE --> CANCELLED: cancel() / teacher_end
    
    PAUSED --> ACTIVE: resume()
    PAUSED --> COMPLETED: complete()
    PAUSED --> CANCELLED: cancel()
    
    COMPLETED --> [*]
    CANCELLED --> [*]
    
    note right of RECEIVED
        startTime = 0 (not set)
    end note
    
    note right of ACTIVE
        startTime set on activation
    end note
    
    note right of PAUSED
        endTime set on pause
    end note
```

### 3.3 Device Status State Machine

```mermaid
stateDiagram-v2
    [*] --> DISCONNECTED
    
    DISCONNECTED --> ON_TASK: pairing complete
    
    ON_TASK --> IDLE: inactivity timeout
    ON_TASK --> LOCKED: LOCK_SCREEN received
    ON_TASK --> DISCONNECTED: connection lost
    
    IDLE --> ON_TASK: user activity
    IDLE --> LOCKED: LOCK_SCREEN received
    IDLE --> DISCONNECTED: connection lost
    
    LOCKED --> ON_TASK: UNLOCK_SCREEN received
    LOCKED --> DISCONNECTED: connection lost
    
    DISCONNECTED --> ON_TASK: reconnected
```

---

## 4. Class Diagram (Core Components)

```mermaid
classDiagram
    direction TB
    
    %% Domain Models
    class Material {
        +String id
        +MaterialType type
        +String title
        +String content
        +String metadata
        +List~VocabularyTerm~ vocabularyTerms
        +long timestamp
    }
    
    class Question {
        +String id
        +String materialId
        +QuestionType questionType
        +String questionText
        +List~String~ options
        +String correctAnswer
    }
    
    class Response {
        +String id
        +String questionId
        +String deviceId
        +String answer
        +Boolean isCorrect
        +long timestamp
        +static create(questionId, answer) Response
    }
    
    class Session {
        +String id
        +String materialId
        +long startTime
        +long endTime
        +SessionStatus status
        +String deviceId
        +static create(materialId, deviceId) Session
    }
    
    %% Enums
    class MaterialType {
        <<enumeration>>
        READING
        QUIZ
        WORKSHEET
        POLL
    }
    
    class QuestionType {
        <<enumeration>>
        MULTIPLE_CHOICE
        TRUE_FALSE
        WRITTEN_ANSWER
    }
    
    class SessionStatus {
        <<enumeration>>
        ACTIVE
        PAUSED
        COMPLETED
        CANCELLED
    }
    
    class DeviceStatus {
        <<enumeration>>
        ON_TASK
        IDLE
        LOCKED
        DISCONNECTED
    }
    
    %% Repositories
    class MaterialRepository {
        <<interface>>
        +getMaterials() Flow~List~Material~~
        +getMaterialById(id) Flow~Material~
        +syncMaterials() Result~Unit~
        +onFetchMaterialsSignal()
    }
    
    class ResponseRepository {
        <<interface>>
        +submitResponse(response) Result~Unit~
        +submitBatch(responses) Result~Unit~
        +getUnsyncedResponses() List~Response~
    }
    
    class SessionRepository {
        <<interface>>
        +startSession(materialId, deviceId) Session
        +getActiveSession() Session?
        +updateSession(session)
        +endSession(sessionId)
    }
    
    %% Network
    class TcpSocketManager {
        +connect(ip, port)
        +disconnect()
        +sendMessage(opcode, operand)
        +addMessageListener(listener)
        +removeMessageListener(listener)
        +isConnected() boolean
    }
    
    class UdpDiscoveryManager {
        +startDiscovery()
        +stopDiscovery()
        +getDiscoveryState() StateFlow~DiscoveryState~
    }
    
    class PairingManager {
        +startPairing()
        +cancelPairing()
        +getPairingState() StateFlow~PairingState~
        +getDeviceId() String
        +isPaired() boolean
    }
    
    class ApiService {
        <<interface>>
        +getMaterials() Call~List~String~~
        +getMaterialById(id) Call~MaterialDto~
        +getAttachment(id) Call~ResponseBody~
        +getConfig() Call~ConfigDto~
        +submitResponse(dto) Call~Unit~
        +submitBatchResponses(dto) Call~Unit~
        +registerDevice(dto) Call~Unit~
    }
    
    %% Relationships
    Material --> MaterialType
    Question --> QuestionType
    Session --> SessionStatus
    
    Material "1" --> "*" Question
    Question "1" --> "*" Response
    Material "1" --> "*" Session
    
    MaterialRepository --> ApiService
    MaterialRepository --> TcpSocketManager
    ResponseRepository --> ApiService
    
    PairingManager --> UdpDiscoveryManager
    PairingManager --> TcpSocketManager
    PairingManager --> ApiService
```

---

## 5. Dependency Injection Structure

```mermaid
graph TB
    subgraph "Hilt Modules"
        subgraph "AppModule"
            APP[Application Context]
        end
        
        subgraph "DatabaseModule"
            DB[(ManuscriptaDatabase)]
            MD[MaterialDao]
            QD[QuestionDao]
            RD[ResponseDao]
            SD[SessionDao]
            DSD[DeviceStatusDao]
        end
        
        subgraph "NetworkModule"
            OKHTTP[OkHttpClient]
            RETROFIT[Retrofit]
            API[ApiService]
        end
        
        subgraph "SocketModule"
            TSM[TcpSocketManager]
            UDM[UdpDiscoveryManager]
        end
        
        subgraph "RepositoryModule"
            MR[MaterialRepository]
            RR[ResponseRepository]
            SR[SessionRepository]
            DSR[DeviceStatusRepository]
            FSM[FileStorageManager]
        end
        
        subgraph "ServiceModule"
            PM[PairingManager]
            KM[KioskManager]
            TTSM[TextToSpeechManager]
            CTS[ContentTransformationService]
        end
    end
    
    DB --> MD
    DB --> QD
    DB --> RD
    DB --> SD
    DB --> DSD
    
    OKHTTP --> RETROFIT
    RETROFIT --> API
    
    MD --> MR
    API --> MR
    TSM --> MR
    FSM --> MR
    
    RD --> RR
    API --> RR
    
    SD --> SR
    
    DSD --> DSR
    TSM --> DSR
    
    UDM --> PM
    TSM --> PM
    API --> PM
```

---

## 6. Binary Protocol Reference

### 6.1 Opcode Registry

| Range | Purpose | Direction |
|-------|---------|-----------|
| `0x00` | UDP Discovery | Server → Client |
| `0x01` - `0x0F` | Control Commands | Server → Client (TCP) |
| `0x10` - `0x1F` | Status Updates | Client → Server (TCP) |
| `0x20` - `0x2F` | Pairing | Bidirectional (TCP) |

### 6.2 Message Definitions

```mermaid
graph LR
    subgraph "UDP Messages"
        DISC[0x00 DISCOVERY<br/>IP + HTTP Port + TCP Port<br/>9 bytes total]
    end
    
    subgraph "Server → Client (TCP)"
        LOCK[0x01 LOCK_SCREEN<br/>No operand]
        UNLOCK[0x02 UNLOCK_SCREEN<br/>No operand]
        REFRESH[0x03 REFRESH_CONFIG<br/>No operand]
        UNPAIR[0x04 UNPAIR<br/>No operand]
        DISTMAT[0x05 DISTRIBUTE_MATERIAL<br/>No operand]
        HANDACK[0x06 HAND_ACK<br/>Device ID UTF-8]
    end
    
    subgraph "Client → Server (TCP)"
        STATUS[0x10 STATUS_UPDATE<br/>JSON payload]
        HAND[0x11 HAND_RAISED<br/>Device ID UTF-8]
        DISTACK[0x12 DISTRIBUTE_ACK<br/>Device ID UTF-8]
    end
    
    subgraph "Pairing (TCP)"
        PREQ[0x20 PAIRING_REQUEST<br/>Device ID UTF-8]
        PACK[0x21 PAIRING_ACK<br/>No operand]
    end
```

---

## 7. Key Design Decisions

### 7.1 Entity ID Generation Policy

| Entity | ID Generator | Rationale |
|--------|--------------|-----------|
| Material | Windows Server | Content created by teacher |
| Question | Windows Server | Part of material content |
| Response | Android Client | Student interaction |
| Session | Android Client | Device-local session |
| DeviceId | Android Client | Device identification |

> [!IMPORTANT]
> Per Validation Rules §3: IDs must be UUIDs generated by the creating client and treated as immutable by the receiving client.

### 7.2 Heartbeat-Triggered Fetch Pattern

The Windows server cannot initiate HTTP requests to Android clients. Material distribution uses:

1. **Android** sends periodic `STATUS_UPDATE` (0x10) via TCP
2. **Windows** responds with `DISTRIBUTE_MATERIAL` (0x05) if content pending
3. **Android** initiates HTTP `GET /distribution/{deviceId}` to download

### 7.3 Clean Architecture Entity Separation

- **Entities** (`*Entity.java`): Room annotations, persistence only
- **Domain Models** (`*.java`): Business logic, factory methods for ID/timestamp generation
- **DTOs** (`*Dto.java`): Network serialization, JSON annotations

---

## 8. Screen Flow Diagram

```mermaid
flowchart TD
    START([App Launch]) --> CHECK{Paired?}
    
    CHECK -->|No| PAIR[Pairing Screen]
    PAIR --> DISC[Discovering...]
    DISC --> CONNECT[Connecting...]
    CONNECT --> PAIRED{Success?}
    PAIRED -->|Yes| HOME
    PAIRED -->|No| RETRY[Show Error]
    RETRY --> DISC
    
    CHECK -->|Yes| HOME[Material List]
    
    HOME --> MAT{Material Type?}
    
    MAT -->|READING| READ[Reading Screen]
    MAT -->|QUIZ| QUIZ[Quiz Screen]
    MAT -->|WORKSHEET| WORK[Worksheet Screen]
    MAT -->|POLL| POLL[Poll Screen]
    
    QUIZ --> ANS[Answer Question]
    ANS --> FEED{Correct?}
    FEED -->|Yes| NEXT[Next Question]
    FEED -->|No| RETRY2[Try Again]
    RETRY2 --> ANS
    NEXT --> ANS
    
    WORK --> ANN[Annotate with Stylus]
    ANN --> SAVE[Save & Sync]
    
    POLL --> VOTE[Submit Vote]
    VOTE --> CONF[Confirmation]
    
    READ --> TRANS[Simplify/Expand/Summarize]
    TRANS --> READ
    
    subgraph "Overlay States"
        LOCK_OV[Lock Overlay]
        HELP_OV[Help Confirmation]
    end
    
    HOME -.->|LOCK_SCREEN| LOCK_OV
    LOCK_OV -.->|UNLOCK_SCREEN| HOME
    HOME -.->|Raise Hand| HELP_OV
```

---

## 9. Requirements Traceability Matrix

| Requirement | Component | Issue(s) |
|-------------|-----------|----------|
| **MAT1** (Display materials) | MaterialListActivity, MaterialRepository | 1.1, 2.1, 4.1 |
| **MAT2B** (Feedback display) | FeedbackDialog, QuizViewModel | 4.2, 4.6 |
| **MAT3** (Try Again) | QuizActivity | 4.2 |
| **MAT4** (Simplify/Expand/Summarize) | ContentTransformationService | 5.2, 5.3, 5.4 |
| **MAT5** (AI assistance) | Future/Out of scope | - |
| **MAT6** (Key Vocabulary) | VocabularyDisplayView | 11.1 |
| **MAT7** (Raise Hand) | RaiseHandManager, SessionStatusView | 6.4, 4.5 |
| **MAT8** (Handwriting) | AnnotationLayerView | 11.2, 4.3 |
| **ACC1** (Stylus input) | Stylus optimization | 5.5 |
| **ACC3** (Text-to-Speech) | TextToSpeechManager | 5.1 |
| **ACC4** (Monochrome) | themes.xml | 4.8 |
| **NET1** (LAN material distribution) | MaterialRepository, TcpSocketManager | 2.1, 6.8 |
| **NET2** (Response submission) | ResponseRepository, ApiService | 2.2, 3.5 |
| **CON2A** (Device status) | DeviceStatusRepository | 2.4, 6.3 |
| **CON6** (Lock screen) | RemoteControlService, KioskManager | 6.5, 6.1 |
| **CON12** (Help alert) | RaiseHandManager | 6.4 |
| **SYS1** (E-ink compatibility) | E-ink optimizations | 10 |

---

## 10. Technology Stack Summary

| Layer | Technology | Purpose |
|-------|------------|---------|
| **Language** | Java | Primary development language |
| **Database** | Room | Local SQLite persistence |
| **Networking (HTTP)** | Retrofit + OkHttp | REST API communication |
| **Networking (TCP)** | Native Java Sockets | Real-time control signals |
| **Networking (UDP)** | DatagramSocket | Server discovery |
| **DI** | Hilt | Dependency injection |
| **UI** | ViewBinding + XML | View layer |
| **Architecture** | MVVM + Clean Architecture | Separation of concerns |
| **Async** | LiveData / StateFlow | Reactive state management |

---

*Document generated based on: `Project Specification.md`, `API Contract.md`, `Validation Rules.md`, `Pairing Process.md`, and `android/issues.md`*
