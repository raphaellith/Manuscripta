using System;
using System.Linq;
using System.Threading.Tasks;
using Microsoft.Data.Sqlite;
using Microsoft.EntityFrameworkCore;
using Xunit;
using Main.Data;
using Main.Models.Entities;
using Main.Models.Entities.Materials;
using Main.Services.Repositories;

namespace MainTests.RepositoryTests;

/// <summary>
/// Integration tests for hierarchy repositories.
/// Verifies EF Core implementation and cascade delete behavior per PersistenceAndCascadingRules.md.
/// </summary>
public class HierarchyRepositoryTests
{
    private DbContextOptions<MainDbContext> CreateSqliteInMemoryOptions(SqliteConnection connection)
    {
        return new DbContextOptionsBuilder<MainDbContext>()
            .UseSqlite(connection)
            .Options;
    }

    #region UnitCollection Repository Tests

    [Fact]
    public async Task EfUnitCollectionRepository_CanAddAndRetrieve()
    {
        using var connection = new SqliteConnection("DataSource=:memory:");
        connection.Open();
        var options = CreateSqliteInMemoryOptions(connection);

        var id = Guid.NewGuid();

        using (var ctx = new MainDbContext(options))
        {
            ctx.Database.EnsureCreated();
            var repo = new EfUnitCollectionRepository(ctx);

            var unitCollection = new UnitCollectionEntity(id, "Test Collection");
            await repo.AddAsync(unitCollection);
        }

        using (var ctx = new MainDbContext(options))
        {
            var repo = new EfUnitCollectionRepository(ctx);
            var retrieved = await repo.GetByIdAsync(id);

            Assert.NotNull(retrieved);
            Assert.Equal("Test Collection", retrieved!.Title);
        }
    }

    [Fact]
    public async Task EfUnitCollectionRepository_CanUpdate()
    {
        using var connection = new SqliteConnection("DataSource=:memory:");
        connection.Open();
        var options = CreateSqliteInMemoryOptions(connection);

        var id = Guid.NewGuid();

        using (var ctx = new MainDbContext(options))
        {
            ctx.Database.EnsureCreated();
            var repo = new EfUnitCollectionRepository(ctx);
            await repo.AddAsync(new UnitCollectionEntity(id, "Original"));
        }

        using (var ctx = new MainDbContext(options))
        {
            var repo = new EfUnitCollectionRepository(ctx);
            var updated = new UnitCollectionEntity(id, "Updated");
            await repo.UpdateAsync(updated);
        }

        using (var ctx = new MainDbContext(options))
        {
            var repo = new EfUnitCollectionRepository(ctx);
            var retrieved = await repo.GetByIdAsync(id);
            Assert.Equal("Updated", retrieved!.Title);
        }
    }

    [Fact]
    public async Task EfUnitCollectionRepository_CanDelete()
    {
        using var connection = new SqliteConnection("DataSource=:memory:");
        connection.Open();
        var options = CreateSqliteInMemoryOptions(connection);

        var id = Guid.NewGuid();

        using (var ctx = new MainDbContext(options))
        {
            ctx.Database.EnsureCreated();
            var repo = new EfUnitCollectionRepository(ctx);
            await repo.AddAsync(new UnitCollectionEntity(id, "To Delete"));
        }

        using (var ctx = new MainDbContext(options))
        {
            var repo = new EfUnitCollectionRepository(ctx);
            await repo.DeleteAsync(id);
        }

        using (var ctx = new MainDbContext(options))
        {
            var repo = new EfUnitCollectionRepository(ctx);
            var retrieved = await repo.GetByIdAsync(id);
            Assert.Null(retrieved);
        }
    }

