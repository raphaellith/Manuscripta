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
/// Tests for EfReMarkableDeviceRepository CRUD operations.
/// Uses SQLite in-memory database per existing RepositoryTests pattern.
/// </summary>
public class EfReMarkableDeviceRepositoryTests
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
            var repo = new EfReMarkableDeviceRepository(ctx);

            var device = new ReMarkableDeviceEntity(deviceId, "My reMarkable");
            await repo.AddAsync(device);
        }

        using (var ctx = new MainDbContext(options))
        {
            var repo = new EfReMarkableDeviceRepository(ctx);
            var retrieved = await repo.GetByIdAsync(deviceId);

            Assert.NotNull(retrieved);
            Assert.Equal(deviceId, retrieved!.DeviceId);
            Assert.Equal("My reMarkable", retrieved.Name);
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
            var repo = new EfReMarkableDeviceRepository(ctx);

            await repo.AddAsync(new ReMarkableDeviceEntity(Guid.NewGuid(), "Device A"));
            await repo.AddAsync(new ReMarkableDeviceEntity(Guid.NewGuid(), "Device B"));
            await repo.AddAsync(new ReMarkableDeviceEntity(Guid.NewGuid(), "Device C"));
        }

        using (var ctx = new MainDbContext(options))
        {
            var repo = new EfReMarkableDeviceRepository(ctx);
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
            var repo = new EfReMarkableDeviceRepository(ctx);
            await repo.AddAsync(new ReMarkableDeviceEntity(deviceId, "Original Name"));
        }

        using (var ctx = new MainDbContext(options))
        {
            var repo = new EfReMarkableDeviceRepository(ctx);
            await repo.UpdateAsync(new ReMarkableDeviceEntity(deviceId, "Updated Name"));
        }

        using (var ctx = new MainDbContext(options))
        {
            var repo = new EfReMarkableDeviceRepository(ctx);
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
            var repo = new EfReMarkableDeviceRepository(ctx);
            await repo.AddAsync(new ReMarkableDeviceEntity(deviceId, "To Delete"));
        }

        using (var ctx = new MainDbContext(options))
        {
            var repo = new EfReMarkableDeviceRepository(ctx);
            await repo.DeleteAsync(deviceId);
        }

        using (var ctx = new MainDbContext(options))
        {
            var repo = new EfReMarkableDeviceRepository(ctx);
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
        var repo = new EfReMarkableDeviceRepository(ctx);

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
        var repo = new EfReMarkableDeviceRepository(ctx);

        // Should not throw when deleting non-existent device
        await repo.DeleteAsync(Guid.NewGuid());
    }

    [Fact]
    public async Task UpdateAsync_NonExistingDevice_DoesNotThrow()
    {
        using var connection = new SqliteConnection("DataSource=:memory:");
        connection.Open();
        var options = CreateSqliteInMemoryOptions(connection);

        using var ctx = new MainDbContext(options);
        ctx.Database.EnsureCreated();
        var repo = new EfReMarkableDeviceRepository(ctx);

        // Should not throw when updating non-existent device
        await repo.UpdateAsync(new ReMarkableDeviceEntity(Guid.NewGuid(), "Ghost"));
    }
}
