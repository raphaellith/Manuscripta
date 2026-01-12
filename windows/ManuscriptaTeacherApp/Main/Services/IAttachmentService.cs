using Main.Models.Entities;

namespace Main.Services;

/// <summary>
/// Service interface for managing attachments.
/// Per AdditionalValidationRules.md §3B.
/// </summary>
public interface IAttachmentService
{
    Task<AttachmentEntity> CreateAsync(AttachmentEntity entity);
    Task DeleteAsync(Guid id);
}
