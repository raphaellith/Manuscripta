using Xunit;
using Main.Models.Entities;

namespace MainTests;

public class QuestionEntityTests
{
    [Fact]
    public void DefaultConstructor_InitializesCollections()
    {
        var q = new QuestionEntity();
        Assert.Equal(0, q.Id);
        Assert.Equal(0, q.MaterialId);
    }

    [Fact]
    public void CanSetProperties()
    {
        var q = new QuestionEntity
        {
            MaterialId = 5,
            QuestionText = "What?",
            QuestionType = "MCQ",
            Options = "[]",
            CorrectAnswer = "A"
        };

        Assert.Equal(5, q.MaterialId);
        Assert.Equal("What?", q.QuestionText);
        Assert.Equal("MCQ", q.QuestionType);
        Assert.Equal("[]", q.Options);
        Assert.Equal("A", q.CorrectAnswer);
    }
}
