using Microsoft.EntityFrameworkCore;
using Main.Data;
using Main.Models.Entities;
using Main.Models.Entities.Responses;
using Main.Models.Enums;
using Main.Services;
using Main.Services.Repositories;

namespace Main.Testing;

/// <summary>
/// Seeds test data for integration testing.
/// Per Integration Test Contract §12.2.
/// </summary>
public class IntegrationSeedService : IHostedService
{
    // Well-known test IDs per Integration Test Contract §3.1 and §8.1
    private static readonly Guid TestDeviceId =
        Guid.Parse("00000001-0000-0000-0000-000000000001");
    private static readonly Guid TestQuestionId =
        Guid.Parse("00000001-0000-0000-0000-000000000002");
    private static readonly Guid TestMaterialId =
        Guid.Parse("00000001-0000-0000-0000-000000000003");
    private static readonly Guid TestAttachmentId =
        Guid.Parse("00000001-0000-0000-0000-000000000004");
    private static readonly Guid TestFeedbackId =
        Guid.Parse("00000001-0000-0000-0000-000000000005");
    private static readonly Guid TestResponseId =
        Guid.Parse("00000001-0000-0000-0000-000000000006");

    // Separate question for the feedback seed path, so that the
    // ResponseSubmission tests can submit fresh responses for
    // TestQuestionId without hitting the one-per-device constraint.
    private static readonly Guid FeedbackQuestionId =
        Guid.Parse("00000001-0000-0000-0000-000000000007");

    // Hierarchy IDs (not referenced by tests, needed for FK chain)
    private static readonly Guid UnitCollectionId =
        Guid.Parse("00000001-0000-0000-0000-000000000010");
    private static readonly Guid UnitId =
        Guid.Parse("00000001-0000-0000-0000-000000000011");
    private static readonly Guid LessonId =
        Guid.Parse("00000001-0000-0000-0000-000000000012");

    private readonly IServiceProvider _serviceProvider;
    private readonly ILogger<IntegrationSeedService> _logger;

    /// <summary>
    /// Initialises the integration seed service.
    /// </summary>
    public IntegrationSeedService(
        IServiceProvider serviceProvider,
        ILogger<IntegrationSeedService> logger)
    {
        _serviceProvider = serviceProvider;
        _logger = logger;
    }

    /// <summary>
    /// Seeds test data on startup.
    /// </summary>
    public async Task StartAsync(CancellationToken cancellationToken)
    {
        _logger.LogInformation("Seeding integration test data...");

        using var scope = _serviceProvider.CreateScope();
        var context = scope.ServiceProvider.GetRequiredService<MainDbContext>();

        // 1. Ensure DB schema is up to date
        await context.Database.MigrateAsync(cancellationToken);

        // 2. Seed DB hierarchy: UnitCollection → Unit → Lesson → Material → Question
        await SeedDatabaseEntities(context, cancellationToken);

        // 3. Seed attachment entity + physical file
        await SeedAttachment(context, cancellationToken);

        // 4. Seed in-memory response + feedback
        var responseRepo = _serviceProvider.GetRequiredService<IResponseRepository>();
        var feedbackRepo = _serviceProvider.GetRequiredService<IFeedbackRepository>();
        await SeedResponseAndFeedback(responseRepo, feedbackRepo);

        // 5. Assign material to test device in distribution service
        var distributionService = _serviceProvider.GetRequiredService<IDistributionService>();
        await distributionService.AssignMaterialsToDeviceAsync(
            TestDeviceId, new[] { TestMaterialId });

        _logger.LogInformation("Integration test data seeded successfully.");
    }

    /// <summary>
    /// No-op on shutdown.
    /// </summary>
    public Task StopAsync(CancellationToken cancellationToken) => Task.CompletedTask;

    private async Task SeedDatabaseEntities(
        MainDbContext context, CancellationToken ct)
    {
        context.UnitCollections.Add(
            new UnitCollectionEntity(UnitCollectionId, "Integration Test Collection"));

        context.Units.Add(
            new UnitEntity(UnitId, UnitCollectionId, "Integration Test Unit"));

        context.Lessons.Add(
            new LessonEntity(LessonId, UnitId,
                "Integration Test Lesson",
                "Lesson for integration testing"));

        context.Materials.Add(new MaterialDataEntity
        {
            Id = TestMaterialId,
            LessonId = LessonId,
            MaterialType = MaterialType.READING,
            Title = "Integration Test Material",
            Content = "This is test content for integration testing.",
            Timestamp = DateTime.UtcNow
        });

        context.Questions.Add(new QuestionDataEntity
        {
            Id = TestQuestionId,
            MaterialId = TestMaterialId,
            QuestionText = "What is the purpose of integration testing?",
            QuestionType = QuestionType.WRITTEN_ANSWER,
            CorrectAnswer = "To verify that components work together correctly"
        });

        // Second question used for seeding the feedback response
        context.Questions.Add(new QuestionDataEntity
        {
            Id = FeedbackQuestionId,
            MaterialId = TestMaterialId,
            QuestionText = "Describe one benefit of automated testing.",
            QuestionType = QuestionType.WRITTEN_ANSWER,
            CorrectAnswer = "Faster feedback loops"
        });

        await context.SaveChangesAsync(ct);
    }

    private async Task SeedAttachment(
        MainDbContext context, CancellationToken ct)
    {
        context.Attachments.Add(new AttachmentEntity(
            TestAttachmentId, TestMaterialId,
            "test-attachment", "png"));
        await context.SaveChangesAsync(ct);

        // Create physical attachment file
        var attachmentsDir = Path.Combine(
            Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData),
            "ManuscriptaTeacherApp", "Attachments");
        Directory.CreateDirectory(attachmentsDir);

        var filePath = Path.Combine(attachmentsDir, $"{TestAttachmentId}.png");

        // Minimal 1×1 red pixel PNG
        byte[] minimalPng =
        {
            0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
            0x00, 0x00, 0x00, 0x0D, 0x49, 0x48, 0x44, 0x52,
            0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x01,
            0x08, 0x02, 0x00, 0x00, 0x00, 0x90, 0x77, 0x53,
            0xDE, 0x00, 0x00, 0x00, 0x0C, 0x49, 0x44, 0x41,
            0x54, 0x08, 0xD7, 0x63, 0xF8, 0xCF, 0xC0, 0x00,
            0x00, 0x00, 0x02, 0x00, 0x01, 0xE2, 0x21, 0xBC,
            0x33, 0x00, 0x00, 0x00, 0x00, 0x49, 0x45, 0x4E,
            0x44, 0xAE, 0x42, 0x60, 0x82
        };

        await File.WriteAllBytesAsync(filePath, minimalPng, ct);
        _logger.LogInformation(
            "Created test attachment file at {FilePath}", filePath);
    }

    private async Task SeedResponseAndFeedback(
        IResponseRepository responseRepo,
        IFeedbackRepository feedbackRepo)
    {
        var response = new WrittenAnswerResponseEntity(
            TestResponseId,
            FeedbackQuestionId,
            TestDeviceId,
            "Integration test answer",
            DateTime.UtcNow,
            true);
        await responseRepo.AddAsync(response);

        var feedback = new FeedbackEntity(
            TestFeedbackId,
            TestResponseId,
            "Good answer!",
            5);
        feedback.Status = FeedbackStatus.READY;
        await feedbackRepo.AddAsync(feedback);
    }
}
