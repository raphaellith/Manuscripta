using System.Text;
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
    private const string PrimaryModel = "qwen3:8b";
    private const string FallbackModel = "granite4";

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
        Guid? unitCollectionId,
        string materialType,
        string title,
        int? readingAge,
        int? actualAge,
        Func<StreamingGenerationChunk, Task>? onChunk = null,
        CancellationToken cancellationToken = default)
    {
    _logger.LogInformation(
        "Starting AI content modification. UnitCollectionIdProvided={UnitCollectionIdProvided}, SelectedContentLength={SelectedContentLength}, InstructionLength={InstructionLength}",
        unitCollectionId.HasValue,
        selectedContent?.Length ?? 0,
        instruction?.Length ?? 0);

        string modelToUse = PrimaryModel;
        bool useFallback = false;

        // §1(4): Start model readiness check in parallel with RAG preparation
        var modelReadyTask = _ollamaClient.EnsureModelReadyAsync(PrimaryModel);

        cancellationToken.ThrowIfCancellationRequested();

        List<string> relevantChunks = new();

        // §3C(2)(a): Retrieve relevant chunks if unitCollectionId is provided
        if (unitCollectionId.HasValue)
        {
            // Ensure embedding model is ready per GenAISpec §2(2)
            await _ollamaClient.EnsureModelReadyAsync("nomic-embed-text");
            cancellationToken.ThrowIfCancellationRequested();

            var combinedQuery = $"[{instruction}] {selectedContent}";
            var queryEmbedding = await _ollamaClient.GenerateEmbeddingAsync(combinedQuery);
            cancellationToken.ThrowIfCancellationRequested();

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

        cancellationToken.ThrowIfCancellationRequested();

        // §3C(2)(b): Construct prompt
        var prompt = GenAIPromptBuilder.BuildModificationPrompt(
            selectedContent, instruction, relevantChunks, materialType, title, readingAge, actualAge);

        // Await the model readiness check before streaming
        await modelReadyTask;

        // §3H(8): Check for cancellation before streaming begins
        cancellationToken.ThrowIfCancellationRequested();

        // §3C(2)(c): Invoke model with streaming per §3H(5)
        var promptContainsInjectedContext = relevantChunks.Count == 0 || relevantChunks.Any(c => !string.IsNullOrEmpty(c) && prompt.Contains(c, StringComparison.Ordinal));
        _logger.LogInformation(
            "RAG prompt assembled for content modification. PromptLength={PromptLength}, RetrievedChunkCount={RetrievedChunkCount}, ContextInjectionVerified={ContextInjectionVerified}",
            prompt.Length,
            relevantChunks.Count,
            promptContainsInjectedContext);

        string modifiedContent;
        try
        {
            modifiedContent = await InvokeModelStreamingAsync(modelToUse, prompt, onChunk, cancellationToken);
        }
        catch (HttpRequestException ex) when (
            ex.StatusCode == System.Net.HttpStatusCode.InternalServerError ||
            (ex.Message.Contains("500", StringComparison.OrdinalIgnoreCase) ||
             ex.Message.Contains("system memory", StringComparison.OrdinalIgnoreCase) ||
             ex.Message.Contains("InternalServerError", StringComparison.OrdinalIgnoreCase)))
        {
            // §1(6)(a): Fall back when primary model fails during modification due to resource constraints.
            if (!useFallback)
            {
                modelToUse = FallbackModel;
                useFallback = true;
                try
                {
                    // Unload the primary model to free memory before attempting fallback
                    await _ollamaClient.UnloadModelAsync(PrimaryModel);
                    await Task.Delay(500); // Brief delay to allow memory to be released

                    await _ollamaClient.EnsureModelReadyAsync(FallbackModel);
                    modifiedContent = await InvokeModelStreamingAsync(modelToUse, prompt, onChunk, cancellationToken);
                }
                catch (Exception fallbackEx)
                {
                    throw new InvalidOperationException(
                        $"Both primary (qwen3:8b) and fallback (granite4) models exhausted due to insufficient system memory. Please close other applications and try again. Error: {fallbackEx.Message}",
                        fallbackEx);
                }
            }
            else
            {
                // Already on fallback, so this is a genuine failure
                throw new InvalidOperationException(
                    $"The fallback model (granite4) also failed due to insufficient system memory. Please close other applications and try again.",
                    ex);
            }
        }

        // §3C(2)(d): Validate and refine
        var result = await _validationService.ValidateAndRefineAsync(modifiedContent, modelToUse, useFallback);

        _logger.LogInformation(
            "Completed AI content modification. OutputLength={OutputLength}, RetrievedChunkCount={RetrievedChunkCount}",
            result.Content?.Length ?? 0,
            relevantChunks.Count);

        return result;
    }

    /// <summary>
    /// Invokes the model with streaming and accumulates content.
    /// Per GenAISpec.md §3H(5) and §3H(8).
    /// </summary>
    private async Task<string> InvokeModelStreamingAsync(
        string model, string prompt, Func<StreamingGenerationChunk, Task>? onChunk, CancellationToken cancellationToken)
    {
        // §3H(5)(b): Accumulate content tokens
        var contentBuilder = new StringBuilder();

        await foreach (var chunk in _ollamaClient.GenerateChatCompletionStreamingAsync(model, prompt, null, cancellationToken))
        {
            // §3H(8): Check for cancellation during streaming
            cancellationToken.ThrowIfCancellationRequested();
            
            // §3H(5)(a): Invoke callback for each chunk
            if (onChunk != null)
            {
                await onChunk(chunk);
            }

            // §3H(5)(b): Only accumulate non-thinking tokens
            if (!chunk.IsThinking && !string.IsNullOrEmpty(chunk.Token))
            {
                contentBuilder.Append(chunk.Token);
            }
        }

        // §3H(5)(c): Return accumulated content for validation
        return contentBuilder.ToString();
    }
}
