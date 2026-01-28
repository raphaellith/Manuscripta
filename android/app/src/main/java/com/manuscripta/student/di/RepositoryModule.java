package com.manuscripta.student.di;

import com.manuscripta.student.data.local.DeviceStatusDao;
import com.manuscripta.student.data.local.FeedbackDao;
import com.manuscripta.student.data.local.ManuscriptaDatabase;
import com.manuscripta.student.data.local.ResponseDao;
import com.manuscripta.student.data.local.SessionDao;
import com.manuscripta.student.data.repository.DeviceStatusRepository;
import com.manuscripta.student.data.repository.DeviceStatusRepositoryImpl;
import com.manuscripta.student.data.repository.FeedbackRepository;
import com.manuscripta.student.data.repository.FeedbackRepositoryImpl;
import com.manuscripta.student.data.repository.ResponseRepository;
import com.manuscripta.student.data.repository.ResponseRepositoryImpl;
import com.manuscripta.student.data.repository.SessionRepository;
import com.manuscripta.student.data.repository.SessionRepositoryImpl;
import com.manuscripta.student.network.ApiService;
import com.manuscripta.student.network.tcp.TcpSocketManager;

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
     * Provides the DeviceStatusDao from the database.
     *
     * @param database The ManuscriptaDatabase instance
     * @return DeviceStatusDao instance
     */
    @Provides
    @Singleton
    public DeviceStatusDao provideDeviceStatusDao(ManuscriptaDatabase database) {
        return database.deviceStatusDao();
    }

    /**
     * Provides the DeviceStatusRepository implementation.
     *
     * @param deviceStatusDao The DeviceStatusDao instance
     * @return DeviceStatusRepository instance
     */
    @Provides
    @Singleton
    public DeviceStatusRepository provideDeviceStatusRepository(DeviceStatusDao deviceStatusDao) {
        return new DeviceStatusRepositoryImpl(deviceStatusDao);
    }

    /**
     * Provides the FeedbackDao from the database.
     *
     * @param database The ManuscriptaDatabase instance
     * @return FeedbackDao instance
     */
    @Provides
    @Singleton
    public FeedbackDao provideFeedbackDao(ManuscriptaDatabase database) {
        return database.feedbackDao();
    }

    /**
     * Provides the FeedbackRepository implementation.
     *
     * @param feedbackDao      The FeedbackDao instance
     * @param apiService       The ApiService instance
     * @param tcpSocketManager The TcpSocketManager instance
     * @return FeedbackRepository instance
     */
    @Provides
    @Singleton
    public FeedbackRepository provideFeedbackRepository(FeedbackDao feedbackDao,
                                                        ApiService apiService,
                                                        TcpSocketManager tcpSocketManager) {
        return new FeedbackRepositoryImpl(feedbackDao, apiService, tcpSocketManager);
    }
}
