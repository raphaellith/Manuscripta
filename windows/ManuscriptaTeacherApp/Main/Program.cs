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
builder.Services.AddScoped<IDeviceRegistryService, DeviceRegistryService>();
builder.Services.AddScoped<DeviceIdValidator>();

// Register repository services for hub
builder.Services.AddScoped<Main.Services.Repositories.IUnitCollectionRepository, Main.Services.Repositories.EfUnitCollectionRepository>();
builder.Services.AddScoped<Main.Services.Repositories.IUnitRepository, Main.Services.Repositories.EfUnitRepository>();
builder.Services.AddScoped<Main.Services.Repositories.ILessonRepository, Main.Services.Repositories.EfLessonRepository>();
builder.Services.AddScoped<Main.Services.Repositories.IMaterialRepository, Main.Services.Repositories.EfMaterialRepository>();

// Register CRUD services for hub
builder.Services.AddScoped<IUnitCollectionService, UnitCollectionService>();
builder.Services.AddScoped<IUnitService, UnitService>();
builder.Services.AddScoped<ILessonService, LessonService>();
builder.Services.AddScoped<IMaterialService, MaterialService>();

// Register network services (singletons for background services)
builder.Services.AddSingleton<IRefreshConfigTracker, RefreshConfigTracker>();
builder.Services.AddSingleton<IUdpBroadcastService, UdpBroadcastService>();
builder.Services.AddSingleton<ITcpPairingService, TcpPairingService>();

// Register background services for UDP broadcasting and TCP pairing
builder.Services.AddHostedService<UdpBroadcastHostedService>();
builder.Services.AddHostedService<TcpPairingHostedService>();

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
}

// Minimal default endpoint to confirm the app is running.
app.MapGet("/", () => Results.Ok("Manuscripta Main API (net10.0) is running"));

app.MapControllers();
app.MapHub<Main.Services.Hubs.TeacherPortalHub>("/hub");

app.Run();

// Expose Program class for WebApplicationFactory integration testing
public partial class Program { }
