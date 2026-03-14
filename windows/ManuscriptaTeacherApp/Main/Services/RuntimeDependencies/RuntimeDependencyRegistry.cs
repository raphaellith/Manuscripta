using System.Collections.Generic;

namespace Main.Services.RuntimeDependencies
{
    /// <summary>
    /// Implementation of the runtime dependency registry.
    /// Per BackendRuntimeDependencyManagementSpecification §2(1).
    /// </summary>
    public class RuntimeDependencyRegistry : IRuntimeDependencyRegistry
    {
        private readonly Dictionary<string, RuntimeDependencyManagerBase> _managers = new();

        /// <summary>
        /// Registers a new runtime dependency manager.
        /// Per BackendRuntimeDependencyManagementSpecification §2(1).
        /// </summary>
        public void Register(RuntimeDependencyManagerBase manager)
        {
            _managers[manager.DependencyId] = manager;
        }

        /// <summary>
        /// Retrieves the runtime dependency manager associated with the given identifier.
        /// Per BackendRuntimeDependencyManagementSpecification §1(5) and §2(1).
        /// </summary>
        public RuntimeDependencyManagerBase? GetManager(string dependencyId)
        {
            _managers.TryGetValue(dependencyId, out var manager);
            return manager;
        }

        /// <summary>
        /// Retrieves all registered runtime dependency managers.
        /// </summary>
        public IEnumerable<RuntimeDependencyManagerBase> GetAllManagers() => _managers.Values;
    }
}
