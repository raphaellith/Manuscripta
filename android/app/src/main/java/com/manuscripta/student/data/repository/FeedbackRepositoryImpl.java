package com.manuscripta.student.data.repository;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Transformations;

import com.manuscripta.student.data.local.FeedbackDao;
import com.manuscripta.student.data.model.FeedbackEntity;
import com.manuscripta.student.domain.mapper.FeedbackMapper;
import com.manuscripta.student.domain.model.Feedback;
import com.manuscripta.student.network.ApiService;
import com.manuscripta.student.network.FeedbackDto;
import com.manuscripta.student.network.FeedbackResponse;
import com.manuscripta.student.network.tcp.AckRetrySender;
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
    /** Handles retry logic for sending ACK messages over TCP. */
    private final AckRetrySender ackRetrySender;
    /** Observable LiveData for feedback changes, backed by Room. */
    private final LiveData<List<Feedback>> feedbackLiveData;

    /**
     * Creates a new FeedbackRepositoryImpl.
     *
     * @param feedbackDao    The DAO for feedback persistence
     * @param apiService     The Retrofit API service
     * @param ackRetrySender The retry sender for ACK messages
     */
    @Inject
    public FeedbackRepositoryImpl(@NonNull FeedbackDao feedbackDao,
                                  @NonNull ApiService apiService,
                                  @NonNull AckRetrySender ackRetrySender) {
        if (feedbackDao == null) {
            throw new IllegalArgumentException("FeedbackDao cannot be null");
        }
        if (apiService == null) {
            throw new IllegalArgumentException("ApiService cannot be null");
        }
        if (ackRetrySender == null) {
            throw new IllegalArgumentException("AckRetrySender cannot be null");
        }
        this.feedbackDao = feedbackDao;
        this.apiService = apiService;
        this.ackRetrySender = ackRetrySender;
        this.feedbackLiveData = Transformations.map(
                feedbackDao.getAllLive(),
                entities -> {
                    List<Feedback> result = new ArrayList<>();
                    for (FeedbackEntity entity : entities) {
                        result.add(FeedbackMapper.toDomain(entity));
                    }
                    return result;
                }
        );
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
                // No feedback available — nothing to acknowledge
                return;
            }
            throw new IOException("Failed to fetch feedback: HTTP " + response.code());
        }

        FeedbackResponse feedbackResponse = response.body();
        if (feedbackResponse == null || feedbackResponse.getFeedback() == null) {
            return;
        }

        // Convert DTOs to entities via domain model for validation
        List<FeedbackEntity> entities = new ArrayList<>();
        List<String> feedbackIds = new ArrayList<>();
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
                feedbackIds.add(dto.getId());
            } catch (IllegalArgumentException e) {
                // Log and skip invalid feedback entries
                Log.w(TAG, "Skipping invalid feedback: " + e.getMessage());
            }
        }

        // Per API Contract §3.6.2, send one FEEDBACK_ACK per valid received entity.
        // ACKs are sent immediately on receipt, before DB persistence.
        for (String feedbackId : feedbackIds) {
            ackRetrySender.send(new FeedbackAckMessage(deviceId, feedbackId), TAG);
        }

        if (!entities.isEmpty()) {
            feedbackDao.insertAll(entities);
        }
    }

    @Override
    @NonNull
    public LiveData<List<Feedback>> getFeedbackLiveData() {
        return feedbackLiveData;
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
