using System;
using System.Collections.Generic;
using System.Linq;
using System.Net;
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

public class MaterialGenerationServiceTests
{
    private static MainDbContext BuildDbContext()
    {
        var options = new DbContextOptionsBuilder<MainDbContext>()
            .UseInMemoryDatabase(Guid.NewGuid().ToString())
            .Options;
        return new MainDbContext(options);
    }

    [Fact]
    public async Task GenerateReading_PrimaryChat500_FallsBackToGranite()
    {
        using var dbContext = BuildDbContext();
        var fileService = new Mock<IFileService>();
        var validationService = new OutputValidationService(new OllamaClientService("http://localhost:11434"), dbContext, fileService.Object);

        var ollama = new FakeOllamaClientService
        {
            ThrowOnPrimaryChat = true,
            PrimaryChatResponse = "primary-output",
            FallbackChatResponse = "fallback-output"
        };

        var embeddingService = new Mock<IEmbeddingService>();
        var logger = new Mock<ILogger<MaterialGenerationService>>();
        embeddingService
            .Setup(s => s.RetrieveRelevantChunksAsync(It.IsAny<float[]>(), It.IsAny<Guid>(), It.IsAny<List<Guid>?>(), It.IsAny<int>()))
            .ReturnsAsync(new List<string> { "chunk-a", "chunk-b" });

        var service = new MaterialGenerationService(ollama, embeddingService.Object, validationService, logger.Object);

        var request = new GenerationRequest
        {
            Description = "Generate a reading on ecosystems.",
            ReadingAge = 10,
            ActualAge = 10,
            DurationInMinutes = 20,
            UnitCollectionId = Guid.NewGuid(),
            Title = "Ecosystems Reading"
        };

        var result = await service.GenerateReading(request);

        Assert.Equal("fallback-output", result.Content);
        Assert.Contains("qwen3:8b", ollama.ChatModels);
        Assert.Contains("granite4", ollama.ChatModels);
    }

    [Fact]
    public async Task GenerateReading_PrimaryChatSuccess_DoesNotUseFallback()
    {
        using var dbContext = BuildDbContext();
        var fileService = new Mock<IFileService>();
        var validationService = new OutputValidationService(new OllamaClientService("http://localhost:11434"), dbContext, fileService.Object);

        var ollama = new FakeOllamaClientService
        {
            ThrowOnPrimaryChat = false,
            PrimaryChatResponse = "primary-output",
            FallbackChatResponse = "fallback-output"
        };

        var embeddingService = new Mock<IEmbeddingService>();
        var logger = new Mock<ILogger<MaterialGenerationService>>();
        embeddingService
            .Setup(s => s.RetrieveRelevantChunksAsync(It.IsAny<float[]>(), It.IsAny<Guid>(), It.IsAny<List<Guid>?>(), It.IsAny<int>()))
            .ReturnsAsync(new List<string> { "chunk-a" });

        var service = new MaterialGenerationService(ollama, embeddingService.Object, validationService, logger.Object);

        var request = new GenerationRequest
        {
            Description = "Generate a reading on rivers.",
            ReadingAge = 11,
            ActualAge = 11,
            DurationInMinutes = 15,
            UnitCollectionId = Guid.NewGuid(),
            Title = "Rivers Reading"
        };

        var result = await service.GenerateReading(request);

        Assert.Equal("primary-output", result.Content);
        Assert.Equal(1, ollama.ChatModels.Count(m => m == "qwen3:8b"));
        Assert.DoesNotContain("granite4", ollama.ChatModels);
    }

    [Fact]
    public async Task CanGenerateWithPrimaryModelAsync_ReturnsFalseWhenClientReportsFalse()
    {
        using var dbContext = BuildDbContext();
        var fileService = new Mock<IFileService>();
        var validationService = new OutputValidationService(new OllamaClientService("http://localhost:11434"), dbContext, fileService.Object);

        var ollama = new FakeOllamaClientService { CanGenerateResult = false };
        var embeddingService = new Mock<IEmbeddingService>();
        var logger = new Mock<ILogger<MaterialGenerationService>>();
        embeddingService
            .Setup(s => s.RetrieveRelevantChunksAsync(It.IsAny<float[]>(), It.IsAny<Guid>(), It.IsAny<List<Guid>?>(), It.IsAny<int>()))
            .ReturnsAsync(new List<string>());

        var service = new MaterialGenerationService(ollama, embeddingService.Object, validationService, logger.Object);

        var result = await service.CanGenerateWithPrimaryModelAsync();
        Assert.False(result);
    }

