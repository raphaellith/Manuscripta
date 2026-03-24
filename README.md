<p align="center">
  <img src="assets/QuillBG.png" alt="Manuscripta Logo" width="120" height="120">
</p>

<h1 align="center">Manuscripta</h1>

<p align="center">
  <strong>AI-Powered Classroom Orchestration for E-Ink Devices</strong>
</p>

<p align="center">
  <a href="#overview">Overview</a> |
  <a href="#key-features">Features</a> |
  <a href="#architecture">Architecture</a> |
  <a href="#getting-started">Getting Started</a> |
  <a href="#documentation">Documentation</a> |
  <a href="#licence">Licence</a>
</p>

---

## Overview

Manuscripta is an accessible educational platform that combines AI-powered e-learning with a distraction-free interface, built for e-ink classroom devices.

### The Problem

For years, schools have been integrating digital devices into classrooms, offering students AI-assisted learning paths tailored to their individual needs. Unfortunately, full-colour screens designed to capture attention aren't really what teachers are looking for—especially in Special Educational Needs environments where minimising distractions and overstimulation is paramount.

While this issue can be partially addressed by using e-ink displays like Remarkable and AIPaper, these systems fail to keep teachers in the loop.

### Our Solution

To give classrooms the best of both worlds, Manuscripta consists of two interdependent components: a portal running on the teacher's Windows laptop and an Android app running on each student's e-ink display.

The portal allows teachers to create and manage custom lesson materials, empowered by privacy-maintaining generative AI tools. When a material is deployed, it's sent to students' individual e-ink displays where they can view and interact with their classwork with minimal audiovisual stimuli.

## Key Features

### For Teachers (Windows Application)

- **Rich Text Editor** — Create and edit materials with headings, bold, italic, underline, strikethrough, bullet and numbered lists, text alignment, blockquotes, code blocks, horizontal rules, tables (with full row/column management), inline and block LaTeX equations (rendered via KaTeX), embedded images (drag-and-drop, copy-paste), embedded PDFs with page navigation, and an inline AI assistant for modifying selected content
- **AI-Powered Content Generation** — Generate worksheets and reading materials using on-device generative AI, with configurable reading age, actual age, and target duration. Source documents are semantically indexed for retrieval-augmented generation (RAG). Worksheets can include auto-generated multiple-choice and written-answer questions with mark schemes
- **Lesson Library Management** — Organise materials in a hierarchical structure of unit collections, units, lessons, and individual materials (readings, worksheets, and polls)
- **Classroom Dashboard** — Real-time device grid showing per-device status (on-task, idle, locked, disconnected), battery level, and help-request alerts with one-click acknowledgement
- **Device Pairing & Management** — UDP broadcast discovery and TCP handshake pairing for Android tablets; additional support for reMarkable and Kindle e-readers with authentication flows. Rename or unpair devices from the dashboard
- **Material Deployment** — Deploy materials to individual or multiple devices simultaneously, with deployment progress indicators and acknowledgement tracking. Differentiated deployment: send different materials to different device groups in one action
- **Remote Device Control** — Lock/unlock screens, refresh device configuration, and push per-device settings (text size, feedback style, mascot selection) from a central interface
- **Differentiated Instruction** — Deploy different materials to specific groups of students simultaneously. Reading age and actual age parameters shape AI content generation, so teachers can produce multiple difficulty variants of the same topic at edit time
- **Responses & Feedback** — Collect student responses to worksheets and polls with class-level and per-device views. Multiple-choice and exact-match written answers are auto-marked on submission. For open-ended written answers, teachers create mark schemes (manually or via AI generation) that drive AI-powered auto-marking. AI-generated feedback enters a queue, is produced in the background, and is held as provisional until the teacher reviews, edits, and explicitly approves it for delivery. Failed deliveries can be retried from the dashboard

### For Students (Android E-Ink Application)

- **Distraction-Free Interface** — Monochromatic display optimised for e-ink tablets with minimal visual stimulation
- **Interactive Materials** — Complete worksheets and polls with multiple-choice and written-answer questions. Feedback style is teacher-configured: immediate mode shows correctness on submission; neutral mode shows only a submission confirmation
- **AI Scaffolding** — Simplify or summarise displayed text via a mascot assistant. AI scaffolding and summarisation are independently togglable by the teacher per device
- **Help Request** — Raise a hand via a single button press (TCP signal to teacher dashboard) with a 3-second cooldown to prevent spam. Teacher sees a pulsing alert and can acknowledge individually or in bulk
- **Dynamic Text Scaling** — Global text size is teacher-configurable per device (range 5-50), scaling all rendered material content proportionally
- **Teacher Feedback** — View marks and written feedback from the teacher in an overlay panel. Feedback appears when the teacher dispatches it and is presented alongside the relevant material
- **Screen Lock** — Teacher can remotely lock and unlock the device screen; a lock overlay prevents interaction until released

## Architecture

