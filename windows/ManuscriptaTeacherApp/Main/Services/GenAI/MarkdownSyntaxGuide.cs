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
        var baseGuide = @"
**Markdown syntax supported:**
- H1 to H3 headers: `#`, `##`, `###`
- Avoid using H4 headers.
- Bold text: `**text**`
- Italic text: `*text*`
- Unordered lists: `- item`
- Ordered lists: `1. item`
- Tables: `| col | col |` with `|---|---|` separator
- LaTeX: `$inline mode$` or `$$display mode$$`
- Code blocks: triple backticks with optional language identifier
- Blockquotes: `> text`
";

        if (includeQuestionSyntax)
        {
            baseGuide += @"

**Questions:**
Embed questions inline using the following syntax.
Ensure all questions are of type `MULTIPLE_CHOICE` or `WRITTEN_ANSWER`. No other types exist.
Place questions at natural break points after relevant content.
Use only plain text without Markdown syntax in question text, options or correct answers.

```
!!! question-draft type=""MULTIPLE_CHOICE""
    text: ""Question text""
    options:
      - ""Option A""
      - ""Option B""
    correct: 0
    max_score: 1

!!! question-draft type=""WRITTEN_ANSWER""
    text: ""Question text""
    correct_answer: ""exact expected answer""
    max_score: 2

!!! question-draft type=""WRITTEN_ANSWER""
    text: ""Question text""
    mark_scheme: ""Marking criteria for AI grading""
    max_score: 4
```

For questions of type `WRITTEN_ANSWER`, optionally include at most one of the attributes `correct_answer` and `mark_scheme`. Never include both attributes in the same question. If neither attributes are included, the question requires manual marking.
- `correct_answer`: Use for questions where there is only one possible correct answer. Only use a `correct_answer` when the answer is short and factual with either one or two words.
- `mark_scheme`: Use for open-ended questions requiring judgement (e.g., ""Explain why...""). Provides criteria for the teacher or AI to grade. Include: what constitutes a correct response, mark allocation per point, and examples of acceptable answers.

For questions of type `MULTIPLE_CHOICE`, optionally include the attribute `correct`, which stores the zero-based index of the correct option. If this attribute is not included, the question will not be auto-marked.
";
        }

        return baseGuide;
    }
}
