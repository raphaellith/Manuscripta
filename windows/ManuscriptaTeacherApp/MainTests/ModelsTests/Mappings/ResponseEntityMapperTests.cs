using System;
using Xunit;
using Main.Models.Entities;
using Main.Models.Entities.Questions;
using Main.Models.Entities.Responses;
using Main.Models.Enums;
using Main.Models.Mappings;

namespace MainTests.ModelsTests.Mappings;

public class ResponseEntityMapperTests
{
    [Fact]
    public void ToDataEntity_WithMultipleChoiceResponse_MapsCorrectly()
    {
        // Arrange
        var id = Guid.NewGuid();
        var questionId = Guid.NewGuid();
        var deviceId = Guid.NewGuid();
        var timestamp = DateTime.UtcNow;
        var response = new MultipleChoiceResponseEntity(id, questionId, deviceId, 2, timestamp, true);

        // Act
        var dataEntity = ResponseEntityMapper.ToDataEntity(response);

        // Assert
        Assert.Equal(id, dataEntity.Id);
        Assert.Equal(questionId, dataEntity.QuestionId);
        Assert.Equal(deviceId, dataEntity.DeviceId);
        Assert.Equal("2", dataEntity.Answer);
        Assert.True(dataEntity.IsCorrect);
        Assert.Equal(timestamp, dataEntity.Timestamp);
    }

    [Fact]
    public void ToDataEntity_WithTrueFalseResponse_MapsCorrectly()
    {
        // Arrange
        var id = Guid.NewGuid();
        var questionId = Guid.NewGuid();
        var deviceId = Guid.NewGuid();
        var timestamp = DateTime.UtcNow;
        var response = new TrueFalseResponseEntity(id, questionId, deviceId, false, timestamp, false);

        // Act
        var dataEntity = ResponseEntityMapper.ToDataEntity(response);

        // Assert
        Assert.Equal(id, dataEntity.Id);
        Assert.Equal(questionId, dataEntity.QuestionId);
        Assert.Equal(deviceId, dataEntity.DeviceId);
        Assert.Equal("False", dataEntity.Answer);
        Assert.False(dataEntity.IsCorrect);
        Assert.Equal(timestamp, dataEntity.Timestamp);
    }

    [Fact]
    public void ToDataEntity_WithWrittenAnswerResponse_MapsCorrectly()
    {
        // Arrange
        var id = Guid.NewGuid();
        var questionId = Guid.NewGuid();
        var deviceId = Guid.NewGuid();
        var timestamp = DateTime.UtcNow;
        var response = new WrittenAnswerResponseEntity(id, questionId, deviceId, "My answer", timestamp, null);

        // Act
        var dataEntity = ResponseEntityMapper.ToDataEntity(response);

        // Assert
        Assert.Equal(id, dataEntity.Id);
        Assert.Equal(questionId, dataEntity.QuestionId);
        Assert.Equal(deviceId, dataEntity.DeviceId);
        Assert.Equal("My answer", dataEntity.Answer);
        Assert.False(dataEntity.IsCorrect); // null becomes false
        Assert.Equal(timestamp, dataEntity.Timestamp);
    }

    [Fact]
    public void ToDataEntity_WithNull_ThrowsArgumentNullException()
    {
        // Act & Assert
        Assert.Throws<ArgumentNullException>(() => ResponseEntityMapper.ToDataEntity(null!));
    }

    [Fact]
    public void ToEntity_WithMultipleChoiceResponse_MapsCorrectly()
    {
        // Arrange
        var id = Guid.NewGuid();
        var questionId = Guid.NewGuid();
        var materialId = Guid.NewGuid();
        var deviceId = Guid.NewGuid();
        var timestamp = DateTime.UtcNow;

        var question = new MultipleChoiceQuestionEntity(
            questionId, materialId, "Question?", 
            new List<string> { "A", "B", "C" }, 
            1
        );

        var dataEntity = new ResponseDataEntity(id, questionId, deviceId, "1", true, timestamp);

        // Act
        var entity = ResponseEntityMapper.ToEntity(dataEntity, question);

        // Assert
        Assert.IsType<MultipleChoiceResponseEntity>(entity);
        var mcEntity = (MultipleChoiceResponseEntity)entity;
        Assert.Equal(id, mcEntity.Id);
        Assert.Equal(questionId, mcEntity.QuestionId);
        Assert.Equal(deviceId, mcEntity.DeviceId);
        Assert.Equal(1, mcEntity.AnswerIndex);
        Assert.True(mcEntity.IsCorrect);
        Assert.Equal(timestamp, mcEntity.Timestamp);
    }

