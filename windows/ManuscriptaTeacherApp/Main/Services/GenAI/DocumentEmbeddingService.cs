using ChromaDB.Client;
using ChromaDB.Client.Models;
using Main.Models.Entities;
using Main.Models.Enums;

namespace Main.Services.GenAI;

/// <summary>
/// Handles source document indexing for semantic retrieval.
/// See GenAISpec.md §3A.
/// </summary>
public class DocumentEmbeddingService
{
    private readonly OllamaClientService _ollamaClient;
    private readonly ChromaClient _chromaClient;
    private readonly ChromaConfigurationOptions _chromaOptions;
    private readonly HttpClient _httpClient;
    private const int ChunkSizeTokens = 512;
    private const int ChunkOverlapTokens = 64;
    private const int MaxEmbeddingRetries = 3;

    public DocumentEmbeddingService(
        OllamaClientService ollamaClient,
        ChromaClient chromaClient,
        ChromaConfigurationOptions chromaOptions,
        HttpClient httpClient)
    {
        _ollamaClient = ollamaClient;
        _chromaClient = chromaClient;
        _chromaOptions = chromaOptions;
        _httpClient = httpClient;
    }

    /// <summary>
    /// Executes the indexing workflow for a source document.
    /// See GenAISpec.md §3A(2) and §3A(5)(a).
    /// </summary>
    public async Task IndexSourceDocumentAsync(SourceDocumentEntity document)
    {
        document.EmbeddingStatus = EmbeddingStatus.PENDING;
        
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
                var collectionClient = new ChromaCollectionClient(collection, _chromaOptions, _httpClient);
                
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
                return;
            }
            catch (Exception ex)
            {
                retries++;
                
                if (retries > MaxEmbeddingRetries)
                {
                    document.EmbeddingStatus = EmbeddingStatus.FAILED;
                    // TODO: Notify frontend via SignalR OnEmbeddingFailed
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
    /// </summary>
    public async Task RemoveSourceDocumentAsync(Guid sourceDocumentId)
    {
        try
        {
            var collection = await GetOrCreateCollectionAsync();
            var collectionClient = new ChromaCollectionClient(collection, _chromaOptions, _httpClient);
            
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
            throw new InvalidOperationException($"Failed to remove source document {sourceDocumentId} from vector store", ex);
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
            var collection = await GetOrCreateCollectionAsync();
            var collectionClient = new ChromaCollectionClient(collection, _chromaOptions, _httpClient);

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
            var results = await collectionClient.Query(
                queryEmbeddings: new ReadOnlyMemory<float>(queryEmbedding),
                nResults: topK,
                where: where,
                include: ChromaQueryInclude.Documents
            );

            return results.Select(r => r.Document).Where(d => d != null).Cast<string>().ToList();
        }
        catch
        {
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
        
        // Simple sentence-based splitting (approximate tokenization)
        // In production, use a proper tokenizer
        var sentences = text.Split(new[] { ". ", "! ", "? " }, StringSplitOptions.RemoveEmptyEntries);
        
        var currentChunk = new List<string>();
        var currentTokenCount = 0;

        foreach (var sentence in sentences)
        {
            // Rough approximation: 1 token ≈ 4 characters
            var sentenceTokens = sentence.Length / 4;

            if (currentTokenCount + sentenceTokens > ChunkSizeTokens && currentChunk.Count > 0)
            {
                chunks.Add(string.Join(". ", currentChunk) + ".");
                
                // Create overlap by keeping some sentences
                var overlapSentences = new List<string>();
                var overlapTokens = 0;
                
                for (int i = currentChunk.Count - 1; i >= 0 && overlapTokens < ChunkOverlapTokens; i--)
                {
                    var s = currentChunk[i];
                    overlapTokens += s.Length / 4;
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
            chunks.Add(string.Join(". ", currentChunk) + ".");
        }

        return chunks;
    }

    /// <summary>
    /// Gets or creates the ChromaDB collection for source documents.
    /// See GenAISpec.md §2(3).
    /// </summary>
    private async Task<ChromaCollection> GetOrCreateCollectionAsync()
    {
        return await _chromaClient.GetOrCreateCollection("source_documents");
    }
}
