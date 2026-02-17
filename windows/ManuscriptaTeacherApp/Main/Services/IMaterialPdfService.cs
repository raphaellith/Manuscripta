namespace Main.Services;

/// <summary>
/// Service for generating PDF documents from materials.
/// Per MaterialConversionSpecification §5.
/// </summary>
public interface IMaterialPdfService
{
    /// <summary>
    /// Generates a PDF document for the specified material.
    /// Per MaterialConversionSpecification §5(2)(a).
    /// </summary>
    /// <param name="materialId">The ID of the material to generate a PDF for.</param>
    /// <returns>A byte array containing the PDF document.</returns>
    /// <exception cref="KeyNotFoundException">Thrown when the material does not exist.</exception>
    Task<byte[]> GeneratePdfAsync(Guid materialId);
}
