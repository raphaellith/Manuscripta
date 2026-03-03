using System;
using System.Collections.Generic;
using System.Linq;using System.Net;using System.Net;
using System.Threading.Tasks;
using Main.Data;
using Main.Models.Dtos;
using Main.Services;
using Main.Services.GenAI;
using Microsoft.EntityFrameworkCore;
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
        var validationService = new OutputValidationService(new OllamaClientService(new Mock<IInferenceRuntimeSelector>().Object), dbContext, fileService.Object);

        var ollama = new FakeOllamaClientService
        {
            ThrowOnPrimaryChat = true,
            PrimaryChatResponse = "primary-output",
            FallbackChatResponse = "fallback-output"
        };

        var embeddingService = new Mock<IEmbeddingService>();
        embeddingService
            .Setup(s => s.RetrieveRelevantChunksAsync(It.IsAny<float[]>(), It.IsAny<Guid>(), It.IsAny<List<Guid>?>(), It.IsAny<int>()))
            .ReturnsAsync(new List<string> { "chunk-a", "chunk-b" });

        var service = new MaterialGenerationService(ollama, embeddingService.Object, validationService);

        var request = new GenerationRequest
        {
            Description = "Generate a reading on ecosystems.",
            ReadingAge = 10,
            ActualAge = 10,
            DurationInMinutes = 20,
            UnitCollectionId = Guid.NewGuid()
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
        var validationService = new OutputValidationService(new OllamaClientService(new Mock<IInferenceRuntimeSelector>().Object), dbContext, fileService.Object);

        var ollama = new FakeOllamaClientService
        {
            ThrowOnPrimaryChat = false,
            PrimaryChatResponse = "primary-output",
            FallbackChatResponse = "fallback-output"
        };

        var embeddingService = new Mock<IEmbeddingService>();
        embeddingService
            .Setup(s => s.RetrieveRelevantChunksAsync(It.IsAny<float[]>(), It.IsAny<Guid>(), It.IsAny<List<Guid>?>(), It.IsAny<int>()))
            .ReturnsAsync(new List<string> { "chunk-a" });

        var service = new MaterialGenerationService(ollama, embeddingService.Object, validationService);

        var request = new GenerationRequest
        {
            Description = "Generate a reading on rivers.",
            ReadingAge = 11,
            ActualAge = 11,
            DurationInMinutes = 15,
            UnitCollectionId = Guid.NewGuid()
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
        var validationService = new OutputValidationService(new OllamaClientService(new Mock<IInferenceRuntimeSelector>().Object), dbContext, fileService.Object);

        var ollama = new FakeOllamaClientService { CanGenerateResult = false };
        var embeddingService = new Mock<IEmbeddingService>();
        embeddingService
            .Setup(s => s.RetrieveRelevantChunksAsync(It.IsAny<float[]>(), It.IsAny<Guid>(), It.IsAny<List<Guid>?>(), It.IsAny<int>()))
            .ReturnsAsync(new List<string>());

        var service = new MaterialGenerationService(ollama, embeddingService.Object, validationService);

        var result = await service.CanGenerateWithPrimaryModelAsync();
        Assert.False(result);
    }

    [Fact]
    public async Task CanGenerateWithPrimaryModelAsync_ReturnsFalseOnException()
    {
        using var dbContext = BuildDbContext();
        var fileService = new Mock<IFileService>();
        var validationService = new OutputValidationService(new OllamaClientService(new Mock<IInferenceRuntimeSelector>().Object), dbContext, fileService.Object);

        var ollama = new FakeOllamaClientService { ThrowOnCanGenerate = true };
        var embeddingService = new Mock<IEmbeddingService>();
        embeddingService
            .Setup(s => s.RetrieveRelevantChunksAsync(It.IsAny<float[]>(), It.IsAny<Guid>(), It.IsAny<List<Guid>?>(), It.IsAny<int>()))
            .ReturnsAsync(new List<string>());

        var service = new MaterialGenerationService(ollama, embeddingService.Object, validationService);

        var result = await service.CanGenerateWithPrimaryModelAsync();
        Assert.False(result);
    }

    private sealed class FakeOllamaClientService : OllamaClientService
    {
        public FakeOllamaClientService() : base(new Mock<IInferenceRuntimeSelector>().Object) { }

        public bool ThrowOnPrimaryChat { get; set; }
        public string PrimaryChatResponse { get; set; } = "primary";
        public string FallbackChatResponse { get; set; } = "fallback";
        public List<string> ChatModels { get; } = new();

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
    }
}
