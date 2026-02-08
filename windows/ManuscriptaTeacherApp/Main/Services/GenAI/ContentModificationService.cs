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
    private readonly IEmbeddingService _embeddingService;
    private readonly OutputValidationService _validationService;
    private const int DefaultTopK = 5;
    private const string ModificationModel = "granite4";

    public ContentModificationService(
        OllamaClientService ollamaClient,
        IEmbeddingService embeddingService,
        OutputValidationService validationService)
    {
        _ollamaClient = ollamaClient;
        _embeddingService = embeddingService;
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
            // Ensure embedding model is ready per GenAISpec §2(2)
            await _ollamaClient.EnsureModelReadyAsync("nomic-embed-text");
            var queryEmbedding = await _ollamaClient.GenerateEmbeddingAsync(instruction);
            relevantChunks = await _embeddingService.RetrieveRelevantChunksAsync(
                queryEmbedding,
                unitCollectionId.Value,
                null,
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
    /// Constructs the modification prompt.
    /// See GenAISpec.md §3C(2)(b) and Material Encoding Specification §4.
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

Return the modified content following these Markdown syntax requirements:
- Headers shall use # for Level 1, ## for Level 2, ### for Level 3 (do not exceed Level 3)
- Text shall be rendered in bold using **text** or __text__ syntax
- Text shall be rendered in italic using *text* or _text_ syntax
- Unordered lists shall be specified using - item or * item syntax
- Ordered lists shall be specified using 1. item syntax
- Tables use GitHub Flavored Markdown syntax with pipes (|) and dashes (---)
- Code blocks shall be specified using triple backticks with optional language identifier
- Blockquotes shall be specified using the > prefix
- Horizontal rules shall be specified using three or more hyphens on a line
- LaTeX notation shall be delimited by single dollar signs ($...$) for inline or double dollar signs ($$...$$) for blocks
- Images shall be embedded using ![alt text](/attachments/{{attachment-uuid}})
- PDF documents shall be embedded using the pdf marker: !!! pdf id=""{{pdf-attachment-uuid}}""
- Text shall be centred using the center marker: !!! center
- Questions shall be referenced using the question marker: !!! question id=""{{question-uuid}}""
- Preserve existing formatting and structure where appropriate

Modified content:";
    }
}
