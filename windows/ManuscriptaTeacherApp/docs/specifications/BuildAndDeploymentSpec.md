# Build and Deployment Specification (Windows)

## Explanatory Note

This document specifies the build and deployment requirements for the Windows application, including the integration of the backend and frontend components into a single distributable package.

For runtime behaviour regarding the frontend's responsibility to start and manage the backend process, see `FrontendWorkflowSpecifications.md` Section 2ZA.


## Section 1 — General Principles

(1) The Windows application consists of two components —

    (a) the **Main** component, an ASP.NET Core backend application; and

    (b) the **UI** component, an Electron frontend application.

(2) In production, both components shall be bundled together into a single Windows installer, such that —

    (a) the end user installs a single application;

    (b) the frontend component manages the lifecycle of the backend component at runtime, as specified in `FrontendWorkflowSpecifications.md` Section 2ZA.

(3) The build process shall be designed for a single-user installation that does not require administrative privileges.


## Section 2 — Build Prerequisites

(1) The following tools shall be available on the build machine —

    (a) **.NET SDK** version 10.0 or later, for building the backend component.

    (b) **Node.js** version 20.0 or later, with npm, for building the frontend component.

    (c) **Git**, for source control operations.

(2) For GitHub Actions workflows, the runner shall be `windows-latest`.


## Section 3 — Backend Build Requirements

(1) **Project Location**

    The backend project is located at `windows/ManuscriptaTeacherApp/Main/Manuscripta.Main.csproj`.

(2) **Publish Configuration**

    The backend shall be published as a self-contained executable with the following parameters —

    (a) **Configuration**: Release

    (b) **Runtime Identifier**: `win-x64`

    (c) **Self-Contained**: true

    (d) **Output Directory**: `windows/ManuscriptaTeacherApp/UI/resources/backend`

(3) **Output Artifacts**

    The publish process shall produce —

    (a) `Manuscripta.Main.exe`, the self-contained backend executable;

    (b) all required .NET runtime libraries and dependencies;

    (c) `appsettings.json`, the backend configuration file.

(4) **Exclusions**

    The `Testing` directory and its contents shall be excluded from the published output.


## Section 4 — Frontend Build Requirements

(1) **Project Location**

    The frontend project is located at `windows/ManuscriptaTeacherApp/UI/`.

(2) **Dependency Installation**

    npm dependencies shall be installed using the `ci` command for reproducible builds.

(3) **Electron Forge Configuration**

    (a) The frontend shall be built using Electron Forge.

    (b) The packager configuration shall include an `extraResource` entry pointing to the backend output directory, ensuring the backend executable is bundled alongside the Electron application outside the ASAR archive.

(4) **Output Artifacts**

    The build process shall produce —

    (a) a Squirrel-based Windows installer;

    (b) the installer shall include both the Electron application and the bundled backend.


## Section 5 — Combined Build Pipeline

(1) **Build Order**

    The components shall be built in the following order —

    (a) Build and publish the backend component per Section 3.

    (b) Build the frontend component per Section 4.

    [Explanatory Note: This order is mandatory because the frontend build process bundles the backend output as an extra resource.]

(2) **Pre-build Cleanup**

    Before each build, the following directories shall be cleaned to ensure a fresh build —

    (a) `windows/ManuscriptaTeacherApp/UI/resources/backend/`

    (b) `windows/ManuscriptaTeacherApp/UI/out/`

(3) **Verification**

    After a successful build, the following shall be verified —

    (a) the backend executable exists at `resources/backend/Manuscripta.Main.exe`;

    (b) the installer exists in the Squirrel output directory.


## Section 6 — GitHub Actions Workflow

(1) **Workflow Purpose**

    A GitHub Actions workflow shall be created to automate the build process and produce a deployment-ready Windows executable.

(2) **Workflow Trigger**

    The workflow shall be triggered —

    (a) **manually**, via `workflow_dispatch`, allowing on-demand builds with optional version input;

    (b) **on release**, when a GitHub release is created or published.

(3) **Workflow File Location**

    The workflow shall be defined at `.github/workflows/windows-build-release.yml`.

(4) **Workflow Steps**

    The workflow shall —

    (a) checkout the repository;

    (b) set up the required .NET SDK and Node.js versions;

    (c) clean previous build artifacts;

    (d) restore backend dependencies and publish the backend as a self-contained executable;

    (e) install frontend dependencies and build the frontend using Electron Forge;

    (f) verify the build outputs;

    (g) upload the generated installer as a workflow artifact with 90-day retention;

    (h) if triggered by a release event, attach the installer to the GitHub release.

(5) **Workflow Inputs**

    When triggered via `workflow_dispatch`, the workflow shall accept —

    (a) **version** (optional): A version string to override the default version in `package.json`.

(6) **Workflow Outputs**

    The workflow shall produce a GitHub Actions artifact named `windows-installer` containing the Squirrel installer.


## Section 7 — Forge Configuration Requirements

(1) **Extra Resource Configuration**

    The `forge.config.ts` file shall include an `extraResource` configuration in `packagerConfig` pointing to `./resources/backend`.

(2) **Resource Path at Runtime**

    (a) In packaged mode, the bundled backend shall be accessible relative to `process.resourcesPath`.

    (b) In development mode, a configurable fallback path shall be used.


## Section 8 — Versioning

(1) **Version Source of Truth**

    The application version shall be defined in `windows/ManuscriptaTeacherApp/UI/package.json` under the `version` field.

(2) **Version Synchronisation**

    (a) The backend does not define an independent version; it is bundled with the frontend.

    (b) The installer version shall match the `package.json` version.

(3) **Version Override**

    The GitHub Actions workflow shall permit a version override via `workflow_dispatch` input for release candidates, hotfixes, and pre-release versioning.


## Appendix 1 — Local Development Workflow

(1) **Independent Development**

    During development, the backend and frontend may be run independently —

    (a) The backend may be started via `dotnet run` from the `Main` directory —

    ```
    cd windows/ManuscriptaTeacherApp/Main
    dotnet run
    ```

    (b) The frontend may be started via `npm start` from the `UI` directory —

    ```
    cd windows/ManuscriptaTeacherApp/UI
    npm start
    ```

    (c) Per `FrontendWorkflowSpecifications.md` Section 2ZA(3)(b), the frontend shall detect an already-running backend and skip spawning a new process.

(2) **Combined Development Build**

    To test the combined build locally, developers should —

    (a) clean previous build artifacts —

    ```
    Remove-Item -Recurse -Force windows/ManuscriptaTeacherApp/UI/resources/backend/ -ErrorAction SilentlyContinue
    Remove-Item -Recurse -Force windows/ManuscriptaTeacherApp/UI/out/ -ErrorAction SilentlyContinue
    ```

    (b) publish the backend to `UI/resources/backend/` —

    ```
    dotnet publish windows/ManuscriptaTeacherApp/Main/Manuscripta.Main.csproj `
        --configuration Release `
        --runtime win-x64 `
        --self-contained true `
        --output windows/ManuscriptaTeacherApp/UI/resources/backend
    ```

    (c) install frontend dependencies —

    ```
    cd windows/ManuscriptaTeacherApp/UI
    npm ci
    ```

    (d) build the frontend installer —

    ```
    cd windows/ManuscriptaTeacherApp/UI
    npm run make
    ```

    (e) install and run the generated installer from the `UI/out/` directory to verify end-to-end functionality.

(3) **Gitignore Configuration**

    The following paths should be excluded from version control —

    (a) `windows/ManuscriptaTeacherApp/UI/resources/backend/`

    (b) `windows/ManuscriptaTeacherApp/UI/out/`