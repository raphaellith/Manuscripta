using System;
using System.IO;
using System.Text.Json;
using System.Text.Json.Serialization;
using System.Collections.Generic;
using System.Threading.Tasks;
using Xunit;
using Xunit.Abstractions;
using Microsoft.Extensions.DependencyInjection;
using Main.Services.GenAI;
using Main.Models.Entities.Questions;
using Main.Models.Entities.Responses;

namespace AiEvaluationTests;

public class DatasetRoot
{
    [JsonPropertyName("responses")]
    public List<DatasetResponse> Responses { get; set; } = new();
}

public class DatasetResponse
{
    [JsonPropertyName("id")]
    public string Id { get; set; }
    [JsonPropertyName("questionText")]
    public string QuestionText { get; set; }
    [JsonPropertyName("markScheme")]
    public string MarkScheme { get; set; }
    [JsonPropertyName("maxScore")]
    public int MaxScore { get; set; }
    [JsonPropertyName("studentAnswer")]
    public string StudentAnswer { get; set; }
    [JsonPropertyName("expectedMark")]
    public int ExpectedMark { get; set; }
}

public class FeedbackEvalResult
{
    public string DatasetName { get; set; }
    public string Model { get; set; } = string.Empty;
    public string QuestionId { get; set; }
    public int Iteration { get; set; }
    public int ExpectedMark { get; set; }
    public int? ActualMark { get; set; }
    public bool ValidFormat { get; set; }
    public string RawFeedback { get; set; }
}

[Trait("Category", "AIEvaluation")]
public class FeedbackGenerationTests : IClassFixture<AiEvaluationWebApplicationFactory>
{
    private readonly AiEvaluationWebApplicationFactory _factory;
    private readonly ITestOutputHelper _output;
    private static readonly JsonSerializerOptions JsonOptions = new() { WriteIndented = true };

    public FeedbackGenerationTests(AiEvaluationWebApplicationFactory factory, ITestOutputHelper output)
    {
        _factory = factory;
        _output = output;
    }

    [Theory]
    [InlineData("CAIE_CompSci_Paper1")]
    [InlineData("CAIE_Maths_Paper3")]
    [InlineData("CAIE_Eco_Paper2")]
    [InlineData("CAIE_FirstLanEng_Paper2")]
    public async Task FeedbackGeneration_Accuracy_Per_Dataset(string datasetName)
    {
        using var scope = _factory.Services.CreateScope();
        var feedbackService = (FeedbackGenerationService)scope.ServiceProvider.GetRequiredService<IFeedbackGenerationService>();

        string dataPath = Path.Combine(AppContext.BaseDirectory, "Data", $"{datasetName}.json");
        if (!File.Exists(dataPath))
        {
            _output.WriteLine($"Warning: Dataset {datasetName} not found at {dataPath}");
            return;
        }

        string json = await File.ReadAllTextAsync(dataPath);
        var dataset = JsonSerializer.Deserialize<DatasetRoot>(json);

        if (dataset == null || dataset.Responses.Count == 0) return;

        var results = new List<FeedbackEvalResult>();
        int iterationsPerResponse = 3;

        foreach (var entry in dataset.Responses)
        {
            var qId = Guid.NewGuid();
            var question = new WrittenAnswerQuestionEntity(
                qId,
                Guid.NewGuid(),
                entry.QuestionText,
                null,
                entry.MarkScheme,
                entry.MaxScore
            );

            var response = new WrittenAnswerResponseEntity(
                Guid.NewGuid(),
                qId,
                Guid.NewGuid(), 
                entry.StudentAnswer,
                DateTime.UtcNow,
                false
            );

            for (int i = 0; i < iterationsPerResponse; i++)
            {
                var feedback = await feedbackService.GenerateFeedbackAsync(question, response);
                
                results.Add(new FeedbackEvalResult
                {
                    DatasetName = datasetName,
                    Model = "qwen3:8b",
                    QuestionId = entry.Id,
                    Iteration = i + 1,
                    ExpectedMark = entry.ExpectedMark,
                    ActualMark = feedback.Marks,
                    ValidFormat = feedback.FeedbackText != null && !feedback.FeedbackText.Contains("MARK:", StringComparison.OrdinalIgnoreCase),
                    RawFeedback = feedback.FeedbackText ?? ""
                });
                
                _output.WriteLine($"[qwen3:8b][{datasetName}] ID: {entry.Id} | Iter: {i + 1} | Expected: {entry.ExpectedMark} | Actual: {feedback.Marks}");
            }
        }

        string resultsDir = Path.Combine(AppContext.BaseDirectory, "Data", "Results");
        Directory.CreateDirectory(resultsDir);
        await File.WriteAllTextAsync(
            Path.Combine(resultsDir, $"FeedbackResults_{datasetName}.json"),
            JsonSerializer.Serialize(results, JsonOptions));
    }

