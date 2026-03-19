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
    public string Model { get; set; } = string.Empty;
    public long GenerationTimeMs { get; set; }
    public bool ContainsDraftQuestion { get; set; }
    public bool ContainsThinkingTags { get; set; }
}

[Trait("Category", "AIEvaluation")]
public class MaterialGenerationTests : IClassFixture<AiEvaluationWebApplicationFactory>
{
    private readonly AiEvaluationWebApplicationFactory _factory;
    private readonly ITestOutputHelper _output;
    private static readonly JsonSerializerOptions JsonOptions = new() { WriteIndented = true };

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
                Model = "qwen3:8b",
                GenerationTimeMs = sw.ElapsedMilliseconds,
                ContainsDraftQuestion = content.Contains(@"!!! question-draft"),
                ContainsThinkingTags = content.Contains("<think>")
            });
            
            _output.WriteLine($"[qwen3:8b] Iteration {i + 1} Generation Time: {sw.ElapsedMilliseconds} ms");
        }
        
        string resultsDir = Path.Combine(AppContext.BaseDirectory, "Data", "Results");
        Directory.CreateDirectory(resultsDir);
        await File.WriteAllTextAsync(
            Path.Combine(resultsDir, "MaterialGenerationResults.json"),
            JsonSerializer.Serialize(results, JsonOptions));
    }

    /// <summary>
    /// Evaluates material generation quality using direct LLM calls, parameterised
    /// by model, to compare qwen3:8b and granite4 under identical prompts.
    /// </summary>
    [Theory]
    [InlineData("qwen3:8b", 10)]
    [InlineData("granite4", 10)]
    public async Task Material_Generation_Model_Comparison(string model, int iterations)
    {
        var ollamaClient = _factory.Services.GetRequiredService<OllamaClientService>();

        string description =
            "Create a short physics worksheet covering Newton's laws of motion "
            + "with three separate draft questions placed inside the reading material.";
        string title = "Newton's Laws AI Test";

        var prompt = GenAIPromptBuilder.BuildGenerationPrompt(
            description,
            readingAge: 14,
            actualAge: 15,
            durationInMinutes: 30,
            relevantChunks: new List<string>(),
            materialType: "worksheet",
            title: title);

        var results = new List<MaterialGenerationResult>();

        for (int i = 0; i < iterations; i++)
        {
            var sw = Stopwatch.StartNew();
            var content = await ollamaClient.GenerateChatCompletionAsync(model, prompt);
            sw.Stop();

            results.Add(new MaterialGenerationResult
            {
                Iteration = i + 1,
                Model = model,
                GenerationTimeMs = sw.ElapsedMilliseconds,
                ContainsDraftQuestion = content.Contains("!!! question-draft"),
                ContainsThinkingTags = content.Contains("<think>")
            });

            _output.WriteLine(
                $"[{model}] Iteration {i + 1}: {sw.ElapsedMilliseconds} ms, " +
                $"draft={content.Contains("!!! question-draft")}, " +
                $"think={content.Contains("<think>")}");
        }

        string resultsDir = Path.Combine(AppContext.BaseDirectory, "Data", "Results");
        Directory.CreateDirectory(resultsDir);
        string safeModel = model.Replace(":", "_");
        await File.WriteAllTextAsync(
            Path.Combine(resultsDir, $"MaterialGenerationResults_{safeModel}.json"),
            JsonSerializer.Serialize(results, JsonOptions));
    }
}
