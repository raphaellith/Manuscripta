namespace Main.Services.GenAI;

/// <summary>
/// Provides standardized Markdown syntax guide for use in LLM prompts.
/// </summary>
public static class MarkdownSyntaxGuide
{
    /// <summary>
    /// Returns the standardized Markdown syntax guide for use in LLM prompts.
    /// </summary>
    /// <param name="includeQuestionSyntax">If true, includes the question embedding syntax guide.</param>
    public static string Get(bool includeQuestionSyntax = false)
    {
        var baseGuide = @"Prefix Level 1 headers with one octothorpe (`#`). Example:
# Header

Prefix Level 2 headers with two octothorpes (`##`). Example:
## Header

Prefix Level 3 headers with three octothorpes (`###`). Example:
### Header

Avoid using headers of level 4 or above.

Format text in bold by adding two asterisks (or two underscores) before and after the text. Example:
**text**
__text__

Format text in italics by adding one asterisk (or one underscore) before and after the text. Example:
*text*
_text_

Prefix each item of an unordered list with one hyphen (or one asterisk). Example:
- item
* item

Prefix each item of an ordered list with sequential numbering. Example:
1. item
2. item

Format tables with GitHub-flavoured Markdown syntax. Separate headers and data rows with pipes ('|'). Follow the header row with a separator row of dashes (`---`). Specify column alignment using colons in the separator row, with `:---` for left alignment, `:---:` for centre alignment, and `---:` for right alignment.

Format mathematical LaTeX expressions in inline mode by adding one dollar sign (`$`) before and after the expression. Example:
$x$

Format mathematical LaTeX expressions in display mode by adding two dollar signs (`$$`) before and after the expression. Example:
$$x$$

Format preformatted code blocks by adding three backticks before and after the block. Example:
```language
code content
```
The language identifier (e.g., `python`, `javascript`) is optional and may be used for syntax highlighting.

Prefix each line of a blockquote with one greater-than symbol (`>`).

Insert horizontal rules with a line containing three or more hyphens (`---`).

Embed images using the syntax
![alt text](/attachments/{{attachment-uuid}})
where {{attachment-uuid}} is the UUID of the attachment.

Embed PDF documents using the syntax
!!! pdf id=""{{pdf-attachment-uuid}}""
where {{pdf-attachment-uuid}} is the UUID of the document.

Centre text using the following syntax.
```
!!! center
    This text will be centred.
```";

        if (includeQuestionSyntax)
        {
            baseGuide += @"

Embed questions using the syntax
!!! question id=""{{question-uuid}}""
where {{question-uuid}} is the UUID of the question.";
        }

        return baseGuide;
    }
}
