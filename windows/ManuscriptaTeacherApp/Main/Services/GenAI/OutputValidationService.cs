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
        var warnings = await ValidateContentAsync(content);

        if (warnings.Count == 0)
        {
            return new GenerationResult { Content = content };
        }

        // §3F(3): Primary model path - apply deterministic fixes
        if (!useFallback)
        {
            content = await ApplyDeterministicFixesAsync(content);
            warnings = await ValidateContentAsync(content);
            
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
            warnings = await ValidateContentAsync(content);
        }

        // §3F(4)(d): Apply deterministic fixes after iterations
        content = await ApplyDeterministicFixesAsync(content);
        warnings = await ValidateContentAsync(content);

        return new GenerationResult
        {
            Content = content,
            Warnings = warnings.Count > 0 ? warnings : null
        };
    }

    /// <summary>
    /// Validates content against Material Encoding Specification.
    /// See GenAISpec.md §3F(2) and Material Encoding Specification §4.
    /// </summary>
    private async Task<List<ValidationWarning>> ValidateContentAsync(string content)
    {
        var warnings = new List<ValidationWarning>();

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

        // §3F(2)(b): Check for malformed question markers with invalid or missing id (Admonition syntax per Material Encoding §4)
        // Valid: !!! question id="uuid" or !!! question id='uuid'
        var validQuestionPatterns = Regex.Matches(content, @"!!![\s]+question[\s]+id\s*=\s*['""]([^'""]+)['""]");
        
        // Find any question markers that are NOT valid (exclude question-draft markers used for AI generation)
        var allQuestionMarkers = Regex.Matches(content, @"!!![\s]+question(?!-draft)(?![\s]+id\s*=\s*['""])");
        foreach (Match match in allQuestionMarkers)
        {
            var lineNum = FindLineNumberForMatch(content, match);
            warnings.Add(new ValidationWarning
            {
                ErrorType = "MALFORMED_MARKER",
                Description = "Question marker missing or invalid id attribute (Material Encoding Specification §4(4))",
                LineNumber = lineNum
            });
        }

        // §3F(2)(c): Check for malformed PDF embed markers (Admonition syntax per Material Encoding §4(2))
        var allPdfMarkers = Regex.Matches(content, @"!!![\s]+pdf(?![\s]+id\s*=\s*['""])");
        foreach (Match match in allPdfMarkers)
        {
            var lineNum = FindLineNumberForMatch(content, match);
            warnings.Add(new ValidationWarning
            {
                ErrorType = "MALFORMED_MARKER",
                Description = "PDF marker missing or invalid id attribute (Material Encoding Specification §4(2))",
                LineNumber = lineNum
            });
        }

        // §3F(2)(b): Check for invalid references - questions that don't exist (Material Encoding Specification §4(4))
        var validQuestionIds = Regex.Matches(content, @"!!![\s]+question[\s]+id\s*=\s*['""]([a-fA-F0-9-]+)['""]");
        foreach (Match match in validQuestionIds)
        {
            var idString = match.Groups[1].Value;
            if (Guid.TryParse(idString, out var questionId))
            {
                var questionExists = await _dbContext.Questions.AnyAsync(q => q.Id == questionId);
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

        // §3F(2)(c): Check for invalid references - attachments that don't exist (Material Encoding Specification §3)
        // Attachments use markdown image syntax: ![alt text](/attachments/{id})
        var validAttachmentIds = Regex.Matches(content, @"!\[([^\]]*)\]\(/attachments/([a-fA-F0-9-]+)\)");
        foreach (Match match in validAttachmentIds)
        {
            var idString = match.Groups[2].Value;
            if (Guid.TryParse(idString, out var attachmentId))
            {
                var attachmentExists = await _dbContext.Attachments.AnyAsync(a => a.Id == attachmentId);
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

        // §3F(2)(d): Check for excessive header levels (Material Encoding Specification §2(1)(d))
        var headerMatches = Regex.Matches(content, @"^#{4,}\s+", RegexOptions.Multiline);
        var headerWarnings = headerMatches.Select(match => {
            var lineNum = FindLineNumberForMatch(content, match);
            return new ValidationWarning
            {
                ErrorType = "INVALID_HEADER_LEVEL",
                Description = "Header levels exceed maximum of H3 (Material Encoding Specification §2(1)(d))",
                LineNumber = lineNum
            };
        }).ToList();
        warnings.AddRange(headerWarnings);

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
    /// See GenAISpec.md §3F(5) and Material Encoding Specification §4.
    /// </summary>
    private async Task<string> ApplyDeterministicFixesAsync(string content)
    {
        // §3F(5)(a): Close unclosed code blocks
        var codeBlockMatches = Regex.Matches(content, "```");
        if (codeBlockMatches.Count % 2 != 0)
        {
            content += "\n```";
        }

        // §3F(5)(b): Normalize header levels to maximum H3
        content = Regex.Replace(content, @"^#{4,}\s+", "### ", RegexOptions.Multiline);

        // §3F(5)(c): Reconstruct malformed question markers where id is parseable (Material Encoding Specification §4(4))
        // Convert from malformed patterns to: !!! question id="uuid" (exclude question-draft markers)
        content = Regex.Replace(
            content,
            @"!!![\s]+question(?!-draft)[\s]*(?:id\s*=\s*)?['""']?([a-fA-F0-9-]+)['""']?",
            "!!! question id=\"$1\""
        );

        // §3F(5)(d): Reconstruct malformed PDF embed markers where id is parseable (Material Encoding Specification §4(2))
        content = Regex.Replace(
            content,
            @"!!![\s]+pdf[\s]*(?:id\s*=\s*)?['""]?([a-fA-F0-9-]+)['""]?",
            "!!! pdf id=\"$1\""
        );

        // §3F(5)(e): Remove invalid or empty custom markers
        // Remove empty question markers (exclude question-draft markers)
        content = Regex.Replace(content, @"!!![\s]+question(?!-draft)\s*(?:id\s*=\s*)?['""']?['""']?", "");
        // Remove empty PDF markers
        content = Regex.Replace(content, @"!!![\s]+pdf\s*(?:id\s*=\s*)?['""]?['""]?", "");
        
        // §3F(5)(e)(i): Remove PDF markers where attachment entity or file does not exist
        content = await RemoveInvalidPdfMarkersAsync(content);
        
        // §3F(5)(e)(ii): Remove question markers where question entity does not exist
        content = await RemoveInvalidQuestionMarkersAsync(content);
        
        // §3F(5)(e)(i): Remove attachment image references where attachment does not exist (Material Encoding Specification §3)
        content = await RemoveInvalidAttachmentReferencesAsync(content);

        return content;
    }

    /// <summary>
    /// Removes PDF embed markers where the referenced entity or file does not exist.
    /// See GenAISpec.md §3F(5)(e)(i) and Material Encoding Specification §4(2).
    /// </summary>
    private async Task<string> RemoveInvalidPdfMarkersAsync(string content)
    {
        var pdfMarkers = Regex.Matches(content, @"!!![\s]+pdf[\s]+id\s*=\s*['""]([a-fA-F0-9-]+)['""]");
        
        foreach (Match match in pdfMarkers)
        {
            var idString = match.Groups[1].Value;
            if (Guid.TryParse(idString, out var attachmentId))
            {
                // Check if attachment entity exists
                var attachmentExists = await _dbContext.Attachments.AnyAsync(a => a.Id == attachmentId);
                
                if (attachmentExists)
                {
                    // Check if attachment file exists
                    var attachment = await _dbContext.Attachments.FindAsync(attachmentId);
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
    /// Removes attachment image references where the referenced entity or file does not exist.
    /// See GenAISpec.md §3F(5)(e)(i) and Material Encoding Specification §3.
    /// </summary>
    private async Task<string> RemoveInvalidAttachmentReferencesAsync(string content)
    {
        var attachmentMarkers = Regex.Matches(content, @"!\[([^\]]*)\]\(/attachments/([a-fA-F0-9-]+)\)");
        
        foreach (Match match in attachmentMarkers)
        {
            var idString = match.Groups[2].Value;
            if (Guid.TryParse(idString, out var attachmentId))
            {
                // Check if attachment entity exists
                var attachmentExists = await _dbContext.Attachments.AnyAsync(a => a.Id == attachmentId);
                
                if (attachmentExists)
                {
                    // Check if attachment file exists
                    var attachment = await _dbContext.Attachments.FindAsync(attachmentId);
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
    /// See GenAISpec.md §3F(5)(e)(ii) and Material Encoding Specification §4(4).
    /// </summary>
    private async Task<string> RemoveInvalidQuestionMarkersAsync(string content)
    {
        var questionMarkers = Regex.Matches(content, @"!!![\s]+question[\s]+id\s*=\s*['""]([a-fA-F0-9-]+)['""]");
        
        foreach (Match match in questionMarkers)
        {
            var idString = match.Groups[1].Value;
            if (Guid.TryParse(idString, out var questionId))
            {
                // Check if question entity exists
                var questionExists = await _dbContext.Questions.AnyAsync(q => q.Id == questionId);
                
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
        var markdownSyntaxGuide = MarkdownSyntaxGuide.Get();

        return $@"
TASK:
Fix all listed syntax errors in the provided document, in accordance with the Markdown syntax described below.
Preserve the content and meaning of the original document.
Only fix errors if they disobey the Markdown syntax described below.
Return the corrected document with all syntax errors fixed.


ORIGINAL DOCUMENT:
{content}


LIST OF SYNTAX ERRORS:
{warningsList}


MARKDOWN SYNTAX:
{markdownSyntaxGuide}
";
    }
}
