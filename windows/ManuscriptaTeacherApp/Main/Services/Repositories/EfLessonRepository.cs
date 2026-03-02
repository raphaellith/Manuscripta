using System;
using System.Collections.Generic;
using System.Linq;
using System.Threading.Tasks;
using Microsoft.EntityFrameworkCore;
using Main.Data;
using Main.Models.Entities;

namespace Main.Services.Repositories;

/// <summary>
/// Repository for managing LessonEntity.
/// Per PersistenceAndCascadingRules.md §1(1)(e).
/// </summary>
public class EfLessonRepository : ILessonRepository
{
    private readonly MainDbContext _ctx;

    public EfLessonRepository(MainDbContext ctx)
    {
        _ctx = ctx;
    }

    public async Task<LessonEntity?> GetByIdAsync(Guid id)
    {
        return await _ctx.Lessons
            .FirstOrDefaultAsync(l => l.Id == id);
    }

    public async Task<IEnumerable<LessonEntity>> GetByUnitIdAsync(Guid unitId)
    {
        return await _ctx.Lessons
            .Where(l => l.UnitId == unitId)
            .ToListAsync();
    }

    public async Task<IEnumerable<LessonEntity>> GetAllAsync()
    {
        return await _ctx.Lessons.ToListAsync();
    }

    public async Task AddAsync(LessonEntity entity)
    {
        await _ctx.Lessons.AddAsync(entity);
        await _ctx.SaveChangesAsync();
    }

    public async Task UpdateAsync(LessonEntity entity)
    {
        var tracked = _ctx.Lessons.Local.FirstOrDefault(l => l.Id == entity.Id);
        if (tracked != null)
        {
            _ctx.Entry(tracked).State = EntityState.Detached;
        }
        
        _ctx.Lessons.Update(entity);
        await _ctx.SaveChangesAsync();
    }

    public async Task DeleteAsync(Guid id)
    {
        var entity = await _ctx.Lessons.FindAsync(id);
        if (entity != null)
        {
            _ctx.Lessons.Remove(entity);
            await _ctx.SaveChangesAsync();
        }
    }
}
