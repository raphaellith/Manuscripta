using System;
using Main.Models.Entities.Questions;
using Main.Services.GenAI;
using Microsoft.Extensions.DependencyInjection;
using Microsoft.Extensions.Logging;
using Moq;
using Xunit;

namespace MainTests.ServicesTests.GenAI;

/// <summary>
/// Spec coverage: GenAISpec Section 3D(1) (feedback generation gating conditions).
/// See docs/specifications/GenAISpec.md.
/// </summary>
public class FeedbackGenerationServiceTests
{
    /// <summary>
    /// Spec coverage: GenAISpec Section 3D(1)(a)-(b) (written answer with mark scheme).
    /// See docs/specifications/GenAISpec.md.
    /// </summary>
    [Fact]
    public void ShouldGenerateFeedback_WrittenAnswerWithMarkScheme_ReturnsTrue()
    {
        var service = new FeedbackGenerationService(
            null!,
            null!,
            null!,
            new Mock<IServiceScopeFactory>().Object,
            null!,
            null!,
            Mock.Of<ILogger<FeedbackGenerationService>>());

        var question = new WrittenAnswerQuestionEntity(
            Guid.NewGuid(),
            Guid.NewGuid(),
            "Explain photosynthesis",
            correctAnswer: null,
            markScheme: "Award points for key steps");

        Assert.True(service.ShouldGenerateFeedback(question));
    }

    /// <summary>
    /// Spec coverage: GenAISpec Section 3D(1)(b) (missing mark scheme disables generation).
    /// See docs/specifications/GenAISpec.md.
    /// </summary>
    [Fact]
    public void ShouldGenerateFeedback_NoMarkScheme_ReturnsFalse()
    {
        var service = new FeedbackGenerationService(
            null!,
            null!,
            null!,
            new Mock<IServiceScopeFactory>().Object,
            null!,
            null!,
            Mock.Of<ILogger<FeedbackGenerationService>>());

        var question = new WrittenAnswerQuestionEntity(
            Guid.NewGuid(),
            Guid.NewGuid(),
            "Explain gravity",
            correctAnswer: null,
            markScheme: null);

        Assert.False(service.ShouldGenerateFeedback(question));
    }

    /// <summary>
    /// Spec coverage: GenAISpec Section 3D(4) (currently generating response visibility).
    /// When idle, GetCurrentlyGeneratingResponseId returns null.
    /// </summary>
    [Fact]
    public void GetCurrentlyGeneratingResponseId_WhenIdle_ReturnsNull()
    {
        var service = new FeedbackGenerationService(
            null!,
            null!,
            null!,
            new Mock<IServiceScopeFactory>().Object,
            null!,
            null!,
            Mock.Of<ILogger<FeedbackGenerationService>>());

        Assert.Null(service.GetCurrentlyGeneratingResponseId());
    }

    /// <summary>
    /// Spec coverage: GenAISpec §3D(9)(e)(i)-(ii).
    /// Valid MARK line extracts mark and remaining text.
    /// </summary>
    [Fact]
    public void ParseFeedbackResponse_ValidMarkLine_ExtractsMarkAndText()
    {
        var (marks, text) = FeedbackGenerationService.ParseFeedbackResponse(
            "MARK: 3\n\nGood answer. You identified the key points.", maxScore: 5);

        Assert.Equal(3, marks);
        Assert.Equal("Good answer. You identified the key points.", text);
    }

    /// <summary>
    /// Spec coverage: GenAISpec §3D(9)(e)(i).
    /// Mark exceeding MaxScore is clamped per §2F(2)(c).
    /// </summary>
    [Fact]
    public void ParseFeedbackResponse_MarkExceedsMaxScore_ClampedToMax()
    {
        var (marks, text) = FeedbackGenerationService.ParseFeedbackResponse(
            "MARK: 10\n\nExcellent work.", maxScore: 5);

        Assert.Equal(5, marks);
        Assert.Equal("Excellent work.", text);
    }

    /// <summary>
    /// Spec coverage: GenAISpec §3D(9)(e).
    /// No MARK line returns null marks and full text.
    /// </summary>
    [Fact]
    public void ParseFeedbackResponse_NoMarkLine_ReturnsNullMarksAndFullText()
    {
        var raw = "This is just feedback text without a mark.";
        var (marks, text) = FeedbackGenerationService.ParseFeedbackResponse(raw, maxScore: 5);

        Assert.Null(marks);
        Assert.Equal(raw, text);
    }

    /// <summary>
    /// Spec coverage: GenAISpec §3D(9)(e) and Validation Rules §2F(2)(c).
    /// When maxScore is null, MARK line is ignored and full text is returned with null marks.
    /// </summary>
    [Fact]
    public void ParseFeedbackResponse_NoMaxScore_IgnoresMarkLine()
    {
        var (marks, text) = FeedbackGenerationService.ParseFeedbackResponse(
            "MARK: 7\n\nSolid response.", maxScore: null);

        Assert.Null(marks);
        Assert.Equal("MARK: 7\n\nSolid response.", text);
    }

    /// <summary>
    /// Spec coverage: GenAISpec §3D(9)(e).
    /// Empty/null response returns gracefully.
    /// </summary>
    [Theory]
    [InlineData(null)]
    [InlineData("")]
    [InlineData("  ")]
    public void ParseFeedbackResponse_EmptyInput_ReturnsNullMarks(string? input)
    {
        var (marks, text) = FeedbackGenerationService.ParseFeedbackResponse(input!, maxScore: 5);

        Assert.Null(marks);
    }

    /// <summary>
    /// Spec coverage: GenAISpec §3D(9)(e).
    /// MARK: 0 is a valid mark.
    /// </summary>
    [Fact]
    public void ParseFeedbackResponse_ZeroMark_ExtractsZero()
    {
        var (marks, text) = FeedbackGenerationService.ParseFeedbackResponse(
            "MARK: 0\n\nThe response does not address the question.", maxScore: 5);

        Assert.Equal(0, marks);
        Assert.Contains("does not address", text);
    }
}
