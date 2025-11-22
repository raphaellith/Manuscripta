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
            var material = new MaterialEntity
            {
                Type = MaterialType.WORKSHEET,
                Title = "T",
                Content = "C",
                Timestamp = 1000
            };

            var question = new QuestionEntity
            {
                QuestionText = "Q",
                QuestionType = "MCQ",
                Material = material
            };

            var response = new ResponseEntity
            {
                Id = 1,
                Answer = "A",
                Question = question
            };

            ctx.Materials.Add(material);
            ctx.Questions.Add(question);
            ctx.Responses.Add(response);
            ctx.SaveChanges();
        }

        using (var ctx = new MainDbContext(options))
        {
            var mat = ctx.Materials.Include(m => m.Questions).FirstOrDefault();
            Assert.NotNull(mat);
            Assert.Equal(MaterialType.WORKSHEET, mat!.Type);
            Assert.Equal(1, ctx.Questions.Count());
            Assert.Equal(1, ctx.Responses.Count());
        }
    }
}
