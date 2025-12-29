using System;
using System.Linq;
using System.Threading.Tasks;
using Microsoft.Data.Sqlite;
using Microsoft.EntityFrameworkCore;
using Xunit;
using Main.Data;
using Main.Models.Entities;
using Main.Models.Entities.Materials;
using Main.Models.Entities.Questions;
using Main.Models.Entities.Responses;
using Main.Models.Entities.Sessions;
using Main.Models.Enums;
using Main.Services.Repositories;

namespace MainTests.RepositoryTests;

public class RepositoryTests
{
    private DbContextOptions<MainDbContext> CreateSqliteInMemoryOptions(SqliteConnection connection)
    {
        return new DbContextOptionsBuilder<MainDbContext>()
            .UseSqlite(connection)
            .Options;
    }

    /// <summary>
    /// Creates the required hierarchy entities for material tests (UnitCollection -> Unit -> Lesson).
    /// </summary>
    private async Task<Guid> CreateLessonHierarchy(MainDbContext ctx)
    {
        var unitCollectionId = Guid.NewGuid();
        var unitId = Guid.NewGuid();
        var lessonId = Guid.NewGuid();

        var unitCollection = new UnitCollectionEntity(unitCollectionId, "Test Unit Collection");
        ctx.UnitCollections.Add(unitCollection);
        
        var unit = new UnitEntity(unitId, unitCollectionId, "Test Unit", new List<string>());
        ctx.Units.Add(unit);
        
        var lesson = new LessonEntity(lessonId, unitId, "Test Lesson", "Test Description");
        ctx.Lessons.Add(lesson);
        
        await ctx.SaveChangesAsync();
        return lessonId;
    }

    [Fact]
    public async Task Repositories_CanAddAndRetrieveEntities()
    {
        using var connection = new SqliteConnection("DataSource=:memory:");
        connection.Open();

        var options = CreateSqliteInMemoryOptions(connection);

        var materialId = Guid.NewGuid();
        var questionId = Guid.NewGuid();
        var responseId = Guid.NewGuid();
        Guid lessonId;

        // In-memory response repository (per PersistenceAndCascadingRules.md §1(2))
        var rRepo = new InMemoryResponseRepository();

        // Create schema and use repositories to add data
        using (var ctx = new MainDbContext(options))
        {
            ctx.Database.EnsureCreated();
            
            // Create parent hierarchy for FK constraint
            lessonId = await CreateLessonHierarchy(ctx);

            var mRepo = new EfMaterialRepository(ctx);
            var qRepo = new EfQuestionRepository(ctx);

            // Add material
            var material = new WorksheetMaterialEntity(
                materialId,
                lessonId,
                "Test Material",
                "Content"
            );
            await mRepo.AddAsync(material);

            // Add question using polymorphic entity
            var question = new MultipleChoiceQuestionEntity(
                questionId,
                materialId,
                "What is 2 + 2?",
                new System.Collections.Generic.List<string> { "3", "4", "5" },
                1
            );
            await qRepo.AddAsync(question);

            // Add response using polymorphic entity (in-memory, not persisted to DB)
            var response = new MultipleChoiceResponseEntity(responseId, questionId, Guid.NewGuid(), 1, null, true);
            await rRepo.AddAsync(response);
        }

        // Use repositories to query
        using (var ctx = new MainDbContext(options))
        {
            var mRepo = new EfMaterialRepository(ctx);
            var qRepo = new EfQuestionRepository(ctx);

            // Test material retrieval
            var m = await mRepo.GetByIdAsync(materialId);
            Assert.NotNull(m);
            Assert.IsType<WorksheetMaterialEntity>(m);
            Assert.Equal("Test Material", m!.Title);

            // Test question retrieval
            var qs = (await qRepo.GetByMaterialIdAsync(materialId)).ToList();
            Assert.Single(qs);
            Assert.IsType<MultipleChoiceQuestionEntity>(qs[0]);

            var q = await qRepo.GetByIdAsync(questionId);
            Assert.NotNull(q);
            Assert.IsType<MultipleChoiceQuestionEntity>(q);
            var mcQuestion = (MultipleChoiceQuestionEntity)q!;
            Assert.Equal(3, mcQuestion.Options.Count);
            Assert.Equal(1, mcQuestion.CorrectAnswerIndex);

            // Test response retrieval (from in-memory repository)
            var rs = (await rRepo.GetByQuestionIdAsync(questionId)).ToList();
            Assert.Single(rs);
            Assert.IsType<MultipleChoiceResponseEntity>(rs[0]);

            var r = await rRepo.GetByIdAsync(responseId);
            Assert.NotNull(r);
            Assert.IsType<MultipleChoiceResponseEntity>(r);
            var mcResponse = (MultipleChoiceResponseEntity)r!;
            Assert.Equal(1, mcResponse.AnswerIndex);
            Assert.True(mcResponse.IsCorrect);
        }
    }

