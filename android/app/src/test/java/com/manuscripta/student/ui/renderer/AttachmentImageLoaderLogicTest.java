package com.manuscripta.student.ui.renderer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.graphics.Bitmap;

import com.manuscripta.student.network.ApiService;
import com.manuscripta.student.utils.FileStorageManager;

import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutorService;

import okhttp3.MediaType;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Response;

/**
 * Pure JUnit tests for the loading logic in
 * {@link AttachmentImageLoader}. Overrides the Android-specific
 * decode methods so that no Robolectric runner is required, which
 * ensures JaCoCo instruments the class under test.
 */
public class AttachmentImageLoaderLogicTest {

    private static final String ATTACHMENT_ID = "att-123";
    private static final String MATERIAL_ID = "mat-456";

    private ApiService apiService;
    private FileStorageManager fileStorageManager;
    private ExecutorService executor;
    private Bitmap fakeBitmap;
    private AttachmentImageLoader loader;

    /**
     * Sets up mocks and a testable loader subclass before each
     * test.
     */
    @Before
    public void setUp() {
        apiService = mock(ApiService.class);
        fileStorageManager = mock(FileStorageManager.class);
        executor = mock(ExecutorService.class);
        fakeBitmap = mock(Bitmap.class);

        loader = new AttachmentImageLoader(
                apiService, fileStorageManager, executor) {
            @Override
            Bitmap decodeBitmapFromFile(String path) {
                return fakeBitmap;
            }

            @Override
            Bitmap decodeBitmapFromBytes(byte[] data) {
                return fakeBitmap;
            }
        };
    }

    // ============= loadFromCache tests =============

    /**
     * When a cached file exists, loadFromCache returns a Bitmap.
     */
    @Test
    public void loadFromCache_fileExists_returnsBitmap() {
        File mockFile = mock(File.class);
        when(mockFile.exists()).thenReturn(true);
        when(mockFile.getAbsolutePath())
                .thenReturn("/fake/path");
        when(fileStorageManager.getAttachmentFile(
                MATERIAL_ID, ATTACHMENT_ID))
                .thenReturn(mockFile);

        Bitmap result = loader.loadFromCache(
                ATTACHMENT_ID, MATERIAL_ID);

        assertNotNull(result);
        assertEquals(fakeBitmap, result);
    }

    /**
     * When no cached file exists, loadFromCache returns null.
     */
    @Test
    public void loadFromCache_noFile_returnsNull() {
        when(fileStorageManager.getAttachmentFile(
                MATERIAL_ID, ATTACHMENT_ID))
                .thenReturn(null);

        Bitmap result = loader.loadFromCache(
                ATTACHMENT_ID, MATERIAL_ID);

        assertNull(result);
    }

    /**
     * When the cached file object exists but file.exists() is
     * false, loadFromCache returns null.
     */
    @Test
    public void loadFromCache_fileDoesNotExist_returnsNull() {
        File mockFile = mock(File.class);
        when(mockFile.exists()).thenReturn(false);
        when(fileStorageManager.getAttachmentFile(
                MATERIAL_ID, ATTACHMENT_ID))
                .thenReturn(mockFile);

        Bitmap result = loader.loadFromCache(
                ATTACHMENT_ID, MATERIAL_ID);

        assertNull(result);
    }

    // ============= loadFromNetwork tests =============

    /**
     * Successful network response returns a Bitmap and caches
     * it.
     *
     * @throws IOException if the mock call fails
     */
    @SuppressWarnings("unchecked")
    @Test
    public void loadFromNetwork_success_returnsBitmapAndCaches()
            throws IOException {
        byte[] imageBytes = new byte[]{1, 2, 3};
        ResponseBody body = ResponseBody.create(
                MediaType.parse("image/png"), imageBytes);
        Response<ResponseBody> response =
                Response.success(body);

        Call<ResponseBody> call = mock(Call.class);
        when(call.execute()).thenReturn(response);
        when(apiService.getAttachment(ATTACHMENT_ID))
                .thenReturn(call);

        Bitmap result = loader.loadFromNetwork(
                ATTACHMENT_ID, MATERIAL_ID);

        assertNotNull(result);
        verify(fileStorageManager).saveAttachment(
                eq(MATERIAL_ID), eq(ATTACHMENT_ID),
                eq("img"), eq(imageBytes));
    }

    /**
     * Failed network response returns null.
     *
     * @throws IOException if the mock call fails
     */
    @SuppressWarnings("unchecked")
    @Test
    public void loadFromNetwork_failure_returnsNull()
            throws IOException {
        Response<ResponseBody> response =
                Response.error(404, ResponseBody.create(
                        MediaType.parse("text/plain"),
                        "Not found"));

        Call<ResponseBody> call = mock(Call.class);
        when(call.execute()).thenReturn(response);
        when(apiService.getAttachment(ATTACHMENT_ID))
                .thenReturn(call);

        Bitmap result = loader.loadFromNetwork(
                ATTACHMENT_ID, MATERIAL_ID);

        assertNull(result);
    }

    /**
     * IOException during network call returns null.
     *
     * @throws IOException if the mock call fails
     */
    @SuppressWarnings("unchecked")
    @Test
    public void loadFromNetwork_ioException_returnsNull()
            throws IOException {
        Call<ResponseBody> call = mock(Call.class);
        when(call.execute()).thenThrow(new IOException("fail"));
        when(apiService.getAttachment(ATTACHMENT_ID))
                .thenReturn(call);

        Bitmap result = loader.loadFromNetwork(
                ATTACHMENT_ID, MATERIAL_ID);

        assertNull(result);
    }

    // ============= loadBitmap tests =============

    /**
     * loadBitmap returns cached bitmap when available.
     */
    @Test
    public void loadBitmap_cached_returnsFromCache() {
        File mockFile = mock(File.class);
        when(mockFile.exists()).thenReturn(true);
        when(mockFile.getAbsolutePath())
                .thenReturn("/fake/path");
        when(fileStorageManager.getAttachmentFile(
                MATERIAL_ID, ATTACHMENT_ID))
                .thenReturn(mockFile);

        Bitmap result = loader.loadBitmap(
                ATTACHMENT_ID, MATERIAL_ID);

        assertNotNull(result);
        assertEquals(fakeBitmap, result);
    }

    /**
     * loadBitmap falls back to network when cache misses.
     *
     * @throws IOException if the mock call fails
     */
    @SuppressWarnings("unchecked")
    @Test
    public void loadBitmap_notCached_fallsBackToNetwork()
            throws IOException {
        when(fileStorageManager.getAttachmentFile(
                MATERIAL_ID, ATTACHMENT_ID))
                .thenReturn(null);

        byte[] imageBytes = new byte[]{1, 2, 3};
        ResponseBody body = ResponseBody.create(
                MediaType.parse("image/png"), imageBytes);
        Response<ResponseBody> response =
                Response.success(body);

        Call<ResponseBody> call = mock(Call.class);
        when(call.execute()).thenReturn(response);
        when(apiService.getAttachment(ATTACHMENT_ID))
                .thenReturn(call);

        Bitmap result = loader.loadBitmap(
                ATTACHMENT_ID, MATERIAL_ID);

        assertNotNull(result);
    }

    // ============= shutdown tests =============

    /**
     * shutdown delegates to the executor.
     */
    @Test
    public void shutdown_delegatesToExecutor() {
        loader.shutdown();
        verify(executor).shutdown();
    }
}
