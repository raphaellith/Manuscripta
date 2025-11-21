# Manuscripta Student Client (Android)

Manuscripta Student Client is an Android application built with Clean Architecture principles, XML-based UI, and modern Android development practices.

## Architecture

This project follows **Clean Architecture** principles with clear separation of concerns:

```
com.manuscripta.student/
├── data/               # Data layer
│   ├── local/         # Room database
│   ├── model/         # Data models
│   └── repository/    # Repository implementations
├── di/                # Dependency injection (Hilt modules)
├── network/           # Retrofit API services
├── ui/                # Presentation layer
│   ├── main/         # Main screen (Activity, ViewModel)
│   └── components/   # Reusable UI components
└── utils/             # Utility classes and constants
```

### Architecture Layers

1. **Presentation Layer** (`ui/`): Activities, Fragments, ViewModels
2. **Domain Layer**: Business logic and use cases (to be added as needed)
3. **Data Layer** (`data/`, `network/`): Repositories, data sources, API services

## Features

The Manuscripta Student Client implements the following key features based on project requirements:

### Lesson Materials (MAT)
- **Material Display**: Display all supported material types including quizzes, worksheets, and polls
- **Immediate Feedback**: Provide formative feedback on quiz responses with ✓ or ✗ indicators and explanations
- **Try Again**: Allow students to retry incorrect answers with guidance
- **Content Transformation**: Simplify, expand, or summarize text content to support different reading levels
- **AI Assistance**: Provide AI-powered scaffolding tools to guide student learning
- **Key Vocabulary Display** (MAT6): Dedicated area to highlight and display teacher-defined key vocabulary terms for each lesson
- **Raise Hand Button** (MAT7): Send help requests to the teacher's dashboard when assistance is needed
- **Handwriting Annotation** (MAT8): Support stylus-based annotation directly on worksheets and PDF documents

### Accessibility (ACC)
- **Multiple Input Methods**: Support touch, typing, and stylus input for diverse learning needs
- **Text-to-Speech**: Read on-screen content aloud when enabled by the teacher
- **Monochrome Display**: Optimized for e-ink displays with minimal audiovisual stimuli to reduce distraction
- **Eye Gaze Control**: Optional support via third-party hardware (future consideration)

### Device Management (SYS)
- **Kiosk Mode**: Lock the device to the Manuscripta app to prevent access to other applications
- **E-ink Optimization**: Performance tuning for e-ink display characteristics (reduced refresh rates, partial updates)
- **Connection Monitoring**: Track and report connection status to the teacher's server
- **Battery Monitoring**: Report battery levels and alert when power is low
- **Remote Control**: Support teacher-initiated screen locks and session management

### Networking (NET)
- **Local Area Network**: Communicate with the teacher's Windows application over LAN
- **Material Synchronization**: Receive lesson materials and content from the teacher
- **Response Submission**: Send quiz answers, poll responses, and student progress data
- **Offline Queue**: Queue responses when disconnected and sync when connection is restored

## Technology Stack

### Core Technologies
- **Language**: Java 17
- **UI**: XML layouts with ViewBinding
- **Architecture**: Clean Architecture + MVVM

### Libraries & Frameworks

#### Dependency Injection
- **Hilt** (2.52): Dependency injection with KSP for Java

#### Networking
- **Retrofit** (2.11.0): RESTful API client
- **OkHttp** (4.12.0): HTTP client with logging interceptor
- **Gson**: JSON serialization/deserialization

#### Database
- **Room** (2.6.1): SQLite object mapping library

#### Android Jetpack
- **AppCompat** (1.7.0): Backward compatibility
- **Material Design** (1.12.0): Material components
- **ConstraintLayout** (2.2.0): Flexible layouts
- **Lifecycle** (2.9.4): ViewModels and LiveData

#### Testing
- **JUnit** (4.13.2): Unit testing framework
- **Mockito** (5.14.2): Mocking framework
- **Robolectric** (4.14.1): Android unit testing
- **Espresso** (3.7.0): UI testing
- **AndroidX Test**: Testing utilities

#### Code Quality
- **Checkstyle** (10.12.0): Java code style enforcement
- **JaCoCo** (0.8.12): Code coverage analysis

## Getting Started

### Prerequisites

- **Android Studio**: Ladybug | 2024.2.1 or later
- **JDK**: Version 17
- **Android SDK**: API 27+ (Minimum), API 36 (Target)
- **Gradle**: 8.13.1 (via wrapper)

### Building the Project

1. **Clone the repository**
   ```bash
   git clone https://github.com/raphaellith/Manuscripta.git
   cd Manuscripta/android
   ```

2. **Open in Android Studio**
   - File → Open → Select the `android` directory

3. **Sync Gradle**
   - Android Studio will automatically sync dependencies
   - Or run: `./gradlew build`

4. **Run the app**
   - Click the "Run" button in Android Studio
   - Or via command line:
     ```bash
     ./gradlew installDebug
     ```

## Testing

### Running Unit Tests

```bash
./gradlew testDebugUnitTest
```

### Running Instrumented Tests

```bash
./gradlew connectedAndroidTest
```

### Code Coverage

Generate coverage report:
```bash
./gradlew jacocoTestReport
```

