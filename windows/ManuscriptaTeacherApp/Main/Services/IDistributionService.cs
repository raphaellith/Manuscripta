using Main.Models.Entities.Materials;
using Main.Models.Entities.Questions;

namespace Main.Services;

/// <summary>
/// Service interface for distributing materials to devices.
/// Implements API Contract.md §2.5 and Session Interaction.md §3.
/// </summary>
public interface IDistributionService
{
    /// <summary>
    /// Gets the distribution bundle for a specific device.
    /// Per API Contract.md §2.5: Returns materials and questions assigned to a device.
    /// </summary>
    /// <param name="deviceId">The device ID to get materials for.</param>
    /// <returns>A tuple of materials and questions, or null if no materials are available.</returns>
    Task<DistributionBundle?> GetDistributionBundleAsync(Guid deviceId);

    /// <summary>
    /// Assigns materials to a device for distribution.
    /// Per Session Interaction.md §3(1): Teacher selects materials for distribution.
    /// </summary>
    /// <param name="deviceId">The device ID to assign materials to.</param>
    /// <param name="materialIds">The IDs of materials to assign.</param>
    Task AssignMaterialsToDeviceAsync(Guid deviceId, IEnumerable<Guid> materialIds);

    /// <summary>
    /// Clears all material assignments for a device.
    /// </summary>
    /// <param name="deviceId">The device ID to clear assignments for.</param>
    Task ClearDeviceAssignmentsAsync(Guid deviceId);
}

/// <summary>
/// Represents a distribution bundle containing materials and questions.
/// Per API Contract.md §2.5: Response format.
/// </summary>
public record DistributionBundle(
    IEnumerable<MaterialEntity> Materials,
    IEnumerable<QuestionEntity> Questions);
