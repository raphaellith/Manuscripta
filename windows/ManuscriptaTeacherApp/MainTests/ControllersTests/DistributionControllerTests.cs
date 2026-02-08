using System.Text.Json;
using Microsoft.AspNetCore.Mvc;
using Microsoft.AspNetCore.Mvc.Testing;
using Microsoft.Extensions.DependencyInjection;
using Microsoft.Extensions.Logging;
using Moq;
using Main.Controllers;
using Main.Models.Entities.Materials;
using Main.Models.Entities.Questions;
using Main.Services;
using Xunit;

namespace MainTests.ControllersTests;

/// <summary>
/// Unit tests for DistributionController.
/// Verifies GET /distribution/{deviceId} endpoint per API Contract.md §2.5.
/// </summary>
public class DistributionControllerTests
{
    private readonly Mock<IDistributionService> _mockDistributionService;
    private readonly Mock<ILogger<DistributionController>> _mockLogger;
    private readonly DistributionController _controller;

    private readonly Guid _testDeviceId = Guid.NewGuid();
    private readonly Guid _testMaterialId = Guid.NewGuid();
    private readonly Guid _testLessonId = Guid.NewGuid();

    public DistributionControllerTests()
    {
        _mockDistributionService = new Mock<IDistributionService>();
        _mockLogger = new Mock<ILogger<DistributionController>>();
        _controller = new DistributionController(_mockDistributionService.Object, _mockLogger.Object);
    }

    #region Constructor Tests

    [Fact]
    public void Constructor_NullDistributionService_ThrowsArgumentNullException()
    {
        Assert.Throws<ArgumentNullException>(() =>
            new DistributionController(null!, _mockLogger.Object));
    }

    [Fact]
    public void Constructor_NullLogger_ThrowsArgumentNullException()
    {
        Assert.Throws<ArgumentNullException>(() =>
            new DistributionController(_mockDistributionService.Object, null!));
    }

    #endregion

    #region GET /distribution/{deviceId} Tests

    [Fact]
    public async Task GetDistribution_ValidDeviceId_Returns200OK()
    {
        // Arrange
        var material = new ReadingMaterialEntity(_testMaterialId, _testLessonId, "Test Material", "Test Content");

        var questions = new List<QuestionEntity>
        {
            new MultipleChoiceQuestionEntity(Guid.NewGuid(), _testMaterialId, "Is this a test?", new List<string> { "Yes", "No" }, 0)
        };

        var bundle = new DistributionBundle(new[] { material }, questions);
        _mockDistributionService.Setup(x => x.GetDistributionBundleAsync(_testDeviceId))
            .ReturnsAsync(bundle);

        // Act
        var result = await _controller.GetDistribution(_testDeviceId.ToString());

        // Assert
        var okResult = Assert.IsType<OkObjectResult>(result);
        Assert.Equal(200, okResult.StatusCode);
    }

    [Fact]
    public async Task GetDistribution_ValidDeviceId_ReturnsMaterialsAndQuestions()
    {
        // Arrange
        var material = new ReadingMaterialEntity(_testMaterialId, _testLessonId, "Test Material", "Test Content");

        var questions = new List<QuestionEntity>
        {
            new MultipleChoiceQuestionEntity(Guid.NewGuid(), _testMaterialId, "Is this a test?", new List<string> { "Yes", "No" }, 0)
        };

        var bundle = new DistributionBundle(new[] { material }, questions);
        _mockDistributionService.Setup(x => x.GetDistributionBundleAsync(_testDeviceId))
            .ReturnsAsync(bundle);

        // Act
        var result = await _controller.GetDistribution(_testDeviceId.ToString());

        // Assert
        var okResult = Assert.IsType<OkObjectResult>(result);
        Assert.NotNull(okResult.Value);
        
        // Verify the response has materials and questions properties
        var valueType = okResult.Value.GetType();
        var materialsProperty = valueType.GetProperty("materials");
        var questionsProperty = valueType.GetProperty("questions");
        
        Assert.NotNull(materialsProperty);
        Assert.NotNull(questionsProperty);
    }

