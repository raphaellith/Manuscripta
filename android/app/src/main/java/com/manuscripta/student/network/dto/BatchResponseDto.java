package com.manuscripta.student.network.dto;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

/**
 * Data Transfer Object for batch submission of multiple student responses.
 * Per API Contract §2.4 (Batch Submit Responses), this represents the request body
 * for POST /responses/batch.
 *
 * <p>Used when reconnecting after offline mode to submit multiple responses at once.</p>
 *
 * <p>Per Validation Rules §1(6), field names use PascalCase in JSON.</p>
 */
public class BatchResponseDto {

    /**
     * The list of responses to submit in batch.
     */
    @SerializedName("Responses")
    @Nullable
    private List<ResponseDto> responses;

    /**
     * Default constructor for Gson deserialization.
     */
    public BatchResponseDto() {
    }

    /**
     * Constructor with responses list.
     *
     * @param responses The list of responses to submit
     */
    public BatchResponseDto(@Nullable List<ResponseDto> responses) {
        this.responses = responses;
    }

    /**
     * Gets the list of responses.
     *
     * @return The list of responses to submit
     */
    @Nullable
    public List<ResponseDto> getResponses() {
        return responses;
    }

    /**
     * Sets the list of responses.
     *
     * @param responses The list of responses to submit
     */
    public void setResponses(@Nullable List<ResponseDto> responses) {
        this.responses = responses;
    }

    /**
     * Adds a response to the batch.
     * If the responses list is null, it will be initialised first.
     *
     * @param response The response to add
     */
    public void addResponse(@NonNull ResponseDto response) {
        if (responses == null) {
            responses = new ArrayList<>();
        }
        responses.add(response);
    }

    /**
     * Returns the number of responses in the batch.
     *
     * @return The number of responses, or 0 if the list is null
     */
    public int size() {
        return responses != null ? responses.size() : 0;
    }

    /**
     * Checks if the batch is empty.
     *
     * @return True if the batch has no responses
     */
    public boolean isEmpty() {
        return responses == null || responses.isEmpty();
    }

    @Override
    @NonNull
    public String toString() {
        return "BatchResponseDto{"
                + "responses=" + responses
                + '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        BatchResponseDto that = (BatchResponseDto) o;
        return responses != null ? responses.equals(that.responses) : that.responses == null;
    }

    @Override
    public int hashCode() {
        return responses != null ? responses.hashCode() : 0;
    }
}
