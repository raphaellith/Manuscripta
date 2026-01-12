using Main.Models.Entities;
using Main.Services.Repositories;

namespace Main.Services;

/// <summary>
/// Service for managing attachments.
/// Per AdditionalValidationRules.md §3B.
/// </summary>
public class AttachmentService : IAttachmentService
{
    private readonly IAttachmentRepository _repository;
    private readonly IMaterialRepository _materialRepo;

    private static readonly HashSet<string> AllowedExtensions = new(StringComparer.OrdinalIgnoreCase)
    {
        "png", "jpeg", "pdf"
    };

    public AttachmentService(
        IAttachmentRepository repository,
        IMaterialRepository materialRepo)
    {
        _repository = repository;
        _materialRepo = materialRepo;
    }

    public async Task<AttachmentEntity> CreateAsync(AttachmentEntity entity)
    {
        // Validate MaterialId references a valid material
        var material = await _materialRepo.GetByIdAsync(entity.MaterialId);
        if (material == null)
        {
            throw new ArgumentException($"Material with ID {entity.MaterialId} does not exist.");
        }

        // Validate FileExtension is in allowlist
        if (string.IsNullOrEmpty(entity.FileExtension) || 
            !AllowedExtensions.Contains(entity.FileExtension))
        {
            throw new ArgumentException($"File extension '{entity.FileExtension}' is not allowed. Allowed: png, jpeg, pdf");
        }

        // Assign UUID if not set
        if (entity.Id == Guid.Empty)
        {
            entity.Id = Guid.NewGuid();
        }

        await _repository.AddAsync(entity);
        return entity;
    }

    public async Task DeleteAsync(Guid id)
    {
        // Per NetworkingAPISpec §1(1)(l)(iii): only delete the entity
        // File deletion is handled by the frontend via IPC
        await _repository.DeleteAsync(id);
    }
}
