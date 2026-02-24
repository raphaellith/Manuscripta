using Main.Models.Entities;

namespace Main.Services.Repositories;

/// <summary>
/// Defines the repository interface for email credentials.
/// Handles encryption and decryption of passwords so upper layers get plaintext.
/// </summary>
public interface IEmailCredentialRepository
{
    Task<EmailCredentialEntity?> GetCredentialsAsync();
    Task SaveCredentialsAsync(EmailCredentialEntity entity);
    Task DeleteCredentialsAsync();
}
