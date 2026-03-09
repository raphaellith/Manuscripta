using System.Text;
using ChromaDB.Client;
using Main.Models.Dtos;

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
        Guid? unitCollectionId,
        Func<StreamingGenerationChunk, Task>? onChunk = null,
        CancellationToken cancellationToken = default)
    {
        // Ensure model is ready
        await _ollamaClient.EnsureModelReadyAsync(ModificationModel);

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
        }

        cancellationToken.ThrowIfCancellationRequested();

        // §3C(2)(b): Construct prompt
        var prompt = GenAIPromptBuilder.BuildModificationPrompt(selectedContent, instruction, relevantChunks);

        cancellationToken.ThrowIfCancellationRequested();

        // §3C(2)(c): Invoke model with streaming per §3H(5)
        string modifiedContent;
        try
        {
            modifiedContent = await InvokeModelStreamingAsync(ModificationModel, prompt, onChunk, cancellationToken);
        }
        catch (HttpRequestException ex) when (ex.Message.Contains("system memory", StringComparison.OrdinalIgnoreCase))
        {
            throw new InvalidOperationException(
                "Insufficient system memory to process this request. Please close other applications and try again.", ex);
        }

        // §3C(2)(d): Validate and refine
        var result = await _validationService.ValidateAndRefineAsync(modifiedContent, ModificationModel, useFallback: true);

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
