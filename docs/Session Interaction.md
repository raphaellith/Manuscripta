# Session Interaction Specification

This document specifies the process through which the two clients communicate during a session. 

## Section 1: General Principles

(1) In this document —

    (a) "Session" references a "session entity", as defined in s2D of the Validation Rules. It records an association between a device and a material.
    (b) "Pair" and "unpair" have the same meanings in Pairing Process Specification.
    (c) "Heart rate" means every 3 seconds.

(2) Devices shall be deemed as Paired, as defined under Pairing Process Specification, throughout this document.

(3) This document may be specified as "Session Interaction Specification".

## Section 2: Heartbeat Mechanism

(1) The Android device must, as long as deemed paired, send a TCP status update message according to the "heart rate" specified in s1(1), to the Windows device, to indicate its current status.

(2) The Windows device should implicitly signal acceptance through TCP acknowledgements. Malformed packets shall be dealt accordingly in the API Contract.

(3) If a Windows device has not received a heartbeat message from a paired Android device A for 10 seconds, it shall change A's status to `DISCONNECTED`, and inform the user accordingly.

(4) If a heart beat has not been acknowledged by the Windows device, the Android device should still send heart beats at the heart rate, and should notify the user that they have been disconnected if 3 consecutive messages are left unacknowledged when the next message is scheduled to be sent. If no acknowledgement has been received within 60 seconds, the Android client should deem itself as unpaired, and use the process in Pairing Process Specification s3.

## Section 3: Distributing Materials

(1) The Windows client distributes materials by sending a TCP `DISTRIBUTE_MATERIAL` message (opcode `0x05`), as specified in `API Contract.md` §3.4, to the Android device(s) which are the intended recipients.

(2) The Android client must, on receipt of the message specified in (1), call `GET /distribution/{deviceId}` as specified in `API Contract.md` §2.5, to retrieve the distribution bundle (materials and questions).

(3) For each material received in the distribution bundle, the Android client creates a separate `SessionEntity` in the `RECEIVED` state, associating that device with that material.

(4) The Windows client must provide a REST API endpoint which:

    (i) Takes a DeviceId as an input.
    (ii) Returns, if any, material entities and their related questions.

As specified in `API Contract.md` §2.5.

## Section 4: Submitting a Response

(1) The Android client may submit a response to a question by using `POST /responses` and `POST /responses/batch` in API specifications.

(2) The Android client may evaluate the correctness of the response if it is straightforward to do so (such as for multiple choice, true / false or simple blank filling questions), and place the result of evaluation in the `IsCorrect` field. 

## Section 5: Lifetime of a Session

(1) **Session States**

A session entity has one of the following `SessionStatus` values:

    (a) `RECEIVED`: Material has been received; student has not yet interacted.
    (b) `ACTIVE`: Student is actively engaged with the material.
    (c) `PAUSED`: Student has taken a break or switched to a different material.
    (d) `COMPLETED`: Session completed normally (student submitted work).
    (e) `CANCELLED`: Session terminated due to unpair or explicit cancellation.

(2) **Session Creation**

When the Android client receives materials via `GET /distribution/{deviceId}`, it creates a session entity in the `RECEIVED` state for each material. The `StartTime` field is not set at creation.

(3) **Session Activation**

A session transitions from `RECEIVED` to `ACTIVE` when the student first interacts with the material. At this point, `StartTime` is set to the current timestamp.

(4) **Valid State Transitions**

The following state transitions are permitted:

    (a) `RECEIVED` → `ACTIVE`: First student interaction (Android, automatic).
    (b) `RECEIVED` → `CANCELLED`: Device unpairs or teacher cancels.
    (c) `ACTIVE` → `PAUSED`: Student requests break or switches to another material (Android).
    (d) `ACTIVE` → `COMPLETED`: Student submits work (Android, automatic).
    (e) `ACTIVE` → `CANCELLED`: Device unpairs or teacher cancels.
    (f) `PAUSED` → `ACTIVE`: Student resumes (Android, student-initiated).
    (g) `PAUSED` → `COMPLETED`: Student submits work (Android, automatic).
    (h) `PAUSED` → `CANCELLED`: Device unpairs or teacher cancels.

(5) **Terminal States**

`COMPLETED` and `CANCELLED` are terminal states. A session in either state cannot transition to any other state.

(6) **EndTime Requirement**

Per `Validation Rules.md` §2D(3)(a), any transition to `PAUSED`, `COMPLETED`, or `CANCELLED` must set the `EndTime` field to the current timestamp.

(7) **Automatic Cancellation**

A session shall be automatically transitioned to `CANCELLED` if the device is deemed unpaired as defined in Section 2(4) of this document.