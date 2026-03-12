using System;
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
    public void BuildModificationPrompt_NoContext_DoesNotIncludeContextInOutput()
    {
        var prompt = GenAIPromptBuilder.BuildModificationPrompt(
            selectedContent: "Original",
            instruction: "Simplify",
            relevantChunks: new List<string>(),
            materialType: "reading",
            title: "Test Title",
            readingAge: 10,
            actualAge: 12
        );

        Assert.Contains("ORIGINAL CONTENT:", prompt);
        Assert.Contains("Original", prompt);
        Assert.Contains("INSTRUCTION:", prompt);
        Assert.Contains("Simplify", prompt);
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
            relevantChunks: new List<string> { "Chunk A", "Chunk B" },
            materialType: "reading",
            title: "Test Title",
            readingAge: 10,
            actualAge: 12
        );

        Assert.Contains("Chunk A", prompt);
        Assert.Contains("Chunk B", prompt);
    }

    /// <summary>
    /// Spec coverage: GenAISpec Section 3C(2)(b)(v)-(vii) (material metadata in prompt).
    /// See docs/specifications/GenAISpec.md.
    /// </summary>
    [Fact]
    public void BuildModificationPrompt_IncludesTitleAndMaterialType()
    {
        var prompt = GenAIPromptBuilder.BuildModificationPrompt(
            selectedContent: "Some content",
            instruction: "Add more detail",
            relevantChunks: new List<string>(),
            materialType: "reading",
            title: "My Reading Material",
            readingAge: 8,
            actualAge: 10
        );

        Assert.Contains("MATERIAL TITLE:", prompt);
        Assert.Contains("My Reading Material", prompt);
        Assert.Contains("reading", prompt);
        Assert.Contains("reading age of 8", prompt);
        Assert.Contains("actual age of 10", prompt);
    }

    /// <summary>
    /// Spec coverage: GenAISpec Section 3C(2)(b)(vi) (optional readingAge/actualAge).
    /// See docs/specifications/GenAISpec.md.
    /// </summary>
    [Fact]
    public void BuildModificationPrompt_NullAges_OmitsAgeConstraints()
    {
        var prompt = GenAIPromptBuilder.BuildModificationPrompt(
            selectedContent: "Some content",
            instruction: "Simplify",
            relevantChunks: new List<string>(),
            materialType: "reading",
            title: "Title",
            readingAge: null,
            actualAge: null
        );

        Assert.DoesNotContain("reading age of", prompt);
        Assert.DoesNotContain("actual age of", prompt);
    }

    /// <summary>
    /// Spec coverage: GenAISpec Section 3C(2)(b)(vi) (optional readingAge/actualAge - only readingAge provided).
    /// See docs/specifications/GenAISpec.md.
    /// </summary>
    [Fact]
    public void BuildModificationPrompt_OnlyReadingAge_IncludesReadingAgeConstraint()
    {
        var prompt = GenAIPromptBuilder.BuildModificationPrompt(
            selectedContent: "Some content",
            instruction: "Simplify",
            relevantChunks: new List<string>(),
            materialType: "reading",
            title: "Title",
            readingAge: 10,
            actualAge: null
        );

        Assert.Contains("reading age of 10", prompt);
        Assert.DoesNotContain("actual age of", prompt);
    }

    /// <summary>
    /// Spec coverage: GenAISpec Section 3C(2)(b)(vi) (optional readingAge/actualAge - only actualAge provided).
    /// See docs/specifications/GenAISpec.md.
    /// </summary>
    [Fact]
    public void BuildModificationPrompt_OnlyActualAge_IncludesActualAgeConstraint()
    {
        var prompt = GenAIPromptBuilder.BuildModificationPrompt(
            selectedContent: "Some content",
            instruction: "Simplify",
            relevantChunks: new List<string>(),
            materialType: "reading",
            title: "Title",
            readingAge: null,
            actualAge: 12
        );

        Assert.DoesNotContain("reading age of", prompt);
        Assert.Contains("actual age of 12", prompt);
    }

    /// <summary>
    /// Spec coverage: GenAISpec Section 3C(2)(b)(viii) (MarkdownSyntaxGuide used, no hardcoded guide).
    /// See docs/specifications/GenAISpec.md.
    /// </summary>
    [Fact]
    public void BuildModificationPrompt_UsesMarkdownSyntaxGuide()
    {
        var prompt = GenAIPromptBuilder.BuildModificationPrompt(
            selectedContent: "Some content",
            instruction: "Add detail",
            relevantChunks: new List<string>(),
            materialType: "reading",
            title: "Title",
            readingAge: 10,
            actualAge: 12
        );

        Assert.Contains("MARKDOWN SYNTAX:", prompt);
        Assert.Contains("H1 to H3 headers", prompt);
        Assert.Contains("Blockquotes:", prompt);
    }

    /// <summary>
    /// Spec coverage: GenAISpec Section 3C(2)(b)(viii) (question-draft syntax for worksheets).
    /// See docs/specifications/GenAISpec.md.
    /// </summary>
    [Fact]
    public void BuildModificationPrompt_Worksheet_IncludesQuestionSyntax()
    {
        var prompt = GenAIPromptBuilder.BuildModificationPrompt(
            selectedContent: "Some content",
            instruction: "Add a question",
            relevantChunks: new List<string>(),
            materialType: "worksheet",
            title: "My Worksheet",
            readingAge: 10,
            actualAge: 12
        );

        Assert.Contains("question-draft", prompt);
        Assert.Contains("MULTIPLE_CHOICE", prompt);
        Assert.Contains("WRITTEN_ANSWER", prompt);
    }

    /// <summary>
    /// Spec coverage: GenAISpec Section 3C(2)(b)(viii) (no question-draft for readings).
    /// See docs/specifications/GenAISpec.md.
    /// </summary>
    [Fact]
    public void BuildModificationPrompt_Reading_ExcludesQuestionSyntax()
    {
        var prompt = GenAIPromptBuilder.BuildModificationPrompt(
            selectedContent: "Some content",
            instruction: "Add detail",
            relevantChunks: new List<string>(),
            materialType: "reading",
            title: "Title",
            readingAge: 10,
            actualAge: 12
        );

        Assert.DoesNotContain("question-draft", prompt);
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
            materialType: "reading",
            title: "Introduction to Science"
        );

        Assert.DoesNotContain("**Questions:**", prompt);
        Assert.DoesNotContain("!!! question-draft", prompt);
        Assert.Contains("MARKDOWN SYNTAX:", prompt);
        Assert.Contains("SOURCE DOCUMENT CONTEXT:", prompt);
    }

    /// <summary>
    /// Spec coverage: GenAISpec Section 3B(3)(c)(v) (explicit no-questions restraint for non-worksheet materials).
    /// See docs/specifications/GenAISpec.md.
    /// </summary>
    [Fact]
    public void BuildGenerationPrompt_ReadingMaterial_IncludesNoQuestionsRestraint()
    {
        var prompt = GenAIPromptBuilder.BuildGenerationPrompt(
            description: "A short lesson",
            readingAge: 10,
            actualAge: 12,
            durationInMinutes: 20,
            relevantChunks: new List<string> { "Context A" },
            materialType: "reading",
            title: "Introduction to Science"
        );

        Assert.Contains("Do not include any questions", prompt);
    }

    /// <summary>
    /// Spec coverage: GenAISpec Section 3B(3)(c)(iv)-(v) (no no-questions restraint for worksheets).
    /// See docs/specifications/GenAISpec.md.
    /// </summary>
    [Fact]
    public void BuildGenerationPrompt_WorksheetMaterial_DoesNotIncludeNoQuestionsRestraint()
    {
        var prompt = GenAIPromptBuilder.BuildGenerationPrompt(
            description: "A short lesson",
            readingAge: 10,
            actualAge: 12,
            durationInMinutes: 20,
            relevantChunks: new List<string> { "Context A" },
            materialType: "worksheet",
            title: "Introduction to Science"
        );

        Assert.DoesNotContain("Do not include any questions", prompt);
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
            materialType: "worksheet",
            title: "Introduction to Science"
        );

        Assert.Contains("question-draft", prompt);
        Assert.Contains("MULTIPLE_CHOICE", prompt);
        Assert.Contains("WRITTEN_ANSWER", prompt);
    }
}
