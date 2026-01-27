using ChromaDB.Client;
using Main.Models.Dtos;

namespace Main.Services.GenAI;

/// <summary>
/// Handles AI-powered material generation for reading and worksheet content.
/// See GenAISpec.md §3B.
/// </summary>
public class MaterialGenerationService
{
    private readonly OllamaClientService _ollamaClient;
    private readonly DocumentEmbeddingService _embeddingService;
    private readonly OutputValidationService _validationService;
    private const int DefaultTopK = 5;
    private const string PrimaryModel = "qwen3:8b";
    private const string FallbackModel = "granite4";

    public MaterialGenerationService(
        OllamaClientService ollamaClient,
        DocumentEmbeddingService embeddingService,
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

        try
        {
            await _ollamaClient.EnsureModelReadyAsync(PrimaryModel);
        }
        catch
        {
            modelToUse = FallbackModel;
            useFallback = true;
            await _ollamaClient.EnsureModelReadyAsync(FallbackModel);
        }

        // §3B(3)(a): Embed the description
        var queryEmbedding = await _ollamaClient.GenerateEmbeddingAsync(request.Description);

        // §3B(3)(b): Query ChromaDB for relevant chunks
        var relevantChunks = await _embeddingService.RetrieveRelevantChunksAsync(
            queryEmbedding,
            request.UnitCollectionId,
            request.SourceDocumentIds,
            DefaultTopK
        );

        // §3B(3)(c): Construct prompt
        var prompt = ConstructGenerationPrompt(
            request.Description,
            request.ReadingAge,
            request.ActualAge,
            request.DurationInMinutes,
            relevantChunks,
            materialType
        );

        // §3B(3)(d): Invoke model
        var generatedContent = await _ollamaClient.GenerateChatCompletionAsync(modelToUse, prompt);

        // §3B(3)(e): Validate and refine
        var result = await _validationService.ValidateAndRefineAsync(generatedContent, modelToUse, useFallback);

        return result;
    }

    /// <summary>
    /// Constructs the generation prompt with context and requirements.
    /// See GenAISpec.md §3B(3)(c).
    /// </summary>
    private string ConstructGenerationPrompt(
        string description,
        int readingAge,
        int actualAge,
        int durationInMinutes,
        List<string> relevantChunks,
        string materialType)
    {
        var contextSection = relevantChunks.Count > 0
            ? $"Relevant context from source documents:\n{string.Join("\n\n", relevantChunks)}\n\n"
            : "";

        var typeSpecificInstructions = materialType == "worksheet"
            ? "Include questions with proper question markers using the format specified in Material Encoding Specification §4(4).\n"
            : "";

        return $@"{contextSection}Generate {materialType} content based on the following requirements:

Description: {description}
Target reading age: {readingAge}
Actual age of audience: {actualAge}
Approximate completion time: {durationInMinutes} minutes

{typeSpecificInstructions}
Format the content using proper Markdown syntax and Material Encoding Specification conventions.
Use headers (maximum H3), lists, code blocks, and tables as appropriate.
Include attachment references where relevant using the format specified in Material Encoding Specification §3.

Generate the content now:";
    }
}
