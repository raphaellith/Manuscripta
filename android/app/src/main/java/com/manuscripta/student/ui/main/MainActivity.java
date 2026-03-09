package com.manuscripta.student.ui.main;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.manuscripta.student.R;
import com.manuscripta.student.data.model.MascotSelection;
import com.manuscripta.student.databinding.ActivityMainBinding;
import com.manuscripta.student.domain.model.Configuration;
import com.manuscripta.student.domain.model.Material;
import com.manuscripta.student.domain.model.Question;
import com.manuscripta.student.ui.feedback.FeedbackFragment;
import com.manuscripta.student.ui.quiz.QuizFragment;
import com.manuscripta.student.ui.quiz.QuizViewModel;
import com.manuscripta.student.ui.reading.ReadingFragment;
import com.manuscripta.student.ui.worksheet.WorksheetFragment;
import com.manuscripta.student.ui.worksheet.WorksheetViewModel;
import com.manuscripta.student.network.tcp.RaiseHandManager;
import com.manuscripta.student.network.tcp.PairingManager;
import com.manuscripta.student.network.tcp.PairingState;
import com.manuscripta.student.ui.pairing.PairingActivity;
import com.manuscripta.student.utils.TextToSpeechManager;

import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * Main activity for the Manuscripta Student application.
 * Manages tab navigation between Reading, Quiz, and Worksheet fragments,
 * plus a footer containing the mascot and audio button areas.
 */
@AndroidEntryPoint
public class MainActivity extends AppCompatActivity {

    /** Index constant for the Reading tab. */
    static final int TAB_READING = 0;

    /** Index constant for the Quiz tab. */
    static final int TAB_QUIZ = 1;

    /** Index constant for the Worksheet tab. */
    static final int TAB_WORKSHEET = 2;

    /** View binding for the main activity layout. */
    private ActivityMainBinding binding;

    /** ViewModel for the main activity. */
    private MainViewModel viewModel;

    /** ViewModel for quiz response submission. */
    private QuizViewModel quizViewModel;

    /** ViewModel for worksheet response submission. */
    private WorksheetViewModel worksheetViewModel;

    /** Text-to-speech service manager. */
    private TextToSpeechManager ttsManager;

    /** The currently displayed fragment. */
    @Nullable
    private Fragment currentFragment;

    /** The currently active tab index. */
    private int activeTab = TAB_READING;

    /** Whether the pairing state observer has fired at least once. */
    private boolean observerInitialised;

    /** Manager for the raise-hand lifecycle, injected by Hilt. */
    @Inject
    RaiseHandManager raiseHandManager;

    /** Manager for the pairing lifecycle, injected by Hilt. */
    @Inject
    PairingManager pairingManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        viewModel = new ViewModelProvider(this).get(MainViewModel.class);
        quizViewModel = new ViewModelProvider(this).get(QuizViewModel.class);
        worksheetViewModel = new ViewModelProvider(this).get(WorksheetViewModel.class);

        ttsManager = new TextToSpeechManager();
        ttsManager.init(this);

        setupTabs();
        wireFooterViews();
        observeViewModel();

        if (savedInstanceState == null) {
            selectTab(TAB_READING);
        } else {
            currentFragment = getSupportFragmentManager()
                    .findFragmentById(R.id.fragmentContainer);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ttsManager.shutdown();
        binding = null;
    }

