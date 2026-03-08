using Microsoft.EntityFrameworkCore.Migrations;

#nullable disable

namespace Manuscripta.Main.Migrations
{
    /// <inheritdoc />
    public partial class AddPerDevicePdfExportSettings : Migration
    {
        /// <inheritdoc />
        protected override void Up(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.AddColumn<string>(
                name: "FontSizePreset",
                table: "ExternalDevices",
                type: "TEXT",
                nullable: true);

            migrationBuilder.AddColumn<string>(
                name: "LinePatternType",
                table: "ExternalDevices",
                type: "TEXT",
                nullable: true);

            migrationBuilder.AddColumn<string>(
                name: "LineSpacingPreset",
                table: "ExternalDevices",
                type: "TEXT",
                nullable: true);

            migrationBuilder.AddColumn<string>(
                name: "FontSizePreset",
                table: "Materials",
                type: "TEXT",
                nullable: true);

            migrationBuilder.AddColumn<string>(
                name: "LinePatternType",
                table: "Materials",
                type: "TEXT",
                nullable: true);

            migrationBuilder.AddColumn<string>(
                name: "LineSpacingPreset",
                table: "Materials",
                type: "TEXT",
                nullable: true);

            migrationBuilder.CreateTable(
                name: "PdfExportSettings",
                columns: table => new
                {
                    Id = table.Column<Guid>(type: "TEXT", nullable: false),
                    FontSizePreset = table.Column<string>(type: "TEXT", nullable: false),
                    LinePatternType = table.Column<string>(type: "TEXT", nullable: false),
                    LineSpacingPreset = table.Column<string>(type: "TEXT", nullable: false)
                },
                constraints: table =>
                {
                    table.PrimaryKey("PK_PdfExportSettings", x => x.Id);
                });
        }

        /// <inheritdoc />
        protected override void Down(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.DropTable(
                name: "PdfExportSettings");

            migrationBuilder.DropColumn(
                name: "FontSizePreset",
                table: "Materials");

            migrationBuilder.DropColumn(
                name: "LinePatternType",
                table: "Materials");

            migrationBuilder.DropColumn(
                name: "LineSpacingPreset",
                table: "Materials");

            migrationBuilder.DropColumn(
                name: "FontSizePreset",
                table: "ExternalDevices");

            migrationBuilder.DropColumn(
                name: "LinePatternType",
                table: "ExternalDevices");

            migrationBuilder.DropColumn(
                name: "LineSpacingPreset",
                table: "ExternalDevices");
        }
    }
}
