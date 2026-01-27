using Microsoft.AspNetCore.Builder;
using Microsoft.Extensions.Hosting;
using Microsoft.EntityFrameworkCore;

using Main.Data;
using Main.Services;
using Main.Services.Network;

var builder = WebApplication.CreateBuilder(args);

// Configure network settings from appsettings.json
builder.Services.Configure<NetworkSettings>(
    builder.Configuration.GetSection("NetworkSettings"));

builder.Services.AddDbContext<MainDbContext>(options =>
  options.UseSqlite(builder.Configuration.GetConnectionString("MainDbContext")));

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
builder.Services.AddScoped<Main.Services.Repositories.ISourceDocumentRepository, Main.Services.Repositories.EfSourceDocumentRepository>();
builder.Services.AddScoped<Main.Services.Repositories.IAttachmentRepository, Main.Services.Repositories.EfAttachmentRepository>();

// Register CRUD services for hub
builder.Services.AddScoped<IUnitCollectionService, UnitCollectionService>();
builder.Services.AddScoped<IUnitService, UnitService>();
builder.Services.AddScoped<ILessonService, LessonService>();
builder.Services.AddScoped<IMaterialService, MaterialService>();
builder.Services.AddScoped<IQuestionService, QuestionService>();
builder.Services.AddScoped<ISourceDocumentService, SourceDocumentService>();
builder.Services.AddSingleton<IFileService, FileService>();
builder.Services.AddScoped<IAttachmentService, AttachmentService>();

// Register network services (singletons for background services)
builder.Services.AddSingleton<IRefreshConfigTracker, RefreshConfigTracker>();
builder.Services.AddSingleton<IUdpBroadcastService, UdpBroadcastService>();
builder.Services.AddSingleton<ITcpPairingService, TcpPairingService>();
builder.Services.AddSingleton<IDeviceStatusCacheService, DeviceStatusCacheService>();

// NOTE: UDP broadcasting and TCP pairing are NOT auto-started.
// They should be triggered on-demand via UI when the teacher starts a pairing/classroom session.
// The services are registered as singletons above and can be injected where needed.
// builder.Services.AddHostedService<UdpBroadcastHostedService>();
// builder.Services.AddHostedService<TcpPairingHostedService>();
builder.Services.AddHostedService<HubEventBridge>();

// NOTE: Controllers are enabled so that REST controllers can be added later.
builder.Services.AddControllers();
builder.Services.AddSignalR()
    .AddJsonProtocol(options =>
    {
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

// Expose Program class for WebApplicationFactory integration testing
public partial class Program { }
