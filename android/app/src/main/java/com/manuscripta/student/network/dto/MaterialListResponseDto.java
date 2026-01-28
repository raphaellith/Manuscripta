package com.manuscripta.student.network.dto;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Data Transfer Object for material list responses from the API.
 * Contains an ordered list of material IDs and optional full material objects.
 *
 * <p>The order of IDs in the list represents the intended display order
 * as determined by the teacher application.</p>
 */
public class MaterialListResponseDto {

    /**
     * Ordered list of material IDs.
     * The order represents the intended display sequence.
     */
    @SerializedName("MaterialIds")
    @Nullable
    private List<String> materialIds;

    /**
     * Optional list of full material objects.
     * May be included for convenience to avoid additional API calls.
     */
    @SerializedName("Materials")
    @Nullable
    private List<MaterialDto> materials;

    /**
     * Total count of materials available.
     * May differ from the returned list if pagination is used.
     */
    @SerializedName("TotalCount")
    @Nullable
    private Integer totalCount;

    /**
     * Default constructor for Gson deserialization.
     */
    public MaterialListResponseDto() {
    }

    /**
     * Constructor with all fields.
     *
     * @param materialIds Ordered list of material IDs
     * @param materials   Optional list of full material objects
     * @param totalCount  Total count of materials available
     */
    public MaterialListResponseDto(@Nullable List<String> materialIds,
                                   @Nullable List<MaterialDto> materials,
                                   @Nullable Integer totalCount) {
        this.materialIds = materialIds;
        this.materials = materials;
        this.totalCount = totalCount;
    }

    /**
     * Gets the ordered list of material IDs.
     *
     * @return The list of material IDs
     */
    @Nullable
    public List<String> getMaterialIds() {
        return materialIds;
    }

    /**
     * Sets the ordered list of material IDs.
     *
     * @param materialIds The list of material IDs
     */
    public void setMaterialIds(@Nullable List<String> materialIds) {
        this.materialIds = materialIds;
    }

    /**
     * Gets the list of full material objects.
     *
     * @return The list of materials
     */
    @Nullable
    public List<MaterialDto> getMaterials() {
        return materials;
    }

    /**
     * Sets the list of full material objects.
     *
     * @param materials The list of materials
     */
    public void setMaterials(@Nullable List<MaterialDto> materials) {
        this.materials = materials;
    }

    /**
     * Gets the total count of materials.
     *
     * @return The total count
     */
    @Nullable
    public Integer getTotalCount() {
        return totalCount;
    }

    /**
     * Sets the total count of materials.
     *
     * @param totalCount The total count
     */
    public void setTotalCount(@Nullable Integer totalCount) {
        this.totalCount = totalCount;
    }

    @Override
    @NonNull
    public String toString() {
        return "MaterialListResponseDto{"
                + "materialIds=" + materialIds
                + ", materials=" + materials
                + ", totalCount=" + totalCount
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
        MaterialListResponseDto that = (MaterialListResponseDto) o;
        if (materialIds != null ? !materialIds.equals(that.materialIds)
                : that.materialIds != null) {
            return false;
        }
        if (materials != null ? !materials.equals(that.materials) : that.materials != null) {
            return false;
        }
        return totalCount != null ? totalCount.equals(that.totalCount) : that.totalCount == null;
    }

    @Override
    public int hashCode() {
        int result = materialIds != null ? materialIds.hashCode() : 0;
        result = 31 * result + (materials != null ? materials.hashCode() : 0);
        result = 31 * result + (totalCount != null ? totalCount.hashCode() : 0);
        return result;
    }
}
