using Microsoft.AspNetCore.Builder;
using Microsoft.Extensions.Hosting;
using Microsoft.EntityFrameworkCore;
using Microsoft.EntityFrameworkCore.Diagnostics;
using ChromaDB.Client;
using System.Diagnostics;
using System.Net.Http;
using System.Threading.Tasks;

using Main.Data;
using Main.Models;
using Main.Services;
using Main.Services.Network;
using Main.Services.GenAI;

var builder = WebApplication.CreateBuilder(args);

// Configure network settings from appsettings.json
builder.Services.AddOptions<NetworkSettings>()
    .Bind(builder.Configuration.GetSection("NetworkSettings"))
    .Validate(settings => settings.ArePortsDistinct(), "NetworkSettings ports must be distinct.")
    .ValidateOnStart();

// Resolve the database path to a deterministic absolute location under %APPDATA%.
// This prevents the SQLite file from being created in an unpredictable working directory
// when the backend is spawned by Electron. The appsettings.json connection string may
// override this default if set to a non-default value.
var configuredConnectionString = builder.Configuration.GetConnectionString("MainDbContext");
string dbConnectionString;

if (string.IsNullOrWhiteSpace(configuredConnectionString)
    || configuredConnectionString == "Data Source=manuscripta.db")
{
    // Default: resolve to %APPDATA%/ManuscriptaTeacherApp/manuscripta.db
    var appDataDir = Path.Combine(
        Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData),
        "ManuscriptaTeacherApp");
    Directory.CreateDirectory(appDataDir);
    var dbPath = Path.Combine(appDataDir, "manuscripta.db");
    dbConnectionString = $"Data Source={dbPath}";
}
else
{
    dbConnectionString = configuredConnectionString;
}

builder.Services.AddDbContext<MainDbContext>(options =>
{
    options.UseSqlite(dbConnectionString);
    // Suppress PendingModelChangesWarning in Testing environment (for integration tests with in-memory databases)
    if (builder.Environment.IsEnvironment("Testing"))
    {
        options.ConfigureWarnings(w => 
            w.Ignore(Microsoft.EntityFrameworkCore.Diagnostics.RelationalEventId.PendingModelChangesWarning));
    }
});

// Configure ChromaDB for client-server mode
// See GenAISpec.md §2(3)
// Note: ChromaDB.Client is a client-server library. The database path is configured
// on the server side (e.g., `chroma run --path /path/to/store`), not in the C# client.
// The client connects via HTTP to the running server using the URI below.
var chromaServerUri = builder.Configuration["ChromaDB:ServerUri"] ?? "http://localhost:8000/api/v2/";

builder.Services.AddSingleton(new ChromaConfigurationOptions(uri: chromaServerUri));
// Register a named "ChromaDB" HttpClient with a URL rewrite handler that translates
// ChromaDB.Client v1 API URLs (query-parameter-based tenant/database) into v2 API
// URLs (path-based routing) expected by the Chroma Rust CLI server.
// See GenAISpec.md §1B, §2(3)(a1).
builder.Services.AddTransient<ChromaV2UrlRewriteHandler>();
builder.Services.AddHttpClient("ChromaDB")
    .AddHttpMessageHandler<ChromaV2UrlRewriteHandler>();
builder.Services.AddHttpClient(); // default HttpClient for non-Chroma use
builder.Services.AddSingleton<ChromaClient>(sp =>
{
    var httpClientFactory = sp.GetRequiredService<IHttpClientFactory>();
    var httpClient = httpClientFactory.CreateClient("ChromaDB");
    return new ChromaClient(new ChromaConfigurationOptions(uri: chromaServerUri), httpClient);
});

// Register pairing and device services
builder.Services.AddSingleton<IDeviceRegistryService, DeviceRegistryService>();
builder.Services.AddScoped<DeviceIdValidator>();

