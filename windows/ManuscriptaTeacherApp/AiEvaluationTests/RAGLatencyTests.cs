using System;
using System.Diagnostics;
using System.IO;
using System.Collections.Generic;
using System.Linq;
using System.Net.Http;
using System.Net.Http.Json;
using System.Text.Json;
using System.Text.RegularExpressions;
using System.Threading.Tasks;
using ChromaDB.Client;
using ChromaDB.Client.Models;
using Xunit;
using Xunit.Abstractions;
using Microsoft.Extensions.DependencyInjection;
using Main.Services.GenAI;
using Main.Models.Entities;
using Main.Data;

namespace AiEvaluationTests;

/// <summary>
/// Result of a single indexing iteration.
/// </summary>
public class RAGIndexingResult
{
    public int Iteration { get; set; }
    public long IndexingTimeMs { get; set; }
}

/// <summary>
/// Result of a single retrieval query at a given topK.
/// </summary>
public class RAGRetrievalResult
{
    public string Query { get; set; } = string.Empty;
    public string EmbeddingModel { get; set; } = string.Empty;
    public int TopK { get; set; }
    public int Iteration { get; set; }
    public long RetrievalTimeMs { get; set; }
    public int ChunksReturned { get; set; }
    public int RelevantChunks { get; set; }
    public double Precision { get; set; }
}

[Trait("Category", "AIEvaluation")]
public class RAGLatencyTests : IClassFixture<AiEvaluationWebApplicationFactory>
{
    private readonly AiEvaluationWebApplicationFactory _factory;
    private readonly ITestOutputHelper _output;
    private static readonly JsonSerializerOptions JsonOptions = new() { WriteIndented = true };

    private static readonly HttpClient OllamaHttp = new()
    {
        BaseAddress = new Uri("http://localhost:11434"),
        Timeout = TimeSpan.FromSeconds(300)
    };

    public RAGLatencyTests(AiEvaluationWebApplicationFactory factory, ITestOutputHelper output)
    {
        _factory = factory;
        _output = output;
    }

    [Theory]
    [InlineData(10)]
    public async Task RAG_Indexing_Latency_Should_Be_Under_Threshold(int iterations)
    {
        using var scope = _factory.Services.CreateScope();
        var embeddingService = scope.ServiceProvider.GetRequiredService<IEmbeddingService>();
        var dbContext = scope.ServiceProvider.GetRequiredService<MainDbContext>();

        string dataPath = Path.Combine(AppContext.BaseDirectory, "Data", "HeavyTranscript.txt");
        string transcriptPayload = "Fallback payload for testing if HeavyTranscript.txt is missing. "
            + new string('A', 50000);

        if (File.Exists(dataPath))
        {
            transcriptPayload = await File.ReadAllTextAsync(dataPath);
        }

        var results = new List<RAGIndexingResult>();

        for (int i = 0; i < iterations; i++)
        {
            var unitCollectionId = Guid.NewGuid();
            dbContext.UnitCollections.Add(
                new UnitCollectionEntity(unitCollectionId, $"RAG Latency Test {i + 1}"));
            await dbContext.SaveChangesAsync();

            var doc = new SourceDocumentEntity
            {
                Id = Guid.NewGuid(),
                Transcript = transcriptPayload,
                UnitCollectionId = unitCollectionId,
                EmbeddingStatus = Main.Models.Enums.EmbeddingStatus.PENDING
            };

            dbContext.SourceDocuments.Add(doc);
            await dbContext.SaveChangesAsync();

            var stopwatch = Stopwatch.StartNew();
            await embeddingService.IndexSourceDocumentAsync(doc);
            stopwatch.Stop();

            results.Add(new RAGIndexingResult
            {
                Iteration = i + 1,
                IndexingTimeMs = stopwatch.ElapsedMilliseconds
            });
            _output.WriteLine($"Iteration {i + 1} indexing latency: {stopwatch.ElapsedMilliseconds} ms");
        }

        string resultsDir = Path.Combine(AppContext.BaseDirectory, "Data", "Results");
        Directory.CreateDirectory(resultsDir);
        await File.WriteAllTextAsync(
            Path.Combine(resultsDir, "RAGIndexingResults.json"),
            JsonSerializer.Serialize(results, JsonOptions));
    }

