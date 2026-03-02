using System;
using System.Collections.Generic;
using System.Threading.Tasks;
using Main.Models.Entities;

namespace Main.Services;

/// <summary>
/// Service interface for managing unit collections.
/// </summary>
public interface IUnitCollectionService
{
    Task<UnitCollectionEntity> CreateAsync(UnitCollectionEntity entity);
    Task<UnitCollectionEntity> UpdateAsync(UnitCollectionEntity entity);
    Task DeleteAsync(Guid id);
}