// Register repository services for hub
builder.Services.AddScoped<Main.Services.Repositories.IUnitCollectionRepository, Main.Services.Repositories.EfUnitCollectionRepository>();
builder.Services.AddScoped<Main.Services.Repositories.IUnitRepository, Main.Services.Repositories.EfUnitRepository>();
builder.Services.AddScoped<Main.Services.Repositories.ILessonRepository, Main.Services.Repositories.EfLessonRepository>();
builder.Services.AddScoped<Main.Services.Repositories.IMaterialRepository, Main.Services.Repositories.EfMaterialRepository>();
builder.Services.AddScoped<Main.Services.Repositories.IQuestionRepository, Main.Services.Repositories.EfQuestionRepository>();
builder.Services.AddSingleton<Main.Services.Repositories.IResponseRepository, Main.Services.Repositories.InMemoryResponseRepository>();
builder.Services.AddSingleton<Main.Services.Repositories.IFeedbackRepository, Main.Services.Repositories.InMemoryFeedbackRepository>();
builder.Services.AddScoped<Main.Services.Repositories.ISourceDocumentRepository, Main.Services.Repositories.EfSourceDocumentRepository>();
builder.Services.AddScoped<Main.Services.Repositories.IAttachmentRepository, Main.Services.Repositories.EfAttachmentRepository>();

// Register configuration — two-tier model per ConfigurationManagementSpecification
// Defaults: long-term persisted (EF Core) per PersistenceAndCascadingRules §1(1)(h)
builder.Services.AddScoped<Main.Services.Repositories.IDefaultConfigurationRepository, Main.Services.Repositories.EfDefaultConfigurationRepository>();
// Overrides: short-term persisted (in-memory) per PersistenceAndCascadingRules §1(2)
builder.Services.AddSingleton<Main.Services.Repositories.IConfigurationOverrideRepository, Main.Services.Repositories.InMemoryConfigurationOverrideRepository>();
builder.Services.AddScoped<IConfigurationService, ConfigurationService>();
// Singleton listener for §3(1)(a): DevicePaired → config refresh
builder.Services.AddSingleton<ConfigurationRefreshListener>();

// Register CRUD services for hub
builder.Services.AddScoped<IUnitCollectionService, UnitCollectionService>();
builder.Services.AddScoped<IUnitService, UnitService>();
builder.Services.AddScoped<ILessonService, LessonService>();
builder.Services.AddScoped<IMaterialService, MaterialService>();
builder.Services.AddScoped<IQuestionService, QuestionService>();
builder.Services.AddScoped<IResponseService, ResponseService>();
builder.Services.AddScoped<ISourceDocumentService, SourceDocumentService>();
builder.Services.AddSingleton<IFileService, FileService>();
builder.Services.AddSingleton<Main.Services.Latex.ILatexRenderer, Main.Services.Latex.LatexRenderer>();
builder.Services.AddScoped<IAttachmentService, AttachmentService>();
builder.Services.AddScoped<IMaterialPdfService, MaterialPdfService>();

// Register external device and email services - NetworkingAPISpec §1(1)(n) and §1(1)(o)
builder.Services.AddScoped<Main.Services.Repositories.IExternalDeviceRepository, Main.Services.Repositories.EfExternalDeviceRepository>();
builder.Services.AddScoped<Main.Services.Repositories.IEmailCredentialRepository, Main.Services.Repositories.EfEmailCredentialRepository>();
builder.Services.AddSingleton<IRmapiService>(sp =>
    new RmapiService(
        sp.GetRequiredService<ILogger<RmapiService>>(),
        new HttpClient()));
builder.Services.AddScoped<IEmailService, SmtpEmailService>();
builder.Services.AddScoped<IExternalDeviceDeploymentService, ExternalDeviceDeploymentService>();
builder.Services.AddScoped<IFeedbackService, FeedbackService>();

