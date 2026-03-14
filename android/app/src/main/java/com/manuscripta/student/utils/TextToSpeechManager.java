package com.manuscripta.student.utils;

import android.content.Context;
import android.speech.tts.TextToSpeech;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Locale;

/**
 * Manages text-to-speech functionality for the Manuscripta student client.
 *
 * <p>Wraps the Android {@link TextToSpeech} API, providing simplified methods
 * for speaking text aloud with English (UK) language support. Respects the
 * teacher-controlled TTS toggle per ACC3A — when TTS is disabled via
 * {@link #setTtsEnabled(boolean)}, {@link #speak(String)} becomes a no-op.</p>
 *
 * <p>Usage:</p>
 * <pre>
 * TextToSpeechManager ttsManager = new TextToSpeechManager();
 * ttsManager.init(context);
 * ttsManager.setTtsEnabled(configuration.isTtsEnabled());
 * ttsManager.speak("Hello, world!");
 * // Later...
 * ttsManager.shutdown();
 * </pre>
 */
public class TextToSpeechManager {

    /** The underlying Android TTS engine instance. */
    @Nullable
    private TextToSpeech textToSpeech;

    /** Whether the TTS engine has been successfully initialised. */
    private boolean initialised;

    /** Whether TTS is enabled by teacher configuration (ACC3A). */
    private boolean ttsEnabled;

    /**
     * Initialises the TTS engine with English (UK) language.
     * Any existing TTS instance will be shut down first.
     *
     * @param context The application context (must not be null)
     * @throws IllegalArgumentException if context is null
     */
    public void init(@NonNull Context context) {
        if (context == null) {
            throw new IllegalArgumentException("Context cannot be null");
        }
        shutdown();
        initialised = false;
        textToSpeech = createTextToSpeech(
                context.getApplicationContext(), this::onTtsInitialised);
    }

    /**
     * Speaks the given text aloud, stopping any currently playing speech.
     * This is a no-op if TTS is disabled by teacher configuration,
     * if the engine is not initialised, or if the text is empty.
     *
     * @param text The text to speak (must not be null)
     * @throws IllegalArgumentException if text is null
     */
    public void speak(@NonNull String text) {
        if (text == null) {
            throw new IllegalArgumentException("Text cannot be null");
        }
        if (text.trim().isEmpty()) {
            return;
        }
        if (!ttsEnabled || !initialised || textToSpeech == null) {
            return;
        }
        textToSpeech.stop();
        textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
    }

    /**
     * Stops any currently playing speech.
     */
    public void stop() {
        if (textToSpeech != null) {
            textToSpeech.stop();
        }
    }

    /**
     * Shuts down the TTS engine and releases all resources.
     * After calling this method, {@link #init(Context)} must be called
     * again before TTS can be used.
     */
    public void shutdown() {
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
            textToSpeech = null;
        }
        initialised = false;
    }

    /**
     * Checks whether the TTS engine is initialised and available for use.
     *
     * @return true if the TTS engine is ready, false otherwise
     */
    public boolean isAvailable() {
        return initialised && textToSpeech != null;
    }

    /**
     * Sets whether TTS is enabled based on teacher configuration.
     * When disabled, any ongoing speech is stopped and subsequent
     * calls to {@link #speak(String)} become no-ops.
     *
     * @param enabled true to enable TTS, false to disable
     */
    public void setTtsEnabled(boolean enabled) {
        this.ttsEnabled = enabled;
        if (!enabled) {
            stop();
        }
    }

    /**
     * Gets whether TTS is currently enabled.
     *
     * @return true if TTS is enabled
     */
    public boolean isTtsEnabled() {
        return ttsEnabled;
    }

    /**
     * Handles TTS engine initialisation callback. Sets the language to
     * English (UK) on success and updates the initialisation state.
     *
     * @param status The initialisation status from the TTS engine
     */
    private void onTtsInitialised(int status) {
        if (status == TextToSpeech.SUCCESS && textToSpeech != null) {
            int result = textToSpeech.setLanguage(Locale.UK);
            initialised = result != TextToSpeech.LANG_MISSING_DATA
                    && result != TextToSpeech.LANG_NOT_SUPPORTED;
        }
    }

    /**
     * Creates a new {@link TextToSpeech} instance. This method is protected
     * to allow subclassing in tests for injecting mock TTS engines.
     *
     * @param context  The application context
     * @param listener The initialisation listener
     * @return A new TextToSpeech instance
     */
    protected TextToSpeech createTextToSpeech(
            @NonNull Context context,
            @NonNull TextToSpeech.OnInitListener listener) {
        return new TextToSpeech(context, listener);
    }
}
