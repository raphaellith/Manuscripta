package com.manuscripta.student.di;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.manuscripta.student.network.ApiService;
import com.manuscripta.student.network.interceptor.AuthInterceptor;
import com.manuscripta.student.network.interceptor.ErrorInterceptor;
import com.manuscripta.student.network.interceptor.LoggingInterceptor;

import org.junit.Before;
import org.junit.Test;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import retrofit2.Retrofit;

/**
 * Unit tests for NetworkModule.
 */
public class NetworkModuleTest {

    private NetworkModule networkModule;

    @Before
    public void setUp() {
        networkModule = new NetworkModule();
    }

    @Test
    public void testProvideOkHttpClient() {
        OkHttpClient client = networkModule.provideOkHttpClient();
        assertNotNull(client);
    }

    @Test
    public void testProvideOkHttpClient_hasInterceptors() {
        OkHttpClient client = networkModule.provideOkHttpClient();

        // Should have exactly 3 interceptors
        assertEquals(3, client.interceptors().size());
    }

    @Test
    public void testProvideOkHttpClient_hasAuthInterceptor() {
        OkHttpClient client = networkModule.provideOkHttpClient();

        boolean hasAuthInterceptor = false;
        for (Interceptor interceptor : client.interceptors()) {
            if (interceptor instanceof AuthInterceptor) {
                hasAuthInterceptor = true;
                break;
            }
        }

        assertTrue("OkHttpClient should have AuthInterceptor", hasAuthInterceptor);
    }

    @Test
    public void testProvideOkHttpClient_hasLoggingInterceptor() {
        OkHttpClient client = networkModule.provideOkHttpClient();

        boolean hasLoggingInterceptor = false;
        for (Interceptor interceptor : client.interceptors()) {
            if (interceptor instanceof LoggingInterceptor) {
                hasLoggingInterceptor = true;
                break;
            }
        }

        assertTrue("OkHttpClient should have LoggingInterceptor", hasLoggingInterceptor);
    }

    @Test
    public void testProvideOkHttpClient_hasErrorInterceptor() {
        OkHttpClient client = networkModule.provideOkHttpClient();

        boolean hasErrorInterceptor = false;
        for (Interceptor interceptor : client.interceptors()) {
            if (interceptor instanceof ErrorInterceptor) {
                hasErrorInterceptor = true;
                break;
            }
        }

        assertTrue("OkHttpClient should have ErrorInterceptor", hasErrorInterceptor);
    }

    @Test
    public void testProvideRetrofit() {
        OkHttpClient client = networkModule.provideOkHttpClient();
        Retrofit retrofit = networkModule.provideRetrofit(client);
        assertNotNull(retrofit);
    }

    @Test
    public void testProvideApiService() {
        OkHttpClient client = networkModule.provideOkHttpClient();
        Retrofit retrofit = networkModule.provideRetrofit(client);
        ApiService apiService = networkModule.provideApiService(retrofit);
        assertNotNull(apiService);
    }
}
