# Integration Test Specification (Windows)

## Explanatory Note

This specification defines how the application should be set up for an integration test environment, with the Android student application.

## Section 1: General Principles

(1) This specification shall implement Section 12 (Server-Side Requirements for Integration Testing) of the Integration Test Contract, to define Windows-specific requirements for integration testing.

(2) In this specification —

    (a) "integration-test mode" means the startup mode of the application which automatically starts all network services, seeds test data, and uses an ephemeral database, as specified in Section 12 of the Integration Test Contract;

    (b) the "test device" means the well-known device defined in Section 3.1 of the Integration Test Contract, having device ID `00000000-0000-0000-0000-000000000001` and device name `Integration Test Tablet`;

    (c) the "Simulation API" means the API served under the `/api/simulation/` route prefix, as defined in Section 5 of this specification.

(3) This specification shall be read together with the Integration Test Contract, which defines the bilateral agreements between the client and the server.

(4) Files required to implement this specification shall be placed in the `Testing` directory of the `Main` component, in accordance with Section 2A(1)(f) of the Windows App Structure Specification.


## Section 2 — Integration Test Startup Mode

(1) The application shall support an integration-test startup mode, activated by the ASP.NET Core environment name `Integration`, such that the application is started with:

    ```
    dotnet run --environment Integration
    ```

(2) When started in integration-test mode, the application shall automatically begin the following network services without requiring user interaction —

    (a) **UDP discovery broadcasting** on the configured port (default 5913), at the configured interval (default 3 seconds), per API Contract §3.3.

    (b) **TCP pairing listener** on the configured port (default 5912), accepting incoming connections, per API Contract §3.1.

    (c) **HTTP REST API** on the configured port (default 5911), serving all endpoints defined in the API Contract §2.

(3) All three services specified in subsection (2) shall be fully operational before any client test begins.

(4) The application shall expose a dedicated configuration file, `appsettings.Integration.json`, in the `Main` project directory. This file shall contain all environment-specific settings for integration-test mode, including —

    (a) the connection string for the ephemeral database specified in Section 3;

    (b) any port overrides required for the test environment; and

    (c) logging configuration appropriate for test diagnostics.

(5) Integration-test mode shall not alter wire-level behaviour of any protocol. Only service lifecycle (auto-start), data seeding (Section 4), and database isolation (Section 3) shall differ from production.

(6) In integration-test mode, the application shall log a message to the console indicating that all services are ready, to assist manual verification.


## Section 3 — Database Isolation

(1) In integration-test mode, the application shall use an **ephemeral in-memory SQLite database** to ensure complete isolation from production data.

(2) The connection string for the ephemeral database shall be configured in `appsettings.Integration.json` using the SQLite in-memory connection string format.

(3) Each run of the application in integration-test mode shall start with a clean database, containing only the seeded test data specified in Section 4.

(4) The database schema shall not be applied using migrations. A non-migration mechanism, such as `EnsureCreated()`, shall be used instead.

    [Explanatory Note: Migrations create a migration history table, which introduces unnecessary dependencies in ephemeral databases that are discarded after each run.]


## Section 4 — Test Data Seeding

(1) On startup in integration-test mode, the application shall seed the following entities into the ephemeral database, in accordance with Section 12.2 of the Integration Test Contract.

(2) A **test data seeder** service shall be provided, which shall seed the following data —

    (a) **Test device configuration**: default configuration values for the test device, conformant to ConfigurationManagementSpecification Appendix 1. This ensures that `GET /config/{deviceId}` returns `200` with valid defaults for the test device.

    (b) **Material entity** with ID `10000000-0000-0000-0000-000000000010`, of type `READING` or `WORKSHEET`, assigned to the test device, with valid content per the Material Encoding Specification.

    (c) **Question entity** with ID `10000000-0000-0000-0000-000000000020`, of type `WRITTEN_ANSWER` or `MULTIPLE_CHOICE`, linked to the material specified in paragraph (b).

    (d) **Material bundle**: the material specified in paragraph (b) shall be included in the distribution bundle for the test device, such that `GET /distribution/{deviceId}` returns `200`.

    (e) **Feedback entity**: at least one feedback entity with status `READY`, linked to a response from the test device, such that `GET /feedback/{deviceId}` returns `200`. Where a response entity is required as a prerequisite, that response shall also be seeded.

    (f) **Attachment file**: a test binary file accessible via `GET /attachments/10000000-0000-0000-0000-000000000050`. The seeder shall place this file in the `%AppData%\ManuscriptaTeacherApp\Attachments` directory with the ID `10000000-0000-0000-0000-000000000050`.

(3) All seeded entities shall maintain valid foreign key relationships, consistent with the entity hierarchy defined in Section 2 of the Persistence and Cascading Rules.

(4) The seeder shall be invoked during the application startup pipeline, after the database schema has been applied but before the network services begin accepting connections.

(5) The seeder shall be implemented as a class within the `Testing` directory, and shall not be referenced by production code, in accordance with Section 2A(3)(e) of the Windows App Structure Specification.


## Section 5 — Simulation API Extensions

(1) The application shall provide a Simulation API, served under the `/api/simulation/` route prefix, with the following endpoints to enable deterministic end-to-end testing, as recommended by Section 12.4 of the Integration Test Contract.

(2) The following endpoints shall be implemented —

    (a) `POST /api/simulation/stage-material`: accepts a device ID in the request body. Stages a material bundle for the given device and triggers the `DISTRIBUTE_MATERIAL` TCP signal (opcode `0x05`) to all connected TCP clients for that device.

    (b) `POST /api/simulation/stage-feedback`: accepts a device ID and a response ID in the request body. Stages feedback entities for the given device and response, and triggers the `RETURN_FEEDBACK` TCP signal (opcode `0x07`) to all connected TCP clients for that device.

    (c) `POST /api/simulation/send-command`: accepts a device ID and a command name (one of `LOCK_SCREEN`, `UNLOCK_SCREEN`, `REFRESH_CONFIG`, `UNPAIR`) in the request body. Triggers the specified TCP command to the connected TCP client for that device.

    (d) `POST /api/simulation/stage-attachment`: accepts a multipart/form-data request containing a file and an attachment ID. Stores the file in the attachment storage directory with the specified ID, such that it is accessible via `GET /attachments/{id}`.

(3) These endpoints shall enable the client tests to deterministically trigger server-initiated flows during the test window, as described in Section 12.4 of the Integration Test Contract.

(4) The Simulation API shall be placed in the `Testing` directory, subject to the restrictions in Section 2A(1)(f) and Section 2A(3)(e) of the Windows App Structure Specification.


    [Explanatory Note: These provisions require that code in the `Testing` directory is not included in production builds where possible, and that production code must never depend on it.]
