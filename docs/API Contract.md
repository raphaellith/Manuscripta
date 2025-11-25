# Manuscripta API Contract

This document defines the communication protocols between the Teacher Application (Windows Server) and the Student Application (Android Client).

**Version:** 1.0
**Status:** Draft

## Overview

This API contract conforms to requirements **NET1** (distributing material content to 30+ student tablets through LAN) and **NET2** (receiving student responses such as poll and quiz answers).

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
   - Session identifier

2. **Student Discovery:** Student tablets listen for UDP broadcast messages on the same port. Upon receiving a broadcast, they:
   - Extract the teacher's IP address and port information
   - Initiate HTTP and TCP connections using the discovered information
   - Complete the pairing handshake on both channels

**Broadcast Message Format:**
```json
{
  "type": "DISCOVERY",
  "teacherIp": "192.168.1.100",
  "httpPort": 5911,
  "tcpPort": 5912,
  "sessionId": "session-uuid",
  "timestamp": "2023-10-27T10:00:00Z"
}
```

**Notes:**
- The teacher application should broadcast discovery messages at regular intervals (3 seconds).
- Student tablets should verify the session identifier to ensure they're connecting to the correct teacher session.

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

## 3. TCP Protocol (Real-time Control)

Used for low-latency control signals that require immediate transmission. TCP messages use a **binary protocol** with opcode-based message structure.

### 3.1. TCP Message Structure

Each TCP message consists of:
1. **Opcode** (1 byte, unsigned, little-endian): Identifies the message type
2. **Operand** (variable length, optional): Custom data associated with the message type

The length and encoding/decoding of the operand depends on the specific opcode.

| Opcode | Name | Operand | Description |
|--------|------|---------|-------------|
| `0x01` | LOCK_SCREEN | None | Locks the student's screen |
| `0x02` | UNLOCK_SCREEN | None | Unlocks the student's screen |
| `0x03` | REFRESH_CONFIG | None | Triggers tablet to re-fetch configuration via HTTP |
| `0x04` | FETCH_MATERIALS | None | Signals client to fetch materials via HTTP GET /materials |

**Heartbeat Response Pattern:**
When the server receives a `STATUS_UPDATE` (0x10) from a client, it checks if there are pending materials for that device. If so, it responds with `FETCH_MATERIALS` (0x04). The client then initiates an HTTP request to download materials. This pattern is necessary because the Windows server cannot initiate HTTP connections to clients.

**Example: Fetch Materials Message**
```
Byte 0: 0x04 (FETCH_MATERIALS opcode)
```

**Example: Lock Screen Message**
```
Byte 0: 0x01 (LOCK_SCREEN opcode)
```

**Example: Unlock Screen Message**
```
Byte 0: 0x02 (UNLOCK_SCREEN opcode)
```

#### Client → Server Status Updates

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

### 3.3. Extensibility

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