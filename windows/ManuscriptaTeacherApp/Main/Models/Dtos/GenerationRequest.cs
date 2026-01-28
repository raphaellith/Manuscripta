namespace Main.Models.Dtos;

/// <summary>
/// Represents a request to generate material content using AI.
/// See AdditionalValidationRules.md §3AB.
/// </summary>
public class GenerationRequest
{
    /// <summary>The teacher's description of desired content.</summary>
    public required string Description { get; set; }

    /// <summary>Target reading age.</summary>
    public required int ReadingAge { get; set; }

    /// <summary>Actual age of the audience.</summary>
    public required int ActualAge { get; set; }

    /// <summary>Approximate completion time.</summary>
    public required int DurationInMinutes { get; set; }

    /// <summary>The unit collection containing source documents.</summary>
    public required Guid UnitCollectionId { get; set; }

    /// <summary>
    /// Optional. If provided, limits semantic retrieval to the specified source documents.
    /// If null or empty, all indexed documents in the unit collection are searched.
    /// </summary>
    public List<Guid>? SourceDocumentIds { get; set; }
}
