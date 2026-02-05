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
            
            // §1(6): Check for insufficient resources per GenAISpec §1(4)
            var canUsePrimary = await _ollamaClient.CanGenerateWithModelAsync(PrimaryModel);
            if (!canUsePrimary)
            {
                throw new InvalidOperationException("Insufficient resources for primary model");
            }
        }
        catch
        {
            // §1(6)(a): Fall back to smaller model if primary is unavailable or insufficient
            modelToUse = FallbackModel;
            useFallback = true;
            await _ollamaClient.EnsureModelReadyAsync(FallbackModel);
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
    /// See GenAISpec.md §3B(3)(c) and Material Encoding Specification §4.
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
            ? string.Join("\n\n", relevantChunks)
            : "";

        var markdownSyntaxGuide = MarkdownSyntaxGuide.Get(includeQuestionSyntax: materialType == "worksheet");

        return $@"
TASK:
Create an educational {materialType} material based on the material description and source document context provided below, obeying all listed restraints.


MATERIAL DESCRIPTION:
{description}


RESTRAINTS:
Adapt the material's reading level for students with a reading age of {readingAge} and an actual age of {actualAge}.
Tailor the material's length so that students can complete the material in approximately {durationInMinutes} minutes.
Format the material using the Markdown syntax described below.
Use British English always.
Avoid American English.


MARKDOWN SYNTAX:

---

{markdownSyntaxGuide}

---


SOURCE DOCUMENT CONTEXT:
{contextSection}
";
    }
}