    [Fact]
    public async Task CanGenerateWithPrimaryModelAsync_ReturnsFalseOnException()
    {
        using var dbContext = BuildDbContext();
        var fileService = new Mock<IFileService>();
        var validationService = new OutputValidationService(new OllamaClientService("http://localhost:11434"), dbContext, fileService.Object);

        var ollama = new FakeOllamaClientService { ThrowOnCanGenerate = true };
        var embeddingService = new Mock<IEmbeddingService>();
        var logger = new Mock<ILogger<MaterialGenerationService>>();
        embeddingService
            .Setup(s => s.RetrieveRelevantChunksAsync(It.IsAny<float[]>(), It.IsAny<Guid>(), It.IsAny<List<Guid>?>(), It.IsAny<int>()))
            .ReturnsAsync(new List<string>());

        var service = new MaterialGenerationService(ollama, embeddingService.Object, validationService, logger.Object);

        var result = await service.CanGenerateWithPrimaryModelAsync();
        Assert.False(result);
    }

    [Fact]
    public async Task GenerateReading_LogsRagPromptInjectionVerification()
    {
        using var dbContext = BuildDbContext();
        var fileService = new Mock<IFileService>();
        var validationService = new OutputValidationService(new OllamaClientService("http://localhost:11434"), dbContext, fileService.Object);

        var ollama = new FakeOllamaClientService
        {
            ThrowOnPrimaryChat = false,
            PrimaryChatResponse = "primary-output"
        };

        var embeddingService = new Mock<IEmbeddingService>();
        embeddingService
            .Setup(s => s.RetrieveRelevantChunksAsync(It.IsAny<float[]>(), It.IsAny<Guid>(), It.IsAny<List<Guid>?>(), It.IsAny<int>()))
            .ReturnsAsync(new List<string> { "chunk-context-1" });

        var logger = new Mock<ILogger<MaterialGenerationService>>();
        var service = new MaterialGenerationService(ollama, embeddingService.Object, validationService, logger.Object);

        var request = new GenerationRequest
        {
            Description = "Generate a reading on climate.",
            ReadingAge = 12,
            ActualAge = 12,
            DurationInMinutes = 20,
            UnitCollectionId = Guid.NewGuid(),
            Title = "Climate Reading"
        };

        await service.GenerateReading(request);

        logger.Verify(
            x => x.Log(
                LogLevel.Information,
                It.IsAny<EventId>(),
                It.Is<It.IsAnyType>((v, _) =>
                    v.ToString() != null &&
                    v.ToString()!.Contains("RAG prompt assembled", StringComparison.Ordinal) &&
                    v.ToString()!.Contains("ContextInjectionVerified=True", StringComparison.Ordinal)),
                It.IsAny<Exception>(),
                It.IsAny<Func<It.IsAnyType, Exception?, string>>()),
            Times.AtLeastOnce);
    }

    /// <summary>
    /// Per FrontendWorkflowSpec §4B(1)(b): Verifies that when SourceDocumentIds are provided
    /// in the GenerationRequest, they are correctly passed to the embedding service.
    /// </summary>
    [Fact]
    public async Task GenerateReading_WithSourceDocumentIds_PassesIdsToEmbeddingService()
    {
        using var dbContext = BuildDbContext();
        var fileService = new Mock<IFileService>();
        var validationService = new OutputValidationService(new OllamaClientService("http://localhost:11434"), dbContext, fileService.Object);

        var ollama = new FakeOllamaClientService
        {
            ThrowOnPrimaryChat = false,
            PrimaryChatResponse = "generated-content"
        };

        var embeddingService = new Mock<IEmbeddingService>();
        var logger = new Mock<ILogger<MaterialGenerationService>>();
        
        // Capture the source document IDs passed to the embedding service
        List<Guid>? capturedSourceDocIds = null;
        embeddingService
            .Setup(s => s.RetrieveRelevantChunksAsync(
                It.IsAny<float[]>(),
                It.IsAny<Guid>(),
                It.IsAny<List<Guid>?>(),
                It.IsAny<int>()))
            .Callback<float[], Guid, List<Guid>?, int>((_, _, sourceDocIds, _) =>
            {
                capturedSourceDocIds = sourceDocIds;
            })
            .ReturnsAsync(new List<string> { "chunk-a" });

        var service = new MaterialGenerationService(ollama, embeddingService.Object, validationService, logger.Object);

        var sourceDoc1 = Guid.NewGuid();
        var sourceDoc2 = Guid.NewGuid();
        var request = new GenerationRequest
        {
            Description = "Generate a reading on volcanoes.",
            ReadingAge = 11,
            ActualAge = 11,
            DurationInMinutes = 20,
            UnitCollectionId = Guid.NewGuid(),
            Title = "Volcanoes Reading",
            SourceDocumentIds = new List<Guid> { sourceDoc1, sourceDoc2 }
        };

        await service.GenerateReading(request);

        // Verify embedding service was called with the specified source document IDs
        Assert.NotNull(capturedSourceDocIds);
        Assert.Equal(2, capturedSourceDocIds!.Count);
        Assert.Contains(sourceDoc1, capturedSourceDocIds);
        Assert.Contains(sourceDoc2, capturedSourceDocIds);
    }

