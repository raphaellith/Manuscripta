using System;
using System.Collections.Generic;
using System.Linq;
using System.Threading.Tasks;
using Moq;
using Xunit;
using Main.Models.Entities.Materials;
using Main.Models.Entities.Sessions;
using Main.Models.Enums;
using Main.Services;
using Main.Services.Repositories;

namespace MainTests.ServicesTests;

public class SessionServiceTests
{
    private readonly Mock<ISessionRepository> _mockSessionRepo;
    private readonly Mock<IMaterialRepository> _mockMaterialRepo;
    private readonly Mock<IDeviceRegistryService> _mockDeviceRegistry;
    private readonly DeviceIdValidator _deviceIdValidator;
    private readonly SessionService _service;

    public SessionServiceTests()
    {
        _mockSessionRepo = new Mock<ISessionRepository>();
        _mockMaterialRepo = new Mock<IMaterialRepository>();
        _mockDeviceRegistry = new Mock<IDeviceRegistryService>();
        
        // Default: all devices are considered paired/valid
        _mockDeviceRegistry.Setup(r => r.IsDevicePairedAsync(It.IsAny<Guid>()))
            .ReturnsAsync(true);
        
        _deviceIdValidator = new DeviceIdValidator(_mockDeviceRegistry.Object);
        _service = new SessionService(_mockSessionRepo.Object, _mockMaterialRepo.Object, _deviceIdValidator);
    }

    #region CreateSessionAsync Tests

    [Fact]
    public async Task CreateSessionAsync_ValidActiveSession_Success()
    {
        // Arrange - §2(3)(a)
        var materialId = Guid.NewGuid();
        var material = new WorksheetMaterialEntity(materialId, "Test Material", "Content");
        var session = new SessionEntity(
            Guid.NewGuid(),
            materialId,
            DateTime.UtcNow,
            SessionStatus.ACTIVE,
            Guid.NewGuid()
        );

        _mockMaterialRepo.Setup(r => r.GetByIdAsync(materialId))
            .ReturnsAsync(material);
        _mockSessionRepo.Setup(r => r.AddAsync(It.IsAny<SessionEntity>()))
            .Returns(Task.CompletedTask);

        // Act
        var result = await _service.CreateSessionAsync(session);

        // Assert
        Assert.NotNull(result);
        Assert.Equal(session.Id, result.Id);
        _mockSessionRepo.Verify(r => r.AddAsync(session), Times.Once);
    }

    [Fact]
    public async Task CreateSessionAsync_ValidCompletedSessionWithEndTime_Success()
    {
        // Arrange - §2D(3)(a) compliant
        var materialId = Guid.NewGuid();
        var material = new WorksheetMaterialEntity(materialId, "Test Material", "Content");
        var session = new SessionEntity(
            Guid.NewGuid(),
            materialId,
            DateTime.UtcNow.AddHours(-1),
            SessionStatus.COMPLETED,
            Guid.NewGuid(),
            DateTime.UtcNow
        );

        _mockMaterialRepo.Setup(r => r.GetByIdAsync(materialId))
            .ReturnsAsync(material);
        _mockSessionRepo.Setup(r => r.AddAsync(It.IsAny<SessionEntity>()))
            .Returns(Task.CompletedTask);

        // Act
        var result = await _service.CreateSessionAsync(session);

        // Assert
        Assert.NotNull(result);
        Assert.Equal(SessionStatus.COMPLETED, result.SessionStatus);
        Assert.NotNull(result.EndTime);
    }

    [Fact]
    public async Task CreateSessionAsync_NullSession_ThrowsArgumentNullException()
    {
        // Act & Assert
        await Assert.ThrowsAsync<ArgumentNullException>(
            () => _service.CreateSessionAsync(null!));
    }

    [Fact]
    public async Task CreateSessionAsync_NonExistingMaterial_ThrowsInvalidOperationException()
    {
        // Arrange
        var materialId = Guid.NewGuid();
        var session = new SessionEntity(
            Guid.NewGuid(),
            materialId,
            DateTime.UtcNow,
            SessionStatus.ACTIVE,
            Guid.NewGuid()
        );

        _mockMaterialRepo.Setup(r => r.GetByIdAsync(materialId))
            .ReturnsAsync((MaterialEntity?)null);

        // Act & Assert
        var exception = await Assert.ThrowsAsync<InvalidOperationException>(
            () => _service.CreateSessionAsync(session));
        Assert.Contains("not found", exception.Message);
    }

