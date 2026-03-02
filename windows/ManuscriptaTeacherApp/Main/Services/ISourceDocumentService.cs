using System;
using System.Threading.Tasks;
using Main.Models.Entities;

namespace Main.Services;

/// <summary>
/// Service interface for managing source documents.
/// Per NetworkingAPISpec §1(1)(k).
/// </summary>
public interface ISourceDocumentService
{
    Task<SourceDocumentEntity> CreateAsync(SourceDocumentEntity entity);
    Task DeleteAsync(Guid id);
}
