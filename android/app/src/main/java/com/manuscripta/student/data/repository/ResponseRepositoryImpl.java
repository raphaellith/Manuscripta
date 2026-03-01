package com.manuscripta.student.data.repository;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

import com.manuscripta.student.data.local.ResponseDao;
import com.manuscripta.student.data.model.ResponseEntity;
import com.manuscripta.student.domain.mapper.ResponseMapper;
import com.manuscripta.student.domain.model.Response;
import com.manuscripta.student.network.ApiService;
import com.manuscripta.student.network.dto.ResponseDto;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
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
 *   <li>Thread-safe operations</li>
 * </ul>
 */
@Singleton
public class ResponseRepositoryImpl implements ResponseRepository {

    /** Tag for logging. */
    private static final String TAG = "ResponseRepositoryImpl";

    /** The DAO for response persistence. */
    private final ResponseDao responseDao;

    /** Executor for background sync operations. */
    private final ExecutorService syncExecutor;

    /** Flag indicating whether a sync operation is in progress. */
    private final AtomicBoolean isSyncing;

    /** The sync engine for network operations. */
    private final SyncEngine syncEngine;

    /**
     * Creates a new ResponseRepositoryImpl with the given DAO and API service.
     *
     * @param responseDao The DAO for response persistence
     * @param apiService  The API service for network operations
     */
    @Inject
    public ResponseRepositoryImpl(@NonNull ResponseDao responseDao,
                                   @NonNull ApiService apiService) {
        this(responseDao, new NetworkSyncEngine(apiService));
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
     * Performs the actual sync operation.
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
            boolean synced = syncEngine.syncResponse(entity);

            if (synced) {
                responseDao.markSynced(entity.getId());
                successCount++;
                if (callback != null) {
                    callback.onSyncSuccess(entity.getId());
                }
            } else {
                failureCount++;
                if (callback != null) {
                    callback.onSyncFailure(entity.getId(), "Sync failed");
                }
            }
        }

        if (callback != null) {
            callback.onSyncComplete(successCount, failureCount);
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
     * This method is package-private as it is not part of the {@link ResponseRepository}
     * interface and is primarily intended for testing purposes.
     *
     * @return true if syncing, false otherwise
     */
    @VisibleForTesting
    boolean isSyncing() {
        return isSyncing.get();
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
     * Network implementation of SyncEngine that uses ApiService to submit responses
     * to the server via HTTP POST /responses.
     *
     * <p>Per API Contract §2.4, expects HTTP 201 Created on success.</p>
     */
    @VisibleForTesting
    static class NetworkSyncEngine implements SyncEngine {

        /** Tag for logging. */
        private static final String TAG = "NetworkSyncEngine";

        /** The API service for network operations. */
        private final ApiService apiService;

        /**
         * Creates a NetworkSyncEngine with the given API service.
         *
         * @param apiService The API service for network operations
         * @throws IllegalArgumentException if apiService is null
         */
        NetworkSyncEngine(@NonNull ApiService apiService) {
            if (apiService == null) {
                throw new IllegalArgumentException("ApiService cannot be null");
            }
            this.apiService = apiService;
        }

        @Override
        public boolean syncResponse(@NonNull ResponseEntity entity) {
            try {
                // Convert ResponseEntity -> Response (domain) -> ResponseDto (network)
                Response domainResponse = ResponseMapper.toDomain(entity);
                // MaterialId is null because ResponseEntity doesn't contain it
                // The server can derive it from questionId if needed
                ResponseDto dto = ResponseMapper.toDto(domainResponse, null);

                // Execute HTTP POST /responses
                retrofit2.Response<Void> response = apiService.submitResponse(dto).execute();

                // Per API Contract §2.4, expect HTTP 201 Created on success
                if (response.code() == 201) {
                    Log.d(TAG, "Successfully synced response: " + entity.getId());
                    return true;
                } else {
                    Log.w(TAG, "Failed to sync response: " + entity.getId()
                            + " - HTTP " + response.code());
                    return false;
                }
            } catch (IOException e) {
                // Network errors should return false to trigger retry
                Log.e(TAG, "Network error syncing response: " + entity.getId()
                        + " - " + e.getMessage());
                return false;
            } catch (Exception e) {
                // Unexpected errors should also return false
                Log.e(TAG, "Unexpected error syncing response: " + entity.getId()
                        + " - " + e.getMessage(), e);
                return false;
            }
        }
    }

    /**
     * Default implementation of SyncEngine.
     * Throws UnsupportedOperationException until network sync is implemented.
     *
     * @deprecated Replaced by NetworkSyncEngine. Kept for testing purposes.
     */
    @Deprecated
    @VisibleForTesting
    static class DefaultSyncEngine implements SyncEngine {
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
