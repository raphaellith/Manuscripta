using System.Text;
using Main.Models.Dtos;

namespace Main.Services.GenAI;

/// <summary>
/// Handles AI-powered material generation for reading and worksheet content.
/// See GenAISpec.md §3B.
/// </summary>
public class MaterialGenerationService : IMaterialGenerationService
{
    private readonly OllamaClientService _ollamaClient;
    private readonly IEmbeddingService _embeddingService;
    private readonly OutputValidationService _validationService;
    private const int DefaultTopK = 5;
    private const string PrimaryModel = "qwen3:8b";
    private const string FallbackModel = "granite4";

    public MaterialGenerationService(
        OllamaClientService ollamaClient,
        IEmbeddingService embeddingService,
        OutputValidationService validationService)
    {
        _ollamaClient = ollamaClient;
        _embeddingService = embeddingService;
        _validationService = validationService;
    }

    /// <summary>
    /// Generates reading content using AI.
    /// See GenAISpec.md §3B(1)(a) and §3H(8).
    /// </summary>
    public async Task<GenerationResult> GenerateReading(GenerationRequest request, Func<StreamingGenerationChunk, Task>? onChunk = null, CancellationToken cancellationToken = default)
    {
        return await GenerateMaterialAsync(request, "reading", onChunk, cancellationToken);
    }

    /// <summary>
    /// Generates worksheet content using AI.
    /// See GenAISpec.md §3B(1)(b) and §3H(8).
    /// </summary>
    public async Task<GenerationResult> GenerateWorksheet(GenerationRequest request, Func<StreamingGenerationChunk, Task>? onChunk = null, CancellationToken cancellationToken = default)
    {
        return await GenerateMaterialAsync(request, "worksheet", onChunk, cancellationToken);
    }

    /// <summary>
    /// Core material generation workflow.
    /// See GenAISpec.md §3B(3) and §3H(8).
    /// </summary>
    private async Task<GenerationResult> GenerateMaterialAsync(GenerationRequest request, string materialType, Func<StreamingGenerationChunk, Task>? onChunk = null, CancellationToken cancellationToken = default)
    {
        // Determine which model to use
        string modelToUse = PrimaryModel;
        bool useFallback = false;

        // §1(4): Start model readiness check in parallel with RAG preparation
        // This reduces latency by overlapping the generation model check with embedding/retrieval
        var modelReadyTask = _ollamaClient.EnsureModelReadyAsync(PrimaryModel);

        // §3H(8): Check for cancellation throughout the pipeline
        cancellationToken.ThrowIfCancellationRequested();

        // §3B(3)(a): Embed the description (runs in parallel with model check)
        // Ensure embedding model is ready per GenAISpec §2(2)
        await _ollamaClient.EnsureModelReadyAsync("nomic-embed-text");
        
        cancellationToken.ThrowIfCancellationRequested();
        var queryEmbedding = await _ollamaClient.GenerateEmbeddingAsync(request.Description);

        cancellationToken.ThrowIfCancellationRequested();

        // §3B(3)(b): Query ChromaDB for relevant chunks (runs in parallel with model check)
        var relevantChunks = await _embeddingService.RetrieveRelevantChunksAsync(
            queryEmbedding,
            request.UnitCollectionId,
            request.SourceDocumentIds,
            DefaultTopK
        );

        cancellationToken.ThrowIfCancellationRequested();

        // §3B(3)(c): Construct prompt
        var prompt = GenAIPromptBuilder.BuildGenerationPrompt(
            request.Description,
            request.ReadingAge,
            request.ActualAge,
            request.DurationInMinutes,
            relevantChunks,
            materialType
        );

        // Await the model readiness check before streaming
        await modelReadyTask;

        // §3H(8): Check for cancellation before streaming begins
        cancellationToken.ThrowIfCancellationRequested();

        // §3B(3)(d): Invoke model with streaming per §3H(5)
        string generatedContent;
        try
        {
            generatedContent = await InvokeModelStreamingAsync(modelToUse, prompt, onChunk, cancellationToken);
        }
        catch (HttpRequestException ex) when (
            ex.StatusCode == System.Net.HttpStatusCode.InternalServerError ||
            (ex.Message.Contains("500", StringComparison.OrdinalIgnoreCase) ||
             ex.Message.Contains("system memory", StringComparison.OrdinalIgnoreCase) ||
             ex.Message.Contains("InternalServerError", StringComparison.OrdinalIgnoreCase)))
        {
            // §1(6)(a): Fall back when primary model fails during generation due to resource constraints.
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
                    generatedContent = await InvokeModelStreamingAsync(modelToUse, prompt, onChunk, cancellationToken);
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

        // §3B(3)(e): Validate and refine
        var result = await _validationService.ValidateAndRefineAsync(generatedContent, modelToUse, useFallback);

        // §3B(3)(g): Return result. For worksheets, question-draft markers are processed when the material is created.
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

    /// <summary>
    /// Optional probe for frontend availability checks. Not used in the critical
    /// generation path; per GenAISpec.md §1(6), resource failures during generation
    /// trigger automatic fallback to a smaller model.
    /// </summary>
    public async Task<bool> CanGenerateWithPrimaryModelAsync()
    {
        try
        {
            // Ensure the model is at least pulled (will throw if daemon not running)
            await _ollamaClient.EnsureModelReadyAsync(PrimaryModel);
            return await _ollamaClient.CanGenerateWithModelAsync(PrimaryModel);
        }
        catch
        {
            return false;
        }
    }

}
