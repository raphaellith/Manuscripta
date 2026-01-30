package com.manuscripta.student.network;

import androidx.annotation.NonNull;

import com.manuscripta.student.network.dto.BatchResponseDto;
import com.manuscripta.student.network.dto.ConfigDto;
import com.manuscripta.student.network.dto.DistributionBundleDto;
import com.manuscripta.student.network.dto.FeedbackResponse;
import com.manuscripta.student.network.dto.PairRequestDto;
import com.manuscripta.student.network.dto.ResponseDto;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;

/**
 * Retrofit API service interface.
 * Defines HTTP endpoints for communication with the Windows teacher server.
 *
 * <p>Per API Contract, the system uses HTTP for transmission of large data chunks such as
 * lesson materials (Server→Client) and student responses (Client→Server). Real-time control
 * signals use TCP socket communication.</p>
 */
public interface ApiService {

    /**
     * Retrieves materials and questions assigned to a specific device.
     * Per API Contract §2.5, returns a distribution bundle containing materials and questions.
     * Triggered by TCP DISTRIBUTE_MATERIAL signal.
     *
     * @param deviceId The device ID to fetch distribution for
     * @return Call wrapping the DistributionBundleDto
     */
    @GET("/distribution/{deviceId}")
    Call<DistributionBundleDto> getDistribution(@NonNull @Path("deviceId") String deviceId);

    /**
     * Downloads a binary attachment file (image, PDF, etc.) referenced within material content.
     * Per API Contract §2.1.3, returns raw binary data.
     *
     * @param attachmentId The attachment ID to fetch
     * @return Call wrapping ResponseBody containing binary data
     */
    @GET("/attachments/{id}")
    Call<ResponseBody> getAttachment(@NonNull @Path("id") String attachmentId);

    /**
     * Retrieves tablet configuration settings for a specific device.
     * Per API Contract §2.2, returns configuration object with kiosk mode, text size, etc.
     *
     * @param deviceId The device ID to fetch configuration for
     * @return Call wrapping the ConfigDto
     */
    @GET("/config/{deviceId}")
    Call<ConfigDto> getConfig(@NonNull @Path("deviceId") String deviceId);

    /**
     * Retrieves feedback for responses submitted by a specific device.
     * Per API Contract §2.6, returns all available feedback for the device.
     * Triggered by TCP RETURN_FEEDBACK signal.
     *
     * @param deviceId The device ID to fetch feedback for
     * @return Call wrapping the FeedbackResponse
     */
    @GET("/feedback/{deviceId}")
    Call<FeedbackResponse> getFeedback(@NonNull @Path("deviceId") String deviceId);

    /**
     * Registers a student device with the teacher server during pairing.
     * Per API Contract §2.4, used during the pairing handshake.
     *
     * @param request PairRequestDto containing DeviceId and device name
     * @return Call wrapping Void (201 Created response)
     */
    @POST("/pair")
    Call<Void> registerDevice(@NonNull @Body PairRequestDto request);

    /**
     * Submits a single student response to a question.
     * Per API Contract §2.4, sends ResponseDto to the teacher server.
     *
     * @param response ResponseDto containing the answer
     * @return Call wrapping Void (201 Created response)
     */
    @POST("/responses")
    Call<Void> submitResponse(@NonNull @Body ResponseDto response);

    /**
     * Submits multiple student responses at once.
     * Per API Contract §2.4, used when reconnecting after offline mode.
     *
     * @param batchResponse BatchResponseDto containing multiple responses
     * @return Call wrapping Void (201 Created response)
     */
    @POST("/responses/batch")
    Call<Void> submitBatchResponses(@NonNull @Body BatchResponseDto batchResponse);
}
