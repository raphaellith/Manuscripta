
package com.manuscripta.student.utils;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Utility class for managing binary file storage (attachments such as PDFs and images)
 * in the app's internal storage.
 *
 * <p>This class separates file system concerns from repository logic and provides
 * thread-safe operations for saving, retrieving, and deleting attachment files.</p>
 *
 * <p>Files are stored using a predictable path pattern:
 * {@code /internal/attachments/{materialId}/{attachmentId}.{ext}}</p>
 */
public class FileStorageManager {

    /** Name of the root directory for attachments within internal storage. */
    private static final String ATTACHMENTS_DIR = "attachments";

    /** The base directory for file storage. */
    @NonNull
    private final File baseDirectory;

    /** Lock for ensuring thread-safe access to file operations. */
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    /**
     * Creates a new FileStorageManager instance using the app's internal storage.
     *
     * @param context The application context (must not be null)
     * @throws IllegalArgumentException if context is null
     */
    public FileStorageManager(@NonNull Context context) {
        if (context == null) {
            throw new IllegalArgumentException("Context cannot be null");
        }
        this.baseDirectory = context.getApplicationContext().getFilesDir();
    }

    /**
     * Creates a new FileStorageManager instance with a custom base directory.
     * This constructor is primarily for testing purposes.
     *
     * @param baseDirectory The base directory for file storage (must not be null)
     * @throws IllegalArgumentException if baseDirectory is null
     */
    public FileStorageManager(@NonNull File baseDirectory) {
        if (baseDirectory == null) {
            throw new IllegalArgumentException("Base directory cannot be null");
        }
        this.baseDirectory = baseDirectory;
    }