    [Fact]
    public void ToEntity_WithTrueFalseResponse_MapsCorrectly()
    {
        // Arrange
        var id = Guid.NewGuid();
        var questionId = Guid.NewGuid();
        var materialId = Guid.NewGuid();
        var deviceId = Guid.NewGuid();
        var timestamp = DateTime.UtcNow;

        var question = new TrueFalseQuestionEntity(questionId, materialId, "Question?", true);
        var dataEntity = new ResponseDataEntity(id, questionId, deviceId, "True", true, timestamp);

        // Act
        var entity = ResponseEntityMapper.ToEntity(dataEntity, question);

        // Assert
        Assert.IsType<TrueFalseResponseEntity>(entity);
        var tfEntity = (TrueFalseResponseEntity)entity;
        Assert.Equal(id, tfEntity.Id);
        Assert.Equal(questionId, tfEntity.QuestionId);
        Assert.Equal(deviceId, tfEntity.DeviceId);
        Assert.True(tfEntity.Answer);
        Assert.True(tfEntity.IsCorrect);
        Assert.Equal(timestamp, tfEntity.Timestamp);
    }

    [Fact]
    public void ToEntity_WithWrittenAnswerResponse_MapsCorrectly()
    {
        // Arrange
        var id = Guid.NewGuid();
        var questionId = Guid.NewGuid();
        var materialId = Guid.NewGuid();
        var deviceId = Guid.NewGuid();
        var timestamp = DateTime.UtcNow;

        var question = new WrittenAnswerQuestionEntity(questionId, materialId, "Question?", "Paris");
        var dataEntity = new ResponseDataEntity(id, questionId, deviceId, "Paris", false, timestamp);

        // Act
        var entity = ResponseEntityMapper.ToEntity(dataEntity, question);

        // Assert
        Assert.IsType<WrittenAnswerResponseEntity>(entity);
        var waEntity = (WrittenAnswerResponseEntity)entity;
        Assert.Equal(id, waEntity.Id);
        Assert.Equal(questionId, waEntity.QuestionId);
        Assert.Equal(deviceId, waEntity.DeviceId);
        Assert.Equal("Paris", waEntity.Answer);
        Assert.False(waEntity.IsCorrect);
        Assert.Equal(timestamp, waEntity.Timestamp);
    }

    [Fact]
    public void ToEntity_WithNullDataEntity_ThrowsArgumentNullException()
    {
        // Arrange
        var question = new TrueFalseQuestionEntity(Guid.NewGuid(), Guid.NewGuid(), "Q?", true);

        // Act & Assert
        Assert.Throws<ArgumentNullException>(() => ResponseEntityMapper.ToEntity(null!, question));
    }

    [Fact]
    public void ToEntity_WithNullQuestion_ThrowsArgumentNullException()
    {
        // Arrange
        var dataEntity = new ResponseDataEntity(Guid.NewGuid(), Guid.NewGuid(), Guid.NewGuid(), "answer");

        // Act & Assert
        Assert.Throws<ArgumentNullException>(() => ResponseEntityMapper.ToEntity(dataEntity, null!));
    }

    [Fact]
    public void ToEntity_WithInvalidAnswerForMultipleChoice_ThrowsInvalidOperationException()
    {
        // Arrange
        var question = new MultipleChoiceQuestionEntity(
            Guid.NewGuid(), Guid.NewGuid(), "Question?", 
            new List<string> { "A", "B" }, 
            0
        );
        var dataEntity = new ResponseDataEntity(Guid.NewGuid(), question.Id, Guid.NewGuid(), "not a number");

        // Act & Assert
        Assert.Throws<InvalidOperationException>(() => ResponseEntityMapper.ToEntity(dataEntity, question));
    }

    [Fact]
    public void ToEntity_WithInvalidAnswerForTrueFalse_ThrowsInvalidOperationException()
    {
        // Arrange
        var question = new TrueFalseQuestionEntity(Guid.NewGuid(), Guid.NewGuid(), "Question?", true);
        var dataEntity = new ResponseDataEntity(Guid.NewGuid(), question.Id, Guid.NewGuid(), "maybe");

        // Act & Assert
        Assert.Throws<InvalidOperationException>(() => ResponseEntityMapper.ToEntity(dataEntity, question));
    }

    [Fact]
    public void RoundTrip_PreservesData()
    {
        // Arrange
        var id = Guid.NewGuid();
        var questionId = Guid.NewGuid();
        var materialId = Guid.NewGuid();
        var deviceId = Guid.NewGuid();
        var timestamp = new DateTime(2025, 11, 26, 12, 0, 0, DateTimeKind.Utc);

        var question = new MultipleChoiceQuestionEntity(
            questionId, materialId, "Question?", 
            new List<string> { "A", "B", "C" }, 
            2
        );

        var original = new MultipleChoiceResponseEntity(id, questionId, deviceId, 2, timestamp, true);

        // Act
        var dataEntity = ResponseEntityMapper.ToDataEntity(original);
        var roundTripped = ResponseEntityMapper.ToEntity(dataEntity, question) as MultipleChoiceResponseEntity;

        // Assert
        Assert.NotNull(roundTripped);
        Assert.Equal(original.Id, roundTripped!.Id);
        Assert.Equal(original.QuestionId, roundTripped.QuestionId);
        Assert.Equal(original.DeviceId, roundTripped.DeviceId);
        Assert.Equal(original.AnswerIndex, roundTripped.AnswerIndex);
        Assert.Equal(original.IsCorrect, roundTripped.IsCorrect);
        Assert.Equal(original.Timestamp, roundTripped.Timestamp);
    }
}
