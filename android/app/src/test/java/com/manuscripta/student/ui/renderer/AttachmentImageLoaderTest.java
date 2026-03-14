package com.manuscripta.student.ui.renderer;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.graphics.Bitmap;
import android.widget.ImageView;

import com.manuscripta.student.network.ApiService;
import com.manuscripta.student.utils.FileStorageManager;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutorService;

import okhttp3.MediaType;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Response;

/**
 * Unit tests for {@link AttachmentImageLoader}.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28, manifest = Config.NONE)
public class AttachmentImageLoaderTest {

    @Mock
    private ApiService apiService;

    @Mock
    private FileStorageManager fileStorageManager;

    @Mock
    private ExecutorService executor;

    private AttachmentImageLoader loader;
    private Bitmap fakeBitmap;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        fakeBitmap = Bitmap.createBitmap(
                1, 1, Bitmap.Config.ARGB_8888);

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

    // ==================== loadImage tests ====================

    @Test
    public void testLoadImage_executesOnProvidedExecutor() {
        ImageView imageView = mock(ImageView.class);

        loader.loadImage("att-1", "mat-1", imageView);

        verify(executor).execute(any(Runnable.class));
    }

    @Test
    public void testLoadImage_runnableLoadsBitmap() {
        ImageView imageView = mock(ImageView.class);
        File cachedFile = mock(File.class);
        when(cachedFile.exists()).thenReturn(true);
        when(cachedFile.getAbsolutePath())
                .thenReturn("/fake/path");
        when(fileStorageManager.getAttachmentFile(
                "mat-1", "att-1")).thenReturn(cachedFile);

        doAnswer(invocation -> {
            Runnable runnable = invocation.getArgument(0);
            runnable.run();
            return null;
        }).when(executor).execute(any(Runnable.class));

        loader.loadImage("att-1", "mat-1", imageView);

        verify(imageView).post(any(Runnable.class));
    }

    // ==================== loadBitmap tests ====================

    @Test
    public void testLoadBitmap_cacheHit_returnsFromCache() {
        File cachedFile = mock(File.class);
        when(cachedFile.exists()).thenReturn(true);
        when(cachedFile.getAbsolutePath())
                .thenReturn("/fake/path");
        when(fileStorageManager.getAttachmentFile(
                "mat-1", "att-1")).thenReturn(cachedFile);

        Bitmap result = loader.loadBitmap("att-1", "mat-1");

        assertNotNull(result);
        verify(apiService, never())
                .getAttachment(anyString());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testLoadBitmap_cacheMiss_fallsBackToNetwork()
            throws IOException {
        when(fileStorageManager.getAttachmentFile(
                "mat-1", "att-1")).thenReturn(null);

        ResponseBody body = mock(ResponseBody.class);
        when(body.bytes()).thenReturn(new byte[]{1, 2, 3});

        Call<ResponseBody> call = mock(Call.class);
        when(call.execute())
                .thenReturn(Response.success(body));
        when(apiService.getAttachment("att-1"))
                .thenReturn(call);

        Bitmap result = loader.loadBitmap("att-1", "mat-1");

        assertNotNull(result);
        verify(fileStorageManager).saveAttachment(
                eq("mat-1"), eq("att-1"),
                eq("img"), any(byte[].class));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testLoadBitmap_bothFail_returnsNull()
            throws IOException {
        when(fileStorageManager.getAttachmentFile(
                "mat-1", "att-1")).thenReturn(null);

        Call<ResponseBody> call = mock(Call.class);
        when(call.execute()).thenThrow(
                new IOException("Network error"));
        when(apiService.getAttachment("att-1"))
                .thenReturn(call);

        AttachmentImageLoader nullLoader =
                new AttachmentImageLoader(
                        apiService, fileStorageManager,
                        executor) {
            @Override
            Bitmap decodeBitmapFromFile(String path) {
                return null;
            }

            @Override
            Bitmap decodeBitmapFromBytes(byte[] data) {
                return null;
            }
        };

        Bitmap result =
                nullLoader.loadBitmap("att-1", "mat-1");

        assertNull(result);
    }

    // ==================== loadFromCache tests ====================

    @Test
    public void testLoadFromCache_fileExists_returnsBitmap() {
        File cachedFile = mock(File.class);
        when(cachedFile.exists()).thenReturn(true);
        when(cachedFile.getAbsolutePath())
                .thenReturn("/fake/path");
        when(fileStorageManager.getAttachmentFile(
                "mat-1", "att-1")).thenReturn(cachedFile);

        Bitmap result =
                loader.loadFromCache("att-1", "mat-1");

        assertNotNull(result);
    }

    @Test
    public void testLoadFromCache_fileNull_returnsNull() {
        when(fileStorageManager.getAttachmentFile(
                "mat-1", "att-1")).thenReturn(null);

        Bitmap result =
                loader.loadFromCache("att-1", "mat-1");

        assertNull(result);
    }

    @Test
    public void testLoadFromCache_fileDoesNotExist_returnsNull() {
        File cachedFile = mock(File.class);
        when(cachedFile.exists()).thenReturn(false);
        when(fileStorageManager.getAttachmentFile(
                "mat-1", "att-1")).thenReturn(cachedFile);

        Bitmap result =
                loader.loadFromCache("att-1", "mat-1");

        assertNull(result);
    }

    // ==================== loadFromNetwork tests ====================

    @Test
    @SuppressWarnings("unchecked")
    public void testLoadFromNetwork_success_cachesAndReturnsBitmap()
            throws IOException {
        ResponseBody body = mock(ResponseBody.class);
        when(body.bytes()).thenReturn(new byte[]{1, 2, 3});

        Call<ResponseBody> call = mock(Call.class);
        when(call.execute())
                .thenReturn(Response.success(body));
        when(apiService.getAttachment("att-1"))
                .thenReturn(call);

        Bitmap result =
                loader.loadFromNetwork("att-1", "mat-1");

        assertNotNull(result);
        verify(fileStorageManager).saveAttachment(
                eq("mat-1"), eq("att-1"),
                eq("img"), any(byte[].class));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testLoadFromNetwork_unsuccessfulResponse_returnsNull()
            throws IOException {
        Call<ResponseBody> call = mock(Call.class);
        when(call.execute()).thenReturn(
                Response.error(404,
                        ResponseBody.create(
                                MediaType.get("text/plain"),
                                "")));
        when(apiService.getAttachment("att-1"))
                .thenReturn(call);

        Bitmap result =
                loader.loadFromNetwork("att-1", "mat-1");

        assertNull(result);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testLoadFromNetwork_ioException_returnsNull()
            throws IOException {
        Call<ResponseBody> call = mock(Call.class);
        when(call.execute()).thenThrow(
                new IOException("Network error"));
        when(apiService.getAttachment("att-1"))
                .thenReturn(call);

        Bitmap result =
                loader.loadFromNetwork("att-1", "mat-1");

        assertNull(result);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testLoadFromNetwork_nullBody_returnsNull()
            throws IOException {
        Call<ResponseBody> call = mock(Call.class);
        when(call.execute())
                .thenReturn(Response.success(null));
        when(apiService.getAttachment("att-1"))
                .thenReturn(call);

        Bitmap result =
                loader.loadFromNetwork("att-1", "mat-1");

        assertNull(result);
    }

    // ==================== shutdown test ====================

    @Test
    public void testShutdown_callsExecutorShutdown() {
        loader.shutdown();
        verify(executor).shutdown();
    }

    // ==================== Constructor tests ====================

    @Test
    public void testDefaultConstructor_createsInstance() {
        AttachmentImageLoader defaultLoader =
                new AttachmentImageLoader(
                        apiService, fileStorageManager);
        assertNotNull(defaultLoader);
        defaultLoader.shutdown();
    }

    // ==================== decodeBitmap defaults ====================

    @Test
    public void testDecodeBitmapFromFile_defaultImpl_returnsValue() {
        AttachmentImageLoader defaultLoader =
                new AttachmentImageLoader(
                        apiService, fileStorageManager,
                        executor);
        // Robolectric shadow may return null or a Bitmap;
        // we just verify it does not throw
        defaultLoader.decodeBitmapFromFile(
                "/nonexistent/path.png");
    }

    @Test
    public void testDecodeBitmapFromBytes_defaultImpl_returnsValue() {
        AttachmentImageLoader defaultLoader =
                new AttachmentImageLoader(
                        apiService, fileStorageManager,
                        executor);
        // Robolectric shadow may return null or a Bitmap;
        // we just verify it does not throw
        defaultLoader.decodeBitmapFromBytes(
                new byte[]{0});
    }
}