// Register GenAI services
// See GenAISpec.md §3(1) and §3(2)
builder.Services.AddSingleton<OllamaClientService>();
builder.Services.AddScoped<IEmbeddingService, DocumentEmbeddingService>();
builder.Services.AddScoped<IMaterialGenerationService, MaterialGenerationService>();
builder.Services.AddScoped<IContentModificationService, ContentModificationService>();
builder.Services.AddSingleton<FeedbackGenerationService>();
builder.Services.AddSingleton<IFeedbackGenerationService>(
    provider => provider.GetRequiredService<FeedbackGenerationService>());
builder.Services.AddHostedService(provider => provider.GetRequiredService<FeedbackGenerationService>());
builder.Services.AddSingleton<FeedbackQueueService>();
builder.Services.AddScoped<IEmbeddingStatusService, EmbeddingStatusService>();
builder.Services.AddScoped<OutputValidationService>();
builder.Services.AddScoped<QuestionExtractionService>();

// Register Runtime Dependency Management
builder.Services.AddSingleton<Main.Services.RuntimeDependencies.RuntimeDependencyRegistry>();
builder.Services.AddSingleton<Main.Services.RuntimeDependencies.IRuntimeDependencyRegistry>(sp =>
{
    var registry = sp.GetRequiredService<Main.Services.RuntimeDependencies.RuntimeDependencyRegistry>();
    var rmapiManager = sp.GetRequiredService<Main.Services.RuntimeDependencies.RmapiRuntimeDependencyManager>();
    var ollamaManager = sp.GetRequiredService<Main.Services.RuntimeDependencies.OllamaRuntimeDependencyManager>();
    var chromaManager = sp.GetRequiredService<Main.Services.RuntimeDependencies.ChromaRuntimeDependencyManager>();
    var qwen3Manager = sp.GetRequiredService<Main.Services.RuntimeDependencies.Qwen3ModelRuntimeDependencyManager>();
    var graniteManager = sp.GetRequiredService<Main.Services.RuntimeDependencies.GraniteModelRuntimeDependencyManager>();
    var nomicManager = sp.GetRequiredService<Main.Services.RuntimeDependencies.NomicEmbedTextModelRuntimeDependencyManager>();
    
    registry.Register(rmapiManager);
    registry.Register(ollamaManager);
    registry.Register(chromaManager);
    registry.Register(qwen3Manager);
    registry.Register(graniteManager);
    registry.Register(nomicManager);
    return registry;
});
builder.Services.AddSingleton<Main.Services.RuntimeDependencies.RmapiRuntimeDependencyManager>();
builder.Services.AddSingleton<Main.Services.RuntimeDependencies.OllamaRuntimeDependencyManager>();
builder.Services.AddSingleton<Main.Services.RuntimeDependencies.ChromaRuntimeDependencyManager>();
builder.Services.AddSingleton<Main.Services.RuntimeDependencies.Qwen3ModelRuntimeDependencyManager>();
builder.Services.AddSingleton<Main.Services.RuntimeDependencies.GraniteModelRuntimeDependencyManager>();
builder.Services.AddSingleton<Main.Services.RuntimeDependencies.NomicEmbedTextModelRuntimeDependencyManager>();

// Register network services (singletons for background services)
builder.Services.AddSingleton<IRefreshConfigTracker, RefreshConfigTracker>();
builder.Services.AddSingleton<IUdpBroadcastService, UdpBroadcastService>();
builder.Services.AddSingleton<ITcpPairingService, TcpPairingService>();
builder.Services.AddSingleton<IDeviceStatusCacheService, DeviceStatusCacheService>();
builder.Services.AddSingleton<IDistributionService, DistributionService>();

// NOTE: UDP broadcasting and TCP pairing are NOT auto-started.
// They should be triggered on-demand via UI when the teacher starts a pairing/classroom session.
// The services are registered as singletons above and can be injected where needed.
// builder.Services.AddHostedService<UdpBroadcastHostedService>();
// builder.Services.AddHostedService<TcpPairingHostedService>();
builder.Services.AddHostedService<HubEventBridge>();

