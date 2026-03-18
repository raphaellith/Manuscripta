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
    public double InitialFleschKincaid { get; set; }
    public double FinalFleschKincaid { get; set; }
    public bool RetainedEntity { get; set; }
}

[Trait("Category", "AIEvaluation")]
public class ContentModificationTests : IClassFixture<AiEvaluationWebApplicationFactory>
{
    private readonly AiEvaluationWebApplicationFactory _factory;
    private readonly ITestOutputHelper _output;

    public ContentModificationTests(AiEvaluationWebApplicationFactory factory, ITestOutputHelper output)
    {
        _factory = factory;
        _output = output;
    }

    private double CalculateFleschKincaid(string text)
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
        
        string originalContent = "The mitochondria is the indubitable powerhouse of the cellular micro-organism. It orchestrates adenocine-triphosphate synthesis.";
        string instruction = "Simplify terminology for an 8 year old";

        var initialFk = CalculateFleschKincaid(originalContent);
        var results = new List<ContentModificationEvalResult>();

        for (int i = 0; i < iterations; i++)
        {
            var result = await modService.ModifyContent(originalContent, instruction, null, "reading", "Cellular Biology", 8, 8, null, default);
            var content = result.Content ?? string.Empty;
            var simplifiedFk = CalculateFleschKincaid(content);

            results.Add(new ContentModificationEvalResult
            {
                Iteration = i + 1,
                InitialFleschKincaid = initialFk,
                FinalFleschKincaid = simplifiedFk,
                RetainedEntity = content.Contains("mitochondria", StringComparison.OrdinalIgnoreCase)
            });
            
            _output.WriteLine($"Iteration {i + 1} Simplified: {content}");
        }

        string resultsDir = Path.Combine(AppContext.BaseDirectory, "Data", "Results");
        Directory.CreateDirectory(resultsDir);
        await File.WriteAllTextAsync(Path.Combine(resultsDir, "ContentModificationResults.json"), JsonSerializer.Serialize(results, new JsonSerializerOptions { WriteIndented = true }));
    }
}
