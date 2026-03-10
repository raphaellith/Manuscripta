using System.Text.RegularExpressions;
using Main.Models.Dtos;
using Main.Models.Entities.Questions;
using Main.Models.Enums;

namespace Main.Services.GenAI;

/// <summary>
/// Extracts question drafts from generated content and creates QuestionEntity objects.
/// See GenAISpec.md §3B(4a).
/// </summary>
public class QuestionExtractionService
{
    private readonly IQuestionService _questionService;

    private const string QuestionDraftPattern = @"!!! question-draft\s+type=""([^""]+)""\s*((?:[^\n]|\n(?!!!! ))+?)(?=\n!!!|\Z)";

    public QuestionExtractionService(IQuestionService questionService)
    {
        _questionService = questionService ?? throw new ArgumentNullException(nameof(questionService));
    }

    /// <summary>
    /// Extracts and creates questions from generated worksheet content.
    /// See GenAISpec.md §3B(4a).
    /// </summary>
    /// <param name="content">The generated worksheet content containing question-draft markers</param>
    /// <param name="materialId">The ID of the material being created</param>
    /// <returns>The modified content with question markers and any validation warnings</returns>
    public async Task<QuestionExtractionResult> ExtractAndCreateQuestionsAsync(string content, Guid materialId)
    {
        var createdQuestionIds = new List<Guid>();
        var warnings = new List<ValidationWarning>();
        var modifiedContent = content;

        // §3B(4a)(a): Find all question-draft markers
        var regex = new Regex(QuestionDraftPattern, RegexOptions.Multiline);
        var matches = regex.Matches(content);

        foreach (Match match in matches)
        {
            var fullMarker = match.Value;
            var type = match.Groups[1].Value;
            var properties = match.Groups[2].Value;

            try
            {
                // §3B(4a)(a): Parse the question draft
                var (questionType, questionData) = ParseQuestionDraft(type, properties);

                // §3B(4a)(b): Create the question entity
                var questionId = Guid.NewGuid();
                var questionEntity = CreateQuestionEntity(questionId, materialId, questionType, questionData);

                // Create the question in the database
                await _questionService.CreateQuestionAsync(questionEntity);

                createdQuestionIds.Add(questionId);

                // §3B(4a)(c): Replace the marker with a valid question marker
                var replacementMarker = $@"!!! question id=""{questionId}""";
                modifiedContent = modifiedContent.Replace(fullMarker, replacementMarker);
            }
            catch (Exception ex)
            {
                // §3B(4a)(d): Handle parsing failures
                warnings.Add(new ValidationWarning
                {
                    ErrorType = "QUESTION_DRAFT_PARSE_ERROR",
                    Description = $"Failed to parse question-draft marker: {ex.Message}"
                });

                // §3B(4a)(d)(i): Remove the malformed marker
                modifiedContent = modifiedContent.Replace(fullMarker, string.Empty);
            }
        }

        return new QuestionExtractionResult
        {
            ModifiedContent = modifiedContent,
            CreatedQuestionIds = createdQuestionIds,
            Warnings = warnings
        };
    }

    /// <summary>
    /// Parses a question-draft block to extract type and properties.
    /// See GenAISpec.md §3B(4a)(a).
    /// </summary>
    private (QuestionType, QuestionData) ParseQuestionDraft(string typeString, string properties)
    {
        if (!Enum.TryParse<QuestionType>(typeString, out var questionType))
            throw new InvalidOperationException($"Invalid question type: {typeString}");

        var questionData = ParseQuestionProperties(properties);
        return (questionType, questionData);
    }

