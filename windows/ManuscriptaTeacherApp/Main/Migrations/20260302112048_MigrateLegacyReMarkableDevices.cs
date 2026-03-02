using Microsoft.EntityFrameworkCore.Migrations;

#nullable disable

namespace Manuscripta.Main.Migrations
{
    /// <summary>
    /// Forward-only migration to handle environments that applied the old migration chain
    /// (20260209225942_AddReMarkableDevices, 20260216231728_AddConfigurationEntity)
    /// before the migration history was rewritten with InitialCreate.
    ///
    /// This migration:
    /// 1. Detects if ReMarkableDevices table exists (legacy schema)
    /// 2. Transforms it to ExternalDevices with the new schema
    /// 3. Creates EmailCredentials table if missing
    /// 4. Updates __EFMigrationsHistory to mark new migrations as applied
    /// </summary>
    public partial class MigrateLegacyReMarkableDevices : Migration
    {
        /// <inheritdoc />
        protected override void Up(MigrationBuilder migrationBuilder)
        {
            // This migration handles legacy databases that had ReMarkableDevices table.
            // NOTE: Device registrations from legacy ReMarkableDevices are NOT migrated
            // because SQLite cannot conditionally SELECT from a table that may not exist.
            // Users on legacy databases will need to re-pair their reMarkable devices.

            // Step 1: Create ExternalDevices if it doesn't exist
            migrationBuilder.Sql(@"
                CREATE TABLE IF NOT EXISTS ExternalDevices (
                    DeviceId TEXT NOT NULL PRIMARY KEY,
                    Name TEXT NOT NULL,
                    Type TEXT NOT NULL DEFAULT 'REMARKABLE',
                    ConfigurationData TEXT NULL
                );
            ");

            // Step 2: Drop legacy ReMarkableDevices if it exists (data loss accepted)
            migrationBuilder.Sql(@"
                DROP TABLE IF EXISTS ReMarkableDevices;
            ");

            // Step 3: Create EmailCredentials table if it doesn't exist (added in new schema)
            migrationBuilder.Sql(@"
                CREATE TABLE IF NOT EXISTS EmailCredentials (
                    Id TEXT NOT NULL PRIMARY KEY,
                    EmailAddress TEXT NOT NULL,
                    SmtpHost TEXT NOT NULL,
                    SmtpPort INTEGER NOT NULL,
                    Password TEXT NOT NULL
                );
            ");

            // Step 4: Update __EFMigrationsHistory to include the new migrations
            // This prevents EF from trying to re-run InitialCreate on legacy DBs
            migrationBuilder.Sql(@"
                -- Mark InitialCreate as applied if not already present
                INSERT OR IGNORE INTO __EFMigrationsHistory (MigrationId, ProductVersion)
                VALUES ('20260224161518_InitialCreate', '10.0.0');

                -- Mark RenameExternalDeviceIdToDeviceId as applied if not already present
                INSERT OR IGNORE INTO __EFMigrationsHistory (MigrationId, ProductVersion)
                VALUES ('20260301232324_RenameExternalDeviceIdToDeviceId', '10.0.0');

                -- Mark ValidateNoPendingChanges as applied if not already present
                INSERT OR IGNORE INTO __EFMigrationsHistory (MigrationId, ProductVersion)
                VALUES ('20260301232408_ValidateNoPendingChanges', '10.0.0');

                -- Remove old migration entries that no longer exist in the codebase
                DELETE FROM __EFMigrationsHistory
                WHERE MigrationId IN (
                    '20260209225942_AddReMarkableDevices',
                    '20260216231728_AddConfigurationEntity'
                );
            ");
        }

        /// <inheritdoc />
        protected override void Down(MigrationBuilder migrationBuilder)
        {
            // This is a forward-only migration.
            // Rolling back to the legacy schema is not supported as the old migrations
            // no longer exist in the codebase.
            //
            // If rollback is needed, restore from backup or manually recreate:
            // 1. Rename ExternalDevices back to ReMarkableDevices
            // 2. Drop Type and ConfigurationData columns
            // 3. Drop EmailCredentials table
            // 4. Restore old __EFMigrationsHistory entries
        }
    }
}
