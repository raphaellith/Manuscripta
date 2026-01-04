using System.Threading.Tasks;
using Main.Models.Entities;

namespace Main.Services;

public interface IFeedbackService
{
    Task<FeedbackEntity> CreateFeedbackAsync(FeedbackEntity feedback);
}
