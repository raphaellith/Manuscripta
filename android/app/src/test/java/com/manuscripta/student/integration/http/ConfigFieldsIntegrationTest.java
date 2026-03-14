package com.manuscripta.student.integration.http;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

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

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import retrofit2.Response;

/**
 * Integration tests verifying that all configuration fields are
 * present and valid per Validation Rules §2G.
 *
 * <p>Per API Contract §2.2, {@code GET /config/{deviceId}} returns
 * a {@link ConfigResponseDto} containing tablet configuration
 * settings. These tests validate range constraints and enum
 * compliance for each field.</p>
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28, manifest = Config.NONE)
@Category(IntegrationTest.class)
public class ConfigFieldsIntegrationTest {

    /** Valid FeedbackStyle values per Validation Rules §2G(1)(b). */
    private static final Set<String> VALID_FEEDBACK_STYLES =
            new HashSet<>(
                    Arrays.asList("IMMEDIATE", "NEUTRAL"));

    /** Valid MascotSelection values per Validation Rules §2G(1)(f). */
    private static final Set<String> VALID_MASCOT_SELECTIONS =
            new HashSet<>(Arrays.asList(
                    "NONE", "MASCOT1", "MASCOT2",
                    "MASCOT3", "MASCOT4", "MASCOT5"));

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
     * Verifies that all six configuration fields are present
     * and non-null in the response.
     *
     * @throws Exception if the network call fails unexpectedly
     */
    @Test
    public void config_allFieldsPresent() throws Exception {
        ConfigResponseDto dto = fetchConfig();

        assertNotNull("TextSize should not be null",
                dto.getTextSize());
        assertNotNull("FeedbackStyle should not be null",
                dto.getFeedbackStyle());
        assertNotNull("TtsEnabled should not be null",
                dto.getTtsEnabled());
        assertNotNull("AiScaffoldingEnabled should not be null",
                dto.getAiScaffoldingEnabled());
        assertNotNull("SummarisationEnabled should not be null",
                dto.getSummarisationEnabled());
        assertNotNull("MascotSelection should not be null",
                dto.getMascotSelection());
    }

    /**
     * Verifies that TextSize is within the valid range (5-50)
     * per Validation Rules §2G(1)(a).
     *
     * @throws Exception if the network call fails unexpectedly
     */
    @Test
    public void config_textSizeInValidRange() throws Exception {
        ConfigResponseDto dto = fetchConfig();

        int textSize = dto.getTextSize();
        assertTrue(
                "TextSize should be >= 5 but was " + textSize,
                textSize >= 5);
        assertTrue(
                "TextSize should be <= 50 but was " + textSize,
                textSize <= 50);
    }

    /**
     * Verifies that FeedbackStyle and MascotSelection are valid
     * enum values per Validation Rules §2G.
     *
     * @throws Exception if the network call fails unexpectedly
     */
    @Test
    public void config_enumFieldsAreValid() throws Exception {
        ConfigResponseDto dto = fetchConfig();

        assertTrue(
                "FeedbackStyle should be IMMEDIATE or "
                        + "NEUTRAL but was "
                        + dto.getFeedbackStyle(),
                VALID_FEEDBACK_STYLES.contains(
                        dto.getFeedbackStyle()));
        assertTrue(
                "MascotSelection should be a valid value"
                        + " but was "
                        + dto.getMascotSelection(),
                VALID_MASCOT_SELECTIONS.contains(
                        dto.getMascotSelection()));
    }

    // ---------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------

    private ConfigResponseDto fetchConfig() throws Exception {
        Response<ConfigResponseDto> response =
                harness.getApiService().getConfig(
                                harness.getConfig()
                                        .getTestDeviceId())
                        .execute();
        assertEquals(200, response.code());
        ConfigResponseDto dto = response.body();
        assertNotNull("Config body should not be null", dto);
        return dto;
    }
}
