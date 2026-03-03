using System;
using System.Linq;
using System.Threading.Tasks;
using Main.Models.Entities.Questions;
using Main.Services;
using Main.Services.GenAI;
using Moq;
using Xunit;

namespace MainTests.ServicesTests.GenAI;

/// <summary>
/// Spec coverage: GenAISpec Section 3B(4) and Appendix C (question-draft parsing rules).
/// See docs/specifications/GenAISpec.md.
/// </summary>
public class QuestionExtractionServiceTests
{
    /// <summary>
    /// Spec coverage: GenAISpec Section 3B(4)(a)-(c) (parse draft and replace with question marker).
    /// See docs/specifications/GenAISpec.md.
    /// </summary>
    [Fact]
    public async Task ExtractAndCreateQuestionsAsync_MultipleChoice_ReplacesMarkerAndCreatesQuestion()
    {
        var questionService = new Mock<IQuestionService>();
        Guid? createdId = null;
        QuestionEntity? createdQuestion = null;

        questionService.Setup(s => s.CreateQuestionAsync(It.IsAny<QuestionEntity>()))
            .Callback<QuestionEntity>(q =>
            {
                createdId = q.Id;
                createdQuestion = q;
            })
            .ReturnsAsync((QuestionEntity q) => q);

        var service = new QuestionExtractionService(questionService.Object);

        var content = "Intro\n" +
                      "!!! question-draft type=\"MULTIPLE_CHOICE\"\n" +
                      "    text: \"What is 2+2?\"\n" +
                      "    options: [\"3\", \"4\"]\n" +
                      "    correct: 1\n" +
                      "    max_score: 1\n" +
                      "Outro";

        var result = await service.ExtractAndCreateQuestionsAsync(content, Guid.NewGuid());

        Assert.Single(result.CreatedQuestionIds);
        Assert.Equal(createdId, result.CreatedQuestionIds.Single());
        Assert.NotNull(createdQuestion);
        Assert.IsType<MultipleChoiceQuestionEntity>(createdQuestion);
        Assert.Contains($"!!! question id=\"{createdId}\"", result.ModifiedContent);
        Assert.Empty(result.Warnings);
    }

    /// <summary>
    /// Spec coverage: GenAISpec Appendix C (mutual exclusivity of correct_answer and mark_scheme).
    /// See docs/specifications/GenAISpec.md.
    /// </summary>
    [Fact]
    public async Task ExtractAndCreateQuestionsAsync_WrittenAnswerWithBothAnswers_ProducesWarningAndRemovesMarker()
    {
        var questionService = new Mock<IQuestionService>();
        var service = new QuestionExtractionService(questionService.Object);

        var content = "!!! question-draft type=\"WRITTEN_ANSWER\"\n" +
                      "    text: \"Explain gravity\"\n" +
                      "    correct_answer: \"It is a force\"\n" +
                      "    mark_scheme: \"Award points for describing attraction\"\n" +
                      "    max_score: 4";

        var result = await service.ExtractAndCreateQuestionsAsync(content, Guid.NewGuid());

        Assert.Empty(result.CreatedQuestionIds);
        Assert.Empty(result.ModifiedContent.Trim());
        Assert.Single(result.Warnings);
        Assert.Contains("mark_scheme", result.Warnings[0].Description);
        questionService.Verify(s => s.CreateQuestionAsync(It.IsAny<QuestionEntity>()), Times.Never);
    }

    /// <summary>
    /// Spec coverage: GenAISpec Appendix C (invalid question types are rejected).
    /// See docs/specifications/GenAISpec.md.
    /// </summary>
    [Fact]
    public async Task ExtractAndCreateQuestionsAsync_InvalidType_ProducesWarningAndRemovesMarker()
    {
        var questionService = new Mock<IQuestionService>();
        var service = new QuestionExtractionService(questionService.Object);

        var content = "!!! question-draft type=\"TRUE_FALSE\"\n" +
                      "    text: \"Is the sky blue?\"\n" +
                      "    options: [\"True\", \"False\"]";

        var result = await service.ExtractAndCreateQuestionsAsync(content, Guid.NewGuid());

        Assert.Empty(result.CreatedQuestionIds);
        Assert.Empty(result.ModifiedContent.Trim());
        Assert.Single(result.Warnings);
        Assert.Contains("Invalid question type", result.Warnings[0].Description);
        questionService.Verify(s => s.CreateQuestionAsync(It.IsAny<QuestionEntity>()), Times.Never);
    }

