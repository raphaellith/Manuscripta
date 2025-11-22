using Microsoft.EntityFrameworkCore;

using Main.Models.Entities;

namespace Main.Data;

public class MainDbContext : DbContext
{
    public MainDbContext(DbContextOptions<MainDbContext> options) : base(options)
    {
    }

    public DbSet<MaterialEntity> Materials { get; set; } = null!;
    public DbSet<QuestionEntity> Questions { get; set; } = null!;
    public DbSet<ResponseEntity> Responses { get; set; } = null!;

    protected override void OnModelCreating(ModelBuilder modelBuilder)
    {
        base.OnModelCreating(modelBuilder);

        // Configure MaterialEntity
        modelBuilder.Entity<MaterialEntity>(entity =>
        {
            entity.HasKey(e => e.Id);
            entity.Property(e => e.Type).HasConversion<string>();
            entity.HasIndex(e => e.Type);
            entity.HasIndex(e => e.Timestamp);
        });

        // Configure QuestionEntity
        modelBuilder.Entity<QuestionEntity>(entity =>
        {
            entity.HasKey(e => e.Id);
            entity.HasIndex(e => e.MaterialId);
            
            entity.HasOne(q => q.Material)
                .WithMany(m => m.Questions)
                .HasForeignKey(q => q.MaterialId)
                .OnDelete(DeleteBehavior.Cascade);
        });

        // Configure ResponseEntity
        modelBuilder.Entity<ResponseEntity>(entity =>
        {
            entity.HasKey(e => e.Id);
            entity.HasIndex(e => e.QuestionId);
            entity.HasIndex(e => e.Synced);
            entity.HasIndex(e => e.Timestamp);
            
            entity.HasOne(r => r.Question)
                .WithMany(q => q.Responses)
                .HasForeignKey(r => r.QuestionId)
                .OnDelete(DeleteBehavior.Cascade);
        });
    }
}