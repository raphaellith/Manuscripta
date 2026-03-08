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

    /// <summary>
    /// Generates a Response PDF for a single device's responses to all questions on a material.
    /// Per MaterialConversionSpecification §5(2)(b) and §7.
    /// </summary>
    /// <param name="materialId">The ID of the material.</param>
    /// <param name="deviceId">The device identifier.</param>
    /// <param name="includeFeedback">Whether to include feedback in the PDF.</param>
    /// <param name="includeMarkScheme">Whether to include mark schemes in the PDF.</param>
    /// <returns>A byte array containing the Response PDF document.</returns>
    /// <exception cref="KeyNotFoundException">Thrown when the material does not exist.</exception>
    /// <exception cref="InvalidOperationException">Thrown when no responses exist from the device for the material.</exception>
    Task<byte[]> GenerateResponsePdfAsync(Guid materialId, string deviceId, bool includeFeedback, bool includeMarkScheme);
}
