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

## Section 3: Distributing a Material

(1) The Windows client should initiate a session by sending a TCP `DISTRIBUTE_MATERIAL` message (opcode `0x05`), as specified in `API Contract.md` §3.4, with the Material ID as operand, to the android device(s) which are the intended receipients of a certain material, to indicate that they should make a GET request to the appropriate endpoint(s) to obtain their associated materials. 

(2) The Android client must, on receipt of the message specified in (1), call the appropriate REST API end point as specified in `API Contract.md` §2.5, in order to retrieve the lesson materials that it should display.

(3) The Windows client must provide an REST API end point which:

    (i) Takes a DeviceId as an input.
    (ii) Return, if any, material entities and their related questions.

As specified in `API Contract.md` §2.5.

## Section 4: Submitting a Response

(1) The Android client may submit a response to a question by using `POST /responses` and `POST /responses/batch` in API specifications.

(2) The Android client may evaluate the correctness of the response if it is straightforward to do so (such as for multiple choice, true / false or simple blank filling questions), and place the result of evaluation in the `IsCorrect` field. 

## Section 5: Lifetime of a Session

(?) This Section is to be drafted after the exact concept, use and state transitions of a session have been ascertained in a group meeting.