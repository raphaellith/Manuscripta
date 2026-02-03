package com.manuscripta.student.di;

import com.manuscripta.student.BuildConfig;
import com.manuscripta.student.network.ApiService;
import com.manuscripta.student.network.interceptor.AuthInterceptor;
import com.manuscripta.student.network.interceptor.ErrorInterceptor;
import com.manuscripta.student.network.interceptor.LoggingInterceptor;

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
     * Base URL for the Manuscripta API.
     */
    private static final String BASE_URL = "https://api.manuscripta.example.com/";

    /**
     * Provides OkHttpClient with custom interceptors for authentication,
     * logging, and error handling.
     * LoggingInterceptor is only enabled in debug builds.
     * ErrorInterceptor always runs for consistent error handling; body logging
     * within it is gated behind debug builds.
     *
     * NOTE: AuthInterceptor currently uses a stub DeviceIdProvider that returns null.
     * To enable device identification headers, inject a real DeviceIdProvider implementation
     * that retrieves the actual device ID (e.g., from Android Settings.Secure.ANDROID_ID
     * or a UUID stored in SharedPreferences).
     *
     * @return OkHttpClient instance configured with interceptors
     */
    @Provides
    @Singleton
    public OkHttpClient provideOkHttpClient() {
        // Stub device ID provider - replace with real implementation when device ID is available
        // Example: () -> Settings.Secure.getString(context.getContentResolver(),
        //                                           Settings.Secure.ANDROID_ID)
        AuthInterceptor.DeviceIdProvider deviceIdProvider = () -> null;

        OkHttpClient.Builder builder = new OkHttpClient.Builder()
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
                .baseUrl(BASE_URL)
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
