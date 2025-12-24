# Pairing Process Specification

This document defines the pairing process between the Windows device and the Android devices.

## Section 1: General Principles

(1) In this document, "paired" means —

    (a) The Windows device has recorded the Android device's deviceId.
    (b) The Android device has recorded the Windows device's IP address, HTTP port, and TCP port.
    (c) Successful bi-directional message interchange using both TCP and HTTP has been proven, as specified in Section 2.

(2) The pairing phase must ensure that:

    (a) The Android devices successfully discover the Windows device.
    (b) The Windows device successfully records paired Android devices.
    (c) Successful bi-directional message interchange using both TCP and HTTP REST APIs have been proven successful.

(3) Requests received from unpaired Android devices should be rejected. 

(4) Pairing phases, as defined in Section 2, must be conducted sequentially. If any pairing phases were deemed unsuccessful, the pairing process must start over.

(5) This document has authority over the general API contract. In the case of any conflicts or contradiction to that contract, that contract should be amended to reflect specifications in this document. 

(6) This document may be cited as "Pairing Process Specification".

## Section 2: Pairing Phases

(1) The Windows device should initiate a pairing process by broadcasting a UDP discovery message, as specified in `API Contract.md` §1.1, on the UDP port specified therein. This message must, as a minimum, include the following information:
    (a) The IP address of the Windows Device.
    (b) The HTTP port to be used.
    (c) The TCP port to be used.

(2) The Android device, on discovery of a message as specified in (1), must:
    (a) Extract the IP address and ports in that message.
    (b) Establish a TCP connection to the Windows device and send a `PAIRING_REQUEST` message (opcode `0x20`), as specified in `API Contract.md` §3.5. This message must contain the deviceId that the Android device generates.
    (c) Make a POST request to the `/pair` endpoint, as specified in `API Contract.md` §2.4, with a body containing the deviceId.

(3) The Windows device should respond to the messages defined in (2) to signal correct message transmission in that channel:
    (a) On receipt of a message specified in (2)(b), respond with a `PAIRING_ACK` message (opcode `0x21`), as specified in `API Contract.md` §3.5, which indicates that message transmission through TCP was successful.
    (b) Respond to the POST request, specified in (2)(c), with a `201 Created` response, as specified in `API Contract.md` §2.4, which indicates that message transmission was successful.

(4) The Windows device should, on completion of (1)-(3) above, for all purposes, deem that Android device as paired, such that requests from that Android device would now be accepted. The Windows device must store the deviceId received during pairing.

(5) The Android device should, on receipt of both messages specified in (3)(a) and (3)(b), deem its pairing with the Windows device as successful. The Android device must store the Windows device's IP address, HTTP port, and TCP port.

## Section 3: Unpairing

(1) In this section, "unpair" means treating a previously "paired" device, as defined in s1(1), to be no longer paired, such that any new requests would be rejected under s1(3).

(2) The Windows Client may unpair an Android client based on explicit user instruction. Once this instruction has been received, it should send a TCP `UNPAIR` message (opcode `0x04`), as specified in `API Contract.md` §3.4, to the corresponding Android client, signalling that it has been unpaired and it should no longer send messages to, or expect to receive messages from, the Windows client. It should also remove the Android client from its registry.

(3) Upon receipt of the `UNPAIR` message specified in (2), the Android device must deem itself as unpaired by clearing the stored Windows device's IP address, HTTP port, and TCP port, and treat the pairing as terminated.

## Appendix 1 - Maintaining this document (Instructions to AI Agents)

(1) An AI agent, when instructed to review or edit this document, shall —
    (a) Cross-reference `API Contract.md`, and replace any implementation details that are in contradiction to this document.
    (b) Where required, fill in detailed message structures of messages in that document, and insert appropriate cross-references in this document. "(?)" in this document shall mean that the implementation details shall be provided.

(2) The AI agent shall seek for clarification from the human rather than making assumptions, if it deems any clauses in the documents involved unclear.