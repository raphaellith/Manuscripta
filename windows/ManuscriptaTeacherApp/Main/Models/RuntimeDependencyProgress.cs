namespace Main.Models
{
    /// <summary>
    /// Represents the progress of a runtime dependency installation.
    /// Used by the IProgress pattern defined in BackendRuntimeDependencyManagementSpecification §2(2).
    /// </summary>
    public class RuntimeDependencyProgress
    {
        public required string Phase { get; set; } // "Downloading", "Verifying", "Installing", "Completed", "Failed"
        public int? ProgressPercentage { get; set; }
        public string? ErrorMessage { get; set; }
    }
}
