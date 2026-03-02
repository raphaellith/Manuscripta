package com.manuscripta.student.integration.http;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import com.manuscripta.student.integration.IntegrationTest;
import com.manuscripta.student.integration.config.IntegrationTestConfig;
import com.manuscripta.student.integration.harness.NetworkIntegrationHarness;
import com.manuscripta.student.network.dto.ConfigResponseDto;
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
 * Integration tests for {@code GET /config/{deviceId}} endpoint.
 *
 * <p>Per API Contract §2.2, the endpoint returns a
 * {@link ConfigResponseDto} containing tablet configuration settings
 * such as text size, feedback style, and feature flags.</p>
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28, manifest = Config.NONE)
@Category(IntegrationTest.class)
public class ConfigEndpointIntegrationTest {

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
     * Verifies that the config endpoint returns 200 with a non-null
     * body containing expected configuration fields.
     *
     * @throws Exception if the network call fails unexpectedly
     */
    @Test
    public void getConfig_returns200WithBody() throws Exception {
        Response<ConfigResponseDto> response = harness.getApiService()
                .getConfig(harness.getConfig().getTestDeviceId())
                .execute();

        assertEquals(200, response.code());
        ConfigResponseDto dto = response.body();
        assertNotNull("Config body should not be null", dto);
    }

    /**
     * Verifies that the config response contains TextSize and
     * FeedbackStyle fields.
     *
     * @throws Exception if the network call fails unexpectedly
     */
    @Test
    public void getConfig_containsExpectedFields() throws Exception {
        Response<ConfigResponseDto> response = harness.getApiService()
                .getConfig(harness.getConfig().getTestDeviceId())
                .execute();

        assertEquals(200, response.code());
        ConfigResponseDto dto = response.body();
        assertNotNull(dto);
        assertNotNull("TextSize should not be null",
                dto.getTextSize());
        assertNotNull("FeedbackStyle should not be null",
                dto.getFeedbackStyle());
    }

    /**
     * Verifies that requesting config for an unknown device returns
     * 404.
     *
     * @throws Exception if the network call fails unexpectedly
     */
    @Test
    public void getConfig_unknownDevice_returns404() throws Exception {
        Response<ConfigResponseDto> response = harness.getApiService()
                .getConfig("nonexistent-device-id")
                .execute();

        assertEquals(404, response.code());
    }
}
