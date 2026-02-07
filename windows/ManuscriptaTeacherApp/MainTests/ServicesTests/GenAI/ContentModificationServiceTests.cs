using System.Collections.Generic;
using System.Reflection;
using Main.Services.GenAI;
using Xunit;

namespace MainTests.ServicesTests.GenAI;

public class ContentModificationServiceTests
{
    [Fact]
    public void ConstructModificationPrompt_NoContext_DoesNotIncludeContextSection()
    {
        var service = new ContentModificationService(null!, null!, null!);
        var method = typeof(ContentModificationService).GetMethod(
            "ConstructModificationPrompt",
            BindingFlags.Instance | BindingFlags.NonPublic);

        Assert.NotNull(method);

        var selected = "Original text";
        var instruction = "Simplify this";
        var prompt = (string)method!.Invoke(service, new object[] { selected, instruction, new List<string>() })!;

        Assert.Contains("Modify the following content", prompt);
        Assert.DoesNotContain("Relevant context from source documents", prompt);
        Assert.Contains(selected, prompt);
        Assert.Contains(instruction, prompt);
        Assert.Contains("Modified content:", prompt);
    }

    [Fact]
    public void ConstructModificationPrompt_WithContext_IncludesContextSection()
    {
        var service = new ContentModificationService(null!, null!, null!);
        var method = typeof(ContentModificationService).GetMethod(
            "ConstructModificationPrompt",
            BindingFlags.Instance | BindingFlags.NonPublic);

        Assert.NotNull(method);

        var selected = "Original text";
        var instruction = "Expand this";
        var chunks = new List<string> { "Context A", "Context B" };
        var prompt = (string)method!.Invoke(service, new object[] { selected, instruction, chunks })!;

        Assert.Contains("Relevant context from source documents", prompt);
        Assert.Contains("Context A", prompt);
        Assert.Contains("Context B", prompt);
        Assert.Contains(selected, prompt);
        Assert.Contains(instruction, prompt);
        Assert.Contains("Modified content:", prompt);
    }
}
