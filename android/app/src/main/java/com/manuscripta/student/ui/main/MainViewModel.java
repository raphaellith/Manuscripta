package com.manuscripta.student.ui.main;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.manuscripta.student.data.local.QuestionDao;
import com.manuscripta.student.data.model.QuestionEntity;
import com.manuscripta.student.data.model.QuestionType;
import com.manuscripta.student.data.repository.ConfigRepository;
import com.manuscripta.student.data.repository.MaterialRepository;
import com.manuscripta.student.domain.mapper.QuestionMapper;
import com.manuscripta.student.domain.model.Configuration;
import com.manuscripta.student.domain.model.Material;
import com.manuscripta.student.domain.model.Question;
import com.manuscripta.student.utils.ConnectionManager;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

/**
 * ViewModel for MainActivity. Orchestrates the overall session state
 * including current material, questions, configuration, and tab navigation.
 */
@HiltViewModel
public class MainViewModel extends ViewModel {

    /** Index constant for the Reading tab. */
    public static final int TAB_READING = 0;

    /** Index constant for the Quiz tab. */
    public static final int TAB_QUIZ = 1;

    /** Index constant for the Worksheet tab. */
    public static final int TAB_WORKSHEET = 2;

    /** Repository for materials. */
    private final MaterialRepository materialRepository;

    /** DAO for question access. */
    private final QuestionDao questionDao;

    /** Repository for configuration. */
    private final ConfigRepository configRepository;

    /** Manager for connection state. */
    private final ConnectionManager connectionManager;

    /** The currently distributed material. */
    private final MutableLiveData<Material> currentMaterial = new MutableLiveData<>();

    /** The questions for the current material. */
    private final MutableLiveData<List<Question>> currentQuestions = new MutableLiveData<>();

    /** The tablet configuration. */
    private final MutableLiveData<Configuration> configuration = new MutableLiveData<>();

    /** The current active tab index. */
    private final MutableLiveData<Integer> activeTab = new MutableLiveData<>(TAB_READING);

    /** The current AI task name (null when no task is active). */
    private final MutableLiveData<String> aiTaskName = new MutableLiveData<>();

    /** The AI response content (null when loading or inactive). */
    private final MutableLiveData<String> aiResponse = new MutableLiveData<>();

    /**
     * Constructor for MainViewModel with Hilt injection.
     *
     * @param materialRepository The material repository
     * @param questionDao        The question DAO for accessing questions
     * @param configRepository   The configuration repository
     * @param connectionManager  The connection manager
     */
    @Inject
    public MainViewModel(@NonNull MaterialRepository materialRepository,
                         @NonNull QuestionDao questionDao,
                         @NonNull ConfigRepository configRepository,
                         @NonNull ConnectionManager connectionManager) {
        this.materialRepository = materialRepository;
        this.questionDao = questionDao;
        this.configRepository = configRepository;
        this.connectionManager = connectionManager;

        configuration.setValue(configRepository.getConfig());
    }

    /**
     * Gets the observable current material LiveData.
     *
     * @return LiveData containing the current material
     */
    @NonNull
    public LiveData<Material> getCurrentMaterial() {
        return currentMaterial;
    }

    /**
     * Gets the observable questions LiveData.
     *
     * @return LiveData containing the list of questions for the current material
     */
    @NonNull
    public LiveData<List<Question>> getCurrentQuestions() {
        return currentQuestions;
    }

    /**
     * Gets the observable configuration LiveData.
     *
     * @return LiveData containing the current configuration
     */
    @NonNull
    public LiveData<Configuration> getConfiguration() {
        return configuration;
    }

    /**
     * Gets the observable connection state LiveData.
     *
     * @return LiveData containing the connection state boolean
     */
    @NonNull
    public LiveData<Boolean> getConnectionState() {
        return connectionManager.getConnectionState();
    }

