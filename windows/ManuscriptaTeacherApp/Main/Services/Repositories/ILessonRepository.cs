using System;
using System.Collections.Generic;
using System.Threading.Tasks;
using Main.Models.Entities;

namespace Main.Services.Repositories;

public interface ILessonRepository
{
    Task<LessonEntity?> GetByIdAsync(Guid id);
    Task<IEnumerable<LessonEntity>> GetByUnitIdAsync(Guid unitId);
    Task<IEnumerable<LessonEntity>> GetAllAsync();
    Task AddAsync(LessonEntity entity);
    Task UpdateAsync(LessonEntity entity);
    Task DeleteAsync(Guid id);
}
