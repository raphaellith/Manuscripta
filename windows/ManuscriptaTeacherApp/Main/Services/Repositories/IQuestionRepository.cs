using System.Collections.Generic;
using System.Threading.Tasks;
using Main.Models.Entities;

namespace Main.Services.Repositories;

public interface IQuestionRepository
{
    Task<QuestionEntity?> GetByIdAsync(int id);
    Task<IEnumerable<QuestionEntity>> GetByMaterialIdAsync(int materialId);
}