Manuscripta employs a client-server architecture with the Windows teacher application acting as the server and Android student devices as clients. All communication occurs over the local area network, ensuring data privacy and eliminating dependence on cloud services.

```
                    ┌─────────────────────────────────────────┐
                    │         Teacher's Windows PC            │
                    │  ┌─────────────────────────────────┐    │
                    │  │ GenAI (Ollama: Qwen3 / Granite) │    │
                    │  └─────────────────────────────────┘    │
                    │  ┌─────────────────────────────────┐    │
                    │  │   Lesson Library & Dashboard    │    │
                    │  └─────────────────────────────────┘    │
                    │  ┌─────────────────────────────────┐    │
                    │  │   HTTP/TCP/UDP Server           │    │
                    │  └─────────────────────────────────┘    │
                    └───────────────┬─────────────────────────┘
                                    │
                         Local Area Network (LAN)
                                    │
        ┌───────────────────────────┼───────────────────────────┐
        │                           │                           │
        ▼                           ▼                           ▼
┌───────────────┐           ┌───────────────┐           ┌───────────────┐
│   E-Ink       │           │   E-Ink       │           │   E-Ink       │
│   Tablet 1    │           │   Tablet 2    │    ...    │   Tablet 30   │
│   (Android)   │           │   (Android)   │           │   (Android)   │
└───────────────┘           └───────────────┘           └───────────────┘
```

### Communication Protocols

The system utilises a hybrid multi-channel networking approach:

| Protocol | Purpose |
|----------|---------|
| **HTTP (REST)** | Material distribution, student response submission, feedback retrieval, device configuration, and file attachments (port 5911) |
| **TCP** | Persistent connection for low-latency control signals (lock/unlock, distribute, feedback dispatch), heartbeat status updates, pairing handshake, and help-request signalling (port 5912) |
| **UDP** | Server broadcast for device discovery during pairing (port 5913) |

### Heartbeat Mechanism

Each paired Android device sends a TCP heartbeat every 3 seconds containing its device ID, current status (`ON_TASK`, `IDLE`, `LOCKED`, or `DISCONNECTED`), battery level (0-100), the currently viewed material ID, and a timestamp. The server marks a device as disconnected if no heartbeat is received for 10 seconds. Three consecutive heartbeat failures on the client trigger a disconnection notification; 60 seconds of connection loss triggers an automatic unpair.

### Technology Stack

**Teacher Application (Windows)**
- Platform: Windows (.NET 10.0, ASP.NET Core)
- Frontend: Electron with React, TypeScript, and TailwindCSS
- Editor: TipTap rich text editor with KaTeX, react-pdf, Turndown, and Marked
- AI Runtime: Ollama (local inference server)
  - Primary model: Qwen3 8B (material generation, content modification, feedback generation)
  - Fallback model: IBM Granite 4.0 (used when primary model exhausts memory)
  - Embeddings: Nomic Embed Text (768-dimensional vectors for RAG)
- Vector Database: ChromaDB (local, for semantic retrieval of source documents)
- Database: SQLite via Entity Framework Core
- PDF Generation: QuestPDF with PdfSharpCore
- Email: MailKit (for external device authentication)
- Real-Time Communication: SignalR (frontend to backend)

**Student Application (Android)**
- Platform: Android (Java/Kotlin build scripts)
- Architecture: MVVM with Clean Architecture
- Local Storage: Room Database
- Networking: Retrofit + OkHttp (HTTP), native Java sockets (TCP/UDP)
- Markdown Rendering: Markwon (with tables, HTML, inline-parser, and LaTeX extensions)
- Dependency Injection: Hilt
- Testing: JUnit 4, Mockito, Robolectric, Espresso
- Code Quality: Checkstyle, JaCoCo (90% minimum coverage)

## Getting Started

### Prerequisites

**Teacher Application**
- Windows PC with sufficient resources to run Ollama and Qwen3 8B locally (approximately 8 GB RAM for the model)
- .NET 10.0 SDK
- Node.js (for Electron frontend)

**Student Application**
- Android-based e-ink tablet (e.g., Boox, AiPaper)
- All devices must be connected to the same local network

### Project Structure

