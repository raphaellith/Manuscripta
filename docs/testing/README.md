# Testing Scripts

This document describes the shell scripts in `windows/ManuscriptaTeacherApp/Main/Testing/scripts/` for manual and automated testing against a running Manuscripta server.

## Prerequisites

- The Manuscripta Windows server must be running (default: `http://localhost:5911`).
- `curl` and `python3` must be available on your PATH.
- The `SimulationController` (at `/api/simulation/`) must be reachable — it is included in the standard server build.

---

## 1. `add-devices.sh` — Generate Dummy Devices

Creates one or more simulated Android devices via the simulation API. Each device is registered with a random name, set to `ON_TASK` status, and announced to the frontend via SignalR (`DevicePaired` broadcast).

### Usage

```bash
./add-devices.sh [COUNT] [BASE_URL]
```

| Parameter  | Default                  | Description                      |
|------------|--------------------------|----------------------------------|
| `COUNT`    | `5`                      | Number of devices to create      |
| `BASE_URL` | `http://localhost:5911`  | Server base URL                  |

### Examples

```bash
# Create 5 devices on the default server
./add-devices.sh

# Create 10 devices
./add-devices.sh 10

# Create 3 devices on a remote server
./add-devices.sh 3 http://192.168.1.50:5911
```

### Output

Returns JSON with the created devices, e.g.:

```json
{
    "count": 2,
    "devices": [
        {
            "deviceId": "a1b2c3d4-...",
            "name": "Simulated Grid Device 4821"
        },
        {
            "deviceId": "e5f6a7b8-...",
            "name": "Simulated Grid Device 1347"
        }
    ]
}
```

### Modifying device generation

- **Number of devices**: pass a different `COUNT` argument.
- **Device names**: names are auto-generated server-side as `"Simulated Grid Device XXXX"` (random 4-digit number). To change the naming scheme, edit [`SimulationController.cs`](../../windows/ManuscriptaTeacherApp/Main/Testing/Simulation/SimulationController.cs), specifically the `AddDevice` method's `name` variable.
- **Initial status**: devices start as `ON_TASK`. To change this, modify the `DeviceStatus` enum value in the `AddDevice` method of `SimulationController.cs`.

---

## 2. `submit-responses.sh` — Submit Responses to Questions

Submits one or more student responses to a specific question from a specific device. This calls `POST /api/v1/responses` as defined in [API Contract §2.3](../../docs/API%20Contract.md).

### Usage

```bash
./submit-responses.sh DEVICE_ID QUESTION_ID [ANSWER] [BASE_URL]
```

| Parameter     | Required | Default                            | Description                           |
|---------------|----------|------------------------------------|---------------------------------------|
| `DEVICE_ID`   | Yes      | —                                  | UUID of a paired device               |
| `QUESTION_ID` | Yes      | —                                  | UUID of an existing question          |
| `ANSWER`      | No       | `"Sample answer from device"`      | The answer string (see below)         |
| `BASE_URL`    | No       | `http://localhost:5911`            | Server base URL                       |

### Answer format

The `ANSWER` parameter is interpreted based on the question type:

| Question Type      | Answer Format                                   | Example   |
|--------------------|--------------------------------------------------|-----------|
| `WRITTEN_ANSWER`   | Any text string                                  | `"The answer is photosynthesis"` |
| `MULTIPLE_CHOICE`  | Zero-based integer index of the selected option  | `"2"` (selects the third option) |

### Environment variables

These optional environment variables provide advanced control:

| Variable          | Default          | Description                                       |
|-------------------|------------------|---------------------------------------------------|
| `RESPONSE_ID`     | Auto-generated   | Override the response UUID                        |
| `TIMESTAMP`       | Current UTC time | Override the ISO 8601 timestamp                   |
| `IS_CORRECT`      | Not sent         | Set to `true` or `false` to include correctness   |
| `RESPONSE_COUNT`  | `1`              | Number of responses to submit (each with unique ID) |

### Examples

```bash
# Submit a written answer
./submit-responses.sh \
  "a1b2c3d4-e5f6-7890-abcd-ef1234567890" \
  "11111111-2222-3333-4444-555555555555" \
  "The mitochondria is the powerhouse of the cell"

# Submit a multiple-choice answer (option index 1)
./submit-responses.sh \
  "a1b2c3d4-e5f6-7890-abcd-ef1234567890" \
  "11111111-2222-3333-4444-555555555555" \
  "1"

# Submit 5 responses with different UUIDs
RESPONSE_COUNT=5 ./submit-responses.sh \
  "a1b2c3d4-e5f6-7890-abcd-ef1234567890" \
  "11111111-2222-3333-4444-555555555555" \
  "My answer"

# Submit with explicit correctness and timestamp
IS_CORRECT=true TIMESTAMP="2026-03-06T12:00:00Z" ./submit-responses.sh \
  "a1b2c3d4-e5f6-7890-abcd-ef1234567890" \
  "11111111-2222-3333-4444-555555555555" \
  "0"
```

### Modifying response generation

- **Device**: change the `DEVICE_ID` argument. Obtain valid device IDs from `add-devices.sh` output.
- **Question**: change the `QUESTION_ID` argument. You must use the UUID of a question that already exists on the server (create questions through the teacher UI).
- **Answer content**: change the `ANSWER` argument. For `MULTIPLE_CHOICE` questions, use a valid zero-based index. For `WRITTEN_ANSWER` questions, use any text.
- **Batch submission**: set `RESPONSE_COUNT=N` to submit multiple responses at once. Each gets a unique auto-generated UUID and timestamp.
- **Correctness flag**: set `IS_CORRECT=true` or `IS_CORRECT=false` to include the optional `IsCorrect` field (per Validation Rules §2C(2)(a)). If not set, the field is omitted.
- **Custom IDs/timestamps**: set `RESPONSE_ID` and/or `TIMESTAMP` environment variables to override auto-generation. When using `RESPONSE_COUNT > 1`, `RESPONSE_ID` is ignored after the first iteration to avoid duplicate IDs.

---

## Typical Workflow

A common end-to-end testing workflow:

```bash
# 1. Start the Manuscripta server

# 2. Create some dummy devices
./add-devices.sh 5
# Note the device IDs from the output

# 3. Create questions through the teacher UI
# Note the question IDs (visible in the UI or via SignalR)

# 4. Submit responses from the dummy devices
./submit-responses.sh "<device-id>" "<question-id>" "My answer"

# 5. Check the responses page in the teacher UI
```

---

## Existing Simulation Endpoints

The scripts call into the following server-side endpoints defined in [`SimulationController.cs`](../../windows/ManuscriptaTeacherApp/Main/Testing/Simulation/SimulationController.cs):

| Method | Endpoint                             | Description                         |
|--------|--------------------------------------|-------------------------------------|
| POST   | `/api/simulation/add-device?count=N` | Register N dummy devices            |
| POST   | `/api/simulation/update-status`      | Update a device's status            |
| POST   | `/api/simulation/raise-hand`         | Simulate a hand-raise event         |
| POST   | `/api/simulation/disconnect`         | Simulate device disconnection       |

And the standard API endpoint:

| Method | Endpoint             | Description                              |
|--------|----------------------|------------------------------------------|
| POST   | `/api/v1/responses`  | Submit a student response (API Contract §2.3) |