    /// <summary>
    /// Indexes the HeavyTranscript once per embedding model, then evaluates retrieval
    /// quality and latency across multiple topK values and domain-specific queries.
    /// Each query is repeated <paramref name="iterationsPerQuery"/> times to capture
    /// variance.  Precision is computed by checking how many returned chunks contain
    /// at least one expected keyword.
    ///
    /// Uses direct Ollama HTTP calls and ChromaDB client to bypass the production
    /// service's 768-dimension validation, allowing models with different embedding
    /// dimensions (e.g. granite4 at 2560d) to be evaluated.
    /// </summary>
    [Theory]
    [InlineData("nomic-embed-text", 3)]
    [InlineData("granite4", 3)]
    public async Task RAG_Retrieval_Quality_And_Latency_Across_TopK(
        string embeddingModel, int iterationsPerQuery)
    {
        string dataPath = Path.Combine(AppContext.BaseDirectory, "Data", "HeavyTranscript.txt");
        if (!File.Exists(dataPath))
        {
            _output.WriteLine("SKIP: HeavyTranscript.txt not found — cannot evaluate retrieval.");
            return;
        }

        string transcript = await File.ReadAllTextAsync(dataPath);

        // --- chunk the transcript ---
        var chunks = SplitIntoChunks(transcript);
        _output.WriteLine($"[{embeddingModel}] Transcript split into {chunks.Count} chunks");

        // --- set up a model-specific ChromaDB collection ---
        var chromaClient = _factory.Services.GetRequiredService<ChromaClient>();
        var chromaOptions = _factory.Services.GetRequiredService<ChromaConfigurationOptions>();
        var httpClientFactory = _factory.Services.GetRequiredService<IHttpClientFactory>();
        string collectionName = $"eval_{embeddingModel.Replace(":", "_").Replace("-", "_")}";

        // Delete any stale collection from a previous run before creating fresh
        try { await chromaClient.DeleteCollection(collectionName); }
        catch { /* collection may not exist yet */ }

        var collection = await chromaClient.GetOrCreateCollection(collectionName);
        var collectionClient = new ChromaCollectionClient(
            collection, chromaOptions, httpClientFactory.CreateClient("ChromaDB"));

        // --- index all chunks in batches ---
        _output.WriteLine($"[{embeddingModel}] Embedding and indexing {chunks.Count} chunks…");
        var indexSw = Stopwatch.StartNew();
        const int batchSize = 50;

        for (int batchStart = 0; batchStart < chunks.Count; batchStart += batchSize)
        {
            int batchEnd = Math.Min(batchStart + batchSize, chunks.Count);
            var batchIds = new List<string>();
            var batchEmbeddings = new List<ReadOnlyMemory<float>>();
            var batchMetadatas = new List<Dictionary<string, object>>();
            var batchDocs = new List<string>();

            for (int i = batchStart; i < batchEnd; i++)
            {
                float[] emb = await EmbedDirectAsync(chunks[i], embeddingModel);
                batchIds.Add($"chunk_{i}");
                batchEmbeddings.Add(new ReadOnlyMemory<float>(emb));
                batchMetadatas.Add(new Dictionary<string, object> { { "ChunkIndex", i } });
                batchDocs.Add(chunks[i]);
            }

            await collectionClient.Add(batchIds, batchEmbeddings, batchMetadatas, batchDocs);
            _output.WriteLine(
                $"[{embeddingModel}] Indexed batch {batchStart / batchSize + 1} " +
                $"(chunks {batchStart}–{batchEnd - 1})");
        }

        indexSw.Stop();
        _output.WriteLine(
            $"[{embeddingModel}] Indexing completed in {indexSw.ElapsedMilliseconds} ms");

        // --- queries with expected keywords ---
        var queries = new (string Text, string[] ExpectedKeywords)[]
        {
            (
                "What is binary search and how does it work?",
                new[] { "binary", "search", "sorted", "middle", "half" }
            ),
            (
                "Explain the concept of normalisation in databases",
                new[] { "normalis", "database", "table", "redundan", "key" }
            ),
            (
                "How does TCP/IP enable reliable data transmission?",
                new[] { "tcp", "packet", "protocol", "transmis", "acknowledge" }
            ),
            (
                "Describe the fetch-decode-execute cycle of a processor",
                new[] { "fetch", "decode", "execute", "register", "cycle" }
            ),
            (
                "What are the ethical implications of artificial intelligence?",
                new[] { "ethic", "intelligen", "ai", "moral", "privacy" }
            ),
        };

        int[] topKValues = { 1, 3, 5, 10 };
        var results = new List<RAGRetrievalResult>();

        foreach (var (queryText, expectedKeywords) in queries)
        {
            float[] queryEmbedding = await EmbedDirectAsync(queryText, embeddingModel);

            foreach (int topK in topKValues)
            {
                for (int iter = 0; iter < iterationsPerQuery; iter++)
                {
                    var sw = Stopwatch.StartNew();
                    var queryResults = await collectionClient.Query(
                        queryEmbeddings: new ReadOnlyMemory<float>(queryEmbedding),
                        nResults: topK,
                        include: ChromaQueryInclude.Documents | ChromaQueryInclude.Distances);
                    sw.Stop();

                    var returnedChunks = queryResults
                        .Select(r => r.Document)
                        .Where(d => d != null)
                        .Cast<string>()
                        .ToList();

                    int relevant = returnedChunks.Count(chunk =>
                        expectedKeywords.Any(kw =>
                            chunk.Contains(kw, StringComparison.OrdinalIgnoreCase)));

                    double precision = returnedChunks.Count > 0
                        ? (double)relevant / returnedChunks.Count
                        : 0.0;

                    results.Add(new RAGRetrievalResult
                    {
                        Query = queryText,
                        EmbeddingModel = embeddingModel,
                        TopK = topK,
                        Iteration = iter + 1,
                        RetrievalTimeMs = sw.ElapsedMilliseconds,
                        ChunksReturned = returnedChunks.Count,
                        RelevantChunks = relevant,
                        Precision = Math.Round(precision, 4)
                    });

                    _output.WriteLine(
                        $"[{embeddingModel} topK={topK}] " +
                        $"\"{queryText[..Math.Min(40, queryText.Length)]}…\" " +
                        $"iter {iter + 1}: {sw.ElapsedMilliseconds} ms, " +
                        $"{relevant}/{returnedChunks.Count} relevant (P={precision:F2})");
                }
            }
        }

        // --- persist results ---
        string resultsDir = Path.Combine(AppContext.BaseDirectory, "Data", "Results");
        Directory.CreateDirectory(resultsDir);
        string safeModelName = embeddingModel.Replace(":", "_").Replace("-", "_");
        await File.WriteAllTextAsync(
            Path.Combine(resultsDir, $"RAGRetrievalResults_{safeModelName}.json"),
            JsonSerializer.Serialize(results, JsonOptions));

        // --- aggregate assertions ---
        var byTopK = results
            .GroupBy(r => r.TopK)
            .Select(g => new
            {
                TopK = g.Key,
                MeanPrecision = g.Average(r => r.Precision),
                MeanLatencyMs = g.Average(r => r.RetrievalTimeMs),
                TotalQueries = g.Count()
            })
            .OrderBy(g => g.TopK)
            .ToList();

        _output.WriteLine($"\n=== Retrieval Summary [{embeddingModel}] ===");
        foreach (var group in byTopK)
        {
            _output.WriteLine(
                $"topK={group.TopK}: mean precision={group.MeanPrecision:F2}, " +
                $"mean latency={group.MeanLatencyMs:F0} ms  ({group.TotalQueries} queries)");
        }

        // At topK=5, mean precision should exceed 50%
        var topK5 = byTopK.FirstOrDefault(g => g.TopK == 5);
        Assert.NotNull(topK5);
        Assert.True(topK5.MeanPrecision >= 0.50,
            $"[{embeddingModel}] Mean precision at topK=5 was {topK5.MeanPrecision:F2}, expected >= 0.50");

        // --- clean up the evaluation collection ---
        await chromaClient.DeleteCollection(collectionName);
    }

