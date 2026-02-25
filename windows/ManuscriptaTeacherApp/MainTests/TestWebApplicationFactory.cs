using Microsoft.AspNetCore.Hosting;
using Microsoft.AspNetCore.Mvc.Testing;
using Microsoft.EntityFrameworkCore;
using Microsoft.EntityFrameworkCore.Diagnostics;
using Microsoft.Extensions.Hosting;
using Microsoft.Extensions.DependencyInjection;
using Microsoft.Data.Sqlite;
using Main.Data;

namespace MainTests;

/// <summary>
/// Custom WebApplicationFactory for integration tests.
/// Uses a unique in-memory SQLite database per instance to prevent test conflicts.
/// </summary>
public class TestWebApplicationFactory : WebApplicationFactory<Program>
{
    private readonly string _databaseName = $"TestDb_{Guid.NewGuid()}";
    private readonly SqliteConnection _keepAliveConnection;

    public TestWebApplicationFactory()
    {
        // Disable ChromaDB auto-start for tests
        // Use the __ separator convention so .NET configuration maps this
        // to the "ChromaDB:AutoStart" key read by Program.cs.
        Environment.SetEnvironmentVariable("ChromaDB__AutoStart", "false");
        
        // Set the environment to Testing before the host is built
        Environment.SetEnvironmentVariable("ASPNETCORE_ENVIRONMENT", "Testing");

        _keepAliveConnection = new SqliteConnection($"Data Source={_databaseName};Mode=Memory;Cache=Shared");
        _keepAliveConnection.Open();
    }

    protected override void ConfigureWebHost(IWebHostBuilder builder)
    {
        builder.ConfigureServices(services =>
        {
            // Remove the existing DbContext registration
            var descriptor = services.SingleOrDefault(
                d => d.ServiceType == typeof(DbContextOptions<MainDbContext>));
            if (descriptor != null)
            {
                services.Remove(descriptor);
            }

            // Remove the DbContext itself
            var dbContextDescriptor = services.SingleOrDefault(
                d => d.ServiceType == typeof(MainDbContext));
            if (dbContextDescriptor != null)
            {
                services.Remove(dbContextDescriptor);
            }

            // Add a unique in-memory SQLite database for this test
            // Configure to suppress PendingModelChangesWarning since we're using an in-memory database
            services.AddDbContext<MainDbContext>(options =>
            {
                options.UseSqlite($"Data Source={_databaseName};Mode=Memory;Cache=Shared");
                options.ConfigureWarnings(w => 
                    w.Ignore(RelationalEventId.PendingModelChangesWarning));
            });

            // Ensure the database is created
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
