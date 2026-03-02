# Integration Test Contract

This document defines the shared assumptions that must hold for network integration tests to pass on both the Android client and Windows server. Every section represents a bilateral agreement: if either side deviates from these rules, cross-platform integration tests will fail.

**Version:** 1.0
**Status:** Draft
**Depends on:** API Contract v1.1, Validation Rules, Pairing Process Specification, Session Interaction Specification

---

## Section 1 — Scope and Purpose

(1) This contract covers **network-level integration tests only** — tests that exercise the real HTTP, TCP, and UDP stacks of one application against a running instance of the other.

(2) These tests complement, but do not replace, each team's unit tests. Unit tests mock the opposite side; integration tests require a live counterpart.

(3) Both teams must maintain their integration tests against this contract. If a change to either application would violate a clause here, the contract must be amended first by agreement.

---

## Section 2 — Environment and Configuration

### 2.1. Network Ports

Both applications must agree on the following default port assignments, consistent with `API Contract.md` §1.1:

| Protocol | Default Port | Environment Variable (Android) | Configuration Key (Windows) |
|----------|-------------|-------------------------------|---------------------------|
| HTTP (REST) | 5911 | `MANUSCRIPTA_HTTP_PORT` | `NetworkSettings:HttpPort` |
| TCP (Binary) | 5912 | `MANUSCRIPTA_TCP_PORT` | `NetworkSettings:TcpPort` |
| UDP (Discovery) | 5913 | `MANUSCRIPTA_UDP_PORT` | `NetworkSettings:UdpBroadcastPort` |

(1) The Android tests read port configuration from the environment variables listed above, falling back to the default values.

(2) The Windows server reads port configuration from `appsettings.json` under the `NetworkSettings` section. Both sides must be configured to use the same ports in the test environment.

(3) The HTTP base URL assumed by Android tests is `http://{host}:{httpPort}/`, where host defaults to `localhost`.

### 2.2. Server Host

| Side | Default | Override |
|------|---------|----------|
| Android tests | `localhost` | `MANUSCRIPTA_SERVER_HOST` env var |
| Windows server | Binds to all interfaces | `applicationUrl` in `launchSettings.json` |

(1) When running tests locally on the same machine, `localhost` is sufficient.

(2) When running across the network (e.g., CI with separate containers), both sides must be configured with the correct reachable hostname or IP address.

### 2.3. HTTP Route Prefix

(1) All HTTP endpoints are served under the `/api/v1/` prefix, per `API Contract.md` §2.

(2) The Windows server enforces that controller endpoints respond **only on the HTTP port** (5911 by default), per host-filtering middleware. Requests to controller routes on other bound ports (e.g., 5910) will receive 404.

(3) The health endpoint `GET /` is available on **any bound port**.

---

## Section 3 — Test Device Identity

### 3.1. Well-Known Test Device

Both sides must accept the following fixed test device for integration testing:

| Field | Value |
|-------|-------|
| Device ID | `inttest-00000000-0000-0000-0000-000000000001` |
| Device Name | `Integration Test Tablet` |

(1) This device ID deliberately uses a non-standard UUID prefix (`inttest-`) to distinguish test traffic from production traffic.

(2) The Windows server must accept this ID in all endpoints that take a device ID, treating it as a valid paired device after registration via `POST /pair`.

(3) The Android tests use this ID consistently across all test classes. No other device IDs are used unless explicitly documented.

### 3.2. Device Registration

(1) Before any HTTP endpoint (other than `POST /pair` itself) can be exercised, the test device must be registered:

```
POST /api/v1/pair
Body: { "DeviceId": "inttest-00000000-0000-0000-0000-000000000001", "Name": "Integration Test Tablet" }
Expected: 201 Created
```

(2) The Windows server must support re-registration of the test device between test runs. This means either:

  (a) Returning `201 Created` on each registration (idempotent), or
  (b) Returning `409 Conflict` for duplicates, which the Android tests accept as a valid "already paired" state.

(3) Both sides must support cleanup between test executions. The Windows server should provide a mechanism (database reset, test fixture teardown, or ephemeral in-memory database) to ensure a clean state.

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

**Android assumption:** Tests assert `201` for first registration, and `4xx` (any 400-level) for duplicates and invalid payloads.

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

**Android assumption:** Tests verify `200` with a non-null body containing `materials` and `questions` arrays (which may be empty). The test device must be registered before calling this endpoint.

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

**Android assumption:** Tests accept either `200` or `404` as valid outcomes. When `200`, the `feedback` array is verified to be non-null.

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
| Non-Android device | `403 Forbidden` |

