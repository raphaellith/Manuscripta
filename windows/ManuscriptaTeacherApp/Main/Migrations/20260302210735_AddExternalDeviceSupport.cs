using System;
using Microsoft.EntityFrameworkCore.Migrations;

#nullable disable

namespace Manuscripta.Main.Migrations
{
    /// <summary>
    /// Forward-only migration that transforms the legacy ReMarkableDevices table
    /// into the generalised ExternalDevices table and adds EmailCredentials.
    /// Existing ReMarkableDevice rows are preserved with Type = 'REMARKABLE'.
    /// </summary>
    public partial class AddExternalDeviceSupport : Migration
    {
        /// <inheritdoc />
        protected override void Up(MigrationBuilder migrationBuilder)
        {
            // 1. Create new tables first
            migrationBuilder.CreateTable(
                name: "EmailCredentials",
                columns: table => new
                {
                    Id = table.Column<Guid>(type: "TEXT", nullable: false),
                    EmailAddress = table.Column<string>(type: "TEXT", nullable: false),
                    SmtpHost = table.Column<string>(type: "TEXT", nullable: false),
                    SmtpPort = table.Column<int>(type: "INTEGER", nullable: false),
                    Password = table.Column<string>(type: "TEXT", nullable: false)
                },
                constraints: table =>
                {
                    table.PrimaryKey("PK_EmailCredentials", x => x.Id);
                });

            migrationBuilder.CreateTable(
                name: "ExternalDevices",
                columns: table => new
                {
                    DeviceId = table.Column<Guid>(type: "TEXT", nullable: false),
                    Name = table.Column<string>(type: "TEXT", nullable: false),
                    Type = table.Column<string>(type: "TEXT", nullable: false),
                    ConfigurationData = table.Column<string>(type: "TEXT", nullable: true)
                },
                constraints: table =>
                {
                    table.PrimaryKey("PK_ExternalDevices", x => x.DeviceId);
                });

            // 2. Copy existing ReMarkableDevices data into ExternalDevices
            migrationBuilder.Sql(
                "INSERT INTO ExternalDevices (DeviceId, Name, Type, ConfigurationData) " +
                "SELECT DeviceId, Name, 'REMARKABLE', '' FROM ReMarkableDevices");

            // 3. Drop the legacy table
            migrationBuilder.DropTable(
                name: "ReMarkableDevices");
        }

        /// <inheritdoc />
        protected override void Down(MigrationBuilder migrationBuilder)
        {
            // 1. Re-create the legacy table
            migrationBuilder.CreateTable(
                name: "ReMarkableDevices",
                columns: table => new
                {
                    DeviceId = table.Column<Guid>(type: "TEXT", nullable: false),
                    Name = table.Column<string>(type: "TEXT", nullable: false)
                },
                constraints: table =>
                {
                    table.PrimaryKey("PK_ReMarkableDevices", x => x.DeviceId);
                });

            // 2. Copy REMARKABLE devices back
            migrationBuilder.Sql(
                "INSERT INTO ReMarkableDevices (DeviceId, Name) " +
                "SELECT DeviceId, Name FROM ExternalDevices WHERE Type = 'REMARKABLE'");

            // 3. Drop the new tables
            migrationBuilder.DropTable(
                name: "EmailCredentials");

            migrationBuilder.DropTable(
                name: "ExternalDevices");
        }
    }
}
