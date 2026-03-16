package com.manuscripta.student.ui.main;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.PopupMenu;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.manuscripta.student.R;
import com.manuscripta.student.data.model.FeedbackStyle;
import com.manuscripta.student.domain.model.Feedback;
import com.manuscripta.student.data.model.MascotSelection;
import com.manuscripta.student.databinding.ActivityMainBinding;
import com.manuscripta.student.domain.model.Configuration;
import com.manuscripta.student.domain.model.Material;
import com.manuscripta.student.domain.model.Question;
import com.manuscripta.student.network.ApiService;
import com.manuscripta.student.ui.feedback.FeedbackFragment;
import com.manuscripta.student.ui.feedback.TeacherFeedbackFragment;
import com.manuscripta.student.ui.quiz.QuizFragment;
import com.manuscripta.student.ui.quiz.QuizViewModel;
import com.manuscripta.student.ui.reading.ReadingFragment;
import com.manuscripta.student.ui.renderer.AttachmentImageLoader;
import com.manuscripta.student.ui.worksheet.WorksheetFragment;
import com.manuscripta.student.ui.worksheet.WorksheetViewModel;
import com.manuscripta.student.network.tcp.RaiseHandManager;
import com.manuscripta.student.network.tcp.PairingManager;
import com.manuscripta.student.network.tcp.PairingState;
import com.manuscripta.student.ui.pairing.PairingActivity;
import com.manuscripta.student.utils.FileStorageManager;
import com.manuscripta.student.utils.TextToSpeechManager;

