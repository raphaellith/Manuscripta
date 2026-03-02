using System;
using System.Collections.Generic;
using System.Threading.Tasks;
using Main.Models.Entities;

namespace Main.Services.Repositories;

/// <summary>
/// Repository interface for managing source documents.
/// Per NetworkingAPISpec §1(1)(k).
/// </summary>
public interface ISourceDocumentRepository
{
    Task<SourceDocumentEntity?> GetByIdAsync(Guid id);
    Task<IEnumerable<SourceDocumentEntity>> GetAllAsync();
    Task AddAsync(SourceDocumentEntity entity);
    Task DeleteAsync(Guid id);
}