```
Manuscripta/
├── android/                          # Android student application
│   ├── app/
│   │   ├── src/main/java/com/manuscripta/student/
│   │   │   ├── data/                 # Room DB, entities, repositories
│   │   │   ├── di/                   # Hilt dependency injection modules
│   │   │   ├── domain/               # Domain models and mappers
│   │   │   ├── network/              # HTTP (Retrofit), TCP, and UDP clients
│   │   │   │   ├── dto/              # Network data transfer objects
│   │   │   │   ├── interceptor/      # OkHttp interceptors
│   │   │   │   ├── tcp/              # TCP socket, heartbeat, pairing, hand-raise
│   │   │   │   └── udp/              # UDP discovery listener
│   │   │   ├── ui/                   # Activities, Fragments, ViewModels
│   │   │   │   ├── main/             # Main activity and navigation
│   │   │   │   ├── pairing/          # Device pairing flow
│   │   │   │   ├── reading/          # Reading material viewer
│   │   │   │   ├── worksheet/        # Worksheet interaction
│   │   │   │   ├── feedback/         # Feedback display
│   │   │   │   ├── renderer/         # Markdown and content rendering
│   │   │   │   └── view/             # Custom views (mascot, AI panel, feedback panel)
│   │   │   └── utils/                # Utilities (TTS, constants)
│   │   ├── src/test/                 # Unit tests (JUnit, Mockito, Robolectric)
│   │   └── src/androidTest/          # Instrumented tests (Espresso)
│   ├── config/                       # Checkstyle and JaCoCo configuration
│   └── gradle/                       # Gradle wrapper and version catalog
├── windows/                          # Windows teacher application
│   └── ManuscriptaTeacherApp/
│       ├── Main/                     # .NET backend (ASP.NET Core Web API)
│       │   ├── Controllers/          # REST API endpoints
│       │   ├── Models/               # Entities, DTOs, enums, mappings
│       │   ├── Services/
│       │   │   ├── GenAI/            # Ollama client, generation, feedback, embeddings
│       │   │   ├── Hubs/             # SignalR hub (frontend communication)
│       │   │   ├── Network/          # TCP and UDP server services
│       │   │   ├── Repositories/     # Data access layer
│       │   │   └── RuntimeDependencies/  # Ollama, model, and ChromaDB installers
│       │   └── Migrations/           # EF Core database migrations
│       ├── MainTests/                # xUnit test project
│       └── UI/                       # Electron + React frontend
│           └── src/renderer/
│               ├── components/       # React components (editor, dashboard, responses)
│               └── utils/            # Markdown conversion, sanitisation
├── docs/                             # Project documentation
└── assets/                           # Logos and images
```

### Building the Applications

**Android Application**

The Android app requires **JDK 17** — it does not build on JDK 21+.

1. Open the `android/` directory in Android Studio (**File > Open**, then select the `android/` folder).
2. Set the Gradle JDK to 17: **Android Studio > Settings > Build, Execution, Deployment > Build Tools > Gradle**, then select a JDK 17 installation from the **Gradle JDK** dropdown.
3. Select the **release** build variant from the **Build Variants** panel (accessible from the left sidebar or via **View > Tool Windows > Build Variants**).
4. Connect a physical Android device via USB (with USB debugging enabled) or start an emulator from the **Device Manager**.
5. Click **Run ▶** (or **Shift+F10**) to build and deploy the release APK to the connected device/emulator.

**Windows Application**

Refer to the Windows team documentation in the `windows/` directory for build instructions.

## Documentation

Comprehensive documentation is available in the `/docs` directory:

| Document | Description |
|----------|-------------|
| [Project Specification](docs/Project%20Specification.md) | Complete requirements and system overview |
| [API Contract](docs/API%20Contract.md) | HTTP, TCP, and UDP protocol specification |
| [Android System Design](docs/Android%20System%20Design.md) | Mobile application architecture |
| [Validation Rules](docs/Validation%20Rules.md) | Data model constraints and validation |
| [Pairing Process](docs/Pairing%20Process.md) | Device discovery and connection |
| [Session Interaction](docs/Session%20Interaction.md) | Heartbeat, control commands, and runtime communication |
| [Material Encoding](docs/Material%20Encoding.md) | Markdown content format specification |
| [Integration Test Contract](docs/Integration%20Test%20Contract.md) | Integration test specifications |
| [Style Guide](docs/STYLE_GUIDE.md) | Code style standards |
| [GitHub Conventions](docs/Github%20Conventions.md) | Development workflow guidelines |
| [Testing Guide](docs/testing/README.md) | Testing framework documentation |

## Team

Manuscripta is developed by:

- **Raphael Li** — Windows Application Development
- **Nemo Shu** — Windows Application Development
- **Will Stephen** — Android Application Development
- **Priya Bargota** — Android Application Development

### Supervision and Partners

This project is supervised by **Professor Dean Mohamedally** at University College London, with industry partners including:

- **Qualcomm** — Industry partner and technology consultation
- **IBM Granite+Interactions Group** — Generative AI model integration
- **National Autistic Society** — Accessibility requirements consultation

## Privacy and Ethics

Manuscripta is designed with privacy at its core:

- **No Cloud Dependency** — All data remains on the local network
- **Alias-Based Identity** — Students provide a temporary alias when joining each class session. A classroom owns a shared set of tablets; identity persists only for the duration of a lesson based on the alias, with no permanent student accounts or personal data stored
- **Minimal Data Collection** — No student accounts, passwords, or personally identifiable information are required. Aliases are the only identifier and carry no link to real-world identity
- **Low Energy Footprint** — E-ink displays and localised AI processing reduce environmental impact

## Licence

This project is licensed under the MIT Licence. See the [LICENSE](LICENSE) file for details.

---

<p align="center">
  <sub>Manuscripta — Bringing focus back to the classroom</sub>
</p>
