using Xunit;
using Main.Models.Enums;

namespace MainTests;

public class MaterialTypeTests
{
    [Fact]
    public void Enum_HasExpectedValues()
    {
        var names = System.Enum.GetNames(typeof(MaterialType));

        Assert.Contains("WORKSHEET", names);
        Assert.Contains("POLL", names);
        Assert.Contains("READING", names);
        Assert.Equal(3, names.Length);
    }

    [Fact]
    public void Parse_FromString_Works()
    {
        var v = System.Enum.Parse(typeof(MaterialType), "WORKSHEET");
        Assert.Equal(MaterialType.WORKSHEET, (MaterialType)v);
    }
}
