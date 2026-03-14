package com.manuscripta.student.integration.http;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import com.manuscripta.student.integration.IntegrationTest;
import com.manuscripta.student.integration.config.IntegrationTestConfig;
import com.manuscripta.student.integration.harness.NetworkIntegrationHarness;
import com.manuscripta.student.network.FeedbackDto;
import com.manuscripta.student.network.FeedbackResponse;
import com.manuscripta.student.network.dto.DeviceInfoDto;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.List;

import retrofit2.Response;

/**
 * Integration tests verifying that the feedback endpoint returns
 * correctly populated feedback data matching the seed.
 *
 * <p>Per API Contract §2.6, {@code GET /feedback/{deviceId}} returns
 * a wrapper containing an array of feedback items. These tests
 * validate field-level correctness of the seeded feedback.</p>
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28, manifest = Config.NONE)
@Category(IntegrationTest.class)
public class FeedbackDataIntegrationTest {

    /** Well-known response ID from IntegrationSeedService. */
    private static final String TEST_RESPONSE_ID =
            "00000001-0000-0000-0000-000000000006";

    private NetworkIntegrationHarness harness;

    /** Wires harness and registers the test device. */
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
     * Verifies that the feedback endpoint returns a non-empty
     * array of feedback for the test device.
     *
     * @throws Exception if the network call fails unexpectedly
     */
    @Test
    public void feedback_containsSeededItems() throws Exception {
        List<FeedbackDto> feedbackList = fetchFeedbackList();

        assertFalse("Feedback list should not be empty",
                feedbackList.isEmpty());
    }

    /**
     * Verifies that the seeded feedback has the expected field
     * values including text, marks, and response ID.
     *
     * @throws Exception if the network call fails unexpectedly
     */
    @Test
    public void feedback_fieldsMatchSeedData() throws Exception {
        List<FeedbackDto> feedbackList = fetchFeedbackList();

        FeedbackDto feedback = findFeedbackByResponseId(
                feedbackList, TEST_RESPONSE_ID);
        assertNotNull(
                "Expected feedback for response "
                        + TEST_RESPONSE_ID, feedback);
        assertEquals("Good answer!", feedback.getText());
        assertEquals(Integer.valueOf(5), feedback.getMarks());
    }

    // ---------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------

    private List<FeedbackDto> fetchFeedbackList() throws Exception {
        Response<FeedbackResponse> response =
                harness.getApiService().getFeedback(
                                harness.getConfig()
                                        .getTestDeviceId())
                        .execute();

        assertEquals("Expected 200 for seeded feedback",
                200, response.code());
        FeedbackResponse body = response.body();
        assertNotNull("Feedback body should not be null", body);
        List<FeedbackDto> feedbackList = body.getFeedback();
        assertNotNull("Feedback list should not be null",
                feedbackList);
        return feedbackList;
    }

    private static FeedbackDto findFeedbackByResponseId(
            List<FeedbackDto> feedbackList, String responseId) {
        for (FeedbackDto fb : feedbackList) {
            if (responseId.equals(fb.getResponseId())) {
                return fb;
            }
        }
        return null;
    }
}
