using Main.Models.Entities;

namespace Main.Services.Repositories;

/// <summary>
/// Repository interface for managing ReMarkableDeviceEntity.
/// Per PersistenceAndCascadingRules.md §1(1)(h).
/// </summary>
public interface IReMarkableDeviceRepository
{
    /// <summary>
    /// Retrieves a reMarkable device by its ID.
    /// </summary>
    Task<ReMarkableDeviceEntity?> GetByIdAsync(Guid id);

    /// <summary>
    /// Retrieves all paired reMarkable devices.
    /// </summary>
    Task<IEnumerable<ReMarkableDeviceEntity>> GetAllAsync();

    /// <summary>
    /// Adds a new reMarkable device entity.
    /// </summary>
    Task AddAsync(ReMarkableDeviceEntity entity);

    /// <summary>
    /// Updates an existing reMarkable device entity.
    /// </summary>
    Task UpdateAsync(ReMarkableDeviceEntity entity);

    /// <summary>
    /// Deletes a reMarkable device entity by its ID.
    /// </summary>
    Task DeleteAsync(Guid id);
}
