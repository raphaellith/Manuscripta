using Microsoft.EntityFrameworkCore;
using Main.Data;
using Main.Models.Entities;

namespace Main.Services.Repositories;

/// <summary>
/// EF Core implementation of <see cref="IDefaultConfigurationRepository"/>.
/// Manages a single default configuration row in SQLite.
/// Per PersistenceAndCascadingRules §1(1)(h), ConfigurationManagementSpecification §1(3)(a).
/// </summary>
public class EfDefaultConfigurationRepository : IDefaultConfigurationRepository
{
    private readonly MainDbContext _context;

    public EfDefaultConfigurationRepository(MainDbContext context)
    {
        _context = context ?? throw new ArgumentNullException(nameof(context));
    }

    /// <inheritdoc />
    public async Task<ConfigurationEntity> GetAsync()
    {
        var existing = await _context.Configurations
            .FirstOrDefaultAsync(c => c.Id == ConfigurationEntity.DefaultId);

        if (existing != null)
            return existing;

        // Seed with defaults per ConfigurationManagementSpecification §1(4) / Appendix 1
        var defaults = ConfigurationEntity.CreateDefault();
        _context.Configurations.Add(defaults);
        await _context.SaveChangesAsync();
        return defaults;
    }

    /// <inheritdoc />
    public async Task UpdateAsync(ConfigurationEntity entity)
    {
        ArgumentNullException.ThrowIfNull(entity);

        var existing = await _context.Configurations
            .FirstOrDefaultAsync(c => c.Id == ConfigurationEntity.DefaultId);

        if (existing == null)
        {
            entity.Id = ConfigurationEntity.DefaultId;
            _context.Configurations.Add(entity);
        }
        else
        {
            existing.TextSize = entity.TextSize;
            existing.FeedbackStyle = entity.FeedbackStyle;
            existing.TtsEnabled = entity.TtsEnabled;
            existing.AiScaffoldingEnabled = entity.AiScaffoldingEnabled;
            existing.SummarisationEnabled = entity.SummarisationEnabled;
            existing.MascotSelection = entity.MascotSelection;
        }

        await _context.SaveChangesAsync();
    }
}
