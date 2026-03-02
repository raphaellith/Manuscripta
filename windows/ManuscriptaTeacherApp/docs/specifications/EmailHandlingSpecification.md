# Email Handling Specification (Windows)

## Explanatory Note

This document specifies how emails shall be sent from, and received by, the teacher application. 

At the current stage, users shall provide their own email credentials to the application. This is a compromise due to the lack of a dedicated email server for the application, and may be improved upon if/when funding is received for a domain and email server.

## Section 1 - General Principles

(1) The Windows Application shall provide means for sending and receiving emails, for the purposes of integration with external platforms (e.g. Kindle devices).

(2) The user shall provide their own email credentials to the application, and shall be advised by the frontend to use a dedicated email address for this purpose. [Explanatory Note: This is a compromise decision and shall be changed to a dedicated Manuscripta email address when possible.]

(3) Email operations shall be implemented using the MailKit library. [Explanatory Note: MailKit is a cross-platform .NET library providing full SMTP and IMAP support, MIT-licensed.]


## Section 2 - Management of Email Credentials

(1) Email credentials shall be stored as an Email Credential object, as defined in the Additional Validation Rules. Only one such object shall be stored at any time.

(2) The Email Credential entity shall contain the fields defined in Additional Validation Rules Section 3E.

(3) The password (or app-specific password) shall be encrypted at rest using the Windows Data Protection API (DPAPI) before persistence, and decrypted only when used for SMTP or IMAP connections.

    [Explanatory Note: DPAPI ties the encryption to the current Windows user account. This avoids the need for a user-managed master password whilst providing protection against direct database inspection.]

(3A) The use of DPAPI may be substituted for another method for the purpose of development and testing on a platform other than Windows, but the exemption under this subsection shall not apply to any production deployment under Windows devices.

(4) When the user submits new or updated email credentials —

    (a) the backend shall invoke the connection test method defined in Section 3(1)(b) using the provided credentials;

    (b) if the connection succeeds, the credentials shall be persisted (or updated), replacing any previously stored credentials; and

    (c) if the connection fails, the backend shall return an appropriate error and the credentials shall not be persisted.

(5) The user shall be able to delete the stored email credentials. Upon deletion, the `EmailCredentialEntity` shall be removed from the database.


## Section 2A - Email Credential Availability

(1) The application shall provide an endpoint to check whether valid email credentials have been configured. 

(2) Prior to initiating any operation entirely dependent on email capabilities, the frontend shall verify the availability of email credentials.

(3) If email credentials are not available, the frontend shall advise the user to configure them in the Settings interface, and the dependent operation shall not proceed.


## Section 3 - Sending Emails

(1) The application shall provide an `IEmailService` interface in the Service layer, exposing at minimum the following methods —

    (a) `Task SendEmailAsync(string recipientAddress, string subject, string body, byte[] attachmentContent, string attachmentFileName)`: Sends an email to the specified recipient with an optional file attachment using the persisted system email credentials.

    (b) `Task<bool> TestConnectionAsync(EmailCredentialEntity credentials)`: Attempts to establish and authenticate an SMTP connection to the specified host and port using the provided (unpersisted) credentials. Returns true if successful, throwing an exception otherwise.

(2) Prior to sending an email via Paragraph (1)(a), the service shall —

    (a) retrieve the stored email credentials;

    (b) if no credentials are stored, throw an appropriate exception indicating that email credentials have not been configured; and

    (c) decrypt the stored password using DPAPI.

(3) The email shall be sent via SMTP using the host, port, and credentials stored in the Email Credential entity. The connection shall use TLS/SSL as determined by the port and server capabilities.

(4) The sender address of the email shall be the `EmailAddress` field of the stored Email Credential entity.


## Section 4 - Receiving Emails

(1) [DEFERRED]

    [Explanatory Note: This section is reserved for future specification of IMAP-based email retrieval. Receiving emails is not currently required by any integration feature.]