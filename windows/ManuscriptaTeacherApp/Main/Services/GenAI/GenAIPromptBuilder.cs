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
        List<string> relevantChunks,
        string materialType,
        string title,
        int? readingAge,
        int? actualAge)
    {
        var contextSection = relevantChunks.Count > 0
            ? string.Join("\n\n", relevantChunks)
            : "";

        // §3C(2)(b)(viii): Include question-draft syntax when materialType is WORKSHEET
        var markdownSyntaxGuide = MarkdownSyntaxGuide.Get(includeQuestionSyntax: string.Equals(materialType, "WORKSHEET", StringComparison.OrdinalIgnoreCase));

        var restraints = "Format the modified content using the Markdown syntax described below.\n" +
                         "Use British English always.\n" +
                         "Avoid American English.\n" +
                         "Include only material content in your response.\n" +
                         "Never include extraneous text such as prompt restatement, internal thought processes, or notes to the user.\n" +
                         "Preserve existing formatting and structure where appropriate.\n" +
                         "Output only the modified content which should be an appropriate replacement for the original content based on the instruction. Do not output the entire material again, only the modified section.\n" +
                         "Do not begin the output with the material title.\n" +
                         "Do not add questions unless the instruction (the INSTRUCTION section) explicitly asks you to add a question. Again, only add questions if the instruction explicitly asks for them. Do not add questions based on your own assumptions about what might be helpful. Only add questions if the instruction explicitly instructs you to do so.";
        if (readingAge.HasValue || actualAge.HasValue)
        {
            if (readingAge.HasValue && actualAge.HasValue)
            {
                restraints += $"\nAdapt the material's reading level for students with a reading age of {readingAge.Value} and an actual age of {actualAge.Value}.";
            }
            else if (readingAge.HasValue)
            {
                restraints += $"\nAdapt the material's reading level for students with a reading age of {readingAge.Value}.";
            }
            else
            {
                restraints += $"\nAdapt the material's reading level for students with an actual age of {actualAge!.Value}.";
            }
        }

        return $@"
TASK:
Modify the following {materialType} material content according to the instruction provided.


MATERIAL TITLE:
{title}


ORIGINAL CONTENT:
{selectedContent}


INSTRUCTION:
{instruction}


RESTRAINTS:
{restraints}


MARKDOWN SYNTAX:

---

{markdownSyntaxGuide}

---


SOURCE DOCUMENT CONTEXT:
{contextSection}
";
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
        string materialType,
        string title)
    {
        var contextSection = relevantChunks.Count > 0
            ? string.Join("\n\n", relevantChunks)
            : "";

        var markdownSyntaxGuide = MarkdownSyntaxGuide.Get(includeQuestionSyntax: materialType == "worksheet");

        return $@"
TASK:
Create an educational {materialType} material based on the material description and source document context provided below, obeying all listed restraints.


MATERIAL TITLE:
{title}


MATERIAL DESCRIPTION:
{description}


RESTRAINTS:
Adapt the material's reading level for students with a reading age of {readingAge} and an actual age of {actualAge}.
Tailor the material's length so that students can complete the material in approximately {durationInMinutes} minutes.
Format the material using the Markdown syntax described below.
Use British English always. Avoid American English.
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