    [Fact]
    public async Task GetDistribution_NoMaterialsAvailable_Returns404NotFound()
    {
        // Arrange - Per API Contract §2.5: 404 if no materials available
        _mockDistributionService.Setup(x => x.GetDistributionBundleAsync(_testDeviceId))
            .ReturnsAsync((DistributionBundle?)null);

        // Act
        var result = await _controller.GetDistribution(_testDeviceId.ToString());

        // Assert
        Assert.IsType<NotFoundObjectResult>(result);
    }

    [Fact]
    public async Task GetDistribution_EmptyDeviceId_Returns400BadRequest()
    {
        // Act
        var result = await _controller.GetDistribution("");

        // Assert
        Assert.IsType<BadRequestObjectResult>(result);
    }

    [Fact]
    public async Task GetDistribution_WhitespaceDeviceId_Returns400BadRequest()
    {
        // Act
        var result = await _controller.GetDistribution("   ");

        // Assert
        Assert.IsType<BadRequestObjectResult>(result);
    }

    [Fact]
    public async Task GetDistribution_InvalidGuidFormat_Returns400BadRequest()
    {
        // Act
        var result = await _controller.GetDistribution("not-a-valid-guid");

        // Assert
        Assert.IsType<BadRequestObjectResult>(result);
    }

    [Fact]
    public async Task GetDistribution_InvalidGuidFormat_ReturnsErrorMessage()
    {
        // Act
        var result = await _controller.GetDistribution("invalid-guid") as BadRequestObjectResult;

        // Assert
        Assert.NotNull(result);
        var value = result.Value?.ToString();
        Assert.Contains("GUID", value, StringComparison.OrdinalIgnoreCase);
    }

    [Fact]
    public async Task GetDistribution_ServiceThrowsException_Returns500()
    {
        // Arrange
        _mockDistributionService.Setup(x => x.GetDistributionBundleAsync(_testDeviceId))
            .ThrowsAsync(new Exception("Test exception"));

        // Act
        var result = await _controller.GetDistribution(_testDeviceId.ToString());

        // Assert
        var statusResult = Assert.IsType<ObjectResult>(result);
        Assert.Equal(500, statusResult.StatusCode);
    }

    [Fact]
    public async Task GetDistribution_MultipleMaterials_ReturnsAll()
    {
        // Arrange
        var material1 = new ReadingMaterialEntity(Guid.NewGuid(), _testLessonId, "Material 1", "Content 1");
        var material2 = new ReadingMaterialEntity(Guid.NewGuid(), _testLessonId, "Material 2", "Content 2");

        var bundle = new DistributionBundle(new[] { material1, material2 }, new List<QuestionEntity>());
        _mockDistributionService.Setup(x => x.GetDistributionBundleAsync(_testDeviceId))
            .ReturnsAsync(bundle);

        // Act
        var result = await _controller.GetDistribution(_testDeviceId.ToString());

        // Assert
        var okResult = Assert.IsType<OkObjectResult>(result);
        Assert.NotNull(okResult.Value);
    }

    #endregion
}

/// <summary>
/// Integration tests for DistributionController JSON serialisation compliance.
/// Verifies AdditionalValidationRules.md Section 1A requirements:
/// - s1A(1): PascalCase field names, SCREAMING_SNAKE_CASE enum values
/// - s1A(2): Windows-only fields excluded from Android DTOs
/// - s1A(3): Polymorphic entities serialised as flat composition-like structures
/// </summary>
public class DistributionControllerSerialisationTests : IClassFixture<CustomWebApplicationFactory>
{
    private readonly CustomWebApplicationFactory _factory;
    private readonly Guid _testDeviceId = Guid.NewGuid();
    private readonly Guid _testMaterialId = Guid.NewGuid();
    private readonly Guid _testLessonId = Guid.NewGuid();

    public DistributionControllerSerialisationTests(CustomWebApplicationFactory factory)
    {
        _factory = factory;
    }

    #region s1A(1) - PascalCase Fields and SCREAMING_SNAKE_CASE Enums

