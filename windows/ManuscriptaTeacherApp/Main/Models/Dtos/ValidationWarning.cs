namespace Main.Models.Dtos;

/// <summary>
/// Represents a validation warning for generated content.
/// See AdditionalValidationRules.md §3AD.
/// </summary>
public class ValidationWarning
{
    /// <summary>A code identifying the error type (e.g., MALFORMED_MARKER, UNCLOSED_BLOCK, INVALID_REFERENCE).</summary>
    public required string ErrorType { get; set; }

    /// <summary>A human-readable description of the issue.</summary>
    public required string Description { get; set; }

    /// <summary>Optional. The line number where the issue occurs.</summary>
    public int? LineNumber { get; set; }
}
