using Main.Models.Dtos;
using Main.Models.Entities;

namespace Main.Services.GenAI;

/// <summary>
/// Handles AI-powered material generation for reading and worksheet content.
/// See GenAISpec.md §3B.
/// </summary>
public class MaterialGenerationService : IMaterialGenerationService
{
    private readonly IInferenceClient _ollamaClient;
    private readonly IEmbeddingService _embeddingService;
    private readonly OutputValidationService _validationService;
    private readonly IInferenceRuntimeSelector _runtimeSelector;
    private const int DefaultTopK = 5;

    // Standard Ollama model names
    private const string StandardPrimaryModel = "qwen3:8b";
    private const string StandardFallbackModel = "granite4";

    // OV-Ollama model names (per ollama_ov import tags)
    private const string OvPrimaryModel = "qwen3-8b-int4-ov:v1";
    private const string OvFallbackModel = "granite4-micro-ov:v1";

    public MaterialGenerationService(
        IInferenceClient ollamaClient,
        IEmbeddingService embeddingService,
        OutputValidationService validationService,
        IInferenceRuntimeSelector runtimeSelector)
    {
        _ollamaClient = ollamaClient;
        _embeddingService = embeddingService;
        _validationService = validationService;
        _runtimeSelector = runtimeSelector;
    }

    /// <summary>
    /// Gets the primary/fallback model names based on active runtime.
    /// </summary>
    private async Task<(string primary, string fallback)> GetModelNamesAsync()
    {
        var runtime = await _runtimeSelector.GetActiveRuntimeAsync();
        return runtime == InferenceRuntime.OPENVINO
            ? (OvPrimaryModel, OvFallbackModel)
            : (StandardPrimaryModel, StandardFallbackModel);
    }

    /// <summary>
    /// Generates reading content using AI.
    /// See GenAISpec.md §3B(1)(a).
    /// </summary>
    public async Task<GenerationResult> GenerateReading(GenerationRequest request)
    {
        return await GenerateMaterialAsync(request, "reading");
    }

    /// <summary>
    /// Generates worksheet content using AI.
    /// See GenAISpec.md §3B(1)(b).
    /// </summary>
    public async Task<GenerationResult> GenerateWorksheet(GenerationRequest request)
    {
        return await GenerateMaterialAsync(request, "worksheet");
    }

    /// <summary>
    /// Core material generation workflow.
    /// See GenAISpec.md §3B(3).
    /// </summary>
    private async Task<GenerationResult> GenerateMaterialAsync(GenerationRequest request, string materialType)
    {
        var (primaryModel, fallbackModel) = await GetModelNamesAsync();

        // Determine which model to use
        string modelToUse = primaryModel;
        bool useFallback = false;

        // §1(6): Check for insufficient resources per GenAISpec §1(6)
        try
        {
            await _ollamaClient.EnsureModelReadyAsync(primaryModel);
            var canUsePrimary = await _ollamaClient.CanGenerateWithModelAsync(primaryModel);
            if (!canUsePrimary)
            {
                throw new InvalidOperationException("Insufficient resources for primary model");
            }
        }
        catch (InvalidOperationException ex) when (ex.Message.Contains("Insufficient resources"))
        {
            // §1(6)(a): Fall back to smaller model if primary is unavailable or insufficient
            modelToUse = fallbackModel;
            useFallback = true;
            
            // CRITICAL: Unload the primary model from memory before attempting fallback
            // The pre-check may have loaded the primary model into memory, which prevents
            // the smaller fallback model from fitting in available memory
            await _ollamaClient.UnloadModelAsync(primaryModel);
            await Task.Delay(1000); // Wait longer for memory to be reclaimed
            
            try
            {
                await _ollamaClient.EnsureModelReadyAsync(fallbackModel);
            }
            catch
            {
                throw new InvalidOperationException(
                    "Both primary and fallback models are unavailable. Please check your Ollama installation and available system memory.");
            }
        }

        // §3B(3)(a): Embed the description
        // Ensure embedding model is ready per GenAISpec §2(2)
        await _ollamaClient.EnsureModelReadyAsync("nomic-embed-text", useStandardOllama: true);
        var queryEmbedding = await _ollamaClient.GenerateEmbeddingAsync(request.Description);

        // §3B(3)(b): Query ChromaDB for relevant chunks
        var relevantChunks = await _embeddingService.RetrieveRelevantChunksAsync(
            queryEmbedding,
            request.UnitCollectionId,
            request.SourceDocumentIds,
            DefaultTopK
        );

        // §3B(3)(c): Construct prompt
        var prompt = GenAIPromptBuilder.BuildGenerationPrompt(
            request.Description,
            request.ReadingAge,
            request.ActualAge,
            request.DurationInMinutes,
            relevantChunks,
            materialType
        );

        // §3B(3)(d): Invoke model
        string generatedContent;
        try
        {
            generatedContent = await _ollamaClient.GenerateChatCompletionAsync(modelToUse, prompt);
        }
        catch (HttpRequestException ex) when (
            ex.StatusCode == System.Net.HttpStatusCode.InternalServerError ||
            (ex.Message.Contains("500", StringComparison.OrdinalIgnoreCase) ||
             ex.Message.Contains("system memory", StringComparison.OrdinalIgnoreCase) ||
             ex.Message.Contains("InternalServerError", StringComparison.OrdinalIgnoreCase)))
        {
            // §1(6)(a): fall back when primary model is unavailable or has insufficient resources at runtime.
            if (!useFallback)
            {
                modelToUse = fallbackModel;
                useFallback = true;
                try
                {
                    // Unload the primary model to free memory before attempting fallback
                    await _ollamaClient.UnloadModelAsync(primaryModel);
                    await Task.Delay(500); // Brief delay to allow memory to be released
                    
                    await _ollamaClient.EnsureModelReadyAsync(fallbackModel);
                    generatedContent = await _ollamaClient.GenerateChatCompletionAsync(modelToUse, prompt);
                }
                catch (Exception fallbackEx)
                {
                    throw new InvalidOperationException(
                        $"Both primary ({primaryModel}) and fallback ({fallbackModel}) models exhausted due to insufficient system memory. Please close other applications and try again. Error: {fallbackEx.Message}",
                        fallbackEx);
                }
            }
            else
            {
                // Already on fallback, so this is a genuine failure
                throw new InvalidOperationException(
                    $"The fallback model ({fallbackModel}) also failed due to insufficient system memory. Please close other applications and try again.",
                    ex);
            }
        }

        // §3B(3)(e): Validate and refine
        var result = await _validationService.ValidateAndRefineAsync(generatedContent, modelToUse, useFallback);

        // §3B(3)(g): Return result. For worksheets, question-draft markers are processed when the material is created.
        return result;
    }

    /// <summary>
    /// Lightweight probe used by the frontend availability check.  Attempts a
    /// quick generation test with the primary model to detect low‑memory
    /// situations that would cause a real request to fail.  Mirrors the logic in
    /// OllamaClientService.CanGenerateWithModelAsync but scoped to the primary
    /// model name.
    /// </summary>
    public async Task<bool> CanGenerateWithPrimaryModelAsync()
    {
        try
        {
            var (primaryModel, _) = await GetModelNamesAsync();
            // Ensure the model is at least pulled (will throw if daemon not running)
            await _ollamaClient.EnsureModelReadyAsync(primaryModel);
            return await _ollamaClient.CanGenerateWithModelAsync(primaryModel);
        }
        catch
        {
            return false;
        }
    }

}
