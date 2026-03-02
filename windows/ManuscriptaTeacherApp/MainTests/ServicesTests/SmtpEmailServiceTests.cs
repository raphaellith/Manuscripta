using System;
using System.Linq;
using System.Threading;
using System.Threading.Tasks;
using Main.Models.Entities;
using Main.Services;
using MailKit;
using MailKit.Net.Smtp;
using MailKit.Security;
using Microsoft.Extensions.Logging;
using MimeKit;
using Moq;
using Xunit;

namespace MainTests.ServicesTests;

public class SmtpEmailServiceTests
{
    private readonly Mock<ILogger<SmtpEmailService>> _mockLogger;
    private readonly Mock<ISmtpClient> _mockSmtpClient;
    private readonly SmtpEmailService _service;

    public SmtpEmailServiceTests()
    {
        _mockLogger = new Mock<ILogger<SmtpEmailService>>();
        _mockSmtpClient = new Mock<ISmtpClient>();

        // Set up mock client basic connection state
        _mockSmtpClient.Setup(c => c.IsConnected).Returns(true);

        _service = new SmtpEmailService(_mockLogger.Object, () => _mockSmtpClient.Object);
    }

    [Fact]
    public async Task TestConnectionAsync_Successful_ConnectsAndAuthenticates()
    {
        var creds = new EmailCredentialEntity(Guid.NewGuid(), "test@test.com", "smtp.test.com", 587, "pw");

        await _service.TestConnectionAsync(creds);

        _mockSmtpClient.Verify(c => c.ConnectAsync("smtp.test.com", 587, SecureSocketOptions.Auto, default(CancellationToken)), Times.Once);
        _mockSmtpClient.Verify(c => c.AuthenticateAsync("test@test.com", "pw", default(CancellationToken)), Times.Once);
        _mockSmtpClient.Verify(c => c.DisconnectAsync(true, default(CancellationToken)), Times.Once);
        _mockSmtpClient.Verify(c => c.Dispose(), Times.Once);
    }

    [Fact]
    public async Task TestConnectionAsync_Fails_ThrowsInvalidOperationException()
    {
        var creds = new EmailCredentialEntity(Guid.NewGuid(), "test@test.com", "smtp.test.com", 587, "pw");

        _mockSmtpClient.Setup(c => c.ConnectAsync(It.IsAny<string>(), It.IsAny<int>(), It.IsAny<SecureSocketOptions>(), default(CancellationToken)))
            .ThrowsAsync(new Exception("Network error"));

        await Assert.ThrowsAsync<InvalidOperationException>(() => _service.TestConnectionAsync(creds));
    }

    [Fact]
    public async Task SendEmailWithAttachmentAsync_SendsMimeMessageWithAttachment()
    {
        var creds = new EmailCredentialEntity(Guid.NewGuid(), "test@test.com", "smtp.test.com", 587, "pw");
        var pdfBytes = new byte[] { 0x25, 0x50, 0x44, 0x46 }; // %PDF

        MimeMessage? sentMessage = null;
        _mockSmtpClient.Setup(c => c.SendAsync(It.IsAny<MimeMessage>(), default(CancellationToken), default(ITransferProgress)))
            .Callback<MimeMessage, CancellationToken, ITransferProgress>((m, ct, tp) => sentMessage = m)
            .ReturnsAsync("OK");

        await _service.SendEmailWithAttachmentAsync(
            creds,
            "reader@kindle.com",
            "Test Subject",
            "This is the body",
            pdfBytes,
            "Attachment.pdf"
        );

        _mockSmtpClient.Verify(c => c.ConnectAsync("smtp.test.com", 587, SecureSocketOptions.Auto, default(CancellationToken)), Times.Once);
        _mockSmtpClient.Verify(c => c.AuthenticateAsync("test@test.com", "pw", default(CancellationToken)), Times.Once);
        _mockSmtpClient.Verify(c => c.SendAsync(It.IsAny<MimeMessage>(), default(CancellationToken), default(ITransferProgress)), Times.Once);

        Assert.NotNull(sentMessage);
        Assert.Equal("Test Subject", sentMessage!.Subject);
        Assert.Equal("test@test.com", sentMessage.From.Mailboxes.First().Address);
        Assert.Equal("reader@kindle.com", sentMessage.To.Mailboxes.First().Address);

        // Verify attachment
        var multipart = sentMessage.Body as Multipart;
        Assert.NotNull(multipart);
        var attachment = multipart.OfType<MimePart>().FirstOrDefault(p => p.FileName == "Attachment.pdf");
        Assert.NotNull(attachment);
        Assert.Equal("application/pdf", attachment.ContentType.MimeType);
    }
}
