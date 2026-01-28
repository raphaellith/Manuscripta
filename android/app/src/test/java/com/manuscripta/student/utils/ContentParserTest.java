package com.manuscripta.student.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.List;

/**
 * Unit tests for {@link ContentParser}.
 * Tests attachment reference extraction from content strings.
 * Per Validation Rules, attachment IDs must be valid UUIDs (8-4-4-4-12 hex format).
 */
public class ContentParserTest {

    /** Test UUID constants for consistent testing. */
    private static final String UUID_1 = "550e8400-e29b-41d4-a716-446655440000";
    private static final String UUID_2 = "6ba7b810-9dad-11d1-80b4-00c04fd430c8";
    private static final String UUID_3 = "f47ac10b-58cc-4372-a567-0e02b2c3d479";

    @Test
    public void testExtractSingleAttachmentReference() {
        String content = "Please see the image at /attachments/" + UUID_1;

        List<String> attachmentIds = ContentParser.extractAttachmentReferences(content);

        assertNotNull(attachmentIds);
        assertEquals(1, attachmentIds.size());
        assertEquals(UUID_1, attachmentIds.get(0));
    }

    @Test
    public void testExtractMultipleAttachmentReferences() {
        String content = "See /attachments/" + UUID_1 + " and also /attachments/" + UUID_2
                + " and /attachments/" + UUID_3;

        List<String> attachmentIds = ContentParser.extractAttachmentReferences(content);

        assertNotNull(attachmentIds);
        assertEquals(3, attachmentIds.size());
        assertEquals(UUID_1, attachmentIds.get(0));
        assertEquals(UUID_2, attachmentIds.get(1));
        assertEquals(UUID_3, attachmentIds.get(2));
    }

    @Test
    public void testExtractUuidAttachmentReference() {
        String content = "Download: /attachments/550e8400-e29b-41d4-a716-446655440000";

        List<String> attachmentIds = ContentParser.extractAttachmentReferences(content);

        assertNotNull(attachmentIds);
        assertEquals(1, attachmentIds.size());
        assertEquals("550e8400-e29b-41d4-a716-446655440000", attachmentIds.get(0));
    }

    @Test
    public void testExtractNoAttachmentReferences() {
        String content = "This is regular content without any attachments.";

        List<String> attachmentIds = ContentParser.extractAttachmentReferences(content);

        assertNotNull(attachmentIds);
        assertTrue(attachmentIds.isEmpty());
    }

    @Test
    public void testExtractEmptyContent() {
        String content = "";

        List<String> attachmentIds = ContentParser.extractAttachmentReferences(content);

        assertNotNull(attachmentIds);
        assertTrue(attachmentIds.isEmpty());
    }

    @Test
    public void testExtractNullContent() {
        List<String> attachmentIds = ContentParser.extractAttachmentReferences(null);

        assertNotNull(attachmentIds);
        assertTrue(attachmentIds.isEmpty());
    }

    @Test
    public void testNonUuidIdsAreNotMatched() {
        // Short IDs should not match - only valid UUIDs are accepted
        String content = "/attachments/abc123 and /attachments/DEF456";

        List<String> attachmentIds = ContentParser.extractAttachmentReferences(content);

        assertTrue("Non-UUID IDs should not be matched", attachmentIds.isEmpty());
    }

    @Test
    public void testExtractMixedContent() {
        String content = "<p>Read the PDF: <a href='/attachments/" + UUID_1 + "'>Download</a></p>"
                + "<img src='/attachments/" + UUID_2 + "' />";

        List<String> attachmentIds = ContentParser.extractAttachmentReferences(content);

        assertEquals(2, attachmentIds.size());
        assertEquals(UUID_1, attachmentIds.get(0));
        assertEquals(UUID_2, attachmentIds.get(1));
    }

    @Test
    public void testHasAttachmentReferencesTrue() {
        String content = "Content with /attachments/" + UUID_1;

        assertTrue(ContentParser.hasAttachmentReferences(content));
    }

