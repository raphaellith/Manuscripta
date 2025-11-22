using Microsoft.AspNetCore.Builder;
using Microsoft.Extensions.Hosting;

var builder = WebApplication.CreateBuilder(args);

// NOTE: Controllers are enabled so that REST controllers can be added later.
builder.Services.AddControllers();

var app = builder.Build();

if (app.Environment.IsDevelopment())
{
    app.UseDeveloperExceptionPage();
}

// Minimal default endpoint to confirm the app is running.
app.MapGet("/", () => Results.Ok("Manuscripta Main API (net10.0) is running"));

app.MapControllers();

app.Run();
