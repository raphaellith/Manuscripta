using System.ComponentModel.DataAnnotations;

namespace Main.Models.Dtos;

/// <summary>
/// Request DTO for batch response submission.
/// Per API Contract §2.3 - POST /responses/batch.
/// </summary>
public class BatchResponseRequest
{
    /// <summary>
    /// Array of response objects to submit.
    /// All responses must be valid; any invalid response invalidates entire batch.
    /// </summary>
    [Required]
    public List<ResponseRequest> Responses { get; set; } = new();
}
