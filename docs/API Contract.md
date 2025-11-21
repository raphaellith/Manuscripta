# Manuscripta API Contract

This document defines the communication protocols between the Teacher Application (Windows Server) and the Student Application (Android Client).

**Version:** 1.0
**Status:** Draft

## Overview

The system uses a hybrid networking approach:
-   **HTTP (REST)**: For high-latency, content-heavy operations (Lesson materials, Configuration, Student Responses).
-   **TCP (Socket)**: For low-latency, real-time control signals (Lock screen, Status updates).

**Roles:**
-   **Server:** Teacher Application (Windows)
-   **Client:** Student Application (Android)

**Base URL (HTTP):** `http://<TEACHER_IP>:<PORT>/api/v1`
**TCP Port:** `<TCP_PORT>`

---

## 1. HTTP Endpoints (Content & Config)

### 1.1. Lesson Materials (Server -> Client)

The student tablet pulls lesson materials from the teacher server.

#### Get All Materials
Fetches a list of available materials for the current session.

-   **Endpoint:** `GET /materials`
-   **Response:** `200 OK`
    ```json
    [
      {
        "id": "uuid-string",
        "title": "Algebra Basics",
        "type": "QUIZ", // or LESSON, WORKSHEET, POLL
        "description": "Introduction to variables",
        "timestamp": "2023-10-27T10:00:00Z"
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
      ],
      "attachments": {
        "worksheet.pdf": "JVBERi0xLj...", // Base64 encoded file content
        "diagram1.png": "iVBORw0KG..."
      }
    }
    ```

**Note on Attachments:**
Files (PDFs, images) are sent as Base64 strings within the `attachments` map. The Android client **MUST** decode these strings and save them to local storage immediately upon receipt. The `content` field may reference these files by name (e.g., `<img src="diagram.png">`).

### 1.2. Tablet Configuration (Server -> Client)

Tablet configuration is an object associated with lesson materials but handled separately to allow dynamic updates.

#### Get Configuration
-   **Endpoint:** `GET /config`
-   **Response:** `200 OK`
    ```json
    {
      "kioskMode": true,
      "textSize": "medium"
    }
    ```

### 1.3. Student Responses (Client -> Server)

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
      "selectedAnswer": "3",
      "timestamp": "2023-10-27T10:05:00Z"
    }
    ```
-   **Response:** `201 Created`
    ```json
    {
      "success": true,
      "feedback": "Correct!", // Immediate feedback if enabled, this could also be llm generated if we can get that working.
      "isCorrect": true
    }
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

## 2. TCP Protocol (Real-time Control)

Used for low-latency signaling. Messages are JSON strings terminated by a newline character `\n`.

### 2.1. Control Signals (Server -> Client)

The teacher sends commands to control the tablet state.

#### Lock Screen
Locks the student's screen to focus attention.
-   **Payload:**
    ```json
    {
      "type": "CONTROL",
      "action": "LOCK_SCREEN",
      "message": "Eyes on the teacher, please." // Do we want a message?
    }
    ```

#### Unlock Screen
-   **Payload:**
    ```json
    {
      "type": "CONTROL",
      "action": "UNLOCK_SCREEN"
    }
    ```

#### Update Configuration
Triggers the tablet to re-fetch configuration via HTTP.
-   **Payload:**
    ```json
    {
      "type": "CONTROL",
      "action": "REFRESH_CONFIG"
    }
    ```

### 2.2. Student Status (Client -> Server)

The tablet reports its real-time status to the teacher dashboard.

#### Status Update
Sent periodically or on state change.
-   **Payload:**
    ```json
    {
      "type": "STATUS",
      "deviceId": "device-123",
      "status": "ON_TASK", // or IDLE, HAND_RAISED, NEEDS_HELP, LOCKED
      "batteryLevel": 85,
      "currentMaterialId": "mat-uuid-1"
    }
    ```

#### Hand Raised
Sent when the student presses the "Raise Hand" button.
-   **Payload:**
    ```json
    {
      "type": "STATUS",
      "deviceId": "device-123",
      "status": "HAND_RAISED"
    }
    ```

---

## 3. Data Models

### 3.1. Entity Identification
**CRITICAL:** Entity IDs (UUIDs) must be persistent and consistent across both Windows and Android applications.
-   **Materials/Questions:** Created by Windows (Server), ID assigned by Server. Android (Client) **must preserve** this ID.
-   **Responses/Sessions:** Created by Android (Client), ID assigned by Client. Windows (Server) **must preserve** this ID.

### 3.2. Material Types
-   `LESSON`: Reading material or informational content.
-   `QUIZ`: Interactive questions with immediate feedback.
-   `WORKSHEET`: Content for reading and annotation.
-   `POLL`: Quick class voting.

### 3.3. Device Status Enum
-   `ON_TASK`: Student is active in the app.
-   `IDLE`: No activity for a threshold period.
-   `HAND_RAISED`: Student explicitly requested help.
-   `LOCKED`: Device is remotely locked.
-   `DISCONNECTED`: (Server-side inferred status).
