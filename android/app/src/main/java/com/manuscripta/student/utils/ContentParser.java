package com.manuscripta.student.utils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
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
     * Accepts null or empty content and returns an empty list in such cases.
     *
     * @param content The content string to parse (may be null or contain attachment references)
     * @return A list of attachment IDs found in the content (never null, may be empty)
     */
    @NonNull
    public static List<String> extractAttachmentReferences(@Nullable String content) {
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
     * Accepts null or empty content and returns false in such cases.
     *
     * @param content The content string to check (may be null)
     * @return true if the content contains at least one attachment reference
     */
    public static boolean hasAttachmentReferences(@Nullable String content) {
        if (content == null || content.isEmpty()) {
            return false;
        }
        return ATTACHMENT_PATTERN.matcher(content).find();
    }

    /**
     * Counts the number of attachment references in the content.
     * Accepts null or empty content and returns 0 in such cases.
     *
     * @param content The content string to parse (may be null)
     * @return The count of attachment references found
     */
    public static int countAttachmentReferences(@Nullable String content) {
        return extractAttachmentReferences(content).size();
    }

    /**
     * Builds a full attachment URL from an attachment ID and base URL.
     * If baseUrl is null or empty, returns a relative path.
     *
     * @param baseUrl      The base URL of the server (e.g., "http://192.168.1.100:8080"), may be null
     * @param attachmentId The attachment ID
     * @return The full attachment URL
     */
    @NonNull
    public static String buildAttachmentUrl(@Nullable String baseUrl, @NonNull String attachmentId) {
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
     * Uses LinkedHashSet for O(1) lookup while preserving insertion order.
     * Accepts null or empty content and returns an empty list in such cases.
     *
     * @param content The content string to parse (may be null)
     * @return A list of unique attachment IDs found in the content
     */
    @NonNull
    public static List<String> extractDistinctAttachmentReferences(@Nullable String content) {
        List<String> allRefs = extractAttachmentReferences(content);
        Set<String> distinctSet = new LinkedHashSet<>(allRefs);
        return new ArrayList<>(distinctSet);
    }
}
