package com.manuscripta.student.ui.materials;

import androidx.annotation.Nullable;

import com.manuscripta.student.data.model.MaterialType;
import com.manuscripta.student.domain.model.Material;

/**
 * Groups materials by title, allowing switching between Reading/Quiz/Worksheet formats
 * of the same educational content.
 *
 * <p>Example: "Battle of Hastings" material group contains:
 * - Reading format: The passage about Battle of Hastings
 * - Quiz format: Questions about Battle of Hastings
 * - Worksheet format: Fill-in-the-blank about Battle of Hastings</p>
 */
public class MaterialGroup {

    private final String title;
    private final Material readingMaterial;
    private final Material quizMaterial;
    private final Material worksheetMaterial;

    /**
     * Creates a material group.
     *
     * @param title The common title shared by all formats
     * @param readingMaterial The reading format (can be null if not available)
     * @param quizMaterial The quiz format (can be null if not available)
     * @param worksheetMaterial The worksheet format (can be null if not available)
     */
    public MaterialGroup(String title,
                        @Nullable Material readingMaterial,
                        @Nullable Material quizMaterial,
                        @Nullable Material worksheetMaterial) {
        this.title = title;
        this.readingMaterial = readingMaterial;
        this.quizMaterial = quizMaterial;
        this.worksheetMaterial = worksheetMaterial;
    }

    public String getTitle() {
        return title;
    }

    @Nullable
    public Material getReadingMaterial() {
        return readingMaterial;
    }

    @Nullable
    public Material getQuizMaterial() {
        return quizMaterial;
    }

    @Nullable
    public Material getWorksheetMaterial() {
        return worksheetMaterial;
    }

    /**
     * Checks if this group has a specific format.
     *
     * @param type The material type to check
     * @return true if the format is available
     */
    public boolean hasFormat(MaterialType type) {
        switch (type) {
            case READING:
                return readingMaterial != null;
            case QUIZ:
                return quizMaterial != null;
            case WORKSHEET:
                return worksheetMaterial != null;
            default:
                return false;
        }
    }

    /**
     * Gets the material for a specific format.
     *
     * @param type The material type to get
     * @return The material, or null if not available
     */
    @Nullable
    public Material getMaterialForType(MaterialType type) {
        switch (type) {
            case READING:
                return readingMaterial;
            case QUIZ:
                return quizMaterial;
            case WORKSHEET:
                return worksheetMaterial;
            default:
                return null;
        }
    }
}
