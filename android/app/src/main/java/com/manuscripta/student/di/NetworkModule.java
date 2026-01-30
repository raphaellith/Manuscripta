package com.manuscripta.student.di;

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
     *
     * @return OkHttpClient instance configured with interceptors
     */
    @Provides
    @Singleton
    public OkHttpClient provideOkHttpClient() {
        // Create device ID provider (TODO: inject actual device ID provider)
        AuthInterceptor.DeviceIdProvider deviceIdProvider = () -> null;

        return new OkHttpClient.Builder()
                .addInterceptor(new AuthInterceptor(deviceIdProvider))
                .addInterceptor(new LoggingInterceptor())
                .addInterceptor(new ErrorInterceptor())
                .build();
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
