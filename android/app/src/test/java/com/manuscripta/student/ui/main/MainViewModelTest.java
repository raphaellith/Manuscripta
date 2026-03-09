package com.manuscripta.student.ui.main;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.manuscripta.student.data.local.QuestionDao;
import com.manuscripta.student.data.model.FeedbackStyle;
import com.manuscripta.student.data.model.MascotSelection;
import com.manuscripta.student.data.model.MaterialType;
import com.manuscripta.student.data.model.QuestionEntity;
import com.manuscripta.student.data.model.QuestionType;
import com.manuscripta.student.data.model.DeviceStatus;
import com.manuscripta.student.data.repository.ConfigRepository;
import com.manuscripta.student.data.repository.DeviceStatusRepository;
import com.manuscripta.student.data.repository.FeedbackRepository;
import com.manuscripta.student.data.repository.MaterialRepository;
import com.manuscripta.student.data.repository.SessionRepository;
import com.manuscripta.student.domain.model.Configuration;
import com.manuscripta.student.domain.model.Material;
import com.manuscripta.student.domain.model.Question;
import com.manuscripta.student.utils.ConnectionManager;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Unit tests for {@link MainViewModel}.
 */
public class MainViewModelTest {

    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    @Mock
    private MaterialRepository mockMaterialRepository;

    @Mock
    private QuestionDao mockQuestionDao;

    @Mock
    private ConfigRepository mockConfigRepository;

    @Mock
    private ConnectionManager mockConnectionManager;

    @Mock
    private SessionRepository mockSessionRepository;

    @Mock
    private DeviceStatusRepository mockDeviceStatusRepository;

    @Mock
    private FeedbackRepository mockFeedbackRepository;

    private MutableLiveData<List<Material>> materialsLiveData;
    private MutableLiveData<com.manuscripta.student.domain.model.DeviceStatus> deviceStatusLiveData;
    private MutableLiveData<Configuration> configLiveData;

