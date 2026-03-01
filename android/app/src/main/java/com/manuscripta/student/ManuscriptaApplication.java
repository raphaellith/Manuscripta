package com.manuscripta.student;

import android.app.Application;
import com.manuscripta.student.utils.ConnectionManager;
import com.manuscripta.student.network.tcp.HeartbeatManager;
import dagger.hilt.android.HiltAndroidApp;
import javax.inject.Inject;

/**
 * Application class for Manuscripta Student Client.
 * Annotated with @HiltAndroidApp to enable Hilt dependency injection.
 */
@HiltAndroidApp
public class ManuscriptaApplication extends Application {

    /** Eagerly injected to ensure HeartbeatManager registers as a TCP listener at startup. */
    @Inject
    // CHECKSTYLE:OFF VisibilityModifier - Required for Hilt/Dagger field injection
    HeartbeatManager heartbeatManager;
    // CHECKSTYLE:ON

    /**
     * The connection manager for monitoring network connectivity.
     * Package-private for Hilt field injection (Dagger does not support private field injection).
     */
    @Inject
    // CHECKSTYLE:OFF VisibilityModifier - Required for Hilt/Dagger field injection
    ConnectionManager connectionManager;
    // CHECKSTYLE:ON

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        connectionManager.shutdown();
    }
}