    /// <summary>
    /// Per FrontendWorkflowSpec §4B(1)(b): Verifies that when no SourceDocumentIds are provided,
    /// null is passed to the embedding service (meaning all documents will be searched).
    /// </summary>
    [Fact]
    public async Task GenerateReading_WithoutSourceDocumentIds_PassesNullToEmbeddingService()
    {
        using var dbContext = BuildDbContext();
        var fileService = new Mock<IFileService>();
        var validationService = new OutputValidationService(new OllamaClientService("http://localhost:11434"), dbContext, fileService.Object);

        var ollama = new FakeOllamaClientService
        {
            ThrowOnPrimaryChat = false,
            PrimaryChatResponse = "generated-content"
        };

        var embeddingService = new Mock<IEmbeddingService>();
        var logger = new Mock<ILogger<MaterialGenerationService>>();
        
        // Track whether callback was invoked and capture the sourceDocIds
        var callbackInvoked = false;
        List<Guid>? capturedSourceDocIds = new List<Guid>(); // Initialize to non-null to verify it becomes null
        embeddingService
            .Setup(s => s.RetrieveRelevantChunksAsync(
                It.IsAny<float[]>(),
                It.IsAny<Guid>(),
                It.IsAny<List<Guid>?>(),
                It.IsAny<int>()))
            .Callback<float[], Guid, List<Guid>?, int>((_, _, sourceDocIds, _) =>
            {
                callbackInvoked = true;
                capturedSourceDocIds = sourceDocIds;
            })
            .ReturnsAsync(new List<string> { "chunk-a" });

        var service = new MaterialGenerationService(ollama, embeddingService.Object, validationService, logger.Object);

        var request = new GenerationRequest
        {
            Description = "Generate a reading on mountains.",
            ReadingAge = 10,
            ActualAge = 10,
            DurationInMinutes = 15,
            UnitCollectionId = Guid.NewGuid(),
            Title = "Mountains Reading"
            // SourceDocumentIds is not provided (null)
        };

        await service.GenerateReading(request);

        // Verify embedding service was called with null sourceDocIds
        Assert.True(callbackInvoked);
        Assert.Null(capturedSourceDocIds);
    }

    /// <summary>
    /// Per FrontendWorkflowSpec §4B(1)(b): Verifies that GenerateWorksheet also respects SourceDocumentIds.
    /// </summary>
    [Fact]
    public async Task GenerateWorksheet_WithSourceDocumentIds_PassesIdsToEmbeddingService()
    {
        using var dbContext = BuildDbContext();
        var fileService = new Mock<IFileService>();
        var validationService = new OutputValidationService(new OllamaClientService("http://localhost:11434"), dbContext, fileService.Object);

        var ollama = new FakeOllamaClientService
        {
            ThrowOnPrimaryChat = false,
            PrimaryChatResponse = "generated-worksheet"
        };

        var embeddingService = new Mock<IEmbeddingService>();
        var logger = new Mock<ILogger<MaterialGenerationService>>();
        
        List<Guid>? capturedSourceDocIds = null;
        embeddingService
            .Setup(s => s.RetrieveRelevantChunksAsync(
                It.IsAny<float[]>(),
                It.IsAny<Guid>(),
                It.IsAny<List<Guid>?>(),
                It.IsAny<int>()))
            .Callback<float[], Guid, List<Guid>?, int>((_, _, sourceDocIds, _) =>
            {
                capturedSourceDocIds = sourceDocIds;
            })
            .ReturnsAsync(new List<string> { "chunk-a" });

        var service = new MaterialGenerationService(ollama, embeddingService.Object, validationService, logger.Object);

        var sourceDoc = Guid.NewGuid();
        var request = new GenerationRequest
        {
            Description = "Generate a worksheet on ecosystems.",
            ReadingAge = 12,
            ActualAge = 12,
            DurationInMinutes = 30,
            UnitCollectionId = Guid.NewGuid(),
            Title = "Ecosystems Worksheet",
            SourceDocumentIds = new List<Guid> { sourceDoc }
        };

        await service.GenerateWorksheet(request);

        Assert.NotNull(capturedSourceDocIds);
        Assert.Single(capturedSourceDocIds!);
        Assert.Contains(sourceDoc, capturedSourceDocIds);
    }