    /**
     * Saves an attachment file to internal storage.
     *
     * <p>The file is saved at: {@code /internal/attachments/{materialId}/{attachmentId}.{ext}}</p>
     *
     * @param materialId   The unique identifier for the material (must not be null or empty)
     * @param attachmentId The unique identifier for the attachment (must not be null or empty)
     * @param extension    The file extension without the dot (must not be null or empty)
     * @param bytes        The binary content to save (must not be null)
     * @return The saved file, or null if the save operation failed
     * @throws IllegalArgumentException if any parameter is null or empty (where applicable)
     */
    @Nullable
    public File saveAttachment(@NonNull String materialId,
                               @NonNull String attachmentId,
                               @NonNull String extension,
                               @NonNull byte[] bytes) {
        validateMaterialId(materialId);
        validateAttachmentId(attachmentId);
        validateExtension(extension);
        if (bytes == null) {
            throw new IllegalArgumentException("Bytes cannot be null");
        }

        lock.writeLock().lock();
        try {
            File materialDir = getMaterialDirectory(materialId);
            if (!materialDir.exists() && !createDirectory(materialDir)) {
                return null;
            }

            File attachmentFile = new File(materialDir, attachmentId + "." + extension);
            if (!writeToFile(attachmentFile, bytes)) {
                return null;
            }
            return attachmentFile;
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Retrieves an attachment file from internal storage.
     *
     * <p>This method searches for files matching the pattern:
     * {@code /internal/attachments/{materialId}/{attachmentId}.*}</p>
     *
     * @param materialId   The unique identifier for the material (must not be null or empty)
     * @param attachmentId The unique identifier for the attachment (must not be null or empty)
     * @return The attachment file if it exists, or null if not found
     * @throws IllegalArgumentException if materialId or attachmentId is null or empty
     */
    @Nullable
    public File getAttachmentFile(@NonNull String materialId,
                                  @NonNull String attachmentId) {
        validateMaterialId(materialId);
        validateAttachmentId(attachmentId);

        lock.readLock().lock();
        try {
            File materialDir = getMaterialDirectory(materialId);
            if (!materialDir.exists() || !materialDir.isDirectory()) {
                return null;
            }

            File[] files = listFilesWithFilter(materialDir, attachmentId);

            if (files != null && files.length > 0) {
                return files[0];
            }
            return null;
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Lists files in a directory that match the attachment ID prefix.
     * This method is protected to allow testing of failure scenarios.
     *
     * @param directory    The directory to list files from
     * @param attachmentId The attachment ID to filter by
     * @return The array of matching files, or null if an I/O error occurs
     */
    @Nullable
    protected File[] listFilesWithFilter(@NonNull File directory, @NonNull String attachmentId) {
        return directory.listFiles((dir, name) -> name.startsWith(attachmentId + "."));
    }

    /**
     * Deletes all attachment files for a specific material.
     *
     * <p>This is typically used during "end lesson" cleanup to remove
     * all attachments associated with a material.</p>
     *
     * @param materialId The unique identifier for the material (must not be null or empty)
     * @return true if the deletion was successful (or directory didn't exist), false otherwise
     * @throws IllegalArgumentException if materialId is null or empty
     */
    public boolean deleteAttachmentsForMaterial(@NonNull String materialId) {
        validateMaterialId(materialId);

        lock.writeLock().lock();
        try {
            File materialDir = getMaterialDirectory(materialId);
            if (!materialDir.exists()) {
                return true;
            }
            return deleteDirectoryRecursively(materialDir);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Clears all attachment files from internal storage.
     *
     * <p>This performs a full cache clear of all stored attachments.</p>
     *
     * @return true if all attachments were successfully cleared, false otherwise
     */
    public boolean clearAllAttachments() {
        lock.writeLock().lock();
        try {
            File attachmentsDir = getAttachmentsRootDirectory();
            if (!attachmentsDir.exists()) {
                return true;
            }
            return deleteDirectoryRecursively(attachmentsDir);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Gets the root directory for all attachments.
     *
     * @return The attachments root directory
     */
    @NonNull
    File getAttachmentsRootDirectory() {
        return new File(baseDirectory, ATTACHMENTS_DIR);
    }

    /**
     * Gets the directory for a specific material's attachments.
     *
     * @param materialId The material identifier
     * @return The material-specific directory
     */
    @NonNull
    private File getMaterialDirectory(@NonNull String materialId) {
        return new File(getAttachmentsRootDirectory(), materialId);
    }

    /**
     * Recursively deletes a directory and all its contents.
     *
     * @param directory The directory to delete
     * @return true if deletion was successful, false otherwise
     */
    private boolean deleteDirectoryRecursively(@NonNull File directory) {
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    if (!deleteDirectoryRecursively(file)) {
                        return false;
                    }
                } else {
                    if (!deleteFile(file)) {
                        return false;
                    }
                }
            }
        }
        return deleteFile(directory);
    }

    /**
     * Creates a directory. This method is protected to allow testing of failure scenarios.
     *
     * @param directory The directory to create
     * @return true if the directory was created successfully, false otherwise
     */
    protected boolean createDirectory(@NonNull File directory) {
        return directory.mkdirs();
    }

    /**
     * Writes bytes to a file. This method is protected to allow testing of failure scenarios.
     *
     * @param file  The file to write to
     * @param bytes The bytes to write
     * @return true if the write was successful, false otherwise
     */
    protected boolean writeToFile(@NonNull File file, @NonNull byte[] bytes) {
        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(bytes);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Deletes a file or directory. This method is protected to allow testing of failure scenarios.
     *
     * @param file The file or directory to delete
     * @return true if the deletion was successful, false otherwise
     */
    protected boolean deleteFile(@NonNull File file) {
        return file.delete();
    }

    /**
     * Validates that the material ID is not null or empty and does not contain path traversal.
     *
     * @param materialId The material ID to validate
     * @throws IllegalArgumentException if materialId is null, empty, or contains path traversal
     */
    private void validateMaterialId(@NonNull String materialId) {
        if (materialId == null || materialId.trim().isEmpty()) {
            throw new IllegalArgumentException("Material ID cannot be null or empty");
        }
        if (containsPathTraversalCharacters(materialId)) {
            throw new IllegalArgumentException("Material ID contains invalid path characters");
        }
    }

    /**
     * Validates that the attachment ID is not null or empty and does not contain path traversal.
     *
     * @param attachmentId The attachment ID to validate
     * @throws IllegalArgumentException if attachmentId is null, empty, or contains path traversal
     */
    private void validateAttachmentId(@NonNull String attachmentId) {
        if (attachmentId == null || attachmentId.trim().isEmpty()) {
            throw new IllegalArgumentException("Attachment ID cannot be null or empty");
        }
        if (containsPathTraversalCharacters(attachmentId)) {
            throw new IllegalArgumentException("Attachment ID contains invalid path characters");
        }
    }

    /**
     * Validates that the extension is not null or empty and does not contain path traversal.
     *
     * @param extension The extension to validate
     * @throws IllegalArgumentException if extension is null, empty, or contains path traversal
     */
    private void validateExtension(@NonNull String extension) {
        if (extension == null || extension.trim().isEmpty()) {
            throw new IllegalArgumentException("Extension cannot be null or empty");
        }
        if (containsPathTraversalCharacters(extension)) {
            throw new IllegalArgumentException("Extension contains invalid path characters");
        }
    }

    /**
     * Checks if the input string contains path traversal characters.
     *
     * @param input The string to check
     * @return true if the string contains path traversal characters, false otherwise
     */
    private boolean containsPathTraversalCharacters(@NonNull String input) {
        return input.contains("..") || input.contains("/") || input.contains("\\");
    }
}
