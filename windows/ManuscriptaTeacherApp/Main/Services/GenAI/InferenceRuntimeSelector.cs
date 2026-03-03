using Microsoft.EntityFrameworkCore;
using Main.Data;
using Main.Models.Entities;

namespace Main.Services.GenAI;

/// <summary>
/// Implementation of IInferenceRuntimeSelector.
/// Manages the user's inference runtime preference and hardware detection.
/// </summary>
public class InferenceRuntimeSelector : IInferenceRuntimeSelector
{
    private readonly IServiceProvider _serviceProvider;
    private readonly ILogger<InferenceRuntimeSelector> _logger;

    public InferenceRuntimeSelector(IServiceProvider serviceProvider, ILogger<InferenceRuntimeSelector> logger)
    {
        _serviceProvider = serviceProvider;
        _logger = logger;
    }

    public async Task<InferenceRuntime> GetActiveRuntimeAsync()
    {
        using var scope = _serviceProvider.CreateScope();
        var dbContext = scope.ServiceProvider.GetRequiredService<MainDbContext>();

        var preference = await dbContext.InferencePreferences.FirstOrDefaultAsync();
        if (preference != null)
        {
            return preference.PreferredRuntime;
        }

        // If no preference is set, default based on hardware detection
        var hasNpu = await DetectNpuHardwareAsync();
        var defaultRuntime = hasNpu ? InferenceRuntime.OPENVINO : InferenceRuntime.STANDARD;
        
        _logger.LogInformation("No runtime preference found. Defaulting to {Runtime} based on NPU detection ({HasNpu}).", defaultRuntime, hasNpu);
        return defaultRuntime;
    }

    public async Task SwitchRuntimeAsync(InferenceRuntime newRuntime)
    {
        using var scope = _serviceProvider.CreateScope();
        var dbContext = scope.ServiceProvider.GetRequiredService<MainDbContext>();

        var preference = await dbContext.InferencePreferences.FirstOrDefaultAsync();
        if (preference == null)
        {
            preference = new InferencePreferenceEntity(Guid.NewGuid(), newRuntime);
            await dbContext.InferencePreferences.AddAsync(preference);
        }
        else
        {
            preference.PreferredRuntime = newRuntime;
            dbContext.InferencePreferences.Update(preference);
        }

        await dbContext.SaveChangesAsync();
        _logger.LogInformation("Switched inference runtime to {NewRuntime}.", newRuntime);
    }

    public Task<bool> DetectNpuHardwareAsync()
    {
        // Per GenAISpec.md §1F(3): "For the purpose of immediate implementation, 
        // this detection may be stubbed to return true if OpenVINO is supported by the developer."
        // We will return true here to enable OV-Ollama by default where preferred.
        
        _logger.LogInformation("DetectNpuHardwareAsync called. Returning stubbed value: true.");
        return Task.FromResult(true);
    }
}
