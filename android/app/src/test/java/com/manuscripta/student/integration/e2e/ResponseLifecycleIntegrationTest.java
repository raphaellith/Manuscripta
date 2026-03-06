package com.manuscripta.student.integration.e2e;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import com.manuscripta.student.integration.IntegrationTest;
import com.manuscripta.student.integration.config.IntegrationTestConfig;
import com.manuscripta.student.integration.harness.NetworkIntegrationHarness;
import com.manuscripta.student.network.FeedbackDto;
import com.manuscripta.student.network.FeedbackResponse;
import com.manuscripta.student.network.dto.DeviceInfoDto;
import com.manuscripta.student.network.dto.DistributionBundleDto;
import com.manuscripta.student.network.dto.ResponseDto;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import retrofit2.Response;

/**
 * End-to-end integration test for the response lifecycle.
 *
 * <p>Exercises the full HTTP API surface in sequence: register
 * device, fetch distribution, submit response, and fetch feedback.
 * This verifies that the endpoints work together correctly within
 * a single device session.</p>
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28, manifest = Config.NONE)
@Category(IntegrationTest.class)
public class ResponseLifecycleIntegrationTest {

    /** Well-known question ID from IntegrationSeedService. */
    private static final String TEST_QUESTION_ID =
            "00000001-0000-0000-0000-000000000002";

    /** Well-known material ID from IntegrationSeedService. */
    private static final String TEST_MATERIAL_ID =
            "00000001-0000-0000-0000-000000000003";

    private IntegrationTestConfig config;
    private NetworkIntegrationHarness harness;

    /** Wires harness (does not register the device yet). */
    @Before
    public void setUp() {
        config = IntegrationTestConfig.fromEnvironment();
        harness = new NetworkIntegrationHarness(config);
        harness.setUp();
    }

    /** Disconnects TCP and releases resources. */
    @After
    public void tearDown() {
        harness.tearDown();
    }

    /**
     * Runs the full response lifecycle: register device, fetch
     * materials, submit a response, and retrieve feedback.
     *
     * @throws Exception if any step fails
     */
    @Test
    public void responseLifecycle_registerSubmitFetchFeedback()
            throws Exception {
        // 1. Register device
        DeviceInfoDto info = new DeviceInfoDto(
                config.getTestDeviceId(),
                config.getTestDeviceName());
        Response<Void> pairResponse = harness.getApiService()
                .registerDevice(info).execute();
        assertEquals("Device registration should return 201",
                201, pairResponse.code());

        // 2. Fetch distribution bundle
        Response<DistributionBundleDto> distResponse =
                harness.getApiService()
                        .getDistribution(
                                config.getTestDeviceId())
                        .execute();
        assertEquals("Distribution should return 200",
                200, distResponse.code());
        DistributionBundleDto bundle = distResponse.body();
        assertNotNull(bundle);
        assertNotNull(bundle.getMaterials());
        assertFalse("Should have at least one material",
                bundle.getMaterials().isEmpty());

        // 3. Submit response for seeded question
        ResponseDto responseDto = new ResponseDto(
                UUID.randomUUID().toString(),
                TEST_QUESTION_ID,
                TEST_MATERIAL_ID,
                config.getTestDeviceId(),
                "Integration test answer",
                Instant.now().toString(),
                null);
        Response<Void> submitResponse = harness.getApiService()
                .submitResponse(responseDto).execute();
        assertEquals("Response submission should return 201",
                201, submitResponse.code());

        // 4. Fetch feedback (seeded by IntegrationSeedService)
        Response<FeedbackResponse> fbResponse =
                harness.getApiService()
                        .getFeedback(config.getTestDeviceId())
                        .execute();
        assertEquals("Feedback should return 200",
                200, fbResponse.code());
        FeedbackResponse feedback = fbResponse.body();
        assertNotNull(feedback);
        List<FeedbackDto> feedbackList =
                feedback.getFeedback();
        assertNotNull("Feedback list should not be null",
                feedbackList);
        assertFalse("Feedback list should not be empty",
                feedbackList.isEmpty());
    }
}
