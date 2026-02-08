using System;
using System.Collections.Generic;
using System.Linq;
using System.Threading;
using System.Threading.Tasks;
using Main.Models.Entities;
using Main.Models.Entities.Responses;
using Main.Models.Enums;
using Main.Services.GenAI;
using Main.Services.Hubs;
using Main.Services.Network;
using Main.Services.Repositories;
using Microsoft.AspNetCore.SignalR;
using Moq;
using Xunit;

namespace MainTests.ServicesTests.GenAI;

/// <summary>
/// Spec coverage: GenAISpec Section 3D(2)-(8A) and 3DA (queueing and dispatch lifecycle).
/// See docs/specifications/GenAISpec.md.
/// </summary>
public class FeedbackQueueServiceTests
{
    private static bool MatchesDispatchFailedArgs(object?[] args, Guid feedbackId, Guid? deviceId, string? messageContains)
    {
        var expectsMessage = messageContains != null;
        var expectedLength = expectsMessage ? 3 : 2;
        if (args.Length != expectedLength)
        {
            return false;
        }

        if (args[0] == null || args[0].GetType() != typeof(Guid) || (Guid)args[0]! != feedbackId)
        {
            return false;
        }

        if (deviceId.HasValue)
        {
            if (args[1] == null || args[1].GetType() != typeof(Guid) || (Guid)args[1]! != deviceId.Value)
            {
                return false;
            }
        }
        else if (args[1] != null)
        {
            return false;
        }

        if (expectsMessage)
        {
            if (args[2] == null || args[2].GetType() != typeof(string))
            {
                return false;
            }

            var message = (string)args[2]!;
            return message.Contains(messageContains, StringComparison.OrdinalIgnoreCase);
        }

        return true;
    }

    private static (Mock<IHubContext<TeacherPortalHub>> hubContext, Mock<IClientProxy> clientProxy) BuildHubMocks()
    {
        var clientProxy = new Mock<IClientProxy>();
        clientProxy.Setup(c => c.SendCoreAsync(
                It.IsAny<string>(),
                It.IsAny<object?[]>(),
                It.IsAny<CancellationToken>()))
            .Returns(Task.CompletedTask);

        var clients = new Mock<IHubClients>();
        clients.Setup(c => c.All).Returns(clientProxy.Object);

        var hubContext = new Mock<IHubContext<TeacherPortalHub>>();
        hubContext.Setup(h => h.Clients).Returns(clients.Object);

        return (hubContext, clientProxy);
    }

    /// <summary>
    /// Spec coverage: GenAISpec Section 3D(5) (queueing behavior for AI feedback generation).
    /// See docs/specifications/GenAISpec.md.
    /// </summary>
    [Fact]
    public void QueueForAiGeneration_DoesNotEnqueueDuplicates()
    {
        var (hubContext, _) = BuildHubMocks();
        var tcpService = new Mock<ITcpPairingService>();
        var responseRepo = new Mock<IResponseRepository>();

        var service = new FeedbackQueueService(hubContext.Object, tcpService.Object, responseRepo.Object);
        var responseId = Guid.NewGuid();

        service.QueueForAiGeneration(responseId);
        service.QueueForAiGeneration(responseId);

        var first = service.DequeueNext();
        var second = service.DequeueNext();

        Assert.Equal(responseId, first);
        Assert.Null(second);
    }

    /// <summary>
    /// Spec coverage: GenAISpec Section 3D(6) (explicit queue removal).
    /// See docs/specifications/GenAISpec.md.
    /// </summary>
    [Fact]
    public void RemoveFromQueue_RemovesSpecificEntry()
    {
        var (hubContext, _) = BuildHubMocks();
        var tcpService = new Mock<ITcpPairingService>();
        var responseRepo = new Mock<IResponseRepository>();

        var service = new FeedbackQueueService(hubContext.Object, tcpService.Object, responseRepo.Object);
        var firstId = Guid.NewGuid();
        var middleId = Guid.NewGuid();
        var lastId = Guid.NewGuid();

        service.QueueForAiGeneration(firstId);
        service.QueueForAiGeneration(middleId);
        service.QueueForAiGeneration(lastId);
        service.RemoveFromQueue(middleId);

        Assert.Equal(firstId, service.DequeueNext());
        Assert.Equal(lastId, service.DequeueNext());
        Assert.Null(service.DequeueNext());
    }

    /// <summary>
    /// Spec coverage: GenAISpec Section 3DA(1)-(2) (provisional feedback is not dispatched).
    /// See docs/specifications/GenAISpec.md.
    /// </summary>
    [Fact]
    public void ShouldDispatchFeedback_RespectsProvisionalStatus()
    {
        var (hubContext, _) = BuildHubMocks();
        var tcpService = new Mock<ITcpPairingService>();
        var responseRepo = new Mock<IResponseRepository>();

        var service = new FeedbackQueueService(hubContext.Object, tcpService.Object, responseRepo.Object);

        var feedback = new FeedbackEntity(Guid.NewGuid(), Guid.NewGuid(), "Text")
        {
            Status = FeedbackStatus.PROVISIONAL
        };

        Assert.False(service.ShouldDispatchFeedback(feedback));

        feedback.Status = FeedbackStatus.READY;
        Assert.True(service.ShouldDispatchFeedback(feedback));
    }

