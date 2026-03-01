package com.manuscripta.student.network.dto;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Unit tests for {@link DistributionBundleDto}.
 * Tests the DTO for material distribution bundle responses per API Contract §2.5.
 */
public class DistributionBundleDtoTest {

    private static final String TEST_MATERIAL_ID = "mat-11111111-1111-1111-1111-111111111111";
    private static final String TEST_QUESTION_ID = "q-22222222-2222-2222-2222-222222222222";

    @Test
    public void testDefaultConstructor() {
        DistributionBundleDto dto = new DistributionBundleDto();

        assertNull(dto.getMaterials());
        assertNull(dto.getQuestions());
    }

    @Test
    public void testConstructorWithAllFields() {
        List<MaterialDto> materials = createTestMaterialsList();
        List<QuestionDto> questions = createTestQuestionsList();

        DistributionBundleDto dto = new DistributionBundleDto(materials, questions);

        assertNotNull(dto.getMaterials());
        assertNotNull(dto.getQuestions());
        assertEquals(2, dto.getMaterials().size());
        assertEquals(2, dto.getQuestions().size());
    }

    @Test
    public void testConstructorWithNullValues() {
        DistributionBundleDto dto = new DistributionBundleDto(null, null);

        assertNull(dto.getMaterials());
        assertNull(dto.getQuestions());
    }

    @Test
    public void testConstructorWithEmptyLists() {
        DistributionBundleDto dto = new DistributionBundleDto(
                Collections.emptyList(),
                Collections.emptyList()
        );

        assertNotNull(dto.getMaterials());
        assertNotNull(dto.getQuestions());
        assertTrue(dto.getMaterials().isEmpty());
        assertTrue(dto.getQuestions().isEmpty());
    }

    @Test
    public void testSetMaterials() {
        DistributionBundleDto dto = new DistributionBundleDto();
        List<MaterialDto> materials = createTestMaterialsList();

        dto.setMaterials(materials);
        assertEquals(materials, dto.getMaterials());

        dto.setMaterials(null);
        assertNull(dto.getMaterials());
    }

    @Test
    public void testSetQuestions() {
        DistributionBundleDto dto = new DistributionBundleDto();
        List<QuestionDto> questions = createTestQuestionsList();

        dto.setQuestions(questions);
        assertEquals(questions, dto.getQuestions());

        dto.setQuestions(null);
        assertNull(dto.getQuestions());
    }

    @Test
    public void testSetMaterialsToEmptyList() {
        DistributionBundleDto dto = new DistributionBundleDto(
                createTestMaterialsList(),
                createTestQuestionsList()
        );

        dto.setMaterials(new ArrayList<>());

        assertNotNull(dto.getMaterials());
        assertTrue(dto.getMaterials().isEmpty());
    }

    @Test
    public void testSetQuestionsToEmptyList() {
        DistributionBundleDto dto = new DistributionBundleDto(
                createTestMaterialsList(),
                createTestQuestionsList()
        );

        dto.setQuestions(new ArrayList<>());

        assertNotNull(dto.getQuestions());
        assertTrue(dto.getQuestions().isEmpty());
    }

    @Test
    public void testToString() {
        DistributionBundleDto dto = new DistributionBundleDto(
                createTestMaterialsList(),
                createTestQuestionsList()
        );

        String result = dto.toString();

        assertNotNull(result);
        assertTrue(result.contains("DistributionBundleDto"));
        assertTrue(result.contains("materials"));
        assertTrue(result.contains("questions"));
    }

    @Test
    public void testToStringWithNullValues() {
        DistributionBundleDto dto = new DistributionBundleDto();

        String result = dto.toString();

        assertNotNull(result);
        assertTrue(result.contains("DistributionBundleDto"));
        assertTrue(result.contains("null"));
    }

    @Test
    public void testEqualsSameObject() {
        DistributionBundleDto dto = new DistributionBundleDto(
                createTestMaterialsList(),
                createTestQuestionsList()
        );

        assertTrue(dto.equals(dto));
    }

    @Test
    public void testEqualsNull() {
        DistributionBundleDto dto = new DistributionBundleDto(
                createTestMaterialsList(),
                createTestQuestionsList()
        );

        assertFalse(dto.equals(null));
    }

    @Test
    public void testEqualsDifferentClass() {
        DistributionBundleDto dto = new DistributionBundleDto(
                createTestMaterialsList(),
                createTestQuestionsList()
        );

        assertFalse(dto.equals("not a dto"));
    }

