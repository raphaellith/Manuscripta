namespace Main.Models.Enums;

/// <summary>
/// Represents the indexing state of a source document for semantic retrieval.
/// See AdditionalValidationRules.md §3A(1)(c).
/// </summary>
public enum EmbeddingStatus
{
    /// <summary>The document is queued for indexing or indexing is in progress.</summary>
    PENDING,

    /// <summary>The document has been successfully indexed and is available for semantic retrieval.</summary>
    INDEXED,

    /// <summary>The indexing process failed. The document is not available for semantic retrieval.</summary>
    FAILED
}
