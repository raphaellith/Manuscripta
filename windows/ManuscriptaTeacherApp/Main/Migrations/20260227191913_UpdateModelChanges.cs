using Microsoft.EntityFrameworkCore.Migrations;

#nullable disable

namespace Manuscripta.Main.Migrations
{
    /// <inheritdoc />
    public partial class UpdateModelChanges : Migration
    {
        /// <inheritdoc />
        protected override void Up(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.DropColumn(
                name: "SourceDocuments",
                table: "Units");

            migrationBuilder.AddColumn<int>(
                name: "EmbeddingStatus",
                table: "SourceDocuments",
                type: "INTEGER",
                nullable: true);
        }

        /// <inheritdoc />
        protected override void Down(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.DropColumn(
                name: "EmbeddingStatus",
                table: "SourceDocuments");

            migrationBuilder.AddColumn<string>(
                name: "SourceDocuments",
                table: "Units",
                type: "TEXT",
                nullable: false,
                defaultValue: "");
        }
    }
}
