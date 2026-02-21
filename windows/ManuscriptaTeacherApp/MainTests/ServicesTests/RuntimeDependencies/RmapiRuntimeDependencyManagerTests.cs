using System;
using System.Threading.Tasks;
using Main.Models;
using Main.Services;
using Main.Services.RuntimeDependencies;
using Moq;
using Xunit;

namespace MainTests.ServicesTests.RuntimeDependencies;

public class RmapiRuntimeDependencyManagerTests
{
    private readonly Mock<IRmapiService> _mockRmapiService;
    private readonly RmapiRuntimeDependencyManager _manager;

    public RmapiRuntimeDependencyManagerTests()
    {
        _mockRmapiService = new Mock<IRmapiService>();
        _manager = new RmapiRuntimeDependencyManager(_mockRmapiService.Object);
    }

    [Fact]
    public void DependencyId_ReturnsRmapi()
    {
        Assert.Equal("rmapi", _manager.DependencyId);
    }

    [Fact]
    public async Task CheckDependencyAvailabilityAsync_CallsRmapiService()
    {
        _mockRmapiService.Setup(s => s.CheckAvailabilityAsync()).ReturnsAsync(true);
        var result = await _manager.CheckDependencyAvailabilityAsync();
        Assert.True(result);
        _mockRmapiService.Verify(s => s.CheckAvailabilityAsync(), Times.Once);
    }

    [Fact]
    public async Task InstallDependencyAsync_CallsRmapiServiceExtraction_WhenVerifying_And_Downloading()
    {
        var progress = new Mock<IProgress<RuntimeDependencyProgress>>();
        _mockRmapiService.Setup(s => s.CheckAvailabilityAsync()).ReturnsAsync(true);
        _mockRmapiService.Setup(s => s.DownloadRmapiAsync(progress.Object)).Returns(Task.CompletedTask);
        _mockRmapiService.Setup(s => s.ExtractAndInstallRmapiAsync(progress.Object)).Returns(Task.CompletedTask);

        var result = await _manager.InstallDependencyAsync(progress.Object);
        Assert.True(result);

        // Verify that the template method called the abstract parts
        _mockRmapiService.Verify(s => s.DownloadRmapiAsync(progress.Object), Times.Once);
        _mockRmapiService.Verify(s => s.ExtractAndInstallRmapiAsync(progress.Object), Times.Once);
        _mockRmapiService.Verify(s => s.CheckAvailabilityAsync(), Times.Once);
    }

    [Fact]
    public async Task UninstallDependencyAsync_CallsRmapiService()
    {
        _mockRmapiService.Setup(s => s.UninstallAsync()).ReturnsAsync(true);
        var result = await _manager.UninstallDependencyAsync();
        Assert.True(result);
        _mockRmapiService.Verify(s => s.UninstallAsync(), Times.Once);
    }

    [Fact]
    public async Task GetDependencyServiceAsync_ReturnsRmapiService()
    {
        var result = await _manager.GetDependencyServiceAsync();
        Assert.Same(_mockRmapiService.Object, result);
    }
}
