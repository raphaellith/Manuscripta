using Main.Models.Entities;

namespace Main.Services.Repositories;

/// <summary>
/// Defines the repository interface for external devices.
/// </summary>
public interface IExternalDeviceRepository
{
    Task<IEnumerable<ExternalDeviceEntity>> GetAllAsync();
    Task<ExternalDeviceEntity?> GetByIdAsync(Guid id);
    Task AddAsync(ExternalDeviceEntity entity);
    Task UpdateAsync(ExternalDeviceEntity entity);
    Task DeleteAsync(Guid id);
}