    [Fact]
    public async Task Repositories_CanUpdateEntities()
    {
        using var connection = new SqliteConnection("DataSource=:memory:");
        connection.Open();

        var options = CreateSqliteInMemoryOptions(connection);

        var materialId = Guid.NewGuid();
        var questionId = Guid.NewGuid();
        Guid lessonId;

        using (var ctx = new MainDbContext(options))
        {
            ctx.Database.EnsureCreated();
            
            // Create parent hierarchy for FK constraint
            lessonId = await CreateLessonHierarchy(ctx);

            var mRepo = new EfMaterialRepository(ctx);
            var qRepo = new EfQuestionRepository(ctx);

            // Add material
            var material = new QuizMaterialEntity(
                materialId,
                lessonId,
                "Original Title",
                "Original Content"
            );
            await mRepo.AddAsync(material);

            // Add question
            var question = new TrueFalseQuestionEntity(questionId, materialId, "Original?", true);
            await qRepo.AddAsync(question);
        }

        using (var ctx = new MainDbContext(options))
        {
            var mRepo = new EfMaterialRepository(ctx);
            var qRepo = new EfQuestionRepository(ctx);

            // Update material
            var material = await mRepo.GetByIdAsync(materialId);
            Assert.NotNull(material);
            Assert.IsType<QuizMaterialEntity>(material);
            
            var updatedMaterial = new QuizMaterialEntity(
                materialId,
                material!.LessonId,
                "Updated Title",
                material.Content,
                material.Timestamp
            );
            await mRepo.UpdateAsync(updatedMaterial);

            // Update question
            var updatedQuestion = new TrueFalseQuestionEntity(questionId, materialId, "Updated?", false);
            await qRepo.UpdateAsync(updatedQuestion);
        }

        using (var ctx = new MainDbContext(options))
        {
            var mRepo = new EfMaterialRepository(ctx);
            var qRepo = new EfQuestionRepository(ctx);

            var material = await mRepo.GetByIdAsync(materialId);
            Assert.NotNull(material);
            Assert.Equal("Updated Title", material!.Title);

            var question = await qRepo.GetByIdAsync(questionId) as TrueFalseQuestionEntity;
            Assert.NotNull(question);
            Assert.Equal("Updated?", question!.QuestionText);
            Assert.False(question.CorrectAnswer);
        }
    }

    [Fact]
    public async Task Repositories_CanDeleteEntities()
    {
        using var connection = new SqliteConnection("DataSource=:memory:");
        connection.Open();

        var options = CreateSqliteInMemoryOptions(connection);

        var materialId = Guid.NewGuid();
        Guid lessonId;

        using (var ctx = new MainDbContext(options))
        {
            ctx.Database.EnsureCreated();
            
            // Create parent hierarchy for FK constraint
            lessonId = await CreateLessonHierarchy(ctx);

            var mRepo = new EfMaterialRepository(ctx);

            var material = new ReadingMaterialEntity(
                materialId,
                lessonId,
                "To Delete",
                "Content"
            );
            await mRepo.AddAsync(material);
        }

        using (var ctx = new MainDbContext(options))
        {
            var mRepo = new EfMaterialRepository(ctx);
            await mRepo.DeleteAsync(materialId);
        }

        using (var ctx = new MainDbContext(options))
        {
            var mRepo = new EfMaterialRepository(ctx);
            var material = await mRepo.GetByIdAsync(materialId);
            Assert.Null(material);
        }
    }

    #region Session Repository Tests (In-Memory per §1(2))

    [Fact]
    public async Task SessionRepository_CanAddAndRetrieveSession()
    {
        // Per PersistenceAndCascadingRules.md §1(2), sessions use in-memory storage
        var sRepo = new InMemorySessionRepository();

        var materialId = Guid.NewGuid();
        var sessionId = Guid.NewGuid();

        // Add session
        var session = new SessionEntity(
            sessionId,
            materialId,
            DateTime.UtcNow,
            SessionStatus.ACTIVE,
            Guid.NewGuid()
        );
        await sRepo.AddAsync(session);

        var retrieved = await sRepo.GetByIdAsync(sessionId);
        Assert.NotNull(retrieved);
        Assert.Equal(sessionId, retrieved!.Id);
        Assert.Equal(materialId, retrieved.MaterialId);
        Assert.Equal(SessionStatus.ACTIVE, retrieved.SessionStatus);
    }

    [Fact]
    public async Task SessionRepository_CanRetrieveAllSessions()
    {
        var sRepo = new InMemorySessionRepository();

        var materialId = Guid.NewGuid();

        // Add multiple sessions
        var session1 = new SessionEntity(Guid.NewGuid(), materialId, DateTime.UtcNow, SessionStatus.ACTIVE, Guid.NewGuid());
        var session2 = new SessionEntity(Guid.NewGuid(), materialId, DateTime.UtcNow.AddHours(-1), SessionStatus.COMPLETED, Guid.NewGuid(), DateTime.UtcNow);
        
        await sRepo.AddAsync(session1);
        await sRepo.AddAsync(session2);

        var sessions = (await sRepo.GetAllAsync()).ToList();
        Assert.Equal(2, sessions.Count);
    }

