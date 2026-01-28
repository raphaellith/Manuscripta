namespace Main.Models.Dtos;

/// <summary>
/// Represents the result of an AI generation or modification operation.
/// See AdditionalValidationRules.md §3AC.
/// </summary>
public class GenerationResult
{
    /// <summary>The generated or modified content.</summary>
    public required string Content { get; set; }

    /// <summary>
    /// Optional. A list of validation issues that could not be automatically resolved.
    /// </summary>
    public List<ValidationWarning>? Warnings { get; set; }
}