    [Theory]
    [InlineData(SessionStatus.PAUSED)]
    [InlineData(SessionStatus.COMPLETED)]
    [InlineData(SessionStatus.CANCELLED)]
    public async Task CreateSessionAsync_NonActiveSessionWithoutEndTime_ThrowsInvalidOperationException(SessionStatus status)
    {
        // Arrange - §2D(3)(a): Non-active sessions must have EndTime
        var materialId = Guid.NewGuid();
        var material = new WorksheetMaterialEntity(materialId, "Test Material", "Content");
        var session = new SessionEntity(
            Guid.NewGuid(),
            materialId,
            DateTime.UtcNow,
            status,
            Guid.NewGuid(),
            null // No end time
        );

        _mockMaterialRepo.Setup(r => r.GetByIdAsync(materialId))
            .ReturnsAsync(material);

        // Act & Assert
        var exception = await Assert.ThrowsAsync<InvalidOperationException>(
            () => _service.CreateSessionAsync(session));
        Assert.Contains("EndTime", exception.Message);
        Assert.Contains("§2D(3)(a)", exception.Message);
    }

    #endregion

    #region ActivateSessionAsync Tests

    [Fact]
    public async Task ActivateSessionAsync_ExistingSession_SetsStatusToActive()
    {
        // Arrange - §2(3)(b)
        var sessionId = Guid.NewGuid();
        var session = new SessionEntity(
            sessionId,
            Guid.NewGuid(),
            DateTime.UtcNow.AddHours(-1),
            SessionStatus.PAUSED,
            Guid.NewGuid(),
            DateTime.UtcNow
        );

        _mockSessionRepo.Setup(r => r.GetByIdAsync(sessionId))
            .ReturnsAsync(session);
        _mockSessionRepo.Setup(r => r.UpdateAsync(It.IsAny<SessionEntity>()))
            .Returns(Task.CompletedTask);

        // Act
        var result = await _service.ActivateSessionAsync(sessionId);

        // Assert
        Assert.Equal(SessionStatus.ACTIVE, result.SessionStatus);
        Assert.Null(result.EndTime);
        _mockSessionRepo.Verify(r => r.UpdateAsync(It.Is<SessionEntity>(s => 
            s.SessionStatus == SessionStatus.ACTIVE && s.EndTime == null)), Times.Once);
    }

    [Fact]
    public async Task ActivateSessionAsync_NonExistingSession_ThrowsInvalidOperationException()
    {
        // Arrange
        var sessionId = Guid.NewGuid();
        _mockSessionRepo.Setup(r => r.GetByIdAsync(sessionId))
            .ReturnsAsync((SessionEntity?)null);

        // Act & Assert
        var exception = await Assert.ThrowsAsync<InvalidOperationException>(
            () => _service.ActivateSessionAsync(sessionId));
        Assert.Contains("not found", exception.Message);
    }

    #endregion

    #region PauseSessionAsync Tests

    [Fact]
    public async Task PauseSessionAsync_ExistingSession_SetsStatusToPausedWithEndTime()
    {
        // Arrange - §2(3)(b)
        var sessionId = Guid.NewGuid();
        var session = new SessionEntity(
            sessionId,
            Guid.NewGuid(),
            DateTime.UtcNow.AddHours(-1),
            SessionStatus.ACTIVE,
            Guid.NewGuid()
        );

        _mockSessionRepo.Setup(r => r.GetByIdAsync(sessionId))
            .ReturnsAsync(session);
        _mockSessionRepo.Setup(r => r.UpdateAsync(It.IsAny<SessionEntity>()))
            .Returns(Task.CompletedTask);

        // Act
        var result = await _service.PauseSessionAsync(sessionId);

        // Assert
        Assert.Equal(SessionStatus.PAUSED, result.SessionStatus);
        Assert.NotNull(result.EndTime);
        _mockSessionRepo.Verify(r => r.UpdateAsync(It.Is<SessionEntity>(s => 
            s.SessionStatus == SessionStatus.PAUSED && s.EndTime != null)), Times.Once);
    }

