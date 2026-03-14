package com.manuscripta.student.di;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.manuscripta.student.BuildConfig;
import com.manuscripta.student.network.ApiService;
import com.manuscripta.student.network.interceptor.AuthInterceptor;
import com.manuscripta.student.network.interceptor.BaseUrlInterceptor;
import com.manuscripta.student.network.interceptor.ErrorInterceptor;
import com.manuscripta.student.network.interceptor.LoggingInterceptor;
import com.manuscripta.student.network.interceptor.RetryInterceptor;
import com.manuscripta.student.network.tcp.PairingManager;
import com.manuscripta.student.utils.ConnectionManager;

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
    private ConnectionManager mockConnectionManager;

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
        OkHttpClient client = networkModule.provideOkHttpClient(mockConnectionManager, mockPairingManager);
        assertNotNull(client);
    }

    @Test
    public void testProvideOkHttpClient_hasInterceptors() {
        OkHttpClient client = networkModule.provideOkHttpClient(mockConnectionManager, mockPairingManager);

        // Release builds have 4 interceptors (BaseUrl, Retry, Auth, Error);
        // debug builds have 5 (BaseUrl, Retry, Auth, Logging, Error)
        assertTrue("OkHttpClient should have at least 4 interceptors",
                   client.interceptors().size() >= 4);
    }

    @Test
    public void testProvideOkHttpClient_hasRetryInterceptor() {
        OkHttpClient client = networkModule.provideOkHttpClient(mockConnectionManager, mockPairingManager);

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
        OkHttpClient client = networkModule.provideOkHttpClient(mockConnectionManager, mockPairingManager);

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
        OkHttpClient client = networkModule.provideOkHttpClient(mockConnectionManager, mockPairingManager);

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
        OkHttpClient client = networkModule.provideOkHttpClient(mockConnectionManager, mockPairingManager);

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
        OkHttpClient client = networkModule.provideOkHttpClient(mockConnectionManager, mockPairingManager);
        Retrofit retrofit = networkModule.provideRetrofit(client);
        assertNotNull(retrofit);
    }

    @Test
    public void testProvideApiService() {
        OkHttpClient client = networkModule.provideOkHttpClient(mockConnectionManager, mockPairingManager);
        Retrofit retrofit = networkModule.provideRetrofit(client);
        ApiService apiService = networkModule.provideApiService(retrofit);
        assertNotNull(apiService);
    }

    @Test
    public void testProvideOkHttpClient_interceptorOrder() {
        OkHttpClient client = networkModule.provideOkHttpClient(mockConnectionManager, mockPairingManager);

        // Order: BaseUrl → Retry → Auth → [Logging (debug only)] → Error
        // Release: BaseUrl, Retry, Auth, Error (size 4);
        // Debug: BaseUrl, Retry, Auth, Logging, Error (size 5)
        int size = client.interceptors().size();
        assertTrue("OkHttpClient should have at least 4 interceptors", size >= 4);

        assertTrue("First interceptor should be BaseUrlInterceptor",
                   client.interceptors().get(0) instanceof BaseUrlInterceptor);
        assertTrue("Second interceptor should be RetryInterceptor",
                   client.interceptors().get(1) instanceof RetryInterceptor);
        assertTrue("Third interceptor should be AuthInterceptor",
                   client.interceptors().get(2) instanceof AuthInterceptor);
        assertTrue("Last interceptor should be ErrorInterceptor",
                   client.interceptors().get(size - 1) instanceof ErrorInterceptor);

        if (size == 5) {
            assertTrue("Fourth interceptor should be LoggingInterceptor",
                       client.interceptors().get(3) instanceof LoggingInterceptor);
        }
    }
}
