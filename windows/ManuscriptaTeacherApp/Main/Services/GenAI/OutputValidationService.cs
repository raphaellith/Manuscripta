using System.Text.RegularExpressions;
using Main.Models.Dtos;

namespace Main.Services.GenAI;

/// <summary>
/// Validates and refines generated content against Material Encoding Specification.
/// See GenAISpec.md §3F.
/// </summary>
public class OutputValidationService
{
    private readonly OllamaClientService _ollamaClient;
    private const int MaxRefinementIterations = 3;

    public OutputValidationService(OllamaClientService ollamaClient)
    {
        _ollamaClient = ollamaClient;
    }

    /// <summary>
    /// Validates generated content and applies refinement if necessary.
    /// See GenAISpec.md §3F(1).
    /// </summary>
    public async Task<GenerationResult> ValidateAndRefineAsync(
        string content,
        string modelUsed,
        bool useFallback)
    {
        var warnings = ValidateContent(content);

        if (warnings.Count == 0)
        {
            return new GenerationResult { Content = content };
        }

        // §3F(3): Primary model path - apply deterministic fixes
        if (!useFallback)
        {
            content = ApplyDeterministicFixes(content);
            warnings = ValidateContent(content);
            
            return new GenerationResult
            {
                Content = content,
                Warnings = warnings.Count > 0 ? warnings : null
            };
        }

        // §3F(4): Fallback model path - apply iterative refinement
        for (int iteration = 0; iteration < MaxRefinementIterations; iteration++)
        {
            if (warnings.Count == 0) break;

            var refinementPrompt = ConstructRefinementPrompt(content, warnings);
            content = await _ollamaClient.GenerateChatCompletionAsync(modelUsed, refinementPrompt);
            warnings = ValidateContent(content);
        }

        // §3F(4)(d): Apply deterministic fixes after iterations
        content = ApplyDeterministicFixes(content);
        warnings = ValidateContent(content);

        return new GenerationResult
        {
            Content = content,
            Warnings = warnings.Count > 0 ? warnings : null
        };
    }

    /// <summary>
    /// Validates content against Material Encoding Specification.
    /// See GenAISpec.md §3F(2).
    /// </summary>
    private List<ValidationWarning> ValidateContent(string content)
    {
        var warnings = new List<ValidationWarning>();

        // §3F(2)(a): Check for unclosed code blocks
        var codeBlockMatches = Regex.Matches(content, "```");
        if (codeBlockMatches.Count % 2 != 0)
        {
            warnings.Add(new ValidationWarning
            {
                ErrorType = "UNCLOSED_BLOCK",
                Description = "Unclosed code block detected"
            });
        }

        // §3F(2)(b): Check for malformed custom markers
        var questionMarkers = Regex.Matches(content, @"\[QUESTION\s+id=['""]?([^'"">\]]+)['""]?\]");
        foreach (Match match in questionMarkers)
        {
            if (string.IsNullOrWhiteSpace(match.Groups[1].Value))
            {
                warnings.Add(new ValidationWarning
                {
                    ErrorType = "MALFORMED_MARKER",
                    Description = "Question marker with invalid or missing id attribute"
                });
            }
        }

        // §3F(2)(c): Check for malformed attachment markers
        var attachmentMarkers = Regex.Matches(content, @"\[ATTACHMENT\s+id=['""]?([^'"">\]]+)['""]?\]");
        foreach (Match match in attachmentMarkers)
        {
            if (string.IsNullOrWhiteSpace(match.Groups[1].Value))
            {
                warnings.Add(new ValidationWarning
                {
                    ErrorType = "MALFORMED_MARKER",
                    Description = "Attachment marker with invalid or missing id attribute"
                });
            }
        }

        // §3F(2)(a): Check for excessive header levels
        var headerMatches = Regex.Matches(content, @"^#{4,}\s+", RegexOptions.Multiline);
        if (headerMatches.Count > 0)
        {
            warnings.Add(new ValidationWarning
            {
                ErrorType = "INVALID_HEADER_LEVEL",
                Description = "Header levels exceed maximum of H3"
            });
        }

        return warnings;
    }

    /// <summary>
    /// Applies deterministic post-processing fixes to common errors.
    /// See GenAISpec.md §3F(5).
    /// </summary>
    private string ApplyDeterministicFixes(string content)
    {
        // §3F(5)(a): Close unclosed code blocks
        var codeBlockMatches = Regex.Matches(content, "```");
        if (codeBlockMatches.Count % 2 != 0)
        {
            content += "\n```";
        }

        // §3F(5)(b): Normalize header levels to maximum H3
        content = Regex.Replace(content, @"^#{4,}\s+", "### ", RegexOptions.Multiline);

        // §3F(5)(c): Reconstruct malformed question markers where id is parseable
        content = Regex.Replace(
            content,
            @"\[QUESTION\s+id\s*=\s*['""]?([a-fA-F0-9-]+)['""]?[^\]]*\]",
            "[QUESTION id=\"$1\"]"
        );

        // §3F(5)(d): Reconstruct malformed attachment markers where id is parseable
        content = Regex.Replace(
            content,
            @"\[ATTACHMENT\s+id\s*=\s*['""]?([a-fA-F0-9-]+)['""]?[^\]]*\]",
            "[ATTACHMENT id=\"$1\"]"
        );

        // §3F(5)(e): Remove invalid or empty custom markers
        content = Regex.Replace(content, @"\[QUESTION\s+id=['""]?['""]?\]", "");
        content = Regex.Replace(content, @"\[ATTACHMENT\s+id=['""]?['""]?\]", "");

        return content;
    }

    /// <summary>
    /// Constructs a refinement prompt for iterative improvement.
    /// See GenAISpec.md §3F(4)(a).
    /// </summary>
    private string ConstructRefinementPrompt(string content, List<ValidationWarning> warnings)
    {
        var warningsList = string.Join("\n", warnings.Select(w => $"- {w.Description}"));

        return $@"The following content has validation errors. Please fix ONLY these errors while preserving the content and meaning:

{warningsList}

Original content:
{content}

Return the corrected content with the errors fixed:";
    }
}
