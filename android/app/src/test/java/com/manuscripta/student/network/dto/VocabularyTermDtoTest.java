package com.manuscripta.student.network.dto;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Unit tests for {@link VocabularyTermDto}.
 * Tests construction, getters, setters, equals, hashCode, and toString methods.
 */
public class VocabularyTermDtoTest {

    @Test
    public void testDefaultConstructor() {
        VocabularyTermDto dto = new VocabularyTermDto();

        assertNull(dto.getTerm());
        assertNull(dto.getDefinition());
    }

    @Test
    public void testConstructorWithAllFields() {
        VocabularyTermDto dto = new VocabularyTermDto("photosynthesis",
                "The process by which plants convert sunlight into energy");

        assertEquals("photosynthesis", dto.getTerm());
        assertEquals("The process by which plants convert sunlight into energy",
                dto.getDefinition());
    }

    @Test
    public void testConstructorWithNullValues() {
        VocabularyTermDto dto = new VocabularyTermDto(null, null);

        assertNull(dto.getTerm());
        assertNull(dto.getDefinition());
    }

    @Test
    public void testSetTerm() {
        VocabularyTermDto dto = new VocabularyTermDto();

        dto.setTerm("ecosystem");
        assertEquals("ecosystem", dto.getTerm());

        dto.setTerm(null);
        assertNull(dto.getTerm());
    }

    @Test
    public void testSetDefinition() {
        VocabularyTermDto dto = new VocabularyTermDto();

        dto.setDefinition("A community of living organisms");
        assertEquals("A community of living organisms", dto.getDefinition());

        dto.setDefinition(null);
        assertNull(dto.getDefinition());
    }

    @Test
    public void testToString() {
        VocabularyTermDto dto = new VocabularyTermDto("mitosis", "Cell division process");

        String result = dto.toString();

        assertNotNull(result);
        assertTrue(result.contains("VocabularyTermDto"));
        assertTrue(result.contains("mitosis"));
        assertTrue(result.contains("Cell division process"));
    }

    @Test
    public void testToStringWithNullValues() {
        VocabularyTermDto dto = new VocabularyTermDto();

        String result = dto.toString();

        assertNotNull(result);
        assertTrue(result.contains("VocabularyTermDto"));
        assertTrue(result.contains("null"));
    }

    @Test
    public void testEqualsSameObject() {
        VocabularyTermDto dto = new VocabularyTermDto("term", "definition");

        assertTrue(dto.equals(dto));
    }

    @Test
    public void testEqualsNull() {
        VocabularyTermDto dto = new VocabularyTermDto("term", "definition");

        assertFalse(dto.equals(null));
    }

    @Test
    public void testEqualsDifferentClass() {
        VocabularyTermDto dto = new VocabularyTermDto("term", "definition");

        assertFalse(dto.equals("not a dto"));
    }

    @Test
    public void testEqualsSameValues() {
        VocabularyTermDto dto1 = new VocabularyTermDto("term", "definition");
        VocabularyTermDto dto2 = new VocabularyTermDto("term", "definition");

        assertTrue(dto1.equals(dto2));
        assertTrue(dto2.equals(dto1));
    }

    @Test
    public void testEqualsDifferentTerm() {
        VocabularyTermDto dto1 = new VocabularyTermDto("term1", "definition");
        VocabularyTermDto dto2 = new VocabularyTermDto("term2", "definition");

        assertFalse(dto1.equals(dto2));
    }

    @Test
    public void testEqualsDifferentDefinition() {
        VocabularyTermDto dto1 = new VocabularyTermDto("term", "definition1");
        VocabularyTermDto dto2 = new VocabularyTermDto("term", "definition2");

        assertFalse(dto1.equals(dto2));
    }

    @Test
    public void testEqualsNullTerms() {
        VocabularyTermDto dto1 = new VocabularyTermDto(null, "definition");
        VocabularyTermDto dto2 = new VocabularyTermDto(null, "definition");

        assertTrue(dto1.equals(dto2));
    }

    @Test
    public void testEqualsNullVsNonNullTerm() {
        VocabularyTermDto dto1 = new VocabularyTermDto(null, "definition");
        VocabularyTermDto dto2 = new VocabularyTermDto("term", "definition");

        assertFalse(dto1.equals(dto2));
    }

    @Test
    public void testEqualsNullDefinitions() {
        VocabularyTermDto dto1 = new VocabularyTermDto("term", null);
        VocabularyTermDto dto2 = new VocabularyTermDto("term", null);

        assertTrue(dto1.equals(dto2));
    }

    @Test
    public void testEqualsNullVsNonNullDefinition() {
        VocabularyTermDto dto1 = new VocabularyTermDto("term", null);
        VocabularyTermDto dto2 = new VocabularyTermDto("term", "definition");

        assertFalse(dto1.equals(dto2));
    }

    @Test
    public void testEqualsAllNullValues() {
        VocabularyTermDto dto1 = new VocabularyTermDto(null, null);
        VocabularyTermDto dto2 = new VocabularyTermDto(null, null);

        assertTrue(dto1.equals(dto2));
    }

    @Test
    public void testHashCodeConsistency() {
        VocabularyTermDto dto = new VocabularyTermDto("term", "definition");

        int hashCode1 = dto.hashCode();
        int hashCode2 = dto.hashCode();

        assertEquals(hashCode1, hashCode2);
    }

    @Test
    public void testHashCodeEquality() {
        VocabularyTermDto dto1 = new VocabularyTermDto("term", "definition");
        VocabularyTermDto dto2 = new VocabularyTermDto("term", "definition");

        assertEquals(dto1.hashCode(), dto2.hashCode());
    }

    @Test
    public void testHashCodeDifferentValues() {
        VocabularyTermDto dto1 = new VocabularyTermDto("term1", "definition1");
        VocabularyTermDto dto2 = new VocabularyTermDto("term2", "definition2");

        // Hash codes may be equal by chance, but typically won't be
        // This test just ensures hashCode doesn't throw
        dto1.hashCode();
        dto2.hashCode();
    }

    @Test
    public void testHashCodeWithNullValues() {
        VocabularyTermDto dto1 = new VocabularyTermDto(null, null);
        VocabularyTermDto dto2 = new VocabularyTermDto(null, null);

        assertEquals(dto1.hashCode(), dto2.hashCode());
    }

    @Test
    public void testHashCodeWithPartialNullValues() {
        VocabularyTermDto dto1 = new VocabularyTermDto("term", null);
        VocabularyTermDto dto2 = new VocabularyTermDto(null, "definition");

        // These should have different hash codes (most likely)
        dto1.hashCode();
        dto2.hashCode();
    }

    @Test
    public void testModificationAfterConstruction() {
        VocabularyTermDto dto = new VocabularyTermDto("initial", "initial definition");

        dto.setTerm("modified");
        dto.setDefinition("modified definition");

        assertEquals("modified", dto.getTerm());
        assertEquals("modified definition", dto.getDefinition());
    }
}
