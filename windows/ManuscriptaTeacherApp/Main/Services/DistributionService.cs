using Microsoft.Extensions.DependencyInjection;
using Main.Models.Entities.Materials;
using Main.Models.Entities.Questions;
using Main.Services.Repositories;

namespace Main.Services;

/// <summary>
/// Service for distributing materials to devices.
/// Implements API Contract.md §2.5 and Session Interaction.md §3.
/// </summary>
/// <remarks>
/// This service is registered as a singleton but needs to access scoped services (repositories).
/// It uses IServiceProvider to create scopes when resolving scoped dependencies to avoid captive dependency issues.
/// </remarks>
public class DistributionService : IDistributionService
{
    private readonly IServiceProvider _serviceProvider;
    private readonly IDeviceRegistryService _deviceRegistry;

    private readonly Dictionary<Guid, HashSet<Guid>> _deviceMaterialAssignments = new();
    private readonly object _lock = new();

    public DistributionService(
        IServiceProvider serviceProvider,
        IDeviceRegistryService deviceRegistry)
    {
        _serviceProvider = serviceProvider ?? throw new ArgumentNullException(nameof(serviceProvider));
        _deviceRegistry = deviceRegistry ?? throw new ArgumentNullException(nameof(deviceRegistry));
    }

    /// <inheritdoc />
    public async Task<DistributionBundle?> GetDistributionBundleAsync(Guid deviceId)
    {
        // Verify device is paired
        if (!await _deviceRegistry.IsDevicePairedAsync(deviceId))
        {
            return null;
        }

        // Get assigned material IDs for this device
        HashSet<Guid>? materialIds;
        lock (_lock)
        {
            if (!_deviceMaterialAssignments.TryGetValue(deviceId, out materialIds) || materialIds.Count == 0)
            {
                return null;
            }
            // Copy to avoid holding lock during async operations
            materialIds = new HashSet<Guid>(materialIds);
        }

        // Create scope to resolve scoped repositories
        using var scope = _serviceProvider.CreateScope();
        var materialRepository = scope.ServiceProvider.GetRequiredService<IMaterialRepository>();
        var questionRepository = scope.ServiceProvider.GetRequiredService<IQuestionRepository>();

        // Fetch materials
        var materials = new List<MaterialEntity>();
        foreach (var materialId in materialIds)
        {
            var material = await materialRepository.GetByIdAsync(materialId);
            if (material != null)
            {
                materials.Add(material);
            }
        }

        if (materials.Count == 0)
        {
            return null;
        }

        // Fetch questions for all materials
        var questions = new List<QuestionEntity>();
        foreach (var material in materials)
        {
            var materialQuestions = await questionRepository.GetByMaterialIdAsync(material.Id);
            questions.AddRange(materialQuestions);
        }

        return new DistributionBundle(materials, questions);
    }

    /// <inheritdoc />
    public Task AssignMaterialsToDeviceAsync(Guid deviceId, IEnumerable<Guid> materialIds)
    {
        if (materialIds == null)
            throw new ArgumentNullException(nameof(materialIds));

        lock (_lock)
        {
            if (!_deviceMaterialAssignments.TryGetValue(deviceId, out var assignments))
            {
                assignments = new HashSet<Guid>();
                _deviceMaterialAssignments[deviceId] = assignments;
            }

            foreach (var materialId in materialIds)
            {
                assignments.Add(materialId);
            }
        }

        return Task.CompletedTask;
    }

    /// <inheritdoc />
    public Task ClearDeviceAssignmentsAsync(Guid deviceId)
    {
        lock (_lock)
        {
            _deviceMaterialAssignments.Remove(deviceId);
        }

        return Task.CompletedTask;
    }
}