    @Test
    public void testEqualsSameValues() {
        DistributionBundleDto dto1 = new DistributionBundleDto(
                createTestMaterialsList(),
                createTestQuestionsList()
        );
        DistributionBundleDto dto2 = new DistributionBundleDto(
                createTestMaterialsList(),
                createTestQuestionsList()
        );

        assertTrue(dto1.equals(dto2));
        assertTrue(dto2.equals(dto1));
    }

    @Test
    public void testEqualsDifferentMaterials() {
        DistributionBundleDto dto1 = new DistributionBundleDto(
                createTestMaterialsList(),
                createTestQuestionsList()
        );
        DistributionBundleDto dto2 = new DistributionBundleDto(
                Collections.singletonList(new MaterialDto("different-id", "READING",
                        "Title", "Content", null, null, 123L)),
                createTestQuestionsList()
        );

        assertFalse(dto1.equals(dto2));
    }

    @Test
    public void testEqualsDifferentQuestions() {
        DistributionBundleDto dto1 = new DistributionBundleDto(
                createTestMaterialsList(),
                createTestQuestionsList()
        );
        DistributionBundleDto dto2 = new DistributionBundleDto(
                createTestMaterialsList(),
                Collections.singletonList(new QuestionDto("different-id", TEST_MATERIAL_ID,
                        "MULTIPLE_CHOICE", "Different?", null, null, null))
        );

        assertFalse(dto1.equals(dto2));
    }

    @Test
    public void testEqualsNullMaterials() {
        DistributionBundleDto dto1 = new DistributionBundleDto(null, createTestQuestionsList());
        DistributionBundleDto dto2 = new DistributionBundleDto(null, createTestQuestionsList());

        assertTrue(dto1.equals(dto2));
    }

    @Test
    public void testEqualsNullVsNonNullMaterials() {
        DistributionBundleDto dto1 = new DistributionBundleDto(null, createTestQuestionsList());
        DistributionBundleDto dto2 = new DistributionBundleDto(
                createTestMaterialsList(),
                createTestQuestionsList()
        );

        assertFalse(dto1.equals(dto2));
    }

    @Test
    public void testEqualsNullQuestions() {
        DistributionBundleDto dto1 = new DistributionBundleDto(createTestMaterialsList(), null);
        DistributionBundleDto dto2 = new DistributionBundleDto(createTestMaterialsList(), null);

        assertTrue(dto1.equals(dto2));
    }

    @Test
    public void testEqualsNullVsNonNullQuestions() {
        DistributionBundleDto dto1 = new DistributionBundleDto(createTestMaterialsList(), null);
        DistributionBundleDto dto2 = new DistributionBundleDto(
                createTestMaterialsList(),
                createTestQuestionsList()
        );

        assertFalse(dto1.equals(dto2));
    }

    @Test
    public void testEqualsAllNullValues() {
        DistributionBundleDto dto1 = new DistributionBundleDto();
        DistributionBundleDto dto2 = new DistributionBundleDto();

        assertTrue(dto1.equals(dto2));
    }

    @Test
    public void testHashCodeConsistency() {
        DistributionBundleDto dto = new DistributionBundleDto(
                createTestMaterialsList(),
                createTestQuestionsList()
        );

        int hashCode1 = dto.hashCode();
        int hashCode2 = dto.hashCode();

        assertEquals(hashCode1, hashCode2);
    }

    @Test
    public void testHashCodeEquality() {
        DistributionBundleDto dto1 = new DistributionBundleDto(
                createTestMaterialsList(),
                createTestQuestionsList()
        );
        DistributionBundleDto dto2 = new DistributionBundleDto(
                createTestMaterialsList(),
                createTestQuestionsList()
        );

        assertEquals(dto1.hashCode(), dto2.hashCode());
    }

    @Test
    public void testHashCodeWithNullValues() {
        DistributionBundleDto dto1 = new DistributionBundleDto();
        DistributionBundleDto dto2 = new DistributionBundleDto();

        assertEquals(dto1.hashCode(), dto2.hashCode());
    }

    @Test
    public void testMaterialsPreservesOrder() {
        MaterialDto mat1 = new MaterialDto("id-1", "READING", "First", "C1", null, null, 1L);
        MaterialDto mat2 = new MaterialDto("id-2", "WORKSHEET", "Second", "C2", null, null, 2L);
        MaterialDto mat3 = new MaterialDto("id-3", "WORKSHEET", "Third", "C3", null, null, 3L);

        List<MaterialDto> orderedList = Arrays.asList(mat1, mat2, mat3);
        DistributionBundleDto dto = new DistributionBundleDto(orderedList, null);

        List<MaterialDto> result = dto.getMaterials();
        assertEquals("id-1", result.get(0).getId());
        assertEquals("id-2", result.get(1).getId());
        assertEquals("id-3", result.get(2).getId());
    }