    /**
     * Navigates back to the PairingActivity and finishes this activity.
     * Called when the device is unpaired by the server.
     */
    private void navigateToPairing() {
        Intent intent = new Intent(this, PairingActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    /**
     * Selects the given tab, swapping the displayed fragment and updating tab styles.
     *
     * @param tabIndex The tab index to select (TAB_READING, TAB_QUIZ, or TAB_WORKSHEET).
     */
    void selectTab(int tabIndex) {
        activeTab = tabIndex;
        updateTabStyles();
        showFragmentForTab(tabIndex);
        updateFooterVisibility();
    }

    /**
     * Returns the currently active tab index.
     *
     * @return The active tab index.
     */
    int getActiveTab() {
        return activeTab;
    }

    /**
     * Navigates to the feedback screen for a correct answer.
     *
     * @param explanation The explanation text to show.
     */
    void showCorrectFeedback(@NonNull String explanation) {
        FeedbackFragment fragment = FeedbackFragment.newCorrectInstance(explanation);
        fragment.setNavigationListener(new FeedbackFragment.FeedbackNavigationListener() {
            @Override
            public void onNextQuestion() {
                selectTab(TAB_READING);
            }

            @Override
            public void onTryAgain() {
                // Not applicable for correct feedback
            }
        });
        currentFragment = fragment;
        replaceFragment(fragment);
    }

    /**
     * Navigates to the feedback screen for an incorrect answer.
     *
     * @param correctAnswer The correct answer text to display.
     */
    void showIncorrectFeedback(@NonNull String correctAnswer) {
        FeedbackFragment fragment = FeedbackFragment.newIncorrectInstance(correctAnswer);
        fragment.setNavigationListener(new FeedbackFragment.FeedbackNavigationListener() {
            @Override
            public void onNextQuestion() {
                // Not applicable for incorrect feedback
            }

            @Override
            public void onTryAgain() {
                selectTab(TAB_QUIZ);
            }
        });
        currentFragment = fragment;
        replaceFragment(fragment);
    }

    /**
     * Sets up tab button click listeners.
     */
    private void setupTabs() {
        binding.tabReading.setOnClickListener(v -> selectTab(TAB_READING));
        binding.tabQuiz.setOnClickListener(v -> selectTab(TAB_QUIZ));
        binding.tabWorksheet.setOnClickListener(v -> selectTab(TAB_WORKSHEET));
    }

    /**
     * Updates the tab button styles to highlight the active tab.
     */
    private void updateTabStyles() {
        setTabStyle(binding.tabReading, activeTab == TAB_READING);
        setTabStyle(binding.tabQuiz, activeTab == TAB_QUIZ);
        setTabStyle(binding.tabWorksheet, activeTab == TAB_WORKSHEET);
    }

    /**
     * Applies the active or inactive style to a tab button.
     *
     * @param tab      The tab button to style.
     * @param isActive Whether the tab is currently active.
     */
    private void setTabStyle(@NonNull Button tab, boolean isActive) {
        if (isActive) {
            tab.setBackgroundResource(R.drawable.bg_tab_active);
        } else {
            tab.setBackgroundResource(R.drawable.bg_tab_inactive);
        }
    }

    /**
     * Shows the appropriate fragment for the given tab index.
     *
     * @param tabIndex The tab index to show.
     */
    private void showFragmentForTab(int tabIndex) {
        Fragment fragment;
        switch (tabIndex) {
            case TAB_QUIZ:
                fragment = createQuizFragment();
                break;
            case TAB_WORKSHEET:
                fragment = createWorksheetFragment();
                break;
            default:
                fragment = ReadingFragment.newInstance();
                break;
        }
        currentFragment = fragment;
        replaceFragment(fragment);
        binding.fragmentContainer.post(this::updateFragmentData);
    }

    /**
     * Creates a QuizFragment with its navigation listener wired up.
     *
     * @return A configured QuizFragment instance.
     */
    @NonNull
    private QuizFragment createQuizFragment() {
        QuizFragment quiz = QuizFragment.newInstance();
        quiz.setNavigationListener(new QuizFragment.QuizNavigationListener() {
            @Override
            public void onBackToLesson() {
                selectTab(TAB_READING);
            }

            @Override
            public void onAnswerSubmitted(@NonNull Question question,
                                          @NonNull String answer,
                                          boolean isCorrect) {
                quizViewModel.saveQuizResponse(question, answer);
                if (isCorrect) {
                    showCorrectFeedback(question.getCorrectAnswer());
                } else {
                    showIncorrectFeedback(question.getCorrectAnswer());
                }
            }
        });
        return quiz;
    }

    /**
     * Creates a WorksheetFragment with its submit listener wired up.
     *
     * @return A configured WorksheetFragment instance.
     */
    @NonNull
    private WorksheetFragment createWorksheetFragment() {
        WorksheetFragment worksheet = WorksheetFragment.newInstance();
        worksheet.setSubmitListener(answers -> {
            worksheetViewModel.submitAllAnswers(answers);
        });
        return worksheet;
    }

    /**
     * Replaces the current fragment in the container with the given fragment.
     *
     * @param fragment The fragment to display.
     */
    private void replaceFragment(@NonNull Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .commit();
    }

    /**
     * Updates footer visibility based on the active tab and configuration.
     * The mascot is only visible on the Reading tab when a mascot is selected.
     */
    private void updateFooterVisibility() {
        if (binding == null) {
            return;
        }
        Configuration config = viewModel.getConfiguration().getValue();
        boolean mascotEnabled = config != null
                && config.getMascotSelection() != MascotSelection.NONE;
        int mascotVisibility = (mascotEnabled && activeTab == TAB_READING)
                ? View.VISIBLE : View.INVISIBLE;
        binding.mascotView.setVisibility(mascotVisibility);
    }

    /**
     * Updates the raise-hand button text to reflect the current state.
     *
     * @param handRaiseState The current hand-raise state
     */
    private void updateRaiseHandButton(
            @NonNull RaiseHandManager.HandRaiseState handRaiseState) {
        if (binding == null) {
            return;
        }
        switch (handRaiseState) {
            case PENDING:
                binding.raiseHandButton.setText(R.string.hand_raised_pending);
                break;
            case ACKNOWLEDGED:
                binding.raiseHandButton.setText(R.string.hand_acknowledged);
                break;
            case IDLE:
            default:
                binding.raiseHandButton.setText(R.string.raise_hand);
                break;
        }
    }

    /**
     * Sets up listeners on footer views: mascot, audio button, and AI panel.
     */
    private void wireFooterViews() {
        binding.mascotView.setOnTaskSelected(
            taskName -> viewModel.requestContentTransformation(taskName)
        );
        binding.audioButton.setOnAudioClickListener(
            this::speakCurrentContent
        );
        binding.aiResponsePanel.setOnCloseListener(
            () -> viewModel.clearAiTask()
        );
        binding.raiseHandButton.setOnClickListener(
            v -> raiseHandManager.toggle()
        );
    }

    /**
     * Subscribes to ViewModel LiveData to keep the UI in sync with
     * material, questions, configuration, and AI task state.
     */
    private void observeViewModel() {
        viewModel.getCurrentMaterial().observe(this, material -> {
            if (material != null) {
                updateFragmentData();
            }
        });
        viewModel.getCurrentQuestions().observe(
            this, questions -> updateFragmentData()
        );
        viewModel.getConfiguration().observe(this, config -> {
            if (config != null) {
                applyConfiguration(config);
            }
        });
        viewModel.getAiTaskName().observe(this, taskName -> {
            if (binding == null || taskName == null) {
                return;
            }
            String response = viewModel.getAiResponse().getValue();
            if (response != null) {
                binding.aiResponsePanel.showContent(taskName, response);
            } else {
                binding.aiResponsePanel.showLoading(taskName);
            }
        });
        viewModel.getAiResponse().observe(this, response -> {
            if (binding == null) {
                return;
            }
            String taskName = viewModel.getAiTaskName().getValue();
            if (taskName != null && response != null) {
                binding.aiResponsePanel.showContent(taskName, response);
            }
        });
        viewModel.getScreenLocked().observe(this, locked -> {
            if (binding == null) {
                return;
            }
            binding.lockOverlay.setVisibility(
                    Boolean.TRUE.equals(locked) ? View.VISIBLE : View.GONE);
        });
        raiseHandManager.getState().observe(this, this::updateRaiseHandButton);
        pairingManager.getPairingState().observe(this, pairingState -> {
            if (pairingState == PairingState.NOT_PAIRED && observerInitialised) {
                navigateToPairing();
            }
            observerInitialised = true;
        });
    }

    /**
     * Applies a configuration change to TTS state and footer visibility.
     *
     * @param config The updated configuration.
     */
    private void applyConfiguration(@NonNull Configuration config) {
        ttsManager.setTtsEnabled(config.isTtsEnabled());
        if (binding == null) {
            return;
        }
        binding.audioButton.setVisibility(
            config.isTtsEnabled() ? View.VISIBLE : View.GONE
        );
        updateFooterVisibility();
    }

    /**
     * Pushes current material and question data to the active fragment.
     */
    private void updateFragmentData() {
        if (currentFragment == null || binding == null) {
            return;
        }
        Material material = viewModel.getCurrentMaterial().getValue();
        if (currentFragment instanceof ReadingFragment) {
            updateReadingFragment(material);
        } else if (currentFragment instanceof QuizFragment) {
            updateQuizFragment();
        } else if (currentFragment instanceof WorksheetFragment) {
            updateWorksheetFragment();
        }
    }

    /**
     * Updates the reading fragment with the current material.
     *
     * @param material The material to display, or null to show loading.
     */
    private void updateReadingFragment(@Nullable Material material) {
        ReadingFragment reading = (ReadingFragment) currentFragment;
        if (material != null) {
            java.util.List<com.manuscripta.student.domain.model.Question>
                    questions = viewModel.getCurrentQuestions().getValue();
            reading.displayMaterial(material,
                    questions != null ? questions
                            : java.util.Collections.emptyList());
        } else {
            reading.showLoading();
        }
    }

    /**
     * Updates the quiz fragment with the first available quiz question.
     */
    private void updateQuizFragment() {
        QuizFragment quiz = (QuizFragment) currentFragment;
        List<Question> quizQuestions = viewModel.getQuizQuestions();
        if (!quizQuestions.isEmpty()) {
            quiz.displayQuestion(quizQuestions.get(0));
        } else {
            quiz.showLoading();
        }
    }

    /**
     * Updates the worksheet fragment with available worksheet questions.
     */
    private void updateWorksheetFragment() {
        WorksheetFragment worksheet = (WorksheetFragment) currentFragment;
        List<Question> wsQuestions = viewModel.getWorksheetQuestions();
        if (!wsQuestions.isEmpty()) {
            worksheet.displayQuestions(wsQuestions);
        } else {
            worksheet.showLoading();
        }
    }

    /**
     * Speaks the text content of the currently visible fragment via TTS.
     */
    private void speakCurrentContent() {
        String text = getCurrentFragmentText();
        if (!text.isEmpty()) {
            ttsManager.speak(text);
        }
    }

    /**
     * Returns the text content of the currently active fragment for TTS.
     *
     * @return The displayable text, or an empty string if unavailable.
     */
    @NonNull
    private String getCurrentFragmentText() {
        if (currentFragment instanceof ReadingFragment) {
            return ((ReadingFragment) currentFragment).getTextContent();
        } else if (currentFragment instanceof QuizFragment) {
            return ((QuizFragment) currentFragment).getTextContent();
        } else if (currentFragment instanceof WorksheetFragment) {
            return ((WorksheetFragment) currentFragment).getTextContent();
        } else if (currentFragment instanceof FeedbackFragment) {
            return ((FeedbackFragment) currentFragment).getTextContent();
        }
        return "";
    }
}
