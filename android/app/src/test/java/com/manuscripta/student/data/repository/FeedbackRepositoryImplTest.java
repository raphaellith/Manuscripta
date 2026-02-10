package com.manuscripta.student.data.repository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.manuscripta.student.data.local.FeedbackDao;
import com.manuscripta.student.data.model.FeedbackEntity;
import com.manuscripta.student.domain.model.Feedback;
import com.manuscripta.student.network.ApiService;
import com.manuscripta.student.network.FeedbackDto;
import com.manuscripta.student.network.FeedbackResponse;
import com.manuscripta.student.network.tcp.TcpSocketManager;
import com.manuscripta.student.network.tcp.message.FeedbackAckMessage;

import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import okhttp3.MediaType;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Response;

/**
 * Unit tests for {@link FeedbackRepositoryImpl}.
 */
public class FeedbackRepositoryImplTest {

    private FeedbackDao mockDao;
    private ApiService mockApiService;
    private TcpSocketManager mockTcpSocketManager;
    private FeedbackRepositoryImpl repository;

    private static final String TEST_DEVICE_ID = "test-device-id";
    private static final String TEST_FEEDBACK_ID = "feedback-123";
    private static final String TEST_RESPONSE_ID = "response-456";

    @Before
    public void setUp() {
        mockDao = mock(FeedbackDao.class);
        mockApiService = mock(ApiService.class);
        mockTcpSocketManager = mock(TcpSocketManager.class);
        repository = new FeedbackRepositoryImpl(mockDao, mockApiService, mockTcpSocketManager);
    }

    // ==================== fetchAndStoreFeedback Tests ====================

