package com.manuscripta.student.di;

import static org.junit.Assert.assertNotNull;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import com.manuscripta.student.data.local.ManuscriptaDatabase;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

/**
 * Unit tests for DatabaseModule.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public class DatabaseModuleTest {

    private DatabaseModule databaseModule;

    @Before
    public void setUp() {
        databaseModule = new DatabaseModule();
    }

    @Test
    public void testConstructor() {
        assertNotNull(new DatabaseModule());
    }

    @Test
    public void testProvideDatabase() {
        Context context = ApplicationProvider.getApplicationContext();
        ManuscriptaDatabase database = databaseModule.provideDatabase(context);
        assertNotNull(database);
    }
}