    [Fact]
    public async Task GetDistribution_FieldNames_ArePascalCase()
    {
        // Arrange - Per s1A(1): Fields serialised in PascalCase
        var material = new ReadingMaterialEntity(_testMaterialId, _testLessonId, "Test Material", "Content");
        var questions = new List<QuestionEntity>
        {
            new MultipleChoiceQuestionEntity(Guid.NewGuid(), _testMaterialId, "Question?", new List<string> { "A", "B" }, 0)
        };
        var bundle = new DistributionBundle(new[] { material }, questions);

        var mockDistributionService = new Mock<IDistributionService>();
        mockDistributionService.Setup(x => x.GetDistributionBundleAsync(_testDeviceId))
            .ReturnsAsync(bundle);

        var client = _factory.WithWebHostBuilder(builder =>
        {
            builder.ConfigureServices(services =>
            {
                var descriptor = services.SingleOrDefault(d => d.ServiceType == typeof(IDistributionService));
                if (descriptor != null) services.Remove(descriptor);
                services.AddSingleton(mockDistributionService.Object);
            });
        }).CreateClient();

        // Act
        var response = await client.GetAsync($"/api/v1/distribution/{_testDeviceId}");
        var content = await response.Content.ReadAsStringAsync();

        // Assert - Field names should be PascalCase (not camelCase)
        Assert.Contains("\"MaterialType\"", content);
        Assert.Contains("\"Title\"", content);
        Assert.Contains("\"Content\"", content);
        Assert.Contains("\"Timestamp\"", content);
        Assert.Contains("\"QuestionType\"", content);
        Assert.Contains("\"QuestionText\"", content);
        Assert.Contains("\"Id\"", content);
        
        // Verify NOT camelCase
        Assert.DoesNotContain("\"materialType\"", content);
        Assert.DoesNotContain("\"title\"", content);
        Assert.DoesNotContain("\"content\"", content);
        Assert.DoesNotContain("\"timestamp\"", content);
    }

    [Fact]
    public async Task GetDistribution_EnumValues_AreSCREAMING_SNAKE_CASE()
    {
        // Arrange - Per s1A(1): Enum members serialised in SCREAMING_SNAKE_CASE
        var material = new WorksheetMaterialEntity(_testMaterialId, _testLessonId, "Test Worksheet Title", "Content");
        var questions = new List<QuestionEntity>
        {
            new WrittenAnswerQuestionEntity(Guid.NewGuid(), _testMaterialId, "Question?", "Answer")
        };
        var bundle = new DistributionBundle(new[] { material }, questions);

        var mockDistributionService = new Mock<IDistributionService>();
        mockDistributionService.Setup(x => x.GetDistributionBundleAsync(_testDeviceId))
            .ReturnsAsync(bundle);

        var client = _factory.WithWebHostBuilder(builder =>
        {
            builder.ConfigureServices(services =>
            {
                var descriptor = services.SingleOrDefault(d => d.ServiceType == typeof(IDistributionService));
                if (descriptor != null) services.Remove(descriptor);
                services.AddSingleton(mockDistributionService.Object);
            });
        }).CreateClient();

        // Act
        var response = await client.GetAsync($"/api/v1/distribution/{_testDeviceId}");
        var content = await response.Content.ReadAsStringAsync();

        // Assert - Enum values should be SCREAMING_SNAKE_CASE strings
        Assert.Contains("\"MaterialType\":\"WORKSHEET\"", content);
        Assert.Contains("\"QuestionType\":\"WRITTEN_ANSWER\"", content);
        
        // Verify NOT numeric
        Assert.DoesNotContain("\"MaterialType\":1", content);
        Assert.DoesNotContain("\"QuestionType\":2", content);
    }

    #endregion

    #region s1A(2) - Windows-Only Fields Excluded from Android DTOs

    [Fact]
    public async Task GetDistribution_Materials_ExcludeLessonId()
    {
        // Arrange - Per s1A(2): LessonId is Windows-only per AdditionalValidationRules §2D(1)(a)
        var material = new ReadingMaterialEntity(_testMaterialId, _testLessonId, "Test Material", "Content");
        var bundle = new DistributionBundle(new[] { material }, new List<QuestionEntity>());

        var mockDistributionService = new Mock<IDistributionService>();
        mockDistributionService.Setup(x => x.GetDistributionBundleAsync(_testDeviceId))
            .ReturnsAsync(bundle);

        var client = _factory.WithWebHostBuilder(builder =>
        {
            builder.ConfigureServices(services =>
            {
                var descriptor = services.SingleOrDefault(d => d.ServiceType == typeof(IDistributionService));
                if (descriptor != null) services.Remove(descriptor);
                services.AddSingleton(mockDistributionService.Object);
            });
        }).CreateClient();

        // Act
        var response = await client.GetAsync($"/api/v1/distribution/{_testDeviceId}");
        var content = await response.Content.ReadAsStringAsync();

        // Assert - LessonId should NOT be present in Android DTO
        Assert.DoesNotContain("\"LessonId\"", content);
        Assert.DoesNotContain("\"lessonId\"", content);
    }

