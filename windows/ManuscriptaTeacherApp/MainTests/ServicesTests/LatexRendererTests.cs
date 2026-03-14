using Main.Services.Latex;
using Xunit;

namespace MainTests.ServicesTests;

/// <summary>
/// Tests for LatexRenderer.
/// Verifies CSharpMath.SkiaSharp-based LaTeX rendering per MaterialConversionSpecification §1A(3).
/// </summary>
public class LatexRendererTests
{
    private readonly LatexRenderer _renderer;

    public LatexRendererTests()
    {
        _renderer = new LatexRenderer();
    }

    [Fact]
    public void RenderToImage_ValidLatex_ReturnsPngBytes()
    {
        // Act
        var result = _renderer.RenderToImage("x^2 + y^2 = z^2");

        // Assert
        Assert.NotNull(result);
        Assert.NotEmpty(result);
        // PNG files start with the PNG signature
        Assert.Equal(0x89, result[0]);
        Assert.Equal(0x50, result[1]); // 'P'
        Assert.Equal(0x4E, result[2]); // 'N'
        Assert.Equal(0x47, result[3]); // 'G'
    }

    [Fact]
    public void RenderToImage_DisplayMode_ReturnsPngBytes()
    {
        // Act
        var result = _renderer.RenderToImage(@"\int_0^1 f(x)\, dx", displayMode: true, fontSize: 24f);

        // Assert
        Assert.NotNull(result);
        Assert.NotEmpty(result);
        Assert.Equal(0x89, result[0]); // PNG signature
    }

    [Fact]
    public void RenderToImage_EmptyString_ReturnsNull()
    {
        // Act
        var result = _renderer.RenderToImage("");

        // Assert
        Assert.Null(result);
    }

    [Fact]
    public void RenderToImage_WhitespaceOnly_ReturnsNull()
    {
        // Act
        var result = _renderer.RenderToImage("   ");

        // Assert
        Assert.Null(result);
    }

    [Fact]
    public void RenderToImage_NullInput_ReturnsNull()
    {
        // Act
        var result = _renderer.RenderToImage(null!);

        // Assert
        Assert.Null(result);
    }

    [Fact]
    public void RenderToImage_FractionExpression_ReturnsPngBytes()
    {
        // Act
        var result = _renderer.RenderToImage(@"\frac{a}{b}");

        // Assert
        Assert.NotNull(result);
        Assert.NotEmpty(result);
    }
}