    private MainViewModel viewModel;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        materialsLiveData = new MutableLiveData<>(Collections.emptyList());
        deviceStatusLiveData = new MutableLiveData<>();
        configLiveData = new MutableLiveData<>(Configuration.createDefault());
        when(mockConfigRepository.getConfig()).thenReturn(Configuration.createDefault());
        when(mockConnectionManager.getConnectionState())
                .thenReturn(new MutableLiveData<>(true));
        when(mockMaterialRepository.getMaterialsLiveData()).thenReturn(materialsLiveData);
        when(mockSessionRepository.getSessionsByMaterialId(org.mockito.ArgumentMatchers.anyString()))
                .thenReturn(Collections.emptyList());
        when(mockDeviceStatusRepository.getDeviceStatusLiveData()).thenReturn(deviceStatusLiveData);
        when(mockConfigRepository.getConfigLiveData()).thenReturn(configLiveData);
        when(mockFeedbackRepository.getFeedbackLiveData())
                .thenReturn(new MutableLiveData<>(Collections.emptyList()));
        viewModel = new MainViewModel(
                mockMaterialRepository, mockQuestionDao,
                mockConfigRepository, mockConnectionManager,
                mockSessionRepository, mockDeviceStatusRepository,
                mockFeedbackRepository);
    }

    @Test
    public void testViewModelCreation() {
        assertNotNull(viewModel);
    }

    @Test
    public void testInitialActiveTabIsReading() {
        assertEquals(Integer.valueOf(MainViewModel.TAB_READING),
                viewModel.getActiveTab().getValue());
    }

    @Test
    public void testSetActiveTab() {
        viewModel.setActiveTab(MainViewModel.TAB_QUIZ);
        assertEquals(Integer.valueOf(MainViewModel.TAB_QUIZ),
                viewModel.getActiveTab().getValue());
    }

    @Test
    public void testSetActiveTabToWorksheet() {
        viewModel.setActiveTab(MainViewModel.TAB_WORKSHEET);
        assertEquals(Integer.valueOf(MainViewModel.TAB_WORKSHEET),
                viewModel.getActiveTab().getValue());
    }

    @Test
    public void testConfigurationLoadedOnCreation() {
        Configuration config = viewModel.getConfiguration().getValue();
        assertNotNull(config);
        assertEquals(Configuration.DEFAULT_TEXT_SIZE, config.getTextSize());
    }

    @Test
    public void testRefreshConfiguration() {
        Configuration custom = new Configuration(
                20, FeedbackStyle.NEUTRAL, true, true, true, MascotSelection.MASCOT1);
        when(mockConfigRepository.getConfig()).thenReturn(custom);

        viewModel.refreshConfiguration();

        Configuration result = viewModel.getConfiguration().getValue();
        assertNotNull(result);
        assertEquals(20, result.getTextSize());
        assertTrue(result.isTtsEnabled());
    }

    @Test
    public void testConfigAutoRefreshesWhenLiveDataUpdates() {
        viewModel.getConfiguration().observeForever(c -> { });

        Configuration pushed = new Configuration(
                24, FeedbackStyle.NEUTRAL, false, true, false, MascotSelection.MASCOT2);
        configLiveData.setValue(pushed);

        Configuration result = viewModel.getConfiguration().getValue();
        assertNotNull(result);
        assertEquals(24, result.getTextSize());
    }

    @Test
    public void testSetCurrentMaterial() {
        Material material = createTestMaterial("mat-1", "Test Title");
        when(mockQuestionDao.getByMaterialId("mat-1"))
                .thenReturn(Collections.emptyList());

        viewModel.setCurrentMaterial(material);

        Material result = viewModel.getCurrentMaterial().getValue();
        assertNotNull(result);
        assertEquals("mat-1", result.getId());
        assertEquals("Test Title", result.getTitle());
    }

    @Test
    public void testSetCurrentMaterialLoadsQuestions() throws InterruptedException {
        Material material = createTestMaterial("mat-1", "Title");
        List<QuestionEntity> entities = Arrays.asList(
                createQuestionEntity("q-1", "mat-1", QuestionType.MULTIPLE_CHOICE),
                createQuestionEntity("q-2", "mat-1", QuestionType.WRITTEN_ANSWER)
        );
        when(mockQuestionDao.getByMaterialId("mat-1")).thenReturn(entities);

        viewModel.setCurrentMaterial(material);

        // loadQuestionsForMaterial runs on a background executor; wait for it
        Thread.sleep(200);

        List<Question> questions = viewModel.getCurrentQuestions().getValue();
        assertNotNull(questions);
        assertEquals(2, questions.size());
        assertEquals("q-1", questions.get(0).getId());
        assertEquals("q-2", questions.get(1).getId());
    }

    @Test
    public void testLoadMaterialById() throws InterruptedException {
        Material material = createTestMaterial("mat-2", "Loaded Title");
        when(mockMaterialRepository.getMaterialById("mat-2")).thenReturn(material);
        when(mockQuestionDao.getByMaterialId("mat-2"))
                .thenReturn(Collections.emptyList());

        viewModel.loadMaterial("mat-2");

        // loadMaterial runs on a background executor; wait for it
        Thread.sleep(200);

        Material result = viewModel.getCurrentMaterial().getValue();
        assertNotNull(result);
        assertEquals("mat-2", result.getId());
    }

    @Test
    public void testLoadMaterialByIdNotFound() {
        when(mockMaterialRepository.getMaterialById("missing")).thenReturn(null);

        viewModel.loadMaterial("missing");

        assertNull(viewModel.getCurrentMaterial().getValue());
    }

    @Test
    public void testGetQuizQuestions() throws InterruptedException {
        Material material = createTestMaterial("mat-1", "Title");
        List<QuestionEntity> entities = Arrays.asList(
                createQuestionEntity("q-1", "mat-1", QuestionType.MULTIPLE_CHOICE),
                createQuestionEntity("q-2", "mat-1", QuestionType.WRITTEN_ANSWER),
                createQuestionEntity("q-3", "mat-1", QuestionType.MULTIPLE_CHOICE)
        );
        when(mockQuestionDao.getByMaterialId("mat-1")).thenReturn(entities);
        viewModel.setCurrentMaterial(material);

        // loadQuestionsForMaterial runs on a background executor; wait for it
        Thread.sleep(200);

        List<Question> quizQuestions = viewModel.getQuizQuestions();
        assertEquals(2, quizQuestions.size());
        assertEquals("q-1", quizQuestions.get(0).getId());
        assertEquals("q-3", quizQuestions.get(1).getId());
    }

    @Test
    public void testGetWorksheetQuestions() throws InterruptedException {
        Material material = createTestMaterial("mat-1", "Title");
        List<QuestionEntity> entities = Arrays.asList(
                createQuestionEntity("q-1", "mat-1", QuestionType.MULTIPLE_CHOICE),
                createQuestionEntity("q-2", "mat-1", QuestionType.WRITTEN_ANSWER),
                createQuestionEntity("q-3", "mat-1", QuestionType.WRITTEN_ANSWER)
        );
        when(mockQuestionDao.getByMaterialId("mat-1")).thenReturn(entities);
        viewModel.setCurrentMaterial(material);

        // loadQuestionsForMaterial runs on a background executor; wait for it
        Thread.sleep(200);

        List<Question> worksheetQuestions = viewModel.getWorksheetQuestions();
        assertEquals(2, worksheetQuestions.size());
        assertEquals("q-2", worksheetQuestions.get(0).getId());
        assertEquals("q-3", worksheetQuestions.get(1).getId());
    }

    @Test
    public void testGetQuizQuestionsWhenNoQuestionsLoaded() {
        List<Question> quizQuestions = viewModel.getQuizQuestions();
        assertNotNull(quizQuestions);
        assertTrue(quizQuestions.isEmpty());
    }

    @Test
    public void testGetWorksheetQuestionsWhenNoQuestionsLoaded() {
        List<Question> worksheetQuestions = viewModel.getWorksheetQuestions();
        assertNotNull(worksheetQuestions);
        assertTrue(worksheetQuestions.isEmpty());
    }

    @Test
    public void testGetConnectionState() {
        LiveData<Boolean> connectionState = viewModel.getConnectionState();
        assertNotNull(connectionState);
        assertEquals(Boolean.TRUE, connectionState.getValue());
    }

    @Test
    public void testTabConstants() {
        assertEquals(0, MainViewModel.TAB_READING);
        assertEquals(1, MainViewModel.TAB_QUIZ);
        assertEquals(2, MainViewModel.TAB_WORKSHEET);
    }

    @Test
    public void testRequestContentTransformation_setsTaskName() {
        viewModel.requestContentTransformation("Simplify");

        assertEquals("Simplify", viewModel.getAiTaskName().getValue());
    }

    @Test
    public void testRequestContentTransformation_clearsResponse() {
        viewModel.requestContentTransformation("Summarise");

        assertNull(viewModel.getAiResponse().getValue());
    }

    @Test
    public void testClearAiTask_nullifiesTaskName() {
        viewModel.requestContentTransformation("Simplify");
        viewModel.clearAiTask();

        assertNull(viewModel.getAiTaskName().getValue());
    }

    @Test
    public void testClearAiTask_nullifiesResponse() {
        viewModel.requestContentTransformation("Simplify");
        viewModel.clearAiTask();

        assertNull(viewModel.getAiResponse().getValue());
    }

    @Test
    public void testGetAiTaskNameInitiallyNull() {
        assertNull(viewModel.getAiTaskName().getValue());
    }

    @Test
    public void testGetAiResponseInitiallyNull() {
        assertNull(viewModel.getAiResponse().getValue());
    }

    @Test
    public void testRequestContentTransformationOverwritesPreviousTask() {
        viewModel.requestContentTransformation("Simplify");
        viewModel.requestContentTransformation("Summarise");

        assertEquals("Summarise", viewModel.getAiTaskName().getValue());
    }

    // ========== Lock screen state ==========

    @Test
    public void testScreenLockedIsTrueWhenDeviceStatusLocked() {
        viewModel.getScreenLocked().observeForever(v -> { });
        deviceStatusLiveData.setValue(
                com.manuscripta.student.domain.model.DeviceStatus.create(
                        "dev-1", DeviceStatus.LOCKED, 50, null, null));

        assertEquals(Boolean.TRUE, viewModel.getScreenLocked().getValue());
    }

    @Test
    public void testScreenLockedIsFalseWhenDeviceStatusOnTask() {
        viewModel.getScreenLocked().observeForever(v -> { });
        deviceStatusLiveData.setValue(
                com.manuscripta.student.domain.model.DeviceStatus.create(
                        "dev-1", DeviceStatus.ON_TASK, 50, null, null));

        assertEquals(Boolean.FALSE, viewModel.getScreenLocked().getValue());
    }

    @Test
    public void testScreenLockedIsFalseWhenNoStatus() {
        viewModel.getScreenLocked().observeForever(v -> { });
        deviceStatusLiveData.setValue(null);

        assertEquals(Boolean.FALSE, viewModel.getScreenLocked().getValue());
    }

    // ========== Material distribution auto-selection ==========

    @Test
    public void testAutoSelectsFirstMaterial_whenNoneSelected() {
        Material mat = createTestMaterial("mat-auto", "Auto Title");
        when(mockQuestionDao.getByMaterialId("mat-auto"))
                .thenReturn(Collections.emptyList());
        viewModel.getCurrentMaterial().observeForever(m -> { });

        materialsLiveData.setValue(Arrays.asList(mat));

        Material result = viewModel.getCurrentMaterial().getValue();
        assertNotNull(result);
        assertEquals("mat-auto", result.getId());
    }

    @Test
    public void testDoesNotOverrideExistingMaterial() {
        Material existing = createTestMaterial("mat-1", "Existing");
        when(mockQuestionDao.getByMaterialId("mat-1"))
                .thenReturn(Collections.emptyList());
        viewModel.getCurrentMaterial().observeForever(m -> { });
        viewModel.setCurrentMaterial(existing);

        Material newMat = createTestMaterial("mat-2", "New");
        materialsLiveData.setValue(Arrays.asList(newMat));

        Material result = viewModel.getCurrentMaterial().getValue();
        assertNotNull(result);
        assertEquals("mat-1", result.getId());
    }

    @Test
    public void testRefreshesCurrentMaterialFromNewDistribution() {
        Material original = createTestMaterial("mat-1", "Original");
        when(mockQuestionDao.getByMaterialId("mat-1"))
                .thenReturn(Collections.emptyList());
        viewModel.getCurrentMaterial().observeForever(m -> { });
        viewModel.setCurrentMaterial(original);

        Material updated = new Material("mat-1", MaterialType.READING,
                "Updated Title", "New content", "{}", "[]", System.currentTimeMillis());
        materialsLiveData.setValue(Arrays.asList(updated));

        Material result = viewModel.getCurrentMaterial().getValue();
        assertNotNull(result);
        assertEquals("Updated Title", result.getTitle());
    }

    private Material createTestMaterial(String id, String title) {
        return new Material(id, MaterialType.READING, title,
                "Content text", "{}", "[]", System.currentTimeMillis());
    }

    private QuestionEntity createQuestionEntity(String id, String materialId,
                                                QuestionType type) {
        return new QuestionEntity(id, materialId, "Question text?",
                type, "[\"A\",\"B\",\"C\"]", "A");
    }
}
