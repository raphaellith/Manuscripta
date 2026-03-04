# Integration Test Contract

This document defines the shared assumptions that must hold for network integration tests to pass across the client and server. Every section represents a bilateral agreement: if either side deviates from these rules, cross-platform integration tests will fail.

The **client** (currently the Android tablet application) runs the integration test suite and performs all assertions. The **server** (currently the Windows teacher application) is the system under test — it must be running and reachable before the client tests begin. This asymmetry is intentional: the server acts as a live counterpart, and the client drives all test scenarios.

**Version:** 1.1
**Status:** Draft
**Depends on:** API Contract v1.1, Validation Rules, Pairing Process Specification, Session Interaction Specification

---

## Section 1 — Scope and Purpose

(1) This contract covers **network-level integration tests only** — tests that exercise the real HTTP, TCP, and UDP stacks of the client against a running instance of the server.

(2) These tests complement, but do not replace, each platform's unit tests. Unit tests mock the opposite side; integration tests require a live counterpart.

(3) Both teams must maintain their implementations against this contract. If a change to either application would violate a clause here, the contract must be amended first by agreement.

(4) All test assertions are performed **client-side**. The server's role is to respond correctly to client requests and to initiate protocol flows (e.g., UDP broadcasts, TCP commands) as specified. The server does not run its own integration test assertions against the client.

---

## Section 2 — Environment and Configuration

### 2.1. Network Ports

Both applications must agree on the following default port assignments, consistent with `API Contract.md` §1.1:

| Protocol | Default Port |
|----------|-------------|
| HTTP (REST) | 5911 |
| TCP (Binary) | 5912 |
| UDP (Discovery) | 5913 |

(1) Both sides must support port overrides via their platform's configuration mechanism (e.g., environment variables, configuration files). Default ports apply when no override is set.

(2) Both sides must be configured to use the **same ports** in the test environment.

(3) The HTTP base URL assumed by client tests is `http://{host}:{httpPort}/`, where `{host}` is the server machine's IP address on the shared network.

### 2.2. Server Host

(1) Integration tests are run **across two physical machines on the same network** — the client (Android tablet or development machine running the test suite) and the server (Windows machine running the teacher application).

(2) The client must be configured with the server's reachable IP address or hostname. The server binds to all interfaces by default.

(3) Both machines must be on the same subnet for UDP broadcast discovery to work (broadcast packets do not cross subnet boundaries).

### 2.3. HTTP Route Prefix

(1) All HTTP endpoints are served under the `/api/v1/` prefix, per `API Contract.md` §2.

(2) The server enforces that controller endpoints respond **only on the HTTP port** (5911 by default). Requests to controller routes on other bound ports will receive `404`.

(3) The health endpoint `GET /` is available on **any bound port**.

---

## Section 3 — Test Device Identity

### 3.1. Well-Known Test Device

Both sides must accept the following fixed test device for integration testing:

| Field | Value |
|-------|-------|
| Device ID | `00000001-0000-0000-0000-000000000001` |
| Device Name | `Integration Test Tablet` |

(1) This device ID is a valid UUID so that it passes the server's `Guid.TryParse()` validation on all endpoints.

(2) The server must accept this ID in all endpoints that take a device ID, treating it as a valid paired device after registration via `POST /pair`.

(3) The client tests use this ID consistently across all test classes. No other device IDs are used unless explicitly documented.

### 3.2. Device Registration

(1) Before any HTTP endpoint (other than `POST /pair` itself) can be exercised, the test device must be registered:

```
POST /api/v1/pair
Body: { "DeviceId": "00000001-0000-0000-0000-000000000001", "Name": "Integration Test Tablet" }
Expected: 201 Created
```

(2) The server must support re-registration of the test device between test runs. This means either:

  (a) Returning `201 Created` on each registration (idempotent), or
  (b) Returning `409 Conflict` for duplicates, which the client tests accept as a valid "already paired" state.

(3) Both sides must support cleanup between test executions. The server should provide a mechanism (database reset, test fixture teardown, or ephemeral in-memory database) to ensure a clean state.

