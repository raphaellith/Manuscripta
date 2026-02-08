using Main.Services.GenAI;
using Xunit;

namespace MainTests.ServicesTests.GenAI;

/// <summary>
/// Spec coverage: GenAISpec Appendix C (Material Encoding reference for prompts).
/// See docs/specifications/GenAISpec.md.
/// </summary>
public class MarkdownSyntaxGuideTests
{
    /// <summary>
    /// Spec coverage: GenAISpec Appendix C (omit Questions section for non-worksheet prompts).
    /// See docs/specifications/GenAISpec.md.
    /// </summary>
    [Fact]
    public void Get_WithoutQuestionSyntax_DoesNotIncludeQuestionDraftBlock()
    {
        var guide = MarkdownSyntaxGuide.Get(includeQuestionSyntax: false);

        Assert.Contains("Markdown syntax supported", guide);
        Assert.DoesNotContain("question-draft", guide);
        Assert.DoesNotContain("MULTIPLE_CHOICE", guide);
    }

    /// <summary>
    /// Spec coverage: GenAISpec Appendix C (include Questions section for worksheet prompts).
    /// See docs/specifications/GenAISpec.md.
    /// </summary>
    [Fact]
    public void Get_WithQuestionSyntax_IncludesQuestionDraftBlockAndRules()
    {
        var guide = MarkdownSyntaxGuide.Get(includeQuestionSyntax: true);

        Assert.Contains("question-draft", guide);
        Assert.Contains("MULTIPLE_CHOICE", guide);
        Assert.Contains("WRITTEN_ANSWER", guide);
        Assert.Contains("correct_answer", guide);
        Assert.Contains("mark_scheme", guide);
    }
}
