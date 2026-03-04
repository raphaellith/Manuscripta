package com.manuscripta.student.ui.main;

import static org.junit.Assert.assertNotNull;

import android.view.View;

import androidx.lifecycle.Lifecycle;
import androidx.test.core.app.ActivityScenario;

import com.manuscripta.student.R;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import dagger.hilt.android.testing.HiltAndroidRule;
import dagger.hilt.android.testing.HiltAndroidTest;
import dagger.hilt.android.testing.HiltTestApplication;

/**
 * Unit tests for {@link MainActivity}.
 */
@HiltAndroidTest
@RunWith(RobolectricTestRunner.class)
@Config(application = HiltTestApplication.class, sdk = 33)
public class MainActivityTest {

    /** Hilt rule for dependency injection in tests. */
    @Rule
    public HiltAndroidRule hiltRule = new HiltAndroidRule(this);

    /**
     * Verifies the activity launches and the tab bar is present.
     */
    @Test
    public void activityLaunches_andTabBarExists() {
        hiltRule.inject();

        try (ActivityScenario<MainActivity> scenario =
                 ActivityScenario.launch(MainActivity.class)) {

            scenario.moveToState(Lifecycle.State.CREATED);

            scenario.onActivity(activity -> {
                View tabBar = activity.findViewById(R.id.tabBar);
                assertNotNull("Tab bar should exist", tabBar);
            });
        }
    }
}