    [Fact]
    public async Task GenerateReading_EmitsQueryingSourceDocumentsStatusChunk()
    {
        using var dbContext = BuildDbContext();
        var fileService = new Mock<IFileService>();
        var validationService = new OutputValidationService(new OllamaClientService("http://localhost:11434"), dbContext, fileService.Object);

        var ollama = new FakeOllamaClientService
        {
            ThrowOnPrimaryChat = false,
            PrimaryChatResponse = "generated-content"
        };

        var embeddingService = new Mock<IEmbeddingService>();
        embeddingService
            .Setup(s => s.RetrieveRelevantChunksAsync(It.IsAny<float[]>(), It.IsAny<Guid>(), It.IsAny<List<Guid>?>(), It.IsAny<int>()))
            .ReturnsAsync(new List<string> { "context-chunk" });

        var logger = new Mock<ILogger<MaterialGenerationService>>();
        var service = new MaterialGenerationService(ollama, embeddingService.Object, validationService, logger.Object);

        var streamedChunks = new List<StreamingGenerationChunk>();
        var request = new GenerationRequest
        {
            Description = "Generate a reading on plants.",
            ReadingAge = 9,
            ActualAge = 9,
            DurationInMinutes = 10,
            UnitCollectionId = Guid.NewGuid(),
            Title = "Plants"
        };

        await service.GenerateReading(
            request,
            chunk =>
            {
                streamedChunks.Add(chunk);
                return Task.CompletedTask;
            });

        Assert.Contains(streamedChunks, c => c.IsQueryingSourceDocuments);
        Assert.Contains(streamedChunks, c => !c.IsThinking && c.Token == "generated-content");

        var firstContentIndex = streamedChunks.FindIndex(c => !string.IsNullOrEmpty(c.Token));
        Assert.True(firstContentIndex >= 0);
        Assert.DoesNotContain(
            streamedChunks.Take(firstContentIndex),
            c => !c.IsQueryingSourceDocuments && !c.Done && string.IsNullOrEmpty(c.Token));
    }

    private sealed class FakeOllamaClientService : OllamaClientService
    {
        public bool ThrowOnPrimaryChat { get; set; }
        public string PrimaryChatResponse { get; set; } = "primary";
        public string FallbackChatResponse { get; set; } = "fallback";
        public List<string> ChatModels { get; } = new();

        public FakeOllamaClientService() : base("http://localhost:11434")
        {
        }

        public override Task EnsureModelReadyAsync(string modelName)
        {
            return Task.CompletedTask;
        }

        public bool CanGenerateResult { get; set; } = true;
        public bool ThrowOnCanGenerate { get; set; }

        public override Task<bool> CanGenerateWithModelAsync(string modelName)
        {
            if (ThrowOnCanGenerate)
            {
                throw new InvalidOperationException("resource check failed");
            }
            return Task.FromResult(CanGenerateResult);
        }

        public override Task<float[]> GenerateEmbeddingAsync(string text, string model = "nomic-embed-text")
        {
            return Task.FromResult(Enumerable.Repeat(0.1f, 768).ToArray());
        }

        public override Task<string> GenerateChatCompletionAsync(string model, string prompt, string? systemPrompt = null)
        {
            ChatModels.Add(model);

            if (model == "qwen3:8b" && ThrowOnPrimaryChat)
            {
                throw new HttpRequestException(
                    "Primary model failed",
                    null,
                    HttpStatusCode.InternalServerError);
            }

            if (model == "qwen3:8b")
            {
                return Task.FromResult(PrimaryChatResponse);
            }

            return Task.FromResult(FallbackChatResponse);
        }

        /// <summary>
        /// Streaming override that yields a single chunk with the same content as the non-streaming version.
        /// Per GenAISpec §3H(2)(a).
        /// </summary>
        public override async IAsyncEnumerable<StreamingGenerationChunk> GenerateChatCompletionStreamingAsync(
            string model, string prompt, string? systemPrompt = null,
            [EnumeratorCancellation] CancellationToken cancellationToken = default)
        {
            ChatModels.Add(model);

            if (model == "qwen3:8b" && ThrowOnPrimaryChat)
            {
                throw new HttpRequestException(
                    "Primary model failed",
                    null,
                    HttpStatusCode.InternalServerError);
            }

            var content = model == "qwen3:8b" ? PrimaryChatResponse : FallbackChatResponse;
            
            // Yield a single chunk with the full content
            await Task.CompletedTask; // Make async to satisfy compiler
            yield return new StreamingGenerationChunk(content, false, true);
        }
    }
}
