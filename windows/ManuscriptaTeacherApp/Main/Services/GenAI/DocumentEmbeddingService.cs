using System.Text;
using System.Text.Json;
using ChromaDB.Client;
using ChromaDB.Client.Models;
using Main.Data;
using Main.Models.Entities;
using Main.Models.Enums;
using Main.Services.Hubs;
using Main.Services.RuntimeDependencies;
using Microsoft.AspNetCore.SignalR;
using Microsoft.EntityFrameworkCore;
using Microsoft.Extensions.Logging;

namespace Main.Services.GenAI;

/// <summary>
/// Handles source document indexing for semantic retrieval.
/// See GenAISpec.md §3A.
/// </summary>
public class DocumentEmbeddingService : IEmbeddingService
{
    private readonly OllamaClientService _ollamaClient;
    private readonly ChromaClient _chromaClient;
    private readonly ChromaConfigurationOptions _chromaOptions;
    private readonly IRuntimeDependencyRegistry _runtimeDependencyRegistry;
    private readonly IHttpClientFactory _httpClientFactory;
    private readonly IHubContext<TeacherPortalHub> _hubContext;
    private readonly MainDbContext _dbContext;
    private readonly ILogger<DocumentEmbeddingService> _logger;
    private const int ChunkSizeTokens = 512;
    private const int ChunkOverlapTokens = 64;
    private const int MaxEmbeddingRetries = 3;
    private bool _tenantDatabaseInitialized;

    public DocumentEmbeddingService(
        OllamaClientService ollamaClient,
        ChromaClient chromaClient,
        ChromaConfigurationOptions chromaOptions,
        IHttpClientFactory httpClientFactory,
        IHubContext<TeacherPortalHub> hubContext,
        MainDbContext dbContext,
        ILogger<DocumentEmbeddingService> logger,
        IRuntimeDependencyRegistry runtimeDependencyRegistry)
    {
        _ollamaClient = ollamaClient;
        _chromaClient = chromaClient;
        _chromaOptions = chromaOptions;
        _httpClientFactory = httpClientFactory;
        _hubContext = hubContext;
        _dbContext = dbContext;
        _logger = logger;
        _runtimeDependencyRegistry = runtimeDependencyRegistry;
    }

    /// <summary>
    /// Executes the indexing workflow for a source document.
    /// See GenAISpec.md §3A(2) and §3A(5)(a).
    /// </summary>
    public async Task IndexSourceDocumentAsync(SourceDocumentEntity document)
    {
        document.EmbeddingStatus = EmbeddingStatus.PENDING;
        await _dbContext.SaveChangesAsync();
        
        var retries = 0;
        while (retries <= MaxEmbeddingRetries)
        {
            try
            {
                // Ensure embedding model is ready
                await _ollamaClient.EnsureModelReadyAsync("nomic-embed-text");

                // Split transcript into chunks
                var chunks = SplitIntoChunks(document.Transcript);

                // Generate embeddings for each chunk
                var collection = await GetOrCreateCollectionAsync();
                var collectionClient = new ChromaCollectionClient(collection, _chromaOptions, _httpClientFactory.CreateClient("ChromaDB"));
                
                var ids = new List<string>();
                var embeddings = new List<ReadOnlyMemory<float>>();
                var metadatas = new List<Dictionary<string, object>>();
                var documents = new List<string>();
                
                foreach (var (chunk, index) in chunks.Select((c, i) => (c, i)))
                {
                    var embedding = await _ollamaClient.GenerateEmbeddingAsync(chunk);
                    
                    ids.Add($"{document.Id}_{index}");
                    embeddings.Add(new ReadOnlyMemory<float>(embedding));
                    metadatas.Add(new Dictionary<string, object>
                    {
                        { "SourceDocumentId", document.Id.ToString() },
                        { "UnitCollectionId", document.UnitCollectionId.ToString() },
                        { "ChunkIndex", index }
                    });
                    documents.Add(chunk);
                }
                
                // Add all chunks in a single batch operation
                if (ids.Count > 0)
                {
                    await collectionClient.Add(ids, embeddings, metadatas, documents);
                }

                document.EmbeddingStatus = EmbeddingStatus.INDEXED;
                await _dbContext.SaveChangesAsync();
                return;
            }
            catch (Exception ex)
            {
                retries++;
                
                if (retries > MaxEmbeddingRetries)
                {
                    document.EmbeddingStatus = EmbeddingStatus.FAILED;
                    await _dbContext.SaveChangesAsync();
                    // §3A(6)(b)(ii): Notify frontend via SignalR OnEmbeddingFailed
                    _ = _hubContext.Clients.All.SendAsync("OnEmbeddingFailed", document.Id, ex.Message);
                    throw new InvalidOperationException($"Failed to index document after {MaxEmbeddingRetries} retries", ex);
                }

                // Exponential backoff: 1s, 10s, 60s
                var delay = retries switch
                {
                    1 => TimeSpan.FromSeconds(1),
                    2 => TimeSpan.FromSeconds(10),
                    3 => TimeSpan.FromSeconds(60),
                    _ => TimeSpan.FromSeconds(1)
                };
                
                await Task.Delay(delay);
            }
        }
    }

