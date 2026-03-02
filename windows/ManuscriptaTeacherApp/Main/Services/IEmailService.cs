using Microsoft.Extensions.Logging;
using Main.Models.Entities;
using MailKit.Net.Smtp;
using MailKit.Security;
using MimeKit;

namespace Main.Services;

/// <summary>
/// Interface for validating and sending emails via SMTP.
/// Per EmailHandlingSpecification §2 and §3.
/// </summary>
public interface IEmailService
{
    /// <summary>
    /// Tests an SMTP connection using the provided credentials.
    /// Per EmailHandlingSpecification §3.
    /// Throws descriptive exceptions if connection/authentication fails.
    /// </summary>
    Task TestConnectionAsync(EmailCredentialEntity credentials);

    /// <summary>
    /// Sends an email with a single PDF attachment.
    /// Per EmailHandlingSpecification §3.
    /// </summary>
    Task SendEmailWithAttachmentAsync(
        EmailCredentialEntity credentials, 
        string recipientEmail, 
        string subject, 
        string body, 
        byte[] attachmentBytes, 
        string attachmentFileName);
}
