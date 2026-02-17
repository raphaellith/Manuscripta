using Microsoft.AspNetCore.Hosting;
using Microsoft.AspNetCore.Mvc.Testing;
using Microsoft.EntityFrameworkCore;
using Microsoft.Extensions.DependencyInjection;
using Main.Data;

namespace MainTests;

/// <summary>
/// Custom WebApplicationFactory for integration tests.
/// Uses a unique in-memory SQLite database per instance to prevent test conflicts.
/// </summary>
public class TestWebApplicationFactory : WebApplicationFactory<Program>
{
    private readonly string _databaseName = $"TestDb_{Guid.NewGuid()}";

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