import java.util.ArrayList;
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

    /** Tag for logging. */
    private static final String TAG = "MainActivity";

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

    /** Cached list of all distributed materials for the dropdown. */
    private final List<Material> allMaterials = new ArrayList<>();

    /** Cached list of all received feedback items for the dropdown. */
    private final List<Feedback> allFeedback = new ArrayList<>();

    /** Whether the pairing state observer has fired at least once. */
    private boolean observerInitialised;

    /** Tracks the number of known materials for new-material notifications. */
    private int lastKnownMaterialCount;

    /** Tracks the number of feedback items already seen by the UI. */
    private int lastKnownFeedbackCount;

    /** Manager for the raise-hand lifecycle, injected by Hilt. */
    @Inject
    RaiseHandManager raiseHandManager;

    /** Manager for the pairing lifecycle, injected by Hilt. */
    @Inject
    PairingManager pairingManager;

    /** API service for network requests, injected by Hilt. */
    @Inject
    ApiService apiService;

    /** File storage manager for attachments, injected by Hilt. */
    @Inject
    FileStorageManager fileStorageManager;

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

        setupMaterialDropdown();
        wireFooterViews();
        observeViewModel();

        if (savedInstanceState != null) {
            currentFragment = getSupportFragmentManager()
                    .findFragmentById(R.id.fragmentContainer);
            reinjectFragmentDependencies();
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
     * Selects the given material, switching to the appropriate fragment
     * based on the material type and loading its data.
     *
     * @param material The material to display.
     */
    void selectMaterial(@NonNull Material material) {
        Log.d(TAG, "Selecting material: " + material.getTitle()
                + " (" + material.getType() + ")");
        viewModel.setCurrentMaterial(material);
        if (binding != null) {
            binding.materialDropdown.setText(material.getTitle());
            binding.teacherFeedbackPanel.hide();
        }
        showFragmentForMaterial(material);
        updateFooterVisibility();
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
                returnToCurrentMaterial();
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
                returnToCurrentMaterial();
            }
        });
        currentFragment = fragment;
        replaceFragment(fragment);
    }

    /**
     * Returns to the fragment for the currently selected material.
     */
    private void returnToCurrentMaterial() {
        Material material = viewModel.getCurrentMaterial().getValue();
        if (material != null) {
            showFragmentForMaterial(material);
        }
    }

    /**
     * Sets up the material dropdown button click listener.
     */
    private void setupMaterialDropdown() {
        binding.materialDropdown.setOnClickListener(v -> showMaterialDropdown());
        try {
            Typeface fraunces = ResourcesCompat.getFont(
                    this, R.font.fraunces_bold);
            if (fraunces != null) {
                binding.materialDropdown.setTypeface(fraunces);
            }
        } catch (Exception e) {
            Log.d(TAG, "Fraunces font not available");
        }
    }

    /**
     * Shows a popup menu listing all available materials and feedback items.
     */
    private void showMaterialDropdown() {
        if (allMaterials.isEmpty() && allFeedback.isEmpty()) {
            Toast.makeText(this, R.string.no_materials, Toast.LENGTH_SHORT).show();
            return;
        }
        PopupMenu popup = new PopupMenu(this, binding.materialDropdown);
        for (int i = 0; i < allMaterials.size(); i++) {
            popup.getMenu().add(0, i, i, allMaterials.get(i).getTitle());
        }
        for (int i = 0; i < allFeedback.size(); i++) {
            int id = allMaterials.size() + i;
            popup.getMenu().add(0, id, id, buildFeedbackLabel(allFeedback.get(i)));
        }
        popup.setOnMenuItemClickListener(item -> {
            int index = item.getItemId();
            if (index < allMaterials.size()) {
                selectMaterial(allMaterials.get(index));
            } else {
                int feedbackIndex = index - allMaterials.size();
                if (feedbackIndex < allFeedback.size()) {
                    selectFeedback(allFeedback.get(feedbackIndex));
                }
            }
            return true;
        });
        popup.show();
    }

    /**
     * Displays the given feedback item as material-style full-screen content.
     *
     * @param feedback The feedback item to display.
     */
    private void selectFeedback(@NonNull Feedback feedback) {
        if (binding != null) {
            binding.materialDropdown.setText(buildFeedbackLabel(feedback));
            binding.teacherFeedbackPanel.hide();
        }
        TeacherFeedbackFragment fragment = TeacherFeedbackFragment.newInstance(feedback);
        currentFragment = fragment;
        replaceFragment(fragment);
        updateFooterVisibility();
    }

    /**
     * Builds a human-readable dropdown label for a feedback item.
     *
     * @param feedback The feedback item.
     * @return A label string for display in the dropdown menu.
     */
    private String buildFeedbackLabel(@NonNull Feedback feedback) {
        if (feedback.hasMarks()) {
            return getString(R.string.feedback_dropdown_marks, feedback.getMarks());
        }
        return getString(R.string.feedback_dropdown_comment);
    }

    /**
     * Shows the appropriate fragment for the given material's type.
     *
     * @param material The material to display.
     */
    private void showFragmentForMaterial(@NonNull Material material) {
        Fragment fragment;
        switch (material.getType()) {
            case POLL:
                fragment = createQuizFragment();
                break;
            case WORKSHEET:
                fragment = createWorksheetFragment();
                break;
            case READING:
            default:
                fragment = createReadingFragment();
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
                returnToCurrentMaterial();
            }

            @Override
            public void onAnswerSubmitted(@NonNull Question question,
                                          @NonNull String answer,
                                          boolean isCorrect) {
                Log.d(TAG, "Quiz answer submitted for question: "
                        + question.getId() + ", answer: " + answer
                        + ", correct: " + isCorrect);
                quizViewModel.saveQuizResponse(question, answer);
                Toast.makeText(MainActivity.this,
                        R.string.answer_submitted, Toast.LENGTH_SHORT).show();
                Configuration config = viewModel.getConfiguration().getValue();
                boolean immediate = config == null
                        || config.getFeedbackStyle() == FeedbackStyle.IMMEDIATE;
                if (immediate) {
                    java.util.List<String> opts = QuizFragment.parseOptions(
                            question.getOptions());
                    String correctText = QuizFragment.resolveCorrectAnswer(
                            question.getCorrectAnswer(), opts);
                    if (isCorrect) {
                        showCorrectFeedback(correctText);
                    } else {
                        showIncorrectFeedback(correctText);
                    }
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
        worksheet.setAttachmentImageLoader(
            new AttachmentImageLoader(apiService, fileStorageManager));
        worksheet.setFileStorageManager(fileStorageManager);
        worksheet.setSubmitListener(answers -> {
            Log.d(TAG, "Worksheet answers submitted: " + answers.size());
            worksheetViewModel.submitAllAnswers(answers);
            Toast.makeText(MainActivity.this,
                    R.string.answer_submitted, Toast.LENGTH_SHORT).show();
        });
        return worksheet;
    }

    /**
     * Creates a ReadingFragment with attachment rendering
     * dependencies wired up.
     *
     * @return A configured ReadingFragment instance.
     */
    @NonNull
    private ReadingFragment createReadingFragment() {
        ReadingFragment reading = ReadingFragment.newInstance();
        reading.setAttachmentImageLoader(
                new AttachmentImageLoader(
                        apiService, fileStorageManager));
        reading.setFileStorageManager(fileStorageManager);
        return reading;
    }

    /**
     * Re-injects dependencies into a fragment recovered from a
     * configuration change (e.g. orientation rotation). Without
     * this, setter-injected references such as the image loader
     * and file storage manager would be null.
     */
    private void reinjectFragmentDependencies() {
        if (currentFragment instanceof ReadingFragment) {
            ReadingFragment reading = (ReadingFragment) currentFragment;
            reading.setAttachmentImageLoader(
                    new AttachmentImageLoader(
                            apiService, fileStorageManager));
            reading.setFileStorageManager(fileStorageManager);
            reading.resetRenderer();
        } else if (currentFragment instanceof WorksheetFragment) {
            WorksheetFragment worksheet = (WorksheetFragment) currentFragment;
            worksheet.setAttachmentImageLoader(
                new AttachmentImageLoader(apiService, fileStorageManager));
            worksheet.setFileStorageManager(fileStorageManager);
            worksheet.setSubmitListener(answers -> {
                        Log.d(TAG, "Worksheet answers submitted: "
                                + answers.size());
                        worksheetViewModel.submitAllAnswers(answers);
                        Toast.makeText(MainActivity.this,
                                R.string.answer_submitted,
                                Toast.LENGTH_SHORT).show();
                    });
            worksheet.resetRenderer();
        } else if (currentFragment instanceof QuizFragment) {
            ((QuizFragment) currentFragment).setNavigationListener(
                    new QuizFragment.QuizNavigationListener() {
                        @Override
                        public void onBackToLesson() {
                            returnToCurrentMaterial();
                        }

                        @Override
                        public void onAnswerSubmitted(
                                @NonNull Question question,
                                @NonNull String answer,
                                boolean isCorrect) {
                            quizViewModel.saveQuizResponse(
                                    question, answer);
                            Toast.makeText(MainActivity.this,
                                    R.string.answer_submitted,
                                    Toast.LENGTH_SHORT).show();
                            Configuration config =
                                    viewModel.getConfiguration().getValue();
                            boolean immediate = config == null
                                    || config.getFeedbackStyle()
                                    == FeedbackStyle.IMMEDIATE;
                            if (immediate) {
                                java.util.List<String> opts =
                                        QuizFragment.parseOptions(
                                                question.getOptions());
                                String correctText =
                                        QuizFragment.resolveCorrectAnswer(
                                                question.getCorrectAnswer(),
                                                opts);
                                if (isCorrect) {
                                    showCorrectFeedback(correctText);
                                } else {
                                    showIncorrectFeedback(correctText);
                                }
                            }
                        }
                    });
        }
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
     * Updates footer visibility based on the current fragment and configuration.
     * The mascot is only visible on the Reading fragment when a mascot is selected.
     */
    private void updateFooterVisibility() {
        if (binding == null) {
            return;
        }
        Configuration config = viewModel.getConfiguration().getValue();
        boolean mascotEnabled = config != null
                && config.getMascotSelection() != MascotSelection.NONE;
        boolean isReading = currentFragment instanceof ReadingFragment;
        int mascotVisibility = (mascotEnabled && isReading)
                ? View.VISIBLE : View.INVISIBLE;
        binding.mascotView.setVisibility(mascotVisibility);
    }

    /**
     * Updates the raise-hand button text and enabled state.
     *
     * @param handRaiseState The current hand-raise state
     */
    private void updateRaiseHandButton(
            @NonNull RaiseHandManager.HandRaiseState handRaiseState) {
        if (binding == null) {
            return;
        }
        switch (handRaiseState) {
            case COOLDOWN:
                binding.raiseHandButton.setText(R.string.hand_raised_cooldown);
                binding.raiseHandButton.setEnabled(false);
                break;
            case IDLE:
            default:
                binding.raiseHandButton.setText(R.string.raise_hand);
                binding.raiseHandButton.setEnabled(true);
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
            v -> raiseHandManager.raiseHand()
        );
    }

    /**
     * Subscribes to ViewModel LiveData to keep the UI in sync with
     * material, questions, configuration, and AI task state.
     */
    private void observeViewModel() {
        viewModel.getAllMaterials().observe(this, materials -> {
            allMaterials.clear();
            if (materials != null && !materials.isEmpty()) {
                allMaterials.addAll(materials);
                if (materials.size() > lastKnownMaterialCount
                        && lastKnownMaterialCount > 0) {
                    showMaterialReceivedNotification();
                }
                lastKnownMaterialCount = materials.size();
            } else if (binding != null) {
                binding.materialDropdown.setText(
                        R.string.no_materials_deployed);
            }
        });
        viewModel.getCurrentMaterial().observe(this, material -> {
            if (material != null) {
                if (binding != null) {
                    binding.materialDropdown.setText(material.getTitle());
                }
                if (currentFragment == null) {
                    showFragmentForMaterial(material);
                } else {
                    updateFragmentData();
                }
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
        viewModel.getFeedbackLiveData().observe(this, feedbackList -> {
            if (binding == null || feedbackList == null) {
                return;
            }
            allFeedback.clear();
            allFeedback.addAll(feedbackList);
            if (feedbackList.size() > lastKnownFeedbackCount
                    && lastKnownFeedbackCount > 0) {
                showFeedbackReceivedNotification();
            }
            lastKnownFeedbackCount = feedbackList.size();
        });
    }

    /**
     * Default configuration text size used as the baseline for
     * computing the scale factor.
     */
    private static final float DEFAULT_TEXT_SIZE = 6f;

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

        float scaleFactor = config.getTextSize() / DEFAULT_TEXT_SIZE;
        if (currentFragment instanceof ReadingFragment) {
            ((ReadingFragment) currentFragment)
                    .setTextScaleFactor(scaleFactor);
        } else if (currentFragment instanceof WorksheetFragment) {
            ((WorksheetFragment) currentFragment)
                    .setTextScaleFactor(scaleFactor);
        }
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
            updateWorksheetFragment(material);
        }
    }

    /**
     * Updates the reading fragment with the current material.
     *
     * @param material The material to display, or null to show loading.
     */
    private void updateReadingFragment(@Nullable Material material) {
        ReadingFragment reading = (ReadingFragment) currentFragment;
        Configuration config =
                viewModel.getConfiguration().getValue();
        if (config != null) {
            reading.setTextScaleFactor(
                    config.getTextSize() / DEFAULT_TEXT_SIZE);
        }
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
     * Updates the worksheet fragment with material content and all related questions.
     *
     * @param material The current material, or null to show loading.
     */
    private void updateWorksheetFragment(@Nullable Material material) {
        WorksheetFragment worksheet = (WorksheetFragment) currentFragment;
        if (material == null) {
            worksheet.showLoading();
            return;
        }
        List<Question> wsQuestions = viewModel.getWorksheetQuestions();
        worksheet.displayMaterial(material, wsQuestions);
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
        } else if (currentFragment instanceof TeacherFeedbackFragment) {
            return ((TeacherFeedbackFragment) currentFragment).getTextContent();
        }
        return "";
    }

    /**
     * Shows a brief notification at the top of the screen when
     * new material has been received from the teacher server.
     */
    private void showMaterialReceivedNotification() {
        Toast toast = Toast.makeText(
                this, R.string.material_received,
                Toast.LENGTH_SHORT);
        toast.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL,
                0, 32);
        toast.show();
    }

    /**
     * Shows a brief notification at the top of the screen when
     * new feedback has been received from the teacher server.
     */
    private void showFeedbackReceivedNotification() {
        Toast toast = Toast.makeText(
                this, R.string.feedback_received,
                Toast.LENGTH_SHORT);
        toast.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL,
                0, 32);
        toast.show();
    }
}
