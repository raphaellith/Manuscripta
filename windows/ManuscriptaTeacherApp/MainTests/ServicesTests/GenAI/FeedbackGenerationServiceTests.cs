using System;
using Main.Models.Entities.Questions;
using Main.Services.GenAI;
using Microsoft.Extensions.DependencyInjection;
using Moq;
using Xunit;

namespace MainTests.ServicesTests.GenAI;

public class FeedbackGenerationServiceTests
{
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
