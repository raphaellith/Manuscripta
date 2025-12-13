using Main.Models.Entities.Materials;
using Main.Models.Entities.Questions;
using Main.Services.Repositories;

namespace Main.Services;

/// <summary>
/// Service for distributing materials to devices.
/// Implements API Contract.md §2.5 and Session Interaction.md §3.
/// </summary>
public class DistributionService : IDistributionService
{
    private readonly IMaterialRepository _materialRepository;
    private readonly IQuestionRepository _questionRepository;
    private readonly IDeviceRegistryService _deviceRegistry;

    private readonly Dictionary<Guid, HashSet<Guid>> _deviceMaterialAssignments = new();
    private readonly object _lock = new();

    public DistributionService(
        IMaterialRepository materialRepository,
        IQuestionRepository questionRepository,
        IDeviceRegistryService deviceRegistry)
    {
        _materialRepository = materialRepository ?? throw new ArgumentNullException(nameof(materialRepository));
        _questionRepository = questionRepository ?? throw new ArgumentNullException(nameof(questionRepository));
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
        }

        // Fetch materials
        var materials = new List<MaterialEntity>();
        foreach (var materialId in materialIds)
        {
            var material = await _materialRepository.GetByIdAsync(materialId);
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
            var materialQuestions = await _questionRepository.GetByMaterialIdAsync(material.Id);
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
