using Microsoft.AspNetCore.Builder;
using Microsoft.Extensions.Hosting;
using Microsoft.EntityFrameworkCore;

using Main.Data;

var builder = WebApplication.CreateBuilder(args);

builder.Services.AddDbContext<MainDbContext>(options =>
  options.UseSqlite(builder.Configuration.GetConnectionString("MainDbContext")));

// NOTE: Controllers are enabled so that REST controllers can be added later.
builder.Services.AddControllers();

var app = builder.Build();

if (app.Environment.IsDevelopment())
{
    app.UseDeveloperExceptionPage();
}

using (var scope = app.Services.CreateScope())
{
    var services = scope.ServiceProvider;

    var context = services.GetRequiredService<MainDbContext>();
    context.Database.EnsureCreated();
    // DbInitializer.Initialize(context);
}

// Minimal default endpoint to confirm the app is running.
app.MapGet("/", () => Results.Ok("Manuscripta Main API (net10.0) is running"));

app.MapControllers();

app.Run();
