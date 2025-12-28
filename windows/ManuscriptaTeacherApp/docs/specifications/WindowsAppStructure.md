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
        (iii) Any other services which are not related to networking or persistence shall be placed directly in this directory.
    
    This shall be referred to as the 'Service layer'.

    (e) The `Controllers` directory shall contain ASP.NET Core controllers, providing the REST APIs specified in the API Contract.

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
    