---

## Section 4 — HTTP Endpoint Contract

This section defines the exact request/response shapes and status codes that both sides must agree on. All JSON field names use **PascalCase**, per `Validation Rules.md` §1(6).

### 4.1. POST /pair — Device Registration

**Request:**
```json
{
  "DeviceId": "<string>",
  "Name": "<string>"
}
```

| Scenario | Expected Status | Response Body |
|----------|----------------|---------------|
| First registration | `201 Created` | Empty |
| Duplicate device ID | `409 Conflict` | — |
| Missing or empty fields | `400 Bad Request` | — |

**Client assertion:** Tests assert `201` for first registration, and `4xx` (any 400-level) for duplicates and invalid payloads.

### 4.2. GET /distribution/{deviceId} — Material Distribution Bundle

**Response (200):**
```json
{
  "materials": [
    {
      "Id": "<UUID>",
      "MaterialType": "READING | WORKSHEET | POLL",
      "Title": "<string, max 500>",
      "Content": "<string, encoded per Material Encoding Spec>",
      "Timestamp": "<long, unix seconds>",
      "Metadata": "<string | null>",
      "VocabularyTerms": ["<string>"] | null
    }
  ],
  "questions": [
    {
      "Id": "<UUID>",
      "MaterialId": "<UUID>",
      "QuestionType": "MULTIPLE_CHOICE | WRITTEN_ANSWER",
      "QuestionText": "<string>",
      "Options": ["<string>"] | null,
      "CorrectAnswer": "<int | string | null>",
      "MaxScore": "<int | null>"
    }
  ]
}
```

| Scenario | Expected Status |
|----------|----------------|
| Materials staged for device | `200 OK` |
| No materials staged / unknown device | `404 Not Found` |

**Client assertion:** Tests verify `200` with a non-null body containing `materials` and `questions` arrays (which may be empty). The test device must be registered before calling this endpoint.

### 4.3. GET /feedback/{deviceId} — Feedback Retrieval

**Response (200):**
```json
{
  "feedback": [
    {
      "Id": "<UUID>",
      "ResponseId": "<UUID>",
      "Text": "<string | null>",
      "Marks": "<int | null>",
      "Status": "PROVISIONAL | READY | DELIVERED"
    }
  ]
}
```

| Scenario | Expected Status |
|----------|----------------|
| READY feedback available | `200 OK` |
| No feedback available / unknown device | `404 Not Found` |

**Client assertion:** Tests accept either `200` or `404` as valid outcomes. When `200`, the `feedback` array is verified to be non-null.

### 4.4. GET /config/{deviceId} — Configuration

**Response (200):**
```json
{
  "TextSize": "<int, 5-50>",
  "FeedbackStyle": "IMMEDIATE | NEUTRAL",
  "TtsEnabled": "<boolean>",
  "AiScaffoldingEnabled": "<boolean>",
  "SummarisationEnabled": "<boolean>",
  "MascotSelection": "NONE | MASCOT1 | MASCOT2 | MASCOT3 | MASCOT4 | MASCOT5"
}
```

| Scenario | Expected Status |
|----------|----------------|
| Valid paired device | `200 OK` |
| Unknown device | `404 Not Found` |
| Non-client device | `403 Forbidden` |

**Client assertion:** Tests verify `200` with a body containing at least `TextSize` and `FeedbackStyle` fields. If the server has no configuration for the test device, it must still return default values.

**Server note:** This endpoint also marks the device as having received configuration (implicit REFRESH_CONFIG acknowledgement, per `Session Interaction.md` §6(3)).

### 4.5. GET /attachments/{id} — Attachment Download

| Scenario | Expected Status | Response |
|----------|----------------|----------|
| Attachment exists | `200 OK` | Raw binary + `Content-Type` header |
| Unknown ID | `404 Not Found` | — |

**Client assertion:** Tests use a well-known test attachment ID of `00000001-0000-0000-0000-000000000004`. If this attachment is not pre-staged, the test gracefully accepts `404` and skips further assertions.

