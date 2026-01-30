package com.manuscripta.student.network;

import androidx.annotation.NonNull;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

/**
 * Retrofit API service interface.
 * Defines HTTP endpoints for communication with the Windows teacher server.
 */
public interface ApiService {

    /**
     * Retrieves feedback for responses submitted by a specific device.
     * Per API Contract §2.6, returns all available feedback for the device.
     *
     * @param deviceId The device ID to fetch feedback for
     * @return Call wrapping the FeedbackResponse
     */
    @GET("/feedback/{deviceId}")
    Call<FeedbackResponse> getFeedback(@NonNull @Path("deviceId") String deviceId);
}
