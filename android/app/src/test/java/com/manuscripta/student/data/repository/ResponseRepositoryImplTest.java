package com.manuscripta.student.data.repository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.manuscripta.student.data.local.ResponseDao;
import com.manuscripta.student.data.model.ResponseEntity;
import com.manuscripta.student.domain.model.Response;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Unit tests for {@link ResponseRepositoryImpl}.
 */
public class ResponseRepositoryImplTest {

    private ResponseDao mockDao;
    private ResponseRepositoryImpl.SyncEngine mockSyncEngine;
    private ResponseRepositoryImpl repository;

    private static final String TEST_ID = "test-response-id";
    private static final String TEST_QUESTION_ID = "test-question-id";
    private static final String TEST_DEVICE_ID = "test-device-id";

    @Before
    public void setUp() {
        mockDao = mock(ResponseDao.class);
        mockSyncEngine = mock(ResponseRepositoryImpl.SyncEngine.class);
        repository = new TestableResponseRepository(mockDao, mockSyncEngine);
    }

    // ==================== Constructor Tests ====================

    @Test
    public void testConstructor_validDao_createsInstance() {
        ResponseRepositoryImpl repo = new ResponseRepositoryImpl(mockDao);
        assertNotNull(repo);
    }

    @Test
    public void testConstructor_nullDao_throwsException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new ResponseRepositoryImpl(null)
        );
        assertEquals("ResponseDao cannot be null", exception.getMessage());
    }

    @Test
    public void testConstructor_nullSyncEngine_throwsException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new ResponseRepositoryImpl(mockDao, null)
        );
        assertEquals("SyncEngine cannot be null", exception.getMessage());
    }

    // ==================== saveResponse Tests ====================

    @Test
    public void testSaveResponse_validResponse_insertsAndQueues() {
        Response response = createTestResponse(false);

        repository.saveResponse(response);

        verify(mockDao).insert(any(ResponseEntity.class));
        assertEquals(1, repository.getSyncQueueSize());
    }

    @Test
    public void testSaveResponse_alreadySynced_insertsButDoesNotQueue() {
        Response response = createTestResponse(true);

        repository.saveResponse(response);

        verify(mockDao).insert(any(ResponseEntity.class));
        assertEquals(0, repository.getSyncQueueSize());
    }

    @Test
    public void testSaveResponse_nullResponse_throwsException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> repository.saveResponse(null)
        );
        assertEquals("Response cannot be null", exception.getMessage());
    }

    // ==================== getResponseById Tests ====================

    @Test
    public void testGetResponseById_existingId_returnsResponse() {
        ResponseEntity entity = createTestEntity();
        when(mockDao.getById(TEST_ID)).thenReturn(entity);

        Response result = repository.getResponseById(TEST_ID);

        assertNotNull(result);
        assertEquals(TEST_ID, result.getId());
        verify(mockDao).getById(TEST_ID);
    }

    @Test
    public void testGetResponseById_nonExistentId_returnsNull() {
        when(mockDao.getById(anyString())).thenReturn(null);

        Response result = repository.getResponseById("non-existent");

        assertNull(result);
    }

    @Test
    public void testGetResponseById_nullId_throwsException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> repository.getResponseById(null)
        );
        assertEquals("Response ID cannot be null or empty", exception.getMessage());
    }

    @Test
    public void testGetResponseById_emptyId_throwsException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> repository.getResponseById("")
        );
        assertEquals("Response ID cannot be null or empty", exception.getMessage());
    }

    @Test
    public void testGetResponseById_blankId_throwsException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> repository.getResponseById("   ")
        );
        assertEquals("Response ID cannot be null or empty", exception.getMessage());
    }

    // ==================== getResponsesByQuestionId Tests ====================

    @Test
    public void testGetResponsesByQuestionId_existingQuestion_returnsResponses() {
        List<ResponseEntity> entities = Arrays.asList(createTestEntity(), createTestEntity());
        when(mockDao.getByQuestionId(TEST_QUESTION_ID)).thenReturn(entities);

        List<Response> result = repository.getResponsesByQuestionId(TEST_QUESTION_ID);

        assertNotNull(result);
        assertEquals(2, result.size());
    }

    @Test
    public void testGetResponsesByQuestionId_noResponses_returnsEmptyList() {
        when(mockDao.getByQuestionId(anyString())).thenReturn(Collections.emptyList());

        List<Response> result = repository.getResponsesByQuestionId(TEST_QUESTION_ID);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testGetResponsesByQuestionId_nullId_throwsException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> repository.getResponsesByQuestionId(null)
        );
        assertEquals("Question ID cannot be null or empty", exception.getMessage());
    }

    @Test
    public void testGetResponsesByQuestionId_emptyId_throwsException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> repository.getResponsesByQuestionId("")
        );
        assertEquals("Question ID cannot be null or empty", exception.getMessage());
    }

    @Test
    public void testGetResponsesByQuestionId_blankId_throwsException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> repository.getResponsesByQuestionId("   ")
        );
        assertEquals("Question ID cannot be null or empty", exception.getMessage());
    }

    // ==================== getAllResponses Tests ====================

    @Test
    public void testGetAllResponses_hasResponses_returnsList() {
        List<ResponseEntity> entities = Arrays.asList(createTestEntity(), createTestEntity());
        when(mockDao.getAll()).thenReturn(entities);

        List<Response> result = repository.getAllResponses();

        assertNotNull(result);
        assertEquals(2, result.size());
    }

    @Test
    public void testGetAllResponses_noResponses_returnsEmptyList() {
        when(mockDao.getAll()).thenReturn(Collections.emptyList());

        List<Response> result = repository.getAllResponses();

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // ==================== getUnsyncedResponses Tests ====================

    @Test
    public void testGetUnsyncedResponses_hasUnsynced_returnsList() {
        List<ResponseEntity> entities = Arrays.asList(createTestEntity(), createTestEntity());
        when(mockDao.getUnsynced()).thenReturn(entities);

        List<Response> result = repository.getUnsyncedResponses();

        assertNotNull(result);
        assertEquals(2, result.size());
    }

    @Test
    public void testGetUnsyncedResponses_noUnsynced_returnsEmptyList() {
        when(mockDao.getUnsynced()).thenReturn(Collections.emptyList());

        List<Response> result = repository.getUnsyncedResponses();

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // ==================== getUnsyncedCount Tests ====================

    @Test
    public void testGetUnsyncedCount_hasUnsynced_returnsCount() {
        when(mockDao.getUnsyncedCount()).thenReturn(5);

        int result = repository.getUnsyncedCount();

        assertEquals(5, result);
    }

    @Test
    public void testGetUnsyncedCount_noUnsynced_returnsZero() {
        when(mockDao.getUnsyncedCount()).thenReturn(0);

        int result = repository.getUnsyncedCount();

        assertEquals(0, result);
    }

    // ==================== deleteResponse Tests ====================

    @Test
    public void testDeleteResponse_validId_deletesAndRemovesFromQueue() {
        // Add to queue first
        Response response = createTestResponse(false);
        repository.saveResponse(response);
        assertEquals(1, repository.getSyncQueueSize());

        repository.deleteResponse(TEST_ID);

        verify(mockDao).deleteById(TEST_ID);
        assertEquals(0, repository.getSyncQueueSize());
    }

    @Test
    public void testDeleteResponse_nullId_throwsException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> repository.deleteResponse(null)
        );
        assertEquals("Response ID cannot be null or empty", exception.getMessage());
    }

    @Test
    public void testDeleteResponse_emptyId_throwsException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> repository.deleteResponse("")
        );
        assertEquals("Response ID cannot be null or empty", exception.getMessage());
    }

    @Test
    public void testDeleteResponse_blankId_throwsException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> repository.deleteResponse("   ")
        );
        assertEquals("Response ID cannot be null or empty", exception.getMessage());
    }

    // ==================== deleteResponsesByQuestionId Tests ====================

    @Test
    public void testDeleteResponsesByQuestionId_validId_deletesAll() {
        List<ResponseEntity> entities = Arrays.asList(
                createTestEntity("id1"),
                createTestEntity("id2")
        );
        when(mockDao.getByQuestionId(TEST_QUESTION_ID)).thenReturn(entities);

        repository.deleteResponsesByQuestionId(TEST_QUESTION_ID);

        verify(mockDao).deleteByQuestionId(TEST_QUESTION_ID);
    }

    @Test
    public void testDeleteResponsesByQuestionId_nullId_throwsException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> repository.deleteResponsesByQuestionId(null)
        );
        assertEquals("Question ID cannot be null or empty", exception.getMessage());
    }

    @Test
    public void testDeleteResponsesByQuestionId_emptyId_throwsException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> repository.deleteResponsesByQuestionId("")
        );
        assertEquals("Question ID cannot be null or empty", exception.getMessage());
    }

    @Test
    public void testDeleteResponsesByQuestionId_blankId_throwsException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> repository.deleteResponsesByQuestionId("   ")
        );
        assertEquals("Question ID cannot be null or empty", exception.getMessage());
    }

    // ==================== deleteAllResponses Tests ====================

    @Test
    public void testDeleteAllResponses_clearsQueueAndDao() {
        // Add to queue first
        Response response = createTestResponse(false);
        repository.saveResponse(response);
        assertEquals(1, repository.getSyncQueueSize());

        repository.deleteAllResponses();

        verify(mockDao).deleteAll();
        assertEquals(0, repository.getSyncQueueSize());
    }

    // ==================== syncPendingResponses Tests ====================

    @Test
    public void testSyncPendingResponses_noUnsynced_completesImmediately() throws InterruptedException {
        when(mockDao.getUnsynced()).thenReturn(Collections.emptyList());

        CountDownLatch latch = new CountDownLatch(1);
        AtomicInteger successCount = new AtomicInteger(-1);
        AtomicInteger failureCount = new AtomicInteger(-1);

        repository.syncPendingResponses(new ResponseRepository.SyncCallback() {
            @Override
            public void onSyncSuccess(String responseId) {}

            @Override
            public void onSyncFailure(String responseId, String error) {}

            @Override
            public void onSyncComplete(int success, int failure) {
                successCount.set(success);
                failureCount.set(failure);
                latch.countDown();
            }
        });

        assertTrue(latch.await(5, TimeUnit.SECONDS));
        assertEquals(0, successCount.get());
        assertEquals(0, failureCount.get());
    }

    @Test
    public void testSyncPendingResponses_allSuccess_marksAsSynced() throws InterruptedException {
        ResponseEntity entity = createTestEntity();
        when(mockDao.getUnsynced()).thenReturn(Collections.singletonList(entity));
        when(mockSyncEngine.syncResponse(any())).thenReturn(true);

        CountDownLatch latch = new CountDownLatch(1);
        AtomicInteger successCount = new AtomicInteger(-1);

        repository.syncPendingResponses(new ResponseRepository.SyncCallback() {
            @Override
            public void onSyncSuccess(String responseId) {}

            @Override
            public void onSyncFailure(String responseId, String error) {}

            @Override
            public void onSyncComplete(int success, int failure) {
                successCount.set(success);
                latch.countDown();
            }
        });

        assertTrue(latch.await(5, TimeUnit.SECONDS));
        assertEquals(1, successCount.get());
        verify(mockDao).markSynced(TEST_ID);
    }

    @Test
    public void testSyncPendingResponses_allFailure_reportsFailure() throws InterruptedException {
        ResponseEntity entity = createTestEntity();
        when(mockDao.getUnsynced()).thenReturn(Collections.singletonList(entity));
        when(mockSyncEngine.syncResponse(any())).thenReturn(false);

        CountDownLatch latch = new CountDownLatch(1);
        AtomicInteger failureCount = new AtomicInteger(-1);

        repository.syncPendingResponses(new ResponseRepository.SyncCallback() {
            @Override
            public void onSyncSuccess(String responseId) {}

            @Override
            public void onSyncFailure(String responseId, String error) {}

            @Override
            public void onSyncComplete(int success, int failure) {
                failureCount.set(failure);
                latch.countDown();
            }
        });

        assertTrue(latch.await(5, TimeUnit.SECONDS));
        assertEquals(1, failureCount.get());
        verify(mockDao, never()).markSynced(anyString());
    }

    @Test
    public void testSyncPendingResponses_withoutCallback_executesSync() throws InterruptedException {
        ResponseEntity entity = createTestEntity();
        when(mockDao.getUnsynced()).thenReturn(Collections.singletonList(entity));
        when(mockSyncEngine.syncResponse(any())).thenReturn(true);

        repository.syncPendingResponses();

        // Wait for async execution
        Thread.sleep(100);

        verify(mockDao).markSynced(TEST_ID);
    }

    @Test
    public void testSyncPendingResponses_alreadySyncing_skipsExecution() throws InterruptedException {
        // Use a real ResponseRepositoryImpl (not testable) with a slow sync engine
        ResponseRepositoryImpl.SyncEngine slowEngine = entity -> {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return true;
        };

        ResponseRepositoryImpl realRepo = new ResponseRepositoryImpl(mockDao, slowEngine);
        ResponseEntity entity = createTestEntity();
        when(mockDao.getUnsynced()).thenReturn(Collections.singletonList(entity));

        // Start first sync
        realRepo.syncPendingResponses();
        Thread.sleep(50); // Let it start

        // Try to start second sync
        realRepo.syncPendingResponses();

        // Wait for completion
        Thread.sleep(600);

        // Should only have called getUnsynced once (not twice)
        verify(mockDao, times(1)).getUnsynced();
    }

    // ==================== syncWithRetry Tests ====================

    @Test
    public void testSyncWithRetry_immediateSuccess_returnsTrue() {
        ResponseEntity entity = createTestEntity();
        when(mockSyncEngine.syncResponse(entity)).thenReturn(true);

        boolean result = repository.syncWithRetry(entity);

        assertTrue(result);
        verify(mockSyncEngine, times(1)).syncResponse(entity);
    }

    @Test
    public void testSyncWithRetry_successOnSecondAttempt_returnsTrue() {
        ResponseEntity entity = createTestEntity();
        when(mockSyncEngine.syncResponse(entity))
                .thenReturn(false)
                .thenReturn(true);

        boolean result = repository.syncWithRetry(entity);

        assertTrue(result);
        verify(mockSyncEngine, times(2)).syncResponse(entity);
    }

    @Test
    public void testSyncWithRetry_allAttemptsFail_returnsFalse() {
        ResponseEntity entity = createTestEntity();
        when(mockSyncEngine.syncResponse(entity)).thenReturn(false);

        boolean result = repository.syncWithRetry(entity);

        assertFalse(result);
        verify(mockSyncEngine, times(ResponseRepositoryImpl.MAX_RETRY_ATTEMPTS)).syncResponse(entity);
    }

    @Test
    public void testSyncWithRetry_successOnLastAttempt_returnsTrue() {
        ResponseEntity entity = createTestEntity();
        // Fail 4 times, succeed on 5th (last) attempt
        when(mockSyncEngine.syncResponse(entity))
                .thenReturn(false)
                .thenReturn(false)
                .thenReturn(false)
                .thenReturn(false)
                .thenReturn(true);

        boolean result = repository.syncWithRetry(entity);

        assertTrue(result);
        verify(mockSyncEngine, times(5)).syncResponse(entity);
    }

    // ==================== calculateNextBackoff Tests ====================

    @Test
    public void testCalculateNextBackoff_initialValue_doubles() {
        long next = repository.calculateNextBackoff(ResponseRepositoryImpl.INITIAL_BACKOFF_MS);

        assertEquals(ResponseRepositoryImpl.INITIAL_BACKOFF_MS * 2, next);
    }

    @Test
    public void testCalculateNextBackoff_approachingMax_capsAtMax() {
        long next = repository.calculateNextBackoff(ResponseRepositoryImpl.MAX_BACKOFF_MS);

        assertEquals(ResponseRepositoryImpl.MAX_BACKOFF_MS, next);
    }

    @Test
    public void testCalculateNextBackoff_exceedsMax_capsAtMax() {
        long largeValue = ResponseRepositoryImpl.MAX_BACKOFF_MS * 2;
        long next = repository.calculateNextBackoff(largeValue);

        assertEquals(ResponseRepositoryImpl.MAX_BACKOFF_MS, next);
    }

    // ==================== isSyncing Tests ====================

    @Test
    public void testIsSyncing_notSyncing_returnsFalse() {
        assertFalse(repository.isSyncing());
    }

    // ==================== Callback Tests ====================

    @Test
    public void testSyncCallback_onSyncSuccess_calledForEachSuccess() throws InterruptedException {
        List<ResponseEntity> entities = Arrays.asList(
                createTestEntity("id1"),
                createTestEntity("id2")
        );
        when(mockDao.getUnsynced()).thenReturn(entities);
        when(mockSyncEngine.syncResponse(any())).thenReturn(true);

        CountDownLatch latch = new CountDownLatch(1);
        List<String> successIds = new ArrayList<>();

        repository.syncPendingResponses(new ResponseRepository.SyncCallback() {
            @Override
            public void onSyncSuccess(String responseId) {
                successIds.add(responseId);
            }

            @Override
            public void onSyncFailure(String responseId, String error) {}

            @Override
            public void onSyncComplete(int success, int failure) {
                latch.countDown();
            }
        });

        assertTrue(latch.await(5, TimeUnit.SECONDS));
        assertEquals(2, successIds.size());
        assertTrue(successIds.contains("id1"));
        assertTrue(successIds.contains("id2"));
    }

    @Test
    public void testSyncCallback_onSyncFailure_calledForEachFailure() throws InterruptedException {
        List<ResponseEntity> entities = Arrays.asList(
                createTestEntity("id1"),
                createTestEntity("id2")
        );
        when(mockDao.getUnsynced()).thenReturn(entities);
        when(mockSyncEngine.syncResponse(any())).thenReturn(false);

        CountDownLatch latch = new CountDownLatch(1);
        List<String> failureIds = new ArrayList<>();

        repository.syncPendingResponses(new ResponseRepository.SyncCallback() {
            @Override
            public void onSyncSuccess(String responseId) {}

            @Override
            public void onSyncFailure(String responseId, String error) {
                failureIds.add(responseId);
            }

            @Override
            public void onSyncComplete(int success, int failure) {
                latch.countDown();
            }
        });

        assertTrue(latch.await(5, TimeUnit.SECONDS));
        assertEquals(2, failureIds.size());
    }

    // ==================== DefaultSyncEngine Tests ====================

    @Test
    public void testDefaultSyncEngine_syncResponse_returnsTrue() throws InterruptedException {
        // Use the constructor that creates DefaultSyncEngine
        ResponseRepositoryImpl repoWithDefaultEngine = new ResponseRepositoryImpl(mockDao);
        ResponseEntity entity = createTestEntity();
        when(mockDao.getUnsynced()).thenReturn(Collections.singletonList(entity));

        CountDownLatch latch = new CountDownLatch(1);
        AtomicInteger successCount = new AtomicInteger(-1);

        repoWithDefaultEngine.syncPendingResponses(new ResponseRepository.SyncCallback() {
            @Override
            public void onSyncSuccess(String responseId) {}

            @Override
            public void onSyncFailure(String responseId, String error) {}

            @Override
            public void onSyncComplete(int success, int failure) {
                successCount.set(success);
                latch.countDown();
            }
        });

        assertTrue(latch.await(5, TimeUnit.SECONDS));
        assertEquals(1, successCount.get());
        verify(mockDao).markSynced(TEST_ID);
    }

    // ==================== Constants Tests ====================

    @Test
    public void testConstants_haveCorrectValues() {
        assertEquals(1000L, ResponseRepositoryImpl.INITIAL_BACKOFF_MS);
        assertEquals(32000L, ResponseRepositoryImpl.MAX_BACKOFF_MS);
        assertEquals(5, ResponseRepositoryImpl.MAX_RETRY_ATTEMPTS);
        assertEquals(2.0, ResponseRepositoryImpl.BACKOFF_MULTIPLIER, 0.001);
    }

    // ==================== Sleep Tests ====================

    @Test
    public void testSleep_normalExecution_completesWithoutException() {
        // Use a real repository (not testable) to test actual sleep
        ResponseRepositoryImpl realRepo = new ResponseRepositoryImpl(mockDao);
        realRepo.sleep(1);
    }

    @Test
    public void testSleep_interrupted_setsInterruptFlag() throws InterruptedException {
        // Use a real repository (not testable) to test actual sleep with interrupt
        ResponseRepositoryImpl realRepo = new ResponseRepositoryImpl(mockDao);

        Thread testThread = new Thread(() -> {
            Thread.currentThread().interrupt();
            realRepo.sleep(1000);
            assertTrue(Thread.currentThread().isInterrupted());
        });

        testThread.start();
        testThread.join(2000);
    }

    // ==================== Helper Methods ====================

    private Response createTestResponse(boolean synced) {
        return new Response(
                TEST_ID,
                TEST_QUESTION_ID,
                "Test answer",
                false,
                System.currentTimeMillis(),
                synced,
                TEST_DEVICE_ID
        );
    }

    private ResponseEntity createTestEntity() {
        return createTestEntity(TEST_ID);
    }

    private ResponseEntity createTestEntity(String id) {
        return new ResponseEntity(
                id,
                TEST_QUESTION_ID,
                "Test answer",
                false,
                System.currentTimeMillis(),
                false,
                TEST_DEVICE_ID
        );
    }

    /**
     * Testable subclass that overrides sleep to avoid actual delays.
     */
    private static class TestableResponseRepository extends ResponseRepositoryImpl {
        TestableResponseRepository(ResponseDao responseDao, SyncEngine syncEngine) {
            super(responseDao, syncEngine);
        }

        @Override
        protected void sleep(long millis) {
            // Do nothing - skip actual sleep in tests
        }
    }
}
