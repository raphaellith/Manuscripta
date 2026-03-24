using System;
using System.Collections.Generic;
using System.Linq;
using System.Threading.Tasks;
using Main.Data;
using Main.Models.Dtos;
using Main.Models.Entities;
using Main.Models.Enums;
using Main.Services;
using Main.Services.GenAI;
using Microsoft.EntityFrameworkCore;
using Moq;
using Xunit;

namespace MainTests.ServicesTests.GenAI;

/// <summary>
/// Spec coverage: GenAISpec Sections 3F-3G (validation and warnings).
/// See docs/specifications/GenAISpec.md.
/// </summary>
public class OutputValidationServiceTests
{
    private static MainDbContext BuildDbContext()
    {
        var options = new DbContextOptionsBuilder<MainDbContext>()
            .UseInMemoryDatabase(Guid.NewGuid().ToString())
            .Options;
        return new MainDbContext(options);
    }

    [Fact]
    public async Task ValidateAndRefineAsync_NoWarnings_ReturnsContentUnchanged()
    {
        using var dbContext = BuildDbContext();
        var fileService = new Mock<IFileService>();
        var service = new OutputValidationService(new OllamaClientService("http://localhost:11434"), dbContext, fileService.Object);

        var content = "# Title\nSimple content without markers.";

        var result = await service.ValidateAndRefineAsync(content, "model", useFallback: true);

        Assert.Equal(content, result.Content);
        Assert.Null(result.Warnings);
    }

    /// <summary>
    /// Spec coverage: GenAISpec Section 3F(5) (deterministic fixes for markers/headers/blocks).
    /// See docs/specifications/GenAISpec.md.
    /// </summary>
    [Fact]
    public async Task ValidateAndRefineAsync_DeterministicFixes_NormalizeAndRemoveInvalidReferences()
    {
        using var dbContext = BuildDbContext();
        var materialId = Guid.NewGuid();
        var lessonId = Guid.NewGuid();

        dbContext.Materials.Add(new MaterialDataEntity
        {
            Id = materialId,
            LessonId = lessonId,
            MaterialType = MaterialType.WORKSHEET,
            Title = "Title",
            Content = "Content",
            Timestamp = DateTime.UtcNow
        });

        var validQuestionId = Guid.NewGuid();
        var missingQuestionId = Guid.NewGuid();

        dbContext.Questions.Add(new QuestionDataEntity
        {
            Id = validQuestionId,
            MaterialId = materialId,
            QuestionText = "Question",
            QuestionType = QuestionType.WRITTEN_ANSWER
        });

        var validAttachmentId = Guid.NewGuid();
        var validPdfId = Guid.NewGuid();
        var missingAttachmentId = Guid.NewGuid();
        var missingPdfId = Guid.NewGuid();

        dbContext.Attachments.AddRange(
            new AttachmentEntity(validAttachmentId, materialId, "image", "png"),
            new AttachmentEntity(validPdfId, materialId, "handout", "pdf")
        );

        await dbContext.SaveChangesAsync();

        var fileService = new Mock<IFileService>();
        var fileExists = new HashSet<Guid> { validAttachmentId, validPdfId };

        fileService.Setup(fs => fs.GetAttachmentFilePath(It.IsAny<Guid>(), It.IsAny<string>()))
            .Returns<Guid, string>((id, ext) => $"/tmp/{id}.{ext}");
        fileService.Setup(fs => fs.FileExists(It.IsAny<string>()))
            .Returns<string>(path => fileExists.Any(id => path.Contains(id.ToString(), StringComparison.Ordinal)));

        var service = new OutputValidationService(new OllamaClientService("http://localhost:11434"), dbContext, fileService.Object);

        var content = $@"#### Too Deep\n\n" +
                      $"!!! question id=\"{validQuestionId}\"\n" +
                      $"!!! question id=\"{missingQuestionId}\"\n" +
                      "!!! question 1234-invalid\n" +
                      $"!!! pdf id=\"{validPdfId}\"\n" +
                      $"!!! pdf id=\"{missingPdfId}\"\n" +
                      $"![img](/attachments/{validAttachmentId})\n" +
                      $"![img](/attachments/{missingAttachmentId})\n" +
                      "```csharp\nvar x = 1;\n";

        var result = await service.ValidateAndRefineAsync(content, "model", useFallback: false);

        Assert.Contains("### Too Deep", result.Content);

        // Valid markers and attachment references may be normalized or removed by deterministic fixes,
        // so we only assert that invalid/missing references are not present.
        Assert.DoesNotContain($"!!! question id=\"{missingQuestionId}\"", result.Content);
        Assert.DoesNotContain("!!! question 1234-invalid", result.Content);
        Assert.DoesNotContain($"!!! pdf id=\"{missingPdfId}\"", result.Content);

        Assert.DoesNotContain($"![img](/attachments/{missingAttachmentId})", result.Content);
        Assert.EndsWith("```", result.Content.TrimEnd());

        Assert.Null(result.Warnings);
    }

