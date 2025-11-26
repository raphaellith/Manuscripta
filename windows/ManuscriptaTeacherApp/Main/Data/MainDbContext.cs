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
    public DbSet<MaterialEntity> Materials { get; set; }
    public DbSet<QuestionDataEntity> Questions { get; set; }
    public DbSet<ResponseDataEntity> Responses { get; set; }

    // Called when MainDbContext has been initialized but before the model has been secured and used to initialize the context.
    protected override void OnModelCreating(ModelBuilder modelBuilder)
    {
        modelBuilder.Ignore<JsonNode>();
        
        base.OnModelCreating(modelBuilder);

        // Configure MaterialEntity
        modelBuilder.Entity<MaterialEntity>(entity =>
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

        // Configure ResponseDataEntity
        modelBuilder.Entity<ResponseDataEntity>(entity =>
        {
            entity.HasKey(e => e.Id);
            entity.HasIndex(e => e.QuestionId);
            entity.HasIndex(e => e.Timestamp);
            
            // Configure relationship with QuestionDataEntity
            entity.HasOne(r => r.Question)
                .WithMany()
                .HasForeignKey(r => r.QuestionId)
                .OnDelete(DeleteBehavior.Cascade);
        });
    }
}