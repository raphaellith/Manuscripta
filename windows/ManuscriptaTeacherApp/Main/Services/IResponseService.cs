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
    Task<ResponseEntity?> GetResponseByIdAsync(Guid id);
    Task<IEnumerable<ResponseEntity>> GetResponsesByQuestionIdAsync(Guid questionId);
    Task<ResponseEntity> UpdateResponseAsync(ResponseEntity response);
    Task DeleteResponseAsync(Guid id);
}