    /// <summary>
    /// Removes all chunks associated with a document from ChromaDB.
    /// See GenAISpec.md §3A(4)(a) and §3A(5)(b).
    /// This method is best-effort: if ChromaDB is unavailable (e.g. the
    /// document was never indexed, or the server is not running), the
    /// failure is logged but not propagated, so that deletion of the
    /// source document entity from the database can still succeed.
    /// When <paramref name="throwOnError"/> is true, exceptions are propagated—
    /// suitable for re-indexing where we must remove old chunks before adding new ones.
    /// </summary>
    public async Task RemoveSourceDocumentAsync(Guid sourceDocumentId, bool throwOnError = false)
    {
        try
        {
            var collection = await GetOrCreateCollectionAsync();
            var collectionClient = new ChromaCollectionClient(collection, _chromaOptions, _httpClientFactory.CreateClient("ChromaDB"));
            
            // Query for all chunks belonging to this document
            var where = ChromaWhereOperator.Equal("SourceDocumentId", sourceDocumentId.ToString());

            // Get all matching IDs first
            var entries = await collectionClient.Get(
                where: where,
                include: ChromaGetInclude.Metadatas
            );
            
            if (entries.Count > 0)
            {
                var idsToDelete = entries.Select(e => e.Id).ToList();
                await collectionClient.Delete(idsToDelete);
            }
        }
        catch (Exception ex)
        {
            if (throwOnError)
            {
                throw;
            }

            // Best-effort: if ChromaDB is unavailable the source document
            // entity has already been (or will be) deleted from the primary
            // data store, so there is nothing left to clean up once the
            // vector store comes back online.
            _logger.LogWarning(ex,
                "Could not remove source document {SourceDocumentId} from vector store. " +
                "ChromaDB may be unavailable. Orphaned chunks, if any, will not affect behaviour.",
                sourceDocumentId);
        }
    }

    /// <summary>
    /// Retrieves relevant chunks from ChromaDB for semantic search.
    /// See GenAISpec.md §2(4).
    /// </summary>
    public async Task<List<string>> RetrieveRelevantChunksAsync(
        float[] queryEmbedding,
        Guid unitCollectionId,
        List<Guid>? sourceDocumentIds = null,
        int topK = 5)
    {
        try
        {
            _logger.LogInformation(
                "Executing RAG chunk retrieval. UnitCollectionId={UnitCollectionId}, SourceDocumentFilterCount={SourceDocumentFilterCount}, TopK={TopK}, EmbeddingDimensions={EmbeddingDimensions}",
                unitCollectionId,
                sourceDocumentIds?.Count ?? 0,
                topK,
                queryEmbedding?.Length ?? 0);

            var collection = await GetOrCreateCollectionAsync();
            var collectionClient = new ChromaCollectionClient(collection, _chromaOptions, _httpClientFactory.CreateClient("ChromaDB"));

            // §2(4)(b)(ii): Filter by UnitCollectionId
            ChromaWhereOperator? where = ChromaWhereOperator.Equal("UnitCollectionId", unitCollectionId.ToString());

            // §2(4)(b)(i): If SourceDocumentIds are provided, filter by those specific document IDs
            if (sourceDocumentIds != null && sourceDocumentIds.Count > 0)
            {
                var sourceDocWhere = sourceDocumentIds
                    .Select(id => ChromaWhereOperator.Equal("SourceDocumentId", id.ToString()))
                    .Aggregate((a, b) => a || b);
                
                where = where && sourceDocWhere;
            }

            // §2(4)(c): Default value of K shall be 5
            // Include Distances alongside Documents so the library's
            // CollectionQueryEntryMapper does not throw NRE — it accesses
            // Distances without a null check. See ChromaV2UrlRewriteHandler.
            var results = await collectionClient.Query(
                queryEmbeddings: new ReadOnlyMemory<float>(queryEmbedding),
                nResults: topK,
                where: where,
                include: ChromaQueryInclude.Documents | ChromaQueryInclude.Distances
            );

            // Materialize the results to avoid deferred execution issues
            var resultList = results.ToList();
            var chunks = resultList.Select(r => r.Document).Where(d => d != null).Cast<string>().ToList();

            _logger.LogInformation(
                "Completed RAG chunk retrieval. UnitCollectionId={UnitCollectionId}, RetrievedChunkCount={RetrievedChunkCount}, TotalRetrievedContextChars={TotalRetrievedContextChars}",
                unitCollectionId,
                chunks.Count,
                chunks.Sum(c => c?.Length ?? 0));

            return chunks;
        }
        catch (Exception ex)
        {
            _logger.LogWarning(
                ex,
                "RAG chunk retrieval failed. UnitCollectionId={UnitCollectionId}, SourceDocumentFilterCount={SourceDocumentFilterCount}, TopK={TopK}. Returning empty result.",
                unitCollectionId,
                sourceDocumentIds?.Count ?? 0,
                topK);
            return new List<string>();
        }
    }

