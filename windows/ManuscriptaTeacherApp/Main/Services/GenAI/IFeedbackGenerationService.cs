using Main.Models.Entities.Questions;

namespace Main.Services.GenAI;

public interface IFeedbackGenerationService
{
    bool ShouldGenerateFeedback(QuestionEntity question);
}