    /// <summary>
    /// Evaluates feedback generation quality using direct LLM calls, parameterised by
    /// model.  Uses <see cref="FeedbackGenerationService.ConstructFeedbackPrompt"/> and
    /// <see cref="FeedbackGenerationService.ParseFeedbackResponse"/> to ensure identical
    /// prompt construction and response parsing across models.
    /// </summary>
    [Theory]
    [InlineData("qwen3:8b", "CAIE_CompSci_Paper1")]
    [InlineData("granite4", "CAIE_CompSci_Paper1")]
    [InlineData("qwen3:8b", "CAIE_Maths_Paper3")]
    [InlineData("granite4", "CAIE_Maths_Paper3")]
    public async Task FeedbackGeneration_Model_Comparison(string model, string datasetName)
    {
        var ollamaClient = _factory.Services.GetRequiredService<OllamaClientService>();

        string dataPath = Path.Combine(AppContext.BaseDirectory, "Data", $"{datasetName}.json");
        if (!File.Exists(dataPath))
        {
            _output.WriteLine($"Warning: Dataset {datasetName} not found at {dataPath}");
            return;
        }

        string json = await File.ReadAllTextAsync(dataPath);
        var dataset = JsonSerializer.Deserialize<DatasetRoot>(json);
        if (dataset == null || dataset.Responses.Count == 0) return;

        var results = new List<FeedbackEvalResult>();
        int iterationsPerResponse = 3;

        foreach (var entry in dataset.Responses)
        {
            var qId = Guid.NewGuid();
            var question = new WrittenAnswerQuestionEntity(
                qId, Guid.NewGuid(), entry.QuestionText,
                null, entry.MarkScheme, entry.MaxScore);

            var response = new WrittenAnswerResponseEntity(
                Guid.NewGuid(), qId, Guid.NewGuid(),
                entry.StudentAnswer, DateTime.UtcNow, false);

            string prompt = FeedbackGenerationService.ConstructFeedbackPrompt(question, response);

            for (int i = 0; i < iterationsPerResponse; i++)
            {
                string rawResponse = await ollamaClient.GenerateChatCompletionAsync(model, prompt);
                var (marks, feedbackText) = FeedbackGenerationService.ParseFeedbackResponse(
                    rawResponse, entry.MaxScore);

                results.Add(new FeedbackEvalResult
                {
                    DatasetName = datasetName,
                    Model = model,
                    QuestionId = entry.Id,
                    Iteration = i + 1,
                    ExpectedMark = entry.ExpectedMark,
                    ActualMark = marks,
                    ValidFormat = feedbackText != null
                        && !feedbackText.Contains("MARK:", StringComparison.OrdinalIgnoreCase),
                    RawFeedback = feedbackText ?? ""
                });

                _output.WriteLine(
                    $"[{model}][{datasetName}] ID: {entry.Id} | "
                    + $"Iter: {i + 1} | Expected: {entry.ExpectedMark} | Actual: {marks}");
            }
        }

        string resultsDir = Path.Combine(AppContext.BaseDirectory, "Data", "Results");
        Directory.CreateDirectory(resultsDir);
        string safeModel = model.Replace(":", "_");
        await File.WriteAllTextAsync(
            Path.Combine(resultsDir, $"FeedbackResults_{datasetName}_{safeModel}.json"),
            JsonSerializer.Serialize(results, JsonOptions));
    }
}
