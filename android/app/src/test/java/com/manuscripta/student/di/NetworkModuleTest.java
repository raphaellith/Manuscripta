package com.manuscripta.student.di;

import static org.junit.Assert.assertNotNull;

import com.manuscripta.student.network.ApiService;

import org.junit.Before;
import org.junit.Test;

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
