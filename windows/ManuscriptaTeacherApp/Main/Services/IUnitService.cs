using System;
using System.Collections.Generic;
using System.Threading.Tasks;
using Main.Models.Entities;

namespace Main.Services;

/// <summary>
/// Service interface for managing units.
/// </summary>
public interface IUnitService
{
    Task<UnitEntity> CreateAsync(UnitEntity entity);
    Task<UnitEntity> UpdateAsync(UnitEntity entity);
    Task DeleteAsync(Guid id);
}
