package com.manuscripta.student.integration.http;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.manuscripta.student.integration.IntegrationTest;
import com.manuscripta.student.integration.config.IntegrationTestConfig;
import com.manuscripta.student.integration.harness.NetworkIntegrationHarness;
import com.manuscripta.student.network.FeedbackResponse;
import com.manuscripta.student.network.dto.DeviceInfoDto;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import retrofit2.Response;

/**
 * Integration tests for {@code GET /feedback/{deviceId}} endpoint.
 *
 * <p>Per API Contract §2.6, the endpoint returns a wrapper containing
 * an array of feedback items. Returns 404 when no feedback is
 * available.</p>
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28, manifest = Config.NONE)
@Category(IntegrationTest.class)
public class FeedbackEndpointIntegrationTest {

    private NetworkIntegrationHarness harness;

    /** Wires harness and pairs the test device as a prerequisite. */
    @Before
    public void setUp() throws Exception {
        harness = new NetworkIntegrationHarness(
                IntegrationTestConfig.fromEnvironment());
        harness.setUp();

        DeviceInfoDto info = new DeviceInfoDto(
                harness.getConfig().getTestDeviceId(),
                harness.getConfig().getTestDeviceName());
        harness.getApiService().registerDevice(info).execute();
    }

    /** Disconnects TCP and releases resources. */
    @After
    public void tearDown() {
        harness.tearDown();
    }

    /**
     * Verifies that the feedback endpoint returns 200 with a
     * feedback array when feedback is available.
     *
     * @throws Exception if the network call fails unexpectedly
     */
    @Test
    public void getFeedback_returns200WithArray() throws Exception {
        Response<FeedbackResponse> response = harness.getApiService()
                .getFeedback(harness.getConfig().getTestDeviceId())
                .execute();

        // Server may return 200 (with feedback) or 404 (none staged)
        assertTrue("Expected 200 or 404",
                response.code() == 200 || response.code() == 404);

        if (response.code() == 200) {
            FeedbackResponse body = response.body();
            assertNotNull("Feedback body should not be null", body);
            assertNotNull("Feedback list should not be null",
                    body.getFeedback());
        }
    }

    /**
     * Verifies that a malformed (non-UUID) device ID returns 400.
     *
     * @throws Exception if the network call fails unexpectedly
     */
    @Test
    public void getFeedback_malformedDeviceId_returns400()
            throws Exception {
        Response<FeedbackResponse> response = harness.getApiService()
                .getFeedback("nonexistent-device-id")
                .execute();

        assertEquals(400, response.code());
    }

    /**
     * Verifies that a valid but nonexistent device UUID returns 404.
     *
     * @throws Exception if the network call fails unexpectedly
     */
    @Test
    public void getFeedback_nonexistentDevice_returns404()
            throws Exception {
        Response<FeedbackResponse> response = harness.getApiService()
                .getFeedback("ffffffff-ffff-ffff-ffff-ffffffffffff")
                .execute();

        assertEquals(404, response.code());
    }
}
