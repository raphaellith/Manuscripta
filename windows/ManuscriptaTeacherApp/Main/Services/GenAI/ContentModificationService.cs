using ChromaDB.Client;
using Main.Models.Dtos;

namespace Main.Services.GenAI;

/// <summary>
/// Handles AI-powered content modification (AI assistant).
/// See GenAISpec.md §3C.
/// </summary>
public class ContentModificationService
{
    private readonly OllamaClientService _ollamaClient;
    private readonly IChromaClient _chromaClient;
    private readonly OutputValidationService _validationService;
    private const int DefaultTopK = 5;
    private const string ModificationModel = "granite4";

    public ContentModificationService(
        OllamaClientService ollamaClient,
        IChromaClient chromaClient,
        OutputValidationService validationService)
    {
        _ollamaClient = ollamaClient;
        _chromaClient = chromaClient;
        _validationService = validationService;
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
        // Ensure model is ready
        await _ollamaClient.EnsureModelReadyAsync(ModificationModel);

        List<string> relevantChunks = new();

        // §3C(2)(a): Retrieve relevant chunks if unitCollectionId is provided
        if (unitCollectionId.HasValue)
        {
            var queryEmbedding = await _ollamaClient.GenerateEmbeddingAsync(instruction);
            relevantChunks = await RetrieveRelevantChunksAsync(
                queryEmbedding,
                unitCollectionId.Value,
                DefaultTopK
            );
        }

        // §3C(2)(b): Construct prompt
        var prompt = ConstructModificationPrompt(selectedContent, instruction, relevantChunks);

        // §3C(2)(c): Invoke model
        var modifiedContent = await _ollamaClient.GenerateChatCompletionAsync(ModificationModel, prompt);

        // §3C(2)(d): Validate and refine
        var result = await _validationService.ValidateAndRefineAsync(modifiedContent, ModificationModel, useFallback: true);

        return result;
    }

    /// <summary>
    /// Retrieves relevant chunks from ChromaDB for context.
    /// See GenAISpec.md §2(4).
    /// </summary>
    private async Task<List<string>> RetrieveRelevantChunksAsync(
        float[] queryEmbedding,
        Guid unitCollectionId,
        int topK)
    {
        try
        {
            var collection = await _chromaClient.GetCollectionAsync("source_documents");

            var where = new Dictionary<string, object>
            {
                { "UnitCollectionId", unitCollectionId.ToString() }
            };

            var results = await collection.QueryAsync(
                queryEmbeddings: new[] { queryEmbedding },
                nResults: topK,
                where: where
            );

            return results.Documents?.FirstOrDefault()?.ToList() ?? new List<string>();
        }
        catch
        {
            return new List<string>();
        }
    }

    /// <summary>
    /// Constructs the modification prompt.
    /// See GenAISpec.md §3C(2)(b).
    /// </summary>
    private string ConstructModificationPrompt(
        string selectedContent,
        string instruction,
        List<string> relevantChunks)
    {
        var contextSection = relevantChunks.Count > 0
            ? $"Relevant context from source documents:\n{string.Join("\n\n", relevantChunks)}\n\n"
            : "";

        return $@"{contextSection}Modify the following content according to the instruction provided.

Original content:
{selectedContent}

Instruction: {instruction}

Return the modified content in Material Encoding Specification format.
Preserve existing formatting and structure where appropriate.
Use proper Markdown syntax (headers, lists, code blocks, tables).
Maintain any existing attachment or question references.

Modified content:";
    }
}
