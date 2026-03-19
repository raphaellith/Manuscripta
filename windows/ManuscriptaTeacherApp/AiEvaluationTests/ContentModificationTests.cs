using System;
using System.IO;
using System.Collections.Generic;
using System.Text.Json;
using System.Threading.Tasks;
using Xunit;
using Xunit.Abstractions;
using Microsoft.Extensions.DependencyInjection;
using Main.Services.GenAI;

namespace AiEvaluationTests;

public class ContentModificationEvalResult
{
    public int Iteration { get; set; }
    public string Model { get; set; } = string.Empty;
    public double InitialFleschKincaid { get; set; }
    public double FinalFleschKincaid { get; set; }
    public bool RetainedEntity { get; set; }
}

[Trait("Category", "AIEvaluation")]
public class ContentModificationTests : IClassFixture<AiEvaluationWebApplicationFactory>
{
    private readonly AiEvaluationWebApplicationFactory _factory;
    private readonly ITestOutputHelper _output;
    private static readonly JsonSerializerOptions JsonOptions = new() { WriteIndented = true };

    private const string OriginalContent =
        "The mitochondria is the indubitable powerhouse of the cellular micro-organism. "
        + "It orchestrates adenocine-triphosphate synthesis.";
    private const string Instruction = "Simplify terminology for an 8 year old";

    public ContentModificationTests(AiEvaluationWebApplicationFactory factory, ITestOutputHelper output)
    {
        _factory = factory;
        _output = output;
    }

    private static double CalculateFleschKincaid(string text)
    {
        if (string.IsNullOrWhiteSpace(text)) return 0;
        var words = text.Split(new char[] { ' ', '\r', '\n' }, StringSplitOptions.RemoveEmptyEntries).Length;
        var sentences = text.Split(new char[] { '.', '!', '?' }, StringSplitOptions.RemoveEmptyEntries).Length;
        
        if (words == 0 || sentences == 0) return 0;
        var syllables = text.Length / 3.0;
        return 0.39 * ((double)words / sentences) + 11.8 * (syllables / words) - 15.59;
    }

    [Theory]
    [InlineData(10)]
    public async Task ModifyContent_Should_Reduce_Complexity_And_Retain_Entities(int iterations)
    {
        using var scope = _factory.Services.CreateScope();
        var modService = scope.ServiceProvider.GetRequiredService<IContentModificationService>();

        var initialFk = CalculateFleschKincaid(OriginalContent);
        var results = new List<ContentModificationEvalResult>();

        for (int i = 0; i < iterations; i++)
        {
            var result = await modService.ModifyContent(
                OriginalContent, Instruction, null, "reading",
                "Cellular Biology", 8, 8, null, default);
            var content = result.Content ?? string.Empty;
            var simplifiedFk = CalculateFleschKincaid(content);

            results.Add(new ContentModificationEvalResult
            {
                Iteration = i + 1,
                Model = "qwen3:8b",
                InitialFleschKincaid = initialFk,
                FinalFleschKincaid = simplifiedFk,
                RetainedEntity = content.Contains("mitochondria", StringComparison.OrdinalIgnoreCase)
            });
            
            _output.WriteLine($"[qwen3:8b] Iteration {i + 1} Simplified: {content}");
        }

        string resultsDir = Path.Combine(AppContext.BaseDirectory, "Data", "Results");
        Directory.CreateDirectory(resultsDir);
        await File.WriteAllTextAsync(
            Path.Combine(resultsDir, "ContentModificationResults.json"),
            JsonSerializer.Serialize(results, JsonOptions));
    }

    /// <summary>
    /// Evaluates content modification quality using direct LLM calls, parameterised by
    /// model, to compare qwen3:8b and granite4 under the identical prompt.
    /// </summary>
    [Theory]
    [InlineData("qwen3:8b", 10)]
    [InlineData("granite4", 10)]
    public async Task ModifyContent_Model_Comparison(string model, int iterations)
    {
        var ollamaClient = _factory.Services.GetRequiredService<OllamaClientService>();

        var prompt = GenAIPromptBuilder.BuildModificationPrompt(
            OriginalContent,
            Instruction,
            relevantChunks: new List<string>(),
            materialType: "reading",
            title: "Cellular Biology",
            readingAge: 8,
            actualAge: 8);

        var initialFk = CalculateFleschKincaid(OriginalContent);
        var results = new List<ContentModificationEvalResult>();

        for (int i = 0; i < iterations; i++)
        {
            var content = await ollamaClient.GenerateChatCompletionAsync(model, prompt);
            var simplifiedFk = CalculateFleschKincaid(content);

            results.Add(new ContentModificationEvalResult
            {
                Iteration = i + 1,
                Model = model,
                InitialFleschKincaid = initialFk,
                FinalFleschKincaid = simplifiedFk,
                RetainedEntity = content.Contains("mitochondria", StringComparison.OrdinalIgnoreCase)
            });

            _output.WriteLine(
                $"[{model}] Iteration {i + 1}: FK={simplifiedFk:F2}, " +
                $"retained={content.Contains("mitochondria", StringComparison.OrdinalIgnoreCase)}");
        }

        string resultsDir = Path.Combine(AppContext.BaseDirectory, "Data", "Results");
        Directory.CreateDirectory(resultsDir);
        string safeModel = model.Replace(":", "_");
        await File.WriteAllTextAsync(
            Path.Combine(resultsDir, $"ContentModificationResults_{safeModel}.json"),
            JsonSerializer.Serialize(results, JsonOptions));
    }
}
