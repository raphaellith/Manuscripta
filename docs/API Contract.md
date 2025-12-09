# Manuscripta API Contract

This document defines the communication protocols between the Teacher Application (Windows Server) and the Student Application (Android Client).

**Version:** 1.1
**Status:** Draft

## Overview

This API contract conforms to requirements **NET1** (distributing material content to 30+ student tablets through LAN) and **NET2** (receiving student responses such as poll and quiz answers). This API contract conforms to Validation Rules.md for data model definitions and validation. See Validation Rules.md §1(5) for referencing requirements.

### Architecture of Communication Subsystem

The system uses a **hybrid multi-channel networking approach** with separate protocols for different types of communication:

-   **HTTP (REST)**: For transmission of large chunks of data, such as lesson materials (Server→Client) and student responses (Client→Server). Messages are primarily transmitted in JSON format. **Note:** The Windows server cannot initiate HTTP requests to clients, so material distribution is triggered via TCP signals (see below).
-   **TCP (Socket)**: For low-latency control signals that require real-time transmission, such as lock screen commands (Server→Client) and student tablet status changes (Client→Server). Also used to notify clients when new materials are available.
-   **UDP Broadcasting**: For device discovery and pairing, allowing student tablets to discover the teacher laptop on the local network.

If a performance bottleneck is observed during implementation, UDP may be introduced for additional message types.

### Material Distribution Pattern (Heartbeat-Triggered Fetch)

Since the Windows server cannot push HTTP requests to Android clients, material distribution uses a **heartbeat-triggered fetch** pattern:

1. **Android Client** sends periodic `STATUS_UPDATE` (0x10) heartbeat messages via TCP
2. **Windows Server** receives the heartbeat and checks if new materials are available for this device
3. **If materials are available**, the server responds with a `FETCH_MATERIALS` (0x04) TCP message
4. **Android Client** receives the signal and initiates an HTTP `GET /materials` request to fetch the material list
5. **Android Client** downloads individual materials via `GET /materials/{id}`

This pattern ensures material distribution works within the constraint that the server cannot initiate connections to clients.

**Connection Establishment:**
Each protocol operates on its own channel. A connection is deemed established only after pairing procedures on **all channels** (HTTP and TCP) have been completed successfully.

**Roles:**
-   **Server:** Teacher Application (Windows)
-   **Client:** Student Application (Android)

**Ports:**
-   **HTTP Port:** `<HTTP_PORT>` (5911)
-   **TCP Port:** `<TCP_PORT>` (5912)
-   **UDP Broadcast Port:** `<UDP_PORT>` (5913)

**Base URL (HTTP):** `http://<TEACHER_IP>:<HTTP_PORT>/api/v<API_VERSION>`

---

## 1. Pairing and Device Discovery

Before establishing HTTP and TCP connections, the student tablets must first discover the teacher laptop on the local network.

### 1.1. UDP Broadcasting (Chosen Approach)

The teacher laptop broadcasts its presence on the local network using UDP, allowing student tablets to discover it automatically.

**Discovery Process:**
1. **Teacher Broadcast:** The teacher application continuously broadcasts a discovery message on the UDP broadcast port containing:
   - Teacher IP address
   - HTTP port
   - TCP port

2. **Student Discovery:** Student tablets listen for UDP broadcast messages on the same port. Upon receiving a broadcast, they:
   - Extract the teacher's IP address and port information
   - Initiate HTTP and TCP connections using the discovered information
   - Complete the pairing handshake on both channels, as specified in `Pairing Process.md` §2

**Broadcast Message Format (Binary):**

UDP discovery messages use the same opcode-operand binary format as TCP messages for consistency and efficiency.

| Field | Offset | Size | Description |
|-------|--------|------|-------------|
| Opcode | 0 | 1 byte | `0x00` = DISCOVERY |
| IP Address | 1 | 4 bytes | IPv4 address (network byte order, big-endian) |
| HTTP Port | 5 | 2 bytes | Unsigned, little-endian |
| TCP Port | 7 | 2 bytes | Unsigned, little-endian |

**Total message size:** 9 bytes

**Example: Discovery Message for 192.168.1.100, HTTP 5911, TCP 5912**
```
Byte 0:      0x00                         (DISCOVERY opcode)
Bytes 1-4:   0xC0 0xA8 0x01 0x64          (192.168.1.100)
Bytes 5-6:   0x17 0x17                    (5911 little-endian)
Bytes 7-8:   0x18 0x17                    (5912 little-endian)
```

