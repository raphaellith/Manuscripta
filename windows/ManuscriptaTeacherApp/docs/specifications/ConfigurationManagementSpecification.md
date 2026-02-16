# Configuration Management Specification (Windows)

## Explanatory Note

This document specifies how configurations are to be managed in the Windows Application.

[Explanatory Note: The provisions defining the obligations of Frontend Workflow Specifications should be replaced with appropriate cross-references when corresponding specifications are written there, and when SignalR endpoints are defined.]


## Section 1 - General Principles

(1) In this specification, "configuration" refers to a list of settings, to be set by the user, that dictates the behaviour of the Android Application.

(2) A configuration shall be represented with a `ConfigurationEntity` object, as specified in the Section 2G of the Validation Rules.

(3) The Windows application shall provide —

    (a) means to set the default values of all devices, and long-term persist these values as defined in Persistence and Cascading Rules; and

    (b) means to override the default values, as specified in paragraph (a) above, for a specific device, and such configurations shall be short-term persisted.

(4) The Windows application shall provide a reasonable set of default configuration values, in case the user does not provide any, as provided in Appendix 1.

(5) The default configuration values shall be set under the "Settings" tab, and the Frontend Workflow Specifications shall define the operation of that tab.


## Section 2 - Override of Configuration Values

(1) The Windows application shall provide a mechanism to override the default configuration values for a specific device.

(2) The mechanism must —

    (a) store only overrides that deviate from the default values, rather than all configuration values of that device; and

    (b) be able to compile a data transfer object, conformant to Section 2G of the Validation Rules, containing —

        (i) the default configuration values, if not overriden; and
        (ii) the overriden configuration values, otherwise,
    
    for the purpose of fulfilling a request made to the `GET /config/{deviceId}` endpoint, defined under Section 2.2 of the API Contract.

(3) The Frontend Workflow Specifications shall define a manner through which the user can override default configuration values for a specific device, or a specific list of devices, using the device grid in the "Classroom" tab.


## Section 3 — Initiation of Config Refresh

(1) Config refresh requests shall be initiated on the following occasions:

    (a) When a device is first paired, request a config refresh to that device.

    (b) When a default value is set or changed, request a config refresh to all devices.

    (c) When an override value is set or changed, request a config refresh to the device(s) the change corresponds to.

(2) The Windows application shall intiate a config refresh request by sending a TCP `CONFIG_REFRESH` (0x07) message, as specified in Section 6(3) of the Session Interaction Specifications.

(3) The Service layer shall provide mechanisms to determine the trigger of a event specified in subsection (1), and the Frontend Workflow Specifications shall provide mechanisms for the backend to report timeouts and other errors regarding config refreshes.

## Appendix 1 - Default Configuration Values

"TextSize": 12,
"FeedbackStyle": "IMMEDIATE",
"TtsEnabled": true,
"AiScaffoldingEnabled": true,
"SummarisationEnabled": true,
"MascotSelection": "MASCOT1"