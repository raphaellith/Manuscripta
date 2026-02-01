using System;
using System.Collections.Generic;
using System.Threading.Tasks;
using Main.Models.Entities.Responses;
using Main.Models.Dtos;

namespace Main.Services;

/// <summary>
/// Service interface for managing responses.
/// </summary>
public interface IResponseService
{
    Task<ResponseEntity> CreateResponseAsync(ResponseEntity response);
    Task<ResponseEntity> UpdateResponseAsync(ResponseEntity response);
    
    /// <summary>
    /// Deletes a response and its associated feedback.
    /// Per PersistenceAndCascadingRules.md §2(2A).
    /// </summary>
    Task DeleteResponseAsync(Guid id);

    /// <summary>
    /// Validates a response according to business rules without persisting it.
    /// Per Validation Rules §2C.
    /// </summary>
    Task ValidateResponseAsync(ResponseEntity response);

    /// <summary>
    /// Creates a response from a DTO, handling type determination and validation internally.
    /// Optimizes DB access by fetching the question only once.
    /// </summary>
    Task<ResponseEntity> CreateResponseAsync(SubmitResponseDto dto);

    /// <summary>
    /// Creates a batch of responses from DTOs, strictly enforcing Validation Rules §1A(2) (All-or-Nothing).
    /// </summary>
    Task CreateBatchResponsesAsync(IEnumerable<SubmitResponseDto> dtos);
}