**Server obligation:** To exercise the full attachment contract, the server must pre-stage a file with ID `00000001-0000-0000-0000-000000000004` in its attachment storage directory (see §12.2).

### 4.6. POST /responses — Single Response Submission

**Request:**
```json
{
  "Id": "<UUID, client-generated>",
  "QuestionId": "<UUID>",
  "DeviceId": "<UUID>",
  "Answer": "<string>",
  "Timestamp": "<ISO 8601 string>",
  "IsCorrect": true | false | null
}
```

| Scenario | Expected Status |
|----------|----------------|
| Valid payload | `201 Created` |
| Missing required fields | `400 Bad Request` |

**Client assertion:** Tests use well-known IDs `00000001-0000-0000-0000-000000000002` (question) and `00000001-0000-0000-0000-000000000003` (material). These must be valid UUIDs for server `Guid.TryParse()` validation. The server should have these entities pre-staged (see §12.2) so that response mapping succeeds.

### 4.7. POST /responses/batch — Batch Response Submission

**Request:**
```json
{
  "Responses": [
    { /* ResponseEntity per §4.6 */ }
  ]
}
```

| Scenario | Expected Status |
|----------|----------------|
| All valid | `201 Created` |
| Any malformed | `400 Bad Request` |

**Client assertion:** Batch semantics are all-or-nothing. If any response in the batch is invalid, the entire batch is rejected.

---

## Section 5 — TCP Binary Protocol Contract

### 5.1. Message Format

All TCP messages follow the binary protocol defined in `API Contract.md` §3.1:

```
[ Opcode (1 byte) ][ Operand (0..N bytes) ]
```

### 5.2. Opcode Registry

Both sides must implement and correctly handle the following opcodes:

| Opcode | Name | Direction | Operand |
|--------|------|-----------|---------|
| `0x01` | LOCK_SCREEN | Server → Client | None |
| `0x02` | UNLOCK_SCREEN | Server → Client | None |
| `0x03` | REFRESH_CONFIG | Server → Client | None |
| `0x04` | UNPAIR | Server → Client | None |
| `0x05` | DISTRIBUTE_MATERIAL | Server → Client | None |
| `0x06` | HAND_ACK | Server → Client | Device ID (UTF-8) |
| `0x07` | RETURN_FEEDBACK | Server → Client | None |
| `0x10` | STATUS_UPDATE | Client → Server | JSON (UTF-8), see §5.4 |
| `0x11` | HAND_RAISED | Client → Server | Device ID (UTF-8) |
| `0x12` | DISTRIBUTE_ACK | Client → Server | Device ID (UTF-8, null-terminated) + Material ID (UTF-8) |
| `0x13` | FEEDBACK_ACK | Client → Server | Device ID (UTF-8, null-terminated) + Feedback ID (UTF-8) |
| `0x20` | PAIRING_REQUEST | Client → Server | Device ID (UTF-8) |
| `0x21` | PAIRING_ACK | Server → Client | None |

### 5.3. Pairing Handshake

The integration tests verify the following sequence:

```
1. Client opens TCP connection to {host}:{tcpPort}
2. Client sends: [0x20][Device ID as UTF-8 bytes]
3. Server responds: [0x21]
4. Connection remains open for subsequent messages
```

**Timing:**
- Client tests use a **3-second pairing timeout** and **1 retry** (faster than production defaults).
- The server must send `PAIRING_ACK` promptly — within the 3-second window.

**State transitions (client-side):**
- `NOT_PAIRED` → `PAIRING_IN_PROGRESS` (on initiating pairing)
- `PAIRING_IN_PROGRESS` → `PAIRED` (on receipt of `PAIRING_ACK`)

### 5.4. STATUS_UPDATE (Heartbeat) Message

The operand of opcode `0x10` is a UTF-8-encoded JSON payload (opcode byte excluded):

```json
{
  "DeviceId": "<UUID string>",
  "Status": "ON_TASK | IDLE | LOCKED | DISCONNECTED",
  "BatteryLevel": "<int, 0-100>",
  "CurrentMaterialId": "<UUID string>",
  "StudentView": "<string>",
  "Timestamp": "<long, unix milliseconds>"
}
```

