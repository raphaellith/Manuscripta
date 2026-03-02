using Microsoft.Extensions.Logging;
using Main.Models.Entities;
using MailKit.Net.Smtp;
using MailKit.Security;
using MimeKit;

namespace Main.Services;

/// <summary>
/// Implementation of IEmailService using MailKit.
/// Used to dispatch PDF attachments to Kindle or other external email endpoints.
/// </summary>
public class SmtpEmailService : IEmailService
{
    private readonly ILogger<SmtpEmailService> _logger;
    private readonly Func<ISmtpClient> _clientFactory;

    public SmtpEmailService(ILogger<SmtpEmailService> logger, Func<ISmtpClient>? clientFactory = null)
    {
        _logger = logger ?? throw new ArgumentNullException(nameof(logger));
        _clientFactory = clientFactory ?? (() => new SmtpClient());
    }

    /// <inheritdoc/>
    public async Task TestConnectionAsync(EmailCredentialEntity credentials)
    {
        using var client = _clientFactory();
        try
        {
            // Connect to server (SecureSocketOptions.Auto uses STARTTLS or SSL based on port automatically)
            await client.ConnectAsync(credentials.SmtpHost, credentials.SmtpPort, SecureSocketOptions.Auto);

            // Authenticate
            await client.AuthenticateAsync(credentials.EmailAddress, credentials.Password);

            _logger.LogInformation("SMTP connection test successful for {EmailAddress}", credentials.EmailAddress);
        }
        catch (Exception ex)
        {
            _logger.LogWarning(ex, "SMTP connection test failed for {EmailAddress}", credentials.EmailAddress);
            throw new InvalidOperationException($"Failed to connect or authenticate to the SMTP server: {ex.Message}", ex);
        }
        finally
        {
            if (client.IsConnected)
                await client.DisconnectAsync(true);
        }
    }

    /// <inheritdoc/>
    public async Task SendEmailWithAttachmentAsync(
        EmailCredentialEntity credentials,
        string recipientEmail,
        string subject,
        string body,
        byte[] attachmentBytes,
        string attachmentFileName)
    {
        var message = new MimeMessage();
        message.From.Add(new MailboxAddress("Manuscripta Teacher App", credentials.EmailAddress));
        message.To.Add(new MailboxAddress("External Device", recipientEmail));
        message.Subject = subject;

        // Build email body
        var builder = new BodyBuilder
        {
            TextBody = body
        };

        // Attach PDF
        builder.Attachments.Add(attachmentFileName, attachmentBytes, ContentType.Parse("application/pdf"));

        message.Body = builder.ToMessageBody();

        using var client = _clientFactory();
        try
        {
            await client.ConnectAsync(credentials.SmtpHost, credentials.SmtpPort, SecureSocketOptions.Auto);
            await client.AuthenticateAsync(credentials.EmailAddress, credentials.Password);
            
            await client.SendAsync(message);
            
            _logger.LogInformation("Successfully sent email with attachment {FileName} to {Recipient}", attachmentFileName, recipientEmail);
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Failed to send email to {Recipient}", recipientEmail);
            throw new InvalidOperationException($"Failed to dispatch email: {ex.Message}", ex);
        }
        finally
        {
            if (client.IsConnected)
                await client.DisconnectAsync(true);
        }
    }
}
