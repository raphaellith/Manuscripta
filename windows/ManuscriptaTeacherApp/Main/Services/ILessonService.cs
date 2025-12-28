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
    Task<LessonEntity> UpdateAsync(LessonEntity entity);
    Task DeleteAsync(Guid id);
}