**Agreements:**
- JSON field names use **PascalCase**, per `Validation Rules.md` §1(6).
- The server must parse with **case-insensitive property matching** and string-to-enum conversion.
- The client sends heartbeats at the configured interval (production: 3 seconds per `Session Interaction.md` §1(1)(c); tests: 1 second).
- The server updates its heartbeat tracking on each STATUS_UPDATE and fires a disconnect event after **10 seconds** of silence, per `Session Interaction.md` §2(3).

### 5.5. HAND_RAISED / HAND_ACK

```
Client sends:  [0x11][Device ID as UTF-8]
Server sends:  [0x06][Device ID as UTF-8]
```

**Client assertion:** After sending HAND_RAISED, the test expects HAND_ACK within **5 seconds** and verifies the response opcode is `0x06`.

### 5.6. DISTRIBUTE_ACK Format

Per `API Contract.md` §3.6.2:

```
Byte 0:       0x12
Bytes 1..N:   Device ID (UTF-8, null-terminated with 0x00)
Bytes N+1..M: Material ID (UTF-8)
```

**Critical detail:** The null byte (`0x00`) separates the two string fields. Both sides must agree on this encoding — the server must parse using the null-byte delimiter.

### 5.7. FEEDBACK_ACK Format

Identical structure to DISTRIBUTE_ACK, with opcode `0x13` and Feedback ID instead of Material ID.

```
Byte 0:       0x13
Bytes 1..N:   Device ID (UTF-8, null-terminated with 0x00)
Bytes N+1..M: Feedback ID (UTF-8)
```

### 5.8. Server-Initiated Commands

The client tests observe (but do not require) the following commands during a connected session:

| Command | Opcode | Operand | Expected ACK Pattern |
|---------|--------|---------|---------------------|
| LOCK_SCREEN | `0x01` | None | Next STATUS_UPDATE reports `LOCKED` (within 6s) |
| UNLOCK_SCREEN | `0x02` | None | Next STATUS_UPDATE reports `ON_TASK` or `IDLE` (within 6s) |
| REFRESH_CONFIG | `0x03` | None | Client calls `GET /config/{deviceId}` (within 5s) |
| UNPAIR | `0x04` | None | TCP connection terminates |

**Client test behaviour:** The command tests are **observational** — they pass whether or not any command is received. To exercise the full command flow, the server must send these commands during the test window (e.g., via the Simulation API, per §12.4).

---

## Section 6 — UDP Discovery Protocol Contract

### 6.1. Packet Format

Per `API Contract.md` §3.3, the UDP discovery packet is exactly **9 bytes**:

| Offset | Size | Field | Encoding |
|--------|------|-------|----------|
| 0 | 1 byte | Opcode | `0x00` (DISCOVERY) |
| 1 | 4 bytes | IPv4 Address | Big-endian (network byte order) |
| 5 | 2 bytes | HTTP Port | Little-endian (unsigned) |
| 7 | 2 bytes | TCP Port | Little-endian (unsigned) |

### 6.2. Broadcast Behaviour

(1) The server broadcasts on port **5913** (default) to the broadcast address **255.255.255.255**.

