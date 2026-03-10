using System;
using Main.Models.Entities.Questions;
using Main.Services.GenAI;
using Microsoft.Extensions.DependencyInjection;
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
            null!);

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
            null!);

        var question = new WrittenAnswerQuestionEntity(
            Guid.NewGuid(),
            Guid.NewGuid(),
            "Explain gravity",
            correctAnswer: null,
            markScheme: null);

        Assert.False(service.ShouldGenerateFeedback(question));
    }
}

