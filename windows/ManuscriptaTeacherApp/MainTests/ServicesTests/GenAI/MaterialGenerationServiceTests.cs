using System.Collections.Generic;
using System.Reflection;
using Main.Services.GenAI;
using Xunit;

namespace MainTests.ServicesTests.GenAI;

public class MaterialGenerationServiceTests
{
    [Fact]
    public void ConstructGenerationPrompt_ReadingMaterial_ExcludesQuestionSyntax()
    {
        var service = new MaterialGenerationService(null!, null!, null!);
        var method = typeof(MaterialGenerationService).GetMethod(
            "ConstructGenerationPrompt",
            BindingFlags.Instance | BindingFlags.NonPublic);

        Assert.NotNull(method);

        var prompt = (string)method!.Invoke(
            service,
            new object[]
            {
                "Rivers and streams",
                9,
                10,
                15,
                new List<string> { "Context A" },
                "reading"
            })!;

        Assert.Contains("Rivers and streams", prompt);
        Assert.Contains("reading age of 9", prompt);
        Assert.Contains("actual age of 10", prompt);
        Assert.Contains("15 minutes", prompt);
        Assert.Contains("Context A", prompt);
        Assert.DoesNotContain("question-draft", prompt);
    }

    [Fact]
    public void ConstructGenerationPrompt_WorksheetMaterial_IncludesQuestionSyntax()
    {
        var service = new MaterialGenerationService(null!, null!, null!);
        var method = typeof(MaterialGenerationService).GetMethod(
            "ConstructGenerationPrompt",
            BindingFlags.Instance | BindingFlags.NonPublic);

        Assert.NotNull(method);

        var prompt = (string)method!.Invoke(
            service,
            new object[]
            {
                "Photosynthesis basics",
                12,
                13,
                25,
                new List<string>(),
                "worksheet"
            })!;

        Assert.Contains("Photosynthesis basics", prompt);
        Assert.Contains("worksheet", prompt);
        Assert.Contains("question-draft", prompt);
        Assert.Contains("WRITTEN_ANSWER", prompt);
    }
}
