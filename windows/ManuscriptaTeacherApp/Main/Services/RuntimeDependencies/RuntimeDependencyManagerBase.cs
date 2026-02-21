using System;
using System.Threading.Tasks;
using Main.Models;

namespace Main.Services.RuntimeDependencies
{
    /// <summary>
    /// Service abstract blueprint class for managing runtime dependencies.
    /// Per BackendRuntimeDependencyManagementSpecification §2(1).
    /// </summary>
    public abstract class RuntimeDependencyManagerBase
    {
        /// <summary>
        /// Unique identifier for the purpose of communication between frontend and backend.
        /// Per BackendRuntimeDependencyManagementSpecification §1(7).
        /// </summary>
        public abstract string DependencyId { get; }

        /// <summary>
        /// Checks whether the dependency is available and functional.
        /// Per BackendRuntimeDependencyManagementSpecification §2(2)(a).
        /// </summary>
        public abstract Task<bool> CheckDependencyAvailabilityAsync();

        /// <summary>
        /// Installs the dependency using the sequence defined in BackendRuntimeDependencyManagementSpecification §2(2)(b) and §2(2A).
        /// </summary>
        public async Task<bool> InstallDependencyAsync(IProgress<RuntimeDependencyProgress> progress)
        {
            try
            {
                progress?.Report(new RuntimeDependencyProgress { Phase = "Downloading" });
                await DownloadDependencyAsync(progress);

                progress?.Report(new RuntimeDependencyProgress { Phase = "Verifying" });
                await VerifyDownloadAsync(progress);

                progress?.Report(new RuntimeDependencyProgress { Phase = "Installing" });
                await PerformInstallDependencyAsync(progress);

                return await CheckDependencyAvailabilityAsync();
            }
            catch (Exception ex)
            {
                progress?.Report(new RuntimeDependencyProgress { Phase = "Failed", ErrorMessage = ex.Message });
                throw;
            }
        }

        /// <summary>
        /// Downloads the dependency files from a specified source.
        /// Per BackendRuntimeDependencyManagementSpecification §2(2A)(a).
        /// </summary>
        protected abstract Task DownloadDependencyAsync(IProgress<RuntimeDependencyProgress> progress);

        /// <summary>
        /// Verifies the downloaded files using a specified method, such as a hash or signature.
        /// Per BackendRuntimeDependencyManagementSpecification §2(2A)(b).
        /// </summary>
        protected abstract Task VerifyDownloadAsync(IProgress<RuntimeDependencyProgress> progress);

        /// <summary>
        /// Installs the downloaded dependency.
        /// Per BackendRuntimeDependencyManagementSpecification §2(2A)(c).
        /// </summary>
        protected abstract Task PerformInstallDependencyAsync(IProgress<RuntimeDependencyProgress> progress);

        /// <summary>
        /// Uninstalls the dependency.
        /// Per BackendRuntimeDependencyManagementSpecification §2(2)(c).
        /// </summary>
        public abstract Task<bool> UninstallDependencyAsync();

        /// <summary>
        /// Provides one domain-specific service instance to manage and use the dependency.
        /// Per BackendRuntimeDependencyManagementSpecification §2(3).
        /// </summary>
        public abstract Task<IDependencyService> GetDependencyServiceAsync();
    }
}