    /// <summary>
    /// Spec coverage: GenAISpec Section 3DA(2) (approve transitions to READY and triggers dispatch).
    /// See docs/specifications/GenAISpec.md.
    /// </summary>
    [Fact]
    public async Task ApproveFeedbackAsync_ResponseExists_SendsReturnFeedback()
    {
        var (hubContext, _) = BuildHubMocks();
        var tcpService = new Mock<ITcpPairingService>();
        var responseRepo = new Mock<IResponseRepository>();

        var service = new FeedbackQueueService(hubContext.Object, tcpService.Object, responseRepo.Object);
        var feedback = new FeedbackEntity(Guid.NewGuid(), Guid.NewGuid(), "Good")
        {
            Status = FeedbackStatus.PROVISIONAL
        };

        var response = new WrittenAnswerResponseEntity(feedback.ResponseId, Guid.NewGuid(), Guid.NewGuid(), "Answer");
        responseRepo.Setup(r => r.GetByIdAsync(feedback.ResponseId))
            .ReturnsAsync(response);

        tcpService.Setup(t => t.SendReturnFeedbackAsync(response.DeviceId.ToString(), It.IsAny<IEnumerable<Guid>>()))
            .Returns(Task.CompletedTask);

        await service.ApproveFeedbackAsync(feedback);

        Assert.Equal(FeedbackStatus.READY, feedback.Status);
        tcpService.Verify(t => t.SendReturnFeedbackAsync(
            response.DeviceId.ToString(),
            It.Is<IEnumerable<Guid>>(ids => ids.Contains(feedback.Id))), Times.Once);
    }

    /// <summary>
    /// Spec coverage: GenAISpec Section 3DA(4) (dispatch failure notifies frontend).
    /// See docs/specifications/GenAISpec.md.
    /// </summary>
    [Fact]
    public async Task ApproveFeedbackAsync_ResponseMissing_NotifiesDispatchFailed()
    {
        var (hubContext, clientProxy) = BuildHubMocks();
        var tcpService = new Mock<ITcpPairingService>();
        var responseRepo = new Mock<IResponseRepository>();

        var service = new FeedbackQueueService(hubContext.Object, tcpService.Object, responseRepo.Object);
        var feedback = new FeedbackEntity(Guid.NewGuid(), Guid.NewGuid(), "Good")
        {
            Status = FeedbackStatus.PROVISIONAL
        };

        responseRepo.Setup(r => r.GetByIdAsync(feedback.ResponseId))
            .ReturnsAsync((ResponseEntity?)null);

        await service.ApproveFeedbackAsync(feedback);

        Assert.Equal(FeedbackStatus.READY, feedback.Status);
        clientProxy.Verify(c => c.SendCoreAsync(
            "OnFeedbackDispatchFailed",
            It.Is<object?[]>(args => MatchesDispatchFailedArgs(args, feedback.Id, null, "Response not found")),
            It.IsAny<CancellationToken>()), Times.Once);
    }

    /// <summary>
    /// Spec coverage: GenAISpec Section 3DA(3) (feedback acknowledgement transitions to DELIVERED).
    /// See docs/specifications/GenAISpec.md.
    /// </summary>
    [Fact]
    public void MarkFeedbackDelivered_ReadyFeedback_MovesToDelivered()
    {
        var (hubContext, _) = BuildHubMocks();
        var tcpService = new Mock<ITcpPairingService>();
        var responseRepo = new Mock<IResponseRepository>();

        var service = new FeedbackQueueService(hubContext.Object, tcpService.Object, responseRepo.Object);
        var feedback = new FeedbackEntity(Guid.NewGuid(), Guid.NewGuid(), "Good")
        {
            Status = FeedbackStatus.READY
        };

        service.MarkFeedbackDelivered(feedback);

        Assert.Equal(FeedbackStatus.DELIVERED, feedback.Status);
    }

    /// <summary>
    /// Spec coverage: GenAISpec Section 3DA(4) (frontend notified on dispatch failure).
    /// See docs/specifications/GenAISpec.md.
    /// </summary>
    [Fact]
    public void HandleDispatchFailure_NotifiesFrontend()
    {
        var (hubContext, clientProxy) = BuildHubMocks();
        var tcpService = new Mock<ITcpPairingService>();
        var responseRepo = new Mock<IResponseRepository>();

        var service = new FeedbackQueueService(hubContext.Object, tcpService.Object, responseRepo.Object);
        var feedback = new FeedbackEntity(Guid.NewGuid(), Guid.NewGuid(), "Good")
        {
            Status = FeedbackStatus.READY
        };
        var deviceId = Guid.NewGuid();

        service.HandleDispatchFailure(feedback, deviceId);

        clientProxy.Verify(c => c.SendCoreAsync(
            "OnFeedbackDispatchFailed",
            It.Is<object?[]>(args => MatchesDispatchFailedArgs(args, feedback.Id, deviceId, null)),
            It.IsAny<CancellationToken>()), Times.Once);
    }
}