    [Fact]
    public async Task GetDistribution_Materials_ExcludeReadingAge()
    {
        // Arrange - Per s1A(2): ReadingAge/ActualAge are Windows-only per AdditionalValidationRules §2D(2)
        var material = new ReadingMaterialEntity(
            _testMaterialId, _testLessonId, "Test Material", "Content",
            readingAge: 12, actualAge: 14);
        var bundle = new DistributionBundle(new[] { material }, new List<QuestionEntity>());

        var mockDistributionService = new Mock<IDistributionService>();
        mockDistributionService.Setup(x => x.GetDistributionBundleAsync(_testDeviceId))
            .ReturnsAsync(bundle);

        var client = _factory.WithWebHostBuilder(builder =>
        {
            builder.ConfigureServices(services =>
            {
                var descriptor = services.SingleOrDefault(d => d.ServiceType == typeof(IDistributionService));
                if (descriptor != null) services.Remove(descriptor);
                services.AddSingleton(mockDistributionService.Object);
            });
        }).CreateClient();

        // Act
        var response = await client.GetAsync($"/api/v1/distribution/{_testDeviceId}");
        var content = await response.Content.ReadAsStringAsync();

        // Assert - ReadingAge and ActualAge should NOT be present
        Assert.DoesNotContain("\"ReadingAge\"", content);
        Assert.DoesNotContain("\"readingAge\"", content);
        Assert.DoesNotContain("\"ActualAge\"", content);
        Assert.DoesNotContain("\"actualAge\"", content);
    }

    [Fact]
    public async Task GetDistribution_Questions_ExcludeMarkScheme()
    {
        // Arrange - Per s1A(2): MarkScheme is Windows-only per AdditionalValidationRules §2E(1)(a)
        var material = new WorksheetMaterialEntity(_testMaterialId, _testLessonId, "Worksheet", "Content");
        var questions = new List<QuestionEntity>
        {
            new WrittenAnswerQuestionEntity(Guid.NewGuid(), _testMaterialId, "Question?", null, markScheme: "Award 1 mark for correct answer")
        };
        var bundle = new DistributionBundle(new[] { material }, questions);

        var mockDistributionService = new Mock<IDistributionService>();
        mockDistributionService.Setup(x => x.GetDistributionBundleAsync(_testDeviceId))
            .ReturnsAsync(bundle);

        var client = _factory.WithWebHostBuilder(builder =>
        {
            builder.ConfigureServices(services =>
            {
                var descriptor = services.SingleOrDefault(d => d.ServiceType == typeof(IDistributionService));
                if (descriptor != null) services.Remove(descriptor);
                services.AddSingleton(mockDistributionService.Object);
            });
        }).CreateClient();

        // Act
        var response = await client.GetAsync($"/api/v1/distribution/{_testDeviceId}");
        var content = await response.Content.ReadAsStringAsync();

        // Assert - MarkScheme should NOT be present in Android DTO
        Assert.DoesNotContain("\"MarkScheme\"", content);
        Assert.DoesNotContain("\"markScheme\"", content);
        Assert.DoesNotContain("Award 1 mark", content);
    }

    #endregion

    #region s1A(3) - Polymorphic Entities as Flat Composition-like Structures

