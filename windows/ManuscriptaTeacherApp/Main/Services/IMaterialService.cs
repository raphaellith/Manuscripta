using System;
using System.Collections.Generic;
using System.Threading.Tasks;
using Main.Models.Entities.Materials;

namespace Main.Services;

/// <summary>
/// Service interface for managing materials and their associated questions.
/// </summary>
public interface IMaterialService
{
    // Material operations
    Task<MaterialEntity> CreateMaterialAsync(MaterialEntity material);
    Task<MaterialEntity?> GetMaterialByIdAsync(Guid id);
    Task<IEnumerable<MaterialEntity>> GetAllMaterialsAsync();
    Task<MaterialEntity> UpdateMaterialAsync(MaterialEntity material);
    Task DeleteMaterialAsync(Guid id);
}
