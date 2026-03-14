package com.manuscripta.student.network;

import androidx.annotation.NonNull;

import com.manuscripta.student.network.dto.BatchResponseDto;
import com.manuscripta.student.network.dto.ConfigResponseDto;
import com.manuscripta.student.network.dto.DeviceInfoDto;
import com.manuscripta.student.network.dto.DistributionBundleDto;
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
 * <p>Per API Contract §2, this interface defines endpoints for content retrieval,
 * configuration management, and response submission.</p>
 *
 * <p>Note: Real-time control signals (lock/unlock, status updates, raise hand) are
 * handled via TCP socket, not HTTP.</p>
 *
 * <p>Related requirements: NET1, NET2, MAT8 (Teacher), MAT15</p>
 */
public interface ApiService {

    // ========== Content Endpoints (Server -> Client) ==========

    /**
     * Downloads a specific attachment file referenced within material content.
     * Per API Contract §2.1.3, returns raw binary data (PDF, images, etc.).
     *
     * <p>The content field within materials may contain references to these
     * attachments using URLs like "/attachments/{id}".</p>
     *
     * @param id The unique identifier of the attachment
     * @return Call wrapping ResponseBody containing the binary data
     */
    @GET("/api/v1/attachments/{id}")
    Call<ResponseBody> getAttachment(@NonNull @Path("id") String id);

    /**
     * Retrieves the material distribution bundle for a specific device.
     * Per API Contract §2.5, returns materials and questions assigned to the device.
     *
     * <p>This endpoint is typically called after receiving a DISTRIBUTE_MATERIAL
     * TCP signal from the server.</p>
     *
     * @param deviceId The device ID to fetch distribution bundle for
     * @return Call wrapping the DistributionBundleDto
     */
    @GET("/api/v1/distribution/{deviceId}")
    Call<DistributionBundleDto> getDistribution(@NonNull @Path("deviceId") String deviceId);

    /**
     * Retrieves feedback for responses submitted by a specific device.
     * Per API Contract §2.6, returns all available feedback for the device.
     *
     * <p>This endpoint is typically called after receiving a RETURN_FEEDBACK
     * TCP signal from the server.</p>
     *
     * @param deviceId The device ID to fetch feedback for
     * @return Call wrapping the FeedbackResponse
     */
    @GET("/api/v1/feedback/{deviceId}")
    Call<FeedbackResponse> getFeedback(@NonNull @Path("deviceId") String deviceId);

    // ========== Configuration Endpoints (Server -> Client) ==========

    /**
     * Retrieves the tablet configuration for a specific device.
     * Per API Contract §2.2, returns settings like kiosk mode and text size.
     *
     * <p>Configuration may be refreshed after receiving a REFRESH_CONFIG
     * TCP signal from the server.</p>
     *
     * @param deviceId The device ID to fetch configuration for
     * @return Call wrapping the ConfigResponseDto
     */
    @GET("/api/v1/config/{deviceId}")
    Call<ConfigResponseDto> getConfig(@NonNull @Path("deviceId") String deviceId);

    // ========== Pairing Endpoints (Client -> Server) ==========

    /**
     * Registers a device with the teacher server during pairing.
     * Per API Contract §2.4 and Pairing Process §2, this is part of the HTTP
     * pairing handshake.
     *
     * @param deviceInfo The device information including device ID and name
     * @return Call wrapping Void (expects 201 Created on success)
     */
    @POST("/api/v1/pair")
    Call<Void> registerDevice(@NonNull @Body DeviceInfoDto deviceInfo);

    // ========== Response Submission Endpoints (Client -> Server) ==========

    /**
     * Submits a single student response to a question.
     * Per API Contract §2.4 (Submit Response), the response ID is generated
     * by the Android client and preserved by the server.
     *
     * @param response The response to submit
     * @return Call wrapping Void (expects 201 Created on success)
     */
    @POST("/api/v1/responses")
    Call<Void> submitResponse(@NonNull @Body ResponseDto response);

    /**
     * Submits multiple responses in a batch.
     * Per API Contract §2.4 (Batch Submit Responses), used when reconnecting
     * after offline mode to submit queued responses.
     *
     * @param batchResponse The batch of responses to submit
     * @return Call wrapping Void (expects 201 Created on success)
     */
    @POST("/api/v1/responses/batch")
    Call<Void> submitBatchResponses(@NonNull @Body BatchResponseDto batchResponse);
}
