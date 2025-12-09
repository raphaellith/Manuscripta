package com.manuscripta.student.utils;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Unit tests for {@link FileStorageManager}.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28, manifest = Config.NONE)
public class FileStorageManagerTest {

    private FileStorageManager storageManager;
    private Context context;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        storageManager = new FileStorageManager(context);
    }

    @After
    public void tearDown() {
        // Clean up any test files
        storageManager.clearAllAttachments();
    }

    // ==================== Constructor Tests ====================

    @Test
    public void testConstructor_validContext_createsInstance() {
        FileStorageManager manager = new FileStorageManager(context);
        assertNotNull(manager);
    }

    @Test
    public void testConstructor_nullContext_throwsException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new FileStorageManager(null)
        );
        assertEquals("Context cannot be null", exception.getMessage());
    }

    // ==================== saveAttachment Tests ====================

    @Test
    public void testSaveAttachment_validInput_savesFileSuccessfully() throws IOException {
        byte[] content = "Test content".getBytes();
        File savedFile = storageManager.saveAttachment("material-1", "attachment-1", "txt", content);

        assertNotNull(savedFile);
        assertTrue(savedFile.exists());
        assertEquals("attachment-1.txt", savedFile.getName());

        // Verify content
        byte[] readContent = readFileContent(savedFile);
        assertArrayEquals(content, readContent);
    }

    @Test
    public void testSaveAttachment_pdfFile_savesSuccessfully() {
        byte[] pdfContent = new byte[]{0x25, 0x50, 0x44, 0x46}; // PDF magic bytes
        File savedFile = storageManager.saveAttachment("material-2", "document", "pdf", pdfContent);

        assertNotNull(savedFile);
        assertTrue(savedFile.exists());
        assertEquals("document.pdf", savedFile.getName());
    }

    @Test
    public void testSaveAttachment_imageFile_savesSuccessfully() {
        byte[] imageContent = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47}; // PNG magic bytes
        File savedFile = storageManager.saveAttachment("material-3", "image", "png", imageContent);

        assertNotNull(savedFile);
        assertTrue(savedFile.exists());
        assertEquals("image.png", savedFile.getName());
    }

    @Test
    public void testSaveAttachment_emptyBytes_savesEmptyFile() {
        byte[] emptyContent = new byte[0];
        File savedFile = storageManager.saveAttachment("material-4", "empty", "txt", emptyContent);

        assertNotNull(savedFile);
        assertTrue(savedFile.exists());
        assertEquals(0, savedFile.length());
    }

    @Test
    public void testSaveAttachment_largeFile_savesSuccessfully() {
        byte[] largeContent = new byte[1024 * 1024]; // 1MB
        for (int i = 0; i < largeContent.length; i++) {
            largeContent[i] = (byte) (i % 256);
        }
        File savedFile = storageManager.saveAttachment("material-5", "large", "bin", largeContent);

        assertNotNull(savedFile);
        assertTrue(savedFile.exists());
        assertEquals(largeContent.length, savedFile.length());
    }

    @Test
    public void testSaveAttachment_sameMaterialMultipleAttachments_savesAll() {
        File file1 = storageManager.saveAttachment("material-6", "attach-1", "pdf", "content1".getBytes());
        File file2 = storageManager.saveAttachment("material-6", "attach-2", "png", "content2".getBytes());
        File file3 = storageManager.saveAttachment("material-6", "attach-3", "txt", "content3".getBytes());

        assertNotNull(file1);
        assertNotNull(file2);
        assertNotNull(file3);
        assertTrue(file1.exists());
        assertTrue(file2.exists());
        assertTrue(file3.exists());
    }

    @Test
    public void testSaveAttachment_overwriteExisting_replacesFile() throws IOException {
        byte[] originalContent = "Original".getBytes();
        byte[] newContent = "New content".getBytes();

        File originalFile = storageManager.saveAttachment("material-7", "attach", "txt", originalContent);
        assertNotNull(originalFile);
        assertArrayEquals(originalContent, readFileContent(originalFile));

        File newFile = storageManager.saveAttachment("material-7", "attach", "txt", newContent);
        assertNotNull(newFile);
        assertArrayEquals(newContent, readFileContent(newFile));
    }

    @Test
    public void testSaveAttachment_nullMaterialId_throwsException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> storageManager.saveAttachment(null, "attach", "txt", "content".getBytes())
        );
        assertEquals("Material ID cannot be null or empty", exception.getMessage());
    }

    @Test
    public void testSaveAttachment_emptyMaterialId_throwsException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> storageManager.saveAttachment("", "attach", "txt", "content".getBytes())
        );
        assertEquals("Material ID cannot be null or empty", exception.getMessage());
    }

    @Test
    public void testSaveAttachment_blankMaterialId_throwsException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> storageManager.saveAttachment("   ", "attach", "txt", "content".getBytes())
        );
        assertEquals("Material ID cannot be null or empty", exception.getMessage());
    }

    @Test
    public void testSaveAttachment_nullAttachmentId_throwsException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> storageManager.saveAttachment("material", null, "txt", "content".getBytes())
        );
        assertEquals("Attachment ID cannot be null or empty", exception.getMessage());
    }

    @Test
    public void testSaveAttachment_emptyAttachmentId_throwsException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> storageManager.saveAttachment("material", "", "txt", "content".getBytes())
        );
        assertEquals("Attachment ID cannot be null or empty", exception.getMessage());
    }

    @Test
    public void testSaveAttachment_blankAttachmentId_throwsException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> storageManager.saveAttachment("material", "   ", "txt", "content".getBytes())
        );
        assertEquals("Attachment ID cannot be null or empty", exception.getMessage());
    }

    @Test
    public void testSaveAttachment_nullExtension_throwsException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> storageManager.saveAttachment("material", "attach", null, "content".getBytes())
        );
        assertEquals("Extension cannot be null or empty", exception.getMessage());
    }

    @Test
    public void testSaveAttachment_emptyExtension_throwsException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> storageManager.saveAttachment("material", "attach", "", "content".getBytes())
        );
        assertEquals("Extension cannot be null or empty", exception.getMessage());
    }

    @Test
    public void testSaveAttachment_blankExtension_throwsException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> storageManager.saveAttachment("material", "attach", "   ", "content".getBytes())
        );
        assertEquals("Extension cannot be null or empty", exception.getMessage());
    }

    @Test
    public void testSaveAttachment_nullBytes_throwsException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> storageManager.saveAttachment("material", "attach", "txt", null)
        );
        assertEquals("Bytes cannot be null", exception.getMessage());
    }

    // ==================== getAttachmentFile Tests ====================

    @Test
    public void testGetAttachmentFile_existingFile_returnsFile() {
        byte[] content = "Test content".getBytes();
        storageManager.saveAttachment("material-get-1", "attach-1", "txt", content);

        File retrieved = storageManager.getAttachmentFile("material-get-1", "attach-1");

        assertNotNull(retrieved);
        assertTrue(retrieved.exists());
        assertEquals("attach-1.txt", retrieved.getName());
    }

    @Test
    public void testGetAttachmentFile_nonExistentMaterial_returnsNull() {
        File retrieved = storageManager.getAttachmentFile("non-existent-material", "attach-1");
        assertNull(retrieved);
    }

    @Test
    public void testGetAttachmentFile_nonExistentAttachment_returnsNull() {
        storageManager.saveAttachment("material-get-2", "existing-attach", "txt", "content".getBytes());

        File retrieved = storageManager.getAttachmentFile("material-get-2", "non-existent");
        assertNull(retrieved);
    }

    @Test
    public void testGetAttachmentFile_multipleAttachments_returnsCorrectFile() {
        storageManager.saveAttachment("material-get-3", "attach-a", "pdf", "pdf content".getBytes());
        storageManager.saveAttachment("material-get-3", "attach-b", "png", "png content".getBytes());
        storageManager.saveAttachment("material-get-3", "attach-c", "txt", "txt content".getBytes());

        File fileA = storageManager.getAttachmentFile("material-get-3", "attach-a");
        File fileB = storageManager.getAttachmentFile("material-get-3", "attach-b");
        File fileC = storageManager.getAttachmentFile("material-get-3", "attach-c");

        assertNotNull(fileA);
        assertNotNull(fileB);
        assertNotNull(fileC);
        assertEquals("attach-a.pdf", fileA.getName());
        assertEquals("attach-b.png", fileB.getName());
        assertEquals("attach-c.txt", fileC.getName());
    }

    @Test
    public void testGetAttachmentFile_nullMaterialId_throwsException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> storageManager.getAttachmentFile(null, "attach")
        );
        assertEquals("Material ID cannot be null or empty", exception.getMessage());
    }

    @Test
    public void testGetAttachmentFile_emptyMaterialId_throwsException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> storageManager.getAttachmentFile("", "attach")
        );
        assertEquals("Material ID cannot be null or empty", exception.getMessage());
    }

    @Test
    public void testGetAttachmentFile_blankMaterialId_throwsException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> storageManager.getAttachmentFile("   ", "attach")
        );
        assertEquals("Material ID cannot be null or empty", exception.getMessage());
    }

    @Test
    public void testGetAttachmentFile_nullAttachmentId_throwsException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> storageManager.getAttachmentFile("material", null)
        );
        assertEquals("Attachment ID cannot be null or empty", exception.getMessage());
    }

    @Test
    public void testGetAttachmentFile_emptyAttachmentId_throwsException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> storageManager.getAttachmentFile("material", "")
        );
        assertEquals("Attachment ID cannot be null or empty", exception.getMessage());
    }

    @Test
    public void testGetAttachmentFile_blankAttachmentId_throwsException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> storageManager.getAttachmentFile("material", "   ")
        );
        assertEquals("Attachment ID cannot be null or empty", exception.getMessage());
    }

    // ==================== deleteAttachmentsForMaterial Tests ====================

    @Test
    public void testDeleteAttachmentsForMaterial_existingMaterial_deletesAll() {
        storageManager.saveAttachment("material-del-1", "attach-1", "txt", "content1".getBytes());
        storageManager.saveAttachment("material-del-1", "attach-2", "pdf", "content2".getBytes());

        boolean result = storageManager.deleteAttachmentsForMaterial("material-del-1");

        assertTrue(result);
        assertNull(storageManager.getAttachmentFile("material-del-1", "attach-1"));
        assertNull(storageManager.getAttachmentFile("material-del-1", "attach-2"));
    }

    @Test
    public void testDeleteAttachmentsForMaterial_nonExistentMaterial_returnsTrue() {
        boolean result = storageManager.deleteAttachmentsForMaterial("non-existent-material");
        assertTrue(result);
    }

    @Test
    public void testDeleteAttachmentsForMaterial_doesNotAffectOtherMaterials() {
        storageManager.saveAttachment("material-del-2", "attach-1", "txt", "content1".getBytes());
        storageManager.saveAttachment("material-del-3", "attach-1", "txt", "content2".getBytes());

        storageManager.deleteAttachmentsForMaterial("material-del-2");

        assertNull(storageManager.getAttachmentFile("material-del-2", "attach-1"));
        assertNotNull(storageManager.getAttachmentFile("material-del-3", "attach-1"));
    }

    @Test
    public void testDeleteAttachmentsForMaterial_nullMaterialId_throwsException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> storageManager.deleteAttachmentsForMaterial(null)
        );
        assertEquals("Material ID cannot be null or empty", exception.getMessage());
    }

    @Test
    public void testDeleteAttachmentsForMaterial_emptyMaterialId_throwsException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> storageManager.deleteAttachmentsForMaterial("")
        );
        assertEquals("Material ID cannot be null or empty", exception.getMessage());
    }

    @Test
    public void testDeleteAttachmentsForMaterial_blankMaterialId_throwsException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> storageManager.deleteAttachmentsForMaterial("   ")
        );
        assertEquals("Material ID cannot be null or empty", exception.getMessage());
    }

    // ==================== clearAllAttachments Tests ====================

    @Test
    public void testClearAllAttachments_withAttachments_clearsAll() {
        storageManager.saveAttachment("material-clear-1", "attach-1", "txt", "content1".getBytes());
        storageManager.saveAttachment("material-clear-2", "attach-1", "pdf", "content2".getBytes());
        storageManager.saveAttachment("material-clear-3", "attach-1", "png", "content3".getBytes());

        boolean result = storageManager.clearAllAttachments();

        assertTrue(result);
        assertNull(storageManager.getAttachmentFile("material-clear-1", "attach-1"));
        assertNull(storageManager.getAttachmentFile("material-clear-2", "attach-1"));
        assertNull(storageManager.getAttachmentFile("material-clear-3", "attach-1"));
    }

    @Test
    public void testClearAllAttachments_emptyStorage_returnsTrue() {
        // Ensure storage is empty first
        storageManager.clearAllAttachments();

        boolean result = storageManager.clearAllAttachments();
        assertTrue(result);
    }

    @Test
    public void testClearAllAttachments_verifyDirectoryDeleted() {
        storageManager.saveAttachment("material-clear-4", "attach-1", "txt", "content".getBytes());

        File attachmentsDir = storageManager.getAttachmentsRootDirectory();
        assertTrue(attachmentsDir.exists());

        storageManager.clearAllAttachments();
        assertFalse(attachmentsDir.exists());
    }

    // ==================== getAttachmentsRootDirectory Tests ====================

    @Test
    public void testGetAttachmentsRootDirectory_returnsCorrectPath() {
        File rootDir = storageManager.getAttachmentsRootDirectory();

        assertNotNull(rootDir);
        assertTrue(rootDir.getPath().contains("attachments"));
    }

    // ==================== Thread Safety Tests ====================

    @Test
    public void testConcurrentSaveOperations_allSucceed() throws InterruptedException {
        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            executor.submit(() -> {
                try {
                    File saved = storageManager.saveAttachment(
                            "concurrent-material",
                            "attach-" + index,
                            "txt",
                            ("content-" + index).getBytes()
                    );
                    if (saved != null && saved.exists()) {
                        successCount.incrementAndGet();
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        assertTrue(latch.await(10, TimeUnit.SECONDS));
        executor.shutdown();
        assertEquals(threadCount, successCount.get());
    }

    @Test
    public void testConcurrentReadWriteOperations_noExceptions() throws InterruptedException {
        // Pre-populate with some data
        for (int i = 0; i < 5; i++) {
            storageManager.saveAttachment("rw-material", "attach-" + i, "txt", "content".getBytes());
        }

        int threadCount = 20;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        List<Exception> exceptions = new ArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            executor.submit(() -> {
                try {
                    if (index % 2 == 0) {
                        // Write operation
                        storageManager.saveAttachment(
                                "rw-material",
                                "new-attach-" + index,
                                "txt",
                                "new content".getBytes()
                        );
                    } else {
                        // Read operation
                        storageManager.getAttachmentFile("rw-material", "attach-" + (index % 5));
                    }
                } catch (Exception e) {
                    synchronized (exceptions) {
                        exceptions.add(e);
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        assertTrue(latch.await(10, TimeUnit.SECONDS));
        executor.shutdown();
        assertTrue("Concurrent operations should not throw exceptions", exceptions.isEmpty());
    }

    @Test
    public void testConcurrentDeleteOperations_noExceptions() throws InterruptedException {
        // Pre-populate with data
        for (int i = 0; i < 10; i++) {
            storageManager.saveAttachment("del-material-" + i, "attach-1", "txt", "content".getBytes());
        }

        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        List<Exception> exceptions = new ArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            executor.submit(() -> {
                try {
                    storageManager.deleteAttachmentsForMaterial("del-material-" + index);
                } catch (Exception e) {
                    synchronized (exceptions) {
                        exceptions.add(e);
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        assertTrue(latch.await(10, TimeUnit.SECONDS));
        executor.shutdown();
        assertTrue("Concurrent delete operations should not throw exceptions", exceptions.isEmpty());
    }

    // ==================== File Path Structure Tests ====================

    @Test
    public void testFilePathStructure_followsPredictablePattern() {
        File savedFile = storageManager.saveAttachment("my-material-id", "my-attachment-id", "pdf", "content".getBytes());

        assertNotNull(savedFile);
        String path = savedFile.getPath();
        assertTrue("Path should contain 'attachments' directory", path.contains("attachments"));
        assertTrue("Path should contain material ID", path.contains("my-material-id"));
        assertTrue("Path should contain attachment filename", path.contains("my-attachment-id.pdf"));
    }

    @Test
    public void testFilePathStructure_materialIdAsSubdirectory() {
        File savedFile = storageManager.saveAttachment("material-path-test", "attach", "txt", "content".getBytes());

        assertNotNull(savedFile);
        File parent = savedFile.getParentFile();
        assertNotNull(parent);
        assertEquals("material-path-test", parent.getName());
    }

    // ==================== Helper Methods ====================

    private byte[] readFileContent(File file) throws IOException {
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] content = new byte[(int) file.length()];
            fis.read(content);
            return content;
        }
    }
}
