using Main.Data;
using Main.Models.Entities;
using Main.Models.Entities.Responses;
using Main.Models.Enums;
using Main.Services;
using Main.Services.Repositories;

namespace Main.Testing;

/// <summary>
/// Seeds test data for integration-test mode.
/// Per IntegrationTestSpecification §4.
///
/// This class seeds the full entity hierarchy required for integration testing:
///   UnitCollection → Unit → Lesson → Material → Question (EF Core)
///   PairedDevice, Response, Feedback (in-memory repositories)
///   Configuration (EF Core)
///   Attachment file (file system)
///   Distribution assignment (in-memory)
///
/// Entity relationships follow Persistence and Cascading Rules §2.
/// </summary>
public static class IntegrationTestDataSeeder
{
    // Well-known test identifiers per Integration Test Contract §3.1 and §8.1.
    // Note: The Contract uses string identifiers (e.g., "test-material-001") which are
    // not valid GUIDs. This seeder uses deterministic GUIDs that map to those identifiers.
    // The client tests discover actual IDs via API responses (e.g., GET /distribution).
    private static readonly Guid TestDeviceId = new("00000000-0000-0000-0000-000000000001");
    private const string TestDeviceName = "Integration Test Tablet";

    private static readonly Guid TestUnitCollectionId = new("10000000-0000-0000-0000-000000000001");
    private static readonly Guid TestUnitId = new("10000000-0000-0000-0000-000000000002");
    private static readonly Guid TestLessonId = new("10000000-0000-0000-0000-000000000003");
    private static readonly Guid TestMaterialId = new("10000000-0000-0000-0000-000000000010");
    private static readonly Guid TestQuestionId = new("10000000-0000-0000-0000-000000000020");
    private static readonly Guid TestResponseId = new("10000000-0000-0000-0000-000000000030");
    private static readonly Guid TestFeedbackId = new("10000000-0000-0000-0000-000000000040");
    private static readonly Guid TestAttachmentId = new("10000000-0000-0000-0000-000000000050");

    /// <summary>
    /// Seeds all test data required for integration testing.
    /// Per IntegrationTestSpecification §4(4): invoked after schema creation,
    /// before network services begin accepting connections.
    /// </summary>
    public static async Task SeedAsync(IServiceProvider services)
    {
        await SeedDatabaseEntitiesAsync(services);
        await SeedInMemoryEntitiesAsync(services);
        SeedAttachmentFile();

        var logger = services.GetRequiredService<ILogger<Program>>();
        logger.LogInformation("Integration test data seeded successfully");
    }

    /// <summary>
    /// Seeds EF Core-persisted entities: hierarchy chain, material, question, attachment, configuration.
    /// Per IntegrationTestSpecification §4(2)(a)–(d), (f).
    /// </summary>
    private static async Task SeedDatabaseEntitiesAsync(IServiceProvider services)
    {
        using var scope = services.CreateScope();
        var context = scope.ServiceProvider.GetRequiredService<MainDbContext>();

        // Seed hierarchy: UnitCollection → Unit → Lesson
        // Required as parent chain for MaterialDataEntity (FK: LessonId)
        context.UnitCollections.Add(new UnitCollectionEntity(TestUnitCollectionId, "Integration Test Collection"));
        context.Units.Add(new UnitEntity(TestUnitId, TestUnitCollectionId, "Integration Test Unit"));
        context.Lessons.Add(new LessonEntity(TestLessonId, TestUnitId, "Integration Test Lesson", "Lesson for integration testing"));

        // §4(2)(b): Material entity with ID test-material-001
        context.Materials.Add(new MaterialDataEntity
        {
            Id = TestMaterialId,
            LessonId = TestLessonId,
            MaterialType = MaterialType.READING,
            Title = "Integration Test Material",
            Content = "# Test Material\n\nThis is a test material for integration testing.",
            Timestamp = DateTime.UtcNow
        });

        // §4(2)(c): Question entity with ID test-question-001, linked to test-material-001
        context.Questions.Add(new QuestionDataEntity
        {
            Id = TestQuestionId,
            MaterialId = TestMaterialId,
            QuestionType = QuestionType.WRITTEN_ANSWER,
            QuestionText = "What is the purpose of integration testing?",
            CorrectAnswer = "To verify cross-platform communication",
            MaxScore = 2
        });

        // §4(2)(f): Attachment entity for test-attachment-001
        context.Attachments.Add(new AttachmentEntity(TestAttachmentId, TestMaterialId, "test-attachment-001", "png"));

        // §4(2)(a): Default configuration per ConfigurationManagementSpecification Appendix 1
        context.Configurations.Add(ConfigurationEntity.CreateDefault());

        await context.SaveChangesAsync();
    }