(2) Broadcast interval is **3 seconds** (configurable via the server's network settings).

(3) The client tests assert the interval between consecutive packets is between **1.5 and 6 seconds** (generous tolerance for system jitter).

### 6.3. Client Test Mechanics

(1) The client test opens a raw UDP socket bound to the configured UDP port.

(2) Socket receive timeout is **20 seconds** — the test fails if no packet arrives within this window.

(3) After parsing the 9-byte payload, the test verifies:
  - Opcode is `0x00`
  - HTTP and TCP port values match the test configuration
  - IP address array is non-null (4 bytes)

---

## Section 7 — End-to-End Flow Assumptions

### 7.1. Full Pairing Flow

The E2E pairing test exercises the complete discovery-to-registration pipeline:

```
1. Receive UDP discovery packet              → Parse server IP + ports
2. Open TCP connection, send PAIRING_REQUEST → Receive PAIRING_ACK (0x21)
3. POST /pair with device ID and name        → 201 Created
4. Assert pairing state == PAIRED
```

**Timeouts:**
- UDP receive: 20 seconds
- TCP pairing: 5 seconds

### 7.2. Material Distribution Flow

The E2E distribution test exercises the TCP-triggered HTTP fetch pattern:

```
1. Pair device (TCP + HTTP)
2. Start heartbeat (STATUS_UPDATE every 1s)
3. Wait for DISTRIBUTE_MATERIAL (0x05)        → 15-second timeout
4. If received:
   a. GET /distribution/{deviceId}            → 200 OK
   b. For each material: send DISTRIBUTE_ACK  → [0x12][deviceId\0materialId]
5. Drain messages briefly, assert no UNPAIR received
```

**Critical:** The distribution test is **conditional** — if no DISTRIBUTE_MATERIAL signal arrives within 15 seconds, the test passes vacuously. To fully exercise this flow, materials must be staged on the server and the DISTRIBUTE_MATERIAL signal sent during the test window (see §12.2 and §12.4).

### 7.3. Feedback Return Flow

The feedback flow follows the same pattern as material distribution:

```
1. Pair device
2. Start heartbeat
3. Wait for RETURN_FEEDBACK (0x07)
4. If received:
   a. GET /feedback/{deviceId}                → 200 OK
   b. For each feedback: send FEEDBACK_ACK    → [0x13][deviceId\0feedbackId]
```

---

## Section 8 — Test Data and Fixture Requirements

### 8.1. Pre-Staged Data (Server Obligations)

For integration tests to exercise their full paths, the server must pre-stage the following data when running in integration-test mode (see §12). If this data is absent, the corresponding tests pass with reduced coverage (graceful fallback).

| Data | Identifier | Purpose |
|------|-----------|---------|
| Attachment file | `00000001-0000-0000-0000-000000000004` | Attachment download test |
| Question entity | `00000001-0000-0000-0000-000000000002` | Response submission test |
| Material entity | `00000001-0000-0000-0000-000000000003` | Response submission test (material reference) |
| Material bundle | (any, for test device) | Distribution bundle test |
| Feedback entities | (any, for test device) | Feedback retrieval test |
| Configuration | (defaults for test device) | Config retrieval test |

### 8.2. Staging Approaches

The server team may satisfy these requirements through any of:

(a) **Database seeding** — Insert test entities via database migrations or startup seeding when running in integration-test mode.

(b) **Simulation API** — The server exposes endpoints under the `/api/simulation` prefix. These should be extended to support material, feedback, and attachment staging (see §12.4).

(c) **Test fixture files** — For attachments, place a file named `00000001-0000-0000-0000-000000000004.{ext}` in the server's attachment storage directory.

(d) **Dedicated integration test environment** — Use a platform-appropriate mechanism (e.g., environment variable, launch profile) with configuration that seeds the database on startup.

### 8.3. State Isolation Between Tests

(1) Each test registers the test device during setup. The server must handle repeated registrations gracefully (§3.2).

(2) TCP tests pair at the start and close the connection during teardown. The server must clean up connection state when the TCP socket closes.

(3) The server's heartbeat monitor (10-second timeout) should not interfere with test execution, as client tests send heartbeats at 1-second intervals during active TCP sessions.

---

## Section 9 — Timing Constraints

### 9.1. Timeouts Summary

| Operation | Client Test Timeout | Server Timeout | Source |
|-----------|---------------------|----------------|--------|
| TCP pairing handshake | 3 seconds | — | Client test harness |
| Heartbeat interval | 1 second (test) / 3 seconds (production) | — | `Session Interaction.md` §1(1)(c) |
| Heartbeat disconnect | — | 10 seconds | `Session Interaction.md` §2(3) |
| DISTRIBUTE_MATERIAL signal wait | 15 seconds | — | Material distribution flow test |
| DISTRIBUTE_ACK wait | — | 30 seconds | `Session Interaction.md` §3(6) |
| FEEDBACK_ACK wait | — | 30 seconds | `Session Interaction.md` §7(5) |
| LOCK/UNLOCK confirmation | — | 6 seconds | `Session Interaction.md` §6(2)(c) |
| REFRESH_CONFIG confirmation | — | 5 seconds | `Session Interaction.md` §6(3)(b) |
| HAND_RAISED retry | 3 seconds | — | `Session Interaction.md` §4A(3) |
| TCP message poll | 5 seconds | — | Various TCP tests |
| UDP socket receive | 20 seconds | — | Discovery tests |
| UDP broadcast interval | — | 3 seconds | `API Contract.md` §1.1 |

### 9.2. Tolerance

(1) Client tests use generous tolerances for timing-dependent assertions (e.g., UDP broadcast cadence accepted between 1.5 and 6 seconds).

(2) Tests that depend on server-initiated actions (commands, distribution signals) use conditional logic — they pass whether or not the action occurs within the timeout window.

---

## Section 10 — Serialisation Conventions

### 10.1. JSON Field Naming

Per `Validation Rules.md` §1(6), all JSON field names in DTOs use **PascalCase**:

```
✓ "DeviceId", "MaterialType", "QuestionText", "BatteryLevel"
✗ "deviceId", "materialType", "questionText", "batteryLevel"
```

Both sides must serialise using PascalCase field names on the wire. The server must parse with **case-insensitive property matching** to tolerate minor discrepancies.

### 10.2. Timestamp Formats

| Context | Format | Unit |
|---------|--------|------|
| Material `Timestamp` field | Long integer | Unix seconds |
| STATUS_UPDATE `Timestamp` field | Long integer | Unix milliseconds |
| Response `Timestamp` field | ISO 8601 string | e.g., `"2025-01-15T10:30:00Z"` |

Both sides must agree on these formats. The inconsistency between Unix seconds and milliseconds across different entities is intentional and documented in `Validation Rules.md`.

### 10.3. Enum Serialisation

Enums are serialised as **SCREAMING_SNAKE_CASE strings**:

| Enum | Values |
|------|--------|
| `MaterialType` | `READING`, `WORKSHEET`, `POLL` |
| `QuestionType` | `MULTIPLE_CHOICE`, `WRITTEN_ANSWER` |
| `DeviceStatus` | `ON_TASK`, `IDLE`, `LOCKED`, `DISCONNECTED` |
| `FeedbackStatus` | `PROVISIONAL`, `READY`, `DELIVERED` |
| `SessionStatus` | `RECEIVED`, `ACTIVE`, `PAUSED`, `COMPLETED`, `CANCELLED` |
| `FeedbackStyle` | `IMMEDIATE`, `NEUTRAL` |
| `MascotSelection` | `NONE`, `MASCOT1`–`MASCOT5` |

### 10.4. ID Format

(1) All entity IDs are **UUID strings** (36 characters, hyphenated: `xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx`).

(2) Device IDs are generated by the client. Material, question, and feedback IDs are generated by the server. Per `Validation Rules.md` §3.

(3) All test entity IDs use the `00000001-0000-0000-0000-00000000000x` pattern to be easily identifiable as test data while remaining valid UUIDs.

### 10.5. String Encoding

(1) All TCP message operands containing strings use **UTF-8** encoding.

(2) DISTRIBUTE_ACK and FEEDBACK_ACK operands use a **null byte (`0x00`)** as the separator between Device ID and Entity ID.

---

## Section 11 — Build and Execution

### 11.1. Running Client Integration Tests (Android)

```bash
cd android
export JAVA_HOME=$(/usr/libexec/java_home -v 17)

# Integration tests only (excluded from normal test runs)
./gradlew test -Pintegration

# Normal unit tests (integration tests excluded via @Category filter)
./gradlew testDebugUnitTest
```

Integration tests are annotated with `@Category(IntegrationTest.class)` and excluded from the default test task via Gradle configuration.

### 11.2. Running Server for Integration Tests (Windows)

The server must be started in **integration-test mode** (see §12.1) so that all network services are active before client tests begin:

```bash
cd windows/ManuscriptaTeacherApp/Main
ASPNETCORE_ENVIRONMENT=Integration dotnet run --no-launch-profile
```

See §12 for the full list of server-side requirements in integration-test mode.

### 11.3. Manual Two-Machine Execution

Integration tests are run manually across two machines on the same network:

(1) **Start the server first.** On the Windows machine, launch the server in integration-test mode (§12.1). Wait for the readiness signal (§12.6) or confirm UDP broadcasts are being sent.

(2) **Configure the client.** On the Android device or development machine, set the server host to the Windows machine's IP address (via the platform's configuration mechanism) and verify the port settings match.

