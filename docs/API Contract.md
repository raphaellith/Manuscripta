# Manuscripta API Contract

This document defines the communication protocols between the Teacher Application (Windows Server) and the Student Application (Android Client).

**Version:** 1.0
**Status:** Draft

## Overview

This API contract conforms to requirements **NET1** (distributing material content to 30+ student tablets through LAN) and **NET2** (receiving student responses such as poll and quiz answers).

### Architecture of Communication Subsystem

The system uses a **hybrid multi-channel networking approach** with separate protocols for different types of communication:

-   **HTTP (REST)**: For transmission of large chunks of data, such as lesson materials (Server→Client) and student responses (Client→Server). Messages are primarily transmitted in JSON format.
-   **TCP (Socket)**: For low-latency control signals that require real-time transmission, such as lock screen commands (Server→Client) and student tablet status changes (Client→Server).
-   **UDP Broadcasting**: For device discovery and pairing, allowing student tablets to discover the teacher laptop on the local network.

If a performance bottleneck is observed during implementation, UDP may be introduced for additional message types.

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

The student tablet pulls lesson materials from the teacher server.

#### Get All Materials
Fetches a list of available materials for the current session.

-   **Endpoint:** `GET /materials`
-   **Response:** `200 OK`
    ```json
    [
      {
        "materials": ["uuid-string1", "uuid-string2", "uuid-string3"] // List of ids, in order of presentation
      }
    ]
    ```

#### Get Material Details
Downloads the full content of a specific material.

-   **Endpoint:** `GET /materials/{id}`
-   **Response:** `200 OK`
    ```json
    {
      "id": "uuid-string",
      "MaterialType": "QUIZ",
      "title": "Algebra Basics",
      "content": "...", // HTML or Text content
      "metadata": {// This is where we can put lesson - specific configurations (button toggles, characters, etc..)
      },
      "timestamp": "2023-10-27T10:00:00Z",
      "vocabularyTerms": [
        { "term": "Variable", "definition": "A symbol used to represent a number." }
      ],
      "questions": [
        {
          "id": "q-uuid-1",
          "text": "What is x if x + 2 = 5?",
          "type": "MULTIPLE_CHOICE",
          "options": ["1", "2", "3", "4"],
          "correctAnswer": "3"
        }
      ]
    }
    ```

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
      "kioskMode": true, // Final configuration to be determined
      "textSize": "medium"
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
      "deviceId": "device-uuid-generated-by-client"
    }
    ```
-   **Response:** `201 Created`
    ```json
    {} // Empty 201 to confirm successful pairing
    ```
-   **Error Response:** `409 Conflict` (if deviceId is already paired)

#### Submit Response
Submits a single answer to a question.

-   **Endpoint:** `POST /responses`
-   **Body:**
    ```json
    {
      "id": "resp-uuid-generated-by-client",
      "questionId": "q-uuid-1",
      "materialId": "mat-uuid-1",
      "studentId": "device-id-or-student-uuid",
      "answer": "3",
      "timestamp": "2023-10-27T10:05:00Z"
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
      "responses": [
        // Array of response objects as above
      ]
    }
    ```

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

Status Update JSON payload:
```json
{
  "deviceId": "device-123",
  "status": "ON_TASK",
  "batteryLevel": 85,
  "currentMaterialId": "mat-uuid-1",
  "studentView": "StudentView" // Placeholder for a system that allows the teacher to pull up a student's view
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

### 4.1. Entity Identification
**CRITICAL:** Entity IDs (UUIDs) must be persistent and consistent across both Windows and Android applications.
-   **Materials/Questions:** Created by Windows (Server), ID assigned by Server. Android (Client) **must preserve** this ID.
-   **Responses/Sessions:** Created by Android (Client), ID assigned by Client. Windows (Server) **must preserve** this ID.

### 4.2. Material Types
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

---