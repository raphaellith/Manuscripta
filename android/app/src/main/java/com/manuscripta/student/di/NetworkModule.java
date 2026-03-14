package com.manuscripta.student.di;

import com.manuscripta.student.BuildConfig;
import com.manuscripta.student.network.ApiService;
import com.manuscripta.student.network.interceptor.AuthInterceptor;
import com.manuscripta.student.network.interceptor.BaseUrlInterceptor;
import com.manuscripta.student.network.interceptor.ErrorInterceptor;
import com.manuscripta.student.network.interceptor.LoggingInterceptor;
import com.manuscripta.student.network.interceptor.RetryInterceptor;
import com.manuscripta.student.network.tcp.PairingManager;
import com.manuscripta.student.utils.ConnectionManager;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.components.SingletonComponent;
import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Hilt module providing network dependencies.
 */
@Module
@InstallIn(SingletonComponent.class)
public class NetworkModule {

    /**
     * Placeholder base URL for Retrofit initialisation.
     * The actual server URL is dynamically resolved by {@link BaseUrlInterceptor}
     * using the server IP and HTTP port discovered during pairing.
     */
    private static final String PLACEHOLDER_BASE_URL = "http://localhost/";

    /**
     * Provides OkHttpClient with custom interceptors for retry, authentication,
     * logging, and error handling.
     * RetryInterceptor runs first so each retry attempt passes through the full chain.
     * LoggingInterceptor is only enabled in debug builds.
     * ErrorInterceptor always runs for consistent error handling; body logging
     * within it is gated behind debug builds.
     *
     * @param connectionManager The ConnectionManager used to check network availability
     *                          before making requests and retries
     * @param pairingManager The PairingManager used to retrieve the paired device ID,
     *                       which is included as the X-Device-ID header on every request.
     *                       Returns null before pairing completes, in which case no header
     *                       is added.
     * @return OkHttpClient instance configured with interceptors
     */
    @Provides
    @Singleton
    public OkHttpClient provideOkHttpClient(ConnectionManager connectionManager,
                                             PairingManager pairingManager) {
        AuthInterceptor.DeviceIdProvider deviceIdProvider = pairingManager::getDeviceId;
        BaseUrlInterceptor.ServerInfoProvider serverInfoProvider =
                new BaseUrlInterceptor.ServerInfoProvider() {
                    @Override public String getServerHost() {
                        return pairingManager.getServerHost();
                    }
                    @Override public int getServerHttpPort() {
                        return pairingManager.getServerHttpPort();
                    }
                };

        OkHttpClient.Builder builder = new OkHttpClient.Builder()
                .addInterceptor(new BaseUrlInterceptor(serverInfoProvider))
                .addInterceptor(new RetryInterceptor(connectionManager))
                .addInterceptor(new AuthInterceptor(deviceIdProvider));

        // Only add detailed request/response logging in debug builds
        if (BuildConfig.DEBUG) {
            builder.addInterceptor(new LoggingInterceptor());
        }

        // ErrorInterceptor always runs for consistent error handling across builds;
        // body logging inside it is gated behind BuildConfig.DEBUG
        builder.addInterceptor(new ErrorInterceptor());

        return builder.build();
    }

    /**
     * Provides Retrofit instance.
     *
     * @param okHttpClient OkHttpClient instance
     * @return Retrofit instance
     */
    @Provides
    @Singleton
    public Retrofit provideRetrofit(OkHttpClient okHttpClient) {
        return new Retrofit.Builder()
                .baseUrl(PLACEHOLDER_BASE_URL)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
    }

    /**
     * Provides ApiService instance.
     *
     * @param retrofit Retrofit instance
     * @return ApiService instance
     */
    @Provides
    @Singleton
    public ApiService provideApiService(Retrofit retrofit) {
        return retrofit.create(ApiService.class);
    }
}