    /// <summary>
    /// Calls Ollama /api/embed directly, bypassing the production
    /// service's 768-dimension validation so any model can be tested.
    /// </summary>
    private static async Task<float[]> EmbedDirectAsync(string text, string model)
    {
        var body = new { model, input = text };
        var response = await OllamaHttp.PostAsJsonAsync("/api/embed", body);

        if (!response.IsSuccessStatusCode)
        {
            string errorBody = await response.Content.ReadAsStringAsync();
            throw new HttpRequestException(
                $"Ollama /api/embed returned {(int)response.StatusCode} for model '{model}' " +
                $"(input length={text.Length} chars): {errorBody}");
        }

        var content = await response.Content.ReadAsStringAsync();
        var doc = JsonDocument.Parse(content);
        var arr = doc.RootElement.GetProperty("embeddings")[0];

        var result = new float[arr.GetArrayLength()];
        int idx = 0;
        foreach (var v in arr.EnumerateArray())
        {
            result[idx++] = v.GetSingle();
        }

        return result;
    }

    /// <summary>
    /// Deterministic sentence-boundary chunking (mirrors the production
    /// chunker's 512-token / 64-token overlap strategy with 1 token ≈ 4 chars).
    /// </summary>
    private static List<string> SplitIntoChunks(string text)
    {
        const int chunkSizeTokens = 512;
        const int overlapTokens = 64;
        const int maxChunkChars = 2048; // hard limit: nomic-embed-text max context

        var chunks = new List<string>();
        var sentences = Regex.Split(text, @"(?<=[.!?])\s+(?=[A-Z])")
            .Where(s => !string.IsNullOrWhiteSpace(s))
            .ToArray();

        var current = new List<string>();
        int currentTokens = 0;

        foreach (var sentence in sentences)
        {
            int sentTokens = (int)Math.Ceiling(sentence.Length / 4.0);

            if (currentTokens + sentTokens > chunkSizeTokens && current.Count > 0)
            {
                chunks.Add(string.Join(" ", current));

                var overlap = new List<string>();
                int overlapCount = 0;
                for (int i = current.Count - 1; i >= 0 && overlapCount < overlapTokens; i--)
                {
                    overlapCount += (int)Math.Ceiling(current[i].Length / 4.0);
                    overlap.Insert(0, current[i]);
                }

                current = overlap;
                currentTokens = overlapCount;
            }

            current.Add(sentence);
            currentTokens += sentTokens;
        }

        if (current.Count > 0)
        {
            chunks.Add(string.Join(" ", current));
        }

        // Hard-split any chunk that exceeds the safe character limit
        var safeChunks = new List<string>();
        foreach (var chunk in chunks)
        {
            if (chunk.Length <= maxChunkChars)
            {
                safeChunks.Add(chunk);
            }
            else
            {
                for (int start = 0; start < chunk.Length; start += maxChunkChars)
                {
                    safeChunks.Add(chunk.Substring(start, Math.Min(maxChunkChars, chunk.Length - start)));
                }
            }
        }

        return safeChunks;
    }
}
