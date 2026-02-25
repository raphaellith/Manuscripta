using Microsoft.AspNetCore.Hosting;
using Microsoft.AspNetCore.Mvc.Testing;
using Microsoft.Data.Sqlite;
using Microsoft.EntityFrameworkCore;
using Microsoft.Extensions.DependencyInjection;
using Main.Data;

namespace MainTests;

/// <summary>
/// WebApplicationFactory for non-Testing environment integration tests.
/// Uses in-memory SQLite but keeps host constraints active.
/// </summary>
public class NonTestingWebApplicationFactory : WebApplicationFactory<Program>
{
    private readonly SqliteConnection _connection = new("Data Source=:memory:");

    public NonTestingWebApplicationFactory()
    {
        // Disable ChromaDB auto-start for tests
        // Use the __ separator convention so .NET configuration maps this
        // to the "ChromaDB:AutoStart" key read by Program.cs.
        Environment.SetEnvironmentVariable("ChromaDB__AutoStart", "false");
        _connection.Open();
    }

    protected override void ConfigureWebHost(IWebHostBuilder builder)
    {
        builder.ConfigureServices(services =>
        {
            var descriptor = services.SingleOrDefault(
                d => d.ServiceType == typeof(DbContextOptions<MainDbContext>));
            if (descriptor != null)
            {
                services.Remove(descriptor);
            }

            var dbContextDescriptor = services.SingleOrDefault(
                d => d.ServiceType == typeof(MainDbContext));
            if (dbContextDescriptor != null)
            {
                services.Remove(dbContextDescriptor);
            }

            services.AddDbContext<MainDbContext>(options => options.UseSqlite(_connection));

            // Program.cs runs migrations in non-Testing environments; avoid EnsureCreated to prevent conflicts.
        });

        builder.UseEnvironment("Development");
    }

    protected override void Dispose(bool disposing)
    {
        if (disposing)
        {
            _connection.Dispose();
        }

        base.Dispose(disposing);
    }
}