    /// <summary>
    /// Spec coverage: GenAISpec Appendix C (MULTIPLE_CHOICE example from MarkdownSyntaxGuide).
    /// See docs/specifications/GenAISpec.md and MarkdownSyntaxGuide.cs.
    /// </summary>
    [Fact]
    public async Task ExtractAndCreateQuestionsAsync_MultipleChoiceMarkdownSyntaxGuideFormat_CreatesCorrectEntity()
    {
        var questionService = new Mock<IQuestionService>();
        QuestionEntity? createdQuestion = null;

        questionService.Setup(s => s.CreateQuestionAsync(It.IsAny<QuestionEntity>()))
            .Callback<QuestionEntity>(q => createdQuestion = q)
            .ReturnsAsync((QuestionEntity q) => q);

        var service = new QuestionExtractionService(questionService.Object);
        var materialId = Guid.NewGuid();

        // Exact format from MarkdownSyntaxGuide.cs
        var content = "!!! question-draft type=\"MULTIPLE_CHOICE\"\n" +
                      "    text: \"Question text\"\n" +
                      "    options:\n" +
                      "      - \"Option A\"\n" +
                      "      - \"Option B\"\n" +
                      "    correct: 0\n" +
                      "    max_score: 1";

        var result = await service.ExtractAndCreateQuestionsAsync(content, materialId);

        // Verify the question was created
        Assert.Single(result.CreatedQuestionIds);
        Assert.Empty(result.Warnings);
        Assert.NotNull(createdQuestion);
        
        // Verify entity type and properties
        var mcQuestion = Assert.IsType<MultipleChoiceQuestionEntity>(createdQuestion);
        Assert.Equal("Question text", mcQuestion.QuestionText);
        Assert.Equal(materialId, mcQuestion.MaterialId);
        Assert.Equal(Main.Models.Enums.QuestionType.MULTIPLE_CHOICE, mcQuestion.QuestionType);
        Assert.Equal(2, mcQuestion.Options.Count);
        Assert.Equal("Option A", mcQuestion.Options[0]);
        Assert.Equal("Option B", mcQuestion.Options[1]);
        Assert.Equal(0, mcQuestion.CorrectAnswerIndex);
        Assert.Equal(1, mcQuestion.MaxScore);
        Assert.Null(mcQuestion.MarkScheme);
        
        // Verify the marker was replaced
        Assert.Contains($"!!! question id=\"{createdQuestion.Id}\"", result.ModifiedContent);
        Assert.DoesNotContain("question-draft", result.ModifiedContent);
    }

    /// <summary>
    /// Spec coverage: GenAISpec Appendix C (WRITTEN_ANSWER with correct_answer example from MarkdownSyntaxGuide).
    /// See docs/specifications/GenAISpec.md and MarkdownSyntaxGuide.cs.
    /// </summary>
    [Fact]
    public async Task ExtractAndCreateQuestionsAsync_WrittenAnswerWithCorrectAnswerMarkdownSyntaxGuideFormat_CreatesCorrectEntity()
    {
        var questionService = new Mock<IQuestionService>();
        QuestionEntity? createdQuestion = null;

        questionService.Setup(s => s.CreateQuestionAsync(It.IsAny<QuestionEntity>()))
            .Callback<QuestionEntity>(q => createdQuestion = q)
            .ReturnsAsync((QuestionEntity q) => q);

        var service = new QuestionExtractionService(questionService.Object);
        var materialId = Guid.NewGuid();

        // Exact format from MarkdownSyntaxGuide.cs
        var content = "!!! question-draft type=\"WRITTEN_ANSWER\"\n" +
                      "    text: \"Question text\"\n" +
                      "    correct_answer: \"exact expected answer\"\n" +
                      "    max_score: 2";

        var result = await service.ExtractAndCreateQuestionsAsync(content, materialId);

        // Verify the question was created
        Assert.Single(result.CreatedQuestionIds);
        Assert.Empty(result.Warnings);
        Assert.NotNull(createdQuestion);
        
        // Verify entity type and properties
        var waQuestion = Assert.IsType<WrittenAnswerQuestionEntity>(createdQuestion);
        Assert.Equal("Question text", waQuestion.QuestionText);
        Assert.Equal(materialId, waQuestion.MaterialId);
        Assert.Equal(Main.Models.Enums.QuestionType.WRITTEN_ANSWER, waQuestion.QuestionType);
        Assert.Equal("exact expected answer", waQuestion.CorrectAnswer);
        Assert.Null(waQuestion.MarkScheme);
        Assert.Equal(2, waQuestion.MaxScore);
        
        // Verify the marker was replaced
        Assert.Contains($"!!! question id=\"{createdQuestion.Id}\"", result.ModifiedContent);
        Assert.DoesNotContain("question-draft", result.ModifiedContent);
    }

