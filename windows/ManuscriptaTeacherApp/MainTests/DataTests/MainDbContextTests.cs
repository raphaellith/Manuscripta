using System;
using System.Linq;
using Microsoft.EntityFrameworkCore;
using Xunit;
using Main.Data;
using Main.Models.Entities;
using Main.Models.Enums;

namespace MainTests;

public class MainDbContextTests
{
    private DbContextOptions<MainDbContext> CreateInMemoryOptions(string dbName)
    {
        return new DbContextOptionsBuilder<MainDbContext>()
            .UseInMemoryDatabase(dbName)
            .Options;
    }

    [Fact]
    public void CanAddAndRetrieve_MaterialWithRelations()
    {
        var options = CreateInMemoryOptions("test_db_1");

        using (var ctx = new MainDbContext(options))
        {
            var materialId = Guid.NewGuid();
            var questionId = Guid.NewGuid();
            var responseId = Guid.NewGuid();

            var material = new MaterialDataEntity
            {
                Id = materialId,
                MaterialType = MaterialType.WORKSHEET,
                Title = "Test Worksheet",
                Content = "Worksheet Content",
                Timestamp = DateTime.UtcNow,
                Synced = false
            };

            ctx.Materials.Add(material);
            ctx.SaveChanges();

            var question = new QuestionDataEntity
            {
                Id = questionId,
                QuestionText = "What is 2 + 2?",
                QuestionType = QuestionType.MULTIPLE_CHOICE,
                MaterialId = materialId,
                Options = new System.Collections.Generic.List<string> { "3", "4", "5" },
                CorrectAnswer = "1",
                Synced = false
            };

            ctx.Questions.Add(question);
            ctx.SaveChanges();

            var response = new ResponseDataEntity(responseId, questionId, Guid.NewGuid(), "1", true);
            ctx.Responses.Add(response);
            ctx.SaveChanges();
        }

        using (var ctx = new MainDbContext(options))
        {
            var mat = ctx.Materials.FirstOrDefault();
            Assert.NotNull(mat);
            Assert.Equal(MaterialType.WORKSHEET, mat!.MaterialType);
            Assert.Equal(1, ctx.Questions.Count());
            Assert.Equal(1, ctx.Responses.Count());
            
            var question = ctx.Questions.FirstOrDefault();
            Assert.NotNull(question);
            Assert.Equal(QuestionType.MULTIPLE_CHOICE, question!.QuestionType);
            
            var response = ctx.Responses.FirstOrDefault();
            Assert.NotNull(response);
            Assert.True(response!.IsCorrect);
        }
    }

    [Fact]
    public void CanQuery_QuestionsByMaterialId()
    {
        var options = CreateInMemoryOptions("test_db_2");

        Guid materialId = Guid.Empty;
        using (var ctx = new MainDbContext(options))
        {
            materialId = Guid.NewGuid();
            var material = new MaterialDataEntity
            {
                Id = materialId,
                MaterialType = MaterialType.QUIZ,
                Title = "Quiz",
                Content = "Content",
                Timestamp = DateTime.UtcNow,
                Synced = false
            };
            ctx.Materials.Add(material);

            for (int i = 0; i < 3; i++)
            {
                ctx.Questions.Add(new QuestionDataEntity
                {
                    Id = Guid.NewGuid(),
                    QuestionText = $"Question {i}",
                    QuestionType = QuestionType.TRUE_FALSE,
                    MaterialId = materialId,
                    CorrectAnswer = "True",
                    Synced = false
                });
            }
            ctx.SaveChanges();
        }

        using (var ctx = new MainDbContext(options))
        {
            var questions = ctx.Questions.Where(q => q.MaterialId == materialId).ToList();
            Assert.Equal(3, questions.Count);
        }
    }
}
