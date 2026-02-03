package com.manuscripta.student.data.repository;

import androidx.annotation.NonNull;

import com.manuscripta.student.domain.model.Configuration;

/**
 * Repository interface for managing tablet configuration.
 * Provides methods for fetching, storing, and retrieving configuration settings.
 *
 * <p>Configuration is fetched from the server via HTTP and persisted locally
 * using SharedPreferences for persistence across app restarts.</p>
 *
 * <p>Per API Contract §3.4, the REFRESH_CONFIG (0x03) TCP message triggers
 * a re-fetch of the configuration.</p>
 *
 * <p><b>Lifecycle Management:</b> Implementations should clean up resources
 * (e.g., unregister listeners) when the repository is no longer needed.
 * Call {@link #destroy()} to release resources.</p>
 */
public interface ConfigRepository {

    /**
     * Fetches configuration from the server and stores it locally.
     * This method should be called when a REFRESH_CONFIG TCP signal is received.
     *
     * @param deviceId The device ID to fetch configuration for
     * @throws Exception if the fetch operation fails
     */
    void fetchAndStoreConfig(@NonNull String deviceId) throws Exception;

    /**
     * Gets the current configuration from local storage.
     * Returns default configuration if no configuration has been stored.
     *
     * @return The current configuration, never null
     */
    @NonNull
    Configuration getConfig();

    /**
     * Clears the stored configuration, resetting to defaults.
     */
    void clearConfig();

    /**
     * Checks if a configuration has been stored locally.
     *
     * @return true if configuration exists, false otherwise
     */
    boolean hasStoredConfig();

    /**
     * Releases resources held by the repository.
     * Should be called when the repository is no longer needed (e.g., during
     * application shutdown or when the singleton scope is being destroyed).
     *
     * <p>After calling this method, the repository should not be used.</p>
     */
    void destroy();
}
