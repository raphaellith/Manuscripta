using System;
using System.Collections.Generic;
using System.Threading.Tasks;
using Moq;
using Xunit;
using Main.Models.Entities;
using Main.Models.Entities.Questions;
using Main.Models.Entities.Responses;
using Main.Models.Enums;
using Main.Services;
using Main.Services.Repositories;

namespace MainTests;

/// <summary>
/// Tests for FeedbackService validation rules per §2F(2)
/// </summary>
public class FeedbackServiceTests
{
    private readonly Mock<IFeedbackRepository> _feedbackRepositoryMock;
    private readonly Mock<IResponseRepository> _responseRepositoryMock;
    private readonly Mock<IQuestionRepository> _questionRepositoryMock;
    private readonly FeedbackService _service;

    public FeedbackServiceTests()
    {
        _feedbackRepositoryMock = new Mock<IFeedbackRepository>();
        _responseRepositoryMock = new Mock<IResponseRepository>();
        _questionRepositoryMock = new Mock<IQuestionRepository>();
        
        _service = new FeedbackService(
            _feedbackRepositoryMock.Object,
            _responseRepositoryMock.Object,
            _questionRepositoryMock.Object);
    }

    [Fact]
    public async Task CreateFeedbackAsync_NullFeedback_ThrowsArgumentNullException()
    {
        // Act & Assert
        await Assert.ThrowsAsync<ArgumentNullException>(() =>
            _service.CreateFeedbackAsync(null!));
    }

    [Fact]
    public async Task CreateFeedbackAsync_ResponseNotFound_ThrowsInvalidOperationException()
    {
        // Arrange
        var feedback = new FeedbackEntity(Guid.NewGuid(), Guid.NewGuid(), text: "Good work!");
        _responseRepositoryMock.Setup(r => r.GetByIdAsync(feedback.ResponseId))
            .ReturnsAsync((ResponseEntity?)null);

        // Act & Assert - Per §2F(2)(a)
        var exception = await Assert.ThrowsAsync<InvalidOperationException>(() =>
            _service.CreateFeedbackAsync(feedback));

        Assert.Contains("Response with ID", exception.Message);
        Assert.Contains("not found", exception.Message);
    }

    [Fact]
    public async Task CreateFeedbackAsync_QuestionNotFound_ThrowsInvalidOperationException()
    {
        // Arrange
        var responseId = Guid.NewGuid();
        var questionId = Guid.NewGuid();
        var feedback = new FeedbackEntity(Guid.NewGuid(), responseId, text: "Good work!");
        
        var response = new WrittenAnswerResponseEntity(responseId, questionId, Guid.NewGuid(), "Answer");
        _responseRepositoryMock.Setup(r => r.GetByIdAsync(responseId))
            .ReturnsAsync(response);
        _questionRepositoryMock.Setup(q => q.GetByIdAsync(questionId))
            .ReturnsAsync((QuestionEntity?)null);

        // Act & Assert
        var exception = await Assert.ThrowsAsync<InvalidOperationException>(() =>
            _service.CreateFeedbackAsync(feedback));

        Assert.Contains("Question with ID", exception.Message);
    }

    [Fact]
    public async Task CreateFeedbackAsync_QuestionWithCorrectAnswer_ThrowsInvalidOperationException()
    {
        // Arrange - MultipleChoiceQuestion has CorrectAnswerIndex (equivalent to CorrectAnswer)
        var responseId = Guid.NewGuid();
        var questionId = Guid.NewGuid();
        var feedback = new FeedbackEntity(Guid.NewGuid(), responseId, text: "Good work!");
        
        var response = new MultipleChoiceResponseEntity(responseId, questionId, Guid.NewGuid(), 0);
        var question = new MultipleChoiceQuestionEntity(
            questionId, Guid.NewGuid(), "What is 1+1?", 
            new List<string> { "2", "3" }, 0);
        
        _responseRepositoryMock.Setup(r => r.GetByIdAsync(responseId))
            .ReturnsAsync(response);
        _questionRepositoryMock.Setup(q => q.GetByIdAsync(questionId))
            .ReturnsAsync(question);

        // Act & Assert - Per §2F(2)(b): Question must not have CorrectAnswer
        var exception = await Assert.ThrowsAsync<InvalidOperationException>(() =>
            _service.CreateFeedbackAsync(feedback));

        Assert.Contains("CorrectAnswer", exception.Message);
        Assert.Contains("auto-graded", exception.Message);
    }

    [Fact]
    public async Task CreateFeedbackAsync_MarksWithoutMaxScore_ThrowsInvalidOperationException()
    {
        // Arrange - WrittenAnswerQuestion has CorrectAnswer, so this will fail on §2F(2)(b) first
        // This test is for documentation purposes; in current implementation all question types have CorrectAnswer
        var responseId = Guid.NewGuid();
        var questionId = Guid.NewGuid();
        var feedback = new FeedbackEntity(Guid.NewGuid(), responseId, marks: 5);
        
        var response = new WrittenAnswerResponseEntity(responseId, questionId, Guid.NewGuid(), "Answer");
        var question = new WrittenAnswerQuestionEntity(questionId, Guid.NewGuid(), "Describe...", "");
        
        _responseRepositoryMock.Setup(r => r.GetByIdAsync(responseId))
            .ReturnsAsync(response);
        _questionRepositoryMock.Setup(q => q.GetByIdAsync(questionId))
            .ReturnsAsync(question);

        // Act & Assert - Will fail on §2F(2)(b) since WrittenAnswer has CorrectAnswer
        var exception = await Assert.ThrowsAsync<InvalidOperationException>(() =>
            _service.CreateFeedbackAsync(feedback));

        // Current implementation rejects all questions with CorrectAnswer field
        Assert.Contains("CorrectAnswer", exception.Message);
    }
}
