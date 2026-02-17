using Microsoft.AspNetCore.Mvc.Testing;
using Microsoft.AspNetCore.Hosting;
using Microsoft.EntityFrameworkCore;
using Microsoft.Extensions.DependencyInjection;
using Main.Data;

namespace MainTests;

/// <summary>
/// Custom WebApplicationFactory that disables ChromaDB auto-start for integration tests.
/// This prevents the test host from trying to launch the 'chroma' process,
/// which may not be available in CI/test environments.
///
/// The environment variable is set in the constructor so it is available when
/// Program.Main reads configuration via WebApplication.CreateBuilder(args),
/// which happens before ConfigureWebHost callbacks run.
/// </summary>
public class CustomWebApplicationFactory : WebApplicationFactory<Program>
{
    private readonly string _databaseName = $"TestDb_{Guid.NewGuid()}";

    public CustomWebApplicationFactory()
    {
        // Use the __ separator convention so .NET configuration maps this
        // to the "ChromaDB:AutoStart" key read by Program.cs.
        Environment.SetEnvironmentVariable("ChromaDB__AutoStart", "false");
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
            services.AddDbContext<MainDbContext>(options =>
            {
                options.UseSqlite($"Data Source={_databaseName};Mode=Memory;Cache=Shared");
            });

            // Ensure the database is created
            var sp = services.BuildServiceProvider();
            using var scope = sp.CreateScope();
            var db = scope.ServiceProvider.GetRequiredService<MainDbContext>();
            
            try
            {
                // Clear any existing schema to avoid conflicts with migrations
                // (especially important for in-memory SQLite databases in tests)
                db.Database.EnsureDeleted();
            }
            catch
            {
                // In-memory database may not support EnsureDeleted, continue anyway
            }
            
            try
            {
                db.Database.EnsureCreated();
            }
            catch (Exception ex) when (ex.InnerException != null && 
                                       ex.InnerException.Message.Contains("already exists"))
            {
                // If tables already exist (can happen with in-memory SQLite and shared cache),
                // continue without re-creating them. The migrations have already been applied.
                System.Diagnostics.Debug.WriteLine($"Database tables already exist: {ex.InnerException.Message}");
            }
        });

        builder.UseEnvironment("Testing");
    }
}
