using System;
using Xunit;
using Main.Models.Entities;

namespace MainTests;

public class ResponseEntityTests
{
    [Fact]
    public void DefaultValues_AreSet()
    {
        var r = new ResponseEntity
        {
            Id = 1,
            QuestionId = 2,
            Answer = "Yes"
        };

        Assert.Equal(1, r.Id);
        Assert.Equal(2, r.QuestionId);
        Assert.Equal("Yes", r.Answer);
        Assert.False(r.Synced);
        Assert.False(r.IsCorrect);
        // Timestamp is set to roughly now
        var diff = DateTime.UtcNow - r.Timestamp;
        Assert.True(diff.TotalSeconds < 10, "Timestamp should be near now");
    }
}
