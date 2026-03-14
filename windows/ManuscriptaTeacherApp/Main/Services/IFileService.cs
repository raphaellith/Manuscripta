namespace Main.Services;

/// <summary>
/// Abstraction for file system operations to enable mocking.
/// Per PersistenceAndCascadingRules.md §2(7) for attachment file deletion.
/// </summary>
public interface IFileService
{
    /// <summary>
    /// Gets the full path for an attachment file.
    /// </summary>
    string GetAttachmentFilePath(Guid attachmentId, string fileExtension);

    /// <summary>
    /// Checks if a file exists at the specified path.
    /// </summary>
    bool FileExists(string path);

    /// <summary>
    /// Deletes a file at the specified path if it exists.
    /// </summary>
    void DeleteFile(string path);
}
