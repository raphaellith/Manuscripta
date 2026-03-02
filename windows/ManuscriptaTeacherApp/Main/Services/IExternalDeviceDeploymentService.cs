using Main.Models.Entities;

namespace Main.Services;

/// <summary>
/// Status of an external device deployment (success/failure, and generic or auth errors).
/// </summary>
public class ExternalDeviceDeploymentResult
{
    public Guid DeviceId { get; set; }
    public bool Success { get; set; }
    public bool AuthFailed { get; set; }
    public string? ErrorMessage { get; set; }
}

/// <summary>
/// Service for deploying materials to abstract external devices (reMarkable/Kindle).
/// </summary>
public interface IExternalDeviceDeploymentService
{
    /// <summary>
    /// Deploys a material (by generating a PDF) to the specified external devices.
    /// Differentiates dispatch mechanism based on the external device type.
    /// </summary>
    Task<List<ExternalDeviceDeploymentResult>> DeployAsync(Guid materialId, List<Guid> deviceIds);
}
