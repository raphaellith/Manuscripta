package com.manuscripta.student.data.repository;

import android.util.Log;

import androidx.annotation.NonNull;

import com.manuscripta.student.data.local.FeedbackDao;
import com.manuscripta.student.data.model.FeedbackEntity;
import com.manuscripta.student.domain.mapper.FeedbackMapper;
import com.manuscripta.student.domain.model.Feedback;
import com.manuscripta.student.network.ApiService;
import com.manuscripta.student.network.FeedbackDto;
import com.manuscripta.student.network.FeedbackResponse;
import com.manuscripta.student.network.tcp.TcpProtocolException;
import com.manuscripta.student.network.tcp.TcpSocketManager;
import com.manuscripta.student.network.tcp.message.FeedbackAckMessage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import retrofit2.Response;

/**
 * Implementation of {@link FeedbackRepository}.
 * Handles the complete feedback fetch→store→ACK flow.
 */
@Singleton
public class FeedbackRepositoryImpl implements FeedbackRepository {

    /** Logging tag for this class. */
    private static final String TAG = "FeedbackRepositoryImpl";

    /** The DAO for feedback persistence. */
    private final FeedbackDao feedbackDao;
    /** The Retrofit API service for network calls. */
    private final ApiService apiService;
    /** The TCP socket manager for sending acknowledgements. */
    private final TcpSocketManager tcpSocketManager;

    /**
     * Creates a new FeedbackRepositoryImpl.
     *
     * @param feedbackDao      The DAO for feedback persistence
     * @param apiService       The Retrofit API service
     * @param tcpSocketManager The TCP socket manager for sending ACK
     */
    @Inject
    public FeedbackRepositoryImpl(@NonNull FeedbackDao feedbackDao,
                                  @NonNull ApiService apiService,
                                  @NonNull TcpSocketManager tcpSocketManager) {
        this.feedbackDao = feedbackDao;
        this.apiService = apiService;
        this.tcpSocketManager = tcpSocketManager;
    }

    @Override
    public void fetchAndStoreFeedback(@NonNull String deviceId) throws Exception {
        if (deviceId == null || deviceId.trim().isEmpty()) {
            throw new IllegalArgumentException("Device ID cannot be null or empty");
        }

        // Fetch feedback from server via HTTP
        Response<FeedbackResponse> response = apiService.getFeedback(deviceId).execute();

        if (!response.isSuccessful()) {
            if (response.code() == 404) {
                // No feedback available - this is not an error, send ACK
                sendAcknowledgement(deviceId);
                return;
            }
            throw new IOException("Failed to fetch feedback: HTTP " + response.code());
        }

        FeedbackResponse feedbackResponse = response.body();
        if (feedbackResponse == null || feedbackResponse.getFeedback() == null) {
            sendAcknowledgement(deviceId);
            return;
        }

        // Convert DTOs to entities via domain model for validation
        List<FeedbackEntity> entities = new ArrayList<>();
        for (FeedbackDto dto : feedbackResponse.getFeedback()) {
            try {
                // Validate through domain model
                Feedback domainFeedback = new Feedback(
                        dto.getId(),
                        dto.getResponseId(),
                        dto.getText(),
                        dto.getMarks()
                );
                // Convert to entity for persistence
                FeedbackEntity entity = FeedbackMapper.toEntity(domainFeedback);
                entities.add(entity);
            } catch (IllegalArgumentException e) {
                // Log and skip invalid feedback entries
                Log.w(TAG, "Skipping invalid feedback: " + e.getMessage());
            }
        }

        if (!entities.isEmpty()) {
            feedbackDao.insertAll(entities);
        }

        // Send acknowledgement via TCP
        sendAcknowledgement(deviceId);
    }

    /**
     * Sends a FEEDBACK_ACK message to the server with retry.
     *
     * <p>Retries up to 3 times with a 500ms delay between attempts to handle
     * transient socket failures. The server allows a 30-second window for ACK
     * receipt, so the total retry window (~1.5s) is well within bounds.</p>
     *
     * @param deviceId The device ID to include in the acknowledgement
     */
    private void sendAcknowledgement(@NonNull String deviceId) {
        int maxAttempts = 3;
        long delayMs = 500;
        FeedbackAckMessage ackMessage = new FeedbackAckMessage(deviceId);

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                tcpSocketManager.send(ackMessage);
                Log.d(TAG, "Sent FEEDBACK_ACK for device: " + deviceId);
                return;
            } catch (TcpProtocolException | IOException e) {
                if (attempt < maxAttempts) {
                    Log.w(TAG, "FEEDBACK_ACK attempt " + attempt + " failed, retrying: "
                            + e.getMessage());
                    try {
                        Thread.sleep(delayMs);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        Log.e(TAG, "ACK retry interrupted", ie);
                        return;
                    }
                } else {
                    Log.e(TAG, "Failed to send FEEDBACK_ACK after " + maxAttempts
                            + " attempts: " + e.getMessage(), e);
                }
            }
        }
    }

    @Override
    public Feedback getFeedbackForResponse(@NonNull String responseId) {
        FeedbackEntity entity = feedbackDao.getByResponseId(responseId);
        if (entity == null) {
            return null;
        }
        return FeedbackMapper.toDomain(entity);
    }

    @Override
    public List<Feedback> getAllFeedback() {
        List<FeedbackEntity> entities = feedbackDao.getAll();
        List<Feedback> feedbackList = new ArrayList<>();
        for (FeedbackEntity entity : entities) {
            feedbackList.add(FeedbackMapper.toDomain(entity));
        }
        return feedbackList;
    }

    @Override
    public List<Feedback> getAllFeedbackByDeviceId(@NonNull String deviceId) {
        List<FeedbackEntity> entities = feedbackDao.getAllByDeviceId(deviceId);
        List<Feedback> feedbackList = new ArrayList<>();
        for (FeedbackEntity entity : entities) {
            feedbackList.add(FeedbackMapper.toDomain(entity));
        }
        return feedbackList;
    }

    @Override
    public void deleteAllFeedback() {
        feedbackDao.deleteAll();
    }

    @Override
    public int getFeedbackCount() {
        return feedbackDao.getCount();
    }
}
