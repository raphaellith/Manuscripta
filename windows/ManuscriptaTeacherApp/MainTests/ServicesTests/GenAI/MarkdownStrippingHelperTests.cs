using Main.Services.GenAI;
using Xunit;

namespace MainTests.ServicesTests.GenAI;

/// <summary>
/// Tests for the shared MarkdownStrippingHelper utility.
/// Ensures all Markdown syntax elements are properly stripped from LLM-generated text.
/// </summary>
public class MarkdownStrippingHelperTests
{
    /// <summary>
    /// Tests that Markdown headers are stripped.
    /// </summary>
    [Fact]
    public void StripMarkdownSyntax_Headers_RemovesMarkdownFormatting()
    {
        var input = "# Header 1\n## Header 2\n### Header 3";
        var expected = "Header 1\nHeader 2\nHeader 3";

        var result = MarkdownStrippingHelper.StripMarkdownSyntax(input);

        Assert.Equal(expected, result);
    }

    /// <summary>
    /// Tests that bold and italic Markdown syntax is stripped.
    /// </summary>
    [Fact]
    public void StripMarkdownSyntax_BoldAndItalic_RemovesMarkdownFormatting()
    {
        var input = "This is **bold** and this is *italic* text.";
        var expected = "This is bold and this is italic text.";

        var result = MarkdownStrippingHelper.StripMarkdownSyntax(input);

        Assert.Equal(expected, result);
    }

    /// <summary>
    /// Tests that code blocks are removed.
    /// </summary>
    [Fact]
    public void StripMarkdownSyntax_CodeBlocks_RemovesCodeBlocks()
    {
        var input = "Here is some text.\n```csharp\nvar x = 5;\n```\nMore text.";
        var expected = "Here is some text.\n\nMore text.";

        var result = MarkdownStrippingHelper.StripMarkdownSyntax(input);

        Assert.Equal(expected, result);
    }

    /// <summary>
    /// Tests that inline code is stripped but content is preserved.
    /// </summary>
    [Fact]
    public void StripMarkdownSyntax_InlineCode_RemovesBackticks()
    {
        var input = "Use the `print()` function in Python.";
        var expected = "Use the print() function in Python.";

        var result = MarkdownStrippingHelper.StripMarkdownSyntax(input);

        Assert.Equal(expected, result);
    }

    /// <summary>
    /// Tests that Markdown links are converted to plain text.
    /// </summary>
    [Fact]
    public void StripMarkdownSyntax_Links_PreservesTextRemovesUrl()
    {
        var input = "Check out [this resource](https://example.com) for more info.";
        var expected = "Check out this resource for more info.";

        var result = MarkdownStrippingHelper.StripMarkdownSyntax(input);

        Assert.Equal(expected, result);
    }

    /// <summary>
    /// Tests that unordered list markers are preserved (no longer stripped).
    /// </summary>
    [Fact]
    public void StripMarkdownSyntax_UnorderedLists_PreservesMarkers()
    {
        var input = "- First item\n- Second item\n* Third item";

        var result = MarkdownStrippingHelper.StripMarkdownSyntax(input);

        Assert.Equal(input, result);
    }

    /// <summary>
    /// Tests that ordered list markers are preserved (no longer stripped).
    /// </summary>
    [Fact]
    public void StripMarkdownSyntax_OrderedLists_PreservesNumbers()
    {
        var input = "1. First item\n2. Second item\n3. Third item";

        var result = MarkdownStrippingHelper.StripMarkdownSyntax(input);

        Assert.Equal(input, result);
    }

    /// <summary>
    /// Tests that blockquotes are stripped.
    /// </summary>
    [Fact]
    public void StripMarkdownSyntax_Blockquotes_RemovesMarkers()
    {
        var input = "> This is a quote\n> Another line";
        var expected = "This is a quote\nAnother line";

        var result = MarkdownStrippingHelper.StripMarkdownSyntax(input);

        Assert.Equal(expected, result);
    }

    /// <summary>
    /// Tests that null or empty input is handled correctly.
    /// </summary>
    [Theory]
    [InlineData(null)]
    [InlineData("")]
    public void StripMarkdownSyntax_NullOrEmpty_ReturnsInput(string? input)
    {
        var result = MarkdownStrippingHelper.StripMarkdownSyntax(input!);

        Assert.Equal(input, result);
    }

    /// <summary>
    /// Tests complex text with multiple Markdown elements.
    /// </summary>
    [Fact]
    public void StripMarkdownSyntax_ComplexText_RemovesAllMarkdown()
    {
        var input = @"## Score Justification

Your response demonstrates **good understanding** of the topic.

### Strengths:
- Clear explanation of `key concepts`
- Well-structured argument

### Areas for Improvement:
1. Add more *specific examples*
2. Improve [conclusion](https://example.com)

> Overall, a solid effort!";

        var expected = @"Score Justification

Your response demonstrates good understanding of the topic.

Strengths:
- Clear explanation of key concepts
- Well-structured argument

Areas for Improvement:
1. Add more specific examples
2. Improve conclusion

Overall, a solid effort!";

        var result = MarkdownStrippingHelper.StripMarkdownSyntax(input);

        Assert.Equal(expected, result);
    }

    /// <summary>
    /// Tests that strikethrough syntax is stripped.
    /// </summary>
    [Fact]
    public void StripMarkdownSyntax_Strikethrough_RemovesTildes()
    {
        var input = "This is ~~deleted~~ text.";
        var expected = "This is deleted text.";

        var result = MarkdownStrippingHelper.StripMarkdownSyntax(input);

        Assert.Equal(expected, result);
    }

    /// <summary>
    /// Tests that horizontal rules are removed.
    /// </summary>
    [Fact]
    public void StripMarkdownSyntax_HorizontalRules_Removed()
    {
        var input = "Above\n---\nBelow";
        var expected = "Above\n\nBelow";

        var result = MarkdownStrippingHelper.StripMarkdownSyntax(input);

        Assert.Equal(expected, result);
    }

    /// <summary>
    /// Tests that HTML tags are removed.
    /// </summary>
    [Fact]
    public void StripMarkdownSyntax_HtmlTags_Removed()
    {
        var input = "This has <b>bold</b> and <em>emphasis</em>.";
        var expected = "This has bold and emphasis.";

        var result = MarkdownStrippingHelper.StripMarkdownSyntax(input);

        Assert.Equal(expected, result);
    }

    /// <summary>
    /// Tests that plain text with no Markdown is returned unchanged.
    /// </summary>
    [Fact]
    public void StripMarkdownSyntax_PlainText_ReturnsUnchanged()
    {
        var input = "This is just plain text with no formatting.";

        var result = MarkdownStrippingHelper.StripMarkdownSyntax(input);

        Assert.Equal(input, result);
    }
}
