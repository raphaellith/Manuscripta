# Windows App Structure

## Explanatory Note

This document defines the structure of the Windows app, and the directories and files which should be used to implement it. It should be updated as the app is developed.

## Section 1 - General Principles

(1) This document may be cited as "Windows App Structure Specification".

(2) The app consists of the following functional components:

    (a) `Main` (placed under `windows/ManuscriptaTeacherApp/Main`). This is an ASP.NET Core Web Application, which handles material models and networking.
    (b) `UI` (placed under `windows/ManuscriptaTeacherApp/UI`). This is a light-weight front-end component which provides the teacher user interface, written in React and TypeScript as a Electron application.
    (c) `GenAI` (placed under `windows/ManuscriptaTeacherApp/GenAI`). This is a light-weight component which provides the GenAI functionality. 

(3) In addition to the above components, the app also includes the following:

    (a) `MainTests` (placed under `windows/ManuscriptaTeacherApp/MainTests`). This is a test project which tests the Main component.

## Section 2A - Directory Structure of `Main`

(1) **Directory Structure**

    (a) The `Main` component uses the following directory structure:

        ```
        Main/
        |- Models/
        | |- Dtos/
        | |- Entities/
        | |- Enums/
        | |- Events/
        | |- Mappings/
        | |- Network/
        |- Data/
        |- Services/
        | |- Network/
        | |- Repositories/
        | |- Hubs/
        |- Controllers/
        ```

    (b) The `Data` directory shall contain one single file, `MainDbContext.cs`, which defines the database context for the app.

    This shall be referred to as the 'Data layer'.

    (c) The `Models` directory shall contain the following files and subdirectories only:
        (i) `Dtos/`. This directory shall contain Data Transfer Objects (DTOs). DTOs shall be used only when models defined in the Entity subdirectory are not suitable for use in networking, due to having properties unique to the Windows app.
        (ii) `Entities/`. This directory shall contain Entity classes, subject to the Validation Rules. Entities placed directly in this directory shall be Data Entities, which shall be suitable for the use in persistence. Additional polymorphic domain entities, and potentially their subclasses, shall be placed in appropriate subdirectories of this directory, indicating the domain they belong to.
        (iii) `Enums/`. This directory shall contain Enum classes, subject to the Validation Rules, which are used by the Entity classes.
        (iv) `Events/`. This directory shall contain Event classes, which defines event arguments.
        (v) `Mappings/`. This directory shall contain Mapping classes, which defines the mapping between data entities and polymorphic domain entities.
        (vi) `Network/`. This directory shall contain a registry of TCP and UDP messages, and corresponding encoding and decoding classes.
    
    This shall be referred to as the 'Model layer'.

    (d) The `Services` directory shall contain the following files and subdirectories only:
        (i) `Network/`. This directory shall contain services regarding TCP and UDP networking. 
        (ii) `Repositories/`. This directory shall contain services regarding persistence.
        (iii) `Hubs/`. This directory shall contain SignalR Hub classes for real-time bidirectional communication with the `UI` component.
        (iv) Any other services which are not related to networking, persistence, or SignalR shall be placed directly in this directory.
    
    This shall be referred to as the 'Service layer'.

    (e) The `Controllers` directory shall contain ASP.NET Core controllers, providing the REST APIs specified in the API Contract for external device communication.

    This shall be referred to as the 'Controller layer'.

(2) **Hierarchy of Layers**

    (a) In this subsection -
        (i) An 'upstream layer', in respect to a layer L, means a layer which L will make calls to.
        (ii) A 'downstream layer', in respect to a layer L, means a layer which treats L as an upstream layer.
        (iii) A 'peer layer', in respect to a layer L, means a layer which is neither an upstream layer nor a downstream layer of L.
    
    (b) The relationships between layers are set out below, and 'downstream layer' relationships shall be constructed accordingly:
        (i) The Data layer shall be an upstream layer of the Model layer.
        (ii) The Model layer shall be an upstream layer of the Service layer.
        (iii) The Service layer shall be an upstream layer of the Controller layer.

(3) **Interface Rules**
    
    (a) Unless otherwise allowed, no layer may make call to a layer which is not its upstream layer or peer layer.

    (b) No service may access Data Entities directly without using polymorphic Entity classes.

    (c) The Service layer should be functionally complete in relation to any downstream layer. That is to say, the service layer should remove the need of any downstream layer to access any upstream layers directly.

    (d) The Service layer, collectively with all upstream layers, must enforce all constraints defined in the Data Validation Specification.

## Section 2B - Directory Structure of `UI`

(1) **Directory Structure**

    (a) The `UI` component uses the following directory structure:

        ```
        UI/
        |- package.json
        |- tsconfig.json
        |- forge.config.ts
        |- webpack.*.ts
        |- src/
        |  |- main/
        |  |- preload/
        |  |- renderer/
        |  |  |- components/
        |  |  |  |- layout/
        |  |  |  |- pages/
        |  |  |  |- modals/
        |  |  |  |- common/
        |  |  |- services/
        |  |  |  |- signalr/
        |  |  |- models/
        |  |  |- state/
        |  |  |- themes/
        |  |- resources/
        ```

    (b) The `main` directory shall contain the Electron main process entry point (`index.ts`), which is responsible for creating and managing the application window.

    (c) The `preload` directory shall contain the Electron preload script (`preload.ts`), which exposes secure APIs to the renderer process.

    (d) The `renderer` directory shall contain the React application and shall include the following subdirectories:
        (i) `components/`. This directory shall contain React components, organised into the following subdirectories:
            - `layout/`. Components responsible for page structure, such as headers and sidebars.
            - `pages/`. Page-level components corresponding to application views.
            - `modals/`. Modal dialog components.
            - `common/`. Shared, reusable components.
        (ii) `services/`. This directory shall contain communication logic with the `Main` component:
            - `signalr/`. SignalR hub connection, event handlers, and method invocations for real-time communication with `Main`.
        (iii) `models/`. This directory shall contain TypeScript type definitions. Types should be aligned with DTOs and Entities defined in the `Main` component.
        (iv) `state/`. This directory shall contain React Context providers and custom hooks for state management.
        (v) `themes/`. This directory shall contain theme configuration files.

    This shall be referred to as the 'Renderer layer'.

    (e) The `resources` directory shall contain static assets such as images, icons, and documents.

(2) **Communication with Main**

    (a) The `UI` component shall communicate with the `Main` component via SignalR hub connection at `/hub` for real-time bidirectional communication.

    (b) All communication logic shall be encapsulated within the `services` directory.

    (c) No component outside of the `services` directory may make direct SignalR calls.

(3) **State Management**

    (a) State management shall use React Context and simple props, depending on complexity.

    (b) Global state that must be shared across multiple pages shall be managed via React Context providers in the `state` directory.

    (c) Local component state shall use React's `useState` hook directly.
    