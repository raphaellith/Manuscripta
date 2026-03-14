package com.manuscripta.student.integration.http;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

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

import retrofit2.Response;

/**
 * Integration tests for {@code POST /pair} endpoint.
 *
 * <p>Exercises device registration against the live Windows server.
 * Per API Contract §2.4, successful registration returns 201 Created.</p>
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28, manifest = Config.NONE)
@Category(IntegrationTest.class)
public class PairEndpointIntegrationTest {

    private NetworkIntegrationHarness harness;

    /** Instantiates and wires the harness. */
    @Before
    public void setUp() {
        harness = new NetworkIntegrationHarness(
                IntegrationTestConfig.fromEnvironment());
        harness.setUp();
    }

    /** Disconnects TCP and releases resources. */
    @After
    public void tearDown() {
        harness.tearDown();
    }

    /**
     * Verifies that registering a new device returns 201 Created.
     *
     * @throws Exception if the network call fails unexpectedly
     */
    @Test
    public void registerDevice_returns201() throws Exception {
        DeviceInfoDto info = new DeviceInfoDto(
                harness.getConfig().getTestDeviceId(),
                harness.getConfig().getTestDeviceName());

        Response<Void> response = harness.getApiService()
                .registerDevice(info).execute();

        assertEquals(201, response.code());
    }

    /**
     * Verifies that registering the same device twice returns a
     * client-error status (4xx), indicating a duplicate.
     *
     * @throws Exception if the network call fails unexpectedly
     */
    @Test
    public void registerDevice_duplicate_returnsClientError()
            throws Exception {
        DeviceInfoDto info = new DeviceInfoDto(
                harness.getConfig().getTestDeviceId(),
                harness.getConfig().getTestDeviceName());

        // First registration
        harness.getApiService().registerDevice(info).execute();

        // Second registration — expect 4xx
        Response<Void> duplicate = harness.getApiService()
                .registerDevice(info).execute();

        assertNotNull(duplicate);
        int code = duplicate.code();
        assertEquals("Expected 4xx for duplicate registration",
                true, code >= 400 && code < 500);
    }

    /**
     * Verifies that registering with a missing device ID returns a
     * client-error status.
     *
     * @throws Exception if the network call fails unexpectedly
     */
    @Test
    public void registerDevice_invalidPayload_returnsClientError()
            throws Exception {
        DeviceInfoDto info = new DeviceInfoDto(null, null);

        Response<Void> response = harness.getApiService()
                .registerDevice(info).execute();

        assertNotNull(response);
        int code = response.code();
        assertEquals("Expected 4xx for invalid payload",
                true, code >= 400 && code < 500);
    }
}
