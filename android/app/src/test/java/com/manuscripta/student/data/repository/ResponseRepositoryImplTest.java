package com.manuscripta.student.data.repository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.manuscripta.student.data.local.ResponseDao;
import com.manuscripta.student.data.model.ResponseEntity;
import com.manuscripta.student.domain.common.Result;
import com.manuscripta.student.domain.model.Response;
import com.manuscripta.student.network.ApiService;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Unit tests for {@link ResponseRepositoryImpl}.
 */
public class ResponseRepositoryImplTest {

    @Mock
    private ResponseDao mockDao;

    @Mock
    private ApiService mockApiService;

    private ResponseRepositoryImpl repository;
    private Queue<String> testSyncQueue;

    private static final String TEST_ID = "test-response-id";
    private static final String TEST_QUESTION_ID = "test-question-id";
    private static final String TEST_ANSWER = "test answer";
    private static final String TEST_DEVICE_ID = "test-device-id";
    private static final long TEST_TIMESTAMP = 1000000L;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        testSyncQueue = new ConcurrentLinkedQueue<>();
        repository = new ResponseRepositoryImpl(mockDao, mockApiService, testSyncQueue);
    }

    // ==================== Constructor Tests ====================

    @Test
    public void testConstructor_validArguments_createsInstance() {
        ResponseRepositoryImpl repo = new ResponseRepositoryImpl(mockDao, mockApiService);
        assertNotNull(repo);
    }

    @Test
    public void testConstructor_nullDao_throwsException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new ResponseRepositoryImpl(null, mockApiService)
        );
        assertEquals("ResponseDao cannot be null", exception.getMessage());
    }

    @Test
    public void testConstructor_nullApiService_throwsException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new ResponseRepositoryImpl(mockDao, null)
        );
        assertEquals("ApiService cannot be null", exception.getMessage());
    }

    @Test
    public void testConstructorWithQueue_nullDao_throwsException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new ResponseRepositoryImpl(null, mockApiService, testSyncQueue)
        );
        assertEquals("ResponseDao cannot be null", exception.getMessage());
    }

    @Test
    public void testConstructorWithQueue_nullApiService_throwsException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new ResponseRepositoryImpl(mockDao, null, testSyncQueue)
        );
        assertEquals("ApiService cannot be null", exception.getMessage());
    }

    @Test
    public void testConstructorWithQueue_nullQueue_throwsException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new ResponseRepositoryImpl(mockDao, mockApiService, null)
        );
        assertEquals("SyncQueue cannot be null", exception.getMessage());
    }

    // ==================== saveResponse Tests ====================

    @Test
    public void testSaveResponse_validResponse_savesAndQueuesForSync() {
        Response response = createTestResponse();
        doNothing().when(mockDao).insert(any(ResponseEntity.class));

        Result<Response> result = repository.saveResponse(response);

        assertTrue(result.isSuccess());
        assertEquals(response.getId(), result.getData().getId());
        verify(mockDao).insert(any(ResponseEntity.class));
        assertTrue(testSyncQueue.contains(response.getId()));
    }

    @Test
    public void testSaveResponse_nullResponse_returnsError() {
        Result<Response> result = repository.saveResponse(null);

        assertTrue(result.isError());
        assertTrue(result.getError() instanceof IllegalArgumentException);
        assertEquals("Response cannot be null", result.getError().getMessage());
    }

    @Test
    public void testSaveResponse_daoThrowsException_returnsError() {
        Response response = createTestResponse();
        doThrow(new RuntimeException("Database error")).when(mockDao).insert(any(ResponseEntity.class));

        Result<Response> result = repository.saveResponse(response);

        assertTrue(result.isError());
        assertEquals("Database error", result.getError().getMessage());
    }

    // ==================== getResponseById Tests ====================

    @Test
    public void testGetResponseById_existingId_returnsResponse() {
        ResponseEntity entity = createTestEntity();
        when(mockDao.getById(TEST_ID)).thenReturn(entity);

        Result<Response> result = repository.getResponseById(TEST_ID);

        assertTrue(result.isSuccess());
        assertNotNull(result.getData());
        assertEquals(TEST_ID, result.getData().getId());
    }

    @Test
    public void testGetResponseById_nonExistentId_returnsNull() {
        when(mockDao.getById("non-existent")).thenReturn(null);

        Result<Response> result = repository.getResponseById("non-existent");

        assertTrue(result.isSuccess());
        assertNull(result.getData());
    }

    @Test
    public void testGetResponseById_nullId_returnsError() {
        Result<Response> result = repository.getResponseById(null);

        assertTrue(result.isError());
        assertEquals("Response ID cannot be null or empty", result.getError().getMessage());
    }

    @Test
    public void testGetResponseById_emptyId_returnsError() {
        Result<Response> result = repository.getResponseById("");

        assertTrue(result.isError());
        assertEquals("Response ID cannot be null or empty", result.getError().getMessage());
    }

    @Test
    public void testGetResponseById_blankId_returnsError() {
        Result<Response> result = repository.getResponseById("   ");

        assertTrue(result.isError());
        assertEquals("Response ID cannot be null or empty", result.getError().getMessage());
    }

    @Test
    public void testGetResponseById_daoThrowsException_returnsError() {
        when(mockDao.getById(anyString())).thenThrow(new RuntimeException("Database error"));

        Result<Response> result = repository.getResponseById(TEST_ID);

        assertTrue(result.isError());
        assertEquals("Database error", result.getError().getMessage());
    }

    // ==================== getResponsesByQuestionId Tests ====================

    @Test
    public void testGetResponsesByQuestionId_validQuestionId_returnsList() {
        List<ResponseEntity> entities = Arrays.asList(createTestEntity(), createTestEntity());
        when(mockDao.getByQuestionId(TEST_QUESTION_ID)).thenReturn(entities);

        Result<List<Response>> result = repository.getResponsesByQuestionId(TEST_QUESTION_ID);

        assertTrue(result.isSuccess());
        assertEquals(2, result.getData().size());
    }

    @Test
    public void testGetResponsesByQuestionId_noResponses_returnsEmptyList() {
        when(mockDao.getByQuestionId(TEST_QUESTION_ID)).thenReturn(Collections.emptyList());

        Result<List<Response>> result = repository.getResponsesByQuestionId(TEST_QUESTION_ID);

        assertTrue(result.isSuccess());
        assertTrue(result.getData().isEmpty());
    }

    @Test
    public void testGetResponsesByQuestionId_nullQuestionId_returnsError() {
        Result<List<Response>> result = repository.getResponsesByQuestionId(null);

        assertTrue(result.isError());
        assertEquals("Question ID cannot be null or empty", result.getError().getMessage());
    }

    @Test
    public void testGetResponsesByQuestionId_emptyQuestionId_returnsError() {
        Result<List<Response>> result = repository.getResponsesByQuestionId("");

        assertTrue(result.isError());
        assertEquals("Question ID cannot be null or empty", result.getError().getMessage());
    }

    @Test
    public void testGetResponsesByQuestionId_blankQuestionId_returnsError() {
        Result<List<Response>> result = repository.getResponsesByQuestionId("   ");

        assertTrue(result.isError());
        assertEquals("Question ID cannot be null or empty", result.getError().getMessage());
    }

    @Test
    public void testGetResponsesByQuestionId_daoThrowsException_returnsError() {
        when(mockDao.getByQuestionId(anyString())).thenThrow(new RuntimeException("Database error"));

        Result<List<Response>> result = repository.getResponsesByQuestionId(TEST_QUESTION_ID);

        assertTrue(result.isError());
        assertEquals("Database error", result.getError().getMessage());
    }

    // ==================== getAllResponses Tests ====================

    @Test
    public void testGetAllResponses_hasResponses_returnsList() {
        List<ResponseEntity> entities = Arrays.asList(createTestEntity(), createTestEntity());
        when(mockDao.getAll()).thenReturn(entities);

        Result<List<Response>> result = repository.getAllResponses();

        assertTrue(result.isSuccess());
        assertEquals(2, result.getData().size());
    }

    @Test
    public void testGetAllResponses_noResponses_returnsEmptyList() {
        when(mockDao.getAll()).thenReturn(Collections.emptyList());

        Result<List<Response>> result = repository.getAllResponses();

        assertTrue(result.isSuccess());
        assertTrue(result.getData().isEmpty());
    }

    @Test
    public void testGetAllResponses_daoThrowsException_returnsError() {
        when(mockDao.getAll()).thenThrow(new RuntimeException("Database error"));

        Result<List<Response>> result = repository.getAllResponses();

        assertTrue(result.isError());
        assertEquals("Database error", result.getError().getMessage());
    }

    // ==================== getUnsyncedResponses Tests ====================

    @Test
    public void testGetUnsyncedResponses_hasUnsynced_returnsList() {
        List<ResponseEntity> entities = Arrays.asList(createTestEntity());
        when(mockDao.getUnsynced()).thenReturn(entities);

        Result<List<Response>> result = repository.getUnsyncedResponses();

        assertTrue(result.isSuccess());
        assertEquals(1, result.getData().size());
    }

    @Test
    public void testGetUnsyncedResponses_noUnsynced_returnsEmptyList() {
        when(mockDao.getUnsynced()).thenReturn(Collections.emptyList());

        Result<List<Response>> result = repository.getUnsyncedResponses();

        assertTrue(result.isSuccess());
        assertTrue(result.getData().isEmpty());
    }

    @Test
    public void testGetUnsyncedResponses_daoThrowsException_returnsError() {
        when(mockDao.getUnsynced()).thenThrow(new RuntimeException("Database error"));

        Result<List<Response>> result = repository.getUnsyncedResponses();

        assertTrue(result.isError());
        assertEquals("Database error", result.getError().getMessage());
    }

    // ==================== getUnsyncedCount Tests ====================

    @Test
    public void testGetUnsyncedCount_hasUnsynced_returnsCount() {
        when(mockDao.getUnsyncedCount()).thenReturn(5);

        Result<Integer> result = repository.getUnsyncedCount();

        assertTrue(result.isSuccess());
        assertEquals(Integer.valueOf(5), result.getData());
    }

    @Test
    public void testGetUnsyncedCount_noUnsynced_returnsZero() {
        when(mockDao.getUnsyncedCount()).thenReturn(0);

        Result<Integer> result = repository.getUnsyncedCount();

        assertTrue(result.isSuccess());
        assertEquals(Integer.valueOf(0), result.getData());
    }

    @Test
    public void testGetUnsyncedCount_daoThrowsException_returnsError() {
        when(mockDao.getUnsyncedCount()).thenThrow(new RuntimeException("Database error"));

        Result<Integer> result = repository.getUnsyncedCount();

        assertTrue(result.isError());
        assertEquals("Database error", result.getError().getMessage());
    }

    // ==================== markResponseSynced Tests ====================

    @Test
    public void testMarkResponseSynced_validId_marksAsSynced() {
        doNothing().when(mockDao).markSynced(TEST_ID);

        Result<Boolean> result = repository.markResponseSynced(TEST_ID);

        assertTrue(result.isSuccess());
        assertTrue(result.getData());
        verify(mockDao).markSynced(TEST_ID);
    }

    @Test
    public void testMarkResponseSynced_nullId_returnsError() {
        Result<Boolean> result = repository.markResponseSynced(null);

        assertTrue(result.isError());
        assertEquals("Response ID cannot be null or empty", result.getError().getMessage());
    }

    @Test
    public void testMarkResponseSynced_emptyId_returnsError() {
        Result<Boolean> result = repository.markResponseSynced("");

        assertTrue(result.isError());
        assertEquals("Response ID cannot be null or empty", result.getError().getMessage());
    }

    @Test
    public void testMarkResponseSynced_blankId_returnsError() {
        Result<Boolean> result = repository.markResponseSynced("   ");

        assertTrue(result.isError());
        assertEquals("Response ID cannot be null or empty", result.getError().getMessage());
    }

    @Test
    public void testMarkResponseSynced_daoThrowsException_returnsError() {
        doThrow(new RuntimeException("Database error")).when(mockDao).markSynced(anyString());

        Result<Boolean> result = repository.markResponseSynced(TEST_ID);

        assertTrue(result.isError());
        assertEquals("Database error", result.getError().getMessage());
    }

    // ==================== markResponsesSynced Tests ====================

    @Test
    public void testMarkResponsesSynced_validIds_marksAllAsSynced() {
        List<String> ids = Arrays.asList("id1", "id2", "id3");
        doNothing().when(mockDao).markAllSynced(ids);

        Result<Boolean> result = repository.markResponsesSynced(ids);

        assertTrue(result.isSuccess());
        assertTrue(result.getData());
        verify(mockDao).markAllSynced(ids);
    }

    @Test
    public void testMarkResponsesSynced_emptyList_returnsSuccessWithoutDaoCall() {
        List<String> ids = Collections.emptyList();

        Result<Boolean> result = repository.markResponsesSynced(ids);

        assertTrue(result.isSuccess());
        assertTrue(result.getData());
        verify(mockDao, never()).markAllSynced(anyList());
    }

    @Test
    public void testMarkResponsesSynced_nullList_returnsError() {
        Result<Boolean> result = repository.markResponsesSynced(null);

        assertTrue(result.isError());
        assertEquals("Response IDs list cannot be null", result.getError().getMessage());
    }

    @Test
    public void testMarkResponsesSynced_daoThrowsException_returnsError() {
        List<String> ids = Arrays.asList("id1", "id2");
        doThrow(new RuntimeException("Database error")).when(mockDao).markAllSynced(anyList());

        Result<Boolean> result = repository.markResponsesSynced(ids);

        assertTrue(result.isError());
        assertEquals("Database error", result.getError().getMessage());
    }

    // ==================== updateResponse Tests ====================

    @Test
    public void testUpdateResponse_validResponse_updatesSuccessfully() {
        Response response = createTestResponse();
        doNothing().when(mockDao).update(any(ResponseEntity.class));

        Result<Boolean> result = repository.updateResponse(response);

        assertTrue(result.isSuccess());
        assertTrue(result.getData());
        verify(mockDao).update(any(ResponseEntity.class));
    }

    @Test
    public void testUpdateResponse_nullResponse_returnsError() {
        Result<Boolean> result = repository.updateResponse(null);

        assertTrue(result.isError());
        assertEquals("Response cannot be null", result.getError().getMessage());
    }

    @Test
    public void testUpdateResponse_daoThrowsException_returnsError() {
        Response response = createTestResponse();
        doThrow(new RuntimeException("Database error")).when(mockDao).update(any(ResponseEntity.class));

        Result<Boolean> result = repository.updateResponse(response);

        assertTrue(result.isError());
        assertEquals("Database error", result.getError().getMessage());
    }

    // ==================== deleteResponse Tests ====================

    @Test
    public void testDeleteResponse_validId_deletesSuccessfully() {
        testSyncQueue.add(TEST_ID);
        doNothing().when(mockDao).deleteById(TEST_ID);

        Result<Boolean> result = repository.deleteResponse(TEST_ID);

        assertTrue(result.isSuccess());
        assertTrue(result.getData());
        verify(mockDao).deleteById(TEST_ID);
        assertFalse(testSyncQueue.contains(TEST_ID));
    }

    @Test
    public void testDeleteResponse_nullId_returnsError() {
        Result<Boolean> result = repository.deleteResponse(null);

        assertTrue(result.isError());
        assertEquals("Response ID cannot be null or empty", result.getError().getMessage());
    }

    @Test
    public void testDeleteResponse_emptyId_returnsError() {
        Result<Boolean> result = repository.deleteResponse("");

        assertTrue(result.isError());
        assertEquals("Response ID cannot be null or empty", result.getError().getMessage());
    }

    @Test
    public void testDeleteResponse_blankId_returnsError() {
        Result<Boolean> result = repository.deleteResponse("   ");

        assertTrue(result.isError());
        assertEquals("Response ID cannot be null or empty", result.getError().getMessage());
    }

    @Test
    public void testDeleteResponse_daoThrowsException_returnsError() {
        doThrow(new RuntimeException("Database error")).when(mockDao).deleteById(anyString());

        Result<Boolean> result = repository.deleteResponse(TEST_ID);

        assertTrue(result.isError());
        assertEquals("Database error", result.getError().getMessage());
    }

    // ==================== deleteResponsesByQuestionId Tests ====================

    @Test
    public void testDeleteResponsesByQuestionId_validId_deletesSuccessfully() {
        doNothing().when(mockDao).deleteByQuestionId(TEST_QUESTION_ID);

        Result<Boolean> result = repository.deleteResponsesByQuestionId(TEST_QUESTION_ID);

        assertTrue(result.isSuccess());
        assertTrue(result.getData());
        verify(mockDao).deleteByQuestionId(TEST_QUESTION_ID);
    }

    @Test
    public void testDeleteResponsesByQuestionId_nullId_returnsError() {
        Result<Boolean> result = repository.deleteResponsesByQuestionId(null);

        assertTrue(result.isError());
        assertEquals("Question ID cannot be null or empty", result.getError().getMessage());
    }

    @Test
    public void testDeleteResponsesByQuestionId_emptyId_returnsError() {
        Result<Boolean> result = repository.deleteResponsesByQuestionId("");

        assertTrue(result.isError());
        assertEquals("Question ID cannot be null or empty", result.getError().getMessage());
    }

    @Test
    public void testDeleteResponsesByQuestionId_blankId_returnsError() {
        Result<Boolean> result = repository.deleteResponsesByQuestionId("   ");

        assertTrue(result.isError());
        assertEquals("Question ID cannot be null or empty", result.getError().getMessage());
    }

    @Test
    public void testDeleteResponsesByQuestionId_daoThrowsException_returnsError() {
        doThrow(new RuntimeException("Database error")).when(mockDao).deleteByQuestionId(anyString());

        Result<Boolean> result = repository.deleteResponsesByQuestionId(TEST_QUESTION_ID);

        assertTrue(result.isError());
        assertEquals("Database error", result.getError().getMessage());
    }

    // ==================== deleteAllResponses Tests ====================

    @Test
    public void testDeleteAllResponses_clearsAllAndQueue() {
        testSyncQueue.add("id1");
        testSyncQueue.add("id2");
        doNothing().when(mockDao).deleteAll();

        Result<Boolean> result = repository.deleteAllResponses();

        assertTrue(result.isSuccess());
        assertTrue(result.getData());
        verify(mockDao).deleteAll();
        assertTrue(testSyncQueue.isEmpty());
    }

    @Test
    public void testDeleteAllResponses_daoThrowsException_returnsError() {
        doThrow(new RuntimeException("Database error")).when(mockDao).deleteAll();

        Result<Boolean> result = repository.deleteAllResponses();

        assertTrue(result.isError());
        assertEquals("Database error", result.getError().getMessage());
    }

    // ==================== syncPendingResponses Tests ====================

    @Test
    public void testSyncPendingResponses_noUnsyncedResponses_returnsZero() {
        when(mockDao.getUnsynced()).thenReturn(Collections.emptyList());

        Result<Integer> result = repository.syncPendingResponses();

        assertTrue(result.isSuccess());
        assertEquals(Integer.valueOf(0), result.getData());
    }

    @Test
    public void testSyncPendingResponses_allSyncSuccessfully_returnsCount() {
        List<ResponseEntity> entities = Arrays.asList(
                createTestEntityWithId("id1"),
                createTestEntityWithId("id2")
        );
        when(mockDao.getUnsynced()).thenReturn(entities);
        testSyncQueue.add("id1");
        testSyncQueue.add("id2");

        // Use a non-sleeping repository for faster tests
        ResponseRepositoryImpl nonSleepingRepo = createNonSleepingRepository(true);

        Result<Integer> result = nonSleepingRepo.syncPendingResponses();

        assertTrue(result.isSuccess());
        assertEquals(Integer.valueOf(2), result.getData());
    }

    @Test
    public void testSyncPendingResponses_partialSuccess_returnsSuccessCount() {
        List<ResponseEntity> entities = Arrays.asList(
                createTestEntityWithId("id1"),
                createTestEntityWithId("id2")
        );
        when(mockDao.getUnsynced()).thenReturn(entities);

        // Create repository where first succeeds, second fails
        ResponseRepositoryImpl partialSuccessRepo = new ResponseRepositoryImpl(mockDao, mockApiService, testSyncQueue) {
            private int callCount = 0;

            @Override
            protected boolean sendResponseToServer(ResponseEntity entity) {
                callCount++;
                return callCount == 1; // Only first call succeeds
            }

            @Override
            protected void sleep(long milliseconds) {
                // No-op for tests
            }
        };

        Result<Integer> result = partialSuccessRepo.syncPendingResponses();

        assertTrue(result.isSuccess());
        assertEquals(Integer.valueOf(1), result.getData());
    }

    @Test
    public void testSyncPendingResponses_daoThrowsException_returnsError() {
        when(mockDao.getUnsynced()).thenThrow(new RuntimeException("Database error"));

        Result<Integer> result = repository.syncPendingResponses();

        assertTrue(result.isError());
        assertEquals("Database error", result.getError().getMessage());
    }

    @Test
    public void testSyncPendingResponses_allSyncsFail_returnsZeroAndSkipsMarkSynced() {
        List<ResponseEntity> entities = Arrays.asList(
                createTestEntityWithId("fail-id1"),
                createTestEntityWithId("fail-id2")
        );
        when(mockDao.getUnsynced()).thenReturn(entities);

        // Create repository where all syncs fail
        ResponseRepositoryImpl allFailRepo = new ResponseRepositoryImpl(mockDao, mockApiService, testSyncQueue) {
            @Override
            protected boolean sendResponseToServer(ResponseEntity entity) {
                return false; // All syncs fail
            }

            @Override
            protected void sleep(long milliseconds) {
                // No-op for tests
            }
        };

        Result<Integer> result = allFailRepo.syncPendingResponses();

        assertTrue(result.isSuccess());
        assertEquals(Integer.valueOf(0), result.getData());
        // markAllSynced should NOT have been called since syncedIds is empty
        verify(mockDao, never()).markAllSynced(anyList());
    }

    // ==================== syncResponseWithRetry Tests ====================

    @Test
    public void testSyncResponseWithRetry_firstAttemptSucceeds_returnsTrue() {
        ResponseEntity entity = createTestEntity();
        ResponseRepositoryImpl successRepo = createNonSleepingRepository(true);

        boolean result = successRepo.syncResponseWithRetry(entity);

        assertTrue(result);
    }

    @Test
    public void testSyncResponseWithRetry_allAttemptsFail_returnsFalse() {
        ResponseEntity entity = createTestEntity();
        ResponseRepositoryImpl failingRepo = createNonSleepingRepository(false);

        boolean result = failingRepo.syncResponseWithRetry(entity);

        assertFalse(result);
    }

    @Test
    public void testSyncResponseWithRetry_succeedsOnThirdAttempt_returnsTrue() {
        ResponseEntity entity = createTestEntity();
        ResponseRepositoryImpl retryRepo = new ResponseRepositoryImpl(mockDao, mockApiService, testSyncQueue) {
            private int attemptCount = 0;

            @Override
            protected boolean sendResponseToServer(ResponseEntity e) {
                attemptCount++;
                return attemptCount >= 3; // Succeed on third attempt
            }

            @Override
            protected void sleep(long milliseconds) {
                // No-op for tests
            }
        };

        boolean result = retryRepo.syncResponseWithRetry(entity);

        assertTrue(result);
    }

    @Test
    public void testSyncResponseWithRetry_networkExceptions_retriesAndFails() {
        ResponseEntity entity = createTestEntity();
        ResponseRepositoryImpl exceptionRepo = new ResponseRepositoryImpl(mockDao, mockApiService, testSyncQueue) {
            @Override
            protected boolean sendResponseToServer(ResponseEntity e) {
                throw new RuntimeException("Network error");
            }

            @Override
            protected void sleep(long milliseconds) {
                // No-op for tests
            }
        };

        boolean result = exceptionRepo.syncResponseWithRetry(entity);

        assertFalse(result);
    }

    // ==================== calculateNextDelay Tests ====================

    @Test
    public void testCalculateNextDelay_doublesDelay() {
        long nextDelay = repository.calculateNextDelay(1000L);
        assertEquals(2000L, nextDelay);
    }

    @Test
    public void testCalculateNextDelay_capsAtMaximum() {
        long nextDelay = repository.calculateNextDelay(20000L);
        assertEquals(ResponseRepositoryImpl.MAX_RETRY_DELAY_MS, nextDelay);
    }

    @Test
    public void testCalculateNextDelay_alreadyAtMax_staysAtMax() {
        long nextDelay = repository.calculateNextDelay(ResponseRepositoryImpl.MAX_RETRY_DELAY_MS);
        assertEquals(ResponseRepositoryImpl.MAX_RETRY_DELAY_MS, nextDelay);
    }

    // ==================== queueForSync Tests ====================

    @Test
    public void testQueueForSync_validId_addsToQueue() {
        repository.queueForSync("new-id");

        assertTrue(testSyncQueue.contains("new-id"));
        assertEquals(1, repository.getSyncQueueSize());
    }

    @Test
    public void testQueueForSync_duplicateId_doesNotAddTwice() {
        repository.queueForSync("duplicate-id");
        repository.queueForSync("duplicate-id");

        assertEquals(1, repository.getSyncQueueSize());
    }

    @Test
    public void testQueueForSync_nullId_doesNotAdd() {
        repository.queueForSync(null);

        assertEquals(0, repository.getSyncQueueSize());
    }

    @Test
    public void testQueueForSync_emptyId_doesNotAdd() {
        repository.queueForSync("");

        assertEquals(0, repository.getSyncQueueSize());
    }

    @Test
    public void testQueueForSync_blankId_doesNotAdd() {
        repository.queueForSync("   ");

        assertEquals(0, repository.getSyncQueueSize());
    }

    // ==================== getSyncQueueSize Tests ====================

    @Test
    public void testGetSyncQueueSize_emptyQueue_returnsZero() {
        assertEquals(0, repository.getSyncQueueSize());
    }

    @Test
    public void testGetSyncQueueSize_withItems_returnsCorrectCount() {
        testSyncQueue.add("id1");
        testSyncQueue.add("id2");
        testSyncQueue.add("id3");

        assertEquals(3, repository.getSyncQueueSize());
    }

    // ==================== clearSyncQueue Tests ====================

    @Test
    public void testClearSyncQueue_clearsAllItems() {
        testSyncQueue.add("id1");
        testSyncQueue.add("id2");

        repository.clearSyncQueue();

        assertTrue(testSyncQueue.isEmpty());
        assertEquals(0, repository.getSyncQueueSize());
    }

    @Test
    public void testClearSyncQueue_emptyQueue_remainsEmpty() {
        repository.clearSyncQueue();

        assertTrue(testSyncQueue.isEmpty());
    }

    // ==================== Constants Tests ====================

    @Test
    public void testConstants_haveExpectedValues() {
        assertEquals(1000L, ResponseRepositoryImpl.DEFAULT_INITIAL_RETRY_DELAY_MS);
        assertEquals(32000L, ResponseRepositoryImpl.MAX_RETRY_DELAY_MS);
        assertEquals(5, ResponseRepositoryImpl.MAX_RETRY_ATTEMPTS);
        assertEquals(2.0, ResponseRepositoryImpl.BACKOFF_MULTIPLIER, 0.001);
    }

    // ==================== Integration-style Tests ====================

    @Test
    public void testSaveAndRetrieve_workflow() {
        Response response = createTestResponse();
        ResponseEntity entity = createTestEntity();

        doNothing().when(mockDao).insert(any(ResponseEntity.class));
        when(mockDao.getById(response.getId())).thenReturn(entity);

        Result<Response> saveResult = repository.saveResponse(response);
        assertTrue(saveResult.isSuccess());

        Result<Response> getResult = repository.getResponseById(response.getId());
        assertTrue(getResult.isSuccess());
        assertNotNull(getResult.getData());
    }

    @Test
    public void testSaveUpdateDelete_workflow() {
        Response response = createTestResponse();

        doNothing().when(mockDao).insert(any(ResponseEntity.class));
        doNothing().when(mockDao).update(any(ResponseEntity.class));
        doNothing().when(mockDao).deleteById(response.getId());

        Result<Response> saveResult = repository.saveResponse(response);
        assertTrue(saveResult.isSuccess());

        Result<Boolean> updateResult = repository.updateResponse(response);
        assertTrue(updateResult.isSuccess());

        Result<Boolean> deleteResult = repository.deleteResponse(response.getId());
        assertTrue(deleteResult.isSuccess());
    }

    // ==================== Sleep Method Test ====================

    @Test
    public void testSleep_interruptedThread_setsInterruptFlag() {
        Thread testThread = new Thread(() -> {
            Thread.currentThread().interrupt();
            repository.sleep(100);
            assertTrue(Thread.currentThread().isInterrupted());
        });
        testThread.start();
        try {
            testThread.join(1000);
        } catch (InterruptedException e) {
            // Ignore
        }
    }

    @Test
    public void testSleep_normalExecution_completesWithoutException() {
        long startTime = System.currentTimeMillis();
        repository.sleep(50);
        long elapsed = System.currentTimeMillis() - startTime;
        assertTrue("Sleep should have waited at least 40ms", elapsed >= 40);
    }

    // ==================== sendResponseToServer Tests ====================

    @Test
    public void testSendResponseToServer_defaultImplementation_returnsTrue() {
        ResponseEntity entity = createTestEntity();
        boolean result = repository.sendResponseToServer(entity);
        assertTrue(result);
    }

    // ==================== Helper Methods ====================

    private Response createTestResponse() {
        return new Response(
                TEST_ID,
                TEST_QUESTION_ID,
                TEST_ANSWER,
                false,
                TEST_TIMESTAMP,
                false,
                TEST_DEVICE_ID
        );
    }

    private ResponseEntity createTestEntity() {
        return new ResponseEntity(
                TEST_ID,
                TEST_QUESTION_ID,
                TEST_ANSWER,
                false,
                TEST_TIMESTAMP,
                false,
                TEST_DEVICE_ID
        );
    }

    private ResponseEntity createTestEntityWithId(String id) {
        return new ResponseEntity(
                id,
                TEST_QUESTION_ID,
                TEST_ANSWER,
                false,
                TEST_TIMESTAMP,
                false,
                TEST_DEVICE_ID
        );
    }

    private ResponseRepositoryImpl createNonSleepingRepository(boolean syncSuccess) {
        return new ResponseRepositoryImpl(mockDao, mockApiService, testSyncQueue) {
            @Override
            protected boolean sendResponseToServer(ResponseEntity entity) {
                return syncSuccess;
            }

            @Override
            protected void sleep(long milliseconds) {
                // No-op for tests
            }
        };
    }
}
