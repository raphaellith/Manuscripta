using System.Collections.Generic;
using System.Threading.Tasks;
using Main.Models.Entities.Sessions;

namespace Main.Services.Repositories;

/// <summary>
/// Repository interface for managing sessions.
/// </summary>
public interface ISessionRepository
{
    Task<SessionEntity?> GetByIdAsync(Guid id);
    Task<IEnumerable<SessionEntity>> GetAllAsync();
    Task AddAsync(SessionEntity entity);
    Task UpdateAsync(SessionEntity entity);
    Task DeleteAsync(Guid id);
}
