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
        }

        /// <inheritdoc />
        protected override void Down(MigrationBuilder migrationBuilder)
        {
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
