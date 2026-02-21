# Backend Runtime Dependency Management Specification (Windows)

## Explanatory Note

This specification defines the requirement to provide and use a standardised interface for all dependencies to be checked and installed at runtime, including AI-related dependencies and integration with external platforms, such as reMarkable.

## Section 1 - General Principles

(1) In this specification, a "runtime dependency" means a piece of external software that is to be installed, upon user consent, at runtime.

(2) Runtime dependencies shall be preferred when —

    (a) the dependency is provided, as a standalone executable or otherwise, that cannot be reasonably be included as a nuget dependency;

    (b) there are strong arguments as to why a user may not wish to use any features requiring such dependency; or

    (c) the provision of such dependency at build time would be against the principle that "Your installer should only install the product intended by the user", as stipulated in the Microsoft App Store submission guidelines.

(3) Where a dependency is to be installed at a local scope, files necessary for runtime dependency management shall be stored in the `%AppData%\ManuscriptaTeacherApp\bin` directory. However, this subsection does not apply if the dependency can only be installed at a global scope.

(4) For the purposes of this specification —

    (a) "manage" in relation to a runtime dependency means to check, install and uninstall dependencies at runtime; and

    (b) "use" in relation to a runtime dependency means to call the dependency at runtime.

(5) The backend shall provide a standardised abstract blueprint class for all runtime dependencies to be managed or used at runtime, as specified in the following sections.

(6) No runtime dependency shall be managed or used beside the means provided by the standardised blueprint class.

(7) Each runtime dependency shall be provided with a unique identifier, for the purpose of communication between the frontend and the backend.


## Section 2 - Runtime Dependency Management Blueprint

(1) An abstract blueprint class, named `RuntimeDependencyManagerBase`, shall be provided for all runtime dependencies to be managed or used at runtime. An implementation inheriting this class shall be provided for each runtime dependency, and be instantiated as a singleton.

(2) The abstract class specified in this section shall require the following public methods for the purpose of dependency management:

    (a) Abstract `Task<Boolean>  CheckDependencyAvailabilityAsync()`: The derived classes shall override this method. Checks whether the dependency is available and functional. Returns `true` if the dependency is available and functional, and `false` otherwise. 

    (b) `Task<Boolean> InstallDependencyAsync()`: Calls `DownloadDependencyAsync()`, `VerifyDownloadAsync()`, and `PerformInstallDependencyAsync()` in sequence, as specified in the following sections, then calls `CheckDependencyAvailabilityAsync()` and returns the result. It shall throw an exception if any of the steps results in an exception. 

    (c) Abstract `Task<Boolean> UninstallDependencyAsync()`: The derived classes shall override this method. Uninstalls the dependency by removing the files related to that dependency. Returns `true` if the dependency is uninstalled, and `false` otherwise. This method shall only be applicable to dependencies installed at a local scope, and an exception shall be thrown if called on a dependency installed at a global scope.

(2A) To fulfill the purposes of paragraph (2)(b) above, the abstract methods defined there shall be implemented by derived classes as follows, and shall throw an exception if any of the steps results in an exception:

    (a) `Task DownloadDependencyAsync()`: Downloads the dependency files from a specified source.

    (b) `Task VerifyDownloadAsync()`: Verifies the downloaded files using a specified method, such as a hash or signature. Throws an exception if the verification fails. 

    This method may be implemented as no-op if verification is not feasible, but that decision shall be clearly justified in the form of comments.

    (c) `Task PerformInstallDependencyAsync()`: Installs the downloaded dependency, and in the scope of local dependencies, store files in the directory specified in Section 1(3) above.

(3) The abstract class shall provide a method `Task<IDependencyService> GetDependencyServiceAsync()` which shall —

    (a) call `CheckDependencyAvailabilityAsync()` and throw an exception if it returns false;
    
    (b) return an instance of a domain-specific service that implements the `IDependencyService` interface, by calling `ProvideDependencyServiceAsync()`.

(3A) Each derived class shall fulfil  `ProvideDependencyServiceAsync()`, as specified in paragraph (3)(b) above, by instantiating a singleton instance of the domain-specific service class, which implements the `IDependencyService` interface.


## Section 3 - Interaction with Frontend Regarding Runtime Dependencies

(1) The frontend shall by default, assume that all runtime dependencies are available and functional.

(2) When the frontend attempts to use a SignalR endpoint that would eventually require runtime dependency(ies) —

    (a) the backend shall notify the frontend that relevant runtime dependency(ies) have not been installed, using the `RuntimeDependencyNotInstalled` handler specified in the Networking API Specification Section 2(1)(f)(i); and

    (b) the frontend shall handle this notification in accordance with the Frontend Workflow Specifications Section 3A.



    