    @Test
    public void testQuestionsPreservesOrder() {
        QuestionDto q1 = new QuestionDto("q-1", "mat-1", "MULTIPLE_CHOICE", "Q1?",
                Arrays.asList("A", "B"), "A", 10);
        QuestionDto q2 = new QuestionDto("q-2", "mat-1", "WRITTEN_ANSWER", "Q2?",
                null, null, 20);
        QuestionDto q3 = new QuestionDto("q-3", "mat-2", "MULTIPLE_CHOICE", "Q3?",
                Arrays.asList("X", "Y", "Z"), "Z", 15);

        List<QuestionDto> orderedList = Arrays.asList(q1, q2, q3);
        DistributionBundleDto dto = new DistributionBundleDto(null, orderedList);

        List<QuestionDto> result = dto.getQuestions();
        assertEquals("q-1", result.get(0).getId());
        assertEquals("q-2", result.get(1).getId());
        assertEquals("q-3", result.get(2).getId());
    }

    @Test
    public void testMaterialWithMultipleQuestions() {
        MaterialDto material = new MaterialDto(TEST_MATERIAL_ID, "WORKSHEET",
                "Worksheet", "Content", null, null, System.currentTimeMillis());

        QuestionDto q1 = new QuestionDto("q-1", TEST_MATERIAL_ID, "MULTIPLE_CHOICE",
                "Question 1?", Arrays.asList("A", "B", "C"), "B", 5);
        QuestionDto q2 = new QuestionDto("q-2", TEST_MATERIAL_ID, "WRITTEN_ANSWER",
                "Question 2?", null, "Expected answer", 10);

        DistributionBundleDto dto = new DistributionBundleDto(
                Collections.singletonList(material),
                Arrays.asList(q1, q2)
        );

        assertEquals(1, dto.getMaterials().size());
        assertEquals(2, dto.getQuestions().size());
        assertEquals(TEST_MATERIAL_ID, dto.getQuestions().get(0).getMaterialId());
        assertEquals(TEST_MATERIAL_ID, dto.getQuestions().get(1).getMaterialId());
    }

    @Test
    public void testLargeDistributionBundle() {
        List<MaterialDto> largeMaterialsList = new ArrayList<>();
        List<QuestionDto> largeQuestionsList = new ArrayList<>();

        for (int i = 0; i < 50; i++) {
            largeMaterialsList.add(new MaterialDto(
                    "mat-" + i,
                    "WORKSHEET",
                    "Material " + i,
                    "Content " + i,
                    null, null,
                    (long) i
            ));
        }

        for (int i = 0; i < 100; i++) {
            largeQuestionsList.add(new QuestionDto(
                    "q-" + i,
                    "mat-" + (i % 50),
                    "MULTIPLE_CHOICE",
                    "Question " + i + "?",
                    Arrays.asList("A", "B"),
                    "A",
                    i
            ));
        }

        DistributionBundleDto dto = new DistributionBundleDto(largeMaterialsList, largeQuestionsList);

        assertEquals(50, dto.getMaterials().size());
        assertEquals(100, dto.getQuestions().size());
        assertEquals("mat-0", dto.getMaterials().get(0).getId());
        assertEquals("mat-49", dto.getMaterials().get(49).getId());
    }

    private List<MaterialDto> createTestMaterialsList() {
        MaterialDto material1 = new MaterialDto(
                TEST_MATERIAL_ID,
                "WORKSHEET",
                "Math Worksheet",
                "Solve these problems",
                null, null,
                System.currentTimeMillis()
        );
        MaterialDto material2 = new MaterialDto(
                "mat-33333333-3333-3333-3333-333333333333",
                "READING",
                "Reading Material",
                "Read this passage",
                null, null,
                System.currentTimeMillis()
        );
        return Arrays.asList(material1, material2);
    }

    private List<QuestionDto> createTestQuestionsList() {
        QuestionDto question1 = new QuestionDto(
                TEST_QUESTION_ID,
                TEST_MATERIAL_ID,
                "MULTIPLE_CHOICE",
                "What is 2+2?",
                Arrays.asList("3", "4", "5"),
                "4",
                10
        );
        QuestionDto question2 = new QuestionDto(
                "q-44444444-4444-4444-4444-444444444444",
                TEST_MATERIAL_ID,
                "WRITTEN_ANSWER",
                "Explain your reasoning",
                null, null,
                20
        );
        return Arrays.asList(question1, question2);
    }
}
