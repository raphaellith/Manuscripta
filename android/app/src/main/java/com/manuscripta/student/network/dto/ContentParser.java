package com.manuscripta.student.network.dto;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class for parsing content strings to extract attachment references.
 * Materials may reference attachment files (PDFs, images) via URLs in the content field.
 *
 * <p>Attachment references follow the format "/attachments/{id}" where {id} is
 * a UUID or alphanumeric identifier.</p>
 */
public final class ContentParser {

    /**
     * Regex pattern to match attachment references in content.
     * Matches URLs like "/attachments/abc-123" or "/attachments/550e8400-e29b-41d4-a716-446655440000"
     */
    private static final Pattern ATTACHMENT_PATTERN =
            Pattern.compile("/attachments/([a-zA-Z0-9\\-]+)");

    /**
     * Private constructor to prevent instantiation of utility class.
     */
    private ContentParser() {
        throw new AssertionError("Utility class should not be instantiated");
    }

    /**
     * Extracts all attachment references from the given content string.
     *
     * @param content The content string to parse (may contain attachment references)
     * @return A list of attachment IDs found in the content (never null, may be empty)
     */
    @NonNull
    public static List<String> extractAttachmentReferences(@NonNull String content) {
        List<String> attachmentIds = new ArrayList<>();

        if (content == null || content.isEmpty()) {
            return attachmentIds;
        }

        Matcher matcher = ATTACHMENT_PATTERN.matcher(content);
        while (matcher.find()) {
            String attachmentId = matcher.group(1);
            if (attachmentId != null && !attachmentId.isEmpty()) {
                attachmentIds.add(attachmentId);
            }
        }

        return attachmentIds;
    }

    /**
     * Checks if the content contains any attachment references.
     *
     * @param content The content string to check
     * @return true if the content contains at least one attachment reference
     */
    public static boolean hasAttachmentReferences(@NonNull String content) {
        if (content == null || content.isEmpty()) {
            return false;
        }
        return ATTACHMENT_PATTERN.matcher(content).find();
    }

    /**
     * Counts the number of attachment references in the content.
     *
     * @param content The content string to parse
     * @return The count of attachment references found
     */
    public static int countAttachmentReferences(@NonNull String content) {
        return extractAttachmentReferences(content).size();
    }

    /**
     * Builds a full attachment URL from an attachment ID and base URL.
     *
     * @param baseUrl      The base URL of the server (e.g., "http://192.168.1.100:8080")
     * @param attachmentId The attachment ID
     * @return The full attachment URL
     */
    @NonNull
    public static String buildAttachmentUrl(@NonNull String baseUrl, @NonNull String attachmentId) {
        if (baseUrl == null || baseUrl.isEmpty()) {
            return "/attachments/" + attachmentId;
        }

        String normalizedBase = baseUrl;
        if (normalizedBase.endsWith("/")) {
            normalizedBase = normalizedBase.substring(0, normalizedBase.length() - 1);
        }

        return normalizedBase + "/attachments/" + attachmentId;
    }

    /**
     * Extracts distinct attachment references from the given content string.
     * Unlike {@link #extractAttachmentReferences(String)}, this method returns unique IDs only.
     *
     * @param content The content string to parse
     * @return A list of unique attachment IDs found in the content
     */
    @NonNull
    public static List<String> extractDistinctAttachmentReferences(@NonNull String content) {
        List<String> allRefs = extractAttachmentReferences(content);
        List<String> distinctRefs = new ArrayList<>();

        for (String ref : allRefs) {
            if (!distinctRefs.contains(ref)) {
                distinctRefs.add(ref);
            }
        }

        return distinctRefs;
    }
}