    [Fact]
    public async Task PauseSessionAsync_NonExistingSession_ThrowsInvalidOperationException()
    {
        // Arrange
        var sessionId = Guid.NewGuid();
        _mockSessionRepo.Setup(r => r.GetByIdAsync(sessionId))
            .ReturnsAsync((SessionEntity?)null);

        // Act & Assert
        var exception = await Assert.ThrowsAsync<InvalidOperationException>(
            () => _service.PauseSessionAsync(sessionId));
        Assert.Contains("not found", exception.Message);
    }

    #endregion

    #region CompleteSessionAsync Tests

    [Fact]
    public async Task CompleteSessionAsync_ExistingSession_SetsStatusToCompletedWithEndTime()
    {
        // Arrange - §2(3)(b)
        var sessionId = Guid.NewGuid();
        var session = new SessionEntity(
            sessionId,
            Guid.NewGuid(),
            DateTime.UtcNow.AddHours(-1),
            SessionStatus.ACTIVE,
            Guid.NewGuid()
        );

        _mockSessionRepo.Setup(r => r.GetByIdAsync(sessionId))
            .ReturnsAsync(session);
        _mockSessionRepo.Setup(r => r.UpdateAsync(It.IsAny<SessionEntity>()))
            .Returns(Task.CompletedTask);

        // Act
        var result = await _service.CompleteSessionAsync(sessionId);

        // Assert
        Assert.Equal(SessionStatus.COMPLETED, result.SessionStatus);
        Assert.NotNull(result.EndTime);
        _mockSessionRepo.Verify(r => r.UpdateAsync(It.Is<SessionEntity>(s => 
            s.SessionStatus == SessionStatus.COMPLETED && s.EndTime != null)), Times.Once);
    }

    [Fact]
    public async Task CompleteSessionAsync_NonExistingSession_ThrowsInvalidOperationException()
    {
        // Arrange
        var sessionId = Guid.NewGuid();
        _mockSessionRepo.Setup(r => r.GetByIdAsync(sessionId))
            .ReturnsAsync((SessionEntity?)null);

        // Act & Assert
        var exception = await Assert.ThrowsAsync<InvalidOperationException>(
            () => _service.CompleteSessionAsync(sessionId));
        Assert.Contains("not found", exception.Message);
    }

    #endregion

    #region CancelSessionAsync Tests

    [Fact]
    public async Task CancelSessionAsync_ExistingSession_SetsStatusToCancelledWithEndTime()
    {
        // Arrange - §2(3)(b)
        var sessionId = Guid.NewGuid();
        var session = new SessionEntity(
            sessionId,
            Guid.NewGuid(),
            DateTime.UtcNow.AddHours(-1),
            SessionStatus.ACTIVE,
            Guid.NewGuid()
        );

        _mockSessionRepo.Setup(r => r.GetByIdAsync(sessionId))
            .ReturnsAsync(session);
        _mockSessionRepo.Setup(r => r.UpdateAsync(It.IsAny<SessionEntity>()))
            .Returns(Task.CompletedTask);

        // Act
        var result = await _service.CancelSessionAsync(sessionId);

        // Assert
        Assert.Equal(SessionStatus.CANCELLED, result.SessionStatus);
        Assert.NotNull(result.EndTime);
        _mockSessionRepo.Verify(r => r.UpdateAsync(It.Is<SessionEntity>(s => 
            s.SessionStatus == SessionStatus.CANCELLED && s.EndTime != null)), Times.Once);
    }

    [Fact]
    public async Task CancelSessionAsync_NonExistingSession_ThrowsInvalidOperationException()
    {
        // Arrange
        var sessionId = Guid.NewGuid();
        _mockSessionRepo.Setup(r => r.GetByIdAsync(sessionId))
            .ReturnsAsync((SessionEntity?)null);

        // Act & Assert
        var exception = await Assert.ThrowsAsync<InvalidOperationException>(
            () => _service.CancelSessionAsync(sessionId));
        Assert.Contains("not found", exception.Message);
    }

