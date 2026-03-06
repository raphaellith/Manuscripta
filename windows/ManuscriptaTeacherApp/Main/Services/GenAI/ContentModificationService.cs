using ChromaDB.Client;
using Main.Models.Dtos;
using Microsoft.Extensions.Logging;

namespace Main.Services.GenAI;

/// <summary>
/// Handles AI-powered content modification (AI assistant).
/// See GenAISpec.md §3C.
/// </summary>
public class ContentModificationService : IContentModificationService
{
    private readonly OllamaClientService _ollamaClient;
    private readonly IEmbeddingService _embeddingService;
    private readonly OutputValidationService _validationService;
    private readonly ILogger<ContentModificationService> _logger;
    private const int DefaultTopK = 5;
    private const string ModificationModel = "granite4";

    public ContentModificationService(
        OllamaClientService ollamaClient,
        IEmbeddingService embeddingService,
        OutputValidationService validationService,
        ILogger<ContentModificationService> logger)
    {
        _ollamaClient = ollamaClient;
        _embeddingService = embeddingService;
        _validationService = validationService;
        _logger = logger;
    }

    /// <summary>
    /// Modifies selected content based on the teacher's instruction.
    /// See GenAISpec.md §3C(1)(a) and §3C(2).
    /// </summary>
    public async Task<GenerationResult> ModifyContent(
        string selectedContent,
        string instruction,
        Guid? unitCollectionId)
    {
        _logger.LogInformation(
            "Starting AI content modification. UnitCollectionIdProvided={UnitCollectionIdProvided}, SelectedContentLength={SelectedContentLength}, InstructionLength={InstructionLength}",
            unitCollectionId.HasValue,
            selectedContent?.Length ?? 0,
            instruction?.Length ?? 0);

        // Ensure model is ready
        await _ollamaClient.EnsureModelReadyAsync(ModificationModel);

        List<string> relevantChunks = new();

        // §3C(2)(a): Retrieve relevant chunks if unitCollectionId is provided
        if (unitCollectionId.HasValue)
        {
            // Ensure embedding model is ready per GenAISpec §2(2)
            await _ollamaClient.EnsureModelReadyAsync("nomic-embed-text");
            var queryEmbedding = await _ollamaClient.GenerateEmbeddingAsync(instruction);
            relevantChunks = await _embeddingService.RetrieveRelevantChunksAsync(
                queryEmbedding,
                unitCollectionId.Value,
                null,
                DefaultTopK
            );

            _logger.LogInformation(
                "RAG retrieval completed for content modification. UnitCollectionId={UnitCollectionId}, RetrievedChunkCount={RetrievedChunkCount}, TotalRetrievedContextChars={TotalRetrievedContextChars}, TopK={TopK}",
                unitCollectionId.Value,
                relevantChunks.Count,
                relevantChunks.Sum(c => c?.Length ?? 0),
                DefaultTopK);
        }
        else
        {
            _logger.LogInformation("Skipping RAG retrieval for content modification because UnitCollectionId was not provided.");
        }

        // §3C(2)(b): Construct prompt
        var prompt = GenAIPromptBuilder.BuildModificationPrompt(selectedContent, instruction, relevantChunks);

        var promptContainsInjectedContext = relevantChunks.Count == 0 || relevantChunks.Any(c => !string.IsNullOrEmpty(c) && prompt.Contains(c, StringComparison.Ordinal));
        _logger.LogInformation(
            "RAG prompt assembled for content modification. PromptLength={PromptLength}, RetrievedChunkCount={RetrievedChunkCount}, ContextInjectionVerified={ContextInjectionVerified}",
            prompt.Length,
            relevantChunks.Count,
            promptContainsInjectedContext);

        // §3C(2)(c): Invoke model
        string modifiedContent;
        try
        {
            modifiedContent = await _ollamaClient.GenerateChatCompletionAsync(ModificationModel, prompt);
        }
        catch (HttpRequestException ex) when (ex.Message.Contains("system memory", StringComparison.OrdinalIgnoreCase))
        {
            throw new InvalidOperationException(
                "Insufficient system memory to process this request. Please close other applications and try again.", ex);
        }

        // §3C(2)(d): Validate and refine
        var result = await _validationService.ValidateAndRefineAsync(modifiedContent, ModificationModel, useFallback: true);

        _logger.LogInformation(
            "Completed AI content modification. OutputLength={OutputLength}, RetrievedChunkCount={RetrievedChunkCount}",
            result.Content?.Length ?? 0,
            relevantChunks.Count);

        return result;
    }

}
