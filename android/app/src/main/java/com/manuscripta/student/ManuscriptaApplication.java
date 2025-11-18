package com.manuscripta.student;

import android.app.Application;
import dagger.hilt.android.HiltAndroidApp;

/**
 * Application class for Manuscripta Student Client.
 * Annotated with @HiltAndroidApp to enable Hilt dependency injection.
 */
@HiltAndroidApp
public class ManuscriptaApplication extends Application {
    
    @Override
    public void onCreate() {
        super.onCreate();
    }
}
