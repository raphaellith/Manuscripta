using System.Runtime.InteropServices;
using System.Security.Cryptography;
using System.Text;
using Microsoft.EntityFrameworkCore;
using Main.Data;
using Main.Models.Entities;

namespace Main.Services.Repositories;

/// <summary>
/// EF Core implementation of IEmailCredentialRepository.
/// Uses Windows DPAPI to encrypt and decrypt the SMTP password at rest.
/// </summary>
public class EfEmailCredentialRepository : IEmailCredentialRepository
{
    private readonly MainDbContext _context;

    // Optional entropy for DPAPI
    private static readonly byte[] Entropy = Encoding.UTF8.GetBytes("ManuscriptaTeacherApp-Email");

    private static readonly byte[] FallbackKey = SHA256.HashData(Encoding.UTF8.GetBytes("Manuscripta-Fallback-Key"));

    public EfEmailCredentialRepository(MainDbContext context)
    {
        _context = context ?? throw new ArgumentNullException(nameof(context));
    }

    private byte[] Protect(byte[] data)
    {
        if (RuntimeInformation.IsOSPlatform(OSPlatform.Windows))
        {
            return ProtectedData.Protect(data, Entropy, DataProtectionScope.CurrentUser);
        }
        
        // Cross-platform fallback (e.g. macOS dev)
        using var aes = Aes.Create();
        aes.Key = FallbackKey;
        aes.GenerateIV();
        using var encryptor = aes.CreateEncryptor();
        var cipher = encryptor.TransformFinalBlock(data, 0, data.Length);
        var result = new byte[aes.IV.Length + cipher.Length];
        Buffer.BlockCopy(aes.IV, 0, result, 0, aes.IV.Length);
        Buffer.BlockCopy(cipher, 0, result, aes.IV.Length, cipher.Length);
        return result;
    }

    private byte[] Unprotect(byte[] protectedData)
    {
        if (RuntimeInformation.IsOSPlatform(OSPlatform.Windows))
        {
            return ProtectedData.Unprotect(protectedData, Entropy, DataProtectionScope.CurrentUser);
        }

        // Cross-platform fallback
        using var aes = Aes.Create();
        aes.Key = FallbackKey;
        var iv = new byte[aes.BlockSize / 8];
        Buffer.BlockCopy(protectedData, 0, iv, 0, iv.Length);
        aes.IV = iv;
        using var decryptor = aes.CreateDecryptor();
        return decryptor.TransformFinalBlock(protectedData, iv.Length, protectedData.Length - iv.Length);
    }

    public async Task<EmailCredentialEntity?> GetCredentialsAsync()
    {
        var entity = await _context.EmailCredentials.FirstOrDefaultAsync();
        if (entity == null)
            return null;

        try
        {
            // Decrypt password
            var encryptedBytes = Convert.FromBase64String(entity.Password);
            var decryptedBytes = Unprotect(encryptedBytes);
            var decryptedPassword = Encoding.UTF8.GetString(decryptedBytes);

            // Return a clone with the decrypted password so we don't accidentally track and save the plaintext
            return new EmailCredentialEntity(
                entity.Id,
                entity.EmailAddress,
                entity.SmtpHost,
                entity.SmtpPort,
                decryptedPassword
            );
        }
        catch (CryptographicException)
        {
            // If DPAPI/AES fails, treat as invalid
            return null;
        }
    }

    public async Task SaveCredentialsAsync(EmailCredentialEntity entity)
    {
        // Encrypt password
        var plaintextBytes = Encoding.UTF8.GetBytes(entity.Password);
        var encryptedBytes = Protect(plaintextBytes);
        var encryptedPassword = Convert.ToBase64String(encryptedBytes);

        var existing = await _context.EmailCredentials.FirstOrDefaultAsync();
        if (existing != null)
        {
            existing.EmailAddress = entity.EmailAddress;
            existing.SmtpHost = entity.SmtpHost;
            existing.SmtpPort = entity.SmtpPort;
            existing.Password = encryptedPassword;
        }
        else
        {
            var newEntity = new EmailCredentialEntity(
                Guid.NewGuid(), // Enforce a single row but still use a UUID PK
                entity.EmailAddress,
                entity.SmtpHost,
                entity.SmtpPort,
                encryptedPassword
            );
            _context.EmailCredentials.Add(newEntity);
        }

        await _context.SaveChangesAsync();
    }

    public async Task DeleteCredentialsAsync()
    {
        var existing = await _context.EmailCredentials.FirstOrDefaultAsync();
        if (existing != null)
        {
            _context.EmailCredentials.Remove(existing);
            await _context.SaveChangesAsync();
        }
    }
}