(3) **Run the client test suite.** Execute the integration tests from the client machine. All assertions happen client-side.

(4) The test device must be fresh (no prior pairing state) or the server must handle re-pairing gracefully.

(5) Port conflicts must be avoided. The default ports (5910–5913) should be reserved for integration test use. Ensure no firewall rules block traffic on these ports between the two machines.

---

## Section 12 — Server-Side Requirements for Integration Testing

This section defines the concrete requirements that the server must satisfy to support the integration test suite. These are framed as actionable obligations for the server team.

### 12.1. Integration Test Startup Mode

The server **must** support a startup mode that **automatically starts** all network services required for integration testing. In production, UDP broadcasting and TCP listening may be triggered on demand (e.g., via UI interaction). In integration-test mode, they must start immediately on launch.

**Requirements:**

(1) On startup in integration-test mode, the server must automatically begin:
  - **UDP discovery broadcasting** on the configured port (default 5913), at the configured interval (default 3 seconds).
  - **TCP pairing listener** on the configured port (default 5912), accepting incoming connections.
  - **HTTP REST API** on the configured port (default 5911), serving all endpoints defined in §4.

(2) All three services must be fully operational **before** any client test begins. The server should block or signal readiness (see §12.6) until all services are listening.

(3) The mechanism for activating integration-test mode is the server team's choice. Suggested approaches:
  - A dedicated environment or launch profile (e.g., `Integration`)
  - A command-line flag (e.g., `--integration-test`)
  - An environment variable (e.g., `MANUSCRIPTA_INTEGRATION=true`)

