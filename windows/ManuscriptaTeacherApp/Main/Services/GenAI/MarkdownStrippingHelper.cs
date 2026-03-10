using System.Text.RegularExpressions;

namespace Main.Services.GenAI;

/// <summary>
/// Provides utility methods for stripping Markdown formatting syntax from
/// LLM-generated text, leaving only plain text.
/// Used by FeedbackGenerationService (GenAISpec §3D) and
/// QuestionExtractionService (GenAISpec §3B(4a)) to sanitise generated output.
/// </summary>
public static class MarkdownStrippingHelper
{
    /// <summary>
    /// Strips Markdown formatting syntax from text, leaving only plain text.
    /// This includes headers, bold, italic, code blocks, links, lists,
    /// blockquotes, horizontal rules, and stray HTML tags.
    /// </summary>
    /// <param name="text">The text potentially containing Markdown syntax.</param>
    /// <returns>Plain text with all Markdown formatting removed, or the
    /// original value when <paramref name="text"/> is null or empty.</returns>
    public static string StripMarkdownSyntax(string text)
    {
        if (string.IsNullOrEmpty(text))
        {
            return text;
        }

        // Remove code blocks (```...```)
        text = Regex.Replace(text, @"```[\s\S]*?```", "", RegexOptions.Multiline);

        // Remove inline code (`...`)
        text = Regex.Replace(text, @"`([^`]+)`", "$1");

        // Remove headers (# ## ### etc.)
        text = Regex.Replace(text, @"^#{1,6}\s+(.+)$", "$1", RegexOptions.Multiline);

        // Remove bold (**text** or __text__)
        text = Regex.Replace(text, @"\*\*(.+?)\*\*", "$1");
        text = Regex.Replace(text, @"__(.+?)__", "$1");

        // Remove italic (*text* or _text_)
        text = Regex.Replace(text, @"\*(.+?)\*", "$1");
        text = Regex.Replace(text, @"_(.+?)_", "$1");

        // Remove strikethrough (~~text~~)
        text = Regex.Replace(text, @"~~(.+?)~~", "$1");

        // Remove links [text](url) – keep the text part
        text = Regex.Replace(text, @"\[([^\]]+)\]\([^\)]+\)", "$1");

        // Remove images ![alt](url)
        text = Regex.Replace(text, @"!\[([^\]]*)\]\([^\)]+\)", "$1");

        // Remove unordered list markers (- or * or +)
        text = Regex.Replace(text, @"^[\s]*[-*+]\s+", "", RegexOptions.Multiline);

        // Remove ordered list markers (1. 2. etc.)
        text = Regex.Replace(text, @"^[\s]*\d+\.\s+", "", RegexOptions.Multiline);

        // Remove blockquotes (>)
        text = Regex.Replace(text, @"^>\s+", "", RegexOptions.Multiline);

        // Remove horizontal rules (--- or *** or ___)
        text = Regex.Replace(text, @"^[\s]*[-*_]{3,}[\s]*$", "", RegexOptions.Multiline);

        // Remove HTML tags (in case the LLM outputs any)
        text = Regex.Replace(text, @"<[^>]+>", "");

        // Clean up multiple consecutive blank lines
        text = Regex.Replace(text, @"\n{3,}", "\n\n");

        return text.Trim();
    }
}
