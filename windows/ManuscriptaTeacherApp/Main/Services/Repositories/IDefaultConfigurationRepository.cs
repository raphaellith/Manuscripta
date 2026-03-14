using Main.Models.Entities;

namespace Main.Services.Repositories;

/// <summary>
/// Repository interface for the long-term persisted default configuration.
/// Per PersistenceAndCascadingRules §1(1)(h), ConfigurationManagementSpecification §1(3)(a).
/// </summary>
public interface IDefaultConfigurationRepository
{
    /// <summary>
    /// Gets the default configuration. Seeds defaults if none exists.
    /// </summary>
    Task<ConfigurationEntity> GetAsync();

    /// <summary>
    /// Updates the default configuration.
    /// </summary>
    /// <param name="entity">The updated default configuration.</param>
    Task UpdateAsync(ConfigurationEntity entity);
}
