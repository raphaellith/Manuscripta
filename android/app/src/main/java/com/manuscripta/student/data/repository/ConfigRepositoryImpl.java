package com.manuscripta.student.data.repository;

import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;

import com.manuscripta.student.data.model.FeedbackStyle;
import com.manuscripta.student.data.model.MascotSelection;
import com.manuscripta.student.domain.mapper.ConfigurationMapper;
import com.manuscripta.student.domain.model.Configuration;
import com.manuscripta.student.network.ApiService;
import com.manuscripta.student.network.dto.ConfigResponseDto;
import com.manuscripta.student.network.tcp.TcpMessage;
import com.manuscripta.student.network.tcp.TcpMessageListener;
import com.manuscripta.student.network.tcp.TcpSocketManager;
import com.manuscripta.student.network.tcp.ConnectionState;
import com.manuscripta.student.network.tcp.TcpProtocolException;
import com.manuscripta.student.network.tcp.message.RefreshConfigMessage;

import java.io.IOException;

import javax.inject.Inject;
import javax.inject.Singleton;

import retrofit2.Response;

/**
 * Implementation of {@link ConfigRepository}.
 * Handles the complete config fetch→store flow with SharedPreferences persistence.
 *
 * <p>This repository listens for REFRESH_CONFIG TCP messages and automatically
 * triggers a configuration refresh when received.</p>
 */
@Singleton
public class ConfigRepositoryImpl implements ConfigRepository, TcpMessageListener {

    /** Logging tag for this class. */
    private static final String TAG = "ConfigRepositoryImpl";

    /** SharedPreferences key for text size. */
    private static final String KEY_TEXT_SIZE = "config_text_size";
    /** SharedPreferences key for feedback style. */
    private static final String KEY_FEEDBACK_STYLE = "config_feedback_style";
    /** SharedPreferences key for TTS enabled. */
    private static final String KEY_TTS_ENABLED = "config_tts_enabled";
    /** SharedPreferences key for AI scaffolding enabled. */
    private static final String KEY_AI_SCAFFOLDING_ENABLED = "config_ai_scaffolding_enabled";
    /** SharedPreferences key for summarisation enabled. */
    private static final String KEY_SUMMARISATION_ENABLED = "config_summarisation_enabled";
    /** SharedPreferences key for mascot selection. */
    private static final String KEY_MASCOT_SELECTION = "config_mascot_selection";
    /** SharedPreferences key for config existence. */
    private static final String KEY_HAS_CONFIG = "config_has_stored";

    /** The SharedPreferences instance for persistence. */
    private final SharedPreferences preferences;
    /** The Retrofit API service for network calls. */
    private final ApiService apiService;
    /** The TCP socket manager for receiving REFRESH_CONFIG messages. */
    private final TcpSocketManager tcpSocketManager;
    /** The current device ID for config fetching. */
    private String deviceId;

    /**
     * Callback for when configuration refresh is triggered.
     */
    public interface ConfigRefreshCallback {
        /**
         * Called when configuration should be refreshed.
         *
         * @param deviceId The device ID to fetch configuration for
         */
        void onConfigRefreshRequested(@NonNull String deviceId);
    }

    /** Callback for refresh requests. */
    private ConfigRefreshCallback refreshCallback;

    /**
     * Creates a new ConfigRepositoryImpl.
     *
     * @param preferences      The SharedPreferences for persistence
     * @param apiService       The Retrofit API service
     * @param tcpSocketManager The TCP socket manager for receiving messages
     */
    @Inject
    public ConfigRepositoryImpl(@NonNull SharedPreferences preferences,
                                @NonNull ApiService apiService,
                                @NonNull TcpSocketManager tcpSocketManager) {
        this.preferences = preferences;
        this.apiService = apiService;
        this.tcpSocketManager = tcpSocketManager;
        this.tcpSocketManager.addMessageListener(this);
    }

    /**
     * Sets the device ID for configuration fetching.
     *
     * @param deviceId The device ID
     */
    public void setDeviceId(@NonNull String deviceId) {
        this.deviceId = deviceId;
    }

    /**
     * Sets the callback for configuration refresh requests.
     *
     * @param callback The callback to invoke when refresh is needed
     */
    public void setRefreshCallback(ConfigRefreshCallback callback) {
        this.refreshCallback = callback;
    }

    @Override
    public void fetchAndStoreConfig(@NonNull String deviceId) throws Exception {
        if (deviceId.trim().isEmpty()) {
            throw new IllegalArgumentException("Device ID cannot be empty");
        }

        // Fetch config from server via HTTP
        Response<ConfigResponseDto> response = apiService.getConfig(deviceId).execute();

        if (!response.isSuccessful()) {
            throw new IOException("Failed to fetch config: HTTP " + response.code());
        }

        ConfigResponseDto dto = response.body();
        if (dto == null) {
            throw new IOException("Config response body is null");
        }

        // Convert DTO to domain model with validation
        Configuration config = ConfigurationMapper.fromDto(dto);

        // Store in SharedPreferences
        storeConfig(config);

        Log.i(TAG, "Configuration fetched and stored successfully");
    }