    /// <summary>
    /// Spec coverage: GenAISpec Appendix C (WRITTEN_ANSWER with mark_scheme example from MarkdownSyntaxGuide).
    /// See docs/specifications/GenAISpec.md and MarkdownSyntaxGuide.cs.
    /// </summary>
    [Fact]
    public async Task ExtractAndCreateQuestionsAsync_WrittenAnswerWithMarkSchemeMarkdownSyntaxGuideFormat_CreatesCorrectEntity()
    {
        var questionService = new Mock<IQuestionService>();
        QuestionEntity? createdQuestion = null;

        questionService.Setup(s => s.CreateQuestionAsync(It.IsAny<QuestionEntity>()))
            .Callback<QuestionEntity>(q => createdQuestion = q)
            .ReturnsAsync((QuestionEntity q) => q);

        var service = new QuestionExtractionService(questionService.Object);
        var materialId = Guid.NewGuid();

        // Exact format from MarkdownSyntaxGuide.cs
        var content = "!!! question-draft type=\"WRITTEN_ANSWER\"\n" +
                      "    text: \"Question text\"\n" +
                      "    mark_scheme: \"Marking criteria for AI grading\"\n" +
                      "    max_score: 4";

        var result = await service.ExtractAndCreateQuestionsAsync(content, materialId);

        // Verify the question was created
        Assert.Single(result.CreatedQuestionIds);
        Assert.Empty(result.Warnings);
        Assert.NotNull(createdQuestion);
        
        // Verify entity type and properties
        var waQuestion = Assert.IsType<WrittenAnswerQuestionEntity>(createdQuestion);
        Assert.Equal("Question text", waQuestion.QuestionText);
        Assert.Equal(materialId, waQuestion.MaterialId);
        Assert.Equal(Main.Models.Enums.QuestionType.WRITTEN_ANSWER, waQuestion.QuestionType);
        Assert.Null(waQuestion.CorrectAnswer);
        Assert.Equal("Marking criteria for AI grading", waQuestion.MarkScheme);
        Assert.Equal(4, waQuestion.MaxScore);
        
        // Verify the marker was replaced
        Assert.Contains($"!!! question id=\"{createdQuestion.Id}\"", result.ModifiedContent);
        Assert.DoesNotContain("question-draft", result.ModifiedContent);
    }

    /// <summary>
    /// Spec coverage: GenAISpec Appendix C (MULTIPLE_CHOICE without correct index, auto-marking disabled).
    /// See docs/specifications/GenAISpec.md and MarkdownSyntaxGuide.cs.
    /// </summary>
    [Fact]
    public async Task ExtractAndCreateQuestionsAsync_MultipleChoiceWithoutCorrectIndex_CreatesEntityWithNullCorrectIndex()
    {
        var questionService = new Mock<IQuestionService>();
        QuestionEntity? createdQuestion = null;

        questionService.Setup(s => s.CreateQuestionAsync(It.IsAny<QuestionEntity>()))
            .Callback<QuestionEntity>(q => createdQuestion = q)
            .ReturnsAsync((QuestionEntity q) => q);

        var service = new QuestionExtractionService(questionService.Object);
        var materialId = Guid.NewGuid();

        // Format without 'correct' attribute
        var content = "!!! question-draft type=\"MULTIPLE_CHOICE\"\n" +
                      "    text: \"What is your opinion?\"\n" +
                      "    options:\n" +
                      "      - \"Option A\"\n" +
                      "      - \"Option B\"\n" +
                      "      - \"Option C\"\n" +
                      "    max_score: 2";

        var result = await service.ExtractAndCreateQuestionsAsync(content, materialId);

        // Verify the question was created
        Assert.Single(result.CreatedQuestionIds);
        Assert.Empty(result.Warnings);
        Assert.NotNull(createdQuestion);
        
        // Verify entity properties
        var mcQuestion = Assert.IsType<MultipleChoiceQuestionEntity>(createdQuestion);
        Assert.Equal("What is your opinion?", mcQuestion.QuestionText);
        Assert.Equal(3, mcQuestion.Options.Count);
        Assert.Null(mcQuestion.CorrectAnswerIndex); // No correct answer specified
        Assert.Equal(2, mcQuestion.MaxScore);
    }

    /// <summary>
    /// Spec coverage: GenAISpec Appendix C (WRITTEN_ANSWER without correct_answer or mark_scheme, manual marking required).
    /// See docs/specifications/GenAISpec.md and MarkdownSyntaxGuide.cs.
    /// </summary>
    [Fact]
    public async Task ExtractAndCreateQuestionsAsync_WrittenAnswerWithoutCorrectOrMarkScheme_CreatesEntityWithBothNull()
    {
        var questionService = new Mock<IQuestionService>();
        QuestionEntity? createdQuestion = null;

        questionService.Setup(s => s.CreateQuestionAsync(It.IsAny<QuestionEntity>()))
            .Callback<QuestionEntity>(q => createdQuestion = q)
            .ReturnsAsync((QuestionEntity q) => q);

        var service = new QuestionExtractionService(questionService.Object);
        var materialId = Guid.NewGuid();

        // Format without 'correct_answer' or 'mark_scheme' attributes
        var content = "!!! question-draft type=\"WRITTEN_ANSWER\"\n" +
                      "    text: \"Describe your understanding\"\n" +
                      "    max_score: 5";

        var result = await service.ExtractAndCreateQuestionsAsync(content, materialId);

        // Verify the question was created
        Assert.Single(result.CreatedQuestionIds);
        Assert.Empty(result.Warnings);
        Assert.NotNull(createdQuestion);
        
        // Verify entity properties
        var waQuestion = Assert.IsType<WrittenAnswerQuestionEntity>(createdQuestion);
        Assert.Equal("Describe your understanding", waQuestion.QuestionText);
        Assert.Null(waQuestion.CorrectAnswer);
        Assert.Null(waQuestion.MarkScheme);
        Assert.Equal(5, waQuestion.MaxScore);
    }
}
