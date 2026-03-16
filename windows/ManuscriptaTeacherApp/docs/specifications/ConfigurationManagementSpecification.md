# Configuration Management Specification (Windows)

## Explanatory Note

This document specifies how configurations of Android devices are to be managed in the Windows Application.

This document is applicable only to Android devices. Non-Android devices do not have configurations.


## Section 1 - General Principles

(1) In this specification,

    (a) a "configuration value" refers to a setting that dictates the behaviour of the Android application on an Android device.

    (b) the "configuration" of an Android device refers to the set of configuration values associated with the Android application thereon.

    (c) the "base configuration" refers to the configuration assumed by all Android devices in the absence of overrides.

    (d) a specific Android device is said to have "overridden" the base configuration if its configuration differs from the base configuration. The list of differences is known as its "override".

    (e) a "device" refers to an Android device, unless otherwise specified.

(2) A configuration shall be represented with a `ConfigurationEntity` object, as specified in the Section 2G of the Validation Rules.

(3) The Windows application shall provide —

    (a) means to modify the base configuration of all devices when no devices are paired, and long-term persist its values as defined in Persistence and Cascading Rules; and

    (b) means to override the base configuration, as specified in paragraph (a) above, for a specific Android device, and short-term persist the override.

(4) The Windows application shall initialise the base configuration to the values provided in Appendix 1.

(5) The base configuration values shall be displayed modifiably under the "Settings" tab, as outlined in S7 of Frontend Workflow Specifications.

(6) The Windows application shall not allow the base configuration to be modified when one or more devices is paired.


## Section 2 - Override of Configuration Values

(1) The Windows application shall provide a mechanism to override the base configuration for a specific device.

(2) The mechanism must —

    (a) ensure that each device's override stores only configuration values that deviate from those in the base configuration, rather than all configuration values of that device; and

    (b) be able to compile a data transfer object outlining the configuration for each device, conformant to Section 2G of the Validation Rules, for the purpose of fulfilling a request made to the `GET /config/{deviceId}` endpoint, defined under Section 2.2 of the API Contract. This data transfer object must contain 

        (i) all overridden configuration values as specified by the device's override; and
        (ii) all non-overriden configuration values as obtained from the base configuration.
    

(3) The user may override base configuration values for a specific Android device using the device grid in the "Classroom" tab, as outlined in S5B(5) and S5H in Frontend Workflow Specifications.


## Section 3 — Initiation of Config Refresh

(1) Config refresh requests shall be initiated on the following occasions:

    (a) When a device is first paired, request a config refresh to that device.

    (b) [Deleted.]

    (c) When an override value is set or changed, request a config refresh to the device(s) the change corresponds to.

(2) The Windows application shall initiate a config refresh request by sending a TCP `CONFIG_REFRESH` message, as specified in Section 6(3) of the Session Interaction Specifications.

(3) The Service layer shall provide mechanisms to determine the trigger of a event specified in subsection (1), and the Frontend Workflow Specifications shall provide mechanisms for the backend to report timeouts and other errors regarding config refreshes.


## Appendix 1 - Initialisation of base configuration

"TextSize": 6,
"FeedbackStyle": "NEUTRAL",
"TtsEnabled": false,
"AiScaffoldingEnabled": false,
"SummarisationEnabled": false,
"MascotSelection": "NONE"


## Appendix 2 - Temporary disapplication of configuration values

For the purposes of this version of the application, the configuration values "TtsEnabled", "AiScaffoldingEnabled" and "SummarisationEnabled" shall not made visible or configurable from the frontend.
