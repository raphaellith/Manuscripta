using Xunit;
using Main.Models.Enums;

namespace MainTests;

public class MaterialTypeTests
{
    [Fact]
    public void Enum_HasExpectedValues()
    {
        var names = System.Enum.GetNames(typeof(MaterialType));

        Assert.Contains("QUIZ", names);
        Assert.Contains("WORKSHEET", names);
        Assert.Contains("POLL", names);
        Assert.Contains("READING", names);
    }

    [Fact]
    public void Parse_FromString_Works()
    {
        var v = System.Enum.Parse(typeof(MaterialType), "QUIZ");
        Assert.Equal(MaterialType.QUIZ, (MaterialType)v);
    }
}