**Android assumption:** Tests verify `200` with a body containing at least `TextSize` and `FeedbackStyle` fields. If the server has no configuration for the test device, it must still return default values.

**Windows note:** Per `ConfigController`, this endpoint also marks the device as having received configuration (implicit REFRESH_CONFIG acknowledgement, per `Session Interaction.md` §6(3)).

### 4.5. GET /attachments/{id} — Attachment Download

| Scenario | Expected Status | Response |
|----------|----------------|----------|
| Attachment exists | `200 OK` | Raw binary + `Content-Type` header |
| Unknown ID | `404 Not Found` | — |

**Android assumption:** Tests use a well-known test attachment ID of `test-attachment-001`. If this attachment is not pre-staged, the test gracefully accepts `404` and skips further assertions.

**Windows obligation:** To exercise the full attachment contract, the server should pre-stage a file with ID `test-attachment-001` in its attachment storage directory (`%APPDATA%/ManuscriptaTeacherApp/Attachments/`).

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

**Android assumption:** Tests use well-known IDs `test-question-001` and `test-material-001`. These need not correspond to real entities in the database — the server must accept the response for persistence even if the referenced question does not exist, **or** the server should have these entities pre-staged.

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

**Android assumption:** Batch semantics are all-or-nothing. If any response in the batch is invalid, the entire batch is rejected.

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
1. Android opens TCP connection to {host}:{tcpPort}
2. Android sends: [0x20][Device ID as UTF-8 bytes]
3. Windows responds: [0x21]
4. Connection remains open for subsequent messages
```

**Timing:**
- Android tests use a **3-second pairing timeout** and **1 retry** (faster than production defaults).
- The Windows server must send `PAIRING_ACK` promptly — within the 3-second window.

**State transitions (Android side):**
- `NOT_PAIRED` → `PAIRING_IN_PROGRESS` (on `startPairing()`)
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
- The Windows server parses with **case-insensitive property matching** and string-to-enum conversion.
- The Android client sends heartbeats at the configured interval (production: 3 seconds per `Session Interaction.md` §1(1)(c); tests: 1 second).
- The Windows server updates its heartbeat tracking on each STATUS_UPDATE and fires a disconnect event after **10 seconds** of silence, per `Session Interaction.md` §2(3).

### 5.5. HAND_RAISED / HAND_ACK

```
Android sends:  [0x11][Device ID as UTF-8]
Windows sends:  [0x06][Device ID as UTF-8]
```

**Android assumption:** After sending HAND_RAISED, the test expects HAND_ACK within **5 seconds** and verifies the response opcode is `0x06`.

### 5.6. DISTRIBUTE_ACK Format

Per `API Contract.md` §3.6.2:

```
Byte 0:       0x12
Bytes 1..N:   Device ID (UTF-8, null-terminated with 0x00)
Bytes N+1..M: Material ID (UTF-8)
```

**Critical detail:** The null byte (`0x00`) separates the two string fields. The Windows server uses `ExtractNullTerminatedStrings()` to parse this. Both sides must agree on this encoding.

### 5.7. FEEDBACK_ACK Format

Identical structure to DISTRIBUTE_ACK, with opcode `0x13` and Feedback ID instead of Material ID.

```
Byte 0:       0x13
Bytes 1..N:   Device ID (UTF-8, null-terminated with 0x00)
Bytes N+1..M: Feedback ID (UTF-8)
```

### 5.8. Server-Initiated Commands

The Android tests observe (but do not require) the following commands during a connected session:

| Command | Opcode | Operand | Expected ACK Pattern |
|---------|--------|---------|---------------------|
| LOCK_SCREEN | `0x01` | None | Next STATUS_UPDATE reports `LOCKED` (within 6s) |
| UNLOCK_SCREEN | `0x02` | None | Next STATUS_UPDATE reports `ON_TASK` or `IDLE` (within 6s) |
| REFRESH_CONFIG | `0x03` | None | Android calls `GET /config/{deviceId}` (within 5s) |
| UNPAIR | `0x04` | None | TCP connection terminates |

**Android test behaviour:** The command tests are **observational** — they pass whether or not any command is received. To exercise the full command flow, the Windows server would need to send these commands during the test window (e.g., via the Simulation API, per §8).

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

(1) The Windows server broadcasts on port **5913** (default) to the broadcast address **255.255.255.255**.

(2) Broadcast interval is **3 seconds** (configurable via `NetworkSettings:BroadcastIntervalMs`).

(3) The Android tests assert the interval between consecutive packets is between **1.5 and 6 seconds** (generous tolerance for system jitter).

### 6.3. Android Test Mechanics

(1) The Android UDP test opens a raw `DatagramSocket` bound to the configured UDP port.

(2) Socket receive timeout is **20 seconds** — the test fails if no packet arrives within this window.

(3) After parsing with `DiscoveryMessageParser.parse()`, the test verifies:
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
4. Assert PairingState == PAIRED
```

