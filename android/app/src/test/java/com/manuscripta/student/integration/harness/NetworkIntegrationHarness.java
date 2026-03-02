package com.manuscripta.student.integration.harness;

import androidx.annotation.NonNull;
import androidx.test.core.app.ApplicationProvider;

import com.manuscripta.student.integration.config.IntegrationTestConfig;
import com.manuscripta.student.network.ApiService;
import com.manuscripta.student.network.interceptor.AuthInterceptor;
import com.manuscripta.student.network.interceptor.ErrorInterceptor;
import com.manuscripta.student.network.interceptor.RetryInterceptor;
import com.manuscripta.student.network.tcp.AckRetrySender;
import com.manuscripta.student.network.tcp.HeartbeatConfig;
import com.manuscripta.student.network.tcp.HeartbeatManager;
import com.manuscripta.student.network.tcp.PairingConfig;
import com.manuscripta.student.network.tcp.PairingManager;
import com.manuscripta.student.network.tcp.TcpMessageDecoder;
import com.manuscripta.student.network.tcp.TcpMessageEncoder;
import com.manuscripta.student.network.tcp.TcpSocketManager;
import com.manuscripta.student.utils.ConnectionManager;

import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Builds and holds the real production networking objects wired with
 * {@link IntegrationTestConfig}.
 *
 * <p>Each test class instantiates this in {@code @Before} and tears it
 * down in {@code @After}. The harness mirrors what the Hilt DI graph
 * creates in production, substituting only the base URL and device-ID
 * provider.</p>
 *
 * <p>Requires Robolectric for {@link ConnectionManager} (needs
 * {@code Context}) and {@link TcpSocketManager} (needs
 * {@code Handler(Looper.getMainLooper())}).</p>
 */
public class NetworkIntegrationHarness {

    /** Fast pairing timeout for tests (3 seconds). */
    private static final long FAST_PAIRING_TIMEOUT_MS = 3000L;

    /** Single retry for pairing in tests. */
    private static final int FAST_PAIRING_RETRIES = 1;

    /** Fast heartbeat interval for tests (1 second). */
    private static final long FAST_HEARTBEAT_INTERVAL_MS = 1000L;

    private final IntegrationTestConfig config;

    private OkHttpClient okHttpClient;
    private Retrofit retrofit;
    private ApiService apiService;
    private TcpMessageEncoder encoder;
    private TcpMessageDecoder decoder;
    private TcpSocketManager tcpSocketManager;
    private PairingManager pairingManager;
    private HeartbeatManager heartbeatManager;
    private AckRetrySender ackRetrySender;
    private ConnectionManager connectionManager;

    /**
     * Creates a harness bound to the given config.
     *
     * @param config connection parameters for the Windows server
     */
    public NetworkIntegrationHarness(
            @NonNull IntegrationTestConfig config) {
        this.config = config;
    }

    /**
     * Constructs all production networking objects. Call from
     * {@code @Before}.
     */
    public void setUp() {
        // --- ConnectionManager (Robolectric provides Context) ---
        connectionManager = new ConnectionManager(
                ApplicationProvider.getApplicationContext());

        // --- DeviceIdProvider returns the fixed test device ID ---
        AuthInterceptor.DeviceIdProvider idProvider =
                config::getTestDeviceId;

        // --- OkHttpClient mirrors NetworkModule chain ---
        okHttpClient = new OkHttpClient.Builder()
                .addInterceptor(
                        new RetryInterceptor(connectionManager))
                .addInterceptor(new AuthInterceptor(idProvider))
                .addInterceptor(new ErrorInterceptor())
                .build();

        // --- Retrofit pointed at live server ---
        retrofit = new Retrofit.Builder()
                .baseUrl(config.getHttpBaseUrl())
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        apiService = retrofit.create(ApiService.class);

        // --- TCP stack (real encoder/decoder/socket) ---
        encoder = new TcpMessageEncoder();
        decoder = new TcpMessageDecoder();
        tcpSocketManager = new TcpSocketManager(encoder, decoder);

        // --- PairingManager with fast timeouts ---
        PairingConfig fastPairingConfig = new PairingConfig(
                FAST_PAIRING_TIMEOUT_MS, FAST_PAIRING_RETRIES);
        pairingManager = new PairingManager(
                tcpSocketManager, fastPairingConfig);

        // --- HeartbeatManager with fast interval ---
        HeartbeatConfig fastHeartbeatConfig = new HeartbeatConfig(
                FAST_HEARTBEAT_INTERVAL_MS, true);
        heartbeatManager = new HeartbeatManager(
                tcpSocketManager, fastHeartbeatConfig);

        // --- AckRetrySender ---
        ackRetrySender = new AckRetrySender(tcpSocketManager);
    }

    /**
     * Disconnects TCP and releases resources. Call from
     * {@code @After}.
     */
    public void tearDown() {
        if (heartbeatManager != null) {
            heartbeatManager.stop();
        }
        if (pairingManager != null) {
            pairingManager.cancelPairing();
        }
        if (tcpSocketManager != null) {
            tcpSocketManager.disconnect();
        }
        if (connectionManager != null) {
            connectionManager.shutdown();
        }
    }

    /**
     * Convenience: opens a TCP connection to the server.
     */
    public void connectTcp() {
        tcpSocketManager.connect(
                config.getServerHost(), config.getTcpPort());
    }

    // ---------------------------------------------------------------
    // Getters
    // ---------------------------------------------------------------

    /** @return the test configuration */
    @NonNull
    public IntegrationTestConfig getConfig() {
        return config;
    }

    /** @return the Retrofit {@link ApiService} */
    @NonNull
    public ApiService getApiService() {
        return apiService;
    }

    /** @return the live {@link TcpSocketManager} */
    @NonNull
    public TcpSocketManager getTcpSocketManager() {
        return tcpSocketManager;
    }

    /** @return the {@link PairingManager} */
    @NonNull
    public PairingManager getPairingManager() {
        return pairingManager;
    }

    /** @return the {@link HeartbeatManager} */
    @NonNull
    public HeartbeatManager getHeartbeatManager() {
        return heartbeatManager;
    }

    /** @return the {@link AckRetrySender} */
    @NonNull
    public AckRetrySender getAckRetrySender() {
        return ackRetrySender;
    }

    /** @return the {@link TcpMessageEncoder} */
    @NonNull
    public TcpMessageEncoder getEncoder() {
        return encoder;
    }

    /** @return the {@link TcpMessageDecoder} */
    @NonNull
    public TcpMessageDecoder getDecoder() {
        return decoder;
    }

    /** @return the {@link OkHttpClient} */
    @NonNull
    public OkHttpClient getOkHttpClient() {
        return okHttpClient;
    }
}
