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
        // Arrange - WrittenAnswerQuestion WITHOUT CorrectAnswer allows feedback per §2F(2)(b)
        // But providing Marks without MaxScore should fail per §2F(2)(c)
        var responseId = Guid.NewGuid();
        var questionId = Guid.NewGuid();
        var feedback = new FeedbackEntity(Guid.NewGuid(), responseId, marks: 5);
        
        var response = new WrittenAnswerResponseEntity(responseId, questionId, Guid.NewGuid(), "Answer");
        // Empty CorrectAnswer means no auto-grading, so feedback is allowed
        var question = new WrittenAnswerQuestionEntity(questionId, Guid.NewGuid(), "Describe...", "");
        
        _responseRepositoryMock.Setup(r => r.GetByIdAsync(responseId))
            .ReturnsAsync(response);
        _questionRepositoryMock.Setup(q => q.GetByIdAsync(questionId))
            .ReturnsAsync(question);

        // Act & Assert - Will fail on §2F(2)(c) since question has no MaxScore
        var exception = await Assert.ThrowsAsync<InvalidOperationException>(() =>
            _service.CreateFeedbackAsync(feedback));

        Assert.Contains("MaxScore", exception.Message);
    }

    [Fact]
    public async Task CreateFeedbackAsync_WrittenAnswerWithCorrectAnswer_ThrowsInvalidOperationException()
    {
        // Arrange - WrittenAnswerQuestion WITH CorrectAnswer is auto-graded
        var responseId = Guid.NewGuid();
        var questionId = Guid.NewGuid();
        var feedback = new FeedbackEntity(Guid.NewGuid(), responseId, text: "Good work!");
        
        var response = new WrittenAnswerResponseEntity(responseId, questionId, Guid.NewGuid(), "Paris");
        var question = new WrittenAnswerQuestionEntity(questionId, Guid.NewGuid(), "What is the capital of France?", "Paris");
        
        _responseRepositoryMock.Setup(r => r.GetByIdAsync(responseId))
            .ReturnsAsync(response);
        _questionRepositoryMock.Setup(q => q.GetByIdAsync(questionId))
            .ReturnsAsync(question);

        // Act & Assert - Per §2F(2)(b): Question with CorrectAnswer cannot receive feedback
        var exception = await Assert.ThrowsAsync<InvalidOperationException>(() =>
            _service.CreateFeedbackAsync(feedback));

        Assert.Contains("CorrectAnswer", exception.Message);
    }

    [Fact]
    public async Task CreateFeedbackAsync_WrittenAnswerWithoutCorrectAnswer_Success()
    {
        // Arrange - WrittenAnswerQuestion WITHOUT CorrectAnswer allows manual feedback
        var responseId = Guid.NewGuid();
        var questionId = Guid.NewGuid();
        var feedbackId = Guid.NewGuid();
        var feedback = new FeedbackEntity(feedbackId, responseId, text: "Great essay!");
        
        var response = new WrittenAnswerResponseEntity(responseId, questionId, Guid.NewGuid(), "My essay...");
        // Empty CorrectAnswer means this is a manually-graded question
        var question = new WrittenAnswerQuestionEntity(questionId, Guid.NewGuid(), "Write an essay about...", "", maxScore: 10);
        
        _responseRepositoryMock.Setup(r => r.GetByIdAsync(responseId))
            .ReturnsAsync(response);
        _questionRepositoryMock.Setup(q => q.GetByIdAsync(questionId))
            .ReturnsAsync(question);
        _feedbackRepositoryMock.Setup(r => r.AddAsync(It.IsAny<FeedbackEntity>()))
            .Returns(Task.CompletedTask);

        // Act
        var result = await _service.CreateFeedbackAsync(feedback);

        // Assert - Feedback should be created successfully
        Assert.Equal(feedbackId, result.Id);
        _feedbackRepositoryMock.Verify(r => r.AddAsync(feedback), Times.Once);
    }
}
