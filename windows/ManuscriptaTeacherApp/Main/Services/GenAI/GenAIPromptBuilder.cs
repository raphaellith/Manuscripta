using System.Collections.Generic;

namespace Main.Services.GenAI;

/// <summary>
/// Centralizes GenAI prompt construction for testability.
/// </summary>
public static class GenAIPromptBuilder
{
    /// <summary>
    /// Constructs the modification prompt.
    /// See GenAISpec.md §3C(2)(b) and Material Encoding Specification §4.
    /// </summary>
    public static string BuildModificationPrompt(
        string selectedContent,
        string instruction,
        List<string> relevantChunks)
    {
        var contextSection = relevantChunks.Count > 0
            ? $"Relevant context from source documents:\n{string.Join("\n\n", relevantChunks)}\n\n"
            : "";

        return $@"{contextSection}Modify the following content according to the instruction provided.

Original content:
{selectedContent}

Instruction: {instruction}

Return the modified content following these Markdown syntax requirements:
- Headers shall use # for Level 1, ## for Level 2, ### for Level 3 (do not exceed Level 3)
- Text shall be rendered in bold using **text** or __text__ syntax
- Text shall be rendered in italic using *text* or _text_ syntax
- Unordered lists shall be specified using - item or * item syntax
- Ordered lists shall be specified using 1. item syntax
- Tables use GitHub Flavored Markdown syntax with pipes (|) and dashes (---)
- Code blocks shall be specified using triple backticks with optional language identifier
- Blockquotes shall be specified using the > prefix
- Horizontal rules shall be specified using three or more hyphens on a line
- LaTeX notation shall be delimited by single dollar signs ($...$) for inline or double dollar signs ($$...$$) for blocks
- Images shall be embedded using ![alt text](/attachments/{{attachment-uuid}})
- PDF documents shall be embedded using the pdf marker: !!! pdf id=""{{pdf-attachment-uuid}}""
- Text shall be centred using the center marker: !!! center
- Questions shall be referenced using the question marker: !!! question id=""{{question-uuid}}""
- Preserve existing formatting and structure where appropriate

Modified content:";
    }

    /// <summary>
    /// Constructs the generation prompt with context and requirements.
    /// See GenAISpec.md §3B(3)(c) and Material Encoding Specification §4.
    /// </summary>
    public static string BuildGenerationPrompt(
        string description,
        int readingAge,
        int actualAge,
        int durationInMinutes,
        List<string> relevantChunks,
        string materialType)
    {
        var contextSection = relevantChunks.Count > 0
            ? string.Join("\n\n", relevantChunks)
            : "";

        var markdownSyntaxGuide = MarkdownSyntaxGuide.Get(includeQuestionSyntax: materialType == "worksheet");

        return $@"
TASK:
Create an educational {materialType} material based on the material description and source document context provided below, obeying all listed restraints.


MATERIAL DESCRIPTION:
{description}


RESTRAINTS:
Adapt the material's reading level for students with a reading age of {readingAge} and an actual age of {actualAge}.
Tailor the material's length so that students can complete the material in approximately {durationInMinutes} minutes.
Format the material using the Markdown syntax described below.
Use British English always.
Avoid American English.
Include only material content in your response.
Never include extraneous text such as prompt restatement, internal thought processes, or notes to the user.


MARKDOWN SYNTAX:

---

{markdownSyntaxGuide}

---


SOURCE DOCUMENT CONTEXT:
{contextSection}
";
    }
}
