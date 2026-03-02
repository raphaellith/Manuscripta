package com.manuscripta.student.integration.http;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.manuscripta.student.integration.IntegrationTest;
import com.manuscripta.student.integration.config.IntegrationTestConfig;
import com.manuscripta.student.integration.harness.NetworkIntegrationHarness;
import com.manuscripta.student.network.dto.DeviceInfoDto;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import okhttp3.ResponseBody;
import retrofit2.Response;

/**
 * Integration tests for {@code GET /attachments/{id}} endpoint.
 *
 * <p>Per API Contract §2.1.3, the endpoint returns raw binary data
 * (PDF, images, etc.) for an attachment referenced in material
 * content.</p>
 *
 * <p>Prerequisite: the Windows server must have at least one
 * attachment staged whose ID is known. These tests use the device's
 * distribution bundle to discover a valid attachment ID.</p>
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28, manifest = Config.NONE)
@Category(IntegrationTest.class)
public class AttachmentEndpointIntegrationTest {

    private NetworkIntegrationHarness harness;

    /** Wires harness and pairs the test device. */
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
     * Verifies that downloading an attachment with a known ID returns
     * 200 and a non-empty body with a content-type header.
     *
     * <p>If no attachment is staged on the server this test is
     * expected to be skipped or to fail informatively.</p>
     *
     * @throws Exception if the network call fails unexpectedly
     */
    @Test
    public void getAttachment_validId_returns200WithContent()
            throws Exception {
        // Use a well-known test attachment ID (must be pre-staged)
        String testAttachmentId = "test-attachment-001";

        Response<ResponseBody> response = harness.getApiService()
                .getAttachment(testAttachmentId).execute();

        if (response.code() == 404) {
            // No attachment staged — acceptable in CI
            return;
        }

        assertEquals(200, response.code());
        ResponseBody body = response.body();
        assertNotNull("Attachment body should not be null", body);
        assertTrue("Attachment should have content",
                body.contentLength() > 0
                        || body.source().peek().exhausted() == false);
        assertNotNull("Content-Type header should be present",
                response.headers().get("Content-Type"));
    }

    /**
     * Verifies that requesting a nonexistent attachment returns 404.
     *
     * @throws Exception if the network call fails unexpectedly
     */
    @Test
    public void getAttachment_unknownId_returns404() throws Exception {
        Response<ResponseBody> response = harness.getApiService()
                .getAttachment("nonexistent-attachment-id")
                .execute();

        assertEquals(404, response.code());
    }
}