    /**
     * Gets the observable active tab LiveData.
     *
     * @return LiveData containing the active tab index
     */
    @NonNull
    public LiveData<Integer> getActiveTab() {
        return activeTab;
    }

    /**
     * Gets the observable AI task name LiveData.
     *
     * @return LiveData containing the current AI task name, or null if inactive
     */
    @NonNull
    public LiveData<String> getAiTaskName() {
        return aiTaskName;
    }

    /**
     * Gets the observable AI response LiveData.
     *
     * @return LiveData containing the AI response content, or null if loading
     */
    @NonNull
    public LiveData<String> getAiResponse() {
        return aiResponse;
    }

    /**
     * Sets the active tab index.
     *
     * @param tabIndex The tab index to set (TAB_READING, TAB_QUIZ, or TAB_WORKSHEET)
     */
    public void setActiveTab(int tabIndex) {
        activeTab.setValue(tabIndex);
    }

    /**
     * Sets the current material and loads associated questions.
     *
     * @param material The material to set as current
     */
    public void setCurrentMaterial(@NonNull Material material) {
        currentMaterial.setValue(material);
        loadQuestionsForMaterial(material.getId());
    }

    /**
     * Refreshes the configuration from the repository.
     */
    public void refreshConfiguration() {
        configuration.setValue(configRepository.getConfig());
    }

    /**
     * Requests a content transformation task (Simplify or Summarise)
     * for the current material. Sets the task name to trigger loading
     * state in the UI.
     *
     * @param taskName The transformation type (e.g. "Simplify", "Summarise")
     */
    public void requestContentTransformation(@NonNull String taskName) {
        aiTaskName.setValue(taskName);
        aiResponse.setValue(null);
        // TODO: Call content transformation API when backend is available
    }

    /**
     * Clears the current AI task state, hiding the response panel.
     */
    public void clearAiTask() {
        aiTaskName.setValue(null);
        aiResponse.setValue(null);
    }

    /**
     * Loads a material by its ID from the repository and sets it as current.
     *
     * @param materialId The ID of the material to load
     */
    public void loadMaterial(@NonNull String materialId) {
        Material material = materialRepository.getMaterialById(materialId);
        if (material != null) {
            currentMaterial.setValue(material);
            loadQuestionsForMaterial(materialId);
        }
    }

    /**
     * Gets all quiz-type questions from the current question set.
     *
     * @return A list of MULTIPLE_CHOICE questions, or an empty list if none
     */
    @NonNull
    public List<Question> getQuizQuestions() {
        List<Question> all = currentQuestions.getValue();
        if (all == null) {
            return new ArrayList<>();
        }
        List<Question> quizQuestions = new ArrayList<>();
        for (Question q : all) {
            if (q.getQuestionType() == QuestionType.MULTIPLE_CHOICE) {
                quizQuestions.add(q);
            }
        }
        return quizQuestions;
    }

    /**
     * Gets all worksheet-type questions from the current question set.
     *
     * @return A list of WRITTEN_ANSWER questions, or an empty list if none
     */
    @NonNull
    public List<Question> getWorksheetQuestions() {
        List<Question> all = currentQuestions.getValue();
        if (all == null) {
            return new ArrayList<>();
        }
        List<Question> worksheetQuestions = new ArrayList<>();
        for (Question q : all) {
            if (q.getQuestionType() == QuestionType.WRITTEN_ANSWER) {
                worksheetQuestions.add(q);
            }
        }
        return worksheetQuestions;
    }

    /**
     * Loads questions for a given material ID from the DAO.
     *
     * @param materialId The material ID to load questions for
     */
    private void loadQuestionsForMaterial(@NonNull String materialId) {
        List<QuestionEntity> entities = questionDao.getByMaterialId(materialId);
        List<Question> questions = new ArrayList<>();
        for (QuestionEntity entity : entities) {
            questions.add(QuestionMapper.toDomain(entity));
        }
        currentQuestions.setValue(questions);
    }
}