    [Fact]
    public async Task EfUnitCollectionRepository_GetAllAsync_ReturnsAll()
    {
        using var connection = new SqliteConnection("DataSource=:memory:");
        connection.Open();
        var options = CreateSqliteInMemoryOptions(connection);

        using (var ctx = new MainDbContext(options))
        {
            ctx.Database.EnsureCreated();
            var repo = new EfUnitCollectionRepository(ctx);
            await repo.AddAsync(new UnitCollectionEntity(Guid.NewGuid(), "Collection 1"));
            await repo.AddAsync(new UnitCollectionEntity(Guid.NewGuid(), "Collection 2"));
        }

        using (var ctx = new MainDbContext(options))
        {
            var repo = new EfUnitCollectionRepository(ctx);
            var all = (await repo.GetAllAsync()).ToList();
            Assert.Equal(2, all.Count);
        }
    }

    #endregion

    #region Unit Repository Tests

    [Fact]
    public async Task EfUnitRepository_CanAddAndRetrieve()
    {
        using var connection = new SqliteConnection("DataSource=:memory:");
        connection.Open();
        var options = CreateSqliteInMemoryOptions(connection);

        var unitCollectionId = Guid.NewGuid();
        var unitId = Guid.NewGuid();

        using (var ctx = new MainDbContext(options))
        {
            ctx.Database.EnsureCreated();
            
            // Create parent first (FK constraint)
            ctx.UnitCollections.Add(new UnitCollectionEntity(unitCollectionId, "Parent Collection"));
            await ctx.SaveChangesAsync();

            var repo = new EfUnitRepository(ctx);
            var unit = new UnitEntity(unitId, unitCollectionId, "Test Unit", new List<string> { "doc.pdf" });
            await repo.AddAsync(unit);
        }

        using (var ctx = new MainDbContext(options))
        {
            var repo = new EfUnitRepository(ctx);
            var retrieved = await repo.GetByIdAsync(unitId);

            Assert.NotNull(retrieved);
            Assert.Equal("Test Unit", retrieved!.Title);
            Assert.Single(retrieved.SourceDocuments);
        }
    }

    [Fact]
    public async Task EfUnitRepository_GetByUnitCollectionIdAsync_ReturnsChildUnits()
    {
        using var connection = new SqliteConnection("DataSource=:memory:");
        connection.Open();
        var options = CreateSqliteInMemoryOptions(connection);

        var unitCollectionId = Guid.NewGuid();

        using (var ctx = new MainDbContext(options))
        {
            ctx.Database.EnsureCreated();
            ctx.UnitCollections.Add(new UnitCollectionEntity(unitCollectionId, "Parent"));
            await ctx.SaveChangesAsync();

            var repo = new EfUnitRepository(ctx);
            await repo.AddAsync(new UnitEntity(Guid.NewGuid(), unitCollectionId, "Unit 1", new List<string>()));
            await repo.AddAsync(new UnitEntity(Guid.NewGuid(), unitCollectionId, "Unit 2", new List<string>()));
        }

        using (var ctx = new MainDbContext(options))
        {
            var repo = new EfUnitRepository(ctx);
            var units = (await repo.GetByUnitCollectionIdAsync(unitCollectionId)).ToList();
            Assert.Equal(2, units.Count);
        }
    }

    #endregion

    #region Lesson Repository Tests

    [Fact]
    public async Task EfLessonRepository_CanAddAndRetrieve()
    {
        using var connection = new SqliteConnection("DataSource=:memory:");
        connection.Open();
        var options = CreateSqliteInMemoryOptions(connection);

        var unitCollectionId = Guid.NewGuid();
        var unitId = Guid.NewGuid();
        var lessonId = Guid.NewGuid();

        using (var ctx = new MainDbContext(options))
        {
            ctx.Database.EnsureCreated();
            
            // Create hierarchy (FK constraints)
            ctx.UnitCollections.Add(new UnitCollectionEntity(unitCollectionId, "Collection"));
            ctx.Units.Add(new UnitEntity(unitId, unitCollectionId, "Unit", new List<string>()));
            await ctx.SaveChangesAsync();

            var repo = new EfLessonRepository(ctx);
            var lesson = new LessonEntity(lessonId, unitId, "Test Lesson", "Description");
            await repo.AddAsync(lesson);
        }

        using (var ctx = new MainDbContext(options))
        {
            var repo = new EfLessonRepository(ctx);
            var retrieved = await repo.GetByIdAsync(lessonId);

            Assert.NotNull(retrieved);
            Assert.Equal("Test Lesson", retrieved!.Title);
            Assert.Equal("Description", retrieved.Description);
        }
    }

