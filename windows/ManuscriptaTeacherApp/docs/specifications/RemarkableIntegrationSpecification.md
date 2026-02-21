# reMarkable Integration Specification (Windows)

## Explanatory Note

This document provides specifications regarding the integration of the application with reMarkable devices. 

## Section 1 - General Principles

(1) The teacher application shall provide means through which a teacher may connect to reMarkable devices, subject to the following assumptions:

    (a) Each reMarkable device shall be linked to its own account; and

    (b) The teacher shall have access to the account credentials of the reMarkable device(s) they wish to connect to.

(2) Materials shall be deployed to, and managed through, the reMarkable cloud of the linked account of each device. This shall be done by converting the material to a PDF file in the manner specified in Material Conversion Specification.

(3) reMarkable devices shall be displayed in a similar manner to Android devices, except that the teacher shall be clearly indicated of the nature of the device, and the fact that no lesson control capabilities are available there.

(4) Communication with the reMarkable cloud shall be done through `rmapi` through the application available at `https://github.com/ddvk/rmapi`.

## Section 2 - Ascertaining the Availability of `rmapi`

(1) Prior to any operation requiring `rmapi`, the application shall verify that `rmapi` is available and functional in accordance with the Backend Runtime Dependency Management Specification.

    [Explanatory Note: This includes operations such as pairing a reMarkable device, uploading materials to the reMarkable cloud, or retrieving documents therefrom.]

(1A) For the purposes of the Backend Runtime Dependency Management Specification, the unique identifier for the `rmapi` dependency shall be `"rmapi"`.

(2) [DELETED. See Backend Runtime Dependency Management Specification §2(2)(a).]

(3) [DELETED. See Backend Runtime Dependency Management Specification §2(2)(a), which provides for session-scoped caching of availability check results.]

(4) [DELETED. See Frontend Workflow Specifications §3A(2)-(3).]

(5) [DELETED. See Frontend Workflow Specifications §3A(2)(d).]

(6) [DELETED. See Frontend Workflow Specifications §3A(4).]


## Section 2A - rmapi Runtime Dependency Manager

(1) The `RmapiRuntimeDependencyManager` class shall extend the `RuntimeDependencyManagerBase` abstract class specified in the Backend Runtime Dependency Management Specification §2.

(2) For the purposes of §2(2A) of that Specification —

    (a) `DownloadDependencyAsync()` shall download the latest Windows x64 release of `rmapi` from `https://github.com/ddvk/rmapi/releases`;

    (b) `VerifyDownloadAsync()` shall verify the downloaded file using SHA256 hash comparison against the published checksums; and

    (c) `InstallDependencyAsync()` shall extract or copy the executable to `%AppData%\ManuscriptaTeacherApp\bin\rmapi.exe`.

(3) The `GetDependencyServiceAsync()` method shall return an instance of `IRmapiService`, which shall provide methods for interacting with the reMarkable cloud via `rmapi`.

(4) For the purposes of §2(2)(a) of that Specification, the availability check shall verify that —

    (a) the `rmapi` executable exists at the path specified in paragraph (2)(c); and

    (b) invoking `rmapi version` returns successfully.


## Section 3 - Pairing reMarkable Devices

(1) Prior to pairing a reMarkable device, the application shall perform the availability check specified in §2, and handle unavailability in accordance with the Backend Runtime Dependency Management Specification §3.

(2) To pair a reMarkable device, the application shall —

    (a) collect a user-friendly device name;

    (b) authenticate with the reMarkable cloud using a one-time code obtained from `https://my.remarkable.com/device/desktop/connect`;

    (c) invoke `rmapi` with the `RMAPI_CONFIG` environment variable set to `%AppData%\ManuscriptaTeacherApp\rmapi\{DeviceId}.conf`, where `{DeviceId}` is the UUID of the device entity;

    (d) upon successful authentication, persist a `ReMarkableDeviceEntity`.

(3) When the application discovers that a previously paired reMarkable device's authentication has become invalid, it shall prompt the user to re-authenticate.

    [Explanatory Note: This may occur when the user revokes access from the reMarkable cloud, or when the authentication token expires.]

(4) To unpair a reMarkable device, the application shall —

    (a) delete the `ReMarkableDeviceEntity` from the database; and

    (b) delete the corresponding configuration file at `%AppData%\ManuscriptaTeacherApp\rmapi\{DeviceId}.conf`.


## Section 4 - Material Deployment to reMarkable Devices

(1) Prior to deploying a material to a reMarkable device, the application shall perform the availability check specified in §2, and handle unavailability in accordance with the Backend Runtime Dependency Management Specification §3.

(2) To deploy a material to a reMarkable device, the application shall —

    (a) generate a PDF of the material in accordance with Material Conversion Specification;

    (b) name the PDF file using the material title only (e.g., `Introduction to Algebra.pdf`);

    (c) invoke `rmapi` with the `RMAPI_CONFIG` environment variable set to the device's configuration file path, using the `put` command to upload the PDF to the `/Manuscripta` folder on the reMarkable cloud.

(3) If a file with the same name already exists in the `/Manuscripta` folder, the application shall rename the new file with a numerical suffix [e.g., `Introduction to Algebra (1).pdf`].

(4) The application shall not await acknowledgement from the reMarkable device, as the reMarkable cloud operates asynchronously.

    [Explanatory Note: Unlike Android devices which receive materials via direct TCP connection, reMarkable devices sync with their cloud periodically. The device will receive the material on its next sync.]

(5) If upload fails due to authentication errors, the application shall trigger the re-authentication workflow specified in §3(3).

(6) If upload fails due to other errors [network, rmapi failure], the application shall display an error message and allow the user to retry.