    /// <summary>
    /// Spec coverage: GenAISpec §3B(3)(g) — question-draft markers must survive validation
    /// so that QuestionExtractionService can process them when the material is persisted.
    /// See docs/specifications/GenAISpec.md §3F(5)(c) and §3F(5)(e).
    /// </summary>
    [Fact]
    public async Task ValidateAndRefineAsync_QuestionDraftMarkers_ArePreservedByDeterministicFixes()
    {
        using var dbContext = BuildDbContext();
        var fileService = new Mock<IFileService>();
        var service = new OutputValidationService(new OllamaClientService("http://localhost:11434"), dbContext, fileService.Object);

        var content = "# Worksheet\n\n" +
                      "!!! question-draft type=\"MULTIPLE_CHOICE\"\n" +
                      "    text: \"Which word is used for the letter A in the NATO Phonetic Alphabet?\"\n" +
                      "    options:\n" +
                      "      - \"Bravo\"\n" +
                      "      - \"Alpha\"\n" +
                      "      - \"Echo\"\n" +
                      "    correct: 1\n" +
                      "    max_score: 1\n\n" +
                      "!!! question-draft type=\"WRITTEN_ANSWER\"\n" +
                      "    text: \"Explain the purpose of the NATO Phonetic Alphabet.\"\n" +
                      "    mark_scheme: \"Award marks for clarity and accuracy\"\n" +
                      "    max_score: 4";

        var result = await service.ValidateAndRefineAsync(content, "qwen3:8b", useFallback: false);

        // question-draft markers must be fully preserved
        Assert.Contains("!!! question-draft type=\"MULTIPLE_CHOICE\"", result.Content);
        Assert.Contains("!!! question-draft type=\"WRITTEN_ANSWER\"", result.Content);
        Assert.Contains("text: \"Which word is used for the letter A in the NATO Phonetic Alphabet?\"", result.Content);
        Assert.Contains("text: \"Explain the purpose of the NATO Phonetic Alphabet.\"", result.Content);
        Assert.Contains("options:", result.Content);
        Assert.Contains("- \"Bravo\"", result.Content);
        Assert.Contains("- \"Alpha\"", result.Content);
        Assert.Contains("- \"Echo\"", result.Content);
        Assert.Contains("correct: 1", result.Content);
        Assert.Contains("max_score: 1", result.Content);
        Assert.Contains("mark_scheme: \"Award marks for clarity and accuracy\"", result.Content);
        Assert.Contains("max_score: 4", result.Content);
    }

    /// <summary>
    /// Spec coverage: GenAISpec §3F(5)(c) and §3F(5)(e) — deterministic fixes must still
    /// operate on genuine malformed question markers while preserving question-draft markers.
    /// See docs/specifications/GenAISpec.md.
    /// </summary>
    [Fact]
    public async Task ValidateAndRefineAsync_MixedQuestionAndQuestionDraftMarkers_OnlyFixesMalformedQuestionMarkers()
    {
        using var dbContext = BuildDbContext();
        var materialId = Guid.NewGuid();
        var lessonId = Guid.NewGuid();

        dbContext.Materials.Add(new MaterialDataEntity
        {
            Id = materialId,
            LessonId = lessonId,
            MaterialType = MaterialType.WORKSHEET,
            Title = "Title",
            Content = "Content",
            Timestamp = DateTime.UtcNow
        });

        var validQuestionId = Guid.NewGuid();
        dbContext.Questions.Add(new QuestionDataEntity
        {
            Id = validQuestionId,
            MaterialId = materialId,
            QuestionText = "Existing question",
            QuestionType = QuestionType.WRITTEN_ANSWER
        });

        await dbContext.SaveChangesAsync();

        var fileService = new Mock<IFileService>();
        var service = new OutputValidationService(new OllamaClientService("http://localhost:11434"), dbContext, fileService.Object);

        var content = $"!!! question id=\"{validQuestionId}\"\n\n" +
                      "!!! question-draft type=\"MULTIPLE_CHOICE\"\n" +
                      "    text: \"Test question\"\n" +
                      "    options:\n" +
                      "      - \"A\"\n" +
                      "      - \"B\"\n" +
                      "    correct: 0\n" +
                      "    max_score: 1";

        var result = await service.ValidateAndRefineAsync(content, "qwen3:8b", useFallback: false);

        // Valid question marker should be preserved
        Assert.Contains($"!!! question id=\"{validQuestionId}\"", result.Content);
        // question-draft marker must be fully preserved
        Assert.Contains("!!! question-draft type=\"MULTIPLE_CHOICE\"", result.Content);
        Assert.Contains("text: \"Test question\"", result.Content);
    }
}
