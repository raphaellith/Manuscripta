package com.manuscripta.student.ui.materials;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.manuscripta.student.R;
import com.manuscripta.student.data.model.MaterialType;
import com.manuscripta.student.domain.model.Material;

import java.util.List;
import java.util.Locale;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * Activity displaying one material with tabs to switch between Reading/Quiz/Worksheet formats.
 * Shows the same educational content in different formats.
 * Includes TextToSpeech functionality for accessibility.
 */
@AndroidEntryPoint
public class MaterialListActivity extends AppCompatActivity implements TextToSpeech.OnInitListener {

    private static final String TAG = "MaterialListActivity";
    private static final String UTTERANCE_ID = "MaterialContentTTS";

    private ProgressBar progressBar;
    private ScrollView contentScrollView;
    private TextView emptyView;
    private TextView materialTitle;
    private TextView materialContent;
    private TextView tabReading;
    private TextView tabQuiz;
    private TextView tabWorksheet;
    private ImageView mascotIcon;
    private TextView helpText;
    private ImageButton audioButton;
    private MaterialListViewModel viewModel;
    private MaterialGroup currentMaterialGroup;

    private TextToSpeech textToSpeech;
    private boolean isTtsInitialized = false;
    private boolean isSpeaking = false;
    private String ttsInitFailureReason = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_material_list);

        setupViews();
        setupTabs();
        setupListeners();
        setupViewModel();
        initializeTextToSpeech();

        viewModel.loadMaterials();
    }

    private void setupViews() {
        progressBar = findViewById(R.id.progressBar);
        contentScrollView = findViewById(R.id.contentScrollView);
        emptyView = findViewById(R.id.emptyView);
        materialTitle = findViewById(R.id.materialTitle);
        materialContent = findViewById(R.id.materialContent);
        tabReading = findViewById(R.id.tabReading);
        tabQuiz = findViewById(R.id.tabQuiz);
        tabWorksheet = findViewById(R.id.tabWorksheet);
        mascotIcon = findViewById(R.id.mascotIcon);
        helpText = findViewById(R.id.helpText);
        audioButton = findViewById(R.id.audioButton);
    }

    private void setupTabs() {
        tabReading.setOnClickListener(v -> switchFormat(MaterialType.READING));
        tabQuiz.setOnClickListener(v -> switchFormat(MaterialType.QUIZ));
        tabWorksheet.setOnClickListener(v -> switchFormat(MaterialType.WORKSHEET));
    }

    private void setupListeners() {
        // Help button functionality
        View.OnClickListener helpListener = v -> {
            Toast.makeText(this, "Help request sent to teacher!", Toast.LENGTH_SHORT).show();
            // TODO: Send help request via network when implemented
        };
        helpText.setOnClickListener(helpListener);
        mascotIcon.setOnClickListener(helpListener);

        // Audio button functionality - play/pause TTS
        audioButton.setOnClickListener(v -> toggleTextToSpeech());
    }

    private void switchFormat(MaterialType type) {
        if (currentMaterialGroup == null || !currentMaterialGroup.hasFormat(type)) {
            Toast.makeText(this, type + " format not available", Toast.LENGTH_SHORT).show();
            return;
        }

        viewModel.switchFormat(type);
    }

    private void setupViewModel() {
        viewModel = new ViewModelProvider(this).get(MaterialListViewModel.class);

        // Observe material group changes
        viewModel.getMaterialGroupState().observe(this, state -> {
            if (state.isLoading()) {
                showLoading();
            } else if (state.isSuccess()) {
                currentMaterialGroup = state.getData();
                showMaterialGroup(currentMaterialGroup);
            } else if (state.isError()) {
                showError(state.getErrorMessage());
            }
        });

        // Observe format changes
        viewModel.getCurrentFormatState().observe(this, format -> {
            updateTabSelection(format);
            displayCurrentMaterial();
        });
    }

    private void showLoading() {
        progressBar.setVisibility(View.VISIBLE);
        contentScrollView.setVisibility(View.GONE);
        emptyView.setVisibility(View.GONE);
    }

    private void showMaterialGroup(MaterialGroup materialGroup) {
        progressBar.setVisibility(View.GONE);

        if (materialGroup == null) {
            contentScrollView.setVisibility(View.GONE);
            emptyView.setVisibility(View.VISIBLE);
            emptyView.setText("No materials available");
            return;
        }

        // Show title
        materialTitle.setText(materialGroup.getTitle());

        // Enable/disable tabs based on available formats
        updateTabAvailability(materialGroup);

        // Show content
        contentScrollView.setVisibility(View.VISIBLE);
        emptyView.setVisibility(View.GONE);
        displayCurrentMaterial();
    }

    private void updateTabAvailability(MaterialGroup group) {
        tabReading.setEnabled(group.hasFormat(MaterialType.READING));
        tabReading.setAlpha(group.hasFormat(MaterialType.READING) ? 1.0f : 0.5f);

        tabQuiz.setEnabled(group.hasFormat(MaterialType.QUIZ));
        tabQuiz.setAlpha(group.hasFormat(MaterialType.QUIZ) ? 1.0f : 0.5f);

        tabWorksheet.setEnabled(group.hasFormat(MaterialType.WORKSHEET));
        tabWorksheet.setAlpha(group.hasFormat(MaterialType.WORKSHEET) ? 1.0f : 0.5f);
    }

    private void updateTabSelection(MaterialType type) {
        // Update tab appearances
        tabReading.setBackgroundResource(
                type == MaterialType.READING ? R.drawable.tab_selected : R.drawable.tab_unselected);

        tabQuiz.setBackgroundResource(
                type == MaterialType.QUIZ ? R.drawable.tab_selected : R.drawable.tab_unselected);

        tabWorksheet.setBackgroundResource(
                type == MaterialType.WORKSHEET ? R.drawable.tab_selected : R.drawable.tab_unselected);

        // Make selected tab bold
        tabReading.setTypeface(null, type == MaterialType.READING ?
                android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
        tabQuiz.setTypeface(null, type == MaterialType.QUIZ ?
                android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
        tabWorksheet.setTypeface(null, type == MaterialType.WORKSHEET ?
                android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
    }

    private void displayCurrentMaterial() {
        Material currentMaterial = viewModel.getCurrentMaterial();
        if (currentMaterial != null) {
            materialContent.setText(currentMaterial.getContent());
        }
    }

    private void showError(String errorMessage) {
        progressBar.setVisibility(View.GONE);
        contentScrollView.setVisibility(View.GONE);
        emptyView.setVisibility(View.VISIBLE);
        emptyView.setText(errorMessage != null ? errorMessage : "Error loading materials");
        Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show();
    }

    /**
     * Initializes the TextToSpeech engine.
     * Checks for TTS engine availability first.
     */
    private void initializeTextToSpeech() {
        Log.d(TAG, "Initializing TextToSpeech...");

        // Check if TTS engines are available
        Intent checkIntent = new Intent();
        checkIntent.setAction(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA);

        // Try to initialize TTS
        try {
            textToSpeech = new TextToSpeech(this, this);
            Log.d(TAG, "TextToSpeech object created, waiting for onInit callback...");
        } catch (Exception e) {
            Log.e(TAG, "Exception creating TextToSpeech: " + e.getMessage(), e);
            ttsInitFailureReason = "Failed to create TTS engine: " + e.getMessage();
            isTtsInitialized = false;
            audioButton.setEnabled(false);
            audioButton.setAlpha(0.5f);
        }
    }

    @Override
    public void onInit(int status) {
        Log.d(TAG, "onInit callback called with status: " + status);

        if (status == TextToSpeech.SUCCESS) {
            Log.d(TAG, "TTS initialization successful, setting up language...");

            // Get available engines for debugging
            if (textToSpeech != null) {
                List<TextToSpeech.EngineInfo> engines = textToSpeech.getEngines();
                Log.d(TAG, "Available TTS engines: " + engines.size());
                for (TextToSpeech.EngineInfo engine : engines) {
                    Log.d(TAG, "  Engine: " + engine.name + " (label: " + engine.label + ")");
                }
                Log.d(TAG, "Current engine: " + textToSpeech.getDefaultEngine());
            }

            // Set language to US English
            int result = textToSpeech.setLanguage(Locale.US);
            Log.d(TAG, "setLanguage result: " + result);

            if (result == TextToSpeech.LANG_MISSING_DATA) {
                Log.e(TAG, "Language data missing for US English");
                ttsInitFailureReason = "US English language data not installed. Please install language data.";
                Toast.makeText(this, ttsInitFailureReason, Toast.LENGTH_LONG).show();
                isTtsInitialized = false;
                audioButton.setEnabled(false);
                audioButton.setAlpha(0.5f);
            } else if (result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e(TAG, "US English not supported by TTS engine");
                ttsInitFailureReason = "US English not supported by text-to-speech engine.";
                Toast.makeText(this, ttsInitFailureReason, Toast.LENGTH_LONG).show();
                isTtsInitialized = false;
                audioButton.setEnabled(false);
                audioButton.setAlpha(0.5f);
            } else {
                isTtsInitialized = true;
                setupTtsListener();
                audioButton.setEnabled(true);
                audioButton.setAlpha(1.0f);
                Log.d(TAG, "TextToSpeech initialized successfully with US English");
                Toast.makeText(this, "Audio ready", Toast.LENGTH_SHORT).show();
            }
        } else if (status == TextToSpeech.ERROR) {
            Log.e(TAG, "TTS initialization failed with ERROR status");
            ttsInitFailureReason = "Text-to-Speech engine failed to initialize. " +
                    "Please check if a TTS engine is installed on your device.";
            Toast.makeText(this, ttsInitFailureReason, Toast.LENGTH_LONG).show();
            isTtsInitialized = false;
            audioButton.setEnabled(false);
            audioButton.setAlpha(0.5f);
            showTtsInstallDialog();
        } else {
            Log.e(TAG, "TTS initialization failed with unknown status: " + status);
            ttsInitFailureReason = "Text-to-Speech initialization failed (status: " + status + ")";
            Toast.makeText(this, ttsInitFailureReason, Toast.LENGTH_LONG).show();
            isTtsInitialized = false;
            audioButton.setEnabled(false);
            audioButton.setAlpha(0.5f);
        }
    }

    /**
     * Sets up TTS utterance listener to track speaking state.
     */
    private void setupTtsListener() {
        textToSpeech.setOnUtteranceProgressListener(new UtteranceProgressListener() {
            @Override
            public void onStart(String utteranceId) {
                runOnUiThread(() -> {
                    isSpeaking = true;
                    updateAudioButtonIcon();
                });
            }

            @Override
            public void onDone(String utteranceId) {
                runOnUiThread(() -> {
                    isSpeaking = false;
                    updateAudioButtonIcon();
                });
            }

            @Override
            public void onError(String utteranceId) {
                runOnUiThread(() -> {
                    isSpeaking = false;
                    updateAudioButtonIcon();
                    Toast.makeText(MaterialListActivity.this,
                            "Error reading text", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    /**
     * Shows a dialog prompting the user to install a TTS engine.
     */
    private void showTtsInstallDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Text-to-Speech Not Available")
                .setMessage("No text-to-speech engine is installed on this device. " +
                        "Would you like to install Google Text-to-Speech?")
                .setPositiveButton("Install", (dialog, which) -> {
                    try {
                        Intent installIntent = new Intent(Intent.ACTION_VIEW);
                        installIntent.setData(Uri.parse(
                                "market://details?id=com.google.android.tts"));
                        startActivity(installIntent);
                    } catch (Exception e) {
                        Log.e(TAG, "Failed to open Play Store", e);
                        Toast.makeText(this, "Could not open Play Store",
                                Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * Toggles TextToSpeech playback - play if stopped, pause if speaking.
     */
    private void toggleTextToSpeech() {
        if (!isTtsInitialized) {
            String message = "Text-to-Speech not ready";
            if (ttsInitFailureReason != null) {
                message = ttsInitFailureReason;
            }
            Log.w(TAG, "TTS toggle attempted but not initialized: " + message);
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
            return;
        }

        if (isSpeaking) {
            // Stop speaking
            stopTextToSpeech();
        } else {
            // Start speaking
            startTextToSpeech();
        }
    }

    /**
     * Starts reading the content aloud.
     */
    private void startTextToSpeech() {
        Log.d(TAG, "startTextToSpeech called");

        Material currentMaterial = viewModel.getCurrentMaterial();
        if (currentMaterial == null || currentMaterial.getContent() == null ||
                currentMaterial.getContent().isEmpty()) {
            Log.w(TAG, "No content available to read");
            Toast.makeText(this, "No content to read", Toast.LENGTH_SHORT).show();
            return;
        }

        String content = currentMaterial.getContent();
        Log.d(TAG, "Content length: " + content.length() + " characters");

        // Check TTS state
        if (textToSpeech == null) {
            Log.e(TAG, "TextToSpeech object is null");
            Toast.makeText(this, "Text-to-Speech not initialized", Toast.LENGTH_SHORT).show();
            return;
        }

        // Stop any ongoing speech
        textToSpeech.stop();

        // Start speaking
        Log.d(TAG, "Calling textToSpeech.speak()...");
        int result = textToSpeech.speak(content, TextToSpeech.QUEUE_FLUSH, null, UTTERANCE_ID);
        Log.d(TAG, "textToSpeech.speak() returned: " + result);

        if (result == TextToSpeech.ERROR) {
            Log.e(TAG, "Error starting TextToSpeech - speak() returned ERROR");
            Toast.makeText(this, "Error starting audio. Check TTS engine.", Toast.LENGTH_LONG).show();
        } else if (result == TextToSpeech.SUCCESS) {
            Log.d(TAG, "TextToSpeech started successfully");
        } else {
            Log.w(TAG, "TextToSpeech speak() returned unexpected value: " + result);
        }
    }

    /**
     * Stops TextToSpeech playback.
     */
    private void stopTextToSpeech() {
        if (textToSpeech != null && textToSpeech.isSpeaking()) {
            textToSpeech.stop();
            isSpeaking = false;
            updateAudioButtonIcon();
            Log.d(TAG, "TextToSpeech stopped");
        }
    }

    /**
     * Updates the audio button icon based on speaking state.
     */
    private void updateAudioButtonIcon() {
        if (isSpeaking) {
            audioButton.setImageResource(R.drawable.ic_pause);
            audioButton.setContentDescription("Pause reading");
        } else {
            audioButton.setImageResource(R.drawable.ic_speaker);
            audioButton.setContentDescription("Read aloud");
        }
    }

    @Override
    protected void onDestroy() {
        // Shutdown TextToSpeech when activity is destroyed
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
            Log.d(TAG, "TextToSpeech shutdown");
        }
        super.onDestroy();
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Stop speaking when activity is paused
        if (isSpeaking) {
            stopTextToSpeech();
        }
    }
}
