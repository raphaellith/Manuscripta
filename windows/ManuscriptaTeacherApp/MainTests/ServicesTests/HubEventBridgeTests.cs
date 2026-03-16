using Microsoft.Extensions.DependencyInjection;
using Microsoft.Extensions.Logging;
using Microsoft.AspNetCore.SignalR;
using Moq;
using Main.Services;
using Main.Services.Network;
using Main.Services.Hubs;
using Main.Services.Repositories;
using Main.Models.Events;
using Main.Models.Entities;
using Main.Models.Enums;
using Xunit;

namespace MainTests.ServicesTests;

public class HubEventBridgeTests
{
    private readonly Mock<ITcpPairingService> _mockTcpService;
    private readonly Mock<IDeviceRegistryService> _mockRegistryService;
    private readonly Mock<IHubContext<TeacherPortalHub>> _mockHubContext;
    private readonly Mock<IHubClients> _mockClients;
    private readonly Mock<IClientProxy> _mockClientProxy;
    private readonly Mock<IServiceProvider> _mockServiceProvider;
    private readonly Mock<ILogger<HubEventBridge>> _mockLogger;
    private readonly HubEventBridge _hubEventBridge;

    public HubEventBridgeTests()
    {
        _mockTcpService = new Mock<ITcpPairingService>();
        _mockRegistryService = new Mock<IDeviceRegistryService>();
        
        // Setup Hub Context Mocks
        _mockHubContext = new Mock<IHubContext<TeacherPortalHub>>();
        _mockClients = new Mock<IHubClients>();
        _mockClientProxy = new Mock<IClientProxy>();
        
        _mockHubContext.Setup(h => h.Clients).Returns(_mockClients.Object);
        _mockClients.Setup(c => c.All).Returns(_mockClientProxy.Object);

        _mockServiceProvider = new Mock<IServiceProvider>();
        _mockLogger = new Mock<ILogger<HubEventBridge>>();

        _hubEventBridge = new HubEventBridge(
            _mockTcpService.Object,
            _mockRegistryService.Object,
            _mockHubContext.Object,
            _mockServiceProvider.Object,
            _mockLogger.Object);
    }

    [Fact]
    public async Task StartAsync_SubscribesToEvents()
    {
        // Act
        await _hubEventBridge.StartAsync(CancellationToken.None);

        // Assert - Events are hard to assert subscription on Moq, 
        // but we can test that raising the event triggers the client proxy
        
        // Trigger StatusUpdateReceived
        var eventArgs = new DeviceStatusEventArgs("device1", "ON_TASK", 80, "mat1", "view1", 123456);
        _mockTcpService.Raise(s => s.StatusUpdateReceived += null, _mockTcpService.Object, eventArgs);

        _mockClientProxy.Verify(
            c => c.SendCoreAsync(
                "UpdateDeviceStatus",
                It.Is<object[]>(args => 
                    args.Length == 1 && 
                    GetProperty(args[0], "deviceId") == "device1" &&
                    GetProperty(args[0], "status") == "ON_TASK"
                ),
                It.IsAny<CancellationToken>()),
            Times.Once);
    }

    [Fact]
    public async Task HandRaisedReceived_InvokesClientMethod()
    {
        await _hubEventBridge.StartAsync(CancellationToken.None);
        var deviceId = Guid.NewGuid();

        _mockTcpService.Raise(s => s.HandRaisedReceived += null, _mockTcpService.Object, deviceId);

        _mockClientProxy.Verify(
            c => c.SendCoreAsync(
                "HandRaised",
                It.Is<object[]>(args => args.Length == 1 && (string)args[0] == deviceId.ToString()),
                It.IsAny<CancellationToken>()),
            Times.Once);
    }

    [Fact]
    public async Task DistributionTimedOut_InvokesClientMethod()
    {
        await _hubEventBridge.StartAsync(CancellationToken.None);
        var deviceId = Guid.NewGuid();
        var materialId = Guid.NewGuid();
        var eventArgs = new EntityDeliveryFailedEventArgs(deviceId, materialId);

        _mockTcpService.Raise(s => s.DistributionTimedOut += null, _mockTcpService.Object, eventArgs);

        _mockClientProxy.Verify(
            c => c.SendCoreAsync(
                "DistributionFailed",
                It.Is<object[]>(args => 
                    args.Length == 1 && 
                    GetProperty(args[0], "deviceId") == deviceId.ToString() &&
                    GetProperty(args[0], "materialId") == materialId.ToString()),
                It.IsAny<CancellationToken>()),
            Times.Once);
    }

