package com.manuscripta.student.ui.reading;

import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.manuscripta.student.R;

import java.util.Locale;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * Activity for displaying reading materials.
 * Shows the material content in a scrollable view optimized for e-ink.
 * Includes TextToSpeech functionality for accessibility.
 */
@AndroidEntryPoint
public class ReadingActivity extends AppCompatActivity implements TextToSpeech.OnInitListener {

    private static final String TAG = "ReadingActivity";
    private static final String UTTERANCE_ID = "MaterialContentTTS";

    private TextView titleView;
    private TextView contentView;
    private TextView helpText;
    private ImageView mascotIcon;
    private ImageButton audioButton;

    private TextToSpeech textToSpeech;
    private boolean isTtsInitialized = false;
    private boolean isSpeaking = false;
    private String currentContent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reading);

        setupViews();
        setupListeners();
        loadMaterial();
        initializeTextToSpeech();
    }

    private void setupViews() {
        titleView = findViewById(R.id.titleView);
        contentView = findViewById(R.id.contentView);
        helpText = findViewById(R.id.helpText);
        mascotIcon = findViewById(R.id.mascotIcon);
        audioButton = findViewById(R.id.audioButton);
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

    private void loadMaterial() {
        String materialId = getIntent().getStringExtra("MATERIAL_ID");
        String title = getIntent().getStringExtra("MATERIAL_TITLE");
        String content = getIntent().getStringExtra("MATERIAL_CONTENT");

        if (title != null) {
            titleView.setText(title);
        }

        if (content != null) {
            currentContent = content;
            contentView.setText(content);
        } else {
            currentContent = "No content available";
            contentView.setText(currentContent);
        }
    }

    /**
     * Initializes the TextToSpeech engine.
     */
    private void initializeTextToSpeech() {
        textToSpeech = new TextToSpeech(this, this);
    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            // Set language to US English
            int result = textToSpeech.setLanguage(Locale.US);

            if (result == TextToSpeech.LANG_MISSING_DATA ||
                    result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e(TAG, "Language not supported");
                Toast.makeText(this, "TTS language not supported", Toast.LENGTH_SHORT).show();
                isTtsInitialized = false;
            } else {
                isTtsInitialized = true;
                setupTtsListener();
                Log.d(TAG, "TextToSpeech initialized successfully");
            }
        } else {
            Log.e(TAG, "TextToSpeech initialization failed");
            Toast.makeText(this, "Text-to-Speech not available", Toast.LENGTH_SHORT).show();
            isTtsInitialized = false;
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
                    Toast.makeText(ReadingActivity.this,
                            "Error reading text", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    /**
     * Toggles TextToSpeech playback - play if stopped, pause if speaking.
     */
    private void toggleTextToSpeech() {
        if (!isTtsInitialized) {
            Toast.makeText(this, "Text-to-Speech not ready", Toast.LENGTH_SHORT).show();
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
        if (currentContent == null || currentContent.isEmpty()) {
            Toast.makeText(this, "No content to read", Toast.LENGTH_SHORT).show();
            return;
        }

        // Stop any ongoing speech
        textToSpeech.stop();

        // Start speaking
        int result = textToSpeech.speak(currentContent, TextToSpeech.QUEUE_FLUSH, null, UTTERANCE_ID);

        if (result == TextToSpeech.ERROR) {
            Log.e(TAG, "Error starting TextToSpeech");
            Toast.makeText(this, "Error starting audio", Toast.LENGTH_SHORT).show();
        } else {
            Log.d(TAG, "TextToSpeech started");
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
