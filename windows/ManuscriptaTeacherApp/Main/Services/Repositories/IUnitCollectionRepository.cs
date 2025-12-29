using System;
using System.Collections.Generic;
using System.Threading.Tasks;
using Main.Models.Entities;

namespace Main.Services.Repositories;

public interface IUnitCollectionRepository
{
    Task<UnitCollectionEntity?> GetByIdAsync(Guid id);
    Task<IEnumerable<UnitCollectionEntity>> GetAllAsync();
    Task AddAsync(UnitCollectionEntity entity);
    Task UpdateAsync(UnitCollectionEntity entity);
    Task DeleteAsync(Guid id);
}