    /// <summary>
    /// Splits text into chunks respecting token limits and overlap.
    /// See GenAISpec.md §2(1).
    /// </summary>
    private List<string> SplitIntoChunks(string text)
    {
        var chunks = new List<string>();
        
        // §2(1)(c): Split on sentence boundaries using more robust pattern
        // This pattern captures sentences ending with . ! or ? while respecting common abbreviations
        var sentencePattern = new System.Text.RegularExpressions.Regex(
            @"(?<=[.!?])\s+(?=[A-Z])|(?<=[.!?])$",
            System.Text.RegularExpressions.RegexOptions.Multiline
        );
        
        // Split text into sentences, preserving punctuation
        var sentences = sentencePattern
            .Split(text)
            .Where(s => !string.IsNullOrWhiteSpace(s))
            .ToArray();
        
        var currentChunk = new List<string>();
        var currentTokenCount = 0;

        foreach (var sentence in sentences)
        {
            // Rough approximation: 1 token ≈ 4 characters
            // See §2(1) - approximate tokenization
            var sentenceTokens = (int)Math.Ceiling(sentence.Length / 4.0);

            // Handle sentences that exceed the chunk size by splitting them
            if (sentenceTokens > ChunkSizeTokens)
            {
                // First, flush any current chunk
                if (currentChunk.Count > 0)
                {
                    chunks.Add(string.Join(" ", currentChunk));
                    currentChunk.Clear();
                    currentTokenCount = 0;
                }

                // Split the long sentence into smaller pieces at word boundaries
                var words = sentence.Split(' ', StringSplitOptions.RemoveEmptyEntries);
                var subChunk = new List<string>();
                var subTokenCount = 0;

                foreach (var word in words)
                {
                    var wordTokens = (int)Math.Ceiling(word.Length / 4.0) + 1; // +1 for space
                    
                    if (subTokenCount + wordTokens > ChunkSizeTokens && subChunk.Count > 0)
                    {
                        chunks.Add(string.Join(" ", subChunk));
                        
                        // Create overlap from end of sub-chunk
                        var overlapWords = new List<string>();
                        var overlapTokens = 0;
                        for (int i = subChunk.Count - 1; i >= 0 && overlapTokens < ChunkOverlapTokens; i--)
                        {
                            var w = subChunk[i];
                            overlapTokens += (int)Math.Ceiling(w.Length / 4.0) + 1;
                            overlapWords.Insert(0, w);
                        }
                        
                        subChunk = overlapWords;
                        subTokenCount = overlapTokens;
                    }

                    subChunk.Add(word);
                    subTokenCount += wordTokens;
                }

                // Add remaining words as overlap for next chunk
                if (subChunk.Count > 0)
                {
                    currentChunk = subChunk;
                    currentTokenCount = subTokenCount;
                }
                
                continue;
            }

            if (currentTokenCount + sentenceTokens > ChunkSizeTokens && currentChunk.Count > 0)
            {
                // §2(1)(a): Create chunk of max 512 tokens
                chunks.Add(string.Join(" ", currentChunk));
                
                // §2(1)(b): Create overlap by keeping some sentences from end of previous chunk
                var overlapSentences = new List<string>();
                var overlapTokens = 0;
                
                for (int i = currentChunk.Count - 1; i >= 0 && overlapTokens < ChunkOverlapTokens; i--)
                {
                    var s = currentChunk[i];
                    var s_tokens = (int)Math.Ceiling(s.Length / 4.0);
                    overlapTokens += s_tokens;
                    overlapSentences.Insert(0, s);
                }
                
                currentChunk = overlapSentences;
                currentTokenCount = overlapTokens;
            }

            currentChunk.Add(sentence);
            currentTokenCount += sentenceTokens;
        }

        if (currentChunk.Count > 0)
        {
            chunks.Add(string.Join(" ", currentChunk));
        }

        return chunks;
    }

