using ChromaDB.Client;
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
    private readonly IChromaClient _chromaClient;
    private const int ChunkSizeTokens = 512;
    private const int ChunkOverlapTokens = 64;
    private const int MaxEmbeddingRetries = 3;

    public DocumentEmbeddingService(OllamaClientService ollamaClient, IChromaClient chromaClient)
    {
        _ollamaClient = ollamaClient;
        _chromaClient = chromaClient;
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
                
                foreach (var (chunk, index) in chunks.Select((c, i) => (c, i)))
                {
                    var embedding = await _ollamaClient.GenerateEmbeddingAsync(chunk);
                    
                    var metadata = new Dictionary<string, object>
                    {
                        { "SourceDocumentId", document.Id.ToString() },
                        { "UnitCollectionId", document.UnitCollectionId.ToString() },
                        { "ChunkIndex", index }
                    };

                    await collection.AddAsync(
                        ids: new[] { $"{document.Id}_{index}" },
                        embeddings: new[] { embedding },
                        metadatas: new[] { metadata },
                        documents: new[] { chunk }
                    );
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
            
            // Query for all chunks belonging to this document
            var where = new Dictionary<string, object>
            {
                { "SourceDocumentId", sourceDocumentId.ToString() }
            };

            // Delete all matching chunks
            await collection.DeleteAsync(where: where);
        }
        catch (Exception ex)
        {
            throw new InvalidOperationException($"Failed to remove source document {sourceDocumentId} from vector store", ex);
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
    private async Task<IChromaCollection> GetOrCreateCollectionAsync()
    {
        try
        {
            return await _chromaClient.GetCollectionAsync("source_documents");
        }
        catch
        {
            return await _chromaClient.CreateCollectionAsync("source_documents");
        }
    }
}
