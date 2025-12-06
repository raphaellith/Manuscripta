using Microsoft.EntityFrameworkCore;
using Microsoft.Extensions.Logging;

using Main.Data;
using Main.Models.Entities;

namespace Main.Services;

/// <summary>
/// Implementation of <see cref="IDeviceRegistryService"/> using Entity Framework Core.
/// Manages paired Android devices per Pairing Process.md §1(1)(a) and §2(4).
/// </summary>
public class DeviceRegistryService : IDeviceRegistryService
{
    private readonly MainDbContext _context;
    private readonly ILogger<DeviceRegistryService> _logger;

    public DeviceRegistryService(MainDbContext context, ILogger<DeviceRegistryService> logger)
    {
        _context = context ?? throw new ArgumentNullException(nameof(context));
        _logger = logger ?? throw new ArgumentNullException(nameof(logger));
    }

    /// <inheritdoc />
    public async Task<bool> RegisterDeviceAsync(Guid deviceId)
    {
        var existingDevice = await _context.PairedDevices
            .FirstOrDefaultAsync(d => d.DeviceId == deviceId);

        if (existingDevice != null)
        {
            _logger.LogInformation("Device {DeviceId} re-paired (already existed)", deviceId);
            return false;
        }

        // New device - create pairing record per Pairing Process §2(4)
        // Wrap in try-catch to handle race conditions where another request
        // registers the same device between our check and save
        var newDevice = new PairedDeviceEntity(deviceId);
        try
        {
            _context.PairedDevices.Add(newDevice);
            await _context.SaveChangesAsync();

            _logger.LogInformation("Device {DeviceId} paired successfully", deviceId);
            return true;
        }
        catch (DbUpdateException ex)
        {
            // Likely a duplicate due to race condition - DeviceId is primary key
            _logger.LogWarning(ex, "Device {DeviceId} registration failed due to duplicate (race condition)", deviceId);
            return false;
        }
    }

    /// <inheritdoc />
    public async Task<bool> IsDevicePairedAsync(Guid deviceId)
    {
        return await _context.PairedDevices
            .AnyAsync(d => d.DeviceId == deviceId);
    }
}
