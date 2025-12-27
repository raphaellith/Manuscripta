using System;
using System.Collections.Generic;
using System.Threading.Tasks;
using Main.Models.Entities;

namespace Main.Services;

/// <summary>
/// Service interface for managing lessons.
/// </summary>
public interface ILessonService
{
    Task<LessonEntity> CreateAsync(LessonEntity entity);
    Task<LessonEntity?> GetByIdAsync(Guid id);
    Task<IEnumerable<LessonEntity>> GetByUnitIdAsync(Guid unitId);
    Task<IEnumerable<LessonEntity>> GetAllAsync();
    Task<LessonEntity> UpdateAsync(LessonEntity entity);
    Task DeleteAsync(Guid id);
}
