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
 */
public class ContentParserTest {

    @Test
    public void testExtractSingleAttachmentReference() {
        String content = "Please see the image at /attachments/img-12345";

        List<String> attachmentIds = ContentParser.extractAttachmentReferences(content);

        assertNotNull(attachmentIds);
        assertEquals(1, attachmentIds.size());
        assertEquals("img-12345", attachmentIds.get(0));
    }

    @Test
    public void testExtractMultipleAttachmentReferences() {
        String content = "See /attachments/doc-1 and also /attachments/doc-2 and /attachments/doc-3";

        List<String> attachmentIds = ContentParser.extractAttachmentReferences(content);

        assertNotNull(attachmentIds);
        assertEquals(3, attachmentIds.size());
        assertEquals("doc-1", attachmentIds.get(0));
        assertEquals("doc-2", attachmentIds.get(1));
        assertEquals("doc-3", attachmentIds.get(2));
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
    public void testExtractAlphanumericIds() {
        String content = "/attachments/abc123 and /attachments/DEF456";

        List<String> attachmentIds = ContentParser.extractAttachmentReferences(content);

        assertEquals(2, attachmentIds.size());
        assertEquals("abc123", attachmentIds.get(0));
        assertEquals("DEF456", attachmentIds.get(1));
    }

    @Test
    public void testExtractMixedContent() {
        String content = "<p>Read the PDF: <a href='/attachments/pdf-001'>Download</a></p>"
                + "<img src='/attachments/img-002' />";

        List<String> attachmentIds = ContentParser.extractAttachmentReferences(content);

        assertEquals(2, attachmentIds.size());
        assertEquals("pdf-001", attachmentIds.get(0));
        assertEquals("img-002", attachmentIds.get(1));
    }

    @Test
    public void testHasAttachmentReferencesTrue() {
        String content = "Content with /attachments/some-id";

        assertTrue(ContentParser.hasAttachmentReferences(content));
    }

    @Test
    public void testHasAttachmentReferencesFalse() {
        String content = "Content without any attachment references";

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
        String content = "/attachments/a /attachments/b /attachments/c";

        assertEquals(3, ContentParser.countAttachmentReferences(content));
    }

    @Test
    public void testBuildAttachmentUrl() {
        String baseUrl = "http://192.168.1.100:8080";
        String attachmentId = "doc-12345";

        String result = ContentParser.buildAttachmentUrl(baseUrl, attachmentId);

        assertEquals("http://192.168.1.100:8080/attachments/doc-12345", result);
    }

    @Test
    public void testBuildAttachmentUrlWithTrailingSlash() {
        String baseUrl = "http://192.168.1.100:8080/";
        String attachmentId = "doc-12345";

        String result = ContentParser.buildAttachmentUrl(baseUrl, attachmentId);

        assertEquals("http://192.168.1.100:8080/attachments/doc-12345", result);
    }

    @Test
    public void testBuildAttachmentUrlWithEmptyBase() {
        String result = ContentParser.buildAttachmentUrl("", "doc-12345");

        assertEquals("/attachments/doc-12345", result);
    }

    @Test
    public void testBuildAttachmentUrlWithNullBase() {
        String result = ContentParser.buildAttachmentUrl(null, "doc-12345");

        assertEquals("/attachments/doc-12345", result);
    }

    @Test
    public void testExtractDistinctAttachmentReferencesWithDuplicates() {
        String content = "/attachments/doc-1 /attachments/doc-2 /attachments/doc-1 /attachments/doc-2";

        List<String> distinctIds = ContentParser.extractDistinctAttachmentReferences(content);

        assertEquals(2, distinctIds.size());
        assertTrue(distinctIds.contains("doc-1"));
        assertTrue(distinctIds.contains("doc-2"));
    }

    @Test
    public void testExtractDistinctAttachmentReferencesNoDuplicates() {
        String content = "/attachments/a /attachments/b /attachments/c";

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
        String content = "/attachments/third /attachments/first /attachments/second";

        List<String> attachmentIds = ContentParser.extractAttachmentReferences(content);

        assertEquals("third", attachmentIds.get(0));
        assertEquals("first", attachmentIds.get(1));
        assertEquals("second", attachmentIds.get(2));
    }

    @Test
    public void testExtractAttachmentReferenceWithSpecialCharactersExcluded() {
        // Underscores should not be matched
        String content = "/attachments/valid-id followed by /attachments/also_valid should not match underscore";

        List<String> attachmentIds = ContentParser.extractAttachmentReferences(content);

        // The regex allows alphanumeric and hyphens
        assertEquals(2, attachmentIds.size());
        assertEquals("valid-id", attachmentIds.get(0));
        assertEquals("also", attachmentIds.get(1)); // stops at underscore
    }

    @Test
    public void testRealWorldContentExample() {
        String content = "<h1>Chapter 5: Cell Biology</h1>\n"
                + "<p>Please review the following diagram:</p>\n"
                + "<img src=\"/attachments/cell-diagram-001\" alt=\"Cell diagram\" />\n"
                + "<p>For additional reading, download the PDF:</p>\n"
                + "<a href=\"/attachments/cell-reading-pdf-002\">Download PDF</a>\n"
                + "<p>Watch the video explanation:</p>\n"
                + "<video src=\"/attachments/cell-video-003\"></video>";

        List<String> attachmentIds = ContentParser.extractAttachmentReferences(content);

        assertEquals(3, attachmentIds.size());
        assertEquals("cell-diagram-001", attachmentIds.get(0));
        assertEquals("cell-reading-pdf-002", attachmentIds.get(1));
        assertEquals("cell-video-003", attachmentIds.get(2));
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
        String content = "See the image at /attachments/end-of-line";

        List<String> attachmentIds = ContentParser.extractAttachmentReferences(content);

        assertEquals(1, attachmentIds.size());
        assertEquals("end-of-line", attachmentIds.get(0));
    }

    @Test
    public void testAttachmentAtStartOfContent() {
        String content = "/attachments/start-of-line is the first image";

        List<String> attachmentIds = ContentParser.extractAttachmentReferences(content);

        assertEquals(1, attachmentIds.size());
        assertEquals("start-of-line", attachmentIds.get(0));
    }
}
