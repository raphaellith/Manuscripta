using System.Collections.Generic;
using System.Threading.Tasks;
using Main.Models.Entities.Materials;

namespace Main.Services.Repositories;

public interface IMaterialRepository
{
    Task<MaterialEntity?> GetByIdAsync(Guid id);
    Task<IEnumerable<MaterialEntity>> GetAllAsync();
    Task AddAsync(MaterialEntity entity);
    Task UpdateAsync(MaterialEntity entity);
    Task DeleteAsync(Guid id);
}
