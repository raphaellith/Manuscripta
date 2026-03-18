using Microsoft.AspNetCore.Hosting;
using Microsoft.AspNetCore.Mvc.Testing;
using Microsoft.EntityFrameworkCore;
using Microsoft.EntityFrameworkCore.Diagnostics;
using Microsoft.Extensions.Hosting;
using Microsoft.Extensions.DependencyInjection;
using Microsoft.Data.Sqlite;
using Main.Data;

namespace AiEvaluationTests;

/// <summary>
/// Custom WebApplicationFactory for AI evaluations.
/// Uses a unique in-memory SQLite database per instance.
/// Actively ensures ChromaDB (and implicitly Ollama) are allowed to start.
/// </summary>
public class AiEvaluationWebApplicationFactory : WebApplicationFactory<Program>
{
    private readonly string _databaseName = $"TestDb_{Guid.NewGuid()}";
    private readonly SqliteConnection _keepAliveConnection;

    public AiEvaluationWebApplicationFactory()
    {
        Environment.SetEnvironmentVariable("ASPNETCORE_ENVIRONMENT", "Testing");
        Environment.SetEnvironmentVariable("ChromaDB__AutoStart", "true");

        _keepAliveConnection = new SqliteConnection($"Data Source={_databaseName};Mode=Memory;Cache=Shared");
        _keepAliveConnection.Open();
    }

    protected override void ConfigureWebHost(IWebHostBuilder builder)
    {
        builder.ConfigureServices(services =>
        {
            var descriptor = services.SingleOrDefault(d => d.ServiceType == typeof(DbContextOptions<MainDbContext>));
            if (descriptor != null) services.Remove(descriptor);

            var dbContextDescriptor = services.SingleOrDefault(d => d.ServiceType == typeof(MainDbContext));
            if (dbContextDescriptor != null) services.Remove(dbContextDescriptor);

            services.AddDbContext<MainDbContext>(options =>
            {
                options.UseSqlite($"Data Source={_databaseName};Mode=Memory;Cache=Shared");
                options.ConfigureWarnings(w => w.Ignore(RelationalEventId.PendingModelChangesWarning));
            });

            var sp = services.BuildServiceProvider();
            using var scope = sp.CreateScope();
            var db = scope.ServiceProvider.GetRequiredService<MainDbContext>();
            db.Database.EnsureCreated();
        });
    }

    protected override void Dispose(bool disposing)
    {
        if (disposing)
        {
            _keepAliveConnection.Dispose();
        }

        base.Dispose(disposing);
    }
}
