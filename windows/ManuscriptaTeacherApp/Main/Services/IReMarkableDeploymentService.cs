namespace Main.Services;

/// <summary>
/// Service interface for deploying materials to reMarkable devices.
/// Per RemarkableIntegrationSpecification §4.
/// </summary>
public interface IReMarkableDeploymentService
{
    /// <summary>
    /// Deploys a material to the specified reMarkable devices.
    /// Per RemarkableIntegrationSpecification §4(2): generates PDF, uploads to each device's cloud.
    /// Per NetworkingAPISpec §1(1)(n)(vii).
    /// </summary>
    /// <param name="materialId">The ID of the material to deploy.</param>
    /// <param name="deviceIds">The IDs of the reMarkable devices to deploy to.</param>
    /// <returns>A deployment result for each device.</returns>
    /// <exception cref="KeyNotFoundException">Thrown when the material does not exist.</exception>
    Task<List<ReMarkableDeploymentResult>> DeployAsync(Guid materialId, List<Guid> deviceIds);
}

/// <summary>
/// Result of a deployment to a single reMarkable device.
/// </summary>
public class ReMarkableDeploymentResult
{
    public Guid DeviceId { get; init; }
    public bool Success { get; init; }
    public bool AuthFailed { get; init; }
    public string? ErrorMessage { get; init; }
}
