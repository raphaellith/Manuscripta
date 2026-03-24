using System;
using System.Collections.Generic;
using System.Linq;
using System.Runtime.CompilerServices;
using System.Threading;
using System.Threading.Tasks;
using Main.Data;
using Main.Models.Dtos;
using Main.Services;
using Main.Services.GenAI;
using Microsoft.EntityFrameworkCore;
using Microsoft.Extensions.Logging;
using Moq;
using Xunit;

namespace MainTests.ServicesTests.GenAI;

public class ContentModificationServiceTests
{
    private static MainDbContext BuildDbContext()
    {
        var options = new DbContextOptionsBuilder<MainDbContext>()
            .UseInMemoryDatabase(Guid.NewGuid().ToString())
            .Options;
        return new MainDbContext(options);
    }

    [Fact]
    public async Task ModifyContent_WithUnitCollection_EmitsQueryingSourceDocumentsStatusChunk()
    {
        using var dbContext = BuildDbContext();
        var fileService = new Mock<IFileService>();
        var validationService = new OutputValidationService(new OllamaClientService("http://localhost:11434"), dbContext, fileService.Object);

        var ollama = new FakeOllamaClientService
        {
            PrimaryChatResponse = "modified-content"
        };

        var embeddingService = new Mock<IEmbeddingService>();
        embeddingService
            .Setup(s => s.RetrieveRelevantChunksAsync(It.IsAny<float[]>(), It.IsAny<Guid>(), It.IsAny<List<Guid>?>(), It.IsAny<int>()))
            .ReturnsAsync(new List<string> { "retrieved-chunk" });

        var logger = new Mock<ILogger<ContentModificationService>>();
        var service = new ContentModificationService(ollama, embeddingService.Object, validationService, logger.Object);

        var streamedChunks = new List<StreamingGenerationChunk>();

        await service.ModifyContent(
            selectedContent: "Original content",
            instruction: "Make it shorter",
            unitCollectionId: Guid.NewGuid(),
            materialType: "reading",
            title: "Sample",
            readingAge: 10,
            actualAge: 10,
            onChunk: chunk =>
            {
                streamedChunks.Add(chunk);
                return Task.CompletedTask;
            });

        Assert.Contains(streamedChunks, c => c.IsQueryingSourceDocuments);
        Assert.Contains(streamedChunks, c => !c.IsThinking && c.Token == "modified-content");

        var firstContentIndex = streamedChunks.FindIndex(c => !string.IsNullOrEmpty(c.Token));
        Assert.True(firstContentIndex >= 0);
        Assert.DoesNotContain(
            streamedChunks.Take(firstContentIndex),
            c => !c.IsQueryingSourceDocuments && !c.Done && string.IsNullOrEmpty(c.Token));
    }

    [Fact]
    public async Task ModifyContent_WithoutUnitCollection_DoesNotEmitQueryingSourceDocumentsStatusChunk()
    {
        using var dbContext = BuildDbContext();
        var fileService = new Mock<IFileService>();
        var validationService = new OutputValidationService(new OllamaClientService("http://localhost:11434"), dbContext, fileService.Object);

        var ollama = new FakeOllamaClientService
        {
            PrimaryChatResponse = "modified-content"
        };

        var embeddingService = new Mock<IEmbeddingService>();
        var logger = new Mock<ILogger<ContentModificationService>>();
        var service = new ContentModificationService(ollama, embeddingService.Object, validationService, logger.Object);

        var streamedChunks = new List<StreamingGenerationChunk>();

        await service.ModifyContent(
            selectedContent: "Original content",
            instruction: "Rewrite this",
            unitCollectionId: null,
            materialType: "reading",
            title: "Sample",
            readingAge: 10,
            actualAge: 10,
            onChunk: chunk =>
            {
                streamedChunks.Add(chunk);
                return Task.CompletedTask;
            });

        Assert.DoesNotContain(streamedChunks, c => c.IsQueryingSourceDocuments);
        Assert.Contains(streamedChunks, c => !c.IsThinking && c.Token == "modified-content");

        embeddingService.Verify(
            s => s.RetrieveRelevantChunksAsync(It.IsAny<float[]>(), It.IsAny<Guid>(), It.IsAny<List<Guid>?>(), It.IsAny<int>()),
            Times.Never);
    }

    private sealed class FakeOllamaClientService : OllamaClientService
    {
        public string PrimaryChatResponse { get; set; } = "modified-content";

        public FakeOllamaClientService() : base("http://localhost:11434")
        {
        }

        public override Task EnsureModelReadyAsync(string modelName)
        {
            return Task.CompletedTask;
        }

        public override Task<float[]> GenerateEmbeddingAsync(string text, string model = "nomic-embed-text")
        {
            return Task.FromResult(Enumerable.Repeat(0.1f, 768).ToArray());
        }

        public override async IAsyncEnumerable<StreamingGenerationChunk> GenerateChatCompletionStreamingAsync(
            string model,
            string prompt,
            string? systemPrompt = null,
            [EnumeratorCancellation] CancellationToken cancellationToken = default)
        {
            await Task.CompletedTask;
            yield return new StreamingGenerationChunk(PrimaryChatResponse, false, true);
        }
    }
}
