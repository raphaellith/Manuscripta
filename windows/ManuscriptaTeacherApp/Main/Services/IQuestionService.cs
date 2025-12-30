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
    Task<QuestionEntity> UpdateQuestionAsync(QuestionEntity question);
    Task DeleteQuestionAsync(Guid id);
}
