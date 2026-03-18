using System;
using System.Diagnostics;
using System.IO;
using System.Collections.Generic;
using System.Text.Json;
using System.Threading.Tasks;
using Xunit;
using Xunit.Abstractions;
using Microsoft.Extensions.DependencyInjection;
using Main.Services.GenAI;
using Main.Models.Entities;
using Main.Data;

namespace AiEvaluationTests;

[Trait("Category", "AIEvaluation")]
public class RAGLatencyTests : IClassFixture<AiEvaluationWebApplicationFactory>
{
    private readonly AiEvaluationWebApplicationFactory _factory;
    private readonly ITestOutputHelper _output;

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
        var embeddingService = scope.ServiceProvider.GetRequiredService<DocumentEmbeddingService>();
        var dbContext = scope.ServiceProvider.GetRequiredService<MainDbContext>();
        
        string dataPath = Path.Combine(AppContext.BaseDirectory, "Data", "HeavyTranscript.txt");
        string transcriptPayload = "Fallback payload for testing if HeavyTranscript.txt is missing. " + new string('A', 50000);
        
        if (File.Exists(dataPath))
        {
            transcriptPayload = await File.ReadAllTextAsync(dataPath);
        }

        var results = new List<long>();

        for (int i = 0; i < iterations; i++)
        {
            var doc = new SourceDocumentEntity
            {
                Id = Guid.NewGuid(),
                Transcript = transcriptPayload,
                UnitCollectionId = Guid.NewGuid(),
                EmbeddingStatus = Main.Models.Enums.EmbeddingStatus.PENDING
            };
            
            dbContext.SourceDocuments.Add(doc);
            await dbContext.SaveChangesAsync();

            var stopwatch = Stopwatch.StartNew();
            await embeddingService.IndexSourceDocumentAsync(doc);
            stopwatch.Stop();
            
            results.Add(stopwatch.ElapsedMilliseconds);
            _output.WriteLine($"Iteration {i + 1} latency: {stopwatch.ElapsedMilliseconds} ms");
        }

        string resultsDir = Path.Combine(AppContext.BaseDirectory, "Data", "Results");
        Directory.CreateDirectory(resultsDir);
        await File.WriteAllTextAsync(Path.Combine(resultsDir, "RAGLatencyResults.json"), JsonSerializer.Serialize(results, new JsonSerializerOptions { WriteIndented = true }));
    }
}
