using Microsoft.EntityFrameworkCore;
using Main.Data;
using Main.Models.Entities;

namespace Main.Services.Repositories;

/// <summary>
/// EF Core implementation of <see cref="IPdfExportSettingsRepository"/>.
/// Manages a single default PDF export settings row in SQLite.
/// Per PersistenceAndCascadingRules §1(1)(k), AdditionalValidationRules §3F.
/// </summary>
public class EfPdfExportSettingsRepository : IPdfExportSettingsRepository
{
    private readonly MainDbContext _context;

    public EfPdfExportSettingsRepository(MainDbContext context)
    {
        _context = context ?? throw new ArgumentNullException(nameof(context));
    }

    /// <inheritdoc />
    public async Task<PdfExportSettingsEntity> GetAsync()
    {
        var existing = await _context.PdfExportSettings
            .FirstOrDefaultAsync(e => e.Id == PdfExportSettingsEntity.DefaultId);

        if (existing != null)
            return existing;

        // Seed with defaults per AdditionalValidationRules §3F(1)
        var defaults = PdfExportSettingsEntity.CreateDefault();
        _context.PdfExportSettings.Add(defaults);
        await _context.SaveChangesAsync();
        return defaults;
    }

    /// <inheritdoc />
    public async Task UpdateAsync(PdfExportSettingsEntity entity)
    {
        ArgumentNullException.ThrowIfNull(entity);

        var existing = await _context.PdfExportSettings
            .FirstOrDefaultAsync(e => e.Id == PdfExportSettingsEntity.DefaultId);

        if (existing == null)
        {
            entity.Id = PdfExportSettingsEntity.DefaultId;
            _context.PdfExportSettings.Add(entity);
        }
        else
        {
            existing.LinePatternType = entity.LinePatternType;
            existing.LineSpacingPreset = entity.LineSpacingPreset;
            existing.FontSizePreset = entity.FontSizePreset;
        }

        await _context.SaveChangesAsync();
    }
}