// Register embedding initialization as background service per GenAISpec.md §3A(8)
// This runs asynchronously after startup to avoid blocking the health endpoint
builder.Services.AddHostedService<EmbeddingInitializationHostedService>();

// NOTE: Controllers are enabled so that REST controllers can be added later.
builder.Services.AddControllers();
builder.Services.AddSignalR(hubOptions =>
{
    hubOptions.EnableDetailedErrors = builder.Environment.IsDevelopment();
    // Allow CancelGeneration to execute concurrently with an in-progress
    // GenerateReading/GenerateWorksheet call on the same connection.
    // Default is 1, which serialises all hub invocations and prevents
    // cancellation from reaching the server while generation is running.
    hubOptions.MaximumParallelInvocationsPerClient = 2;
    // Increase max message size from 32KB default to 10MB for large source documents
    // (per FrontendWorkflowSpecifications §4AA source document uploads)
    hubOptions.MaximumReceiveMessageSize = 10 * 1024 * 1024;
});
// Per AdditionalValidationRules.md s1A(1): PascalCase fields, SCREAMING_SNAKE_CASE enums
builder.Services.AddControllers()
    .AddJsonOptions(options =>
    {
        // s1A(1): Fields serialised in PascalCase (null = preserve original C# casing)
        options.JsonSerializerOptions.PropertyNamingPolicy = null;
        // s1A(1): Enum members serialised as SCREAMING_SNAKE_CASE strings
        options.JsonSerializerOptions.Converters.Add(new System.Text.Json.Serialization.JsonStringEnumConverter());
    });
builder.Services.AddSignalR()
    .AddJsonProtocol(options =>
    {
        // SignalR is internal to the Windows app (backend ↔ Electron frontend).
        // Per Validation Rules s1(6), internal implementation may use another case where justifiable.
        // camelCase is idiomatic for the TypeScript frontend consuming these payloads.
        options.PayloadSerializerOptions.PropertyNamingPolicy = System.Text.Json.JsonNamingPolicy.CamelCase;
        // Enum members serialised as SCREAMING_SNAKE_CASE strings per s1A(1)
        options.PayloadSerializerOptions.Converters.Add(new System.Text.Json.Serialization.JsonStringEnumConverter());
    });

// Enable CORS for Electron renderer (running on different port)
builder.Services.AddCors(options =>
{
    options.AddPolicy("AllowElectron", policy =>
    {
        policy.WithOrigins("http://localhost:9000", "http://localhost:3000")
              .AllowAnyHeader()
              .AllowAnyMethod()
              .AllowCredentials();
    });
});

var app = builder.Build();

// Eagerly resolve singleton so its DevicePaired event subscription is active
app.Services.GetRequiredService<ConfigurationRefreshListener>();
// Configure QuestPDF license once at startup (before any PDFs are generated)
QuestPDF.Settings.License = QuestPDF.Infrastructure.LicenseType.Community;

if (app.Environment.IsDevelopment())
{
    app.UseDeveloperExceptionPage();
}

// Apply CORS policy
app.UseCors("AllowElectron");

