package com.manuscripta.student.di;

import android.content.Context;

import androidx.room.Room;

import com.manuscripta.student.data.local.ManuscriptaDatabase;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.android.qualifiers.ApplicationContext;
import dagger.hilt.components.SingletonComponent;

/**
 * Hilt module providing database dependencies.
 */
@Module
@InstallIn(SingletonComponent.class)
public class DatabaseModule {

    /**
     * Provides the Room database instance.
     *
     * @param context Application context
     * @return ManuscriptaDatabase instance
     */
    @Provides
    @Singleton
    public ManuscriptaDatabase provideDatabase(@ApplicationContext Context context) {
        return Room.databaseBuilder(
                context,
                ManuscriptaDatabase.class,
                "manuscripta_database"
        ).build();
    }
}
