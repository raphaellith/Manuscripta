using Microsoft.AspNetCore.Mvc.Testing;

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
    public CustomWebApplicationFactory()
    {
        // Use the __ separator convention so .NET configuration maps this
        // to the "ChromaDB:AutoStart" key read by Program.cs.
        Environment.SetEnvironmentVariable("ChromaDB__AutoStart", "false");
    }
}
