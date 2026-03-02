using System.ComponentModel.DataAnnotations;
using System.Text.Json.Serialization;

namespace Main.Models.Entities;

/// <summary>
/// Represents the SMTP credentials required to dispatch emails.
/// Per AdditionalValidationRules §3E.
/// </summary>
public class EmailCredentialEntity
{
    [Key]
    public Guid Id { get; set; }

    public string EmailAddress { get; set; }
    public string SmtpHost { get; set; }
    public int SmtpPort { get; set; }

    /// <summary>
    /// The encrypted password or app-specific password.
    /// Decryption/Encryption MUST be handled via DPAPI before saving or using.
    /// </summary>
    [JsonInclude]
    public string Password { get; internal set; }

    public EmailCredentialEntity() { }

    public EmailCredentialEntity(Guid id, string emailAddress, string smtpHost, int smtpPort, string password)
    {
        Id = id;
        EmailAddress = emailAddress;
        SmtpHost = smtpHost;
        SmtpPort = smtpPort;
        Password = password;
    }
}
