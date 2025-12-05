using Microsoft.EntityFrameworkCore;
using System.Text.Json.Nodes;

using Main.Models.Entities;

namespace Main.Data;

public class MainDbContext : DbContext
{
    public MainDbContext(DbContextOptions<MainDbContext> options) : base(options)
    {
    }

    // Create a DbSet<TEntity> property for each entity set, corresponding to a database table
    public DbSet<MaterialDataEntity> Materials { get; set; }
    public DbSet<QuestionDataEntity> Questions { get; set; }
    // NOTE: ResponseDataEntity and SessionDataEntity are NOT persisted to the database.
    // Per PersistenceAndCascadingRules.md §1(2), they require short-term persistence only.
    // They are managed by InMemoryResponseRepository and InMemorySessionRepository.

    // Called when MainDbContext has been initialized but before the model has been secured and used to initialize the context.
    protected override void OnModelCreating(ModelBuilder modelBuilder)
    {
        modelBuilder.Ignore<JsonNode>();
        
        base.OnModelCreating(modelBuilder);

        // Configure MaterialDataEntity
        modelBuilder.Entity<MaterialDataEntity>(entity =>
        {
            entity.HasKey(e => e.Id);
            entity.Property(e => e.MaterialType).HasConversion<string>();
            entity.HasIndex(e => e.MaterialType);
            entity.HasIndex(e => e.Timestamp);
            entity.HasIndex(e => e.Synced);
        });

        // Configure QuestionDataEntity
        modelBuilder.Entity<QuestionDataEntity>(entity =>
        {
            entity.HasKey(e => e.Id);
            entity.Property(e => e.QuestionType).HasConversion<string>();
            entity.HasIndex(e => e.QuestionType);
            entity.HasIndex(e => e.MaterialId);
            entity.HasIndex(e => e.Synced);
            
            // Configure relationship with MaterialEntity
            entity.HasOne(q => q.Material)
                .WithMany()
                .HasForeignKey(q => q.MaterialId)
                .OnDelete(DeleteBehavior.Cascade);
        });

        // NOTE: ResponseDataEntity and SessionDataEntity are NOT configured here.
        // Per PersistenceAndCascadingRules.md §1(2), they require short-term persistence only
        // and are managed by in-memory repositories (InMemoryResponseRepository, InMemorySessionRepository).
        // Orphan removal for responses when a question is deleted (§2(2)) is handled at the service layer.
    }
}