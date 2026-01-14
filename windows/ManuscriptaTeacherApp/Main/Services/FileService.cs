namespace Main.Services;

/// <summary>
/// File system operations for attachment files.
/// Per PersistenceAndCascadingRules.md §2(7) and AdditionalValidationRules.md §3B(2)(c).
/// </summary>
public class FileService : IFileService
{
    private readonly string _attachmentsDir;

    public FileService()
    {
        var appDataPath = Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData);
        _attachmentsDir = Path.Combine(appDataPath, "ManuscriptaTeacherApp", "Attachments");
    }

    public string GetAttachmentFilePath(Guid attachmentId, string fileExtension)
    {
        return Path.Combine(_attachmentsDir, $"{attachmentId}.{fileExtension}");
    }

    public bool FileExists(string path)
    {
        return File.Exists(path);
    }

    public void DeleteFile(string path)
    {
        if (File.Exists(path))
        {
            File.Delete(path);
        }
    }
}