    /// <summary>
    /// Parses the YAML-like properties from a question-draft block.
    /// </summary>
    private QuestionData ParseQuestionProperties(string propertiesBlock)
    {
        var data = new QuestionData();
        var lines = propertiesBlock.Split('\n', StringSplitOptions.RemoveEmptyEntries);

        for (int i = 0; i < lines.Length; i++)
        {
            var trimmedLine = lines[i].Trim();
            if (string.IsNullOrEmpty(trimmedLine))
                continue;

            if (trimmedLine.StartsWith("text:"))
            {
                data.QuestionText = ExtractQuotedValue(trimmedLine, "text:");
            }
            else if (trimmedLine.StartsWith("options:"))
            {
                // Check if options are inline (array format) or multi-line (YAML list format)
                var inlineValue = trimmedLine.Substring("options:".Length).Trim();
                if (inlineValue.StartsWith("["))
                {
                    // Inline array format: options: ["Option 1", "Option 2"]
                    data.Options = ParseList(trimmedLine, "options:");
                }
                else
                {
                    // Multi-line YAML list format:
                    // options:
                    //   - "Option A"
                    //   - "Option B"
                    data.Options = ParseYamlList(lines, ref i);
                }
            }
            else if (trimmedLine.StartsWith("correct:"))
            {
                var correctIndexStr = ExtractValue(trimmedLine, "correct:");
                if (int.TryParse(correctIndexStr, out var correctIndex))
                {
                    data.CorrectAnswerIndex = correctIndex;
                }
            }
            else if (trimmedLine.StartsWith("correct_answer:"))
            {
                data.CorrectAnswer = ExtractQuotedValue(trimmedLine, "correct_answer:");
            }
            else if (trimmedLine.StartsWith("mark_scheme:"))
            {
                data.MarkScheme = ExtractQuotedValue(trimmedLine, "mark_scheme:");
            }
            else if (trimmedLine.StartsWith("max_score:"))
            {
                var scoreStr = ExtractValue(trimmedLine, "max_score:");
                if (int.TryParse(scoreStr, out var score))
                {
                    data.MaxScore = score;
                }
            }
        }

        if (string.IsNullOrEmpty(data.QuestionText))
            throw new InvalidOperationException("Question text is required");

        return data;
    }

    /// <summary>
    /// Extracts a quoted value from a property line (e.g., text: "value").
    /// </summary>
    private string ExtractQuotedValue(string line, string key)
    {
        var startIndex = line.IndexOf($"{key} \"") + key.Length + 2;
        var endIndex = line.LastIndexOf('"');

        if (startIndex > key.Length + 1 && endIndex > startIndex)
        {
            return line[startIndex..endIndex];
        }

        return string.Empty;
    }

    /// <summary>
    /// Extracts an unquoted value from a property line (e.g., correct: 0).
    /// </summary>
    private string ExtractValue(string line, string key)
    {
        var startIndex = line.IndexOf(key) + key.Length;
        return line[startIndex..].Trim();
    }

    /// <summary>
    /// Parses a list from a property line (e.g., options: ["Option 1", "Option 2"]).
    /// </summary>
    private List<string> ParseList(string line, string key)
    {
        var startIndex = line.IndexOf('[');
        var endIndex = line.LastIndexOf(']');

        if (startIndex < 0 || endIndex <= startIndex)
            return new List<string>();

        var listContent = line[(startIndex + 1)..endIndex];
        var items = new List<string>();

        var itemPattern = new Regex(@"""([^""]*?)""");
        foreach (Match match in itemPattern.Matches(listContent))
        {
            items.Add(match.Groups[1].Value);
        }

        return items;
    }

    /// <summary>
    /// Parses a YAML list format spanning multiple lines.
    /// Expected format:
    ///   - "Item 1"
    ///   - "Item 2"
    /// </summary>
    /// <param name="lines">All lines from the properties block</param>
    /// <param name="currentIndex">Current line index (will be updated to the last consumed line)</param>
    private List<string> ParseYamlList(string[] lines, ref int currentIndex)
    {
        var items = new List<string>();
        
        // Move to the next line after "options:"
        currentIndex++;
        
        // Parse subsequent lines that start with "-"
        while (currentIndex < lines.Length)
        {
            var line = lines[currentIndex];
            var trimmedLine = line.Trim();
            
            // Check if line starts with "-" (list item marker)
            if (trimmedLine.StartsWith("-"))
            {
                // Extract the quoted value after "-"
                var itemPattern = new Regex(@"-\s*""([^""]+)""");
                var match = itemPattern.Match(trimmedLine);
                
                if (match.Success)
                {
                    items.Add(match.Groups[1].Value);
                    currentIndex++;
                }
                else
                {
                    // Invalid format, stop parsing this list
                    break;
                }
            }
            else
            {
                // No longer a list item, decrement index to allow parsing of this line
                currentIndex--;
                break;
            }
        }
        
        return items;
    }

