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
using Main.Models.Dtos;

namespace AiEvaluationTests;

public class MaterialGenerationResult
{
    public int Iteration { get; set; }
    public long GenerationTimeMs { get; set; }
    public bool ContainsDraftQuestion { get; set; }
    public bool ContainsThinkingTags { get; set; }
}

[Trait("Category", "AIEvaluation")]
public class MaterialGenerationTests : IClassFixture<AiEvaluationWebApplicationFactory>
{
    private readonly AiEvaluationWebApplicationFactory _factory;
    private readonly ITestOutputHelper _output;

    public MaterialGenerationTests(AiEvaluationWebApplicationFactory factory, ITestOutputHelper output)
    {
        _factory = factory;
        _output = output;
    }

    [Theory]
    [InlineData(10)]
    public async Task Material_Generation_Metrics_Are_Acceptable(int iterations)
    {
        using var scope = _factory.Services.CreateScope();
        var generationService = scope.ServiceProvider.GetRequiredService<IMaterialGenerationService>();
        
        var results = new List<MaterialGenerationResult>();

        for (int i = 0; i < iterations; i++)
        {
            var request = new GenerationRequest
            {
                UnitCollectionId = Guid.NewGuid(),
                Description = "Create a short physics worksheet covering Newton's laws of motion with three separate draft questions placed inside the reading material.",
                ReadingAge = 14,
                ActualAge = 15,
                DurationInMinutes = 30,
                Title = "Newton's Laws AI Test"
            };

            var sw = Stopwatch.StartNew();
            var result = await generationService.GenerateWorksheet(request, null, default);
            sw.Stop();

            var content = result.Content ?? string.Empty;
            
            results.Add(new MaterialGenerationResult
            {
                Iteration = i + 1,
                GenerationTimeMs = sw.ElapsedMilliseconds,
                ContainsDraftQuestion = content.Contains(@"!!! question-draft"),
                ContainsThinkingTags = content.Contains("<think>")
            });
            
            _output.WriteLine($"Iteration {i + 1} Generation Time: {sw.ElapsedMilliseconds} ms");
        }
        
        string resultsDir = Path.Combine(AppContext.BaseDirectory, "Data", "Results");
        Directory.CreateDirectory(resultsDir);
        await File.WriteAllTextAsync(Path.Combine(resultsDir, "MaterialGenerationResults.json"), JsonSerializer.Serialize(results, new JsonSerializerOptions { WriteIndented = true }));
    }
}
