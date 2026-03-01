package com.manuscripta.student.data.repository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.manuscripta.student.data.model.ResponseEntity;
import com.manuscripta.student.network.ApiService;
import com.manuscripta.student.network.dto.ResponseDto;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;

import okhttp3.MediaType;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Response;

/**
 * Unit tests for {@link ResponseRepositoryImpl.NetworkSyncEngine}.
 */
public class NetworkSyncEngineTest {

    @Mock
    private ApiService mockApiService;

    @Mock
    private Call<Void> mockCall;

    private ResponseRepositoryImpl.SyncEngine syncEngine;

    private static final String TEST_ID = "test-response-id";
    private static final String TEST_QUESTION_ID = "test-question-id";
    private static final String TEST_DEVICE_ID = "test-device-id";
    private static final long TEST_TIMESTAMP = 1640000000000L;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    // ========== Constructor tests ==========

    @Test
    public void testConstructor_nullApiService_throwsException() {
        assertThrows(IllegalArgumentException.class,
                () -> new ResponseRepositoryImpl.NetworkSyncEngine(null));
    }

    @Test
    public void testConstructor_validApiService_succeeds() {
        syncEngine = new ResponseRepositoryImpl.NetworkSyncEngine(mockApiService);
        assertNotNull(syncEngine);
    }

    // ========== syncResponse tests ==========

    @Test
    public void testSyncResponse_http201_returnsTrue() throws IOException {
        // Arrange
        syncEngine = new ResponseRepositoryImpl.NetworkSyncEngine(mockApiService);
        ResponseEntity entity = createTestEntity();

        Response<Void> httpResponse = Response.success(null,
                new okhttp3.Response.Builder()
                        .request(new Request.Builder().url("http://test.com").build())
                        .protocol(Protocol.HTTP_1_1)
                        .code(201)
                        .message("Created")
                        .build());

        when(mockApiService.submitResponse(any(ResponseDto.class))).thenReturn(mockCall);
        when(mockCall.execute()).thenReturn(httpResponse);

        // Act
        boolean result = syncEngine.syncResponse(entity);

        // Assert
        assertTrue(result);
    }

    @Test
    public void testSyncResponse_http200_returnsFalse() throws IOException {
        // Arrange
        syncEngine = new ResponseRepositoryImpl.NetworkSyncEngine(mockApiService);
        ResponseEntity entity = createTestEntity();

        Response<Void> httpResponse = Response.success(null,
                new okhttp3.Response.Builder()
                        .request(new Request.Builder().url("http://test.com").build())
                        .protocol(Protocol.HTTP_1_1)
                        .code(200)
                        .message("OK")
                        .build());

        when(mockApiService.submitResponse(any(ResponseDto.class))).thenReturn(mockCall);
        when(mockCall.execute()).thenReturn(httpResponse);

        // Act
        boolean result = syncEngine.syncResponse(entity);

        // Assert
        assertFalse(result);
    }

    @Test
    public void testSyncResponse_http400_returnsFalse() throws IOException {
        // Arrange
        syncEngine = new ResponseRepositoryImpl.NetworkSyncEngine(mockApiService);
        ResponseEntity entity = createTestEntity();

        Response<Void> httpResponse = Response.error(400,
                ResponseBody.create("Bad Request", MediaType.parse("text/plain")));

        when(mockApiService.submitResponse(any(ResponseDto.class))).thenReturn(mockCall);
        when(mockCall.execute()).thenReturn(httpResponse);

        // Act
        boolean result = syncEngine.syncResponse(entity);

        // Assert
        assertFalse(result);
    }

    @Test
    public void testSyncResponse_http500_returnsFalse() throws IOException {
        // Arrange
        syncEngine = new ResponseRepositoryImpl.NetworkSyncEngine(mockApiService);
        ResponseEntity entity = createTestEntity();

        Response<Void> httpResponse = Response.error(500,
                ResponseBody.create("Internal Server Error", MediaType.parse("text/plain")));

        when(mockApiService.submitResponse(any(ResponseDto.class))).thenReturn(mockCall);
        when(mockCall.execute()).thenReturn(httpResponse);

        // Act
        boolean result = syncEngine.syncResponse(entity);

        // Assert
        assertFalse(result);
    }

    @Test
    public void testSyncResponse_ioException_returnsFalse() throws IOException {
        // Arrange
        syncEngine = new ResponseRepositoryImpl.NetworkSyncEngine(mockApiService);
        ResponseEntity entity = createTestEntity();

        when(mockApiService.submitResponse(any(ResponseDto.class))).thenReturn(mockCall);
        when(mockCall.execute()).thenThrow(new IOException("Network error"));

        // Act
        boolean result = syncEngine.syncResponse(entity);

        // Assert
        assertFalse(result);
    }

    @Test
    public void testSyncResponse_unexpectedException_returnsFalse() throws IOException {
        // Arrange
        syncEngine = new ResponseRepositoryImpl.NetworkSyncEngine(mockApiService);
        ResponseEntity entity = createTestEntity();

        when(mockApiService.submitResponse(any(ResponseDto.class))).thenReturn(mockCall);
        when(mockCall.execute()).thenThrow(new RuntimeException("Unexpected error"));

        // Act
        boolean result = syncEngine.syncResponse(entity);

        // Assert
        assertFalse(result);
    }

    @Test
    public void testSyncResponse_correctDtoCreated() throws IOException {
        // Arrange
        syncEngine = new ResponseRepositoryImpl.NetworkSyncEngine(mockApiService);
        ResponseEntity entity = createTestEntity();

        ArgumentCaptor<ResponseDto> dtoCaptor = ArgumentCaptor.forClass(ResponseDto.class);

        Response<Void> httpResponse = Response.success(null,
                new okhttp3.Response.Builder()
                        .request(new Request.Builder().url("http://test.com").build())
                        .protocol(Protocol.HTTP_1_1)
                        .code(201)
                        .message("Created")
                        .build());

        when(mockApiService.submitResponse(dtoCaptor.capture())).thenReturn(mockCall);
        when(mockCall.execute()).thenReturn(httpResponse);

        // Act
        syncEngine.syncResponse(entity);

        // Assert
        ResponseDto capturedDto = dtoCaptor.getValue();
        assertNotNull(capturedDto);
        assertEquals(TEST_ID, capturedDto.getId());
        assertEquals(TEST_QUESTION_ID, capturedDto.getQuestionId());
        assertEquals(TEST_DEVICE_ID, capturedDto.getStudentId());
        assertEquals("2021-12-20T11:33:20Z", capturedDto.getTimestamp());
        assertEquals("Test answer", capturedDto.getAnswer());
        assertNull(capturedDto.getMaterialId());
    }

    // ========== Helper methods ==========

    /**
     * Creates a test ResponseEntity with predefined values.
     *
     * @return Test ResponseEntity
     */
    private ResponseEntity createTestEntity() {
        return new ResponseEntity(
                TEST_ID,
                TEST_QUESTION_ID,
                "Test answer",
                false,
                TEST_TIMESTAMP,
                false,
                TEST_DEVICE_ID
        );
    }
}
