package com.manuscripta.student.data.repository;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

import com.manuscripta.student.data.local.ResponseDao;
import com.manuscripta.student.data.model.ResponseEntity;
import com.manuscripta.student.domain.common.Result;
import com.manuscripta.student.domain.mapper.ResponseMapper;
import com.manuscripta.student.domain.model.Response;
import com.manuscripta.student.network.ApiService;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Implementation of {@link ResponseRepository} providing local storage with sync capabilities.
 *
 * <p>This repository implements an offline-first architecture where responses are always
 * saved locally first, then queued for synchronization with the teacher's Windows app.
 * Network operations use exponential backoff for retry logic.</p>
 */
@Singleton
public class ResponseRepositoryImpl implements ResponseRepository {

    /** Default initial delay for retry (1 second). */
    @VisibleForTesting
    static final long DEFAULT_INITIAL_RETRY_DELAY_MS = 1000L;

    /** Maximum delay between retries (32 seconds). */
    @VisibleForTesting
    static final long MAX_RETRY_DELAY_MS = 32000L;

    /** Maximum number of retry attempts. */
    @VisibleForTesting
    static final int MAX_RETRY_ATTEMPTS = 5;

    /** Multiplier for exponential backoff. */
    @VisibleForTesting
    static final double BACKOFF_MULTIPLIER = 2.0;

    private final ResponseDao responseDao;
    private final ApiService apiService;
    private final Queue<String> syncQueue;

    /**
     * Creates a new ResponseRepositoryImpl.
     *
     * @param responseDao The DAO for local database operations
     * @param apiService  The API service for network operations
     * @throws IllegalArgumentException if responseDao is null
     */
    @Inject
    public ResponseRepositoryImpl(@NonNull ResponseDao responseDao,
                                  @NonNull ApiService apiService) {
        if (responseDao == null) {
            throw new IllegalArgumentException("ResponseDao cannot be null");
        }
        if (apiService == null) {
            throw new IllegalArgumentException("ApiService cannot be null");
        }
        this.responseDao = responseDao;
        this.apiService = apiService;
        this.syncQueue = new ConcurrentLinkedQueue<>();
    }

    /**
     * Constructor for testing with custom sync queue.
     *
     * @param responseDao The DAO for local database operations
     * @param apiService  The API service for network operations
     * @param syncQueue   Custom sync queue for testing
     */
    @VisibleForTesting
    ResponseRepositoryImpl(@NonNull ResponseDao responseDao,
                          @NonNull ApiService apiService,
                          @NonNull Queue<String> syncQueue) {
        if (responseDao == null) {
            throw new IllegalArgumentException("ResponseDao cannot be null");
        }
        if (apiService == null) {
            throw new IllegalArgumentException("ApiService cannot be null");
        }
        if (syncQueue == null) {
            throw new IllegalArgumentException("SyncQueue cannot be null");
        }
        this.responseDao = responseDao;
        this.apiService = apiService;
        this.syncQueue = syncQueue;
    }

    @NonNull
    @Override
    public Result<Response> saveResponse(@NonNull Response response) {
        if (response == null) {
            return Result.error(new IllegalArgumentException("Response cannot be null"));
        }
        try {
            ResponseEntity entity = ResponseMapper.toEntity(response);
            responseDao.insert(entity);
            queueForSync(response.getId());
            return Result.success(response);
        } catch (Exception e) {
            return Result.error(e);
        }
    }

    @NonNull
    @Override
    public Result<Response> getResponseById(@NonNull String id) {
        if (id == null || id.trim().isEmpty()) {
            return Result.error(new IllegalArgumentException("Response ID cannot be null or empty"));
        }
        try {
            ResponseEntity entity = responseDao.getById(id);
            if (entity == null) {
                return Result.success(null);
            }
            return Result.success(ResponseMapper.toDomain(entity));
        } catch (Exception e) {
            return Result.error(e);
        }
    }

    @NonNull
    @Override
    public Result<List<Response>> getResponsesByQuestionId(@NonNull String questionId) {
        if (questionId == null || questionId.trim().isEmpty()) {
            return Result.error(new IllegalArgumentException("Question ID cannot be null or empty"));
        }
        try {
            List<ResponseEntity> entities = responseDao.getByQuestionId(questionId);
            List<Response> responses = new ArrayList<>();
            for (ResponseEntity entity : entities) {
                responses.add(ResponseMapper.toDomain(entity));
            }
            return Result.success(responses);
        } catch (Exception e) {
            return Result.error(e);
        }
    }

    @NonNull
    @Override
    public Result<List<Response>> getAllResponses() {
        try {
            List<ResponseEntity> entities = responseDao.getAll();
            List<Response> responses = new ArrayList<>();
            for (ResponseEntity entity : entities) {
                responses.add(ResponseMapper.toDomain(entity));
            }
            return Result.success(responses);
        } catch (Exception e) {
            return Result.error(e);
        }
    }

    @NonNull
    @Override
    public Result<List<Response>> getUnsyncedResponses() {
        try {
            List<ResponseEntity> entities = responseDao.getUnsynced();
            List<Response> responses = new ArrayList<>();
            for (ResponseEntity entity : entities) {
                responses.add(ResponseMapper.toDomain(entity));
            }
            return Result.success(responses);
        } catch (Exception e) {
            return Result.error(e);
        }
    }

    @NonNull
    @Override
    public Result<Integer> getUnsyncedCount() {
        try {
            int count = responseDao.getUnsyncedCount();
            return Result.success(count);
        } catch (Exception e) {
            return Result.error(e);
        }
    }

