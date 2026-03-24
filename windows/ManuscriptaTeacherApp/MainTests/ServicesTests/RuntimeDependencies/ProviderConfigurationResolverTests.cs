using System;
using System.IO;
using System.Text.Json;
using Main.Services.RuntimeDependencies;
using Microsoft.Extensions.Configuration;
using Microsoft.Extensions.Logging;
using Moq;
using Xunit;

namespace MainTests.ServicesTests.RuntimeDependencies;

/// <summary>
/// Tests for <see cref="ProviderConfigurationResolver"/>.
/// Validates precedence order, persistence of defaults, and resilience to malformed runtime JSON.
/// </summary>
public class ProviderConfigurationResolverTests : IDisposable
{
    private readonly string _tempDir;
    private readonly string _defaultsFilePath;
    private readonly string _runtimeFilePath;
    private readonly Mock<ILogger<ProviderConfigurationResolver>> _mockLogger;

    public ProviderConfigurationResolverTests()
    {
        _tempDir = Path.Combine(Path.GetTempPath(), $"prov-cfg-test-{Guid.NewGuid():N}");
        Directory.CreateDirectory(_tempDir);
        _defaultsFilePath = Path.Combine(_tempDir, "provider-config-defaults.json");
        _runtimeFilePath = Path.Combine(_tempDir, "provider-config.json");
        _mockLogger = new Mock<ILogger<ProviderConfigurationResolver>>();
    }

    public void Dispose()
    {
        try { Directory.Delete(_tempDir, recursive: true); } catch { }
    }

    // ---------------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------------

    private void WriteDefaultsFile(object providerConfigurations)
    {
        var wrapper = new { ProviderConfigurations = providerConfigurations };
        File.WriteAllText(_defaultsFilePath, JsonSerializer.Serialize(wrapper, new JsonSerializerOptions { WriteIndented = true }));
    }

    private void WriteRuntimeFile(object providerConfigurations)
    {
        var wrapper = new { ProviderConfigurations = providerConfigurations };
        File.WriteAllText(_runtimeFilePath, JsonSerializer.Serialize(wrapper, new JsonSerializerOptions { WriteIndented = true }));
    }

    private ProviderConfigurationResolver CreateResolver(IConfiguration? configuration = null)
    {
        var config = configuration ?? new ConfigurationBuilder().Build();
        return new TestableProviderConfigurationResolver(
            config,
            _mockLogger.Object,
            _defaultsFilePath,
            _runtimeFilePath);
    }

    // ---------------------------------------------------------------------------
    // Argument validation
    // ---------------------------------------------------------------------------

    [Fact]
    public void GetProviderConfigurationValue_EmptyKey_ThrowsArgumentException()
    {
        WriteDefaultsFile(new { KEY = "value" });
        var resolver = CreateResolver();

        Assert.Throws<ArgumentException>(() => resolver.GetProviderConfigurationValue(""));
        Assert.Throws<ArgumentException>(() => resolver.GetProviderConfigurationValue("   "));
    }

    [Fact]
    public void GetRequiredField_EmptyFieldName_ThrowsArgumentException()
    {
        WriteDefaultsFile(new { KEY = "{\"Field\":\"v\"}" });
        var resolver = CreateResolver();

        Assert.Throws<ArgumentException>(() => resolver.GetRequiredField("KEY", ""));
        Assert.Throws<ArgumentException>(() => resolver.GetRequiredField("KEY", "   "));
    }

    [Fact]
    public void GetProviderConfigurationValue_KeyNotFound_ThrowsInvalidOperationException()
    {
        WriteDefaultsFile(new { });
        var resolver = CreateResolver();

        var ex = Assert.Throws<InvalidOperationException>(() => resolver.GetProviderConfigurationValue("MISSING_KEY"));
        Assert.Contains("MISSING_KEY", ex.Message);
    }

    // ---------------------------------------------------------------------------
    // Precedence: runtime > deployment overrides > bundled defaults
    // ---------------------------------------------------------------------------

