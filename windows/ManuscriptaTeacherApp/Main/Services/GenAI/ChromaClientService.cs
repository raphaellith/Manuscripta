using Main.Services.RuntimeDependencies;

namespace Main.Services.GenAI
{
    /// <summary>
    /// Provides access to ChromaDB vector store client.
    /// This service wraps interaction with ChromaDB for semantic document retrieval.
    /// Per GenAISpec.md §1B(3)(f) and BackendRuntimeDependencyManagementSpecification §2(3A).
    /// </summary>
    public class ChromaClientService : IDependencyService
    {
        // This service is a marker service that indicates ChromaDB is available.
        // The actual ChromaClient and ChromaConfigurationOptions are registered separately in the DI container
        // and injected into services that require vector store access (e.g., DocumentEmbeddingService).
        
        public ChromaClientService()
        {
        }
    }
}
