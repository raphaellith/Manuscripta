package com.manuscripta.student.ui.renderer;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.manuscripta.student.network.ApiService;
import com.manuscripta.student.utils.FileStorageManager;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.ResponseBody;
import retrofit2.Response;

/**
 * Loads attachment images referenced in material content.
 *
 * <p>Resolves {@code /attachments/{id}} references by checking the
 * local cache first (via {@link FileStorageManager}) and falling back
 * to the network (via {@link ApiService}) as specified in Material
 * Encoding §3.</p>
 */
public class AttachmentImageLoader {

    /** Tag for logging. */
    private static final String TAG = "AttachmentImageLoader";

    /** The API service for downloading attachments. */
    @NonNull
    private final ApiService apiService;

    /** The file storage manager for cached attachments. */
    @NonNull
    private final FileStorageManager fileStorageManager;

    /** Executor for background image loading. */
    @NonNull
    private final ExecutorService executor;

    /**
     * Creates a new AttachmentImageLoader with a default executor.
     *
     * @param apiService         the API service for network requests
     * @param fileStorageManager the file storage manager for cache
     */
    public AttachmentImageLoader(
            @NonNull ApiService apiService,
            @NonNull FileStorageManager fileStorageManager) {
        this(apiService, fileStorageManager,
                Executors.newFixedThreadPool(2));
    }

    /**
     * Creates a new AttachmentImageLoader with a custom executor.
     * Primarily for testing.
     *
     * @param apiService         the API service for network requests
     * @param fileStorageManager the file storage manager for cache
     * @param executor           the executor for background tasks
     */
    AttachmentImageLoader(
            @NonNull ApiService apiService,
            @NonNull FileStorageManager fileStorageManager,
            @NonNull ExecutorService executor) {
        this.apiService = apiService;
        this.fileStorageManager = fileStorageManager;
        this.executor = executor;
    }

    /**
     * Loads an attachment image into the specified ImageView.
     *
     * <p>First checks the local cache via FileStorageManager. If not
     * found, downloads from the server via ApiService and caches
     * the result. Loading runs on a background thread.</p>
     *
     * @param attachmentId the UUID of the attachment to load
     * @param materialId   the UUID of the parent material
     * @param imageView    the ImageView to display the image in
     */
    public void loadImage(
            @NonNull String attachmentId,
            @NonNull String materialId,
            @NonNull ImageView imageView) {
        Log.d(TAG, "loadImage called: attachment="
                + attachmentId + " material=" + materialId);
        executor.execute(() -> {
            Bitmap bitmap = loadBitmap(
                    attachmentId, materialId);
            if (bitmap != null) {
                Log.d(TAG, "Loaded bitmap "
                        + bitmap.getWidth() + "x"
                        + bitmap.getHeight()
                        + " for " + attachmentId);
                imageView.post(
                        () -> imageView.setImageBitmap(bitmap));
            } else {
                Log.w(TAG, "Failed to load bitmap for "
                        + attachmentId);
            }
        });
    }

    /**
     * Loads a bitmap from local cache or network.
     *
     * @param attachmentId the attachment UUID
     * @param materialId   the material UUID
     * @return the loaded Bitmap, or null if unavailable
     */
    @Nullable
    Bitmap loadBitmap(
            @NonNull String attachmentId,
            @NonNull String materialId) {
        Bitmap cached = loadFromCache(
                attachmentId, materialId);
        if (cached != null) {
            return cached;
        }
        return loadFromNetwork(attachmentId, materialId);
    }

    /**
     * Attempts to load an image from the local file cache.
     *
     * @param attachmentId the attachment UUID
     * @param materialId   the material UUID
     * @return the cached Bitmap, or null if not cached
     */
    @Nullable
    Bitmap loadFromCache(
            @NonNull String attachmentId,
            @NonNull String materialId) {
        File file = fileStorageManager.getAttachmentFile(
                materialId, attachmentId);
        Log.d(TAG, "loadFromCache: file="
                + (file != null ? file.getAbsolutePath()
                        : "null")
                + " exists="
                + (file != null && file.exists()));
        if (file != null && file.exists()) {
            return decodeBitmapFromFile(
                    file.getAbsolutePath());
        }
        return null;
    }

    /**
     * Downloads an image from the server and caches it locally.
     *
     * @param attachmentId the attachment UUID
     * @param materialId   the material UUID
     * @return the downloaded Bitmap, or null on failure
     */
    @Nullable
    Bitmap loadFromNetwork(
            @NonNull String attachmentId,
            @NonNull String materialId) {
        try {
            Log.d(TAG, "loadFromNetwork: fetching "
                    + attachmentId);
            Response<ResponseBody> response =
                    apiService.getAttachment(attachmentId)
                            .execute();
            Log.d(TAG, "loadFromNetwork: HTTP "
                    + response.code()
                    + " body=" + (response.body() != null));
            if (response.isSuccessful()
                    && response.body() != null) {
                byte[] bytes = response.body().bytes();
                Log.d(TAG, "loadFromNetwork: got "
                        + bytes.length + " bytes");
                fileStorageManager.saveAttachment(
                        materialId, attachmentId,
                        "img", bytes);
                return decodeBitmapFromBytes(bytes);
            }
        } catch (IOException e) {
            Log.e(TAG, "loadFromNetwork failed: "
                    + e.getMessage(), e);
        }
        return null;
    }

    /**
     * Decodes a bitmap from a file path.
     * Package-private to allow test subclasses to override.
     *
     * @param path the file path to decode
     * @return the decoded Bitmap, or null on failure
     */
    @Nullable
    Bitmap decodeBitmapFromFile(@NonNull String path) {
        return BitmapFactory.decodeFile(path);
    }

    /**
     * Decodes a bitmap from a byte array.
     * Package-private to allow test subclasses to override.
     *
     * @param data the byte array to decode
     * @return the decoded Bitmap, or null on failure
     */
    @Nullable
    Bitmap decodeBitmapFromBytes(@NonNull byte[] data) {
        return BitmapFactory.decodeByteArray(
                data, 0, data.length);
    }

    /**
     * Shuts down the background executor.
     * Should be called when the loader is no longer needed.
     */
    public void shutdown() {
        executor.shutdown();
    }
}