    [Fact]
    public async Task GetDistribution_Materials_SerialiseAsFlatStructure()
    {
        // Arrange - Per s1A(3): Polymorphic MaterialEntity should appear composition-like
        var reading = new ReadingMaterialEntity(Guid.NewGuid(), _testLessonId, "Reading", "Content1");
        var worksheet = new WorksheetMaterialEntity(Guid.NewGuid(), _testLessonId, "Worksheet", "Content2");
        var poll = new PollMaterialEntity(Guid.NewGuid(), _testLessonId, "Poll", "Content3");
        
        var materials = new List<MaterialEntity> { reading, worksheet, poll };
        var bundle = new DistributionBundle(materials, new List<QuestionEntity>());

        var mockDistributionService = new Mock<IDistributionService>();
        mockDistributionService.Setup(x => x.GetDistributionBundleAsync(_testDeviceId))
            .ReturnsAsync(bundle);

        var client = _factory.WithWebHostBuilder(builder =>
        {
            builder.ConfigureServices(services =>
            {
                var descriptor = services.SingleOrDefault(d => d.ServiceType == typeof(IDistributionService));
                if (descriptor != null) services.Remove(descriptor);
                services.AddSingleton(mockDistributionService.Object);
            });
        }).CreateClient();

        // Act
        var response = await client.GetAsync($"/api/v1/distribution/{_testDeviceId}");
        var content = await response.Content.ReadAsStringAsync();

        // Assert - Should have flat structure with MaterialType discriminator
        Assert.Contains("\"READING\"", content);
        Assert.Contains("\"WORKSHEET\"", content);
        Assert.Contains("\"POLL\"", content);
        
        // Should NOT have type discriminator metadata
        Assert.DoesNotContain("\"$type\"", content);
        Assert.DoesNotContain("ReadingMaterialEntity", content);
        Assert.DoesNotContain("WorksheetMaterialEntity", content);
        Assert.DoesNotContain("PollMaterialEntity", content);
    }

    [Fact]
    public async Task GetDistribution_Questions_SerialiseAsFlatStructure()
    {
        // Arrange - Per s1A(3): Polymorphic QuestionEntity should appear composition-like
        var material = new WorksheetMaterialEntity(_testMaterialId, _testLessonId, "Worksheet", "Content");
        var questions = new List<QuestionEntity>
        {
            new MultipleChoiceQuestionEntity(Guid.NewGuid(), _testMaterialId, "MC Question?", new List<string> { "A", "B" }, 0),
            new WrittenAnswerQuestionEntity(Guid.NewGuid(), _testMaterialId, "WA Question?", "Answer")
        };
        var bundle = new DistributionBundle(new[] { material }, questions);

        var mockDistributionService = new Mock<IDistributionService>();
        mockDistributionService.Setup(x => x.GetDistributionBundleAsync(_testDeviceId))
            .ReturnsAsync(bundle);

        var client = _factory.WithWebHostBuilder(builder =>
        {
            builder.ConfigureServices(services =>
            {
                var descriptor = services.SingleOrDefault(d => d.ServiceType == typeof(IDistributionService));
                if (descriptor != null) services.Remove(descriptor);
                services.AddSingleton(mockDistributionService.Object);
            });
        }).CreateClient();

        // Act
        var response = await client.GetAsync($"/api/v1/distribution/{_testDeviceId}");
        var content = await response.Content.ReadAsStringAsync();

        // Assert - Should have flat structure with QuestionType discriminator
        Assert.Contains("\"MULTIPLE_CHOICE\"", content);
        Assert.Contains("\"WRITTEN_ANSWER\"", content);
        
        // Should NOT have type discriminator metadata
        Assert.DoesNotContain("\"$type\"", content);
        Assert.DoesNotContain("MultipleChoiceQuestionEntity", content);
        Assert.DoesNotContain("WrittenAnswerQuestionEntity", content);
    }

    [Fact]
    public async Task GetDistribution_Materials_ContainAllRequiredFields()
    {
        // Arrange - Per Validation Rules §2A: Required fields for MaterialEntity
        var material = new ReadingMaterialEntity(
            _testMaterialId, _testLessonId, "Test Title", "Test Content",
            metadata: "test metadata");
        var bundle = new DistributionBundle(new[] { material }, new List<QuestionEntity>());

        var mockDistributionService = new Mock<IDistributionService>();
        mockDistributionService.Setup(x => x.GetDistributionBundleAsync(_testDeviceId))
            .ReturnsAsync(bundle);

        var client = _factory.WithWebHostBuilder(builder =>
        {
            builder.ConfigureServices(services =>
            {
                var descriptor = services.SingleOrDefault(d => d.ServiceType == typeof(IDistributionService));
                if (descriptor != null) services.Remove(descriptor);
                services.AddSingleton(mockDistributionService.Object);
            });
        }).CreateClient();

        // Act
        var response = await client.GetAsync($"/api/v1/distribution/{_testDeviceId}");
        var content = await response.Content.ReadAsStringAsync();
        var json = JsonDocument.Parse(content);
        var materials = json.RootElement.GetProperty("materials").EnumerateArray().First();

        // Assert - All required fields from Validation Rules §2A should be present
        Assert.True(materials.TryGetProperty("Id", out _), "Id field missing");
        Assert.True(materials.TryGetProperty("MaterialType", out _), "MaterialType field missing");
        Assert.True(materials.TryGetProperty("Title", out _), "Title field missing");
        Assert.True(materials.TryGetProperty("Content", out _), "Content field missing");
        Assert.True(materials.TryGetProperty("Timestamp", out _), "Timestamp field missing");
    }