    [Fact]
    public async Task EfLessonRepository_GetByUnitIdAsync_ReturnsChildLessons()
    {
        using var connection = new SqliteConnection("DataSource=:memory:");
        connection.Open();
        var options = CreateSqliteInMemoryOptions(connection);

        var unitCollectionId = Guid.NewGuid();
        var unitId = Guid.NewGuid();

        using (var ctx = new MainDbContext(options))
        {
            ctx.Database.EnsureCreated();
            ctx.UnitCollections.Add(new UnitCollectionEntity(unitCollectionId, "Collection"));
            ctx.Units.Add(new UnitEntity(unitId, unitCollectionId, "Unit", new List<string>()));
            await ctx.SaveChangesAsync();

            var repo = new EfLessonRepository(ctx);
            await repo.AddAsync(new LessonEntity(Guid.NewGuid(), unitId, "Lesson 1", "Desc 1"));
            await repo.AddAsync(new LessonEntity(Guid.NewGuid(), unitId, "Lesson 2", "Desc 2"));
        }

        using (var ctx = new MainDbContext(options))
        {
            var repo = new EfLessonRepository(ctx);
            var lessons = (await repo.GetByUnitIdAsync(unitId)).ToList();
            Assert.Equal(2, lessons.Count);
        }
    }

    #endregion

    #region Cascade Delete Tests per PersistenceAndCascadingRules.md

    [Fact]
    public async Task CascadeDelete_UnitCollection_DeletesUnits()
    {
        // Per §2(3): Deletion of a unit collection must delete all units
        using var connection = new SqliteConnection("DataSource=:memory:");
        connection.Open();
        var options = CreateSqliteInMemoryOptions(connection);

        var unitCollectionId = Guid.NewGuid();
        var unitId = Guid.NewGuid();

        using (var ctx = new MainDbContext(options))
        {
            ctx.Database.EnsureCreated();
            ctx.UnitCollections.Add(new UnitCollectionEntity(unitCollectionId, "Collection"));
            ctx.Units.Add(new UnitEntity(unitId, unitCollectionId, "Unit", new List<string>()));
            await ctx.SaveChangesAsync();
        }

        using (var ctx = new MainDbContext(options))
        {
            var repo = new EfUnitCollectionRepository(ctx);
            await repo.DeleteAsync(unitCollectionId);
        }

        using (var ctx = new MainDbContext(options))
        {
            // Verify cascade deletion
            Assert.Null(await ctx.UnitCollections.FindAsync(unitCollectionId));
            Assert.Null(await ctx.Units.FindAsync(unitId));
        }
    }

    [Fact]
    public async Task CascadeDelete_Unit_DeletesLessons()
    {
        // Per §2(4): Deletion of a unit must delete all lessons
        using var connection = new SqliteConnection("DataSource=:memory:");
        connection.Open();
        var options = CreateSqliteInMemoryOptions(connection);

        var unitCollectionId = Guid.NewGuid();
        var unitId = Guid.NewGuid();
        var lessonId = Guid.NewGuid();

        using (var ctx = new MainDbContext(options))
        {
            ctx.Database.EnsureCreated();
            ctx.UnitCollections.Add(new UnitCollectionEntity(unitCollectionId, "Collection"));
            ctx.Units.Add(new UnitEntity(unitId, unitCollectionId, "Unit", new List<string>()));
            ctx.Lessons.Add(new LessonEntity(lessonId, unitId, "Lesson", "Description"));
            await ctx.SaveChangesAsync();
        }

        using (var ctx = new MainDbContext(options))
        {
            var repo = new EfUnitRepository(ctx);
            await repo.DeleteAsync(unitId);
        }

        using (var ctx = new MainDbContext(options))
        {
            Assert.Null(await ctx.Units.FindAsync(unitId));
            Assert.Null(await ctx.Lessons.FindAsync(lessonId));
        }
    }

