using Main.Models.Entities.Questions;

namespace Main.Services.GenAI;

public interface IFeedbackGenerationService
{
    bool ShouldGenerateFeedback(QuestionEntity question);

    /// <summary>
    /// Returns the response ID currently being processed for feedback generation, or null if idle.
    /// See GenAISpec.md §3D(4).
    /// </summary>
    string? GetCurrentlyGeneratingResponseId();
}
