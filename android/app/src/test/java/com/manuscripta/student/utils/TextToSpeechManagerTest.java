package com.manuscripta.student.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.speech.tts.TextToSpeech;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Locale;

/**
 * Unit tests for {@link TextToSpeechManager}.
 * Uses a mock TTS engine injected via the protected factory method
 * to verify behaviour without requiring a real TTS engine.
 */
public class TextToSpeechManagerTest {

    /** Mock Android TTS engine. */
    @Mock
    private TextToSpeech mockTts;

    /** Mock application context. */
    @Mock
    private Context mockContext;

    /** The manager under test, with overridden factory method. */
    private TextToSpeechManager manager;

    /** Captured TTS initialisation listener for simulating callbacks. */
    private TextToSpeech.OnInitListener capturedListener;

    /**
     * Sets up the test environment with a mocked TTS engine.
     */
    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        when(mockContext.getApplicationContext()).thenReturn(mockContext);
        manager = new TextToSpeechManager() {
            @Override
            protected TextToSpeech createTextToSpeech(
                    Context context,
                    TextToSpeech.OnInitListener listener) {
                capturedListener = listener;
                return mockTts;
            }
        };
    }

    /**
     * Tears down the test environment.
     */
    @After
    public void tearDown() {
        manager.shutdown();
    }

    // ---- init tests ----

    /**
     * Verifies that init throws when given a null context.
     */
    @Test
    public void testInit_nullContext_throwsException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> manager.init(null)
        );
        assertEquals("Context cannot be null", exception.getMessage());
    }

    /**
     * Verifies that a successful TTS initialisation makes the engine available.
     */
    @Test
    public void testInit_successfulInitialisation_engineAvailable() {
        when(mockTts.setLanguage(Locale.UK))
                .thenReturn(TextToSpeech.LANG_AVAILABLE);

        manager.init(mockContext);
        capturedListener.onInit(TextToSpeech.SUCCESS);

        assertTrue(manager.isAvailable());
    }

    /**
     * Verifies that a failed TTS initialisation leaves the engine unavailable.
     */
    @Test
    public void testInit_failedInitialisation_engineNotAvailable() {
        manager.init(mockContext);
        capturedListener.onInit(TextToSpeech.ERROR);

        assertFalse(manager.isAvailable());
    }

    /**
     * Verifies that LANG_NOT_SUPPORTED status leaves the engine unavailable.
     */
    @Test
    public void testInit_languageNotSupported_engineNotAvailable() {
        when(mockTts.setLanguage(Locale.UK))
                .thenReturn(TextToSpeech.LANG_NOT_SUPPORTED);

        manager.init(mockContext);
        capturedListener.onInit(TextToSpeech.SUCCESS);

        assertFalse(manager.isAvailable());
    }

    /**
     * Verifies that LANG_MISSING_DATA status leaves the engine unavailable.
     */
    @Test
    public void testInit_languageMissingData_engineNotAvailable() {
        when(mockTts.setLanguage(Locale.UK))
                .thenReturn(TextToSpeech.LANG_MISSING_DATA);

        manager.init(mockContext);
        capturedListener.onInit(TextToSpeech.SUCCESS);

        assertFalse(manager.isAvailable());
    }

    /**
     * Verifies that re-initialisation shuts down the previous engine.
     */
    @Test
    public void testInit_reinitialisation_shutsDownPreviousEngine() {
        when(mockTts.setLanguage(Locale.UK))
                .thenReturn(TextToSpeech.LANG_AVAILABLE);

        manager.init(mockContext);
        capturedListener.onInit(TextToSpeech.SUCCESS);
        assertTrue(manager.isAvailable());

        // Re-initialise — the old engine should be shut down
        manager.init(mockContext);

        verify(mockTts).shutdown();
    }

    /**
     * Verifies that language is set to UK English on successful init.
     */
    @Test
    public void testInit_setsLanguageToUkEnglish() {
        when(mockTts.setLanguage(Locale.UK))
                .thenReturn(TextToSpeech.LANG_AVAILABLE);

        manager.init(mockContext);
        capturedListener.onInit(TextToSpeech.SUCCESS);

        verify(mockTts).setLanguage(Locale.UK);
    }

    // ---- speak tests ----

    /**
     * Verifies that speak throws when given null text.
     */
    @Test
    public void testSpeak_nullText_throwsException() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> manager.speak(null)
        );
        assertEquals("Text cannot be null", exception.getMessage());
    }

    /**
     * Verifies that speak delegates to the TTS engine when enabled
     * and initialised.
     */
    @Test
    public void testSpeak_enabledAndInitialised_speaksText() {
        when(mockTts.setLanguage(Locale.UK))
                .thenReturn(TextToSpeech.LANG_AVAILABLE);

        manager.init(mockContext);
        capturedListener.onInit(TextToSpeech.SUCCESS);
        manager.setTtsEnabled(true);

        manager.speak("Hello world");

        verify(mockTts).speak(eq("Hello world"),
                eq(TextToSpeech.QUEUE_FLUSH), isNull(), isNull());
    }

    /**
     * Verifies that speak stops current speech before speaking new text.
     */
    @Test
    public void testSpeak_stopsCurrentSpeechBeforeSpeaking() {
        when(mockTts.setLanguage(Locale.UK))
                .thenReturn(TextToSpeech.LANG_AVAILABLE);

        manager.init(mockContext);
        capturedListener.onInit(TextToSpeech.SUCCESS);
        manager.setTtsEnabled(true);

        manager.speak("Hello");

        InOrder order = inOrder(mockTts);
        order.verify(mockTts).stop();
        order.verify(mockTts).speak(anyString(), anyInt(), any(), any());
    }

    /**
     * Verifies that speak is a no-op when TTS is disabled by teacher.
     */
    @Test
    public void testSpeak_disabledByTeacher_noOp() {
        when(mockTts.setLanguage(Locale.UK))
                .thenReturn(TextToSpeech.LANG_AVAILABLE);

        manager.init(mockContext);
        capturedListener.onInit(TextToSpeech.SUCCESS);
        manager.setTtsEnabled(false);

        manager.speak("Hello");

        verify(mockTts, never())
                .speak(anyString(), anyInt(), any(), any());
    }

    /**
     * Verifies that speak is a no-op when the engine is not initialised.
     */
    @Test
    public void testSpeak_notInitialised_noOp() {
        manager.setTtsEnabled(true);

        manager.speak("Hello");

        verify(mockTts, never())
                .speak(anyString(), anyInt(), any(), any());
    }

    /**
     * Verifies that speak is a no-op for empty text.
     */
    @Test
    public void testSpeak_emptyText_noOp() {
        when(mockTts.setLanguage(Locale.UK))
                .thenReturn(TextToSpeech.LANG_AVAILABLE);

        manager.init(mockContext);
        capturedListener.onInit(TextToSpeech.SUCCESS);
        manager.setTtsEnabled(true);

        manager.speak("");

        verify(mockTts, never())
                .speak(anyString(), anyInt(), any(), any());
    }

    /**
     * Verifies that speak is a no-op for blank (whitespace-only) text.
     */
    @Test
    public void testSpeak_blankText_noOp() {
        when(mockTts.setLanguage(Locale.UK))
                .thenReturn(TextToSpeech.LANG_AVAILABLE);

        manager.init(mockContext);
        capturedListener.onInit(TextToSpeech.SUCCESS);
        manager.setTtsEnabled(true);

        manager.speak("   ");

        verify(mockTts, never())
                .speak(anyString(), anyInt(), any(), any());
    }

    /**
     * Verifies that speak is a no-op after shutdown.
     */
    @Test
    public void testSpeak_afterShutdown_noOp() {
        when(mockTts.setLanguage(Locale.UK))
                .thenReturn(TextToSpeech.LANG_AVAILABLE);

        manager.init(mockContext);
        capturedListener.onInit(TextToSpeech.SUCCESS);
        manager.setTtsEnabled(true);
        manager.shutdown();

        manager.speak("Hello");

        // speak() on mockTts should never have been called
        // (stop/shutdown were called during shutdown, but not speak)
        verify(mockTts, never())
                .speak(anyString(), anyInt(), any(), any());
    }

    /**
     * Verifies that speak works with initialisation failure then re-init.
     */
    @Test
    public void testSpeak_afterReinitialisationSuccess_speaksText() {
        // First init fails
        manager.init(mockContext);
        capturedListener.onInit(TextToSpeech.ERROR);
        assertFalse(manager.isAvailable());

        // Second init succeeds
        when(mockTts.setLanguage(Locale.UK))
                .thenReturn(TextToSpeech.LANG_AVAILABLE);
        manager.init(mockContext);
        capturedListener.onInit(TextToSpeech.SUCCESS);
        manager.setTtsEnabled(true);

        manager.speak("After reinit");

        verify(mockTts).speak(eq("After reinit"),
                eq(TextToSpeech.QUEUE_FLUSH), isNull(), isNull());
    }

    // ---- stop tests ----

    /**
     * Verifies that stop delegates to the TTS engine.
     */
    @Test
    public void testStop_whenEngineExists_stopsPlayback() {
        when(mockTts.setLanguage(Locale.UK))
                .thenReturn(TextToSpeech.LANG_AVAILABLE);

        manager.init(mockContext);
        capturedListener.onInit(TextToSpeech.SUCCESS);

        manager.stop();

        verify(mockTts).stop();
    }

    /**
     * Verifies that stop does not throw when no engine exists.
     */
    @Test
    public void testStop_whenNoEngine_doesNotThrow() {
        manager.stop();
        // Should complete without exception
    }

    // ---- shutdown tests ----

    /**
     * Verifies that shutdown releases all TTS resources.
     */
    @Test
    public void testShutdown_releasesResources() {
        when(mockTts.setLanguage(Locale.UK))
                .thenReturn(TextToSpeech.LANG_AVAILABLE);

        manager.init(mockContext);
        capturedListener.onInit(TextToSpeech.SUCCESS);
        assertTrue(manager.isAvailable());

        manager.shutdown();

        assertFalse(manager.isAvailable());
        verify(mockTts).shutdown();
    }

    /**
     * Verifies that shutdown does not throw when no engine exists.
     */
    @Test
    public void testShutdown_whenNoEngine_doesNotThrow() {
        manager.shutdown();
        assertFalse(manager.isAvailable());
    }

    /**
     * Verifies that shutdown is idempotent.
     */
    @Test
    public void testShutdown_calledTwice_doesNotThrow() {
        when(mockTts.setLanguage(Locale.UK))
                .thenReturn(TextToSpeech.LANG_AVAILABLE);

        manager.init(mockContext);
        capturedListener.onInit(TextToSpeech.SUCCESS);

        manager.shutdown();
        manager.shutdown();

        assertFalse(manager.isAvailable());
    }

    // ---- isAvailable tests ----

    /**
     * Verifies that isAvailable returns false before init.
     */
    @Test
    public void testIsAvailable_beforeInit_returnsFalse() {
        assertFalse(manager.isAvailable());
    }

    /**
     * Verifies that isAvailable returns true after successful init.
     */
    @Test
    public void testIsAvailable_afterSuccessfulInit_returnsTrue() {
        when(mockTts.setLanguage(Locale.UK))
                .thenReturn(TextToSpeech.LANG_AVAILABLE);

        manager.init(mockContext);
        capturedListener.onInit(TextToSpeech.SUCCESS);

        assertTrue(manager.isAvailable());
    }

    /**
     * Verifies that isAvailable returns false after shutdown.
     */
    @Test
    public void testIsAvailable_afterShutdown_returnsFalse() {
        when(mockTts.setLanguage(Locale.UK))
                .thenReturn(TextToSpeech.LANG_AVAILABLE);

        manager.init(mockContext);
        capturedListener.onInit(TextToSpeech.SUCCESS);
        manager.shutdown();

        assertFalse(manager.isAvailable());
    }

    // ---- setTtsEnabled / isTtsEnabled tests ----

    /**
     * Verifies that setTtsEnabled(true) enables TTS.
     */
    @Test
    public void testSetTtsEnabled_true_enablesTts() {
        manager.setTtsEnabled(true);
        assertTrue(manager.isTtsEnabled());
    }

    /**
     * Verifies that setTtsEnabled(false) disables TTS.
     */
    @Test
    public void testSetTtsEnabled_false_disablesTts() {
        manager.setTtsEnabled(true);
        manager.setTtsEnabled(false);
        assertFalse(manager.isTtsEnabled());
    }

    /**
     * Verifies that disabling TTS stops any current speech.
     */
    @Test
    public void testSetTtsEnabled_disabling_stopsCurrentSpeech() {
        when(mockTts.setLanguage(Locale.UK))
                .thenReturn(TextToSpeech.LANG_AVAILABLE);

        manager.init(mockContext);
        capturedListener.onInit(TextToSpeech.SUCCESS);
        manager.setTtsEnabled(true);

        manager.setTtsEnabled(false);

        verify(mockTts).stop();
    }

    /**
     * Verifies that isTtsEnabled defaults to false.
     */
    @Test
    public void testIsTtsEnabled_defaultsFalse() {
        assertFalse(manager.isTtsEnabled());
    }

    /**
     * Verifies that enabling TTS does not stop speech.
     */
    @Test
    public void testSetTtsEnabled_enabling_doesNotStopSpeech() {
        when(mockTts.setLanguage(Locale.UK))
                .thenReturn(TextToSpeech.LANG_AVAILABLE);

        manager.init(mockContext);
        capturedListener.onInit(TextToSpeech.SUCCESS);

        manager.setTtsEnabled(true);

        verify(mockTts, never()).stop();
    }

    // ---- integration-style behaviour tests ----

    /**
     * Verifies the full lifecycle: init, enable, speak, shutdown.
     */
    @Test
    public void testFullLifecycle_initSpeakShutdown() {
        when(mockTts.setLanguage(Locale.UK))
                .thenReturn(TextToSpeech.LANG_AVAILABLE);

        // Init
        manager.init(mockContext);
        capturedListener.onInit(TextToSpeech.SUCCESS);
        assertTrue(manager.isAvailable());

        // Enable and speak
        manager.setTtsEnabled(true);
        manager.speak("Test speech");

        verify(mockTts).speak(eq("Test speech"),
                eq(TextToSpeech.QUEUE_FLUSH), isNull(), isNull());

        // Shutdown
        manager.shutdown();
        assertFalse(manager.isAvailable());
    }

    /**
     * Verifies that TTS enabled state defaults to false per ACC3A
     * (teacher must explicitly enable).
     */
    @Test
    public void testDefaultState_ttsDisabledPerAcc3a() {
        assertFalse(manager.isTtsEnabled());
        assertFalse(manager.isAvailable());
    }
}
