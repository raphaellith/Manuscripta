using System.Collections.Generic;
using System.Threading.Tasks;
using Main.Models.Entities;

namespace Main.Services.Repositories;

public interface IResponseRepository
{
    Task<ResponseEntity?> GetByIdAsync(int id);
    Task<IEnumerable<ResponseEntity>> GetByQuestionIdAsync(int questionId);
}