    @Test
    public void testHasAttachmentReferencesFalse() {
        String content = "Content without any attachment references";

        assertFalse(ContentParser.hasAttachmentReferences(content));
    }

    @Test
    public void testHasAttachmentReferencesWithNonUuidReturnsFalse() {
        // Non-UUID IDs should not be detected as attachments
        String content = "Content with /attachments/short-id";

        assertFalse(ContentParser.hasAttachmentReferences(content));
    }

    @Test
    public void testHasAttachmentReferencesEmptyContent() {
        assertFalse(ContentParser.hasAttachmentReferences(""));
    }

    @Test
    public void testHasAttachmentReferencesNullContent() {
        assertFalse(ContentParser.hasAttachmentReferences(null));
    }

    @Test
    public void testCountAttachmentReferencesZero() {
        String content = "No attachments here";

        assertEquals(0, ContentParser.countAttachmentReferences(content));
    }

    @Test
    public void testCountAttachmentReferencesMultiple() {
        String content = "/attachments/" + UUID_1 + " /attachments/" + UUID_2
                + " /attachments/" + UUID_3;

        assertEquals(3, ContentParser.countAttachmentReferences(content));
    }

    @Test
    public void testBuildAttachmentUrl() {
        String baseUrl = "http://192.168.1.100:8080";
        String attachmentId = UUID_1;

        String result = ContentParser.buildAttachmentUrl(baseUrl, attachmentId);

        assertEquals("http://192.168.1.100:8080/attachments/" + UUID_1, result);
    }

    @Test
    public void testBuildAttachmentUrlWithTrailingSlash() {
        String baseUrl = "http://192.168.1.100:8080/";
        String attachmentId = UUID_1;

        String result = ContentParser.buildAttachmentUrl(baseUrl, attachmentId);

        assertEquals("http://192.168.1.100:8080/attachments/" + UUID_1, result);
    }

    @Test
    public void testBuildAttachmentUrlWithEmptyBase() {
        String result = ContentParser.buildAttachmentUrl("", UUID_1);

        assertEquals("/attachments/" + UUID_1, result);
    }

    @Test
    public void testBuildAttachmentUrlWithNullBase() {
        String result = ContentParser.buildAttachmentUrl(null, UUID_1);

        assertEquals("/attachments/" + UUID_1, result);
    }

    @Test
    public void testExtractDistinctAttachmentReferencesWithDuplicates() {
        String content = "/attachments/" + UUID_1 + " /attachments/" + UUID_2
                + " /attachments/" + UUID_1 + " /attachments/" + UUID_2;

        List<String> distinctIds = ContentParser.extractDistinctAttachmentReferences(content);

        assertEquals(2, distinctIds.size());
        assertTrue(distinctIds.contains(UUID_1));
        assertTrue(distinctIds.contains(UUID_2));
    }

    @Test
    public void testExtractDistinctAttachmentReferencesNoDuplicates() {
        String content = "/attachments/" + UUID_1 + " /attachments/" + UUID_2
                + " /attachments/" + UUID_3;

        List<String> distinctIds = ContentParser.extractDistinctAttachmentReferences(content);

        assertEquals(3, distinctIds.size());
    }

    @Test
    public void testExtractDistinctAttachmentReferencesEmpty() {
        List<String> distinctIds = ContentParser.extractDistinctAttachmentReferences("");

        assertNotNull(distinctIds);
        assertTrue(distinctIds.isEmpty());
    }

    @Test
    public void testExtractDistinctAttachmentReferencesNull() {
        List<String> distinctIds = ContentParser.extractDistinctAttachmentReferences(null);

        assertNotNull(distinctIds);
        assertTrue(distinctIds.isEmpty());
    }

