package com.manuscripta.student.integration.http;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import com.manuscripta.student.integration.IntegrationTest;
import com.manuscripta.student.integration.config.IntegrationTestConfig;
import com.manuscripta.student.integration.harness.NetworkIntegrationHarness;
import com.manuscripta.student.network.dto.DeviceInfoDto;
import com.manuscripta.student.network.dto.DistributionBundleDto;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import retrofit2.Response;

/**
 * Integration tests for {@code GET /distribution/{deviceId}} endpoint.
 *
 * <p>Exercises material distribution retrieval against the live Windows
 * server. Per API Contract §2.5, the endpoint returns a
 * {@link DistributionBundleDto} containing materials and questions
 * assigned to the device.</p>
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28, manifest = Config.NONE)
@Category(IntegrationTest.class)
public class DistributionEndpointIntegrationTest {

    private NetworkIntegrationHarness harness;

    /** Wires harness and pairs the test device as a prerequisite. */
    @Before
    public void setUp() throws Exception {
        harness = new NetworkIntegrationHarness(
                IntegrationTestConfig.fromEnvironment());
        harness.setUp();

        // Pair device so distribution endpoint accepts requests
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
     * Verifies that fetching the distribution bundle returns 200 and
     * a non-null body.
     *
     * @throws Exception if the network call fails unexpectedly
     */
    @Test
    public void getDistribution_returns200WithBody() throws Exception {
        Response<DistributionBundleDto> response = harness.getApiService()
                .getDistribution(
                        harness.getConfig().getTestDeviceId())
                .execute();

        assertEquals(200, response.code());
        assertNotNull("Response body should not be null",
                response.body());
    }

    /**
     * Verifies that the distribution bundle contains materials and
     * questions lists (may be empty if nothing is staged).
     *
     * @throws Exception if the network call fails unexpectedly
     */
    @Test
    public void getDistribution_bundleContainsLists() throws Exception {
        Response<DistributionBundleDto> response = harness.getApiService()
                .getDistribution(
                        harness.getConfig().getTestDeviceId())
                .execute();

        assertEquals(200, response.code());
        DistributionBundleDto bundle = response.body();
        assertNotNull(bundle);
        // Lists should be present (possibly empty)
        assertNotNull("Materials list should not be null",
                bundle.getMaterials());
        assertNotNull("Questions list should not be null",
                bundle.getQuestions());
    }

    /**
     * Verifies that requesting distribution for an unknown device
     * returns 404.
     *
     * @throws Exception if the network call fails unexpectedly
     */
    @Test
    public void getDistribution_unknownDevice_returns404()
            throws Exception {
        Response<DistributionBundleDto> response = harness.getApiService()
                .getDistribution("nonexistent-device-id")
                .execute();

        assertEquals(404, response.code());
    }
}