(4) The server must use the same port defaults and protocol implementations as production. Integration-test mode must not alter wire-level behaviour — only service lifecycle (auto-start) and data seeding (§12.2) should differ.

### 12.2. Test Data Seeding

In integration-test mode, the server **must** seed the following entities on startup, so that client tests can exercise their full paths without manual data preparation:

| Entity | Identifier | Requirements |
|--------|-----------|--------------|
| Test device configuration | (for `...000000000001`) | Default configuration values: `TextSize` within 5–50, `FeedbackStyle` of `IMMEDIATE` or `NEUTRAL`, and reasonable defaults for all other fields per §4.4 |
| Attachment file | `00000001-0000-0000-0000-000000000004` | Any binary file (e.g., a small PNG or PDF) accessible via `GET /attachments/00000001-0000-0000-0000-000000000004` |
| Question entity | `00000001-0000-0000-0000-000000000002` | Linked to `00000001-0000-0000-0000-000000000003`, type `WRITTEN_ANSWER` or `MULTIPLE_CHOICE` |
| Material entity | `00000001-0000-0000-0000-000000000003` | Assigned to the well-known test device (§3.1), type `READING` or `WORKSHEET` |
| Material bundle | (for test device) | At least one material assigned to the test device so `GET /distribution/{deviceId}` returns `200` |
| Feedback entities | (for test device) | At least one feedback entity with status `READY` linked to a response from the test device, so `GET /feedback/{deviceId}` returns `200` |

(1) Seeding should use the server's standard data access layer (e.g., database migrations, startup initialisers, or repository calls).

(2) The seeded data must be consistent — foreign key relationships between materials, questions, and feedback must be valid.

