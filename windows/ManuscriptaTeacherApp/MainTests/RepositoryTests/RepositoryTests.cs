using System;
using System.Linq;
using System.Threading.Tasks;
using Microsoft.Data.Sqlite;
using Microsoft.EntityFrameworkCore;
using Xunit;
using Main.Data;
using Main.Models.Entities;
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
    public async Task Repositories_CanRetrieveEntities_ByIdAndForeignKeys()
    {
        using var connection = new SqliteConnection("DataSource=:memory:");
        connection.Open();

        var options = CreateSqliteInMemoryOptions(connection);

        // Create schema and seed data
        using (var ctx = new MainDbContext(options))
        {
            ctx.Database.EnsureCreated();

            var material = new MaterialEntity
            {
                Type = MaterialType.WORKSHEET,
                Title = "Mat",
                Content = "C",
                Timestamp = 1
            };

            ctx.Materials.Add(material);
            await ctx.SaveChangesAsync();

            var question = new QuestionEntity
            {
                MaterialId = material.Id,
                QuestionText = "Q",
                QuestionType = "MCQ"
            };

            ctx.Questions.Add(question);
            await ctx.SaveChangesAsync();

            var response = new ResponseEntity(123, question.Id, "A", false);

            ctx.Responses.Add(response);
            await ctx.SaveChangesAsync();
        }

        // Use repositories to query
        using (var ctx = new MainDbContext(options))
        {
            var mRepo = new EfMaterialRepository(ctx);
            var qRepo = new EfQuestionRepository(ctx);
            var rRepo = new EfResponseRepository(ctx);

            var m = await mRepo.GetByIdAsync(1);
            Assert.NotNull(m);

            var qs = (await qRepo.GetByMaterialIdAsync(m!.Id)).ToList();
            Assert.Single(qs);

            var q = await qRepo.GetByIdAsync(qs[0].Id);
            Assert.NotNull(q);

            var rs = (await rRepo.GetByQuestionIdAsync(q!.Id)).ToList();
            Assert.Single(rs);

            var r = await rRepo.GetByIdAsync(rs[0].Id);
            Assert.NotNull(r);
            Assert.Equal(123, r!.Id);
        }
    }
}
