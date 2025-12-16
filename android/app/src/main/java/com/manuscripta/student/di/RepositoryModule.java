package com.manuscripta.student.di;

import com.manuscripta.student.data.local.ManuscriptaDatabase;
import com.manuscripta.student.data.local.SessionDao;
import com.manuscripta.student.data.repository.SessionRepository;
import com.manuscripta.student.data.repository.SessionRepositoryImpl;
import com.manuscripta.student.data.local.ResponseDao;
import com.manuscripta.student.data.repository.ResponseRepository;
import com.manuscripta.student.data.repository.ResponseRepositoryImpl;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
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
}
