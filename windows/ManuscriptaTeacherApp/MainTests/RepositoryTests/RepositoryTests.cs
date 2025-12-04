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

    [Fact]
    public async Task Repositories_CanAddAndRetrieveEntities()
    {
        using var connection = new SqliteConnection("DataSource=:memory:");
        connection.Open();

        var options = CreateSqliteInMemoryOptions(connection);

        var materialId = Guid.NewGuid();
        var questionId = Guid.NewGuid();
        var responseId = Guid.NewGuid();

        // Create schema and use repositories to add data
        using (var ctx = new MainDbContext(options))
        {
            ctx.Database.EnsureCreated();

            var mRepo = new EfMaterialRepository(ctx);
            var qRepo = new EfQuestionRepository(ctx);
            var rRepo = new EfResponseRepository(ctx, qRepo);

            // Add material
            var material = new WorksheetMaterialEntity(
                materialId,
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

            // Add response using polymorphic entity
            var response = new MultipleChoiceResponseEntity(responseId, questionId, Guid.NewGuid(), 1, null, true);
            await rRepo.AddAsync(response);
        }

        // Use repositories to query
        using (var ctx = new MainDbContext(options))
        {
            var mRepo = new EfMaterialRepository(ctx);
            var qRepo = new EfQuestionRepository(ctx);
            var rRepo = new EfResponseRepository(ctx, qRepo);

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

            // Test response retrieval
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

        using (var ctx = new MainDbContext(options))
        {
            ctx.Database.EnsureCreated();

            var mRepo = new EfMaterialRepository(ctx);
            var qRepo = new EfQuestionRepository(ctx);

            // Add material
            var material = new QuizMaterialEntity(
                materialId,
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
                "Updated Title",
                material!.Content,
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

        using (var ctx = new MainDbContext(options))
        {
            ctx.Database.EnsureCreated();

            var mRepo = new EfMaterialRepository(ctx);

            var material = new ReadingMaterialEntity(
                materialId,
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

    #region Session Repository Tests

    [Fact]
    public async Task SessionRepository_CanAddAndRetrieveSession()
    {
        using var connection = new SqliteConnection("DataSource=:memory:");
        connection.Open();

        var options = CreateSqliteInMemoryOptions(connection);

        var materialId = Guid.NewGuid();
        var sessionId = Guid.NewGuid();

        using (var ctx = new MainDbContext(options))
        {
            ctx.Database.EnsureCreated();

            var mRepo = new EfMaterialRepository(ctx);
            var sRepo = new EfSessionRepository(ctx);

            // Add material first (session requires material)
            var material = new WorksheetMaterialEntity(materialId, "Test Material", "Content");
            await mRepo.AddAsync(material);

            // Add session
            var session = new SessionEntity(
                sessionId,
                materialId,
                DateTime.UtcNow,
                SessionStatus.ACTIVE,
                Guid.NewGuid()
            );
            await sRepo.AddAsync(session);
        }

        using (var ctx = new MainDbContext(options))
        {
            var sRepo = new EfSessionRepository(ctx);

            var session = await sRepo.GetByIdAsync(sessionId);
            Assert.NotNull(session);
            Assert.Equal(sessionId, session!.Id);
            Assert.Equal(materialId, session.MaterialId);
            Assert.Equal(SessionStatus.ACTIVE, session.SessionStatus);
        }
    }

    [Fact]
    public async Task SessionRepository_CanRetrieveAllSessions()
    {
        using var connection = new SqliteConnection("DataSource=:memory:");
        connection.Open();

        var options = CreateSqliteInMemoryOptions(connection);

        var materialId = Guid.NewGuid();

        using (var ctx = new MainDbContext(options))
        {
            ctx.Database.EnsureCreated();

            var mRepo = new EfMaterialRepository(ctx);
            var sRepo = new EfSessionRepository(ctx);

            var material = new WorksheetMaterialEntity(materialId, "Test Material", "Content");
            await mRepo.AddAsync(material);

            // Add multiple sessions
            var session1 = new SessionEntity(Guid.NewGuid(), materialId, DateTime.UtcNow, SessionStatus.ACTIVE, Guid.NewGuid());
            var session2 = new SessionEntity(Guid.NewGuid(), materialId, DateTime.UtcNow.AddHours(-1), SessionStatus.COMPLETED, Guid.NewGuid(), DateTime.UtcNow);
            
            await sRepo.AddAsync(session1);
            await sRepo.AddAsync(session2);
        }

        using (var ctx = new MainDbContext(options))
        {
            var sRepo = new EfSessionRepository(ctx);

            var sessions = (await sRepo.GetAllAsync()).ToList();
            Assert.Equal(2, sessions.Count);
        }
    }

    [Fact]
    public async Task SessionRepository_CanUpdateSession()
    {
        using var connection = new SqliteConnection("DataSource=:memory:");
        connection.Open();

        var options = CreateSqliteInMemoryOptions(connection);

        var materialId = Guid.NewGuid();
        var sessionId = Guid.NewGuid();
        var deviceId = Guid.NewGuid();
        var startTime = DateTime.UtcNow.AddHours(-1);

        using (var ctx = new MainDbContext(options))
        {
            ctx.Database.EnsureCreated();

            var mRepo = new EfMaterialRepository(ctx);
            var sRepo = new EfSessionRepository(ctx);

            var material = new WorksheetMaterialEntity(materialId, "Test Material", "Content");
            await mRepo.AddAsync(material);

            var session = new SessionEntity(sessionId, materialId, startTime, SessionStatus.ACTIVE, deviceId);
            await sRepo.AddAsync(session);
        }

        using (var ctx = new MainDbContext(options))
        {
            var sRepo = new EfSessionRepository(ctx);

            var endTime = DateTime.UtcNow;
            var updatedSession = new SessionEntity(sessionId, materialId, startTime, SessionStatus.COMPLETED, deviceId, endTime);
            await sRepo.UpdateAsync(updatedSession);
        }

        using (var ctx = new MainDbContext(options))
        {
            var sRepo = new EfSessionRepository(ctx);

            var session = await sRepo.GetByIdAsync(sessionId);
            Assert.NotNull(session);
            Assert.Equal(SessionStatus.COMPLETED, session!.SessionStatus);
            Assert.NotNull(session.EndTime);
        }
    }

    [Fact]
    public async Task SessionRepository_CanDeleteSession()
    {
        using var connection = new SqliteConnection("DataSource=:memory:");
        connection.Open();

        var options = CreateSqliteInMemoryOptions(connection);

        var materialId = Guid.NewGuid();
        var sessionId = Guid.NewGuid();

        using (var ctx = new MainDbContext(options))
        {
            ctx.Database.EnsureCreated();

            var mRepo = new EfMaterialRepository(ctx);
            var sRepo = new EfSessionRepository(ctx);

            var material = new WorksheetMaterialEntity(materialId, "Test Material", "Content");
            await mRepo.AddAsync(material);

            var session = new SessionEntity(sessionId, materialId, DateTime.UtcNow, SessionStatus.ACTIVE, Guid.NewGuid());
            await sRepo.AddAsync(session);
        }

        using (var ctx = new MainDbContext(options))
        {
            var sRepo = new EfSessionRepository(ctx);
            await sRepo.DeleteAsync(sessionId);
        }

        using (var ctx = new MainDbContext(options))
        {
            var sRepo = new EfSessionRepository(ctx);
            var session = await sRepo.GetByIdAsync(sessionId);
            Assert.Null(session);
        }
    }

    [Fact]
    public async Task SessionRepository_GetByIdAsync_NonExistingSession_ReturnsNull()
    {
        using var connection = new SqliteConnection("DataSource=:memory:");
        connection.Open();

        var options = CreateSqliteInMemoryOptions(connection);

        using (var ctx = new MainDbContext(options))
        {
            ctx.Database.EnsureCreated();

            var sRepo = new EfSessionRepository(ctx);
            var session = await sRepo.GetByIdAsync(Guid.NewGuid());
            Assert.Null(session);
        }
    }

    [Fact]
    public async Task SessionRepository_AllSessionStatuses_PersistCorrectly()
    {
        using var connection = new SqliteConnection("DataSource=:memory:");
        connection.Open();

        var options = CreateSqliteInMemoryOptions(connection);

        var materialId = Guid.NewGuid();
        var activeId = Guid.NewGuid();
        var pausedId = Guid.NewGuid();
        var completedId = Guid.NewGuid();
        var cancelledId = Guid.NewGuid();

        using (var ctx = new MainDbContext(options))
        {
            ctx.Database.EnsureCreated();

            var mRepo = new EfMaterialRepository(ctx);
            var sRepo = new EfSessionRepository(ctx);

            var material = new WorksheetMaterialEntity(materialId, "Test Material", "Content");
            await mRepo.AddAsync(material);

            var endTime = DateTime.UtcNow;

            await sRepo.AddAsync(new SessionEntity(activeId, materialId, DateTime.UtcNow, SessionStatus.ACTIVE, Guid.NewGuid()));
            await sRepo.AddAsync(new SessionEntity(pausedId, materialId, DateTime.UtcNow.AddHours(-1), SessionStatus.PAUSED, Guid.NewGuid(), endTime));
            await sRepo.AddAsync(new SessionEntity(completedId, materialId, DateTime.UtcNow.AddHours(-2), SessionStatus.COMPLETED, Guid.NewGuid(), endTime));
            await sRepo.AddAsync(new SessionEntity(cancelledId, materialId, DateTime.UtcNow.AddHours(-3), SessionStatus.CANCELLED, Guid.NewGuid(), endTime));
        }

        using (var ctx = new MainDbContext(options))
        {
            var sRepo = new EfSessionRepository(ctx);

            var active = await sRepo.GetByIdAsync(activeId);
            var paused = await sRepo.GetByIdAsync(pausedId);
            var completed = await sRepo.GetByIdAsync(completedId);
            var cancelled = await sRepo.GetByIdAsync(cancelledId);

            Assert.Equal(SessionStatus.ACTIVE, active!.SessionStatus);
            Assert.Equal(SessionStatus.PAUSED, paused!.SessionStatus);
            Assert.Equal(SessionStatus.COMPLETED, completed!.SessionStatus);
            Assert.Equal(SessionStatus.CANCELLED, cancelled!.SessionStatus);
        }
    }

    #endregion
}
