using Microsoft.EntityFrameworkCore;
using System.Text.Json.Nodes;

using Main.Models.Entities;

namespace Main.Data;

public class MainDbContext : DbContext
{
    public MainDbContext(DbContextOptions<MainDbContext> options) : base(options)
    {
    }

    // Hierarchy entities - Per PersistenceAndCascadingRules.md §1(1)(c-e)
    public DbSet<UnitCollectionEntity> UnitCollections { get; set; }
    public DbSet<UnitEntity> Units { get; set; }
    public DbSet<LessonEntity> Lessons { get; set; }
    
    // Material/Question entities - Per PersistenceAndCascadingRules.md §1(1)(a-b)
    public DbSet<MaterialDataEntity> Materials { get; set; }
    public DbSet<QuestionDataEntity> Questions { get; set; }
    
    // PairedDevices removed from persistent context per PersistenceAndCascadingRules §1(2)
    // Managed by in-memory DeviceRegistryService
    
    /// <summary>
    /// Source documents imported into unit collections.
    /// Per PersistenceAndCascadingRules.md §1(1)(f).
    /// </summary>
    public DbSet<SourceDocumentEntity> SourceDocuments { get; set; }
    
    /// <summary>
    /// Attachments associated with materials.
    /// Per PersistenceAndCascadingRules.md §1(1)(g).
    /// </summary>
    public DbSet<AttachmentEntity> Attachments { get; set; }
    
    // NOTE: ResponseDataEntity and SessionDataEntity are NOT persisted to the database.
    // Per PersistenceAndCascadingRules.md §1(2), they require short-term persistence only.
    // They are managed by InMemoryResponseRepository and InMemorySessionRepository.

    // Called when MainDbContext has been initialized but before the model has been secured and used to initialize the context.
    protected override void OnModelCreating(ModelBuilder modelBuilder)
    {
        modelBuilder.Ignore<JsonNode>();
        
        base.OnModelCreating(modelBuilder);

        // Configure UnitCollectionEntity
        modelBuilder.Entity<UnitCollectionEntity>(entity =>
        {
            entity.HasKey(e => e.Id);
        });

        // Configure UnitEntity with cascade delete from UnitCollection
        // Per PersistenceAndCascadingRules.md §2(3)
        modelBuilder.Entity<UnitEntity>(entity =>
        {
            entity.HasKey(e => e.Id);
            entity.HasIndex(e => e.UnitCollectionId);
            
            entity.HasOne(u => u.UnitCollection)
                .WithMany()
                .HasForeignKey(u => u.UnitCollectionId)
                .OnDelete(DeleteBehavior.Cascade);
        });

        // Configure LessonEntity with cascade delete from Unit
        // Per PersistenceAndCascadingRules.md §2(4)
        modelBuilder.Entity<LessonEntity>(entity =>
        {
            entity.HasKey(e => e.Id);
            entity.HasIndex(e => e.UnitId);
            
            entity.HasOne(l => l.Unit)
                .WithMany()
                .HasForeignKey(l => l.UnitId)
                .OnDelete(DeleteBehavior.Cascade);
        });

        // Configure MaterialDataEntity with cascade delete from Lesson
        // Per PersistenceAndCascadingRules.md §2(5)
        modelBuilder.Entity<MaterialDataEntity>(entity =>
        {
            entity.HasKey(e => e.Id);
            entity.Property(e => e.MaterialType).HasConversion<string>();
            entity.HasIndex(e => e.MaterialType);
            entity.HasIndex(e => e.Timestamp);
            entity.HasIndex(e => e.LessonId);
            
            entity.HasOne(m => m.Lesson)
                .WithMany()
                .HasForeignKey(m => m.LessonId)
                .OnDelete(DeleteBehavior.Cascade);
        });

        // Configure QuestionDataEntity with cascade delete from Material
        // Per PersistenceAndCascadingRules.md §2(1)
        modelBuilder.Entity<QuestionDataEntity>(entity =>
        {
            entity.HasKey(e => e.Id);
            entity.Property(e => e.QuestionType).HasConversion<string>();
            entity.HasIndex(e => e.QuestionType);
            entity.HasIndex(e => e.MaterialId);
            
            entity.HasOne(q => q.Material)
                .WithMany()
                .HasForeignKey(q => q.MaterialId)
                .OnDelete(DeleteBehavior.Cascade);
        });

        // PairedDeviceEntity configuration removed (non-persistent)

        // Configure SourceDocumentEntity with cascade delete from UnitCollection
        // Per PersistenceAndCascadingRules.md §2(3A)
        modelBuilder.Entity<SourceDocumentEntity>(entity =>
        {
            entity.HasKey(e => e.Id);
            entity.HasIndex(e => e.UnitCollectionId);
            
            entity.HasOne(sd => sd.UnitCollection)
                .WithMany()
                .HasForeignKey(sd => sd.UnitCollectionId)
                .OnDelete(DeleteBehavior.Cascade);
        });

        // Configure AttachmentEntity with cascade delete from Material
        // Per PersistenceAndCascadingRules.md §2(6)
        modelBuilder.Entity<AttachmentEntity>(entity =>
        {
            entity.HasKey(e => e.Id);
            entity.HasIndex(e => e.MaterialId);
            
            entity.HasOne(a => a.Material)
                .WithMany()
                .HasForeignKey(a => a.MaterialId)
                .OnDelete(DeleteBehavior.Cascade);
        });

        // NOTE: ResponseDataEntity and SessionDataEntity are NOT configured here.
        // Per PersistenceAndCascadingRules.md §1(2), they require short-term persistence only
        // and are managed by in-memory repositories (InMemoryResponseRepository, InMemorySessionRepository).
        // Orphan removal for responses when a question is deleted (§2(2)) is handled at the service layer.
    }
}