    [Fact]
    public async Task SessionRepository_CanUpdateSession()
    {
        var sRepo = new InMemorySessionRepository();

        var materialId = Guid.NewGuid();
        var sessionId = Guid.NewGuid();
        var deviceId = Guid.NewGuid();
        var startTime = DateTime.UtcNow.AddHours(-1);

        var session = new SessionEntity(sessionId, materialId, startTime, SessionStatus.ACTIVE, deviceId);
        await sRepo.AddAsync(session);

        var endTime = DateTime.UtcNow;
        var updatedSession = new SessionEntity(sessionId, materialId, startTime, SessionStatus.COMPLETED, deviceId, endTime);
        await sRepo.UpdateAsync(updatedSession);

        var retrieved = await sRepo.GetByIdAsync(sessionId);
        Assert.NotNull(retrieved);
        Assert.Equal(SessionStatus.COMPLETED, retrieved!.SessionStatus);
        Assert.NotNull(retrieved.EndTime);
    }

    [Fact]
    public async Task SessionRepository_CanDeleteSession()
    {
        var sRepo = new InMemorySessionRepository();

        var materialId = Guid.NewGuid();
        var sessionId = Guid.NewGuid();

        var session = new SessionEntity(sessionId, materialId, DateTime.UtcNow, SessionStatus.ACTIVE, Guid.NewGuid());
        await sRepo.AddAsync(session);

        await sRepo.DeleteAsync(sessionId);

        var retrieved = await sRepo.GetByIdAsync(sessionId);
        Assert.Null(retrieved);
    }

    [Fact]
    public async Task SessionRepository_GetByIdAsync_NonExistingSession_ReturnsNull()
    {
        var sRepo = new InMemorySessionRepository();
        var session = await sRepo.GetByIdAsync(Guid.NewGuid());
        Assert.Null(session);
    }

    [Fact]
    public async Task SessionRepository_AllSessionStatuses_WorkCorrectly()
    {
        var sRepo = new InMemorySessionRepository();

        var materialId = Guid.NewGuid();
        var activeId = Guid.NewGuid();
        var pausedId = Guid.NewGuid();
        var completedId = Guid.NewGuid();
        var cancelledId = Guid.NewGuid();

        var endTime = DateTime.UtcNow;

        await sRepo.AddAsync(new SessionEntity(activeId, materialId, DateTime.UtcNow, SessionStatus.ACTIVE, Guid.NewGuid()));
        await sRepo.AddAsync(new SessionEntity(pausedId, materialId, DateTime.UtcNow.AddHours(-1), SessionStatus.PAUSED, Guid.NewGuid(), endTime));
        await sRepo.AddAsync(new SessionEntity(completedId, materialId, DateTime.UtcNow.AddHours(-2), SessionStatus.COMPLETED, Guid.NewGuid(), endTime));
        await sRepo.AddAsync(new SessionEntity(cancelledId, materialId, DateTime.UtcNow.AddHours(-3), SessionStatus.CANCELLED, Guid.NewGuid(), endTime));

        var active = await sRepo.GetByIdAsync(activeId);
        var paused = await sRepo.GetByIdAsync(pausedId);
        var completed = await sRepo.GetByIdAsync(completedId);
        var cancelled = await sRepo.GetByIdAsync(cancelledId);

        Assert.Equal(SessionStatus.ACTIVE, active!.SessionStatus);
        Assert.Equal(SessionStatus.PAUSED, paused!.SessionStatus);
        Assert.Equal(SessionStatus.COMPLETED, completed!.SessionStatus);
        Assert.Equal(SessionStatus.CANCELLED, cancelled!.SessionStatus);
    }

    #endregion

    #region Response Repository Tests (In-Memory per §1(2))

    [Fact]
    public async Task ResponseRepository_DeleteByQuestionId_RemovesAllRelatedResponses()
    {
        // Per PersistenceAndCascadingRules.md §2(2): Deletion of a question must delete any responses associated with it
        var rRepo = new InMemoryResponseRepository();

        var questionId = Guid.NewGuid();
        var otherQuestionId = Guid.NewGuid();

        // Add responses to the question
        await rRepo.AddAsync(new MultipleChoiceResponseEntity(Guid.NewGuid(), questionId, Guid.NewGuid(), 1, null, true));
        await rRepo.AddAsync(new MultipleChoiceResponseEntity(Guid.NewGuid(), questionId, Guid.NewGuid(), 2, null, false));
        await rRepo.AddAsync(new TrueFalseResponseEntity(Guid.NewGuid(), otherQuestionId, Guid.NewGuid(), true, null, true));

        // Delete responses by question ID
        await rRepo.DeleteByQuestionIdAsync(questionId);

        // Verify responses for deleted question are gone
        var responsesForQuestion = (await rRepo.GetByQuestionIdAsync(questionId)).ToList();
        Assert.Empty(responsesForQuestion);

        // Verify responses for other question are still there
        var responsesForOther = (await rRepo.GetByQuestionIdAsync(otherQuestionId)).ToList();
        Assert.Single(responsesForOther);
    }

    #endregion
}
