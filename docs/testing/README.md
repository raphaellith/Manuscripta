# Testing Scripts

This document describes the PowerShell scripts in `windows\ManuscriptaTeacherApp\Main\Testing\scripts\` for manual and automated testing against a running Manuscripta server.

## Prerequisites

- The Manuscripta Windows server must be running (default: `http://localhost:5911`).
- PowerShell 5.1+ (included with Windows 10/11).
- The `SimulationController` (at `/api/simulation/`) must be reachable — it is included in the standard server build.

> **Execution Policy**: If you get an error about script execution being disabled, run:
> ```powershell
> Set-ExecutionPolicy -Scope CurrentUser -ExecutionPolicy RemoteSigned
> ```

---

## 1. `add-devices.ps1` — Generate Dummy Devices

Creates one or more simulated Android devices via the simulation API. Each device is registered with a random name, set to `ON_TASK` status, and announced to the frontend via SignalR (`DevicePaired` broadcast).

### Usage

```powershell
.\add-devices.ps1 [-Count <int>] [-BaseUrl <string>]
```

| Parameter  | Default                  | Description                      |
|------------|--------------------------|----------------------------------|
| `-Count`   | `5`                      | Number of devices to create      |
| `-BaseUrl` | `http://localhost:5911`  | Server base URL                  |

### Examples

```powershell
# Create 5 devices on the default server
.\add-devices.ps1

# Create 10 devices
.\add-devices.ps1 -Count 10

# Create 3 devices on a remote server
.\add-devices.ps1 -Count 3 -BaseUrl http://192.168.1.50:5911
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

- **Number of devices**: pass a different `-Count` value.
- **Device names**: names are auto-generated server-side as `"Simulated Grid Device XXXX"` (random 4-digit number). To change the naming scheme, edit `SimulationController.cs` in `Main\Testing\Simulation\`, specifically the `AddDevice` method's `name` variable.
- **Initial status**: devices start as `ON_TASK`. To change this, modify the `DeviceStatus` enum value in the `AddDevice` method of `SimulationController.cs`.

---

## 2. `submit-responses.ps1` — Submit Responses to Questions

Submits one or more student responses to a specific question from a specific device. This calls `POST /api/v1/responses` as defined in API Contract §2.3.

### Usage

```powershell
.\submit-responses.ps1 -DeviceId <guid> -QuestionId <guid> [-Answer <string>] [-BaseUrl <string>]
                       [-ResponseCount <int>] [-IsCorrect <bool>] [-ResponseId <guid>] [-Timestamp <string>]
```

| Parameter        | Required | Default                            | Description                           |
|------------------|----------|------------------------------------|---------------------------------------|
| `-DeviceId`      | Yes      | —                                  | UUID of a paired device               |
| `-QuestionId`    | Yes      | —                                  | UUID of an existing question          |
| `-Answer`        | No       | `"Sample answer from device"`      | The answer string (see below)         |
| `-BaseUrl`       | No       | `http://localhost:5911`            | Server base URL                       |
| `-ResponseCount` | No       | `1`                                | Number of responses to submit (each gets a unique ID) |
| `-IsCorrect`     | No       | Not sent                           | `$true` or `$false` — optional correctness evaluation |
| `-ResponseId`    | No       | Auto-generated                     | Override the response UUID            |
| `-Timestamp`     | No       | Current UTC time                   | Override the ISO 8601 timestamp       |

### Answer format

The `-Answer` parameter is interpreted based on the question type:

| Question Type      | Answer Format                                   | Example   |
|--------------------|--------------------------------------------------|-----------|
| `WRITTEN_ANSWER`   | Any text string                                  | `"The answer is photosynthesis"` |
| `MULTIPLE_CHOICE`  | Zero-based integer index of the selected option  | `"2"` (selects the third option) |

### Examples

```powershell
# Submit a written answer
.\submit-responses.ps1 `
  -DeviceId "a1b2c3d4-e5f6-7890-abcd-ef1234567890" `
  -QuestionId "11111111-2222-3333-4444-555555555555" `
  -Answer "The mitochondria is the powerhouse of the cell"

# Submit a multiple-choice answer (option index 1)
.\submit-responses.ps1 `
  -DeviceId "a1b2c3d4-e5f6-7890-abcd-ef1234567890" `
  -QuestionId "11111111-2222-3333-4444-555555555555" `
  -Answer "1"

# Submit 5 responses with different UUIDs
.\submit-responses.ps1 `
  -DeviceId "a1b2c3d4-e5f6-7890-abcd-ef1234567890" `
  -QuestionId "11111111-2222-3333-4444-555555555555" `
  -Answer "My answer" `
  -ResponseCount 5

# Submit with correctness flag and custom timestamp
.\submit-responses.ps1 `
  -DeviceId "a1b2c3d4-e5f6-7890-abcd-ef1234567890" `
  -QuestionId "11111111-2222-3333-4444-555555555555" `
  -Answer "0" `
  -IsCorrect $true `
  -Timestamp "2026-03-06T12:00:00Z"
```

### Modifying response generation

- **Device**: change the `-DeviceId` parameter. Obtain valid device IDs from `add-devices.ps1` output.
- **Question**: change the `-QuestionId` parameter. You must use the UUID of a question that already exists on the server (create questions through the teacher UI).
- **Answer content**: change the `-Answer` parameter. For `MULTIPLE_CHOICE` questions, use a valid zero-based index (e.g. `"0"`, `"1"`). For `WRITTEN_ANSWER` questions, use any text string.
- **Batch submission**: set `-ResponseCount N` to submit multiple responses at once. Each gets a unique auto-generated UUID and timestamp.
- **Correctness flag**: set `-IsCorrect $true` or `-IsCorrect $false` to include the optional `IsCorrect` field (per Validation Rules §2C(2)(a)). If not set, the field is omitted.
- **Custom IDs/timestamps**: set `-ResponseId` and/or `-Timestamp` to override auto-generation. When using `-ResponseCount` greater than 1, `-ResponseId` is ignored to avoid duplicate IDs.

---

## Typical Workflow

A common end-to-end testing workflow:

```powershell
# 1. Start the Manuscripta server

# 2. Navigate to the scripts directory
cd windows\ManuscriptaTeacherApp\Main\Testing\scripts

# 3. Create some dummy devices
.\add-devices.ps1 -Count 5
# Note the device IDs from the output

# 4. Create questions through the teacher UI
# Note the question IDs (visible in the UI or via SignalR)

# 5. Submit responses from the dummy devices
.\submit-responses.ps1 -DeviceId "<device-id>" -QuestionId "<question-id>" -Answer "My answer"

# 6. Check the responses page in the teacher UI
```

---

## Existing Simulation Endpoints

The scripts call into the following server-side endpoints defined in `SimulationController.cs` (`Main\Testing\Simulation\`):

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
