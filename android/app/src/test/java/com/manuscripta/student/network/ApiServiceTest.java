package com.manuscripta.student.network;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.manuscripta.student.network.dto.BatchResponseDto;
import com.manuscripta.student.network.dto.ConfigResponseDto;
import com.manuscripta.student.network.dto.DeviceInfoDto;
import com.manuscripta.student.network.dto.DistributionBundleDto;
import com.manuscripta.student.network.dto.MaterialDto;
import com.manuscripta.student.network.dto.FeedbackResponse;
import com.manuscripta.student.network.dto.QuestionDto;
import com.manuscripta.student.network.dto.ResponseDto;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.ResponseBody;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Unit tests for {@link ApiService} using MockWebServer.
 * Tests all HTTP endpoints defined in API Contract §2.
 *
 * <p>Related requirements: NET1, NET2, MAT8 (Teacher), MAT15</p>
 */
public class ApiServiceTest {

    private static final String TEST_DEVICE_ID = "device-11111111-1111-1111-1111-111111111111";
    private static final String TEST_ATTACHMENT_ID = "att-22222222-2222-2222-2222-222222222222";
    private static final String TEST_MATERIAL_ID = "mat-33333333-3333-3333-3333-333333333333";
    private static final String TEST_QUESTION_ID = "q-44444444-4444-4444-4444-444444444444";
    private static final String TEST_RESPONSE_ID = "resp-55555555-5555-5555-5555-555555555555";
    private static final String TEST_FEEDBACK_ID = "fb-66666666-6666-6666-6666-666666666666";

    private MockWebServer mockWebServer;
    private ApiService apiService;
    private Gson gson;

    @Before
    public void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        gson = new GsonBuilder().create();

        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(1, TimeUnit.SECONDS)
                .readTimeout(1, TimeUnit.SECONDS)
                .writeTimeout(1, TimeUnit.SECONDS)
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(mockWebServer.url("/"))
                .client(client)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build();

