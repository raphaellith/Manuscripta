using System;
using System.Threading.Tasks;
using Main.Services;
using Main.Models;

namespace Main.Services.RuntimeDependencies
{
    public class RmapiRuntimeDependencyManager : RuntimeDependencyManagerBase
    {
        private readonly IRmapiService _rmapiService;

        /// <summary>
        /// Initializes a new instance of the <see cref="RmapiRuntimeDependencyManager"/> class.
        /// </summary>
        /// <param name="rmapiService">The underlying rmapi service to wrap.</param>
        public RmapiRuntimeDependencyManager(IRmapiService rmapiService)
        {
            _rmapiService = rmapiService;
        }

        /// <summary>
        /// Unique identifier for the purpose of communication between frontend and backend.
        /// Per BackendRuntimeDependencyManagementSpecification §1(7).
        /// </summary>
        public override string DependencyId => "rmapi";

        // Since the template method architecture dictates state, we define the temp zip paths once logically
        private string? _tempZipPath;
        private string? _tempExtractPath;

        /// <summary>
        /// Checks whether the dependency is available and functional.
        /// Per BackendRuntimeDependencyManagementSpecification §2(2)(a).
        /// </summary>
        public override Task<bool> CheckDependencyAvailabilityAsync()
        {
            return _rmapiService.CheckAvailabilityAsync();
        }

        protected override async Task DownloadDependencyAsync(IProgress<RuntimeDependencyProgress> progress)
        {
            var downloadUrl = $"https://github.com/ddvk/rmapi/releases/download/{RmapiService.RmapiReleaseVersion}/rmapi-win64.zip";
            _tempZipPath = System.IO.Path.GetTempFileName();
            _tempExtractPath = System.IO.Path.Combine(System.IO.Path.GetTempPath(), System.IO.Path.GetRandomFileName());
            
            await _rmapiService.DownloadDependencyAsync(downloadUrl, _tempZipPath, progress);
        }

        protected override async Task VerifyDownloadAsync(IProgress<RuntimeDependencyProgress> progress)
        {
            if (_tempZipPath == null) throw new InvalidOperationException("Download was not initialized properly prior to verification.");
            await _rmapiService.VerifyDownloadAsync(_tempZipPath, progress);
        }

        protected override async Task PerformInstallDependencyAsync(IProgress<RuntimeDependencyProgress> progress)
        {
            if (_tempZipPath == null || _tempExtractPath == null) throw new InvalidOperationException("Download was not initialized properly prior to installation.");
            
            try
            {
                await _rmapiService.InstallExtractedAsync(_tempZipPath, _tempExtractPath, progress);
            }
            finally
            {
                if (System.IO.File.Exists(_tempZipPath)) System.IO.File.Delete(_tempZipPath);
                if (System.IO.Directory.Exists(_tempExtractPath)) System.IO.Directory.Delete(_tempExtractPath, true);
            }
        }

        /// <summary>
        /// Uninstalls the dependency.
        /// Per BackendRuntimeDependencyManagementSpecification §2(2)(c).
        /// </summary>
        public override Task<bool> UninstallDependencyAsync()
        {
            return _rmapiService.UninstallAsync();
        }

        /// <summary>
        /// Provides one domain-specific service instance to manage and use the dependency.
        /// Per BackendRuntimeDependencyManagementSpecification §2(3).
        /// </summary>
        public override Task<IDependencyService> GetDependencyServiceAsync()
        {
            return Task.FromResult<IDependencyService>(_rmapiService);
        }
    }
}