    @Test
    public void testFetchAndStoreFeedback_nullDeviceId_throwsException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> repository.fetchAndStoreFeedback(null)
        );
        assertEquals("Device ID cannot be null or empty", exception.getMessage());
    }

    @Test
    public void testFetchAndStoreFeedback_emptyDeviceId_throwsException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> repository.fetchAndStoreFeedback("")
        );
        assertEquals("Device ID cannot be null or empty", exception.getMessage());
    }

    @Test
    public void testFetchAndStoreFeedback_blankDeviceId_throwsException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> repository.fetchAndStoreFeedback("   ")
        );
        assertEquals("Device ID cannot be null or empty", exception.getMessage());
    }

    @Test
    public void testFetchAndStoreFeedback_successfulResponse_storesFeedbackAndSendsAck()
            throws Exception {
        // Given
        FeedbackDto dto = new FeedbackDto(TEST_FEEDBACK_ID, TEST_RESPONSE_ID, "Good work!", 85);
        FeedbackResponse feedbackResponse = new FeedbackResponse(Collections.singletonList(dto));
        Response<FeedbackResponse> response = Response.success(feedbackResponse);
        Call<FeedbackResponse> mockCall = mock(Call.class);
        when(mockCall.execute()).thenReturn(response);
        when(mockApiService.getFeedback(TEST_DEVICE_ID)).thenReturn(mockCall);

        // When
        repository.fetchAndStoreFeedback(TEST_DEVICE_ID);

        // Then
        verify(mockDao).insertAll(anyList());
        verify(mockTcpSocketManager).send(any(FeedbackAckMessage.class));
    }

    @Test
    public void testFetchAndStoreFeedback_404Response_sendsAckWithoutStoring() throws Exception {
        // Given
        Response<FeedbackResponse> response = Response.error(404,
                ResponseBody.create(MediaType.parse("application/json"), "Not found"));
        Call<FeedbackResponse> mockCall = mock(Call.class);
        when(mockCall.execute()).thenReturn(response);
        when(mockApiService.getFeedback(TEST_DEVICE_ID)).thenReturn(mockCall);

        // When
        repository.fetchAndStoreFeedback(TEST_DEVICE_ID);

        // Then
        verify(mockDao, never()).insertAll(anyList());
        verify(mockTcpSocketManager).send(any(FeedbackAckMessage.class));
    }

    @Test
    public void testFetchAndStoreFeedback_httpError_throwsIOException() throws Exception {
        // Given
        Response<FeedbackResponse> response = Response.error(500,
                ResponseBody.create(MediaType.parse("application/json"), "Server error"));
        Call<FeedbackResponse> mockCall = mock(Call.class);
        when(mockCall.execute()).thenReturn(response);
        when(mockApiService.getFeedback(TEST_DEVICE_ID)).thenReturn(mockCall);

        // When/Then
        IOException exception = assertThrows(
                IOException.class,
                () -> repository.fetchAndStoreFeedback(TEST_DEVICE_ID)
        );
        assertTrue(exception.getMessage().contains("HTTP 500"));
    }

    @Test
    public void testFetchAndStoreFeedback_nullResponseBody_sendsAckWithoutStoring()
            throws Exception {
        // Given
        Response<FeedbackResponse> response = Response.success(null);
        Call<FeedbackResponse> mockCall = mock(Call.class);
        when(mockCall.execute()).thenReturn(response);
        when(mockApiService.getFeedback(TEST_DEVICE_ID)).thenReturn(mockCall);

        // When
        repository.fetchAndStoreFeedback(TEST_DEVICE_ID);

        // Then
        verify(mockDao, never()).insertAll(anyList());
        verify(mockTcpSocketManager).send(any(FeedbackAckMessage.class));
    }

    @Test
    public void testFetchAndStoreFeedback_nullFeedbackList_sendsAckWithoutStoring()
            throws Exception {
        // Given
        FeedbackResponse feedbackResponse = new FeedbackResponse(null);
        Response<FeedbackResponse> response = Response.success(feedbackResponse);
        Call<FeedbackResponse> mockCall = mock(Call.class);
        when(mockCall.execute()).thenReturn(response);
        when(mockApiService.getFeedback(TEST_DEVICE_ID)).thenReturn(mockCall);

        // When
        repository.fetchAndStoreFeedback(TEST_DEVICE_ID);

        // Then
        verify(mockDao, never()).insertAll(anyList());
        verify(mockTcpSocketManager).send(any(FeedbackAckMessage.class));
    }

    @Test
    public void testFetchAndStoreFeedback_emptyFeedbackList_sendsAckWithoutStoring()
            throws Exception {
        // Given
        FeedbackResponse feedbackResponse = new FeedbackResponse(Collections.emptyList());
        Response<FeedbackResponse> response = Response.success(feedbackResponse);
        Call<FeedbackResponse> mockCall = mock(Call.class);
        when(mockCall.execute()).thenReturn(response);
        when(mockApiService.getFeedback(TEST_DEVICE_ID)).thenReturn(mockCall);

        // When
        repository.fetchAndStoreFeedback(TEST_DEVICE_ID);

        // Then
        verify(mockDao, never()).insertAll(anyList());
        verify(mockTcpSocketManager).send(any(FeedbackAckMessage.class));
    }

    @Test
    public void testFetchAndStoreFeedback_invalidFeedback_skipsInvalidAndStoresValid()
            throws Exception {
        // Given - one valid feedback with marks, one invalid (no text and no marks)
        FeedbackDto validDto = new FeedbackDto(TEST_FEEDBACK_ID, TEST_RESPONSE_ID, null, 85);
        FeedbackDto invalidDto = new FeedbackDto("invalid-id", "resp-2", null, null);
        FeedbackResponse feedbackResponse = new FeedbackResponse(
                Arrays.asList(validDto, invalidDto));
        Response<FeedbackResponse> response = Response.success(feedbackResponse);
        Call<FeedbackResponse> mockCall = mock(Call.class);
        when(mockCall.execute()).thenReturn(response);
        when(mockApiService.getFeedback(TEST_DEVICE_ID)).thenReturn(mockCall);

        // When
        repository.fetchAndStoreFeedback(TEST_DEVICE_ID);

        // Then - should still insert (only the valid one)
        verify(mockDao).insertAll(anyList());
        verify(mockTcpSocketManager).send(any(FeedbackAckMessage.class));
    }

    @Test
    public void testFetchAndStoreFeedback_tcpSendFails_logsErrorButDoesNotThrow()
            throws Exception {
        // Given
        FeedbackDto dto = new FeedbackDto(TEST_FEEDBACK_ID, TEST_RESPONSE_ID, "Feedback", 90);
        FeedbackResponse feedbackResponse = new FeedbackResponse(Collections.singletonList(dto));
        Response<FeedbackResponse> response = Response.success(feedbackResponse);
        Call<FeedbackResponse> mockCall = mock(Call.class);
        when(mockCall.execute()).thenReturn(response);
        when(mockApiService.getFeedback(TEST_DEVICE_ID)).thenReturn(mockCall);
        doThrow(new IOException("Connection failed"))
                .when(mockTcpSocketManager).send(any(FeedbackAckMessage.class));

        // When - should not throw even though TCP send fails
        repository.fetchAndStoreFeedback(TEST_DEVICE_ID);

        // Then
        verify(mockDao).insertAll(anyList());
    }

    // ==================== getFeedbackForResponse Tests ====================

    @Test
    public void testGetFeedbackForResponse_existingResponse_returnsFeedback() {
        // Given
        FeedbackEntity entity = new FeedbackEntity(TEST_FEEDBACK_ID, TEST_RESPONSE_ID,
                "Great job!", 95);
        when(mockDao.getByResponseId(TEST_RESPONSE_ID)).thenReturn(entity);

        // When
        Feedback result = repository.getFeedbackForResponse(TEST_RESPONSE_ID);

        // Then
        assertNotNull(result);
        assertEquals(TEST_FEEDBACK_ID, result.getId());
        assertEquals(TEST_RESPONSE_ID, result.getResponseId());
        assertEquals("Great job!", result.getText());
        assertEquals(Integer.valueOf(95), result.getMarks());
    }

    @Test
    public void testGetFeedbackForResponse_nonExistent_returnsNull() {
        // Given
        when(mockDao.getByResponseId(TEST_RESPONSE_ID)).thenReturn(null);

        // When
        Feedback result = repository.getFeedbackForResponse(TEST_RESPONSE_ID);

        // Then
        assertNull(result);
    }

    // ==================== getAllFeedback Tests ====================

    @Test
    public void testGetAllFeedback_hasFeedback_returnsList() {
        // Given
        List<FeedbackEntity> entities = Arrays.asList(
                new FeedbackEntity("id1", "resp1", "Feedback 1", 80),
                new FeedbackEntity("id2", "resp2", "Feedback 2", 90)
        );
        when(mockDao.getAll()).thenReturn(entities);

        // When
        List<Feedback> result = repository.getAllFeedback();

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
    }

    @Test
    public void testGetAllFeedback_noFeedback_returnsEmptyList() {
        // Given
        when(mockDao.getAll()).thenReturn(Collections.emptyList());

        // When
        List<Feedback> result = repository.getAllFeedback();

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // ==================== getAllFeedbackByDeviceId Tests ====================

    @Test
    public void testGetAllFeedbackByDeviceId_hasFeedback_returnsList() {
        // Given
        List<FeedbackEntity> entities = Arrays.asList(
                new FeedbackEntity("id1", "resp1", "Feedback 1", 80),
                new FeedbackEntity("id2", "resp2", "Feedback 2", 90)
        );
        when(mockDao.getAllByDeviceId(TEST_DEVICE_ID)).thenReturn(entities);

        // When
        List<Feedback> result = repository.getAllFeedbackByDeviceId(TEST_DEVICE_ID);

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
    }

    // ==================== deleteAllFeedback Tests ====================

    @Test
    public void testDeleteAllFeedback_callsDao() {
        // When
        repository.deleteAllFeedback();

        // Then
        verify(mockDao).deleteAll();
    }

    // ==================== getFeedbackCount Tests ====================

    @Test
    public void testGetFeedbackCount_returnsCount() {
        // Given
        when(mockDao.getCount()).thenReturn(5);

        // When
        int result = repository.getFeedbackCount();

        // Then
        assertEquals(5, result);
    }
}
