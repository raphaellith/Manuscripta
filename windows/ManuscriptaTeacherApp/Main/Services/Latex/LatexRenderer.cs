using CSharpMath.SkiaSharp;

namespace Main.Services.Latex;

/// <summary>
/// Renders LaTeX expressions to PNG images using CSharpMath.SkiaSharp.
/// Per MaterialConversionSpecification §1A(3).
/// </summary>
public class LatexRenderer : ILatexRenderer
{
    /// <inheritdoc/>
    public byte[]? RenderToImage(string latex, bool displayMode = false, float fontSize = 20f)
    {
        if (string.IsNullOrWhiteSpace(latex))
            return null;

        try
        {
            var painter = new MathPainter
            {
                // CSharpMath has no display mode property; use \displaystyle LaTeX command
                // to produce larger fractions/sums/integrals for block rendering
                LaTeX = displayMode ? $@"\displaystyle{{{latex}}}" : latex,
                FontSize = fontSize,
            };

            // DrawAsStream returns a PNG-encoded stream
            using var stream = painter.DrawAsStream();
            if (stream == null)
                return null;

            using var memoryStream = new MemoryStream();
            stream.CopyTo(memoryStream);
            var bytes = memoryStream.ToArray();

            // Validate we got actual image data
            return bytes.Length > 0 ? bytes : null;
        }
        catch (Exception)
        {
            // Per MaterialConversionSpecification §6(4):
            // LaTeX rendering failures shall result in the raw LaTeX source
            // being displayed as plain text.
            return null;
        }
    }
}
