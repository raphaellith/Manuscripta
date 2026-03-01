package com.manuscripta.student;

import android.app.Application;
import dagger.hilt.android.HiltAndroidApp;
import javax.inject.Inject;
import com.manuscripta.student.network.tcp.HeartbeatManager;

/**
 * Application class for Manuscripta Student Client.
 * Annotated with @HiltAndroidApp to enable Hilt dependency injection.
 */
@HiltAndroidApp
public class ManuscriptaApplication extends Application {

    /** Eagerly injected to ensure HeartbeatManager registers as a TCP listener at startup. */
    @Inject
    HeartbeatManager heartbeatManager;

    @Override
    public void onCreate() {
        super.onCreate();
    }
}