    [Fact]
    public async Task CascadeDelete_Lesson_DeletesMaterials()
    {
        // Per §2(5): Deletion of a lesson must delete all materials
        using var connection = new SqliteConnection("DataSource=:memory:");
        connection.Open();
        var options = CreateSqliteInMemoryOptions(connection);

        var unitCollectionId = Guid.NewGuid();
        var unitId = Guid.NewGuid();
        var lessonId = Guid.NewGuid();
        var materialId = Guid.NewGuid();

        using (var ctx = new MainDbContext(options))
        {
            ctx.Database.EnsureCreated();
            ctx.UnitCollections.Add(new UnitCollectionEntity(unitCollectionId, "Collection"));
            ctx.Units.Add(new UnitEntity(unitId, unitCollectionId, "Unit", new List<string>()));
            ctx.Lessons.Add(new LessonEntity(lessonId, unitId, "Lesson", "Description"));
            await ctx.SaveChangesAsync();

            // Add material with proper FK
            var mRepo = new EfMaterialRepository(ctx);
            var material = new WorksheetMaterialEntity(materialId, lessonId, "Material", "Content");
            await mRepo.AddAsync(material);
        }

        using (var ctx = new MainDbContext(options))
        {
            var repo = new EfLessonRepository(ctx);
            await repo.DeleteAsync(lessonId);
        }

        using (var ctx = new MainDbContext(options))
        {
            Assert.Null(await ctx.Lessons.FindAsync(lessonId));
            Assert.Null(await ctx.Materials.FindAsync(materialId));
        }
    }

    [Fact]
    public async Task CascadeDelete_FullHierarchy_DeletesEverything()
    {
        // Test the entire cascade chain: UnitCollection -> Unit -> Lesson -> Material -> Question
        using var connection = new SqliteConnection("DataSource=:memory:");
        connection.Open();
        var options = CreateSqliteInMemoryOptions(connection);

        var unitCollectionId = Guid.NewGuid();
        var unitId = Guid.NewGuid();
        var lessonId = Guid.NewGuid();
        var materialId = Guid.NewGuid();
        var questionId = Guid.NewGuid();

        using (var ctx = new MainDbContext(options))
        {
            ctx.Database.EnsureCreated();
            
            // Build full hierarchy
            ctx.UnitCollections.Add(new UnitCollectionEntity(unitCollectionId, "Collection"));
            ctx.Units.Add(new UnitEntity(unitId, unitCollectionId, "Unit", new List<string>()));
            ctx.Lessons.Add(new LessonEntity(lessonId, unitId, "Lesson", "Description"));
            await ctx.SaveChangesAsync();

            var mRepo = new EfMaterialRepository(ctx);
            await mRepo.AddAsync(new WorksheetMaterialEntity(materialId, lessonId, "Material", "Content"));
            
            var qRepo = new EfQuestionRepository(ctx);
            await qRepo.AddAsync(new Main.Models.Entities.Questions.MultipleChoiceQuestionEntity(
                questionId, materialId, "Is this a test?", new List<string> { "Yes", "No" }, 0));
        }

        using (var ctx = new MainDbContext(options))
        {
            // Delete at top level
            var repo = new EfUnitCollectionRepository(ctx);
            await repo.DeleteAsync(unitCollectionId);
        }

        using (var ctx = new MainDbContext(options))
        {
            // Verify entire hierarchy is deleted
            Assert.Null(await ctx.UnitCollections.FindAsync(unitCollectionId));
            Assert.Null(await ctx.Units.FindAsync(unitId));
            Assert.Null(await ctx.Lessons.FindAsync(lessonId));
            Assert.Null(await ctx.Materials.FindAsync(materialId));
            Assert.Null(await ctx.Questions.FindAsync(questionId));
        }
    }

    #endregion
}
