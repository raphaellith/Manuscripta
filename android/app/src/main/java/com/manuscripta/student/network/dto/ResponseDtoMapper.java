package com.manuscripta.student.network.dto;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.manuscripta.student.domain.model.Response;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Mapper class to convert between Response domain model and ResponseDto (network layer).
 * Provides mapping for network transmission per API Contract §2.4.
 *
 * <p>Per API Contract §4.4, timestamps are transmitted as ISO 8601 strings but stored
 * internally as Unix epoch milliseconds.</p>
 */
public final class ResponseDtoMapper {

    /** ISO 8601 date format pattern used for timestamp serialisation. */
    private static final String ISO_8601_FORMAT = "yyyy-MM-dd'T'HH:mm:ss'Z'";

    /**
     * Private constructor to prevent instantiation of utility class.
     */
    private ResponseDtoMapper() {
        throw new AssertionError("Utility class should not be instantiated");
    }

    /**
     * Converts a Response domain model to a ResponseDto for network transmission.
     * The materialId parameter is required because the domain model does not store it
     * (it is stored in the Question entity).
     *
     * @param domain     The Response domain model to convert
     * @param materialId The ID of the material containing the question
     * @return ResponseDto ready for network transmission
     */
    @NonNull
    public static ResponseDto toDto(@NonNull Response domain, @Nullable String materialId) {
        return new ResponseDto(
                domain.getId(),
                domain.getQuestionId(),
                materialId,
                domain.getDeviceId(),
                domain.getAnswer(),
                formatTimestamp(domain.getTimestamp()),
                domain.isCorrect() ? Boolean.TRUE : null
        );
    }

    /**
     * Converts a list of Response domain models to ResponseDtos.
     *
     * @param responses  The list of Response domain models
     * @param materialId The ID of the material containing the questions
     * @return List of ResponseDtos ready for network transmission
     */
    @NonNull
    public static List<ResponseDto> toDtoList(@NonNull List<Response> responses,
                                               @Nullable String materialId) {
        List<ResponseDto> dtos = new ArrayList<>();
        for (Response response : responses) {
            dtos.add(toDto(response, materialId));
        }
        return dtos;
    }

    /**
     * Creates a BatchResponseDto from a list of Response domain models.
     *
     * @param responses  The list of Response domain models
     * @param materialId The ID of the material containing the questions
     * @return BatchResponseDto ready for batch network transmission
     */
    @NonNull
    public static BatchResponseDto toBatchDto(@NonNull List<Response> responses,
                                               @Nullable String materialId) {
        return new BatchResponseDto(toDtoList(responses, materialId));
    }

    /**
     * Formats a Unix epoch milliseconds timestamp to ISO 8601 string format.
     * Per API Contract §4.4, timestamps are transmitted as ISO 8601 strings.
     *
     * @param timestamp Unix epoch milliseconds
     * @return ISO 8601 formatted timestamp string (e.g., "2023-10-27T10:05:00Z")
     */
    @NonNull
    public static String formatTimestamp(long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat(ISO_8601_FORMAT, Locale.UK);
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        return sdf.format(new Date(timestamp));
    }

    /**
     * Parses an ISO 8601 string timestamp to Unix epoch milliseconds.
     * Per API Contract §4.4, timestamps must deserialise to Unix longs.
     *
     * @param isoTimestamp ISO 8601 formatted timestamp string
     * @return Unix epoch milliseconds, or 0 if parsing fails or input is null
     */
    public static long parseTimestamp(@Nullable String isoTimestamp) {
        if (isoTimestamp == null || isoTimestamp.isEmpty()) {
            return 0L;
        }
        try {
            SimpleDateFormat sdf = new SimpleDateFormat(ISO_8601_FORMAT, Locale.UK);
            sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
            Date date = sdf.parse(isoTimestamp);
            return date != null ? date.getTime() : 0L;
        } catch (java.text.ParseException e) {
            return 0L;
        }
    }
}
