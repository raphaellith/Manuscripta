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
        // Determine which model to use
        string modelToUse = PrimaryModel;
        bool useFallback = false;

        // §1(6): Check for insufficient resources per GenAISpec §1(6)
        try
        {
            await _ollamaClient.EnsureModelReadyAsync(PrimaryModel);
            var canUsePrimary = await _ollamaClient.CanGenerateWithModelAsync(PrimaryModel);
            if (!canUsePrimary)
            {
                throw new InvalidOperationException("Insufficient resources for primary model");
            }
        }
        catch (InvalidOperationException ex) when (ex.Message.Contains("Insufficient resources"))
        {
            // §1(6)(a): Fall back to smaller model if primary is unavailable or insufficient
            modelToUse = FallbackModel;
            useFallback = true;
            
            // CRITICAL: Unload the primary model from memory before attempting fallback
            // The pre-check may have loaded the primary model into memory, which prevents
            // the smaller fallback model from fitting in available memory
            await _ollamaClient.UnloadModelAsync(PrimaryModel);
            await Task.Delay(1000); // Wait longer for memory to be reclaimed
            
            try
            {
                await _ollamaClient.EnsureModelReadyAsync(FallbackModel);
            }
            catch
            {
                throw new InvalidOperationException(
                    "Both primary and fallback models are unavailable. Please check your Ollama installation and available system memory.");
            }
        }

        // §3B(3)(a): Embed the description
        // Ensure embedding model is ready per GenAISpec §2(2)
        await _ollamaClient.EnsureModelReadyAsync("nomic-embed-text");
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
            (ex.Message.Contains("500", StringComparison.OrdinalIgnoreCase) ||
             ex.Message.Contains("system memory", StringComparison.OrdinalIgnoreCase) ||
             ex.Message.Contains("InternalServerError", StringComparison.OrdinalIgnoreCase)))
        {
            // §1(6)(a): fall back when primary model is unavailable or has insufficient resources at runtime.
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
                    generatedContent = await _ollamaClient.GenerateChatCompletionAsync(modelToUse, prompt);
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

}