**Notes:**
- The teacher application should broadcast discovery messages at regular intervals (3 seconds).
- IP address uses network byte order (big-endian) as per standard networking conventions.
- Ports use little-endian to maintain consistency with TCP message operands.

---

## 2. HTTP Endpoints (Content & Config)

### 2.1. Lesson Materials (Server -> Client)

**DELETED** - These endpoints have been removed due to security concerns (unauthorized access to all materials). Use the alternative API: `GET /session/{deviceId}` as specified in `API Contract.md` §2.5 for session-specific material distribution.

### 2.1.3. Attachments (Server -> Client)
Downloads specific attachment files referenced within material content.

-   **Endpoint:** `GET /attachments/{id}`
-   **Response:** `200 OK`
    The response body will contain the raw binary data of the requested file (e.g., image, PDF).
    -   **Content-Type:** `image/png`, `application/pdf`, etc. (determined by the server based on file type).

**Note:** The `content` field within material details (`GET /materials/{id}`) may contain references to these attachments using URLs like `/attachments/{id}`. The Android client is expected to fetch these referenced attachments separately.

### 2.2. Tablet Configuration (Server -> Client)

Tablet configuration is an object associated with lesson materials but handled separately to allow dynamic updates.

#### Get Configuration
-   **Endpoint:** `GET /config`
-   **Response:** `200 OK`
    ```json
    {
      "KioskMode": true, // Final configuration to be determined
      "TextSize": "medium"
    }
    ```

### 2.3. Student Responses (Client -> Server)

Students submit their work to the teacher.

### 2.4. Device Pairing (Client -> Server)

Used during the pairing handshake to register a student device with the teacher server. See `Pairing Process.md` §2 for the full pairing sequence.

#### Register Device
-   **Endpoint:** `POST /pair`
-   **Body:**
    ```json
    {
      "DeviceId": "device-uuid-generated-by-client"
    }
    ```
-   **Response:** `201 Created`
    ```json
    {} // Empty 201 to confirm successful pairing
    ```
-   **Error Response:** `409 Conflict` (if DeviceId is already paired)

#### Submit Response
Submits a single answer to a question. The JSON object conforms to ResponseEntity as defined in Validation Rules.md §2C.

-   **Endpoint:** `POST /responses`
-   **Body:**
    ```json
    {
      "Id": "resp-uuid-generated-by-client",
      "QuestionId": "q-uuid-1",
      "MaterialId": "mat-uuid-1",
      "StudentId": "device-id-or-student-uuid",
      "Answer": "3",
      "Timestamp": "2023-10-27T10:05:00Z"
    }
    ```
-   **Response:** `201 Created`
    ```json
    {} // Empty 201 to confirm submission
    ```

#### Batch Submit Responses
Submits multiple responses at once (e.g., when reconnecting after offline mode).

-   **Endpoint:** `POST /responses/batch`
-   **Body:**
    ```json
    {
      "Responses": [
        // Array of response objects as above
      ]
    }
    ```
-   **Response:** `201 Created`

### 2.5. Session Management (Server -> Client via TCP trigger)

Used to distribute materials to devices during a session. See `Session Interaction.md` §3 for the full distribution process.

#### Get Session Materials
Retrieves materials and questions assigned to a specific device for the current session.

-   **Endpoint:** `GET /session/{deviceId}`
-   **Response:** `200 OK`
    ```json
    {
      "materials": [
        // Array of MaterialEntity objects as defined in Validation Rules.md §2A
      ],
      "questions": [
        // Array of QuestionEntity objects as defined in Validation Rules.md §2B
      ]
    }
    ```
-   **Error Response:** `404 Not Found` (if no active session for deviceId)

---

## 3. Binary Protocol (TCP & UDP)

Both TCP and UDP use a unified **binary protocol** with opcode-based message structure for consistency and efficiency.

### 3.1. Message Structure

Each binary message consists of:
1. **Opcode** (1 byte, unsigned): Identifies the message type
2. **Operand** (variable length, optional): Custom data associated with the message type

The length and encoding/decoding of the operand depends on the specific opcode.

### 3.2. Opcode Registry

| Range | Purpose |
|-------|---------|
| `0x00` | UDP Discovery |
| `0x01` - `0x0F` | Server → Client Control (TCP) |
| `0x10` - `0x1F` | Client → Server Status (TCP) |
| `0x20` - `0x2F` | Pairing (TCP) |

### 3.3. UDP Messages

#### Server Broadcast

