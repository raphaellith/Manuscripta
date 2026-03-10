using System.Collections.Concurrent;
using Main.Models.Entities;
using Main.Models.Enums;
using Main.Services.Hubs;
using Main.Services.Network;
using Main.Services.Repositories;
using Microsoft.AspNetCore.SignalR;

namespace Main.Services.GenAI;

/// <summary>
/// Manages the feedback approval workflow and dispatch status, as well as the generation queue.
/// See GenAISpec.md §3DA and §3D.
/// </summary>
public class FeedbackQueueService
{
    private readonly ConcurrentQueue<Guid> _generationQueue = new();
    private readonly IHubContext<TeacherPortalHub> _hubContext;
    private readonly ITcpPairingService _tcpPairingService;
    private readonly IResponseRepository _responseRepository;

    public FeedbackQueueService(
        IHubContext<TeacherPortalHub> hubContext,
        ITcpPairingService tcpPairingService,
        IResponseRepository responseRepository)
    {
        _hubContext = hubContext;
        _tcpPairingService = tcpPairingService;
        _responseRepository = responseRepository;
    }

    /// <summary>
    /// Checks if a response is currently queued for generation.
    /// See GenAISpec.md §3D(3).
    /// </summary>
    public bool IsQueued(Guid responseId)
    {
        return _generationQueue.AsEnumerable().Contains(responseId);
    }

    /// <summary>
    /// Dequeues the next response ID for processing.
    /// Returns null if the queue is empty.
    /// </summary>
    public Guid? DequeueNext()
    {
        if (_generationQueue.TryDequeue(out var responseId))
        {
            return responseId;
        }
        return null;
    }

    /// <summary>
    /// Adds a response to the generation queue.
    /// See GenAISpec.md §3D(5).
    /// </summary>
    public void QueueForAiGeneration(Guid responseId)
    {
        if (!_generationQueue.Contains(responseId))
        {
            _generationQueue.Enqueue(responseId);
        }
    }

    /// <summary>
    /// Removes a response from the generation queue.
    /// See GenAISpec.md §3D(6) and §3D(7).
    /// </summary>
    public void RemoveFromQueue(Guid responseId)
    {
        // Rebuild the queue without the specified responseId
        var itemsToKeep = new List<Guid>();
        while (_generationQueue.TryDequeue(out var id))
        {
            if (id != responseId)
            {
                itemsToKeep.Add(id);
            }
        }
        
        // Re-enqueue the remaining items
        foreach (var item in itemsToKeep)
        {
            _generationQueue.Enqueue(item);
        }
    }

    /// <summary>
    /// Moves a queued response to the front of the generation queue.
    /// See GenAISpec.md §3D(8A).
    /// </summary>
    /// <returns>true if the response was prioritized, false if it was not in the queue or is currently generating</returns>
    public bool PrioritizeResponse(Guid responseId)
    {
        // Check if the response is in the queue (but not if it's currently being generated)
        var itemsToKeep = new List<Guid>();
        bool found = false;
        
        while (_generationQueue.TryDequeue(out var id))
        {
            if (id == responseId)
            {
                found = true;
            }
            else
            {
                itemsToKeep.Add(id);
            }
        }
        
        if (!found)
        {
            // Re-enqueue the items that were dequeued
            foreach (var item in itemsToKeep)
            {
                _generationQueue.Enqueue(item);
            }
            return false;
        }
        
        // Enqueue the prioritized response first
        _generationQueue.Enqueue(responseId);
        
        // Then re-enqueue the remaining items
        foreach (var item in itemsToKeep)
        {
            _generationQueue.Enqueue(item);
        }
        
        return true;
    }

    /// <summary>
    /// Returns a snapshot of all response IDs currently in the generation queue.
    /// Used by the hub to expose queue status to the frontend.
    /// </summary>
    public List<Guid> GetQueuedResponseIds()
    {
        return _generationQueue.ToArray().ToList();
    }

    /// <summary>
    /// Checks if feedback should be dispatched to the student device.
    /// See GenAISpec.md §3DA(1).
    /// </summary>
    public bool ShouldDispatchFeedback(FeedbackEntity feedback)
    {
        // PROVISIONAL feedback shall not be dispatched
        return feedback.Status != FeedbackStatus.PROVISIONAL;
    }

    /// <summary>
    /// Approves feedback and updates status to READY.
    /// See GenAISpec.md §3DA(2).
    /// </summary>
    public async Task ApproveFeedbackAsync(FeedbackEntity feedback)
    {
        if (feedback.Status == FeedbackStatus.PROVISIONAL)
        {
            feedback.Status = FeedbackStatus.READY;
            
            // §3DA(2)(b): Trigger dispatch immediately via Session Interaction Specification §7
            // Retrieve the response to get the device ID
            var response = await _responseRepository.GetByIdAsync(feedback.ResponseId);
            if (response != null)
            {
                // Send RETURN_FEEDBACK message to the device
                // This will trigger the Android device to retrieve feedback via GET /feedback/{deviceId}
                await _tcpPairingService.SendReturnFeedbackAsync(
                    response.DeviceId.ToString(),
                    new[] { feedback.Id });
            }
            else
            {
                // Log warning if response not found but still transition feedback to READY
                // The teacher can manually retry dispatch if needed
                _ = _hubContext.Clients.All.SendAsync("OnFeedbackDispatchFailed", feedback.Id, null, "Response not found for feedback");
            }
        }
    }

    /// <summary>
    /// Marks feedback as delivered after receiving acknowledgement.
    /// See GenAISpec.md §3DA(3).
    /// </summary>
    public void MarkFeedbackDelivered(FeedbackEntity feedback)
    {
        if (feedback.Status == FeedbackStatus.READY)
        {
            feedback.Status = FeedbackStatus.DELIVERED;
        }
    }

    /// <summary>
    /// Handles feedback dispatch failure.
    /// See GenAISpec.md §3DA(4).
    /// </summary>
    public void HandleDispatchFailure(FeedbackEntity feedback, Guid deviceId)
    {
        // Feedback remains in READY status
        // §3DA(4)(a): Notify frontend via SignalR OnFeedbackDispatchFailed
        _ = _hubContext.Clients.All.SendAsync("OnFeedbackDispatchFailed", feedback.Id, deviceId);
    }
}
