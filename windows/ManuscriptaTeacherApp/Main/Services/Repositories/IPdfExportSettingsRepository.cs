using Main.Models.Entities;

namespace Main.Services.Repositories;

/// <summary>
/// Repository interface for the long-term persisted default PDF export settings.
/// Per PersistenceAndCascadingRules §1(1)(k), AdditionalValidationRules §3F.
/// </summary>
public interface IPdfExportSettingsRepository
{
    /// <summary>
    /// Gets the default PDF export settings. Seeds defaults if none exists.
    /// </summary>
    Task<PdfExportSettingsEntity> GetAsync();

    /// <summary>
    /// Updates the default PDF export settings.
    /// </summary>
    /// <param name="entity">The updated PDF export settings.</param>
    Task UpdateAsync(PdfExportSettingsEntity entity);
}
