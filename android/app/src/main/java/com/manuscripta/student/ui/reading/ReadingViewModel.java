package com.manuscripta.student.ui.reading;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.manuscripta.student.data.repository.MaterialRepository;
import com.manuscripta.student.domain.model.Material;
import com.manuscripta.student.utils.UiState;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

/**
 * ViewModel for the ReadingFragment. Manages the loading and display
 * state of reading material content.
 */
@HiltViewModel
public class ReadingViewModel extends ViewModel {

    /** Repository for material access. */
    private final MaterialRepository materialRepository;

    /** The UI state for the current material. */
    private final MutableLiveData<UiState<Material>> materialState =
            new MutableLiveData<>(UiState.loading());

    /**
     * Constructor for ReadingViewModel with Hilt injection.
     *
     * @param materialRepository The material repository
     */
    @Inject
    public ReadingViewModel(@NonNull MaterialRepository materialRepository) {
        this.materialRepository = materialRepository;
    }

    /**
     * Gets the observable material UI state.
     *
     * @return LiveData containing the UI state of the current material
     */
    @NonNull
    public LiveData<UiState<Material>> getMaterialState() {
        return materialState;
    }

    /**
     * Sets the material directly, updating the state to success.
     *
     * @param material The material to display
     */
    public void setMaterial(@NonNull Material material) {
        materialState.setValue(UiState.success(material));
    }

    /**
     * Loads a material by its ID from the repository.
     * Updates the state to loading, then success or error.
     *
     * @param materialId The ID of the material to load
     */
    public void loadMaterial(@NonNull String materialId) {
        materialState.setValue(UiState.loading());
        Material material = materialRepository.getMaterialById(materialId);
        if (material != null) {
            materialState.setValue(UiState.success(material));
        } else {
            materialState.setValue(UiState.error("Material not found"));
        }
    }

    /**
     * Sets the state to loading.
     */
    public void setLoading() {
        materialState.setValue(UiState.loading());
    }
}
