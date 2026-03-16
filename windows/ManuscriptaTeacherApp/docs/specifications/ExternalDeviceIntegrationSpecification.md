# External Device Integration Specification (Windows)

## Explanatory Note

This document specifies the integration of the application with "external devices" — third-party hardware or platforms (e.g. reMarkable, Kindle) that cannot run the native student application and operate asynchronously.

## Section 1 - General Principles

(1) The teacher application shall provide means through which a teacher may connect to supported external devices, subject to the assumption that each external device is linked to an external account accessible by the teacher or student.

(2) External devices differ from standard Android student tablets in that:
    (a) materials are deployed asynchronously through an external cloud or email service;
    (b) classroom control capabilities (e.g. locking, unlocking, live response collection) are not available; and
    (c) communication does not occur over direct TCP connections.

(3) External devices shall be displayed in the device grid alongside standard devices, but the teacher shall be clearly indicated of the nature of the device (e.g. via an icon or badge) and the fact that classroom control capabilities are unavailable.

(4) Materials shall be deployed to external devices by converting the material to a PDF file in the manner specified in the Material Conversion Specification, and dispatched using the device's designated delivery mechanism (e.g. rmapi, Email).

## Section 2 - Supported Device Types and Delivery Mechanisms

(1) The application shall support the following external device types, defined by the `ExternalDeviceType` enum:
    (a) `REMARKABLE`: Delivered via the reMarkable cloud using `rmapi` (`https://github.com/ddvk/rmapi`).
    (b) `KINDLE`: Delivered via Amazon's Personal Documents service using the "Send to Kindle" email address.

(2) Prior to any operation involving an external device (pairing or deployment), the application shall verify that the prerequisite capability for that device type is available in the following manner:
    (a) For devices of type `REMARKABLE`, the application shall verify that `rmapi` is available in accordance with the Backend Runtime Dependency Management Specification.
    (b) For devices of type `KINDLE`, the application shall verify that Email Credentials are functional in accordance with the Email Handling Specification.

## Section 2A - rmapi Runtime Dependency Manager

(1) The `RmapiRuntimeDependencyManager` class shall extend the `RuntimeDependencyManagerBase` abstract class specified in the Backend Runtime Dependency Management Specification Section 2.

(2) For the purposes of Section 2(2A) of that Specification —

    (a) `DownloadDependencyAsync()` shall download version 0.0.32 Windows x64 release of `rmapi` from `https://github.com/ddvk/rmapi/releases`;

    (b) `VerifyDownloadAsync()` shall verify the downloaded file using SHA256 hash comparison against the published checksums; and

    (c) `PerformInstallDependencyAsync()` shall extract or copy the executable to `%AppData%\ManuscriptaTeacherApp\bin\rmapi.exe`.

(3) The `GetDependencyServiceAsync()` method shall return an instance of `IRmapiService`, which shall provide methods for interacting with the reMarkable cloud via `rmapi`.

(4) For the purposes of Section 2(2)(a) of that Specification, the availability check shall verify that —

    (a) the `rmapi` executable exists at the path specified in paragraph (2)(c); and

    (b) invoking `rmapi version` returns successfully.

## Section 3 - Pairing External Devices

(1) To pair an external device, the application shall collect a user-friendly device name and type-specific configuration data.

(2) When pairing an external device of type `REMARKABLE`, the application shall authenticate with the reMarkable cloud using a one-time code obtained from `https://my.remarkable.com/device/desktop/connect`. Upon successful authentication, the application shall invoke `rmapi` to generate a configuration file at `%AppData%\ManuscriptaTeacherApp\rmapi\{DeviceId}.conf`.

(3) When pairing an external device of type `KINDLE`, the application shall collect the unique "Send to Kindle" email prefix and append the fixed `@kindle.com` domain. The application shall advise the user to ensure the Manuscripta sender address is added to their "Approved Personal Document E-mail List" on Amazon.

(4) Upon fulfilling the type-specific requirements in Subsections (2) and (3), the application shall persist an `ExternalDeviceEntity` in accordance with Additional Validation Rules Section 3D.

(5) To unpair an external device, the application shall —
    (a) delete the `ExternalDeviceEntity` from the database.
    (b) delete the corresponding `.conf` file, if the device is of type `REMARKABLE`.

(6) If a previously paired device's authentication becomes invalid (e.g. an revoked reMarkable token or bouncing Kindle email), the application shall prompt the user to re-authenticate or verify their settings.

## Section 4 - Material Deployment to External Devices

(1) To deploy a material to an external device, the application shall —
    (a) generate a PDF of the material in accordance with the Material Conversion Specification, passing the target device's identifier so that the effective PDF settings are resolved with the per-device overrides defined in AdditionalValidationRules §3D(1)(e–g).
    (b) name the PDF file using the material title (e.g. `Introduction to Algebra.pdf`).

(2) When deploying to a device of type `REMARKABLE`, the application shall invoke `rmapi` using the device's configuration file to upload the PDF to the `/Manuscripta` folder on the reMarkable cloud. If a file with the same name exists, `rmapi` shall upload the new file with a numerical suffix.

(3) When deploying to a device of type `KINDLE`, the application shall invoke the `IEmailService` defined in the Email Handling Specification, sending the generated PDF as an attachment to the device's "Send to Kindle" email address.

(4) The application shall not await acknowledgement from the external device, as external cloud services operate asynchronously. 

(5) If dispatch fails due to capability errors (e.g. missing `rmapi`, missing email credentials), or transient network errors, the application shall display an error message and allow the user to retry.
