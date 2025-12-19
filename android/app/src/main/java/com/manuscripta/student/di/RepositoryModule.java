package com.manuscripta.student.di;

import android.content.Context;

import androidx.annotation.Nullable;

import com.manuscripta.student.data.local.ManuscriptaDatabase;
import com.manuscripta.student.data.local.MaterialDao;
import com.manuscripta.student.data.local.ResponseDao;
import com.manuscripta.student.data.local.SessionDao;
import com.manuscripta.student.data.repository.MaterialRepository;
import com.manuscripta.student.data.repository.MaterialRepositoryImpl;
import com.manuscripta.student.data.repository.ResponseRepository;
import com.manuscripta.student.data.repository.ResponseRepositoryImpl;
import com.manuscripta.student.data.repository.SessionRepository;
import com.manuscripta.student.data.repository.SessionRepositoryImpl;
import com.manuscripta.student.network.tcp.TcpSocketManager;
import com.manuscripta.student.utils.FileStorageManager;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.android.qualifiers.ApplicationContext;
import dagger.hilt.components.SingletonComponent;

/**
 * Hilt module providing repository dependencies.
 */
@Module
@InstallIn(SingletonComponent.class)
public class RepositoryModule {

    /**
     * Provides the SessionDao from the database.
     *
     * @param database The ManuscriptaDatabase instance
     * @return SessionDao instance
     */
    @Provides
    @Singleton
    public SessionDao provideSessionDao(ManuscriptaDatabase database) {
        return database.sessionDao();
    }

    /**
     * Provides the SessionRepository implementation.
     *
     * @param sessionDao The SessionDao instance
     * @return SessionRepository instance
     */
    @Provides
    @Singleton
    public SessionRepository provideSessionRepository(SessionDao sessionDao) {
        return new SessionRepositoryImpl(sessionDao);
    }

    /**
     * Provides the ResponseDao from the database.
     *
     * @param database The ManuscriptaDatabase instance
     * @return ResponseDao instance
     */
    @Provides
    @Singleton
    public ResponseDao provideResponseDao(ManuscriptaDatabase database) {
        return database.responseDao();
    }

    /**
     * Provides the ResponseRepository implementation.
     *
     * @param responseDao The ResponseDao instance
     * @return ResponseRepository instance
     */
    @Provides
    @Singleton
    public ResponseRepository provideResponseRepository(ResponseDao responseDao) {
        return new ResponseRepositoryImpl(responseDao);
    }

    /**
     * Provides the MaterialDao from the database.
     *
     * @param database The ManuscriptaDatabase instance
     * @return MaterialDao instance
     */
    @Provides
    @Singleton
    public MaterialDao provideMaterialDao(ManuscriptaDatabase database) {
        return database.materialDao();
    }

    /**
     * Provides the FileStorageManager for attachment file storage.
     *
     * @param context The application context
     * @return FileStorageManager instance
     */
    @Provides
    @Singleton
    public FileStorageManager provideFileStorageManager(@ApplicationContext Context context) {
        return new FileStorageManager(context);
    }

    /**
     * Provides the MaterialRepository implementation.
     *
     * @param materialDao        The MaterialDao instance
     * @param fileStorageManager The FileStorageManager instance
     * @param tcpSocketManager   The TcpSocketManager for sending ACKs (nullable)
     * @return MaterialRepository instance
     */
    @Provides
    @Singleton
    public MaterialRepository provideMaterialRepository(MaterialDao materialDao,
                                                        FileStorageManager fileStorageManager,
                                                        @Nullable TcpSocketManager tcpSocketManager) {
        return new MaterialRepositoryImpl(materialDao, fileStorageManager, tcpSocketManager);
    }
}
