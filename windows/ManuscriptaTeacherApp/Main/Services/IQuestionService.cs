using System;
using System.Collections.Generic;
using System.Threading.Tasks;
using Main.Models.Entities.Questions;

namespace Main.Services;

/// <summary>
/// Service interface for managing questions.
/// </summary>
public interface IQuestionService
{
    Task<QuestionEntity> CreateQuestionAsync(QuestionEntity question);
    Task<QuestionEntity?> GetQuestionByIdAsync(Guid id);
    Task<IEnumerable<QuestionEntity>> GetQuestionsByMaterialIdAsync(Guid materialId);
    Task<QuestionEntity> UpdateQuestionAsync(QuestionEntity question);
    Task DeleteQuestionAsync(Guid id);
}