    /// <summary>
    /// Seeds in-memory entities: paired device, response, feedback, distribution assignment.
    /// Per IntegrationTestSpecification §4(2)(a), (d), (e).
    /// </summary>
    private static async Task SeedInMemoryEntitiesAsync(IServiceProvider services)
    {
        // Register the well-known test device per Integration Test Contract §3.1
        var deviceRegistry = services.GetRequiredService<IDeviceRegistryService>();
        await deviceRegistry.RegisterDeviceAsync(TestDeviceId, TestDeviceName);

        // Seed a response for the test device (prerequisite for feedback)
        var responseRepo = services.GetRequiredService<IResponseRepository>();
        var testResponse = new WrittenAnswerResponseEntity(
            TestResponseId, TestQuestionId, TestDeviceId,
            "Integration test answer", timestamp: DateTime.UtcNow, isCorrect: false);
        await responseRepo.AddAsync(testResponse);

        // §4(2)(e): Feedback entity with status READY, linked to the response
        var feedbackRepo = services.GetRequiredService<IFeedbackRepository>();
        var testFeedback = new FeedbackEntity(
            TestFeedbackId, TestResponseId, text: "Good effort", marks: 1)
        {
            Status = FeedbackStatus.READY
        };
        await feedbackRepo.AddAsync(testFeedback);

        // §4(2)(d): Assign the test material to the test device for distribution
        var distributionService = services.GetRequiredService<IDistributionService>();
        await distributionService.AssignMaterialsToDeviceAsync(TestDeviceId, new[] { TestMaterialId });
    }

    /// <summary>
    /// Places a small test PNG file in the attachment storage directory.
    /// Per IntegrationTestSpecification §4(2)(f).
    /// </summary>
    private static void SeedAttachmentFile()
    {
        var attachmentsDir = Path.Combine(
            Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData),
            "ManuscriptaTeacherApp",
            "Attachments");
        Directory.CreateDirectory(attachmentsDir);

        var filePath = Path.Combine(attachmentsDir, $"{TestAttachmentId}.png");
        if (!File.Exists(filePath))
        {
            // Minimal valid 1x1 PNG (67 bytes)
            byte[] minimalPng =
            {
                0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, // PNG signature
                0x00, 0x00, 0x00, 0x0D, 0x49, 0x48, 0x44, 0x52, // IHDR chunk
                0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x01, // 1x1 pixels
                0x08, 0x02, 0x00, 0x00, 0x00, 0x90, 0x77, 0x53, // 8-bit RGB
                0xDE, 0x00, 0x00, 0x00, 0x0C, 0x49, 0x44, 0x41, // IDAT chunk
                0x54, 0x08, 0xD7, 0x63, 0xF8, 0xCF, 0xC0, 0x00, // compressed data
                0x00, 0x00, 0x02, 0x00, 0x01, 0xE2, 0x21, 0xBC, // ...
                0x33, 0x00, 0x00, 0x00, 0x00, 0x49, 0x45, 0x4E, // IEND chunk
                0x44, 0xAE, 0x42, 0x60, 0x82
            };
            File.WriteAllBytes(filePath, minimalPng);
        }
    }
}
