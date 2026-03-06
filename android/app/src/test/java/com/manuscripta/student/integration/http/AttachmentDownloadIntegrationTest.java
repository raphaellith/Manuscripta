package com.manuscripta.student.integration.http;

import static org.junit.Assert.assertArrayEquals;
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

import java.util.Arrays;

import okhttp3.ResponseBody;
import retrofit2.Response;

/**
 * Integration tests verifying that attachment binary content
 * downloads correctly from the server.
 *
 * <p>Per API Contract §2.1.3, {@code GET /attachments/{id}} returns
 * raw binary data with the appropriate Content-Type header. These
 * tests verify that the seeded PNG attachment is served with the
 * correct MIME type and valid PNG content.</p>
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28, manifest = Config.NONE)
@Category(IntegrationTest.class)
public class AttachmentDownloadIntegrationTest {

    /** Well-known test attachment ID from IntegrationSeedService. */
    private static final String TEST_ATTACHMENT_ID =
            "00000001-0000-0000-0000-000000000004";

    /** PNG file signature (first 4 bytes of every valid PNG). */
    private static final byte[] PNG_MAGIC_BYTES =
            {(byte) 0x89, 0x50, 0x4E, 0x47};

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
     * Verifies that the seeded attachment is served with an
     * {@code image/png} Content-Type header.
     *
     * @throws Exception if the network call fails unexpectedly
     */
    @Test
    public void download_contentTypeIsImagePng() throws Exception {
        Response<ResponseBody> response = harness.getApiService()
                .getAttachment(TEST_ATTACHMENT_ID).execute();

        assertEquals("Expected 200 for seeded attachment",
                200, response.code());
        String contentType = response.headers()
                .get("Content-Type");
        assertNotNull("Content-Type header should be present",
                contentType);
        assertTrue("Content-Type should be image/png but was "
                        + contentType,
                contentType.contains("image/png"));
    }

    /**
     * Verifies that the downloaded attachment body starts with
     * the standard PNG file signature bytes (0x89504E47) and
     * contains a non-trivial amount of data.
     *
     * @throws Exception if the network call fails unexpectedly
     */
    @Test
    public void download_bodyStartsWithPngSignature()
            throws Exception {
        Response<ResponseBody> response = harness.getApiService()
                .getAttachment(TEST_ATTACHMENT_ID).execute();

        assertEquals(200, response.code());
        ResponseBody body = response.body();
        assertNotNull("Response body should not be null", body);

        byte[] bytes = body.bytes();
        assertTrue("Attachment body should not be empty",
                bytes.length > 0);

        byte[] header = Arrays.copyOf(bytes, 4);
        assertArrayEquals(
                "First 4 bytes should be PNG signature",
                PNG_MAGIC_BYTES, header);
    }
}
