using Main.Models.Entities;

namespace Main.Services.Repositories;

/// <summary>
/// Repository interface for managing AttachmentEntity.
/// Per PersistenceAndCascadingRules.md §1(1)(g).
/// </summary>
public interface IAttachmentRepository
{
    Task<AttachmentEntity?> GetByIdAsync(Guid id);
    Task<IEnumerable<AttachmentEntity>> GetAllAsync();
    Task<IEnumerable<AttachmentEntity>> GetByMaterialIdAsync(Guid materialId);
    Task AddAsync(AttachmentEntity entity);
    Task DeleteAsync(Guid id);
}
