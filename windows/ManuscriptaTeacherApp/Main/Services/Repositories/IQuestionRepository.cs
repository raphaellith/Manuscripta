using System.Collections.Generic;
using System.Threading.Tasks;
using Main.Models.Entities.Questions;

namespace Main.Services.Repositories;

public interface IQuestionRepository
{
    Task<QuestionEntity?> GetByIdAsync(Guid id);
    Task<IEnumerable<QuestionEntity>> GetByMaterialIdAsync(Guid materialId);
    Task AddAsync(QuestionEntity entity);
    Task UpdateAsync(QuestionEntity entity);
    Task DeleteAsync(Guid id);
}
