using System;
using System.Threading.Tasks;
using Main.Data;
using Main.Models.Entities;
using Main.Models.Enums;
using Main.Services.GenAI;
using Microsoft.EntityFrameworkCore;
using Xunit;

namespace MainTests.ServicesTests.GenAI;

/// <summary>
/// Spec coverage: GenAISpec Section 3E (Embedding Status Query).
/// See docs/specifications/GenAISpec.md.
/// </summary>
public class EmbeddingStatusServiceTests
{
    private static MainDbContext BuildDbContext()
    {
        var options = new DbContextOptionsBuilder<MainDbContext>()
            .UseInMemoryDatabase(Guid.NewGuid().ToString())
            .Options;
        return new MainDbContext(options);
    }

    [Fact]
    public async Task GetEmbeddingStatus_MissingDocument_Throws()
    {
        using var dbContext = BuildDbContext();
        var service = new EmbeddingStatusService(dbContext);

        await Assert.ThrowsAsync<InvalidOperationException>(() =>
            service.GetEmbeddingStatus(Guid.NewGuid()));
    }

    /// <summary>
    /// Spec coverage: GenAISpec Section 3A(2)(a) and 3E(2) (default pending status).
    /// See docs/specifications/GenAISpec.md.
    /// </summary>
    [Fact]
    public async Task GetEmbeddingStatus_NullStatus_ReturnsPending()
    {
        using var dbContext = BuildDbContext();
        var docId = Guid.NewGuid();

        dbContext.SourceDocuments.Add(new SourceDocumentEntity(docId, Guid.NewGuid(), "Transcript"));
        await dbContext.SaveChangesAsync();

        var service = new EmbeddingStatusService(dbContext);
        var status = await service.GetEmbeddingStatus(docId);

        Assert.Equal(EmbeddingStatus.PENDING, status);
    }

    /// <summary>
    /// Spec coverage: GenAISpec Section 3E(2) (return current status).
    /// See docs/specifications/GenAISpec.md.
    /// </summary>
    [Fact]
    public async Task GetEmbeddingStatus_WithStatus_ReturnsStatus()
    {
        using var dbContext = BuildDbContext();
        var docId = Guid.NewGuid();

        dbContext.SourceDocuments.Add(new SourceDocumentEntity(docId, Guid.NewGuid(), "Transcript")
        {
            EmbeddingStatus = EmbeddingStatus.INDEXED
        });
        await dbContext.SaveChangesAsync();

        var service = new EmbeddingStatusService(dbContext);
        var status = await service.GetEmbeddingStatus(docId);

        Assert.Equal(EmbeddingStatus.INDEXED, status);
    }
}
