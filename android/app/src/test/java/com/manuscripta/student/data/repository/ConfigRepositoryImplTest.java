package com.manuscripta.student.data.repository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.SharedPreferences;

import com.manuscripta.student.data.model.FeedbackStyle;
import com.manuscripta.student.data.model.MascotSelection;
import com.manuscripta.student.domain.model.Configuration;
import com.manuscripta.student.network.ApiService;
import com.manuscripta.student.network.dto.ConfigResponseDto;
import com.manuscripta.student.network.tcp.TcpSocketManager;
import com.manuscripta.student.network.tcp.message.RefreshConfigMessage;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;

import retrofit2.Call;
import retrofit2.Response;

public class ConfigRepositoryImplTest {

    private static final String TEST_DEVICE_ID = "device-123";

    @Mock
    private SharedPreferences preferences;
    @Mock
    private SharedPreferences.Editor editor;
    @Mock
    private ApiService apiService;
    @Mock
    private TcpSocketManager tcpSocketManager;
    @Mock
    private Call<ConfigResponseDto> call;

    private ConfigRepositoryImpl repository;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        when(preferences.edit()).thenReturn(editor);
        when(editor.putInt(anyString(), anyInt())).thenReturn(editor);
        when(editor.putString(anyString(), anyString())).thenReturn(editor);
        when(editor.putBoolean(anyString(), anyBoolean())).thenReturn(editor);

        repository = new ConfigRepositoryImpl(preferences, apiService, tcpSocketManager);
    }

    @Test
    public void testFetchAndStoreConfigSuccess() throws Exception {
        ConfigResponseDto dto = new ConfigResponseDto(20, "NEUTRAL", true, false, true, "NONE");
        when(apiService.getConfig(TEST_DEVICE_ID)).thenReturn(call);
        when(call.execute()).thenReturn(Response.success(dto));

        repository.fetchAndStoreConfig(TEST_DEVICE_ID);

        verify(editor).putInt("config_text_size", 20);
        verify(editor).putString("config_feedback_style", "NEUTRAL");
        verify(editor).putBoolean("config_tts_enabled", true);
        verify(editor).putBoolean("config_ai_scaffolding_enabled", false);
        verify(editor).putBoolean("config_summarisation_enabled", true);
        verify(editor).putString("config_mascot_selection", "NONE");
        verify(editor).putBoolean("config_has_stored", true);
        verify(editor).apply();
    }

    @Test(expected = IOException.class)
    public void testFetchAndStoreConfigApiError() throws Exception {
        when(apiService.getConfig(TEST_DEVICE_ID)).thenReturn(call);
        when(call.execute()).thenReturn(Response.error(500, okhttp3.ResponseBody.create(null, "")));

        repository.fetchAndStoreConfig(TEST_DEVICE_ID);
    }

    @Test(expected = IOException.class)
    public void testFetchAndStoreConfigNullBody() throws Exception {
        when(apiService.getConfig(TEST_DEVICE_ID)).thenReturn(call);
        when(call.execute()).thenReturn(Response.success(null));

        repository.fetchAndStoreConfig(TEST_DEVICE_ID);
    }

    @Test
    public void testGetConfigStored() {
        when(preferences.getBoolean("config_has_stored", false)).thenReturn(true);
        when(preferences.getInt(eq("config_text_size"), anyInt())).thenReturn(30);
        when(preferences.getString(eq("config_feedback_style"), anyString())).thenReturn("IMMEDIATE");
        when(preferences.getBoolean(eq("config_tts_enabled"), anyBoolean())).thenReturn(false);
        when(preferences.getBoolean(eq("config_ai_scaffolding_enabled"), anyBoolean())).thenReturn(true);
        when(preferences.getBoolean(eq("config_summarisation_enabled"), anyBoolean())).thenReturn(false);
        when(preferences.getString(eq("config_mascot_selection"), anyString())).thenReturn("MASCOT3");

        Configuration config = repository.getConfig();

        assertNotNull(config);
        assertEquals(30, config.getTextSize());
        assertEquals(FeedbackStyle.IMMEDIATE, config.getFeedbackStyle());
        assertFalse(config.isTtsEnabled());
        assertTrue(config.isAiScaffoldingEnabled());
        assertFalse(config.isSummarisationEnabled());
        assertEquals(MascotSelection.MASCOT3, config.getMascotSelection());
    }

    @Test
    public void testGetConfigDefault() {
        when(preferences.getBoolean("config_has_stored", false)).thenReturn(false);

        Configuration config = repository.getConfig();

        assertNotNull(config);
        // Default values
        assertEquals(Configuration.DEFAULT_TEXT_SIZE, config.getTextSize());
    }

    @Test
    public void testClearConfig() {
        repository.clearConfig();

        verify(editor).remove("config_text_size");
        verify(editor).remove("config_feedback_style");
        verify(editor).remove("config_tts_enabled");
        verify(editor).remove("config_ai_scaffolding_enabled");
        verify(editor).remove("config_summarisation_enabled");
        verify(editor).remove("config_mascot_selection");
        verify(editor).remove("config_has_stored");
        verify(editor).apply();
    }

    @Test
    public void testHasStoredConfig() {
        when(preferences.getBoolean("config_has_stored", false)).thenReturn(true);
        assertTrue(repository.hasStoredConfig());

        when(preferences.getBoolean("config_has_stored", false)).thenReturn(false);
        assertFalse(repository.hasStoredConfig());
    }

    @Test
    public void testOnMessageReceived() {
        ConfigRepository.ConfigRefreshCallback callback = mock(ConfigRepository.ConfigRefreshCallback.class);
        repository.setRefreshCallback(callback);
        repository.setDeviceId(TEST_DEVICE_ID);

        repository.onMessageReceived(new RefreshConfigMessage());

        verify(callback).onConfigRefreshRequested(TEST_DEVICE_ID);
    }

    @Test
    public void testDestroy() {
        repository.destroy();
        verify(tcpSocketManager).removeMessageListener(repository);
    }

    @Test(expected = IllegalStateException.class)
    public void testFetchAfterDestroy() throws Exception {
        repository.destroy();
        repository.fetchAndStoreConfig(TEST_DEVICE_ID);
    }

    @Test(expected = IllegalStateException.class)
    public void testGetConfigAfterDestroy() {
        repository.destroy();
        repository.getConfig();
    }

    @Test(expected = IllegalStateException.class)
    public void testClearConfigAfterDestroy() {
        repository.destroy();
        repository.clearConfig();
    }

    @Test(expected = IllegalStateException.class)
    public void testHasStoredConfigAfterDestroy() {
        repository.destroy();
        repository.hasStoredConfig();
    }
}