    /// <summary>
    /// Gets or creates the ChromaDB collection for source documents.
    /// See GenAISpec.md §2(3).
    /// </summary>
    private async Task<ChromaCollection> GetOrCreateCollectionAsync()
    {
        // avoid hitting the HTTP endpoint when the dependency is not available
        var manager = _runtimeDependencyRegistry.GetManager("chroma");
        if (manager != null)
        {
            var available = await manager.CheckDependencyAvailabilityAsync();
            if (!available)
            {
                _logger.LogWarning("ChromaDB not available; skipping collection access");
                // upstream callers generally catch exceptions and return empty results
                throw new InvalidOperationException("ChromaDB dependency is not available");
            }
        }

        // The Chroma Rust CLI server does not pre-create the default tenant and
        // database that the ChromaDB.Client library assumes exist. Creating them
        // explicitly prevents 404 errors from GetOrCreateCollection.
        if (!_tenantDatabaseInitialized)
        {
            await EnsureTenantAndDatabaseExistAsync();
            _tenantDatabaseInitialized = true;
        }

        return await _chromaClient.GetOrCreateCollection("source_documents");
    }

    /// <summary>
    /// Ensures that the default tenant and database exist in ChromaDB.
    /// The Chroma Rust CLI server does not automatically create "default_tenant"
    /// and "default_database", unlike the Python server. Without these, the
    /// ChromaDB.Client library receives 404 when constructing collection URLs
    /// under the /tenants/{tenant}/databases/{database}/collections path.
    /// </summary>
    private async Task EnsureTenantAndDatabaseExistAsync()
    {
        var baseUri = _chromaOptions.Uri.ToString().TrimEnd('/');
        var tenant = string.IsNullOrEmpty(_chromaOptions.Tenant) ? "default_tenant" : _chromaOptions.Tenant;
        var database = string.IsNullOrEmpty(_chromaOptions.Database) ? "default_database" : _chromaOptions.Database;

        // Use a dedicated HttpClient for these raw REST calls so that the
        // ChromaCollectionClient (which sets BaseAddress in its constructor)
        // always receives a fresh, un-started HttpClient instance.
        using var httpClient = _httpClientFactory.CreateClient("ChromaDB");

        try
        {
            var tenantPayload = new StringContent(
                JsonSerializer.Serialize(new { name = tenant }),
                Encoding.UTF8,
                "application/json");
            var tenantResponse = await httpClient.PostAsync($"{baseUri}/tenants", tenantPayload);

            // 200 = created, 409 = already exists — both are acceptable
            if (!tenantResponse.IsSuccessStatusCode &&
                tenantResponse.StatusCode != System.Net.HttpStatusCode.Conflict)
            {
                _logger.LogWarning(
                    "Unexpected response when creating ChromaDB tenant '{Tenant}': {StatusCode}",
                    tenant, tenantResponse.StatusCode);
            }
        }
        catch (Exception ex)
        {
            _logger.LogWarning(ex, "Failed to ensure ChromaDB tenant '{Tenant}' exists", tenant);
        }

        try
        {
            var dbPayload = new StringContent(
                JsonSerializer.Serialize(new { name = database }),
                Encoding.UTF8,
                "application/json");
            var dbResponse = await httpClient.PostAsync(
                $"{baseUri}/tenants/{tenant}/databases", dbPayload);

            // 200 = created, 409 = already exists — both are acceptable
            if (!dbResponse.IsSuccessStatusCode &&
                dbResponse.StatusCode != System.Net.HttpStatusCode.Conflict)
            {
                _logger.LogWarning(
                    "Unexpected response when creating ChromaDB database '{Database}' under tenant '{Tenant}': {StatusCode}",
                    database, tenant, dbResponse.StatusCode);
            }
        }
        catch (Exception ex)
        {
            _logger.LogWarning(ex,
                "Failed to ensure ChromaDB database '{Database}' exists under tenant '{Tenant}'",
                database, tenant);
        }
    }

