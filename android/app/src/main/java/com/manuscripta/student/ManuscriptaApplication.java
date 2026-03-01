package com.manuscripta.student;

import android.app.Application;
import com.manuscripta.student.utils.ConnectionManager;
import dagger.hilt.android.HiltAndroidApp;

import javax.inject.Inject;

/**
 * Application class for Manuscripta Student Client.
 * Annotated with @HiltAndroidApp to enable Hilt dependency injection.
 */
@HiltAndroidApp
public class ManuscriptaApplication extends Application {

    @Inject
    ConnectionManager connectionManager;

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
