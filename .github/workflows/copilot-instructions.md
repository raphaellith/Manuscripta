# Manuscripta Android Client - Copilot Instructions

## Context Gathering (MANDATORY FIRST STEP)
Before any implementation work, read relevant documentation in `/docs/`:

### Core Documentation
| Document | Description | Authority |
|----------|-------------|-----------|
| `Project Specification.md` | Requirements referenced as `SECx` (e.g., `MAT1`, `CON2A`) | Requirements source |
| `API Contract.md` | HTTP/TCP/UDP protocols, binary message formats, data models | Network implementation |
| `Validation Rules.md` | Entity validation, field constraints, ID generation policy | Data model definitions |
| `Pairing Process.md` | Pairing phases between Windows and Android devices | **Overrides API Contract on conflicts** |
| `Session Interaction.md` | Heartbeat mechanism, material distribution, session lifecycle | Runtime behaviour |

### Reference Documentation
| Document | Description |
|----------|-------------|
| `Android System Design.md` | System architecture audit with ERD, sequence diagrams, state machines |
| `Github Conventions.md` | Branch naming, PR/issue conventions, documentation versioning |

### Source Files
- Implementation: `/android/app/src/main/java/com/manuscripta/student/`

## Architecture Overview

### Clean Architecture Layers
```
data/             → Persistence and network layer
  ├── local/      → Room database (ManuscriptaDatabase), DAOs
  ├── model/      → Entity classes (*Entity.java), enums
  └── repository/ → Repository implementations
domain/           → Business logic layer
  ├── mapper/     → Bidirectional Entity↔Domain mappers (static utility classes)
  └── model/      → Domain models (validation in constructors, factory methods)
di/               → Hilt dependency injection modules
  ├── DatabaseModule   → Database, DAOs
  ├── NetworkModule    → OkHttpClient, Retrofit, ApiService
  ├── SocketModule     → TcpSocketManager, UdpDiscoveryManager
  └── RepositoryModule → Repository bindings, FileStorageManager
network/          → Networking layer
  ├── tcp/        → TCP socket management, message encoding/decoding, pairing
  ├── udp/        → UDP discovery for Windows server detection
  └── ApiService  → Retrofit API interface
ui/               → Activities, ViewModels, Views
utils/            → Utility classes
  ├── Constants         → App-wide constants
  ├── Result            → Generic result wrapper for success/error states
  ├── UiState           → UI state management utilities
  ├── FileStorageManager    → File I/O operations
  └── MulticastLockManager  → WiFi multicast lock handling
```

### Model Layer Separation
| Layer | Suffix | Purpose | Annotations |
|-------|--------|---------|-------------|
| Entity | `*Entity.java` | Room persistence | `@Entity`, `@PrimaryKey` |
| Domain | `*.java` | Business logic, factory methods | None |
| DTO | `*Dto.java` | Network serialisation | JSON annotations |

### Entity ID Contract (CRITICAL)
Per `docs/API Contract.md` §4.1:
- **Materials/Questions**: IDs assigned by Windows server, Android must preserve them
- **Responses/Sessions**: IDs assigned by Android client using `UUID.randomUUID().toString()`

## Code Style Rules (Checkstyle Enforced)

### Formatting
- **Line length**: Maximum 120 characters (imports/packages exempt)
- **Method length**: Maximum 150 lines
- **Parameters**: Maximum 7 per method
- **Indentation**: 4 spaces, no tabs
- **Braces**: Required for all control statements, K&R style (`{` on same line)

### Naming Conventions
- Classes: `PascalCase` (e.g., `MaterialEntity`, `MaterialMapper`)
- Methods/variables: `camelCase`
- Constants: `SCREAMING_SNAKE_CASE`
- Packages: lowercase only

### Imports
- No wildcard imports (`import java.util.*` ✗)
- Remove unused imports
- No redundant imports

### Javadoc Requirements
All public classes, methods, and fields require Javadoc:
```java
/**
 * Brief description of the class/method.
 *
 * @param paramName Description of parameter
 * @return Description of return value
 * @throws ExceptionType When this exception is thrown
 */
```

## Library-Specific Patterns

### Room (Database)
```java
// Entity pattern - use @NonNull for required fields, final for immutability
@Entity(tableName = "materials")
public class MaterialEntity {
    @PrimaryKey @NonNull private final String id;
    @NonNull private final MaterialType type;
    // Constructor with all fields, getters only (no setters for immutability)
}

// Foreign keys with cascade delete and index
@Entity(
    tableName = "responses",
    foreignKeys = @ForeignKey(
        entity = QuestionEntity.class,
        parentColumns = "id", childColumns = "questionId",
        onDelete = ForeignKey.CASCADE
    ),
    indices = @Index("questionId")
)

// DAO pattern - use OnConflictStrategy.REPLACE for upserts
@Dao
public interface MaterialDao {
    @Query("SELECT * FROM materials WHERE id = :id")
    MaterialEntity getById(String id);
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(MaterialEntity material);
}
```

