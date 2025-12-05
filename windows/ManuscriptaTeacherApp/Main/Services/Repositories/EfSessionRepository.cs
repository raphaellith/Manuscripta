using System.Collections.Generic;
using System.Linq;
using System.Threading.Tasks;
using Microsoft.EntityFrameworkCore;
using Main.Data;
using Main.Models.Entities;
using Main.Models.Entities.Sessions;
using Main.Models.Mappings;

namespace Main.Services.Repositories;

/// <summary>
/// Repository for managing sessions. Uses SessionDataEntity for persistence
/// and SessionEntity for business logic.
/// </summary>
public class EfSessionRepository : ISessionRepository
{
    private readonly MainDbContext _ctx;

    public EfSessionRepository(MainDbContext ctx)
    {
        _ctx = ctx;
    }

    public async Task<SessionEntity?> GetByIdAsync(Guid id)
    {
        var dataEntity = await _ctx.Sessions
            .FirstOrDefaultAsync(s => s.Id == id);
        
        if (dataEntity == null)
            return null;
        
        return SessionEntityMapper.ToEntity(dataEntity);
    }

    public async Task<IEnumerable<SessionEntity>> GetAllAsync()
    {
        var dataEntities = await _ctx.Sessions.ToListAsync();
        return dataEntities.Select(de => SessionEntityMapper.ToEntity(de));
    }

    public async Task AddAsync(SessionEntity entity)
    {
        var dataEntity = SessionEntityMapper.ToDataEntity(entity);
        await _ctx.Sessions.AddAsync(dataEntity);
        await _ctx.SaveChangesAsync();
    }

    public async Task UpdateAsync(SessionEntity entity)
    {
        var dataEntity = SessionEntityMapper.ToDataEntity(entity);
        _ctx.Sessions.Update(dataEntity);
        await _ctx.SaveChangesAsync();
    }

    public async Task DeleteAsync(Guid id)
    {
        var dataEntity = await _ctx.Sessions.FindAsync(id);
        if (dataEntity != null)
        {
            _ctx.Sessions.Remove(dataEntity);
            await _ctx.SaveChangesAsync();
        }
    }
}
