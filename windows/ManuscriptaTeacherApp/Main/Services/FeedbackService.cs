using System;
using System.Collections.Generic;
using System.Threading.Tasks;
using Main.Models.Entities;
using Main.Models.Entities.Responses;
using Main.Models.Entities.Questions;
using Main.Services.Repositories;

namespace Main.Services;

/// <summary>
/// Service for managing feedback.
/// Enforces business rules and data validation per Validation Rules §2F.
/// </summary>
public class FeedbackService : IFeedbackService
{
    private readonly IFeedbackRepository _feedbackRepository;
    private readonly IResponseRepository _responseRepository;
    private readonly IQuestionRepository _questionRepository;

    public FeedbackService(
        IFeedbackRepository feedbackRepository,
        IResponseRepository responseRepository,
        IQuestionRepository questionRepository)
    {
        _feedbackRepository = feedbackRepository ?? throw new ArgumentNullException(nameof(feedbackRepository));
        _responseRepository = responseRepository ?? throw new ArgumentNullException(nameof(responseRepository));
        _questionRepository = questionRepository ?? throw new ArgumentNullException(nameof(questionRepository));
    }

    public async Task<FeedbackEntity> CreateFeedbackAsync(FeedbackEntity feedback)
    {
        if (feedback == null)
            throw new ArgumentNullException(nameof(feedback));

        // Validate feedback according to §2F(2)
        await ValidateFeedbackAsync(feedback);

        await _feedbackRepository.AddAsync(feedback);
        return feedback;
    }

    #region Validation

    /// <summary>
    /// Validates feedback according to Validation Rules §2F(2):
    /// - (a) ResponseId must reference a valid ResponseEntity
    /// - (b) Associated QuestionEntity must not have CorrectAnswer
    /// - (c) If Marks present, question must have MaxScore and Marks <= MaxScore
    /// </summary>
    private async Task ValidateFeedbackAsync(FeedbackEntity feedback)
    {
        // Rule §2F(2)(a): ResponseId must associate with a ResponseEntity
        var response = await _responseRepository.GetByIdAsync(feedback.ResponseId);
        if (response == null)
            throw new InvalidOperationException($"Response with ID {feedback.ResponseId} not found.");

        // Get the associated question
        var question = await _questionRepository.GetByIdAsync(response.QuestionId);
        if (question == null)
            throw new InvalidOperationException($"Question with ID {response.QuestionId} not found.");

        // Rule §2F(2)(b): QuestionEntity must not have CorrectAnswer field
        // Questions with CorrectAnswer are auto-graded and shouldn't have manual feedback
        if (HasCorrectAnswer(question))
            throw new InvalidOperationException(
                "Feedback cannot be provided for questions with a CorrectAnswer field. " +
                "Such questions are auto-graded.");

        // Rule §2F(2)(c): If Marks present, question must have MaxScore and Marks <= MaxScore
        if (feedback.Marks.HasValue)
        {
            if (!question.MaxScore.HasValue)
                throw new InvalidOperationException(
                    "Marks cannot be assigned to a question that does not have a MaxScore defined.");

            if (feedback.Marks.Value > question.MaxScore.Value)
                throw new InvalidOperationException(
                    $"Marks ({feedback.Marks.Value}) cannot exceed MaxScore ({question.MaxScore.Value}).");

            if (feedback.Marks.Value < 0)
                throw new InvalidOperationException("Marks cannot be negative.");
        }
    }

    /// <summary>
    /// Determines if a question has a CorrectAnswer field.
    /// MultipleChoiceQuestionEntity has CorrectAnswerIndex (equivalent).
    /// WrittenAnswerQuestionEntity has CorrectAnswer string.
    /// </summary>
    private static bool HasCorrectAnswer(QuestionEntity question)
    {
        return question switch
        {
            MultipleChoiceQuestionEntity => true, // Has CorrectAnswerIndex
            WrittenAnswerQuestionEntity => true,  // Has CorrectAnswer string
            _ => false
        };
    }

    #endregion
}
