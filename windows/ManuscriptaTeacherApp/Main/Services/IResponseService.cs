using System;
using System.Collections.Generic;
using System.Threading.Tasks;
using Main.Models.Entities.Responses;

namespace Main.Services;

/// <summary>
/// Service interface for managing responses.
/// </summary>
public interface IResponseService
{
    Task<ResponseEntity> CreateResponseAsync(ResponseEntity response);
    Task<ResponseEntity> UpdateResponseAsync(ResponseEntity response);
    
    /// <summary>
    /// Creates multiple responses atomically with optimized validation.
    /// Validates unique device IDs once before processing.
    /// All-or-nothing semantics: if any response fails, none are stored.
    /// </summary>
    Task CreateResponseBatchAsync(IEnumerable<ResponseEntity> responses);
}

