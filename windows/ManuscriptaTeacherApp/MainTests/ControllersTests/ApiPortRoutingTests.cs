using System.Net;
using System.Net.Http.Json;
using Xunit;

namespace MainTests.ControllersTests;

/// <summary>
/// Integration tests for host-based routing in non-Testing environments.
/// Verifies controllers are restricted to HTTP API port, while health and SignalR stay open.
/// </summary>
public class ApiPortRoutingTests : IClassFixture<NonTestingWebApplicationFactory>
{
    private readonly NonTestingWebApplicationFactory _factory;

    public ApiPortRoutingTests(NonTestingWebApplicationFactory factory)
    {
        _factory = factory;
    }

    [Fact]
    public async Task Controllers_Accessible_OnHttpPort_InNonTesting()
    {
        using var client = _factory.CreateClient();
        var request = new HttpRequestMessage(HttpMethod.Get, $"/api/v1/config/{Guid.NewGuid()}");
        request.Headers.Host = "localhost:5911";

        var response = await client.SendAsync(request);

        response.EnsureSuccessStatusCode();
    }

    [Fact]
    public async Task Controllers_ReturnNotFound_OnSignalRPort_InNonTesting()
    {
        using var client = _factory.CreateClient();
        var request = new HttpRequestMessage(HttpMethod.Get, $"/api/v1/config/{Guid.NewGuid()}");
        request.Headers.Host = "localhost:5910";

        var response = await client.SendAsync(request);

        // In-memory test server simulates port via Host header; expect 404 from RequireHost.
        Assert.Equal(HttpStatusCode.NotFound, response.StatusCode);
    }

    [Theory]
    [InlineData(5910)]
    [InlineData(5911)]
    [InlineData(5914)]
    public async Task HealthEndpoint_Accessible_OnAnyPort(int port)
    {
        using var client = _factory.CreateClient();
        var request = new HttpRequestMessage(HttpMethod.Get, "/");
        request.Headers.Host = $"localhost:{port}";

        var response = await client.SendAsync(request);

        response.EnsureSuccessStatusCode();
    }

    [Theory]
    [InlineData(5910)]
    [InlineData(5911)]
    [InlineData(5919)]
    public async Task SignalRHub_Negotiate_Accessible_OnAnyPort(int port)
    {
        using var client = _factory.CreateClient();
        var request = new HttpRequestMessage(HttpMethod.Post, "/TeacherPortalHub/negotiate?negotiateVersion=1");
        request.Headers.Host = $"localhost:{port}";
        request.Content = JsonContent.Create(new { });

        var response = await client.SendAsync(request);

        Assert.NotEqual(HttpStatusCode.NotFound, response.StatusCode);
    }
}
