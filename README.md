<p align="center">
  <img src="assets/quill-logo.png" alt="Manuscripta Logo" width="120" height="120">
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

Manuscripta is a classroom orchestration system that bridges the gap between cutting-edge generative AI and distraction-free learning. The platform enables teachers to harness on-device AI to create and distribute educational materials directly to students' e-ink tablets, combining the benefits of digital learning with the focus-enhancing properties of monochromatic displays.

In an era where full-colour tablets often prove more distracting than helpful in educational settings, Manuscripta offers a thoughtful alternative. By decoupling content creation from content consumption, the system empowers teachers with powerful AI tools whilst providing students with a calm, focused reading environment.

### The Challenge

Head teachers and educators have long grappled with a difficult trade-off: whilst modern tablets enable personalised, AI-enhanced learning experiences, their vibrant displays and myriad applications frequently distract students from the task at hand. Existing solutions—such as classroom management software on standard tablets—address only part of the problem, leaving the fundamental issue of visual distraction unresolved.

### Our Solution

Manuscripta takes a different approach. Teachers use AI-powered Windows laptops to generate and customise lesson materials, which are then seamlessly distributed to students' e-ink devices over the local network. Students interact with content through simple, monochromatic interfaces designed to minimise distraction and maximise engagement.

## Key Features

### For Teachers (Windows Application)

- **AI-Powered Content Generation** — Create quizzes, worksheets, reading materials, and polls using on-device generative AI models (IBM Granite via OpenVINO/Qualcomm AI Stack)
- **Lesson Library Management** — Organise materials in a hierarchical structure of units, lessons, and individual materials
- **Real-Time Dashboard** — Monitor student progress, device status, and engagement through a comprehensive classroom overview
- **Differentiated Instruction** — Deploy different materials to specific groups of students simultaneously
- **Adaptive Complexity** — Adjust reading levels and text complexity to suit different age groups and abilities
- **Response Aggregation** — Collect and analyse anonymised student responses to quizzes and polls
- **Remote Device Control** — Lock screens, refresh configurations, and manage individual tablets from a central interface

### For Students (Android E-Ink Application)

- **Distraction-Free Interface** — Monochromatic display optimised for e-ink tablets with minimal visual stimulation
- **Interactive Materials** — Complete quizzes, worksheets, and polls with immediate or teacher-configured feedback
- **AI Learning Support** — Simplify, expand, or summarise text with built-in AI assistance
- **Handwriting Support** — Annotate worksheets and documents using a stylus
- **Help Request System** — Discreetly request teacher assistance without disrupting the class
- **Text-to-Speech** — Optional audio support for accessibility
- **Key Vocabulary Display** — Highlighted terms and definitions for each lesson

## Architecture

Manuscripta employs a client-server architecture with the Windows teacher application acting as the server and Android student devices as clients. All communication occurs over the local area network, ensuring data privacy and eliminating dependence on cloud services.

```
                    ┌─────────────────────────────────────────┐
                    │         Teacher's Windows PC            │
                    │  ┌─────────────────────────────────┐    │
                    │  │   GenAI Models (Granite/OpenVINO)│    │
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
| **HTTP (REST)** | Transmission of lesson materials and student responses |
| **TCP** | Low-latency control signals, status updates, and real-time commands |
| **UDP** | Device discovery and initial pairing |

### Technology Stack

**Teacher Application (Windows)**
- Platform: Windows (.NET)
- AI Integration: IBM Granite models via OpenVINO and Qualcomm AI Stack
- Distribution: Microsoft Store

**Student Application (Android)**
- Platform: Android (Java)
- Architecture: MVVM with Clean Architecture
- Local Storage: Room Database
- Networking: Retrofit (HTTP), native Java sockets (TCP/UDP)
- Dependency Injection: Hilt

## Getting Started

### Prerequisites

**Teacher Application**
- Windows PC with Intel or Qualcomm AI chipset
- Sufficient computational resources for local AI model execution

**Student Application**
- Android-based e-ink tablet (e.g., Boox, AiPaper)
- All devices must be connected to the same local network

### Project Structure

```
Manuscripta/
├── android/                 # Android student application
│   ├── app/                 # Main application module
│   ├── config/              # Configuration files
│   └── gradle/              # Build configuration
├── windows/                 # Windows teacher application
├── docs/                    # Project documentation
│   ├── API Contract.md      # Communication protocols
│   ├── Project Specification.md
│   ├── Android System Design.md
│   ├── Validation Rules.md
│   └── ...
└── assets/                  # Project assets
```

### Building the Applications

**Android Application**

```bash
cd android
./gradlew build
```

**Windows Application**

Refer to the Windows team documentation in the `windows/` directory for build instructions.

## Documentation

Comprehensive documentation is available in the `/docs` directory:

| Document | Description |
|----------|-------------|
| [Project Specification](docs/Project%20Specification.md) | Complete requirements and system overview |
| [API Contract](docs/API%20Contract.md) | Communication protocols and data formats |
| [Android System Design](docs/Android%20System%20Design.md) | Mobile application architecture |
| [Validation Rules](docs/Validation%20Rules.md) | Data model constraints and validation |
| [Pairing Process](docs/Pairing%20Process.md) | Device discovery and connection |
| [Session Interaction](docs/Session%20Interaction.md) | Runtime communication patterns |
| [Material Encoding](docs/Material%20Encoding.md) | Content format specification |
| [GitHub Conventions](docs/Github%20Conventions.md) | Development workflow guidelines |

## Team

Manuscripta is developed by:

- **Raphael Li** — Windows Application Development
- **Nemo Shu** — Windows Application Development
- **Will Stephen** — Android Application Development
- **Priya Bargota** — Android Application Development

### Supervision and Partners

This project is supervised by **Professor Dean Mohamedally** at University College London, with industry partners including:

- **Qualcomm** — AI chipset and Snapdragon technology support
- **IBM Granite+Interactions Group** — Generative AI model integration
- **National Autistic Society** — Accessibility requirements consultation

## Privacy and Ethics

Manuscripta is designed with privacy at its core:

- **No Cloud Dependency** — All data remains on the local network
- **Anonymised Data** — Student responses are aggregated without personal identifiers
- **GDPR Compliant** — No personally identifiable student data is collected or stored
- **Low Energy Footprint** — E-ink displays and localised AI processing reduce environmental impact

## Licence

This project is licensed under the MIT Licence. See the [LICENSE](LICENSE) file for details.

---

<p align="center">
  <sub>Manuscripta — Bringing focus back to the classroom</sub>
</p>