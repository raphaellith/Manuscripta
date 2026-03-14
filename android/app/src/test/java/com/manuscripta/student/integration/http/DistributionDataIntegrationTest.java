package com.manuscripta.student.integration.http;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import com.manuscripta.student.integration.IntegrationTest;
import com.manuscripta.student.integration.config.IntegrationTestConfig;
import com.manuscripta.student.integration.harness.NetworkIntegrationHarness;
import com.manuscripta.student.network.dto.DeviceInfoDto;
import com.manuscripta.student.network.dto.DistributionBundleDto;
import com.manuscripta.student.network.dto.MaterialDto;
import com.manuscripta.student.network.dto.QuestionDto;

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
 * Integration tests verifying that the distribution bundle
 * contains correct, fully-populated data from the server.
 *
 * <p>Per API Contract §2.5, {@code GET /distribution/{deviceId}}
 * returns a bundle of materials and questions assigned to the
 * device. These tests validate field-level correctness of the
 * seeded test data.</p>
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28, manifest = Config.NONE)
@Category(IntegrationTest.class)
public class DistributionDataIntegrationTest {

    /** Well-known test material ID from IntegrationSeedService. */
    private static final String TEST_MATERIAL_ID =
            "00000001-0000-0000-0000-000000000003";

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
     * Verifies that the distribution bundle contains the seeded
     * material with expected field values (ID, type, title).
     *
     * @throws Exception if the network call fails unexpectedly
     */
    @Test
    public void distribution_containsSeededMaterial()
            throws Exception {
        DistributionBundleDto bundle = fetchBundle();

        List<MaterialDto> materials = bundle.getMaterials();
        assertNotNull("Materials list should not be null",
                materials);
        assertFalse("Materials list should not be empty",
                materials.isEmpty());

        MaterialDto material = findMaterialById(
                materials, TEST_MATERIAL_ID);
        assertNotNull("Expected material with ID "
                + TEST_MATERIAL_ID, material);
        assertEquals("READING", material.getType());
        assertNotNull("Title should not be null",
                material.getTitle());
        assertNotNull("Content should not be null",
                material.getContent());
    }

    /**
     * Verifies that questions in the bundle are correctly linked
     * to the seeded material and have expected fields populated.
     *
     * @throws Exception if the network call fails unexpectedly
     */
    @Test
    public void distribution_questionsLinkedToMaterial()
            throws Exception {
        DistributionBundleDto bundle = fetchBundle();

        List<QuestionDto> questions = bundle.getQuestions();
        assertNotNull("Questions list should not be null",
                questions);
        assertFalse("Questions list should not be empty",
                questions.isEmpty());

        for (QuestionDto question : questions) {
            assertNotNull("Question ID should not be null",
                    question.getId());
            assertEquals(
                    "Question materialId should match",
                    TEST_MATERIAL_ID,
                    question.getMaterialId());
            assertNotNull("QuestionType should not be null",
                    question.getQuestionType());
            assertNotNull("QuestionText should not be null",
                    question.getQuestionText());
        }
    }

    /**
     * Verifies that server-assigned material IDs are preserved
     * exactly as seeded, confirming the ID contract from
     * API Contract §4.1.
     *
     * @throws Exception if the network call fails unexpectedly
     */
    @Test
    public void distribution_serverAssignedIdsPreserved()
            throws Exception {
        DistributionBundleDto bundle = fetchBundle();

        List<MaterialDto> materials = bundle.getMaterials();
        assertNotNull(materials);
        assertFalse(materials.isEmpty());

        MaterialDto material = materials.get(0);
        assertNotNull("Material ID should not be null",
                material.getId());
        assertEquals(
                "Material ID must match server-assigned value",
                TEST_MATERIAL_ID, material.getId());
    }

    // ---------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------

    private DistributionBundleDto fetchBundle() throws Exception {
        Response<DistributionBundleDto> response =
                harness.getApiService()
                        .getDistribution(
                                harness.getConfig()
                                        .getTestDeviceId())
                        .execute();

        assertEquals("Expected 200 for seeded distribution",
                200, response.code());
        DistributionBundleDto bundle = response.body();
        assertNotNull("Bundle should not be null", bundle);
        return bundle;
    }

    private static MaterialDto findMaterialById(
            List<MaterialDto> materials, String id) {
        for (MaterialDto mat : materials) {
            if (id.equals(mat.getId())) {
                return mat;
            }
        }
        return null;
    }
}
