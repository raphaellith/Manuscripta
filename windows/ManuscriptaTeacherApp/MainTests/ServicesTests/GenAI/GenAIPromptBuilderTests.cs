using System.Collections.Generic;
using Main.Services.GenAI;
using Xunit;

namespace MainTests.ServicesTests.GenAI;

/// <summary>
/// Spec coverage: GenAISpec Section 3B (Material Generation prompts) and Section 3C (Content Modification).
/// See docs/specifications/GenAISpec.md.
/// </summary>
public class GenAIPromptBuilderTests
{
    /// <summary>
    /// Spec coverage: GenAISpec Section 3C(2)(b)(iii) (optional context inclusion).
    /// See docs/specifications/GenAISpec.md.
    /// </summary>
    [Fact]
    public void BuildModificationPrompt_NoContext_DoesNotIncludeContextSection()
    {
        var prompt = GenAIPromptBuilder.BuildModificationPrompt(
            selectedContent: "Original",
            instruction: "Simplify",
            relevantChunks: new List<string>()
        );

        Assert.DoesNotContain("Relevant context from source documents:", prompt);
        Assert.Contains("Original content:\nOriginal", prompt);
        Assert.Contains("Instruction: Simplify", prompt);
    }

    /// <summary>
    /// Spec coverage: GenAISpec Section 3C(2)(b)(iii) (context included when provided).
    /// See docs/specifications/GenAISpec.md.
    /// </summary>
    [Fact]
    public void BuildModificationPrompt_WithContext_IncludesContextSection()
    {
        var prompt = GenAIPromptBuilder.BuildModificationPrompt(
            selectedContent: "Original",
            instruction: "Simplify",
            relevantChunks: new List<string> { "Chunk A", "Chunk B" }
        );

        Assert.Contains("Relevant context from source documents:", prompt);
        Assert.Contains("Chunk A", prompt);
        Assert.Contains("Chunk B", prompt);
    }

    /// <summary>
    /// Spec coverage: GenAISpec Section 3B(3)(c)(v) (exclude Questions section for readings).
    /// See docs/specifications/GenAISpec.md.
    /// </summary>
    [Fact]
    public void BuildGenerationPrompt_ReadingMaterial_ExcludesQuestionSyntax()
    {
        var prompt = GenAIPromptBuilder.BuildGenerationPrompt(
            description: "A short lesson",
            readingAge: 10,
            actualAge: 12,
            durationInMinutes: 20,
            relevantChunks: new List<string> { "Context A" },
            materialType: "reading"
        );

        Assert.DoesNotContain("**Questions:**", prompt);
        Assert.DoesNotContain("!!! question-draft", prompt);
        Assert.Contains("MARKDOWN SYNTAX:", prompt);
        Assert.Contains("SOURCE DOCUMENT CONTEXT:", prompt);
    }

    /// <summary>
    /// Spec coverage: GenAISpec Section 3B(3)(c)(iv)-(v) (include question-draft syntax for worksheets).
    /// See docs/specifications/GenAISpec.md.
    /// </summary>
    [Fact]
    public void BuildGenerationPrompt_WorksheetMaterial_IncludesQuestionSyntax()
    {
        var prompt = GenAIPromptBuilder.BuildGenerationPrompt(
            description: "A short lesson",
            readingAge: 10,
            actualAge: 12,
            durationInMinutes: 20,
            relevantChunks: new List<string> { "Context A" },
            materialType: "worksheet"
        );

        Assert.Contains("question-draft", prompt);
        Assert.Contains("MULTIPLE_CHOICE", prompt);
        Assert.Contains("WRITTEN_ANSWER", prompt);
    }
}