// Skip database initialization and orphan cleanup in Testing environment (for integration tests)
if (!app.Environment.IsEnvironment("Testing"))
{
    using (var scope = app.Services.CreateScope())
    {
        var services = scope.ServiceProvider;

        var context = services.GetRequiredService<MainDbContext>();
        
        // Only run migrations if we are NOT running from EF Core tools (to prevent DB lock issues during design time)
        bool isEfCoreTool = AppDomain.CurrentDomain.GetAssemblies().Any(a => a.GetName().Name == "ef");
        if (!isEfCoreTool)
        {
            // Pre-migration schema fixup: handle databases where AddReMarkableDevices was applied
            // before the SourceDocuments column was added to UnitEntity. Since the migration ID is
            // already in __EFMigrationsHistory, EF skips it and the column is never created.
            try
            {
                var conn = context.Database.GetDbConnection();
                if (conn.State != System.Data.ConnectionState.Open) await conn.OpenAsync();

                using var checkCmd = conn.CreateCommand();
                checkCmd.CommandText =
                    "SELECT COUNT(*) FROM sqlite_master WHERE type='table' AND name='Units' " +
                    "AND sql NOT LIKE '%SourceDocuments%'";
                var needsFixup = Convert.ToInt64(await checkCmd.ExecuteScalarAsync()) > 0;

                if (needsFixup)
                {
                    using var alterCmd = conn.CreateCommand();
                    alterCmd.CommandText = "ALTER TABLE \"Units\" ADD COLUMN \"SourceDocuments\" TEXT NOT NULL DEFAULT '[]'";
                    await alterCmd.ExecuteNonQueryAsync();
                    Console.WriteLine("[MIGRATION FIXUP] Added missing SourceDocuments column to Units table.");
                }
            }
            catch (Exception ex)
            {
                // Ignore on fresh databases where the Units table doesn't exist yet
                Console.WriteLine($"[MIGRATION FIXUP] Skipped (expected on fresh DB): {ex.Message}");
            }

            context.Database.Migrate();
        }
        
        // DbInitializer.Initialize(context);

        // Orphan file removal per PersistenceAndCascadingRules §3
        // Delete attachment files not linked to any entity
        var fileService = services.GetRequiredService<IFileService>();
        var attachmentRepo = services.GetRequiredService<Main.Services.Repositories.IAttachmentRepository>();
        
        var attachmentsDir = Path.Combine(
            Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData),
            "ManuscriptaTeacherApp",
            "Attachments"
        );
        
        if (Directory.Exists(attachmentsDir))
        {
            // Get all attachment entity IDs
            var allAttachments = await attachmentRepo.GetAllAsync();
            var validIds = new HashSet<string>(allAttachments.Select(a => a.Id.ToString().ToLowerInvariant()));
            
            // Scan directory for orphan files
            foreach (var filePath in Directory.GetFiles(attachmentsDir))
            {
                var fileName = Path.GetFileNameWithoutExtension(filePath);
                // File should be named as UUID
                if (!validIds.Contains(fileName.ToLowerInvariant()))
                {
                    try
                    {
                        fileService.DeleteFile(filePath);
                        Console.WriteLine($"Deleted orphan attachment file: {fileName}");
                    }
                    catch (Exception ex)
                    {
                        Console.WriteLine($"Failed to delete orphan file {fileName}: {ex.Message}");
                    }
                }
            }
        }

        // Orphan rmapi config file removal per PersistenceAndCascadingRules §3(2)
        var rmapiConfigDir = Path.Combine(
            Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData),
            "ManuscriptaTeacherApp",
            "rmapi"
        );

        if (Directory.Exists(rmapiConfigDir))
        {
            var externalRepo = services.GetRequiredService<Main.Services.Repositories.IExternalDeviceRepository>();
            var allDevices = await externalRepo.GetAllAsync();
            
            // Only consider REMARKABLE type devices for rmapi config cleanup (PersistenceAndCascadingRules §3(2))
            var remarkableDevices = allDevices.Where(d => d.Type == Main.Models.Entities.ExternalDeviceType.REMARKABLE).ToList();
            var validDeviceIds = new HashSet<string>(remarkableDevices.Select(d => d.DeviceId.ToString().ToLowerInvariant()));

            var existingConfigFiles = new HashSet<string>(Directory.GetFiles(rmapiConfigDir, "*.conf")
                .Select(f => Path.GetFileNameWithoutExtension(f).ToLowerInvariant()));

            foreach (var device in remarkableDevices)
            {
                if (!existingConfigFiles.Contains(device.DeviceId.ToString().ToLowerInvariant()))
                {
                    try
                    {
                        await externalRepo.DeleteAsync(device.DeviceId);
                        Console.WriteLine($"Deleted orphan ExternalDeviceEntity: {device.DeviceId}");
                    }
                    catch (Exception ex)
                    {
                        Console.WriteLine($"Failed to delete orphan entity {device.DeviceId}: {ex.Message}");
                    }
                }
            }

            foreach (var configFile in Directory.GetFiles(rmapiConfigDir, "*.conf"))
            {
                var fileDeviceId = Path.GetFileNameWithoutExtension(configFile).ToLowerInvariant();
                if (!validDeviceIds.Contains(fileDeviceId))
                {
                    try
                    {
                        File.Delete(configFile);
                        Console.WriteLine($"Deleted orphan rmapi config: {fileDeviceId}.conf");
                    }
                    catch (Exception ex)
                    {
                        Console.WriteLine($"Failed to delete orphan rmapi config {fileDeviceId}: {ex.Message}");
                    }
                }
            }
        }
    }
}

