namespace Main.Services.Latex;

/// <summary>
/// Interface for rendering LaTeX expressions to image bytes.
/// Per MaterialConversionSpecification §1A(3).
/// </summary>
public interface ILatexRenderer
{
    /// <summary>
    /// Renders a LaTeX expression to PNG image bytes.
    /// </summary>
    /// <param name="latex">The LaTeX expression to render.</param>
    /// <param name="displayMode">If true, renders in display (block) mode; otherwise inline mode.</param>
    /// <param name="fontSize">Font size for rendering (default 20).</param>
    /// <returns>PNG image bytes, or null if rendering fails per §6(4).</returns>
    byte[]? RenderToImage(string latex, bool displayMode = false, float fontSize = 20f);
}
