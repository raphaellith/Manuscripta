using System.Threading.Tasks;
using Main.Models.Entities;

namespace Main.Services.Repositories;

public interface IMaterialRepository
{
    Task<MaterialEntity?> GetByIdAsync(int id);
}
