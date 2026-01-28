using System.Text.RegularExpressions;
using Main.Data;
using Main.Models.Dtos;
using Microsoft.EntityFrameworkCore;

namespace Main.Services.GenAI;

/// <summary>
/// Validates and refines generated content against Material Encoding Specification.
/// See GenAISpec.md §3F.
/// </summary>
public class OutputValidationService
{
    private readonly OllamaClientService _ollamaClient;
    private readonly MainDbContext _dbContext;
    private readonly IFileService _fileService;
    private const int MaxRefinementIterations = 3;

    public OutputValidationService(
        OllamaClientService ollamaClient,
        MainDbContext dbContext,
        IFileService fileService)
    {
        _ollamaClient = ollamaClient;
        _dbContext = dbContext;
        _fileService = fileService;
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
        var lines = content.Split(new[] { "\r\n", "\r", "\n" }, StringSplitOptions.None);

        // §3F(2)(a): Check for unclosed code blocks
        var codeBlockMatches = Regex.Matches(content, "```");
        if (codeBlockMatches.Count % 2 != 0)
        {
            var firstUnclosedLine = FindLineNumberForMatch(content, codeBlockMatches[codeBlockMatches.Count - 1]);
            warnings.Add(new ValidationWarning
            {
                ErrorType = "UNCLOSED_BLOCK",
                Description = "Unclosed code block detected",
                LineNumber = firstUnclosedLine
            });
        }

        // §3F(2)(b): Check for malformed question markers with invalid or missing id
        var questionMarkers = Regex.Matches(content, @"\[QUESTION\s+id=['""]?([^'"">\]]+)['""]?\]");
        foreach (Match match in questionMarkers)
        {
            if (string.IsNullOrWhiteSpace(match.Groups[1].Value))
            {
                var lineNum = FindLineNumberForMatch(content, match);
                warnings.Add(new ValidationWarning
                {
                    ErrorType = "MALFORMED_MARKER",
                    Description = "Question marker with invalid or missing id attribute",
                    LineNumber = lineNum
                });
            }
        }

        // §3F(2)(c): Check for malformed attachment markers with invalid or missing id
        var attachmentMarkers = Regex.Matches(content, @"\[ATTACHMENT\s+id=['""]?([^'"">\]]+)['""]?\]");
        foreach (Match match in attachmentMarkers)
        {
            if (string.IsNullOrWhiteSpace(match.Groups[1].Value))
            {
                var lineNum = FindLineNumberForMatch(content, match);
                warnings.Add(new ValidationWarning
                {
                    ErrorType = "MALFORMED_MARKER",
                    Description = "Attachment marker with invalid or missing id attribute",
                    LineNumber = lineNum
                });
            }
        }

        // §3F(2)(b): Check for invalid references - questions that don't exist
        var validQuestionIds = Regex.Matches(content, @"\[QUESTION\s+id=['""]?([a-fA-F0-9-]+)['""]?\]");
        foreach (Match match in validQuestionIds)
        {
            var idString = match.Groups[1].Value;
            if (Guid.TryParse(idString, out var questionId))
            {
                var questionExists = _dbContext.Questions.Any(q => q.Id == questionId);
                if (!questionExists)
                {
                    var lineNum = FindLineNumberForMatch(content, match);
                    warnings.Add(new ValidationWarning
                    {
                        ErrorType = "INVALID_REFERENCE",
                        Description = $"Question reference {questionId} does not exist",
                        LineNumber = lineNum
                    });
                }
            }
        }

        // §3F(2)(c): Check for invalid references - attachments that don't exist
        var validAttachmentIds = Regex.Matches(content, @"\[ATTACHMENT\s+id=['""]?([a-fA-F0-9-]+)['""]?\]");
        foreach (Match match in validAttachmentIds)
        {
            var idString = match.Groups[1].Value;
            if (Guid.TryParse(idString, out var attachmentId))
            {
                var attachmentExists = _dbContext.Attachments.Any(a => a.Id == attachmentId);
                if (!attachmentExists)
                {
                    var lineNum = FindLineNumberForMatch(content, match);
                    warnings.Add(new ValidationWarning
                    {
                        ErrorType = "INVALID_REFERENCE",
                        Description = $"Attachment reference {attachmentId} does not exist",
                        LineNumber = lineNum
                    });
                }
            }
        }

        // §3F(2)(b): Check for excessive header levels
        var headerMatches = Regex.Matches(content, @"^#{4,}\s+", RegexOptions.Multiline);
        foreach (Match match in headerMatches)
        {
            var lineNum = FindLineNumberForMatch(content, match);
            warnings.Add(new ValidationWarning
            {
                ErrorType = "INVALID_HEADER_LEVEL",
                Description = "Header levels exceed maximum of H3",
                LineNumber = lineNum
            });
        }

        return warnings;
    }

    /// <summary>
    /// Helper method to find the line number for a regex match.
    /// </summary>
    private int FindLineNumberForMatch(string content, Match match)
    {
        var beforeMatch = content.Substring(0, match.Index);
        var lineNumber = beforeMatch.Count(c => c == '\n') + 1;
        return lineNumber;
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
        
        // §3F(5)(e)(i): Remove attachment markers where attachment entity or file does not exist
        content = RemoveInvalidAttachmentMarkers(content);
        
        // §3F(5)(e)(ii): Remove question markers where question entity does not exist
        content = RemoveInvalidQuestionMarkers(content);

        return content;
    }

    /// <summary>
    /// Removes attachment markers where the referenced entity or file does not exist.
    /// See GenAISpec.md §3F(5)(e)(i).
    /// </summary>
    private string RemoveInvalidAttachmentMarkers(string content)
    {
        var attachmentMarkers = Regex.Matches(content, @"\[ATTACHMENT\s+id=['""]?([a-fA-F0-9-]+)['""]?\]");
        
        foreach (Match match in attachmentMarkers)
        {
            var idString = match.Groups[1].Value;
            if (Guid.TryParse(idString, out var attachmentId))
            {
                // Check if attachment entity exists
                var attachmentExists = _dbContext.Attachments.Any(a => a.Id == attachmentId);
                
                if (attachmentExists)
                {
                    // Check if attachment file exists
                    var attachment = _dbContext.Attachments.Find(attachmentId);
                    if (attachment != null)
                    {
                        var filePath = _fileService.GetAttachmentFilePath(attachment.Id, attachment.FileExtension);
                        if (!_fileService.FileExists(filePath))
                        {
                            attachmentExists = false;
                        }
                    }
                }
                
                if (!attachmentExists)
                {
                    content = content.Replace(match.Value, "");
                }
            }
        }
        
        return content;
    }

    /// <summary>
    /// Removes question markers where the referenced entity does not exist.
    /// See GenAISpec.md §3F(5)(e)(ii).
    /// </summary>
    private string RemoveInvalidQuestionMarkers(string content)
    {
        var questionMarkers = Regex.Matches(content, @"\[QUESTION\s+id=['""]?([a-fA-F0-9-]+)['""]?\]");
        
        foreach (Match match in questionMarkers)
        {
            var idString = match.Groups[1].Value;
            if (Guid.TryParse(idString, out var questionId))
            {
                // Check if question entity exists
                var questionExists = _dbContext.Questions.Any(q => q.Id == questionId);
                
                if (!questionExists)
                {
                    content = content.Replace(match.Value, "");
                }
            }
        }
        
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
