package com.manuscripta.student.di;

import android.content.Context;

import com.manuscripta.student.data.local.DeviceStatusDao;
import com.manuscripta.student.data.local.FeedbackDao;
import com.manuscripta.student.data.local.ManuscriptaDatabase;
import com.manuscripta.student.data.local.MaterialDao;
import com.manuscripta.student.data.local.ResponseDao;
import com.manuscripta.student.data.local.SessionDao;
import com.manuscripta.student.data.repository.DeviceStatusRepository;
import com.manuscripta.student.data.repository.DeviceStatusRepositoryImpl;
import com.manuscripta.student.data.repository.FeedbackRepository;
import com.manuscripta.student.data.repository.FeedbackRepositoryImpl;
import com.manuscripta.student.data.repository.MaterialRepository;
import com.manuscripta.student.data.repository.MaterialRepositoryImpl;
import com.manuscripta.student.data.repository.ResponseRepository;
import com.manuscripta.student.data.repository.ResponseRepositoryImpl;
import com.manuscripta.student.data.repository.SessionRepository;
import com.manuscripta.student.data.repository.SessionRepositoryImpl;
import com.manuscripta.student.network.ApiService;
import com.manuscripta.student.network.tcp.PairingManager;
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
     * @param apiService  The ApiService instance for network sync
     * @return ResponseRepository instance
     */
    @Provides
    @Singleton
    public ResponseRepository provideResponseRepository(ResponseDao responseDao,
                                                         ApiService apiService) {
        return new ResponseRepositoryImpl(responseDao, apiService);
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
     * @param apiService         The ApiService instance
     * @param tcpSocketManager   The TcpSocketManager instance
     * @param pairingManager     The PairingManager instance
     * @return MaterialRepository instance
     */
    @Provides
    @Singleton
    public MaterialRepository provideMaterialRepository(MaterialDao materialDao,
                                                        FileStorageManager fileStorageManager,
                                                        ApiService apiService,
                                                        TcpSocketManager tcpSocketManager,
                                                        PairingManager pairingManager) {
        return new MaterialRepositoryImpl(materialDao, fileStorageManager, apiService,
                tcpSocketManager, pairingManager);
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
}