### Hilt (Dependency Injection)
```java
// Application class
@HiltAndroidApp
public class ManuscriptaApplication extends Application { }

// Module pattern - @Singleton for app-wide instances
@Module
@InstallIn(SingletonComponent.class)
public class DatabaseModule {
    @Provides @Singleton
    public ManuscriptaDatabase provideDatabase(@ApplicationContext Context context) { }
}
```

### Retrofit (Networking)
```java
// API interface pattern
public interface ApiService {
    @GET("/materials/{id}")
    Call<MaterialResponse> getMaterial(@Path("id") String materialId);
    
    @POST("/responses")
    Call<Void> submitResponse(@Body ResponseRequest request);
}
```

### Domain Mappers (Static Utility Classes)
```java
public class MaterialMapper {
    private MaterialMapper() {
        throw new AssertionError("Utility class should not be instantiated");
    }
    
    @NonNull
    public static Material toDomain(@NonNull MaterialEntity entity) { }
    
    @NonNull
    public static MaterialEntity toEntity(@NonNull Material domain) { }
}
```

### Domain Models (Validation in Constructor)
```java
public class Material {
    public Material(@NonNull String id, ...) {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("Material id cannot be null or empty");
        }
        // Assign fields
    }
}
```

## Testing Patterns

### Coverage Threshold
**Minimum line coverage: 95%** — Tests should validate meaningful behaviour, not chase 100% coverage through implementation mirroring.

### Behaviour-Driven Testing (MANDATORY)
Tests must verify **expected behaviour and outcomes**, not mirror implementation details:

#### What to Test
- **Correct outputs** for valid inputs (the "happy path")
- **Edge cases** and boundary conditions
- **Error handling** — correct exceptions for invalid inputs
- **State transitions** — observable state changes after operations
- **Contract compliance** — does the class fulfil its documented responsibilities?

#### What NOT to Do
- ❌ Do NOT write tests that simply call each method and assert the return matches a hardcoded value copied from the implementation
- ❌ Do NOT test private methods or internal state via reflection
- ❌ Do NOT use `setAccessible(true)` to bypass access controls
- ❌ Do NOT write tests that would pass even if the implementation were completely wrong
- ❌ Do NOT aim for 100% coverage by testing trivial getters/setters without meaningful assertions

#### Test Quality Checklist
Before considering a test complete, ask:
1. Would this test **fail if the implementation had a bug**?
2. Does this test verify **what the code should do**, not how it does it?
3. Is the test **independent of implementation details** that might change during refactoring?
4. Does the test document **expected behaviour** that another developer could understand?

### Public API Testing
All tests must interact with classes through their **public API only**:
- Set up state using public methods (`startDiscovery()`, `connect()`, etc.)
- Verify outcomes using public methods (`isRunning()`, `getResult()`, etc.)
- If a scenario cannot be tested via public APIs, consider whether the test is necessary or if the class design needs improvement

### DAO Tests (Robolectric + In-Memory Database)
```java
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28, manifest = Config.NONE)
public class MaterialDaoTest {
    private ManuscriptaDatabase database;
    
    @Before
    public void setUp() {
        Context context = ApplicationProvider.getApplicationContext();
        database = Room.inMemoryDatabaseBuilder(context, ManuscriptaDatabase.class)
            .allowMainThreadQueries()
            .fallbackToDestructiveMigration()
            .build();
    }
    
    @After
    public void tearDown() {
        if (database != null) database.close();
    }
}
```

### Mapper Tests (Given-When-Then Pattern)
```java
@Test
public void testToDomain() {
    // Given
    MaterialEntity entity = new MaterialEntity(...);
    // When
    Material domain = MaterialMapper.toDomain(entity);
    // Then
    assertEquals(entity.getId(), domain.getId());
}
```

## Build Commands

## Build & Verification (MANDATORY)

All build checks and verifications **must use Java 17**. Do not mark work as complete until the core check command passes with Java 17:

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 17) ./gradlew checkstyle jacocoTestReport jacocoTestCoverageVerification
```

This command runs all verification tasks (unit tests, Checkstyle, coverage, and build checks) and will fail if any check fails. Always use this as the single source of truth for build and verification.

Individual check commands are also configured to auto-approve:

```bash
cd android
./gradlew testDebugUnitTest      # Run unit tests (Java 17)
./gradlew checkstyle             # Run style checks (Java 17)
./gradlew jacocoTestReport       # Generate coverage report (Java 17)
./gradlew assembleDebug          # Build debug APK (Java 17)
```

**Work is only complete when the above Gradle check command passes with Java 17.**

## UK English
Use British English spelling in all documentation and string literals (e.g., "colour", "behaviour", "summarise", "organise").

## Rules
- Do not use and git write commands such as commit, push, etc.. only commands that read from git unless specified otheriwse. 
- When asked to update git issues, read existing 'android' labelled issues for writing/style refeence and always use UK English.