    @Test
    public void testExtractAttachmentReferencePreservesOrder() {
        String content = "/attachments/" + UUID_3 + " /attachments/" + UUID_1
                + " /attachments/" + UUID_2;

        List<String> attachmentIds = ContentParser.extractAttachmentReferences(content);

        assertEquals(UUID_3, attachmentIds.get(0));
        assertEquals(UUID_1, attachmentIds.get(1));
        assertEquals(UUID_2, attachmentIds.get(2));
    }

    @Test
    public void testNonUuidWithUnderscoresNotMatched() {
        // IDs with underscores and other invalid formats should not be matched
        String content = "/attachments/valid_id /attachments/also_invalid";

        List<String> attachmentIds = ContentParser.extractAttachmentReferences(content);

        assertTrue("IDs with underscores should not be matched", attachmentIds.isEmpty());
    }

    @Test
    public void testRealWorldContentExample() {
        String content = "<h1>Chapter 5: Cell Biology</h1>\n"
                + "<p>Please review the following diagram:</p>\n"
                + "<img src=\"/attachments/" + UUID_1 + "\" alt=\"Cell diagram\" />\n"
                + "<p>For additional reading, download the PDF:</p>\n"
                + "<a href=\"/attachments/" + UUID_2 + "\">Download PDF</a>\n"
                + "<p>Watch the video explanation:</p>\n"
                + "<video src=\"/attachments/" + UUID_3 + "\"></video>";

        List<String> attachmentIds = ContentParser.extractAttachmentReferences(content);

        assertEquals(3, attachmentIds.size());
        assertEquals(UUID_1, attachmentIds.get(0));
        assertEquals(UUID_2, attachmentIds.get(1));
        assertEquals(UUID_3, attachmentIds.get(2));
    }

    @Test
    public void testIncorrectPathNotMatched() {
        // Test that similar but incorrect paths are not matched
        String content = "/attachment/wrong /attachments wrong /attachmentsid";

        List<String> attachmentIds = ContentParser.extractAttachmentReferences(content);

        assertTrue(attachmentIds.isEmpty());
    }

    @Test
    public void testAttachmentAtEndOfContent() {
        String content = "See the image at /attachments/" + UUID_1;

        List<String> attachmentIds = ContentParser.extractAttachmentReferences(content);

        assertEquals(1, attachmentIds.size());
        assertEquals(UUID_1, attachmentIds.get(0));
    }

    @Test
    public void testAttachmentAtStartOfContent() {
        String content = "/attachments/" + UUID_1 + " is the first image";

        List<String> attachmentIds = ContentParser.extractAttachmentReferences(content);

        assertEquals(1, attachmentIds.size());
        assertEquals(UUID_1, attachmentIds.get(0));
    }

    @Test
    public void testUppercaseUuidIsMatched() {
        // UUIDs can have uppercase hex characters
        String uppercaseUuid = "550E8400-E29B-41D4-A716-446655440000";
        String content = "/attachments/" + uppercaseUuid;

        List<String> attachmentIds = ContentParser.extractAttachmentReferences(content);

        assertEquals(1, attachmentIds.size());
        assertEquals(uppercaseUuid, attachmentIds.get(0));
    }

    @Test
    public void testMixedCaseUuidIsMatched() {
        // Mixed case UUIDs should also be matched
        String mixedCaseUuid = "550e8400-E29B-41d4-A716-446655440000";
        String content = "/attachments/" + mixedCaseUuid;

        List<String> attachmentIds = ContentParser.extractAttachmentReferences(content);

        assertEquals(1, attachmentIds.size());
        assertEquals(mixedCaseUuid, attachmentIds.get(0));
    }

    @Test
    public void testPartialUuidNotMatched() {
        // Incomplete UUIDs should not be matched
        String partialUuid = "550e8400-e29b-41d4";
        String content = "/attachments/" + partialUuid;

        List<String> attachmentIds = ContentParser.extractAttachmentReferences(content);

        assertTrue("Partial UUIDs should not be matched", attachmentIds.isEmpty());
    }
}