    /// <summary>
    /// Creates an appropriate QuestionEntity based on the parsed data.
    /// See GenAISpec.md §3B(4a)(b).
    /// </summary>
    private QuestionEntity CreateQuestionEntity(Guid questionId, Guid materialId, QuestionType type, QuestionData data)
    {
        return type switch
        {
            QuestionType.MULTIPLE_CHOICE => CreateMultipleChoiceQuestion(questionId, materialId, data),
            QuestionType.WRITTEN_ANSWER => CreateWrittenAnswerQuestion(questionId, materialId, data),
            _ => throw new InvalidOperationException($"Unsupported question type: {type}")
        };
    }

    /// <summary>
    /// Creates a MultipleChoiceQuestionEntity from parsed data.
    /// See GenAISpec.md §3B(4a)(b) and per AdditionalValidationRules §2E(2)(a): MULTIPLE_CHOICE questions must not have MarkScheme.
    /// Markdown syntax is stripped from question text and options to ensure plain-text output.
    /// </summary>
    private QuestionEntity CreateMultipleChoiceQuestion(Guid questionId, Guid materialId, QuestionData data)
    {
        if (string.IsNullOrEmpty(data.QuestionText))
            throw new InvalidOperationException("Question text is required");

        if (data.Options == null || data.Options.Count == 0)
            throw new InvalidOperationException("Multiple choice questions must have at least one option");

        // Strip Markdown syntax from LLM-generated text fields
        var strippedOptions = data.Options
            .Select(MarkdownStrippingHelper.StripMarkdownSyntax)
            .ToList();

        return new MultipleChoiceQuestionEntity(
            questionId,
            materialId,
            MarkdownStrippingHelper.StripMarkdownSyntax(data.QuestionText!),
            strippedOptions,
            data.CorrectAnswerIndex,
            data.MaxScore
        );
    }

    /// <summary>
    /// Creates a WrittenAnswerQuestionEntity from parsed data.
    /// See GenAISpec.md §3B(4a)(b) and per AdditionalValidationRules §2E(2)(b): Cannot have both MarkScheme and CorrectAnswer.
    /// Markdown syntax is stripped from question text, correct answer, and mark scheme to ensure plain-text output.
    /// </summary>
    private QuestionEntity CreateWrittenAnswerQuestion(Guid questionId, Guid materialId, QuestionData data)
    {
        if (string.IsNullOrEmpty(data.QuestionText))
            throw new InvalidOperationException("Question text is required");

        // §2E(2)(b): Cannot have both MarkScheme and CorrectAnswer
        if (!string.IsNullOrEmpty(data.MarkScheme) && !string.IsNullOrEmpty(data.CorrectAnswer))
            throw new InvalidOperationException("Cannot have both mark_scheme and correct_answer for a written answer question");

        // Strip Markdown syntax from LLM-generated text fields
        var strippedCorrectAnswer = data.CorrectAnswer != null
            ? MarkdownStrippingHelper.StripMarkdownSyntax(data.CorrectAnswer)
            : null;
        var strippedMarkScheme = data.MarkScheme != null
            ? MarkdownStrippingHelper.StripMarkdownSyntax(data.MarkScheme)
            : null;

        return new WrittenAnswerQuestionEntity(
            questionId,
            materialId,
            MarkdownStrippingHelper.StripMarkdownSyntax(data.QuestionText!),
            strippedCorrectAnswer,
            strippedMarkScheme,
            data.MaxScore
        );
    }

    /// <summary>
    /// Internal class to hold parsed question data.
    /// </summary>
    private class QuestionData
    {
        public string? QuestionText { get; set; }
        public List<string>? Options { get; set; }
        public int? CorrectAnswerIndex { get; set; }
        public string? CorrectAnswer { get; set; }
        public string? MarkScheme { get; set; }
        public int? MaxScore { get; set; }
    }
}

/// <summary>
/// Result of question extraction from generated content.
/// </summary>
public class QuestionExtractionResult
{
    /// <summary>
    /// The generated content with question-draft markers replaced by question markers.
    /// See GenAISpec.md §3B(4a)(c).
    /// </summary>
    public required string ModifiedContent { get; set; }

    /// <summary>
    /// The list of created question IDs.
    /// See AdditionalValidationRules §3AC(2)(b).
    /// </summary>
    public required List<Guid> CreatedQuestionIds { get; set; }

    /// <summary>
    /// Any validation warnings from parsing failures.
    /// See GenAISpec.md §3B(4a)(d).
    /// </summary>
    public required List<ValidationWarning> Warnings { get; set; }
}
