using System.Collections.Generic;

namespace Main.Services.RuntimeDependencies
{
    /// <summary>
    /// Registry for mapping dependency IDs to their respective managers.
    /// Supports the generic handling requirement in BackendRuntimeDependencyManagementSpecification §1(5) and §2(1).
    /// </summary>
    public interface IRuntimeDependencyRegistry
    {
        /// <summary>
        /// Retrieves the runtime dependency manager associated with the given identifier.
        /// Per BackendRuntimeDependencyManagementSpecification §1(5) and §2(1).
        /// </summary>
        RuntimeDependencyManagerBase? GetManager(string dependencyId);
        /// <summary>
        /// Retrieves all registered runtime dependency managers.
        /// </summary>
        IEnumerable<RuntimeDependencyManagerBase> GetAllManagers();
    }
}