    [Fact]
    public async Task GetDistribution_Questions_ContainAllRequiredFields()
    {
        // Arrange - Per Validation Rules §2B: Required fields for QuestionEntity
        var material = new WorksheetMaterialEntity(_testMaterialId, _testLessonId, "Worksheet", "Content");
        var questions = new List<QuestionEntity>
        {
            new MultipleChoiceQuestionEntity(Guid.NewGuid(), _testMaterialId, "Question?", new List<string> { "A", "B" }, 0, maxScore: 5)
        };
        var bundle = new DistributionBundle(new[] { material }, questions);

        var mockDistributionService = new Mock<IDistributionService>();
        mockDistributionService.Setup(x => x.GetDistributionBundleAsync(_testDeviceId))
            .ReturnsAsync(bundle);

        var client = _factory.WithWebHostBuilder(builder =>
        {
            builder.ConfigureServices(services =>
            {
                var descriptor = services.SingleOrDefault(d => d.ServiceType == typeof(IDistributionService));
                if (descriptor != null) services.Remove(descriptor);
                services.AddSingleton(mockDistributionService.Object);
            });
        }).CreateClient();

        // Act
        var response = await client.GetAsync($"/api/v1/distribution/{_testDeviceId}");
        var content = await response.Content.ReadAsStringAsync();
        var json = JsonDocument.Parse(content);
        var question = json.RootElement.GetProperty("questions").EnumerateArray().First();

        // Assert - All required fields from Validation Rules §2B should be present
        Assert.True(question.TryGetProperty("Id", out _), "Id field missing");
        Assert.True(question.TryGetProperty("MaterialId", out _), "MaterialId field missing");
        Assert.True(question.TryGetProperty("QuestionType", out _), "QuestionType field missing");
        Assert.True(question.TryGetProperty("QuestionText", out _), "QuestionText field missing");
    }

    #endregion

    #region Timestamp Format Tests

    [Fact]
    public async Task GetDistribution_Materials_TimestampIsUnixLong()
    {
        // Arrange - Per Validation Rules §2A(1)(d): Timestamp should be a Unix timestamp (long)
        var material = new ReadingMaterialEntity(_testMaterialId, _testLessonId, "Test", "Content");
        var bundle = new DistributionBundle(new[] { material }, new List<QuestionEntity>());

        var mockDistributionService = new Mock<IDistributionService>();
        mockDistributionService.Setup(x => x.GetDistributionBundleAsync(_testDeviceId))
            .ReturnsAsync(bundle);

        var client = _factory.WithWebHostBuilder(builder =>
        {
            builder.ConfigureServices(services =>
            {
                var descriptor = services.SingleOrDefault(d => d.ServiceType == typeof(IDistributionService));
                if (descriptor != null) services.Remove(descriptor);
                services.AddSingleton(mockDistributionService.Object);
            });
        }).CreateClient();

        // Act
        var response = await client.GetAsync($"/api/v1/distribution/{_testDeviceId}");
        var content = await response.Content.ReadAsStringAsync();
        var json = JsonDocument.Parse(content);
        var materials = json.RootElement.GetProperty("materials").EnumerateArray().First();

        // Assert - Timestamp should be a number (Unix timestamp), not an ISO string
        var timestamp = materials.GetProperty("Timestamp");
        Assert.Equal(JsonValueKind.Number, timestamp.ValueKind);
        
        // Should be a reasonable Unix timestamp (after year 2020)
        var timestampValue = timestamp.GetInt64();
        Assert.True(timestampValue > 1577836800, "Timestamp should be a Unix timestamp after 2020");
    }

    #endregion
}