    [Fact]
    public void GetProviderConfigurationValue_DefaultsReturnedWhenNoHigherPrecedenceValues()
    {
        WriteDefaultsFile(new { MY_KEY = "default_value" });
        var resolver = CreateResolver();

        var result = resolver.GetProviderConfigurationValue("MY_KEY");

        Assert.Equal("default_value", result);
    }

    [Fact]
    public void GetProviderConfigurationValue_DeploymentOverrideWinsOverDefault()
    {
        WriteDefaultsFile(new { MY_KEY = "default_value" });

        var configuration = new ConfigurationBuilder()
            .AddInMemoryCollection(new[]
            {
                new System.Collections.Generic.KeyValuePair<string, string?>(
                    "ProviderConfigurationOverrides:MY_KEY", "override_value")
            })
            .Build();

        var resolver = CreateResolver(configuration);

        var result = resolver.GetProviderConfigurationValue("MY_KEY");

        Assert.Equal("override_value", result);
    }

    [Fact]
    public void GetProviderConfigurationValue_RuntimeValueWinsOverDeploymentOverrideAndDefault()
    {
        WriteDefaultsFile(new { MY_KEY = "default_value" });
        WriteRuntimeFile(new { MY_KEY = "runtime_value" });

        var configuration = new ConfigurationBuilder()
            .AddInMemoryCollection(new[]
            {
                new System.Collections.Generic.KeyValuePair<string, string?>(
                    "ProviderConfigurationOverrides:MY_KEY", "override_value")
            })
            .Build();

        var resolver = CreateResolver(configuration);

        var result = resolver.GetProviderConfigurationValue("MY_KEY");

        Assert.Equal("runtime_value", result);
    }

    // ---------------------------------------------------------------------------
    // Persistence of defaults on first run
    // ---------------------------------------------------------------------------

    [Fact]
    public void Initialize_WhenNoRuntimeFile_PersistsDefaultsToRuntimeFile()
    {
        WriteDefaultsFile(new { MY_KEY = "default_value" });
        Assert.False(File.Exists(_runtimeFilePath));

        var resolver = CreateResolver();
        resolver.GetProviderConfigurationValue("MY_KEY"); // trigger initialisation

        Assert.True(File.Exists(_runtimeFilePath), "Runtime config file should be created on first run.");
        var json = File.ReadAllText(_runtimeFilePath);
        using var doc = JsonDocument.Parse(json);
        Assert.True(doc.RootElement.TryGetProperty("ProviderConfigurations", out var section));
        Assert.True(section.TryGetProperty("MY_KEY", out var prop));
        Assert.Equal("default_value", prop.GetString());
    }

    [Fact]
    public void Initialize_WhenKeyAlreadyInRuntime_DoesNotOverwriteWithDefault()
    {
        WriteDefaultsFile(new { MY_KEY = "default_value" });
        WriteRuntimeFile(new { MY_KEY = "existing_runtime_value" });

        var resolver = CreateResolver();
        resolver.GetProviderConfigurationValue("MY_KEY"); // trigger initialisation

        // runtime file should still contain the original runtime value
        var json = File.ReadAllText(_runtimeFilePath);
        using var doc = JsonDocument.Parse(json);
        doc.RootElement.TryGetProperty("ProviderConfigurations", out var section);
        section.TryGetProperty("MY_KEY", out var prop);
        Assert.Equal("existing_runtime_value", prop.GetString());
    }

    [Fact]
    public void Initialize_WhenKeyHasDeploymentOverride_DefaultNotPersistedToRuntime()
    {
        WriteDefaultsFile(new { MY_KEY = "default_value" });

        var configuration = new ConfigurationBuilder()
            .AddInMemoryCollection(new[]
            {
                new System.Collections.Generic.KeyValuePair<string, string?>(
                    "ProviderConfigurationOverrides:MY_KEY", "override_value")
            })
            .Build();

        var resolver = CreateResolver(configuration);
        resolver.GetProviderConfigurationValue("MY_KEY");

        // The runtime file should have been initialised empty (no MY_KEY persisted because override covered it).
        if (File.Exists(_runtimeFilePath))
        {
            var json = File.ReadAllText(_runtimeFilePath);
            using var doc = JsonDocument.Parse(json);
            if (doc.RootElement.TryGetProperty("ProviderConfigurations", out var section))
            {
                Assert.False(section.TryGetProperty("MY_KEY", out _),
                    "Key covered by a deployment override must not be persisted to the runtime file.");
            }
        }
    }