    @NonNull
    @Override
    public Result<Boolean> markResponseSynced(@NonNull String id) {
        if (id == null || id.trim().isEmpty()) {
            return Result.error(new IllegalArgumentException("Response ID cannot be null or empty"));
        }
        try {
            responseDao.markSynced(id);
            return Result.success(true);
        } catch (Exception e) {
            return Result.error(e);
        }
    }

    @NonNull
    @Override
    public Result<Boolean> markResponsesSynced(@NonNull List<String> ids) {
        if (ids == null) {
            return Result.error(new IllegalArgumentException("Response IDs list cannot be null"));
        }
        if (ids.isEmpty()) {
            return Result.success(true);
        }
        try {
            responseDao.markAllSynced(ids);
            return Result.success(true);
        } catch (Exception e) {
            return Result.error(e);
        }
    }

    @NonNull
    @Override
    public Result<Boolean> updateResponse(@NonNull Response response) {
        if (response == null) {
            return Result.error(new IllegalArgumentException("Response cannot be null"));
        }
        try {
            ResponseEntity entity = ResponseMapper.toEntity(response);
            responseDao.update(entity);
            return Result.success(true);
        } catch (Exception e) {
            return Result.error(e);
        }
    }

    @NonNull
    @Override
    public Result<Boolean> deleteResponse(@NonNull String id) {
        if (id == null || id.trim().isEmpty()) {
            return Result.error(new IllegalArgumentException("Response ID cannot be null or empty"));
        }
        try {
            responseDao.deleteById(id);
            syncQueue.remove(id);
            return Result.success(true);
        } catch (Exception e) {
            return Result.error(e);
        }
    }

    @NonNull
    @Override
    public Result<Boolean> deleteResponsesByQuestionId(@NonNull String questionId) {
        if (questionId == null || questionId.trim().isEmpty()) {
            return Result.error(new IllegalArgumentException("Question ID cannot be null or empty"));
        }
        try {
            responseDao.deleteByQuestionId(questionId);
            return Result.success(true);
        } catch (Exception e) {
            return Result.error(e);
        }
    }

    @NonNull
    @Override
    public Result<Boolean> deleteAllResponses() {
        try {
            responseDao.deleteAll();
            syncQueue.clear();
            return Result.success(true);
        } catch (Exception e) {
            return Result.error(e);
        }
    }

    @NonNull
    @Override
    public Result<Integer> syncPendingResponses() {
        try {
            List<ResponseEntity> unsyncedEntities = responseDao.getUnsynced();
            if (unsyncedEntities.isEmpty()) {
                return Result.success(0);
            }

            int syncedCount = 0;
            List<String> syncedIds = new ArrayList<>();

            for (ResponseEntity entity : unsyncedEntities) {
                boolean synced = syncResponseWithRetry(entity);
                if (synced) {
                    syncedIds.add(entity.getId());
                    syncedCount++;
                }
            }

            if (!syncedIds.isEmpty()) {
                responseDao.markAllSynced(syncedIds);
                for (String id : syncedIds) {
                    syncQueue.remove(id);
                }
            }

            return Result.success(syncedCount);
        } catch (Exception e) {
            return Result.error(e);
        }
    }

    /**
     * Attempts to sync a single response with exponential backoff retry logic.
     *
     * @param entity The response entity to sync
     * @return true if sync succeeded, false otherwise
     */
    @VisibleForTesting
    boolean syncResponseWithRetry(@NonNull ResponseEntity entity) {
        long delay = DEFAULT_INITIAL_RETRY_DELAY_MS;

        for (int attempt = 0; attempt < MAX_RETRY_ATTEMPTS; attempt++) {
            try {
                boolean success = sendResponseToServer(entity);
                if (success) {
                    return true;
                }
            } catch (Exception e) {
                // Network error, will retry
            }

            if (attempt < MAX_RETRY_ATTEMPTS - 1) {
                sleep(delay);
                delay = calculateNextDelay(delay);
            }
        }

        return false;
    }

    /**
     * Calculates the next delay using exponential backoff.
     *
     * @param currentDelay The current delay in milliseconds
     * @return The next delay, capped at MAX_RETRY_DELAY_MS
     */
    @VisibleForTesting
    long calculateNextDelay(long currentDelay) {
        long nextDelay = (long) (currentDelay * BACKOFF_MULTIPLIER);
        return Math.min(nextDelay, MAX_RETRY_DELAY_MS);
    }

    /**
     * Sends a response to the server.
     * Override in tests to mock network behavior.
     *
     * @param entity The response entity to send
     * @return true if send succeeded, false otherwise
     */
    @VisibleForTesting
    protected boolean sendResponseToServer(@NonNull ResponseEntity entity) {
        // TODO: Implement actual API call when ApiService endpoints are defined
        // For now, this is a placeholder that simulates network availability check
        // In production, this would call apiService.syncResponse(entity)
        return true;
    }

    /**
     * Sleeps for the specified duration.
     * Override in tests to avoid actual sleeping.
     *
     * @param milliseconds Duration to sleep
     */
    @VisibleForTesting
    protected void sleep(long milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public void queueForSync(@NonNull String responseId) {
        if (responseId == null || responseId.trim().isEmpty()) {
            return;
        }
        if (!syncQueue.contains(responseId)) {
            syncQueue.add(responseId);
        }
    }

    @Override
    public int getSyncQueueSize() {
        return syncQueue.size();
    }

    @Override
    public void clearSyncQueue() {
        syncQueue.clear();
    }
}