    @Override
    @NonNull
    public Configuration getConfig() {
        if (!hasStoredConfig()) {
            return Configuration.createDefault();
        }

        int textSize = preferences.getInt(KEY_TEXT_SIZE, Configuration.DEFAULT_TEXT_SIZE);
        String feedbackStyleStr = preferences.getString(KEY_FEEDBACK_STYLE,
                Configuration.DEFAULT_FEEDBACK_STYLE.name());
        boolean ttsEnabled = preferences.getBoolean(KEY_TTS_ENABLED,
                Configuration.DEFAULT_TTS_ENABLED);
        boolean aiScaffoldingEnabled = preferences.getBoolean(KEY_AI_SCAFFOLDING_ENABLED,
                Configuration.DEFAULT_AI_SCAFFOLDING_ENABLED);
        boolean summarisationEnabled = preferences.getBoolean(KEY_SUMMARISATION_ENABLED,
                Configuration.DEFAULT_SUMMARISATION_ENABLED);
        String mascotSelectionStr = preferences.getString(KEY_MASCOT_SELECTION,
                Configuration.DEFAULT_MASCOT_SELECTION.name());

        FeedbackStyle feedbackStyle;
        try {
            feedbackStyle = FeedbackStyle.valueOf(feedbackStyleStr);
        } catch (IllegalArgumentException e) {
            feedbackStyle = Configuration.DEFAULT_FEEDBACK_STYLE;
        }

        MascotSelection mascotSelection;
        try {
            mascotSelection = MascotSelection.valueOf(mascotSelectionStr);
        } catch (IllegalArgumentException e) {
            mascotSelection = Configuration.DEFAULT_MASCOT_SELECTION;
        }

        return new Configuration(
                textSize,
                feedbackStyle,
                ttsEnabled,
                aiScaffoldingEnabled,
                summarisationEnabled,
                mascotSelection
        );
    }

    @Override
    public void clearConfig() {
        SharedPreferences.Editor editor = preferences.edit();
        editor.remove(KEY_TEXT_SIZE);
        editor.remove(KEY_FEEDBACK_STYLE);
        editor.remove(KEY_TTS_ENABLED);
        editor.remove(KEY_AI_SCAFFOLDING_ENABLED);
        editor.remove(KEY_SUMMARISATION_ENABLED);
        editor.remove(KEY_MASCOT_SELECTION);
        editor.remove(KEY_HAS_CONFIG);
        editor.apply();

        Log.i(TAG, "Configuration cleared");
    }

    @Override
    public boolean hasStoredConfig() {
        return preferences.getBoolean(KEY_HAS_CONFIG, false);
    }

    /**
     * Stores a Configuration to SharedPreferences.
     *
     * @param config The configuration to store
     */
    private void storeConfig(@NonNull Configuration config) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt(KEY_TEXT_SIZE, config.getTextSize());
        editor.putString(KEY_FEEDBACK_STYLE, config.getFeedbackStyle().name());
        editor.putBoolean(KEY_TTS_ENABLED, config.isTtsEnabled());
        editor.putBoolean(KEY_AI_SCAFFOLDING_ENABLED, config.isAiScaffoldingEnabled());
        editor.putBoolean(KEY_SUMMARISATION_ENABLED, config.isSummarisationEnabled());
        editor.putString(KEY_MASCOT_SELECTION, config.getMascotSelection().name());
        editor.putBoolean(KEY_HAS_CONFIG, true);
        editor.apply();
    }

    // ========== TcpMessageListener implementation ==========

    @Override
    public void onMessageReceived(@NonNull TcpMessage message) {
        if (message instanceof RefreshConfigMessage) {
            Log.d(TAG, "Received REFRESH_CONFIG signal");
            if (refreshCallback != null && deviceId != null) {
                refreshCallback.onConfigRefreshRequested(deviceId);
            }
        }
    }

    @Override
    public void onConnectionStateChanged(@NonNull ConnectionState state) {
        // Not handling connection state changes in this repository
    }

    @Override
    public void onError(@NonNull TcpProtocolException error) {
        Log.w(TAG, "TCP error: " + error.getMessage());
    }

    /**
     * Releases resources held by the repository.
     * Unregisters the TCP message listener to prevent memory leaks.
     */
    @Override
    public void destroy() {
        tcpSocketManager.removeMessageListener(this);
    }
}
