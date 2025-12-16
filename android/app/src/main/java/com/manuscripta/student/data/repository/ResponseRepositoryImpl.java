package com.manuscripta.student.data.repository;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

import com.manuscripta.student.data.local.ResponseDao;
import com.manuscripta.student.data.model.ResponseEntity;
import com.manuscripta.student.domain.mapper.ResponseMapper;
import com.manuscripta.student.domain.model.Response;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Implementation of {@link ResponseRepository} that manages student responses
 * with local storage and a sync queue for offline support.
 *
 * <p>Features:</p>
 * <ul>
 *   <li>Local persistence via Room DAO</li>
 *   <li>Sync queue for offline responses</li>
 *   <li>Exponential backoff retry logic for failed syncs</li>
 *   <li>Thread-safe operations</li>
 * </ul>
 */
@Singleton
public class ResponseRepositoryImpl implements ResponseRepository {

    /** Initial delay for exponential backoff in milliseconds. */
    @VisibleForTesting
    static final long INITIAL_BACKOFF_MS = 1000L;

    /** Maximum delay for exponential backoff in milliseconds. */
    @VisibleForTesting
    static final long MAX_BACKOFF_MS = 32000L;

    /** Maximum number of retry attempts. */
    @VisibleForTesting
    static final int MAX_RETRY_ATTEMPTS = 5;

    /** Backoff multiplier for exponential growth. */
    @VisibleForTesting
    static final double BACKOFF_MULTIPLIER = 2.0;

    /** The DAO for response persistence. */
    private final ResponseDao responseDao;

    /** Executor for background sync operations. */
    private final ExecutorService syncExecutor;

    /** Flag indicating whether a sync operation is in progress. */
    private final AtomicBoolean isSyncing;

    /** The sync engine for network operations. */
    private final SyncEngine syncEngine;

    /**
     * Creates a new ResponseRepositoryImpl with the given DAO.
     *
     * @param responseDao The DAO for response persistence
     */
    @Inject
    public ResponseRepositoryImpl(@NonNull ResponseDao responseDao) {
        this(responseDao, new DefaultSyncEngine());
    }

    /**
     * Creates a new ResponseRepositoryImpl with a custom sync engine.
     * This constructor is primarily for testing purposes.
     *
     * @param responseDao The DAO for response persistence
     * @param syncEngine  The sync engine for network operations
     */
    @VisibleForTesting
    ResponseRepositoryImpl(@NonNull ResponseDao responseDao, @NonNull SyncEngine syncEngine) {
        if (responseDao == null) {
            throw new IllegalArgumentException("ResponseDao cannot be null");
        }
        if (syncEngine == null) {
            throw new IllegalArgumentException("SyncEngine cannot be null");
        }
        this.responseDao = responseDao;
        this.syncEngine = syncEngine;
        this.syncExecutor = Executors.newSingleThreadExecutor();
        this.isSyncing = new AtomicBoolean(false);
    }

    @Override
    public void saveResponse(@NonNull Response response) {
        if (response == null) {
            throw new IllegalArgumentException("Response cannot be null");
        }
        ResponseEntity entity = ResponseMapper.toEntity(response);
        responseDao.insert(entity);
    }