        apiService = retrofit.create(ApiService.class);
    }

    @After
    public void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    // ========== GET /attachments/{id} Tests ==========

    @Test
    public void testGetAttachmentSuccess() throws IOException, InterruptedException {
        byte[] binaryData = new byte[] {0x25, 0x50, 0x44, 0x46}; // PDF magic bytes
        mockWebServer.enqueue(new MockResponse()
                .setBody(new okio.Buffer().write(binaryData))
                .setHeader("Content-Type", "application/pdf")
                .setResponseCode(200));

        Call<ResponseBody> call = apiService.getAttachment(TEST_ATTACHMENT_ID);
        Response<ResponseBody> response = call.execute();

        assertTrue(response.isSuccessful());
        assertNotNull(response.body());

        RecordedRequest request = mockWebServer.takeRequest();
        assertEquals("GET", request.getMethod());
        assertEquals("/attachments/" + TEST_ATTACHMENT_ID, request.getPath());
    }

    @Test
    public void testGetAttachmentReturnsImageBinary() throws IOException {
        byte[] pngData = new byte[] {(byte) 0x89, 0x50, 0x4E, 0x47}; // PNG magic bytes
        mockWebServer.enqueue(new MockResponse()
                .setBody(new okio.Buffer().write(pngData))
                .setHeader("Content-Type", "image/png")
                .setResponseCode(200));

        Call<ResponseBody> call = apiService.getAttachment("img-12345");
        Response<ResponseBody> response = call.execute();

        assertTrue(response.isSuccessful());
        assertNotNull(response.body());
        byte[] responseBytes = response.body().bytes();
        assertEquals(4, responseBytes.length);
        assertEquals((byte) 0x89, responseBytes[0]);
    }

    @Test
    public void testGetAttachmentNotFound() throws IOException {
        mockWebServer.enqueue(new MockResponse().setResponseCode(404));

        Call<ResponseBody> call = apiService.getAttachment("nonexistent-id");
        Response<ResponseBody> response = call.execute();

        assertEquals(404, response.code());
    }

    // ========== GET /distribution/{deviceId} Tests ==========

    @Test
    public void testGetDistributionSuccess() throws IOException, InterruptedException {
        DistributionBundleDto bundle = createTestDistributionBundle();
        String jsonResponse = gson.toJson(bundle);

        mockWebServer.enqueue(new MockResponse()
                .setBody(jsonResponse)
                .setHeader("Content-Type", "application/json")
                .setResponseCode(200));

        Call<DistributionBundleDto> call = apiService.getDistribution(TEST_DEVICE_ID);
        Response<DistributionBundleDto> response = call.execute();

        assertTrue(response.isSuccessful());
        assertNotNull(response.body());
        assertEquals(1, response.body().getMaterials().size());
        assertEquals(1, response.body().getQuestions().size());

        RecordedRequest request = mockWebServer.takeRequest();
        assertEquals("GET", request.getMethod());
        assertEquals("/distribution/" + TEST_DEVICE_ID, request.getPath());
    }

    @Test
    public void testGetDistributionEmptyBundle() throws IOException {
        DistributionBundleDto emptyBundle = new DistributionBundleDto(
                Collections.emptyList(),
                Collections.emptyList()
        );
        String jsonResponse = gson.toJson(emptyBundle);

        mockWebServer.enqueue(new MockResponse()
                .setBody(jsonResponse)
                .setHeader("Content-Type", "application/json")
                .setResponseCode(200));

        Call<DistributionBundleDto> call = apiService.getDistribution(TEST_DEVICE_ID);
        Response<DistributionBundleDto> response = call.execute();

        assertTrue(response.isSuccessful());
        assertNotNull(response.body());
        assertTrue(response.body().getMaterials().isEmpty());
        assertTrue(response.body().getQuestions().isEmpty());
    }

    @Test
    public void testGetDistributionNotFound() throws IOException {
        mockWebServer.enqueue(new MockResponse().setResponseCode(404));

        Call<DistributionBundleDto> call = apiService.getDistribution("unknown-device");
        Response<DistributionBundleDto> response = call.execute();

        assertEquals(404, response.code());
    }

    @Test
    public void testGetDistributionWithMultipleMaterialsAndQuestions() throws IOException {
        List<MaterialDto> materials = Arrays.asList(
                new MaterialDto("mat-1", "WORKSHEET", "Math", "Content 1", null, null, 1L),
                new MaterialDto("mat-2", "READING", "Reading", "Content 2", null, null, 2L)
        );
        List<QuestionDto> questions = Arrays.asList(
                new QuestionDto("q-1", "mat-1", "MULTIPLE_CHOICE", "Q1?",
                        Arrays.asList("A", "B"), "A", 10),
                new QuestionDto("q-2", "mat-1", "WRITTEN_ANSWER", "Q2?",
                        null, null, 20)
        );
        DistributionBundleDto bundle = new DistributionBundleDto(materials, questions);
        String jsonResponse = gson.toJson(bundle);

        mockWebServer.enqueue(new MockResponse()
                .setBody(jsonResponse)
                .setHeader("Content-Type", "application/json")
                .setResponseCode(200));

        Call<DistributionBundleDto> call = apiService.getDistribution(TEST_DEVICE_ID);
        Response<DistributionBundleDto> response = call.execute();

        assertTrue(response.isSuccessful());
        assertNotNull(response.body());
        assertEquals(2, response.body().getMaterials().size());
        assertEquals(2, response.body().getQuestions().size());
    }

    // ========== GET /feedback/{deviceId} Tests ==========

    @Test
    public void testGetFeedbackSuccess() throws IOException, InterruptedException {
        String jsonResponse = "{\"feedback\":[{\"Id\":\"" + TEST_FEEDBACK_ID
                + "\",\"ResponseId\":\"" + TEST_RESPONSE_ID
                + "\",\"Text\":\"Great work!\",\"Marks\":90}]}";

        mockWebServer.enqueue(new MockResponse()
                .setBody(jsonResponse)
                .setHeader("Content-Type", "application/json")
                .setResponseCode(200));

        Call<FeedbackResponse> call = apiService.getFeedback(TEST_DEVICE_ID);
        Response<FeedbackResponse> response = call.execute();

        assertTrue(response.isSuccessful());
        assertNotNull(response.body());
        assertNotNull(response.body().getFeedback());
        assertEquals(1, response.body().getFeedback().size());

        RecordedRequest request = mockWebServer.takeRequest();
        assertEquals("GET", request.getMethod());
        assertEquals("/feedback/" + TEST_DEVICE_ID, request.getPath());
    }

    @Test
    public void testGetFeedbackEmpty() throws IOException {
        String jsonResponse = "{\"feedback\":[]}";

        mockWebServer.enqueue(new MockResponse()
                .setBody(jsonResponse)
                .setHeader("Content-Type", "application/json")
                .setResponseCode(200));

        Call<FeedbackResponse> call = apiService.getFeedback(TEST_DEVICE_ID);
        Response<FeedbackResponse> response = call.execute();

        assertTrue(response.isSuccessful());
        assertNotNull(response.body());
        assertTrue(response.body().getFeedback().isEmpty());
    }

    @Test
    public void testGetFeedbackNotFound() throws IOException {
        mockWebServer.enqueue(new MockResponse().setResponseCode(404));

        Call<FeedbackResponse> call = apiService.getFeedback("unknown-device");
        Response<FeedbackResponse> response = call.execute();

        assertEquals(404, response.code());
    }

    // ========== GET /config/{deviceId} Tests ==========

    @Test
    public void testGetConfigSuccess() throws IOException, InterruptedException {
        ConfigResponseDto config = new ConfigResponseDto(true, "medium");
        String jsonResponse = gson.toJson(config);

        mockWebServer.enqueue(new MockResponse()
                .setBody(jsonResponse)
                .setHeader("Content-Type", "application/json")
                .setResponseCode(200));

        Call<ConfigResponseDto> call = apiService.getConfig(TEST_DEVICE_ID);
        Response<ConfigResponseDto> response = call.execute();

        assertTrue(response.isSuccessful());
        assertNotNull(response.body());
        assertEquals(Boolean.TRUE, response.body().getKioskMode());
        assertEquals("medium", response.body().getTextSize());

        RecordedRequest request = mockWebServer.takeRequest();
        assertEquals("GET", request.getMethod());
        assertEquals("/config/" + TEST_DEVICE_ID, request.getPath());
    }

    @Test
    public void testGetConfigKioskModeDisabled() throws IOException {
        ConfigResponseDto config = new ConfigResponseDto(false, "large");
        String jsonResponse = gson.toJson(config);

        mockWebServer.enqueue(new MockResponse()
                .setBody(jsonResponse)
                .setHeader("Content-Type", "application/json")
                .setResponseCode(200));

        Call<ConfigResponseDto> call = apiService.getConfig(TEST_DEVICE_ID);
        Response<ConfigResponseDto> response = call.execute();

        assertTrue(response.isSuccessful());
        assertNotNull(response.body());
        assertEquals(Boolean.FALSE, response.body().getKioskMode());
        assertEquals("large", response.body().getTextSize());
    }

    @Test
    public void testGetConfigWithNullValues() throws IOException {
        ConfigResponseDto config = new ConfigResponseDto(null, null);
        String jsonResponse = gson.toJson(config);

        mockWebServer.enqueue(new MockResponse()
                .setBody(jsonResponse)
                .setHeader("Content-Type", "application/json")
                .setResponseCode(200));

        Call<ConfigResponseDto> call = apiService.getConfig(TEST_DEVICE_ID);
        Response<ConfigResponseDto> response = call.execute();

        assertTrue(response.isSuccessful());
        assertNotNull(response.body());
        assertNull(response.body().getKioskMode());
        assertNull(response.body().getTextSize());
    }

    // ========== POST /pair Tests ==========

    @Test
    public void testRegisterDeviceSuccess() throws IOException, InterruptedException {
        mockWebServer.enqueue(new MockResponse().setResponseCode(201));

        DeviceInfoDto deviceInfo = new DeviceInfoDto(TEST_DEVICE_ID, "Student Tablet 1");
        Call<Void> call = apiService.registerDevice(deviceInfo);
        Response<Void> response = call.execute();

        assertTrue(response.isSuccessful());
        assertEquals(201, response.code());

        RecordedRequest request = mockWebServer.takeRequest();
        assertEquals("POST", request.getMethod());
        assertEquals("/pair", request.getPath());

        String requestBody = request.getBody().readUtf8();
        assertTrue(requestBody.contains(TEST_DEVICE_ID));
        assertTrue(requestBody.contains("Student Tablet 1"));
    }

    @Test
    public void testRegisterDeviceConflict() throws IOException {
        mockWebServer.enqueue(new MockResponse().setResponseCode(409));

        DeviceInfoDto deviceInfo = new DeviceInfoDto(TEST_DEVICE_ID, "Already Paired Device");
        Call<Void> call = apiService.registerDevice(deviceInfo);
        Response<Void> response = call.execute();

        assertEquals(409, response.code());
    }

    @Test
    public void testRegisterDeviceBadRequest() throws IOException {
        mockWebServer.enqueue(new MockResponse().setResponseCode(400));

        DeviceInfoDto deviceInfo = new DeviceInfoDto(null, null);
        Call<Void> call = apiService.registerDevice(deviceInfo);
        Response<Void> response = call.execute();

        assertEquals(400, response.code());
    }

    // ========== POST /responses Tests ==========

    @Test
    public void testSubmitResponseSuccess() throws IOException, InterruptedException {
        mockWebServer.enqueue(new MockResponse().setResponseCode(201));

        ResponseDto response = new ResponseDto(
                TEST_RESPONSE_ID,
                TEST_QUESTION_ID,
                TEST_MATERIAL_ID,
                TEST_DEVICE_ID,
                "B",
                "2023-10-27T10:05:00Z",
                true
        );
        Call<Void> call = apiService.submitResponse(response);
        Response<Void> result = call.execute();

        assertTrue(result.isSuccessful());
        assertEquals(201, result.code());

        RecordedRequest request = mockWebServer.takeRequest();
        assertEquals("POST", request.getMethod());
        assertEquals("/responses", request.getPath());

        String requestBody = request.getBody().readUtf8();
        assertTrue(requestBody.contains(TEST_RESPONSE_ID));
        assertTrue(requestBody.contains(TEST_QUESTION_ID));
    }

    @Test
    public void testSubmitResponseBadRequest() throws IOException {
        mockWebServer.enqueue(new MockResponse().setResponseCode(400));

        ResponseDto response = new ResponseDto(null, null, null, null, null, null, null);
        Call<Void> call = apiService.submitResponse(response);
        Response<Void> result = call.execute();

        assertEquals(400, result.code());
    }

    @Test
    public void testSubmitResponseWithWrittenAnswer() throws IOException, InterruptedException {
        mockWebServer.enqueue(new MockResponse().setResponseCode(201));

        ResponseDto response = new ResponseDto(
                TEST_RESPONSE_ID,
                TEST_QUESTION_ID,
                TEST_MATERIAL_ID,
                TEST_DEVICE_ID,
                "This is my written answer explaining the concept.",
                "2023-10-27T10:05:00Z",
                null
        );
        Call<Void> call = apiService.submitResponse(response);
        Response<Void> result = call.execute();

        assertTrue(result.isSuccessful());

        RecordedRequest request = mockWebServer.takeRequest();
        String requestBody = request.getBody().readUtf8();
        assertTrue(requestBody.contains("This is my written answer"));
    }

    // ========== POST /responses/batch Tests ==========

    @Test
    public void testSubmitBatchResponsesSuccess() throws IOException, InterruptedException {
        mockWebServer.enqueue(new MockResponse().setResponseCode(201));

        ResponseDto response1 = new ResponseDto(
                "resp-1", "q-1", "mat-1", TEST_DEVICE_ID, "A", "2023-10-27T10:00:00Z", true
        );
        ResponseDto response2 = new ResponseDto(
                "resp-2", "q-2", "mat-1", TEST_DEVICE_ID, "C", "2023-10-27T10:01:00Z", false
        );
        BatchResponseDto batch = new BatchResponseDto(Arrays.asList(response1, response2));

        Call<Void> call = apiService.submitBatchResponses(batch);
        Response<Void> result = call.execute();

        assertTrue(result.isSuccessful());
        assertEquals(201, result.code());

        RecordedRequest request = mockWebServer.takeRequest();
        assertEquals("POST", request.getMethod());
        assertEquals("/responses/batch", request.getPath());

        String requestBody = request.getBody().readUtf8();
        assertTrue(requestBody.contains("resp-1"));
        assertTrue(requestBody.contains("resp-2"));
    }

    @Test
    public void testSubmitBatchResponsesEmpty() throws IOException {
        mockWebServer.enqueue(new MockResponse().setResponseCode(201));

        BatchResponseDto batch = new BatchResponseDto(Collections.emptyList());
        Call<Void> call = apiService.submitBatchResponses(batch);
        Response<Void> result = call.execute();

        assertTrue(result.isSuccessful());
    }

    @Test
    public void testSubmitBatchResponsesLarge() throws IOException, InterruptedException {
        mockWebServer.enqueue(new MockResponse().setResponseCode(201));

        BatchResponseDto batch = new BatchResponseDto();
        for (int i = 0; i < 50; i++) {
            batch.addResponse(new ResponseDto(
                    "resp-" + i,
                    "q-" + (i % 10),
                    TEST_MATERIAL_ID,
                    TEST_DEVICE_ID,
                    String.valueOf(i % 4),
                    "2023-10-27T10:" + String.format("%02d", i % 60) + ":00Z",
                    i % 2 == 0
            ));
        }

        Call<Void> call = apiService.submitBatchResponses(batch);
        Response<Void> result = call.execute();

        assertTrue(result.isSuccessful());

        RecordedRequest request = mockWebServer.takeRequest();
        String requestBody = request.getBody().readUtf8();
        assertTrue(requestBody.contains("resp-0"));
        assertTrue(requestBody.contains("resp-49"));
    }

    @Test
    public void testSubmitBatchResponsesBadRequest() throws IOException {
        mockWebServer.enqueue(new MockResponse().setResponseCode(400));

        BatchResponseDto batch = new BatchResponseDto(null);
        Call<Void> call = apiService.submitBatchResponses(batch);
        Response<Void> result = call.execute();

        assertEquals(400, result.code());
    }

    // ========== Error Handling Tests ==========

    @Test
    public void testServerError() throws IOException {
        mockWebServer.enqueue(new MockResponse().setResponseCode(500));

        Call<ConfigResponseDto> call = apiService.getConfig(TEST_DEVICE_ID);
        Response<ConfigResponseDto> response = call.execute();

        assertEquals(500, response.code());
    }

    @Test
    public void testServiceUnavailable() throws IOException {
        mockWebServer.enqueue(new MockResponse().setResponseCode(503));

        Call<DistributionBundleDto> call = apiService.getDistribution(TEST_DEVICE_ID);
        Response<DistributionBundleDto> response = call.execute();

        assertEquals(503, response.code());
    }

    // ========== Helper Methods ==========

    private DistributionBundleDto createTestDistributionBundle() {
        MaterialDto material = new MaterialDto(
                TEST_MATERIAL_ID,
                "WORKSHEET",
                "Math Worksheet",
                "Solve these problems",
                null, null,
                System.currentTimeMillis()
        );

        QuestionDto question = new QuestionDto(
                TEST_QUESTION_ID,
                TEST_MATERIAL_ID,
                "MULTIPLE_CHOICE",
                "What is 2+2?",
                Arrays.asList("3", "4", "5"),
                "4",
                10
        );

        return new DistributionBundleDto(
                Collections.singletonList(material),
                Collections.singletonList(question)
        );
    }
}
