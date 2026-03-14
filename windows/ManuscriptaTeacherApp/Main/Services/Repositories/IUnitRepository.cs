using System;
using System.Collections.Generic;
using System.Threading.Tasks;
using Main.Models.Entities;

namespace Main.Services.Repositories;

public interface IUnitRepository
{
    Task<UnitEntity?> GetByIdAsync(Guid id);
    Task<IEnumerable<UnitEntity>> GetByUnitCollectionIdAsync(Guid unitCollectionId);
    Task<IEnumerable<UnitEntity>> GetAllAsync();
    Task AddAsync(UnitEntity entity);
    Task UpdateAsync(UnitEntity entity);
    Task DeleteAsync(Guid id);
}
