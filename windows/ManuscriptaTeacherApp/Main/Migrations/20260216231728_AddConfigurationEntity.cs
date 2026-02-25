using System;
using Microsoft.EntityFrameworkCore.Migrations;

#nullable disable

namespace Manuscripta.Main.Migrations
{
    /// <inheritdoc />
    public partial class AddConfigurationEntity : Migration
    {
        /// <inheritdoc />
        protected override void Up(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.CreateTable(
                name: "Configurations",
                columns: table => new
                {
                    Id = table.Column<Guid>(type: "TEXT", nullable: false),
                    TextSize = table.Column<int>(type: "INTEGER", nullable: false),
                    FeedbackStyle = table.Column<string>(type: "TEXT", nullable: false),
                    TtsEnabled = table.Column<bool>(type: "INTEGER", nullable: false),
                    AiScaffoldingEnabled = table.Column<bool>(type: "INTEGER", nullable: false),
                    SummarisationEnabled = table.Column<bool>(type: "INTEGER", nullable: false),
                    MascotSelection = table.Column<string>(type: "TEXT", nullable: false)
                },
                constraints: table =>
                {
                    table.PrimaryKey("PK_Configurations", x => x.Id);
                });
        }

        /// <inheritdoc />
        protected override void Down(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.DropTable(
                name: "Configurations");
        }
    }
}