    // ---------------------------------------------------------------------------
    // Malformed runtime JSON
    // ---------------------------------------------------------------------------

    [Fact]
    public void Initialize_MalformedRuntimeJson_FallsBackToDefaultsGracefully()
    {
        WriteDefaultsFile(new { MY_KEY = "default_value" });
        File.WriteAllText(_runtimeFilePath, "{ this is not valid json }");

        var resolver = CreateResolver();

        // Should not throw; should resolve from defaults.
        var result = resolver.GetProviderConfigurationValue("MY_KEY");
        Assert.Equal("default_value", result);
    }

    [Fact]
    public void Initialize_RuntimeJsonMissingProviderConfigurationsProperty_FallsBackToDefaultsGracefully()
    {
        WriteDefaultsFile(new { MY_KEY = "default_value" });
        File.WriteAllText(_runtimeFilePath, "{\"SomeOtherProperty\": {}}");

        var resolver = CreateResolver();

        var result = resolver.GetProviderConfigurationValue("MY_KEY");
        Assert.Equal("default_value", result);
    }

    // ---------------------------------------------------------------------------
    // GetRequiredField
    // ---------------------------------------------------------------------------

    [Fact]
    public void GetRequiredField_ExtractsStringFieldFromJsonValue()
    {
        WriteDefaultsFile(new { MY_KEY = "{\"Host\":\"https://example.com\",\"Port\":\"8080\"}" });
        var resolver = CreateResolver();

        var host = resolver.GetRequiredField("MY_KEY", "Host");
        var port = resolver.GetRequiredField("MY_KEY", "Port");

        Assert.Equal("https://example.com", host);
        Assert.Equal("8080", port);
    }

    [Fact]
    public void GetRequiredField_MissingField_ThrowsInvalidOperationException()
    {
        WriteDefaultsFile(new { MY_KEY = "{\"Host\":\"https://example.com\"}" });
        var resolver = CreateResolver();

        var ex = Assert.Throws<InvalidOperationException>(() => resolver.GetRequiredField("MY_KEY", "MissingField"));
        Assert.Contains("MissingField", ex.Message);
    }

    [Fact]
    public void GetRequiredField_ValueIsNotJson_ThrowsInvalidOperationException()
    {
        WriteDefaultsFile(new { MY_KEY = "not-a-json-object" });
        var resolver = CreateResolver();

        Assert.Throws<InvalidOperationException>(() => resolver.GetRequiredField("MY_KEY", "Field"));
    }

    // ---------------------------------------------------------------------------
    // Thread safety: multiple concurrent calls should not cause double-initialisation errors
    // ---------------------------------------------------------------------------

    [Fact]
    public void GetProviderConfigurationValue_ConcurrentCalls_ReturnConsistentResults()
    {
        WriteDefaultsFile(new { MY_KEY = "default_value" });
        var resolver = CreateResolver();

        var results = new string[100];
        System.Threading.Tasks.Parallel.For(0, 100, i =>
        {
            results[i] = resolver.GetProviderConfigurationValue("MY_KEY");
        });

        Assert.All(results, r => Assert.Equal("default_value", r));
    }

    // ---------------------------------------------------------------------------
    // Testable subclass that injects configurable file paths
    // ---------------------------------------------------------------------------

    private class TestableProviderConfigurationResolver : ProviderConfigurationResolver
    {
        private readonly string _defaultsFilePath;
        private readonly string _runtimeFilePath;

        public TestableProviderConfigurationResolver(
            IConfiguration configuration,
            ILogger<ProviderConfigurationResolver> logger,
            string defaultsFilePath,
            string runtimeFilePath)
            : base(configuration, logger)
        {
            _defaultsFilePath = defaultsFilePath;
            _runtimeFilePath = runtimeFilePath;
        }

        protected override string GetDefaultsFilePath() => _defaultsFilePath;

        protected override string GetRuntimeFilePath() => _runtimeFilePath;
    }
}
