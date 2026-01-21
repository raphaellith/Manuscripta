package com.manuscripta.student.ui.materials;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.manuscripta.student.data.model.MaterialType;
import com.manuscripta.student.domain.model.Material;
import com.manuscripta.student.ui.common.UiState;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

/**
 * ViewModel for the Material View screen.
 * Shows ONE material group with tabs to switch between Reading/Quiz/Worksheet formats.
 */
@HiltViewModel
public class MaterialListViewModel extends ViewModel {

    private final MutableLiveData<UiState<MaterialGroup>> materialGroupState;
    private final MutableLiveData<MaterialType> currentFormatState;
    private MaterialGroup currentMaterialGroup;

    /**
     * Creates a new MaterialListViewModel.
     */
    @Inject
    public MaterialListViewModel() {
        this.materialGroupState = new MutableLiveData<>(UiState.loading());
        this.currentFormatState = new MutableLiveData<>(MaterialType.READING);
    }

    /**
     * Returns LiveData for material group state.
     *
     * @return LiveData of material group UI state
     */
    public LiveData<UiState<MaterialGroup>> getMaterialGroupState() {
        return materialGroupState;
    }

    /**
     * Returns LiveData for current format.
     *
     * @return LiveData of current format type
     */
    public LiveData<MaterialType> getCurrentFormatState() {
        return currentFormatState;
    }

    /**
     * Loads materials from the repository and creates material group.
     */
    public void loadMaterials() {
        materialGroupState.setValue(UiState.loading());

        // Hardcoded test material group for Kindle testing
        currentMaterialGroup = createTestMaterialGroup();
        materialGroupState.setValue(UiState.success(currentMaterialGroup));
    }

    /**
     * Switches to a different format (Reading/Quiz/Worksheet).
     *
     * @param type The format to switch to
     */
    public void switchFormat(MaterialType type) {
        if (currentMaterialGroup != null && currentMaterialGroup.hasFormat(type)) {
            currentFormatState.setValue(type);
        }
    }

    /**
     * Gets the current material based on selected format.
     *
     * @return The current material, or null if not available
     */
    public Material getCurrentMaterial() {
        if (currentMaterialGroup == null || currentFormatState.getValue() == null) {
            return null;
        }
        return currentMaterialGroup.getMaterialForType(currentFormatState.getValue());
    }

    /**
     * Creates a hardcoded test material group for testing on Kindle.
     * Creates ONE topic with three different formats: Reading, Quiz, and Worksheet.
     *
     * @return Material group with all three formats
     */
    private MaterialGroup createTestMaterialGroup() {
        String topic = "The Battle of Hastings";

        // READING format - The passage to read
        Material readingMaterial = new Material(
                "mat-hastings-reading",
                MaterialType.READING,
                topic,
                "The Battle of Hastings: A Turning Point in English History\n\n" +
                        "On October 14, 1066, one of the most important battles in English history took place " +
                        "near the town of Hastings in East Sussex.\n\n" +
                        "The Conflict:\n" +
                        "The battle was fought between the Norman-French army of Duke William II of Normandy and " +
                        "the English army under King Harold Godwinson. The conflict arose after the death of " +
                        "King Edward the Confessor in January 1066, which left England without a clear heir.\n\n" +
                        "The Battle:\n" +
                        "Harold's army positioned themselves on Senlac Hill, forming a strong defensive shield wall. " +
                        "William's forces, consisting of infantry, archers, and cavalry, repeatedly attacked the " +
                        "English position throughout the day.\n\n" +
                        "The Outcome:\n" +
                        "After hours of fierce fighting, King Harold was killed - legend says by an arrow to the eye. " +
                        "With their king dead, the English forces broke and fled. William's victory led to the " +
                        "Norman conquest of England, fundamentally changing English culture, language, and governance.\n\n" +
                        "Impact:\n" +
                        "The Norman Conquest brought French language and customs to England, built castles across " +
                        "the land, and established a new ruling class. The effects of 1066 are still visible in " +
                        "England today.",
                "{}",
                "[]",
                System.currentTimeMillis()
        );

        // QUIZ format - Questions about the same content
        Material quizMaterial = new Material(
                "mat-hastings-quiz",
                MaterialType.QUIZ,
                topic,
                "Test your knowledge of the Battle of Hastings:\n\n" +
                        "1. In which year did the Battle of Hastings take place?\n" +
                        "   a) 1056\n" +
                        "   b) 1066\n" +
                        "   c) 1076\n" +
                        "   d) 1086\n\n" +
                        "2. Who were the two main leaders in this battle?\n" +
                        "   a) William of Normandy and Harold Godwinson\n" +
                        "   b) Julius Caesar and King Arthur\n" +
                        "   c) Richard the Lionheart and Saladin\n" +
                        "   d) Henry VIII and Thomas More\n\n" +
                        "3. Where did Harold's army position themselves?\n" +
                        "   a) On flat ground near the sea\n" +
                        "   b) On Senlac Hill\n" +
                        "   c) In Dover Castle\n" +
                        "   d) Behind the River Thames\n\n" +
                        "4. What was the outcome of the battle?\n" +
                        "   a) Harold won and remained king\n" +
                        "   b) Both sides retreated\n" +
                        "   c) William won and became king\n" +
                        "   d) The battle was a draw\n\n" +
                        "5. What major change did the Norman Conquest bring to England?\n" +
                        "   a) Introduction of tea drinking\n" +
                        "   b) French language and customs\n" +
                        "   c) Democracy was established\n" +
                        "   d) Ending of all wars",
                "{}",
                "[]",
                System.currentTimeMillis()
        );

        // WORKSHEET format - Fill-in-the-blank and exercises
        Material worksheetMaterial = new Material(
                "mat-hastings-worksheet",
                MaterialType.WORKSHEET,
                topic,
                "Complete the following exercises about the Battle of Hastings:\n\n" +
                        "PART A: Fill in the blanks\n\n" +
                        "1. The Battle of Hastings took place on __________ 14, __________ (date).\n\n" +
                        "2. The battle was fought between __________ of Normandy and King __________ of England.\n\n" +
                        "3. The English army formed a defensive __________ wall on Senlac Hill.\n\n" +
                        "4. King Harold was reportedly killed by an __________ to the eye.\n\n" +
                        "5. William's victory led to the __________ Conquest of England.\n\n" +
                        "PART B: Short Answer\n\n" +
                        "6. Why was there conflict over who should be king after Edward the Confessor died?\n\n" +
                        "7. Describe the English defensive position at the start of the battle.\n\n" +
                        "8. What were three types of troops in William's army?\n\n" +
                        "9. Name two lasting impacts of the Norman Conquest on England.\n\n" +
                        "PART C: Critical Thinking\n\n" +
                        "10. Why do you think the Battle of Hastings is considered a turning point in English history? " +
                        "Write 2-3 sentences.",
                "{}",
                "[]",
                System.currentTimeMillis()
        );

        return new MaterialGroup(topic, readingMaterial, quizMaterial, worksheetMaterial);
    }

    /**
     * Refreshes the materials list.
     */
    public void refresh() {
        loadMaterials();
    }
}
