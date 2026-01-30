package com.manuscripta.student.network.dto;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Data Transfer Object for distribution bundle response from the API.
 * Per API Contract §2.5, contains materials and questions assigned to a specific device.
 *
 * <p>The distribution bundle is retrieved via {@code GET /distribution/{deviceId}} and is
 * triggered by a TCP DISTRIBUTE_MATERIAL signal from the teacher server.</p>
 *
 * <p>Per Validation Rules §1(6), field names use PascalCase in JSON.</p>
 */
public class DistributionBundleDto {

    /**
     * Array of MaterialDto objects assigned to the device.
     * May be null if no materials are available.
     */
    @SerializedName("materials")
    @Nullable
    private List<MaterialDto> materials;

    /**
     * Array of QuestionDto objects associated with the materials.
     * May be null if no questions are available.
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
     * @param materials List of material DTOs
     * @param questions List of question DTOs
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
                + "materials=" + (materials != null ? materials.size() + " items" : "null")
                + ", questions=" + (questions != null ? questions.size() + " items" : "null")
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
