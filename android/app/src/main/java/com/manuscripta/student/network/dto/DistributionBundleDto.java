package com.manuscripta.student.network.dto;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Data Transfer Object for material distribution bundle responses from the API.
 * Per API Contract §2.5, this represents the response body for GET /distribution/{deviceId}.
 *
 * <p>Contains a bundle of materials and questions assigned to a specific device.
 * The Android client creates a separate SessionEntity for each material received.</p>
 *
 * <p>Per Validation Rules §1(6), field names use lowercase in this response
 * as specified in API Contract §2.5.</p>
 */
public class DistributionBundleDto {

    /**
     * The list of materials assigned to the device.
     * Per API Contract §2.5, contains MaterialEntity objects as defined in Validation Rules §2A.
     */
    @SerializedName("materials")
    @Nullable
    private List<MaterialDto> materials;

    /**
     * The list of questions associated with the materials.
     * Per API Contract §2.5, contains QuestionEntity objects as defined in Validation Rules §2B.
     */
    @SerializedName("questions")
    @Nullable
    private List<QuestionDto> questions;

    /**
     * Default constructor for Gson deserialization.
     */
    public DistributionBundleDto() {
    }

    /**
     * Constructor with all fields.
     *
     * @param materials The list of materials
     * @param questions The list of questions
     */
    public DistributionBundleDto(@Nullable List<MaterialDto> materials,
                                  @Nullable List<QuestionDto> questions) {
        this.materials = materials;
        this.questions = questions;
    }

    /**
     * Gets the list of materials.
     *
     * @return The list of materials
     */
    @Nullable
    public List<MaterialDto> getMaterials() {
        return materials;
    }

    /**
     * Sets the list of materials.
     *
     * @param materials The list of materials
     */
    public void setMaterials(@Nullable List<MaterialDto> materials) {
        this.materials = materials;
    }

    /**
     * Gets the list of questions.
     *
     * @return The list of questions
     */
    @Nullable
    public List<QuestionDto> getQuestions() {
        return questions;
    }

    /**
     * Sets the list of questions.
     *
     * @param questions The list of questions
     */
    public void setQuestions(@Nullable List<QuestionDto> questions) {
        this.questions = questions;
    }

    @Override
    @NonNull
    public String toString() {
        return "DistributionBundleDto{"
                + "materials=" + materials
                + ", questions=" + questions
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
        DistributionBundleDto that = (DistributionBundleDto) o;
        if (materials != null ? !materials.equals(that.materials) : that.materials != null) {
            return false;
        }
        return questions != null ? questions.equals(that.questions) : that.questions == null;
    }

    @Override
    public int hashCode() {
        int result = materials != null ? materials.hashCode() : 0;
        result = 31 * result + (questions != null ? questions.hashCode() : 0);
        return result;
    }
}
