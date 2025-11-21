package com.manuscripta.student.ui.main;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import android.widget.TextView;

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

@HiltAndroidTest
@RunWith(RobolectricTestRunner.class)
@Config(application = HiltTestApplication.class, sdk = 33)
public class MainActivityTest {

    @Rule
    public HiltAndroidRule hiltRule = new HiltAndroidRule(this);

    @Test
    public void activityLaunches_andSetsTextCorrectly() {
        hiltRule.inject();

        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {

            scenario.moveToState(Lifecycle.State.CREATED);

            scenario.onActivity(activity -> {
                TextView textView = activity.findViewById(R.id.textView);

                assertNotNull("TextView should exist", textView);
                assertEquals("Hello, Manuscripta!", textView.getText().toString());
            });
        }
    }
}