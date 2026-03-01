package com.manuscripta.student.di;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.manuscripta.student.BuildConfig;
import com.manuscripta.student.network.ApiService;
import com.manuscripta.student.network.interceptor.AuthInterceptor;
import com.manuscripta.student.network.interceptor.ErrorInterceptor;
import com.manuscripta.student.network.interceptor.LoggingInterceptor;
import com.manuscripta.student.network.interceptor.RetryInterceptor;
import com.manuscripta.student.network.tcp.PairingManager;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import retrofit2.Retrofit;

/**
 * Unit tests for NetworkModule.
 */
public class NetworkModuleTest {

    @Mock
    private PairingManager mockPairingManager;

    private NetworkModule networkModule;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        networkModule = new NetworkModule();
    }

    @Test
    public void testProvideOkHttpClient() {
        OkHttpClient client = networkModule.provideOkHttpClient(mockPairingManager);
        assertNotNull(client);
    }

    @Test
    public void testProvideOkHttpClient_hasInterceptors() {
        OkHttpClient client = networkModule.provideOkHttpClient(mockPairingManager);

        // Release builds have 3 interceptors (Retry, Auth, Error);
        // debug builds have 4 (Retry, Auth, Logging, Error)
        assertTrue("OkHttpClient should have at least 3 interceptors",
                   client.interceptors().size() >= 3);
    }

    @Test
    public void testProvideOkHttpClient_hasRetryInterceptor() {
        OkHttpClient client = networkModule.provideOkHttpClient(mockPairingManager);

        boolean hasRetryInterceptor = false;
        for (Interceptor interceptor : client.interceptors()) {
            if (interceptor instanceof RetryInterceptor) {
                hasRetryInterceptor = true;
                break;
            }
        }

        assertTrue("OkHttpClient should have RetryInterceptor", hasRetryInterceptor);
    }

    @Test
    public void testProvideOkHttpClient_hasAuthInterceptor() {
        OkHttpClient client = networkModule.provideOkHttpClient(mockPairingManager);

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
        OkHttpClient client = networkModule.provideOkHttpClient(mockPairingManager);

        boolean hasLoggingInterceptor = false;
        for (Interceptor interceptor : client.interceptors()) {
            if (interceptor instanceof LoggingInterceptor) {
                hasLoggingInterceptor = true;
                break;
            }
        }

        if (BuildConfig.DEBUG) {
            assertTrue("OkHttpClient should have LoggingInterceptor in debug builds", hasLoggingInterceptor);
        } else {
            assertFalse("OkHttpClient should not have LoggingInterceptor in release builds", hasLoggingInterceptor);
        }
    }

    @Test
    public void testProvideOkHttpClient_hasErrorInterceptor() {
        OkHttpClient client = networkModule.provideOkHttpClient(mockPairingManager);

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
        OkHttpClient client = networkModule.provideOkHttpClient(mockPairingManager);
        Retrofit retrofit = networkModule.provideRetrofit(client);
        assertNotNull(retrofit);
    }

    @Test
    public void testProvideApiService() {
        OkHttpClient client = networkModule.provideOkHttpClient(mockPairingManager);
        Retrofit retrofit = networkModule.provideRetrofit(client);
        ApiService apiService = networkModule.provideApiService(retrofit);
        assertNotNull(apiService);
    }

    @Test
    public void testProvideOkHttpClient_interceptorOrder() {
        OkHttpClient client = networkModule.provideOkHttpClient(mockPairingManager);

        // Order: Retry → Auth → [Logging (debug only)] → Error
        // Release: Retry, Auth, Error (size 3); Debug: Retry, Auth, Logging, Error (size 4)
        int size = client.interceptors().size();
        assertTrue("OkHttpClient should have at least 3 interceptors", size >= 3);

        assertTrue("First interceptor should be RetryInterceptor",
                   client.interceptors().get(0) instanceof RetryInterceptor);
        assertTrue("Second interceptor should be AuthInterceptor",
                   client.interceptors().get(1) instanceof AuthInterceptor);
        assertTrue("Last interceptor should be ErrorInterceptor",
                   client.interceptors().get(size - 1) instanceof ErrorInterceptor);

        if (size == 4) {
            assertTrue("Third interceptor should be LoggingInterceptor",
                       client.interceptors().get(2) instanceof LoggingInterceptor);
        }
    }
}