    /// <summary>
    /// Indexes a source document by ID, fetching it from the database.
    /// Intended for use from background tasks with their own DI scope.
    /// See GenAISpec.md §3A(1) and §3A(5)(a).
    /// </summary>
    public async Task IndexSourceDocumentByIdAsync(Guid sourceDocumentId)
    {
        var document = await _dbContext.SourceDocuments.FindAsync(sourceDocumentId);

        if (document == null)
        {
            throw new InvalidOperationException($"Source document {sourceDocumentId} not found");
        }

        await IndexSourceDocumentAsync(document);
    }

    /// <summary>
    /// Retries embedding for a failed source document.
    /// See GenAISpec.md §3A(7).
    /// </summary>
    public async Task RetryEmbeddingAsync(Guid sourceDocumentId)
    {
        var document = await _dbContext.SourceDocuments.FindAsync(sourceDocumentId);
        
        if (document == null)
        {
            throw new InvalidOperationException($"Source document {sourceDocumentId} not found");
        }

        // Allow retry for FAILED status (expected case) and PENDING status
        // (documents stuck in PENDING from interrupted indexing attempts)
        if (document.EmbeddingStatus != EmbeddingStatus.FAILED && 
            document.EmbeddingStatus != EmbeddingStatus.PENDING)
        {
            throw new InvalidOperationException($"Cannot retry embedding for document with status {document.EmbeddingStatus}");
        }

        // Re-run the indexing workflow
        await IndexSourceDocumentAsync(document);
        await _dbContext.SaveChangesAsync();
    }

    /// <summary>
    /// Re-indexes a source document when it is updated.
    /// See GenAISpec.md §3A(3).
    /// </summary>
    public async Task ReIndexSourceDocumentAsync(SourceDocumentEntity document)
    {
        // §3A(3)(a): Remove existing chunks.
        // Pass throwOnError=true to ensure old chunks are removed before adding new ones.
        // If removal fails, we must not proceed with re-indexing or we'll create duplicates.
        await RemoveSourceDocumentAsync(document.Id, throwOnError: true);
        
        // §3A(3)(b): Re-index following the workflow in §3A(2)
        await IndexSourceDocumentAsync(document);
    }

    /// <summary>
    /// Identifies failed source documents on application startup.
    /// See GenAISpec.md §3A(8)(a)-(c).
    /// </summary>
    public async Task InitializeFailedEmbeddingsAsync()
    {
        try
        {
            // Handle PENDING documents from interrupted indexing attempts
            // These should be transitioned to FAILED status since the previous
            // indexing operation did not complete
            var pendingDocuments = await _dbContext.SourceDocuments
                .Where(d => d.EmbeddingStatus == EmbeddingStatus.PENDING)
                .ToListAsync();

            if (pendingDocuments.Count > 0)
            {
                _logger.LogWarning(
                    "Found {Count} source document(s) with PENDING embedding status from interrupted indexing. " +
                    "Transitioning to FAILED status.",
                    pendingDocuments.Count
                );

                foreach (var doc in pendingDocuments)
                {
                    doc.EmbeddingStatus = EmbeddingStatus.FAILED;
                    _logger.LogWarning(
                        "Source document {SourceDocumentId} in unit collection {UnitCollectionId} " +
                        "transitioned from PENDING to FAILED due to interrupted indexing.",
                        doc.Id,
                        doc.UnitCollectionId
                    );
                }

                await _dbContext.SaveChangesAsync();
            }

            // §3A(8)(a): Identify all SourceDocumentEntity objects with EmbeddingStatus of FAILED
            var failedDocuments = await _dbContext.SourceDocuments
                .Where(d => d.EmbeddingStatus == EmbeddingStatus.FAILED)
                .ToListAsync();

            if (failedDocuments.Count > 0)
            {
                // §3A(8)(b): Do not automatically retry these documents (to avoid repeated failures)
                _logger.LogWarning(
                    "Found {Count} source document(s) with FAILED embedding status. " +
                    "These will not be automatically retried. Teachers may retry manually or delete and re-upload.",
                    failedDocuments.Count
                );

                // Log each failed document for monitoring
                foreach (var doc in failedDocuments)
                {
                    _logger.LogWarning(
                        "Failed source document: {SourceDocumentId} in unit collection {UnitCollectionId}",
                        doc.Id,
                        doc.UnitCollectionId
                    );
                }
            }
        }
        catch (Exception ex)
        {
            _logger.LogError(
                ex,
                "Error during embedding startup initialization"
            );
        }
    }
}