| Opcode | Name | Operand | Description |
|--------|------|---------|-------------|
| `0x00` | DISCOVERY | IP (4 bytes) + HTTP port (2 bytes) + TCP port (2 bytes) | Broadcasts server presence |

See §1.1 for detailed format.

### 3.4. TCP Control Messages (Server → Client)

| Opcode | Name | Operand | Description |
|--------|------|---------|-------------|
| `0x01` | LOCK_SCREEN | None | Locks the student's screen |
| `0x02` | UNLOCK_SCREEN | None | Unlocks the student's screen |
| `0x03` | REFRESH_CONFIG | None | Triggers tablet to re-fetch configuration via HTTP |
| `0x04` | UNPAIR | None | Unpairs the device |
| `0x05` | DISTRIBUTE_MATERIAL | None | Instructs device to fetch materials for a session |

### 3.5. TCP Pairing Messages

Used during the pairing handshake to establish TCP connectivity. See `Pairing Process.md` §2 for the full pairing sequence.

#### Client → Server Pairing Request

| Opcode | Name | Operand | Description |
|--------|------|---------|-------------|
| `0x20` | PAIRING_REQUEST | Device ID (UTF-8 string) | Client requests pairing with server |

**Example: Pairing Request Message**
```
Byte 0: 0x20 (PAIRING_REQUEST opcode)
Bytes 1-N: "device-uuid" (UTF-8 encoded device ID)
```

#### Server → Client Pairing Acknowledgement

| Opcode | Name | Operand | Description |
|--------|------|---------|-------------|
| `0x21` | PAIRING_ACK | None | Server acknowledges successful TCP pairing |

**Example: Pairing Acknowledgement Message**
```
Byte 0: 0x21 (PAIRING_ACK opcode)
```

### 3.6. TCP Status Messages (Client → Server)

| Opcode | Name | Operand | Description |
|--------|------|---------|-------------|
| `0x10` | STATUS_UPDATE | JSON payload | Reports device status to teacher |
| `0x11` | HAND_RAISED | Device ID (UTF-8 string) | Student requests help |

**Example: Status Update Message**
```
Byte 0: 0x10 (STATUS_UPDATE opcode)
Bytes 1-N: JSON payload (see below)
```

Status Update JSON payload must conform to the `DeviceStatusEntity` as defined in `Validation Rules.md` §2E:
```json
{
  "DeviceId": "device-123",
  "Status": "ON_TASK",
  "BatteryLevel": 85,
  "CurrentMaterialId": "mat-uuid-1",
  "StudentView": "page-5",
  "Timestamp": 1702147200
}
```

**Example: Hand Raised Message**
```
Byte 0: 0x11 (HAND_RAISED opcode)
Bytes 1-N: "device-123" (UTF-8 encoded device ID)
```

### 3.7. Extensibility

Additional opcodes can be defined as needed. Both applications should:
- Maintain a registry of opcode definitions
- Implement encoder/decoder functions for each message type
- Handle unknown opcodes gracefully (log and ignore)

---

## 4. Data Models

All data models in this contract must conform to Validation Rules.md. This document takes precedence in case of contradictions.

### 4.1. Entity Identification
**CRITICAL:** Entity IDs (UUIDs) must be persistent and consistent across both Windows and Android applications.
-   **Materials/Questions:** Created by Windows (Server), ID assigned by Server. Android (Client) **must preserve** this ID.
-   **Responses/Sessions:** Created by Android (Client), ID assigned by Client. Windows (Server) **must preserve** this ID.

### 4.2. Material Types
See Validation Rules.md §2A(1)(a) for the authoritative MaterialType enum.
-   `READING`: Reading material or informational content.
-   `QUIZ`: Interactive questions with immediate feedback.
-   `WORKSHEET`: Content for reading and annotation.
-   `POLL`: Quick class voting.

### 4.3. Device Status Enum
-   `ON_TASK`: Student is active in the app.
-   `IDLE`: No activity for a threshold period.
-   `HAND_RAISED`: Student explicitly requested help.
-   `LOCKED`: Device is remotely locked.
-   `DISCONNECTED`: (Server-side inferred status).

### 4.4. Serialization Rules
Timestamps are transmitted as ISO 8601 strings (e.g., '2023-10-27T10:00:00Z') but must deserialize to Unix longs per Validation Rules.md §2A(1)(d).
IDs are transmitted as strings; validate as per Validation Rules.md §1(2).
Enums (e.g., MaterialType) are transmitted as strings; must match Validation Rules.md values exactly.

---