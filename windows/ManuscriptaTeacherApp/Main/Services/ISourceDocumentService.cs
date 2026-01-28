using System;
using System.Threading.Tasks;
using Main.Models.Entities;

namespace Main.Services;

/// <summary>
/// Service interface for managing source documents.
/// Per NetworkingAPISpec §1(1)(k) and GenAISpec.md §3A.
/// </summary>
public interface ISourceDocumentService
{
    Task<SourceDocumentEntity> CreateAsync(SourceDocumentEntity entity);
    Task UpdateAsync(SourceDocumentEntity entity);
    Task DeleteAsync(Guid id);
}