    @Override
    public Response getResponseById(@NonNull String id) {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("Response ID cannot be null or empty");
        }
        ResponseEntity entity = responseDao.getById(id);
        if (entity == null) {
            return null;
        }
        return ResponseMapper.toDomain(entity);
    }

    @Override
    @NonNull
    public List<Response> getResponsesByQuestionId(@NonNull String questionId) {
        if (questionId == null || questionId.trim().isEmpty()) {
            throw new IllegalArgumentException("Question ID cannot be null or empty");
        }
        List<ResponseEntity> entities = responseDao.getByQuestionId(questionId);
        return mapEntitiesToDomain(entities);
    }

    @Override
    @NonNull
    public List<Response> getAllResponses() {
        List<ResponseEntity> entities = responseDao.getAll();
        return mapEntitiesToDomain(entities);
    }

    @Override
    @NonNull
    public List<Response> getUnsyncedResponses() {
        List<ResponseEntity> entities = responseDao.getUnsynced();
        return mapEntitiesToDomain(entities);
    }

    @Override
    public int getUnsyncedCount() {
        return responseDao.getUnsyncedCount();
    }

    @Override
    public void deleteResponse(@NonNull String id) {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("Response ID cannot be null or empty");
        }
        responseDao.deleteById(id);
    }

    @Override
    public void deleteResponsesByQuestionId(@NonNull String questionId) {
        if (questionId == null || questionId.trim().isEmpty()) {
            throw new IllegalArgumentException("Question ID cannot be null or empty");
        }
        responseDao.deleteByQuestionId(questionId);
    }

    @Override
    public void deleteAllResponses() {
        responseDao.deleteAll();
    }

    @Override
    public void syncPendingResponses() {
        syncPendingResponses(null);
    }

    @Override
    public void syncPendingResponses(SyncCallback callback) {
        if (!isSyncing.compareAndSet(false, true)) {
            // Already syncing, skip
            return;
        }

        syncExecutor.execute(() -> {
            try {
                performSync(callback);
            } finally {
                isSyncing.set(false);
            }
        });
    }

    /**
     * Performs the actual sync operation with retry logic.
     *
     * @param callback Optional callback for sync progress
     */
    private void performSync(SyncCallback callback) {
        List<ResponseEntity> unsyncedResponses = responseDao.getUnsynced();

        if (unsyncedResponses.isEmpty()) {
            if (callback != null) {
                callback.onSyncComplete(0, 0);
            }
            return;
        }

        int successCount = 0;
        int failureCount = 0;

        for (ResponseEntity entity : unsyncedResponses) {
            boolean synced = syncWithRetry(entity);

            if (synced) {
                responseDao.markSynced(entity.getId());
                successCount++;
                if (callback != null) {
                    callback.onSyncSuccess(entity.getId());
                }
            } else {
                failureCount++;
                if (callback != null) {
                    callback.onSyncFailure(entity.getId(), "Sync failed after max retries");
                }
            }
        }

        if (callback != null) {
            callback.onSyncComplete(successCount, failureCount);
        }
    }

    /**
     * Attempts to sync a single response with exponential backoff retry.
     *
     * @param entity The response entity to sync
     * @return true if sync succeeded, false otherwise
     */
    @VisibleForTesting
    boolean syncWithRetry(@NonNull ResponseEntity entity) {
        long backoffMs = INITIAL_BACKOFF_MS;

        for (int attempt = 0; attempt < MAX_RETRY_ATTEMPTS; attempt++) {
            try {
                if (syncEngine.syncResponse(entity)) {
                    return true;
                }
            } catch (Exception e) {
                // Treat any exception as a sync failure
            }

            // Don't sleep after the last failed attempt
            if (attempt < MAX_RETRY_ATTEMPTS - 1) {
                sleep(backoffMs);
                backoffMs = calculateNextBackoff(backoffMs);
            }
        }

        return false;
    }

    /**
     * Calculates the next backoff delay using exponential backoff.
     *
     * @param currentBackoff The current backoff delay
     * @return The next backoff delay, capped at MAX_BACKOFF_MS
     */
    @VisibleForTesting
    long calculateNextBackoff(long currentBackoff) {
        long nextBackoff = (long) (currentBackoff * BACKOFF_MULTIPLIER);
        return Math.min(nextBackoff, MAX_BACKOFF_MS);
    }

    /**
     * Sleep for the specified duration.
     * This method is protected to allow testing without actual delays.
     *
     * @param millis The duration to sleep in milliseconds
     */
    @VisibleForTesting
    protected void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Maps a list of ResponseEntity objects to Response domain objects.
     *
     * @param entities The list of entities to map
     * @return List of domain objects
     */
    @NonNull
    private List<Response> mapEntitiesToDomain(@NonNull List<ResponseEntity> entities) {
        List<Response> responses = new ArrayList<>(entities.size());
        for (ResponseEntity entity : entities) {
            responses.add(ResponseMapper.toDomain(entity));
        }
        return responses;
    }

    /**
     * Checks if a sync operation is currently in progress.
     *
     * @return true if syncing, false otherwise
     */
    public boolean isSyncing() {
        return isSyncing.get();
    }

    /**
     * Shuts down the sync executor service.
     * This should be called when the repository is no longer needed.
     * Waits up to the configured timeout for pending tasks to complete.
     */
    public void shutdown() {
        syncExecutor.shutdown();
        try {
            if (!syncExecutor.awaitTermination(getShutdownTimeoutSeconds(), TimeUnit.SECONDS)) {
                syncExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            syncExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Returns the timeout in seconds to wait for clean shutdown.
     * This method is protected to allow overriding in tests.
     *
     * @return The shutdown timeout in seconds
     */
    @VisibleForTesting
    protected long getShutdownTimeoutSeconds() {
        return 5L;
    }

    /**
     * Interface for sync engine operations.
     * This abstraction allows for easier testing.
     */
    @VisibleForTesting
    interface SyncEngine {
        /**
         * Attempts to sync a response to the server.
         *
         * @param entity The response entity to sync
         * @return true if sync succeeded, false otherwise
         */
        boolean syncResponse(@NonNull ResponseEntity entity);
    }

    /**
     * Default implementation of SyncEngine.
     * Throws UnsupportedOperationException until network sync is implemented.
     * 
     * @see <a href="https://github.com/raphaellith/Manuscripta/issues/TBD">Issue: Implement Response Network Sync</a>
     */
    private static class DefaultSyncEngine implements SyncEngine {
        @Override
        public boolean syncResponse(@NonNull ResponseEntity entity) {
            // Network sync not yet implemented - see android/issues.md
            throw new UnsupportedOperationException(
                "Network sync not yet implemented. Responses are stored locally only. "
                + "See issue: Implement Response Network Sync"
            );
        }
    }
}