**Timeouts:**
- UDP receive: 20 seconds
- TCP pairing: 5 seconds (test-level latch)

### 7.2. Material Distribution Flow

The E2E distribution test exercises the TCP-triggered HTTP fetch pattern:

```
1. Pair device (TCP + HTTP, via helper)
2. Start heartbeat (STATUS_UPDATE every 1s)
3. Wait for DISTRIBUTE_MATERIAL (0x05)        → 15-second timeout
4. If received:
   a. GET /distribution/{deviceId}            → 200 OK
   b. For each material: send DISTRIBUTE_ACK  → [0x12][deviceId\0materialId]
5. Drain messages briefly, assert no UNPAIR received
```

**Critical:** The distribution test is **conditional** — if no DISTRIBUTE_MATERIAL signal arrives within 15 seconds, the test passes vacuously. To fully exercise this flow, materials must be staged on the Windows server and the DISTRIBUTE_MATERIAL signal sent during the test window.

### 7.3. Feedback Return Flow (Implied)

Though not currently implemented as a standalone E2E test, the feedback flow follows the same pattern:

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

### 8.1. Pre-Staged Data (Windows Server Obligations)

For integration tests to exercise their full paths, the Windows server should pre-stage the following data. If this data is absent, the corresponding tests pass with reduced coverage (graceful fallback).

| Data | Identifier | Purpose | Test File |
|------|-----------|---------|-----------|
| Attachment file | `test-attachment-001` | Attachment download test | `AttachmentEndpointIntegrationTest` |
| Question entity | `test-question-001` | Response submission test | `ResponseSubmissionIntegrationTest` |
| Material entity | `test-material-001` | Response submission test (material reference) | `ResponseSubmissionIntegrationTest` |
| Material bundle | (any, for test device) | Distribution bundle test | `DistributionEndpointIntegrationTest` |
| Feedback entities | (any, for test device) | Feedback retrieval test | `FeedbackEndpointIntegrationTest` |
| Configuration | (defaults for test device) | Config retrieval test | `ConfigEndpointIntegrationTest` |

### 8.2. Staging Approaches

The Windows team may satisfy these requirements through any of:

(a) **Database seeding** — Insert test entities via EF Core migrations or startup seeding when running in a test/integration environment.

(b) **Simulation API** — The Windows server exposes `POST /api/simulation/add-device` and related endpoints under the `/api/simulation` prefix. These could be extended to support material and feedback staging.

(c) **Test fixture files** — For attachments, place a file named `test-attachment-001.{ext}` in the attachments directory (`%APPDATA%/ManuscriptaTeacherApp/Attachments/`).

(d) **Dedicated integration test environment** — Use `ASPNETCORE_ENVIRONMENT=Integration` (or similar) with an `appsettings.Integration.json` that seeds the database.

### 8.3. State Isolation Between Tests

(1) Each integration test class registers the test device in its `@Before` setup. The Windows server must handle repeated registrations gracefully (§3.2).

(2) TCP tests pair at the start and close the connection in `@After`. The Windows server should clean up connection state when the TCP socket closes.

(3) The Windows server's heartbeat monitor (10-second timeout) should not interfere with test execution, as Android tests send heartbeats at 1-second intervals during active TCP sessions.

---

## Section 9 — Timing Constraints

### 9.1. Timeouts Summary

| Operation | Android Test Timeout | Windows Server Timeout | Source |
|-----------|---------------------|----------------------|--------|
| TCP pairing handshake | 3 seconds | — | `NetworkIntegrationHarness` |
| Heartbeat interval | 1 second (test) / 3 seconds (production) | — | `Session Interaction.md` §1(1)(c) |
| Heartbeat disconnect | — | 10 seconds | `Session Interaction.md` §2(3) |
| DISTRIBUTE_MATERIAL signal wait | 15 seconds | — | `MaterialDistributionFlowIntegrationTest` |
| DISTRIBUTE_ACK wait | — | 30 seconds | `Session Interaction.md` §3(6) |
| FEEDBACK_ACK wait | — | 30 seconds | `Session Interaction.md` §7(5) |
| LOCK/UNLOCK confirmation | — | 6 seconds | `Session Interaction.md` §6(2)(c) |
| REFRESH_CONFIG confirmation | — | 5 seconds | `Session Interaction.md` §6(3)(b) |
| HAND_RAISED retry | 3 seconds | — | `Session Interaction.md` §4A(3) |
| TCP message poll | 5 seconds | — | Various TCP tests |
| UDP socket receive | 20 seconds | — | Discovery tests |
| UDP broadcast interval | — | 3 seconds | `API Contract.md` §1.1 |