// Per FrontendWorkflowSpecifications §2ZA(5)(a)-(d): Embedding initialization is now a background
// hosted service and will not block startup, ensuring the health endpoint responds quickly.

// Port-based routing per API Contract.md §Ports and FrontendWorkflowSpecifications §2ZA(8).
// - SignalR and health endpoint: available on ANY bound port (frontend uses dynamic port selection)
// - REST API controllers: ONLY on port 5911 (Android clients need stable port per API Contract)
//
// Per FrontendWorkflowSpecifications §2ZA(8)(c), the SignalR port is dynamically selected:
// frontend tries 5910 first, then falls back to 5914-5919 if unavailable.
// Therefore, SignalR/health must NOT be restricted to a specific port.
//
// Note: In Testing environment, host constraints are skipped since TestWebApplicationFactory
// uses an in-memory test server that doesn't bind to real ports.
var networkSettings = app.Configuration.GetSection("NetworkSettings").Get<NetworkSettings>() ?? new NetworkSettings();

if (app.Environment.IsEnvironment("Testing"))
{
    // Testing environment: no host constraints for in-memory test server
    app.MapGet("/", () => Results.Ok("Manuscripta Main API (net10.0) is running"));
    app.MapControllers();
    app.MapHub<Main.Services.Hubs.TeacherPortalHub>("/TeacherPortalHub");
}
else
{
    // Production/Development: enforce port-based routing for HTTP API only
    var httpApiHost = $"*:{networkSettings.HttpPort}";

    // Health endpoint: available on any bound port for frontend health checks.
    // Per FrontendWorkflowSpecifications §2ZA(5)(a): health probe uses dynamic port.
    app.MapGet("/", () => Results.Ok("Manuscripta Main API (net10.0) is running"));

    // REST API controllers on HTTP API port ONLY per API Contract.md §Ports.
    // Android clients rely on stable port 5911 for material distribution.
    app.MapControllers().RequireHost(httpApiHost);

    // SignalR hub: available on any bound port per FrontendWorkflowSpecifications §2ZA(8).
    // Frontend uses dynamic port selection and connects to whatever port succeeded.
    app.MapHub<Main.Services.Hubs.TeacherPortalHub>("/TeacherPortalHub");
}

// Per GenAISpec §1(9): Runtime dependencies (Ollama, Chroma, LLMs) shall not be installed on startup.
// They must only be installed upon user consent expressed from the frontend.
// Per BackendRuntimeDependencyManagementSpecification §3(1): Frontend assumes dependencies are available.
// When a feature requiring a dependency is used, the backend will notify the frontend via
// RuntimeDependencyNotInstalled if the dependency is unavailable (§3(2)(a)), and the frontend
// will handle this notification per Frontend Workflow Specifications §3A.

app.Run();

// Expose Program class for WebApplicationFactory integration testing
public partial class Program { }
