# Persistence and Cascading Rules

## Explanatory Note

This document defines the persistence rules, and the mandatory cascading rules regarding persistence and deletion. 

This document is effective for the Windows app only.

## Section 1 - Requirements for Persistence

(1) Unless otherwise specified, any create, update and delete operations on the following data entity classes must be long-term persisted, as soon as it is practical:

    (a) `MaterialDataEntity`.
    (b) `QuestionDataEntity`.
    (c) `UnitCollectionDataEntity`.
    (d) `UnitDataEntity`.
    (e) `LessonDataEntity`.
    (f) `SourceDocumentEntity`.
    (g) `AttachmentEntity`.
    (h) `ConfigurationEntity`, for the purpose of storing default configuration values, as defined in Configuration Management Specification.
    (i) `ReMarkableDeviceEntity`.

(2) Unless otherwise specified, any create, update and delete operations on any other data entity not specified in (1) must be short-term persisted.

(3) In this document —

    (a) "Long-term persistence" means that data must be retrievable after the application restarts.
    (b) "Short-term persistence" means that unless otherwise specified, data must be retrievable during the corresponding application run, but must not be persisted beyond the application run.

(4) The provisions in this document shall define required outcome behaviour. They do not mandate any particular implementation mechanism. An implementation that achieves these outcomes through any means (including but not limited to database foreign key constraints, service-layer logic, or any combination thereof) shall be considered compliant.

## Section 1A — Database and File Storage Paths

(1) All long-term persisted data under §1(1) shall be stored in a SQLite database file at the following path —

    ```
    %APPDATA%\ManuscriptaTeacherApp\manuscripta.db
    ```

(2) The backend shall resolve this path to an absolute location on startup, creating the directory if it does not already exist. This ensures that the database is written to a deterministic location regardless of the process's working directory.

(3) The default path in (1) may be overridden by setting a custom connection string in `appsettings.json` under `ConnectionStrings.MainDbContext`. When a non-default value is provided, the backend shall use the configured connection string verbatim.

(4) Attachment files shall be stored under `%APPDATA%\ManuscriptaTeacherApp\Attachments\`, as specified in `FrontendWorkflowSpecifications.md`.


## Section 2 - Requirements for Orphan Removal

(1) The deletion of a material M must delete any questions associated with M.

(2) The deletion of a question Q must delete any responses associated with Q.

(2A) The deletion of a response R must delete any feedback associated with R.

(3) The deletion of a unit collection C must delete any units associated with C.

(3A) The deletion of a unit collection C must delete any source documents associated with C.

(4) The deletion of a unit U must delete any lessons associated with U.

(5) The deletion of a lesson L must delete any materials associated with L.

(6) The deletion of a material M must delete any attachments associated with M.

(7) The deletion of an attachment A must delete the attachment file named by A's `FileName` attribute.

(8) The deletion of a `ReMarkableDeviceEntity` R must delete the rmapi configuration file at `%AppData%\ManuscriptaTeacherApp\rmapi\{R.DeviceId}.conf`.

## Section 3 - Proactive Orphan Removal

(1) **Removal of orphaned attachment files**

    The backend shall, on startup -

    (a) Retrieve all attachment entities from the database.
    (b) For each file placed under the `Attachments` directory, check if it is associated with an attachment entity.
    (c) If a file is not associated with an attachment entity, delete the file.

(2) **Removal of orphaned rmapi configuration files**

    The backend shall, on startup —

    (a) Retrieve all `ReMarkableDeviceEntity` entities from the database.
    (b) For each `.conf` file placed under the `%AppData%\ManuscriptaTeacherApp\rmapi` directory, check if its base name (without extension) matches a `DeviceId` of a `ReMarkableDeviceEntity`.
    (c) If a file is not associated with a `ReMarkableDeviceEntity`, delete the file.
    (d) If a `RemarkableDeviceEntity` is not associated with a `.conf` file, delete the entity.