    [Fact]
    public async Task FeedbackDeliveryTimedOut_InvokesClientMethodWithFeedbackId()
    {
        // Arrange
        await _hubEventBridge.StartAsync(CancellationToken.None);
        var deviceId = Guid.NewGuid();
        var feedbackId = Guid.NewGuid();
        var eventArgs = new EntityDeliveryFailedEventArgs(deviceId, feedbackId);

        // Act
        _mockTcpService.Raise(s => s.FeedbackDeliveryTimedOut += null, _mockTcpService.Object, eventArgs);

        // Assert - Per NetworkingAPISpec §2(1)(d)(v): includes deviceId and feedbackId
        _mockClientProxy.Verify(
            c => c.SendCoreAsync(
                "FeedbackDeliveryFailed",
                It.Is<object[]>(args => 
                    args.Length == 1 && 
                    GetProperty(args[0], "deviceId") == deviceId.ToString() &&
                    GetProperty(args[0], "feedbackId") == feedbackId.ToString()),
                It.IsAny<CancellationToken>()),
            Times.Once);
    }

    [Fact]
    public async Task DevicePaired_InvokesClientMethod()
    {
        await _hubEventBridge.StartAsync(CancellationToken.None);
        var device = new PairedDeviceEntity(Guid.NewGuid(), "Device 1");

        _mockRegistryService.Raise(s => s.DevicePaired += null, _mockRegistryService.Object, device);

        _mockClientProxy.Verify(
            c => c.SendCoreAsync(
                "DevicePaired",
                It.Is<object[]>(args => args.Length == 1 && args[0] == device),
                It.IsAny<CancellationToken>()),
            Times.Once);
    }

    [Fact]
    public async Task StopAsync_UnsubscribesFromEvents()
    {
        // Arrange
        await _hubEventBridge.StartAsync(CancellationToken.None);
        await _hubEventBridge.StopAsync(CancellationToken.None);
        
        // Act - raise event
        var deviceId = Guid.NewGuid();
        _mockTcpService.Raise(s => s.HandRaisedReceived += null, _mockTcpService.Object, deviceId);

        // Assert - should NOT be called again
        _mockClientProxy.Verify(
            c => c.SendCoreAsync(
                "HandRaised",
                It.IsAny<object[]>(),
                It.IsAny<CancellationToken>()),
            Times.Never);
    }

    [Fact]
    public async Task FeedbackAckReceived_WithReadyFeedback_InvokesRefreshResponses()
    {
        // Arrange — mock the IServiceScope chain for IFeedbackRepository resolution
        var feedbackId = Guid.NewGuid();
        var deviceId = Guid.NewGuid();
        var feedback = new FeedbackEntity
        {
            Id = feedbackId,
            ResponseId = Guid.NewGuid(),
            Text = "Good work",
            Status = FeedbackStatus.READY
        };

        var mockFeedbackRepo = new Mock<IFeedbackRepository>();
        mockFeedbackRepo.Setup(r => r.GetByIdAsync(feedbackId)).ReturnsAsync(feedback);

        var mockScopeServiceProvider = new Mock<IServiceProvider>();
        mockScopeServiceProvider
            .Setup(sp => sp.GetService(typeof(IFeedbackRepository)))
            .Returns(mockFeedbackRepo.Object);

        var mockScope = new Mock<IServiceScope>();
        mockScope.Setup(s => s.ServiceProvider).Returns(mockScopeServiceProvider.Object);

        var mockScopeFactory = new Mock<IServiceScopeFactory>();
        mockScopeFactory.Setup(f => f.CreateScope()).Returns(mockScope.Object);

        _mockServiceProvider
            .Setup(sp => sp.GetService(typeof(IServiceScopeFactory)))
            .Returns(mockScopeFactory.Object);

        await _hubEventBridge.StartAsync(CancellationToken.None);
        var eventArgs = new FeedbackAckEventArgs(deviceId, feedbackId);

        // Act
        _mockTcpService.Raise(s => s.FeedbackAckReceived += null, _mockTcpService.Object, eventArgs);

        // Allow FireAndForget to complete
        await Task.Delay(200);

        // Assert — Per GenAISpec §3DA(3)(c): RefreshResponses must be invoked
        _mockClientProxy.Verify(
            c => c.SendCoreAsync(
                "RefreshResponses",
                It.Is<object[]>(args => args.Length == 0 || args.All(a => a == null)),
                It.IsAny<CancellationToken>()),
            Times.Once);
    }

    // Helper to get anonymous type property
    private static string? GetProperty(object obj, string name)
    {
        return obj.GetType().GetProperty(name)?.GetValue(obj)?.ToString();
    }
}