    #endregion

    #region GetAllSessionsAsync Tests

    [Fact]
    public async Task GetAllSessionsAsync_ReturnsSessions()
    {
        // Arrange - §2(3)(c)
        var sessions = new List<SessionEntity>
        {
            new SessionEntity(Guid.NewGuid(), Guid.NewGuid(), DateTime.UtcNow, SessionStatus.ACTIVE, Guid.NewGuid()),
            new SessionEntity(Guid.NewGuid(), Guid.NewGuid(), DateTime.UtcNow.AddHours(-1), SessionStatus.COMPLETED, Guid.NewGuid(), DateTime.UtcNow)
        };

        _mockSessionRepo.Setup(r => r.GetAllAsync())
            .ReturnsAsync(sessions);

        // Act
        var result = await _service.GetAllSessionsAsync();

        // Assert
        Assert.Equal(2, result.Count());
    }

    [Fact]
    public async Task GetAllSessionsAsync_EmptyRepository_ReturnsEmptyCollection()
    {
        // Arrange
        _mockSessionRepo.Setup(r => r.GetAllAsync())
            .ReturnsAsync(new List<SessionEntity>());

        // Act
        var result = await _service.GetAllSessionsAsync();

        // Assert
        Assert.Empty(result);
    }

    #endregion

    #region GetSessionByIdAsync Tests

    [Fact]
    public async Task GetSessionByIdAsync_ExistingSession_ReturnsSession()
    {
        // Arrange - §2(3)(d)
        var sessionId = Guid.NewGuid();
        var session = new SessionEntity(
            sessionId,
            Guid.NewGuid(),
            DateTime.UtcNow,
            SessionStatus.ACTIVE,
            Guid.NewGuid()
        );

        _mockSessionRepo.Setup(r => r.GetByIdAsync(sessionId))
            .ReturnsAsync(session);

        // Act
        var result = await _service.GetSessionByIdAsync(sessionId);

        // Assert
        Assert.NotNull(result);
        Assert.Equal(sessionId, result!.Id);
    }

    [Fact]
    public async Task GetSessionByIdAsync_NonExistingSession_ReturnsNull()
    {
        // Arrange
        var sessionId = Guid.NewGuid();
        _mockSessionRepo.Setup(r => r.GetByIdAsync(sessionId))
            .ReturnsAsync((SessionEntity?)null);

        // Act
        var result = await _service.GetSessionByIdAsync(sessionId);

        // Assert
        Assert.Null(result);
    }

    #endregion

    #region DeleteSessionAsync Tests

    [Fact]
    public async Task DeleteSessionAsync_CallsRepositoryDelete()
    {
        // Arrange - §2(3)(e)
        var sessionId = Guid.NewGuid();
        _mockSessionRepo.Setup(r => r.DeleteAsync(sessionId))
            .Returns(Task.CompletedTask);

        // Act
        await _service.DeleteSessionAsync(sessionId);

        // Assert
        _mockSessionRepo.Verify(r => r.DeleteAsync(sessionId), Times.Once);
    }

    #endregion

    #region Constructor Tests

    [Fact]
    public void Constructor_NullSessionRepository_ThrowsArgumentNullException()
    {
        // Act & Assert
        Assert.Throws<ArgumentNullException>(() => 
            new SessionService(null!, _mockMaterialRepo.Object, _deviceIdValidator));
    }

    [Fact]
    public void Constructor_NullMaterialRepository_ThrowsArgumentNullException()
    {
        // Act & Assert
        Assert.Throws<ArgumentNullException>(() => 
            new SessionService(_mockSessionRepo.Object, null!, _deviceIdValidator));
    }

    [Fact]
    public void Constructor_NullDeviceIdValidator_ThrowsArgumentNullException()
    {
        // Act & Assert
        Assert.Throws<ArgumentNullException>(() => 
            new SessionService(_mockSessionRepo.Object, _mockMaterialRepo.Object, null!));
    }

    #endregion
}
