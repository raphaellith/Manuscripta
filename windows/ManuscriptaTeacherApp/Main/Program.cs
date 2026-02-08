using Microsoft.AspNetCore.Builder;
using Microsoft.Extensions.Hosting;
using Microsoft.EntityFrameworkCore;
using ChromaDB.Client;
using System.Diagnostics;
using System.Net.Http;
using System.Threading.Tasks;

using Main.Data;
using Main.Services;
using Main.Services.Network;
using Main.Services.GenAI;

var builder = WebApplication.CreateBuilder(args);

// Start ChromaDB server if not already running
// See GenAISpec.md §2(3)
var autoStartChroma = builder.Configuration.GetValue("ChromaDB:AutoStart", true);
if (autoStartChroma)
{
    await StartChromaDbServerAsync(builder.Configuration);
}

// Configure network settings from appsettings.json
builder.Services.Configure<NetworkSettings>(
    builder.Configuration.GetSection("NetworkSettings"));

builder.Services.AddDbContext<MainDbContext>(options =>
  options.UseSqlite(builder.Configuration.GetConnectionString("MainDbContext")));

// Configure ChromaDB for client-server mode
// See GenAISpec.md §2(3)
// Note: ChromaDB.Client is a client-server library. The database path is configured
// on the server side (e.g., `chroma run --path /path/to/store`), not in the C# client.
// The client connects via HTTP to the running server using the URI below.
var chromaServerUri = builder.Configuration["ChromaDB:ServerUri"] ?? "http://localhost:8000/api/v1/";

builder.Services.AddSingleton(new ChromaConfigurationOptions(uri: chromaServerUri));
builder.Services.AddHttpClient();
builder.Services.AddSingleton<ChromaClient>(sp =>
{
    var httpClientFactory = sp.GetRequiredService<IHttpClientFactory>();
    var httpClient = httpClientFactory.CreateClient();
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

// Register CRUD services for hub
builder.Services.AddScoped<IUnitCollectionService, UnitCollectionService>();
builder.Services.AddScoped<IUnitService, UnitService>();
builder.Services.AddScoped<ILessonService, LessonService>();
builder.Services.AddScoped<IMaterialService, MaterialService>();
builder.Services.AddScoped<IQuestionService, QuestionService>();
builder.Services.AddScoped<IResponseService, ResponseService>();
builder.Services.AddScoped<ISourceDocumentService, SourceDocumentService>();
builder.Services.AddSingleton<IFileService, FileService>();
builder.Services.AddScoped<IAttachmentService, AttachmentService>();
builder.Services.AddScoped<IFeedbackService, FeedbackService>();

// Register GenAI services
// See GenAISpec.md §3(1) and §3(2)
builder.Services.AddSingleton<OllamaClientService>();
builder.Services.AddScoped<IEmbeddingService, DocumentEmbeddingService>();
builder.Services.AddScoped<MaterialGenerationService>();
builder.Services.AddScoped<ContentModificationService>();
builder.Services.AddSingleton<FeedbackGenerationService>();
builder.Services.AddSingleton<IFeedbackGenerationService>(
    provider => provider.GetRequiredService<FeedbackGenerationService>());
builder.Services.AddHostedService(provider => provider.GetRequiredService<FeedbackGenerationService>());
builder.Services.AddSingleton<FeedbackQueueService>();
builder.Services.AddScoped<EmbeddingStatusService>();
builder.Services.AddScoped<OutputValidationService>();
builder.Services.AddScoped<QuestionExtractionService>();

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

// NOTE: Controllers are enabled so that REST controllers can be added later.
builder.Services.AddControllers();
builder.Services.AddSignalR(hubOptions =>
{
    hubOptions.EnableDetailedErrors = builder.Environment.IsDevelopment();
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

if (app.Environment.IsDevelopment())
{
    app.UseDeveloperExceptionPage();
}

// Apply CORS policy
app.UseCors("AllowElectron");

using (var scope = app.Services.CreateScope())
{
    var services = scope.ServiceProvider;

    var context = services.GetRequiredService<MainDbContext>();
    context.Database.EnsureCreated();
    // DbInitializer.Initialize(context);

    // Initialize embedding startup handler per GenAISpec.md §3A(8)
    var embeddingService = services.GetRequiredService<Main.Services.GenAI.IEmbeddingService>();
    await embeddingService.InitializeFailedEmbeddingsAsync();

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
}

// Minimal default endpoint to confirm the app is running.
app.MapGet("/", () => Results.Ok("Manuscripta Main API (net10.0) is running"));

app.MapControllers();
app.MapHub<Main.Services.Hubs.TeacherPortalHub>("/hub");

app.Run();


// Helper method to start ChromaDB server if not already running
async Task StartChromaDbServerAsync(IConfiguration configuration)
{
    var serverUri = configuration["ChromaDB:ServerUri"] ?? "http://localhost:8000/api/v1/";
    
    // Per GenAISpec.md §2(3)(b): ChromaDB shall store its data in %AppData%\ManuscriptaTeacherApp\VectorStore
    var dataPath = Path.Combine(
        Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData),
        "ManuscriptaTeacherApp",
        "VectorStore"
    );
    
    // Ensure data directory exists
    Directory.CreateDirectory(dataPath);
    
    // Check if server is already running
    try
    {
        using var httpClient = new HttpClient { Timeout = TimeSpan.FromSeconds(2) };
        var healthCheckUri = serverUri.TrimEnd('/') + "/heartbeat";
        var response = await httpClient.GetAsync(healthCheckUri);
        
        if (response.IsSuccessStatusCode)
        {
            Console.WriteLine("ChromaDB server is already running.");
            return;
        }
    }
    catch
    {
        // Server is not running, proceed to start it
    }
    
    try
    {
        Console.WriteLine("Starting ChromaDB server...");
        
        var chromaProcess = new Process
        {
            StartInfo = new ProcessStartInfo
            {
                FileName = "chroma",
                Arguments = $"run --path \"{dataPath}\"",
                UseShellExecute = false,
                RedirectStandardOutput = true,
                RedirectStandardError = true,
                CreateNoWindow = true
            }
        };
        
        chromaProcess.Start();
        
        // Wait for server to start (up to 30 seconds)
        var startTime = DateTime.UtcNow;
        var timeout = TimeSpan.FromSeconds(30);
        var serverStarted = false;
        
        while (DateTime.UtcNow - startTime < timeout)
        {
            try
            {
                using var httpClient = new HttpClient { Timeout = TimeSpan.FromSeconds(1) };
                var healthCheckUri = serverUri.TrimEnd('/') + "/heartbeat";
                var response = await httpClient.GetAsync(healthCheckUri);
                
                if (response.IsSuccessStatusCode)
                {
                    serverStarted = true;
                    Console.WriteLine("ChromaDB server started successfully.");
                    break;
                }
            }
            catch
            {
                // Server not ready yet, retry
            }
            
            await Task.Delay(500);
        }
        
        if (!serverStarted)
        {
            Console.WriteLine("Warning: ChromaDB server may not have started properly. Continuing anyway...");
        }
    }
    catch (Exception ex)
    {
        Console.WriteLine($"Failed to start ChromaDB server: {ex.Message}");
        Console.WriteLine("Ensure ChromaDB is installed and the 'chroma' command is available in your PATH.");
        throw;
    }
}

// Expose Program class for WebApplicationFactory integration testing
public partial class Program { }
