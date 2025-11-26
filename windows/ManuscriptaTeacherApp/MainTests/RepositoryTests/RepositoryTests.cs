using System;
using System.Linq;
using System.Threading.Tasks;
using Microsoft.Data.Sqlite;
using Microsoft.EntityFrameworkCore;
using Xunit;
using Main.Data;
using Main.Models.Entities;
using Main.Models.Entities.Questions;
using Main.Models.Entities.Responses;
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
            var material = new MaterialEntity
            {
                Id = materialId,
                MaterialType = MaterialType.WORKSHEET,
                Title = "Test Material",
                Content = "Content",
                Timestamp = DateTimeOffset.UtcNow.ToUnixTimeSeconds(),
                Synced = false
            };
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
            var response = new MultipleChoiceResponseEntity(responseId, questionId, 1, null, true);
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
            var material = new MaterialEntity
            {
                Id = materialId,
                MaterialType = MaterialType.QUIZ,
                Title = "Original Title",
                Content = "Original Content",
                Timestamp = 1000,
                Synced = false
            };
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
            material!.Title = "Updated Title";
            await mRepo.UpdateAsync(material);

            // Update question
            var updatedQuestion = new TrueFalseQuestionEntity(questionId, materialId, "Updated?", false);
            await qRepo.UpdateAsync(updatedQuestion);
        }

        using (var ctx = new MainDbContext(options))
        {
            var mRepo = new EfMaterialRepository(ctx);
            var qRepo = new EfQuestionRepository(ctx);

            var material = await mRepo.GetByIdAsync(materialId);
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

            var material = new MaterialEntity
            {
                Id = materialId,
                MaterialType = MaterialType.READING,
                Title = "To Delete",
                Content = "Content",
                Timestamp = 1000,
                Synced = false
            };
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
}
