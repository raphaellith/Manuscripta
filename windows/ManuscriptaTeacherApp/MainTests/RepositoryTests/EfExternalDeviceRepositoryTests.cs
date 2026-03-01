using System;
using System.Linq;
using System.Threading.Tasks;
using Microsoft.Data.Sqlite;
using Microsoft.EntityFrameworkCore;
using Xunit;
using Main.Data;
using Main.Models.Entities;
using Main.Services.Repositories;

namespace MainTests.RepositoryTests;

/// <summary>
/// Tests for EfExternalDeviceRepository CRUD operations.
/// </summary>
public class EfExternalDeviceRepositoryTests
{
    private DbContextOptions<MainDbContext> CreateSqliteInMemoryOptions(SqliteConnection connection)
    {
        return new DbContextOptionsBuilder<MainDbContext>()
            .UseSqlite(connection)
            .Options;
    }

    [Fact]
    public async Task AddAndGetById_ReturnsAddedDevice()
    {
        using var connection = new SqliteConnection("DataSource=:memory:");
        connection.Open();
        var options = CreateSqliteInMemoryOptions(connection);

        var deviceId = Guid.NewGuid();

        using (var ctx = new MainDbContext(options))
        {
            ctx.Database.EnsureCreated();
            var repo = new EfExternalDeviceRepository(ctx);

            var device = new ExternalDeviceEntity(deviceId, "My Device", ExternalDeviceType.REMARKABLE);
            await repo.AddAsync(device);
        }

        using (var ctx = new MainDbContext(options))
        {
            var repo = new EfExternalDeviceRepository(ctx);
            var retrieved = await repo.GetByIdAsync(deviceId);

            Assert.NotNull(retrieved);
            Assert.Equal(deviceId, retrieved!.DeviceId);
            Assert.Equal("My Device", retrieved.Name);
            Assert.Equal(ExternalDeviceType.REMARKABLE, retrieved.Type);
        }
    }

    [Fact]
    public async Task GetAllAsync_ReturnsAllDevices()
    {
        using var connection = new SqliteConnection("DataSource=:memory:");
        connection.Open();
        var options = CreateSqliteInMemoryOptions(connection);

        using (var ctx = new MainDbContext(options))
        {
            ctx.Database.EnsureCreated();
            var repo = new EfExternalDeviceRepository(ctx);

            await repo.AddAsync(new ExternalDeviceEntity(Guid.NewGuid(), "Device A", ExternalDeviceType.REMARKABLE));
            await repo.AddAsync(new ExternalDeviceEntity(Guid.NewGuid(), "Device B", ExternalDeviceType.KINDLE));
            await repo.AddAsync(new ExternalDeviceEntity(Guid.NewGuid(), "Device C", ExternalDeviceType.REMARKABLE));
        }

        using (var ctx = new MainDbContext(options))
        {
            var repo = new EfExternalDeviceRepository(ctx);
            var all = (await repo.GetAllAsync()).ToList();
            Assert.Equal(3, all.Count);
        }
    }

    [Fact]
    public async Task UpdateAsync_UpdatesDeviceName()
    {
        using var connection = new SqliteConnection("DataSource=:memory:");
        connection.Open();
        var options = CreateSqliteInMemoryOptions(connection);

        var deviceId = Guid.NewGuid();

        using (var ctx = new MainDbContext(options))
        {
            ctx.Database.EnsureCreated();
            var repo = new EfExternalDeviceRepository(ctx);
            await repo.AddAsync(new ExternalDeviceEntity(deviceId, "Original Name", ExternalDeviceType.KINDLE));
        }

        using (var ctx = new MainDbContext(options))
        {
            var repo = new EfExternalDeviceRepository(ctx);
            await repo.UpdateAsync(new ExternalDeviceEntity(deviceId, "Updated Name", ExternalDeviceType.KINDLE));
        }

        using (var ctx = new MainDbContext(options))
        {
            var repo = new EfExternalDeviceRepository(ctx);
            var device = await repo.GetByIdAsync(deviceId);
            Assert.NotNull(device);
            Assert.Equal("Updated Name", device!.Name);
        }
    }

    [Fact]
    public async Task DeleteAsync_RemovesDevice()
    {
        using var connection = new SqliteConnection("DataSource=:memory:");
        connection.Open();
        var options = CreateSqliteInMemoryOptions(connection);

        var deviceId = Guid.NewGuid();

        using (var ctx = new MainDbContext(options))
        {
            ctx.Database.EnsureCreated();
            var repo = new EfExternalDeviceRepository(ctx);
            await repo.AddAsync(new ExternalDeviceEntity(deviceId, "To Delete", ExternalDeviceType.REMARKABLE));
        }

        using (var ctx = new MainDbContext(options))
        {
            var repo = new EfExternalDeviceRepository(ctx);
            await repo.DeleteAsync(deviceId);
        }

        using (var ctx = new MainDbContext(options))
        {
            var repo = new EfExternalDeviceRepository(ctx);
            var device = await repo.GetByIdAsync(deviceId);
            Assert.Null(device);
        }
    }

    [Fact]
    public async Task GetByIdAsync_NonExistingDevice_ReturnsNull()
    {
        using var connection = new SqliteConnection("DataSource=:memory:");
        connection.Open();
        var options = CreateSqliteInMemoryOptions(connection);

        using var ctx = new MainDbContext(options);
        ctx.Database.EnsureCreated();
        var repo = new EfExternalDeviceRepository(ctx);

        var device = await repo.GetByIdAsync(Guid.NewGuid());
        Assert.Null(device);
    }

    [Fact]
    public async Task DeleteAsync_NonExistingDevice_DoesNotThrow()
    {
        using var connection = new SqliteConnection("DataSource=:memory:");
        connection.Open();
        var options = CreateSqliteInMemoryOptions(connection);

        using var ctx = new MainDbContext(options);
        ctx.Database.EnsureCreated();
        var repo = new EfExternalDeviceRepository(ctx);

        await repo.DeleteAsync(Guid.NewGuid());
    }

    [Fact]
    public async Task UpdateAsync_NonExistingDevice_ThrowsException()
    {
        using var connection = new SqliteConnection("DataSource=:memory:");
        connection.Open();
        var options = CreateSqliteInMemoryOptions(connection);

        using var ctx = new MainDbContext(options);
        ctx.Database.EnsureCreated();
        var repo = new EfExternalDeviceRepository(ctx);

        await Assert.ThrowsAsync<InvalidOperationException>(
            () => repo.UpdateAsync(new ExternalDeviceEntity(Guid.NewGuid(), "Ghost", ExternalDeviceType.REMARKABLE)));
    }
}
