using Microsoft.Extensions.Hosting;
using Main.Services.GenAI;

namespace Main.Services;

/// <summary>
/// Hosted service that initializes failed embeddings after application startup.
/// Per GenAISpec.md §3A(8).
/// 
/// This service runs the embedding initialization asynchronously in the background
/// after the application has started, preventing startup blocking and ensuring
/// the health endpoint responds quickly to frontend health checks.
/// Per FrontendWorkflowSpecifications §2ZA(5)(a)-(d).
/// </summary>
public class EmbeddingInitializationHostedService : BackgroundService
{
    private readonly IServiceScopeFactory _serviceScopeFactory;
    private readonly ILogger<EmbeddingInitializationHostedService> _logger;

    public EmbeddingInitializationHostedService(
        IServiceScopeFactory serviceScopeFactory,
        ILogger<EmbeddingInitializationHostedService> logger)
    {
        _serviceScopeFactory = serviceScopeFactory ?? throw new ArgumentNullException(nameof(serviceScopeFactory));
        _logger = logger ?? throw new ArgumentNullException(nameof(logger));
    }

    /// <summary>
    /// Executes the background task that initializes failed embeddings.
    /// This runs after the application has started and the HTTP server is listening.
    /// </summary>
    protected override async Task ExecuteAsync(CancellationToken stoppingToken)
    {
        try
        {
            _logger.LogInformation("Embedding initialization service starting...");
            
            // Create a scope to resolve the scoped IEmbeddingService
            // Singletons cannot directly depend on scoped services, so we create a scope
            // for this background operation per Microsoft DI best practices
            using (var scope = _serviceScopeFactory.CreateScope())
            {
                var embeddingService = scope.ServiceProvider.GetRequiredService<IEmbeddingService>();
                await embeddingService.InitializeFailedEmbeddingsAsync();
            }
            
            _logger.LogInformation("Embedding initialization service completed successfully.");
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error during background embedding initialization");
            // Don't throw - this is a background task and shouldn't terminate the application
        }
    }
}