View the HTML report at:
```
app/build/reports/jacoco/jacocoTestReport/html/index.html
```

Verify 100% coverage:
```bash
./gradlew jacocoTestCoverageVerification
```

### Coverage Exclusions

The following are excluded from coverage requirements:
- Generated code (R.class, BuildConfig)
- Data binding classes
- Hilt generated files (_Factory, _MembersInjector, Module, Component)
- Application class (ManuscriptaApplication)
- Test files

## Code Quality

### Checkstyle

Run Checkstyle analysis:
```bash
./gradlew checkstyle
```

View the report at:
```
app/build/reports/checkstyle/checkstyle.xml
```

### Configuration

- **Checkstyle config**: `config/checkstyle/checkstyle.xml`
- **Line length**: 120 characters max
- **Method length**: 150 lines max
- **Parameters**: 7 max per method

## CI/CD

### GitHub Actions

The project uses GitHub Actions for continuous integration:

**Workflow**: `.github/workflows/pr-check.yml`

#### PR Checks (Triggered on `android/**` branches)

1. **Checkstyle Job**
   - Runs code style checks
   - Fails on any warnings
   - Uploads analysis reports

2. **Unit Tests Job**
   - Executes all unit tests
   - Uploads test results

3. **Coverage Job**
   - Generates JaCoCo coverage report
   - Enforces 100% coverage (with standard exclusions)
   - Comments coverage report on PR
   - Uploads coverage artifacts

### CI Requirements

All PRs must pass:
- Checkstyle with zero warnings
- All unit tests passing
- 100% code coverage (excluding standard exclusions)

## 📁 Project Structure

```
android/
├── .github/
│   └── workflows/
│       └── pr-check.yml          # CI/CD workflow
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/
│   │   │   │   └── com/manuscripta/student/
│   │   │   │       ├── ManuscriptaApplication.java
│   │   │   │       ├── data/
│   │   │   │       │   ├── local/
│   │   │   │       │   │   └── ManuscriptaDatabase.java
│   │   │   │       │   ├── model/
│   │   │   │       │   └── repository/
│   │   │   │       ├── di/
│   │   │   │       │   ├── DatabaseModule.java
│   │   │   │       │   └── NetworkModule.java
│   │   │   │       ├── network/
│   │   │   │       │   └── ApiService.java
│   │   │   │       ├── ui/
│   │   │   │       │   └── main/
│   │   │   │       │       ├── MainActivity.java
│   │   │   │       │       └── MainViewModel.java
│   │   │   │       └── utils/
│   │   │   │           └── Constants.java
│   │   │   ├── res/
│   │   │   │   ├── layout/
│   │   │   │   │   └── activity_main.xml
│   │   │   │   ├── values/
│   │   │   │   └── ...
│   │   │   └── AndroidManifest.xml
│   │   ├── test/                 # Unit tests
│   │   └── androidTest/          # Instrumented tests
│   └── build.gradle.kts
├── config/
│   ├── checkstyle/
│   │   └── checkstyle.xml
│   └── jacoco/
│       └── README.md
├── gradle/
│   └── libs.versions.toml        # Dependency versions
├── build.gradle.kts
└── README.md
```

## Configuration Files

### Gradle Configuration

- **Root**: `build.gradle.kts` - Project-level build configuration
- **App**: `app/build.gradle.kts` - Module-level build configuration
- **Versions**: `gradle/libs.versions.toml` - Centralized dependency versions

### Quality Configuration

- **Checkstyle**: `config/checkstyle/checkstyle.xml`
- **JaCoCo**: Configured in `app/build.gradle.kts`

## Permissions

The app requires the following permissions:
- `INTERNET`: For network communication with the Manuscripta API
- `WAKE_LOCK`: For maintaining connection during active sessions
- `BATTERY_STATS`: For monitoring battery level

## Hardware Requirements

The student client is designed for Android-based e-ink tablets such as:
- **Boox** series (e.g., Boox Tab, Boox Note Air)
- **AiPaper** devices
- Other Android e-ink tablets with API 27+ support

**Key Considerations:**
- **Stylus Support**: For handwriting annotation features (MAT8)
- **Display**: Monochrome e-ink optimized for reduced distraction (ACC4)
- **Connectivity**: WiFi required for LAN communication with teacher device
- **Kiosk Mode**: Android Enterprise features required for full device lockdown

## Development Guidelines

### Code Style

- Follow Java naming conventions
- Maximum line length: 120 characters
- Use meaningful variable and method names
- Add Javadoc comments for public methods and classes
- Run Checkstyle before committing: `./gradlew checkstyle`

### Testing

- Write unit tests for all business logic
- Aim for 100% code coverage (excluding standard exclusions)
- Use meaningful test names following the pattern: `test[MethodName][Scenario]`
- Mock external dependencies using Mockito

### Git Workflow

1. Create feature branch from `main`: `android/feature-name`
2. Make changes and commit with clear messages
3. Run tests and quality checks locally
4. Push and create Pull Request
5. Ensure all CI checks pass
6. Request code review

---

**Note**: This project is configured for Java 17 with strict code quality requirements. All code must pass Checkstyle validation and maintain 100% unit test coverage (with standard exclusions) before merging.