(3) If the server cannot seed all entities (e.g., feedback requires a prior response submission), the client tests will fall back gracefully by accepting `404` responses. However, **full seeding is strongly recommended** to maximise test coverage.

### 12.3. Database Isolation

(1) The server **must** use an **ephemeral database** in integration-test mode (e.g., in-memory SQLite or a per-run temporary file).

(2) Each test run must start with a clean state — no leftover devices, materials, sessions, or pairing records from previous runs.

(3) The seeded test data (§12.2) is the only data present at the start of each run.

### 12.4. Simulation API Extensions (Recommended)

The server currently exposes a Simulation API under `/api/simulation/` with endpoints for adding simulated devices and updating status. To enable deterministic end-to-end testing, the following extensions are **recommended**:

| Endpoint | Method | Purpose |
|----------|--------|---------|
| `/api/simulation/stage-material` | POST | Stage a material bundle for a given device ID, triggering the `DISTRIBUTE_MATERIAL` TCP signal |
| `/api/simulation/stage-feedback` | POST | Stage feedback entities for a given device and response ID, triggering the `RETURN_FEEDBACK` TCP signal |
| `/api/simulation/send-command` | POST | Trigger a specific TCP command (`LOCK_SCREEN`, `UNLOCK_SCREEN`, `REFRESH_CONFIG`, `UNPAIR`) for a given device ID |
| `/api/simulation/stage-attachment` | POST | Upload a test attachment with a specified ID |

(1) These endpoints enable the client tests to **deterministically trigger** server-initiated flows during the test window, rather than relying on pre-staging and hoping the server sends signals at the right time.

(2) Without these extensions, E2E tests for distribution, feedback, and server commands remain **observational** (passing vacuously if no signal arrives). With them, the tests become **deterministic**.

(3) The Simulation API must only be available in non-production environments (integration-test mode and development).

### 12.5. Graceful Re-Registration and Connection Cleanup

(1) The server **must** handle repeated `POST /pair` calls for the same device ID without crashing or entering an inconsistent state (per §3.2).

(2) When a TCP connection closes (either cleanly or due to network failure), the server **must** clean up the connection state for that device — removing it from the active connections registry and stopping any heartbeat monitoring for that device.

(3) The server's heartbeat monitor (10-second disconnect timeout) must not interfere with test execution. Client tests send heartbeats at 1-second intervals during active sessions.

### 12.6. Server Readiness Signal

(1) The server **should** expose a health check endpoint that returns `200 OK` when **all** services (HTTP, TCP listener, UDP broadcaster) are operational.

(2) The existing `GET /` endpoint is sufficient if it is only reachable once all services have started. If `GET /` becomes available before TCP/UDP services are ready, a dedicated readiness endpoint (e.g., `GET /health`) is recommended.

(3) Client tests should poll the readiness endpoint before starting test execution, with a configurable timeout (recommended: 30 seconds).

---

## Appendix A — Known Gaps and Future Work

(1) **Simulation API implementation** — The Simulation API extensions described in §12.4 are recommended but not yet implemented. Until they are available, E2E tests for distribution, feedback, and server commands remain observational.

(2) **Bidirectional test coverage** — Currently only the client side runs integration tests against the server. A server-side test harness that exercises the server against a simulated client would provide additional coverage, but is not required by this contract.

(3) **Multi-device test scenarios** — The current contract defines a single well-known test device. Future iterations may add additional test devices to verify multi-device scenarios (e.g., broadcast to multiple clients, device-specific distribution).

---

## Appendix B — Document References

| Document | Relevance |
|----------|-----------|
| `API Contract.md` v1.1 | Authoritative for HTTP endpoints, binary protocol, and opcode registry |
| `Validation Rules.md` | Authoritative for DTO field definitions, constraints, and PascalCase naming |
| `Pairing Process.md` | Authoritative for pairing handshake sequence (overrides API Contract on conflicts) |
| `Session Interaction.md` | Authoritative for heartbeat, distribution, feedback, and control command flows |
| `Material Encoding.md` | Authoritative for material `Content` field encoding |
