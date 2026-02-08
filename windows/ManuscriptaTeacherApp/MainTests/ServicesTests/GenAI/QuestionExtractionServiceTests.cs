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
}