### 9.2. Tolerance

(1) Android tests use generous tolerances for timing-dependent assertions (e.g., UDP broadcast cadence accepted between 1.5 and 6 seconds).

(2) Tests that depend on server-initiated actions (commands, distribution signals) use conditional logic — they pass whether or not the action occurs within the timeout window.

---

## Section 10 — Serialisation Conventions

### 10.1. JSON Field Naming

Per `Validation Rules.md` §1(6), all JSON field names in DTOs use **PascalCase**:

```
✓ "DeviceId", "MaterialType", "QuestionText", "BatteryLevel"
✗ "deviceId", "materialType", "questionText", "batteryLevel"
```

**Android implementation:** Uses Gson `@SerializedName("PascalCaseName")` annotations.
**Windows implementation:** Uses System.Text.Json with `PropertyNameCaseInsensitive = true` for parsing, and PascalCase property names for serialisation.

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

(2) Device IDs are generated by the Android client. Material, question, and feedback IDs are generated by the Windows server. Per `Validation Rules.md` §3.

(3) The test device ID (`inttest-00000000-0000-0000-0000-000000000001`) is a special case with a non-standard prefix.

### 10.5. String Encoding

(1) All TCP message operands containing strings use **UTF-8** encoding.

(2) DISTRIBUTE_ACK and FEEDBACK_ACK operands use a **null byte (`0x00`)** as the separator between Device ID and Entity ID.

---

## Section 11 — Build and Execution

### 11.1. Running Android Integration Tests

```bash
cd android
export JAVA_HOME=$(/usr/libexec/java_home -v 17)

# Integration tests only (excluded from normal test runs)
./gradlew test -Pintegration

# Normal unit tests (integration tests excluded via @Category filter)
./gradlew testDebugUnitTest
```

Integration tests are annotated with `@Category(IntegrationTest.class)` and excluded from the default test task via Gradle configuration.

### 11.2. Running Windows Server for Integration Tests

```bash
cd windows/ManuscriptaTeacherApp/Main
dotnet run --environment Development
```

The server must be running and accessible at the configured host and ports before Android integration tests are started.

### 11.3. CI Pipeline Considerations

(1) Integration tests require a running Windows server instance. CI pipelines must start the server as a background process before executing Android integration tests.

(2) The test device must be fresh (no prior pairing state) or the server must handle re-pairing gracefully.

(3) Port conflicts must be avoided. The default ports (5910–5913) should be reserved for integration test use.

---

## Appendix A — Known Gaps and Future Work

(1) **Material staging automation** — No automated mechanism currently exists to stage materials for the test device. The Simulation API (`/api/simulation`) could be extended with endpoints for creating test materials and questions.

(2) **Feedback staging automation** — Similar to materials, feedback entities must be manually or programmatically created for the test device's submitted responses.

(3) **Server command triggering** — The TCP command tests (LOCK, UNLOCK, REFRESH_CONFIG, UNPAIR) are observational. A test control API or Simulation API extension would allow the Android tests to trigger these commands on demand.

(4) **Bidirectional test harness** — Currently only the Android side has a test harness. A corresponding Windows-side harness that exercises the server against a simulated Android client would provide symmetric test coverage.

(5) **Attachment staging** — The `test-attachment-001` file must be manually placed. An API endpoint for uploading test attachments would improve automation.

---

## Appendix B — Document References

| Document | Relevance |
|----------|-----------|
| `API Contract.md` v1.1 | Authoritative for HTTP endpoints, binary protocol, and opcode registry |
| `Validation Rules.md` | Authoritative for DTO field definitions, constraints, and PascalCase naming |
| `Pairing Process.md` | Authoritative for pairing handshake sequence (overrides API Contract on conflicts) |
| `Session Interaction.md` | Authoritative for heartbeat, distribution, feedback, and control command flows |
| `Material Encoding.md` | Authoritative for material `Content` field encoding |
