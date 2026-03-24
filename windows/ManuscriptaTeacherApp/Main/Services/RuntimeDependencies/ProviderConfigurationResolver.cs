using System.Text.Json;
using Microsoft.Extensions.Configuration;
using Microsoft.Extensions.Logging;

namespace Main.Services.RuntimeDependencies;

/// <summary>
/// Resolves provider configuration entries using precedence:
/// runtime source > deployment overrides > bundled defaults.
/// </summary>
public class ProviderConfigurationResolver : IProviderConfigurationResolver
{
    private const string RuntimeDirectoryName = "ManuscriptaTeacherApp";
    private const string RuntimeConfigSubdirectory = "config";
    private const string RuntimeConfigFileName = "provider-config.json";
    private const string DefaultsFileName = "provider-config-defaults.json";

    private readonly IConfiguration _configuration;
    private readonly ILogger<ProviderConfigurationResolver> _logger;
    private readonly object _sync = new();

    private bool _initialized;
    private Dictionary<string, string> _runtimeValues = new(StringComparer.OrdinalIgnoreCase);
    private Dictionary<string, string> _deploymentOverrides = new(StringComparer.OrdinalIgnoreCase);
    private Dictionary<string, string> _defaults = new(StringComparer.OrdinalIgnoreCase);

    public ProviderConfigurationResolver(
        IConfiguration configuration,
        ILogger<ProviderConfigurationResolver> logger)
    {
        _configuration = configuration;
        _logger = logger;
    }

    public string GetProviderConfigurationValue(string key)
    {
        if (string.IsNullOrWhiteSpace(key))
        {
            throw new ArgumentException("Provider configuration key cannot be empty.", nameof(key));
        }

        EnsureInitialized();

        if (_runtimeValues.TryGetValue(key, out var runtimeValue) && !string.IsNullOrWhiteSpace(runtimeValue))
        {
            return runtimeValue;
        }

        if (_deploymentOverrides.TryGetValue(key, out var deploymentValue) && !string.IsNullOrWhiteSpace(deploymentValue))
        {
            return deploymentValue;
        }

        if (_defaults.TryGetValue(key, out var defaultValue) && !string.IsNullOrWhiteSpace(defaultValue))
        {
            return defaultValue;
        }

        throw new InvalidOperationException($"Provider configuration key '{key}' cannot be resolved.");
    }

    public string GetRequiredField(string key, string fieldName)
    {
        if (string.IsNullOrWhiteSpace(fieldName))
        {
            throw new ArgumentException("Field name cannot be empty.", nameof(fieldName));
        }

        var value = GetProviderConfigurationValue(key);

        try
        {
            using var doc = JsonDocument.Parse(value);
            if (!doc.RootElement.TryGetProperty(fieldName, out var property))
            {
                throw new InvalidOperationException(
                    $"Provider configuration key '{key}' is missing required field '{fieldName}'.");
            }

            if (property.ValueKind != JsonValueKind.String)
            {
                throw new InvalidOperationException(
                    $"Provider configuration key '{key}' field '{fieldName}' must be a string.");
            }

            var fieldValue = property.GetString();
            if (string.IsNullOrWhiteSpace(fieldValue))
            {
                throw new InvalidOperationException(
                    $"Provider configuration key '{key}' field '{fieldName}' is empty.");
            }

            return fieldValue;
        }
        catch (JsonException ex)
        {
            throw new InvalidOperationException(
                $"Provider configuration key '{key}' does not contain valid JSON.", ex);
        }
    }

    private void EnsureInitialized()
    {
        if (_initialized)
        {
            return;
        }

        lock (_sync)
        {
            if (_initialized)
            {
                return;
            }

            _defaults = LoadDefaults();
            _deploymentOverrides = LoadDeploymentOverrides();
            _runtimeValues = LoadRuntimeValues();

            PersistResolvedDefaultsToRuntimeSource();

            _initialized = true;
        }
    }

    private Dictionary<string, string> LoadDefaults()
    {
        var defaultsPath = Path.Combine(AppContext.BaseDirectory, DefaultsFileName);
        if (!File.Exists(defaultsPath))
        {
            throw new InvalidOperationException(
                $"Bundled provider defaults artifact not found at '{defaultsPath}'.");
        }

        using var doc = JsonDocument.Parse(File.ReadAllText(defaultsPath));
        if (!doc.RootElement.TryGetProperty("ProviderConfigurations", out var providerConfigurations)
            || providerConfigurations.ValueKind != JsonValueKind.Object)
        {
            throw new InvalidOperationException(
                $"Bundled provider defaults artifact '{defaultsPath}' is missing 'ProviderConfigurations'.");
        }

        var result = new Dictionary<string, string>(StringComparer.OrdinalIgnoreCase);
        foreach (var property in providerConfigurations.EnumerateObject())
        {
            if (property.Value.ValueKind == JsonValueKind.String)
            {
                var value = property.Value.GetString();
                if (!string.IsNullOrWhiteSpace(value))
                {
                    result[property.Name] = value;
                }
            }
        }

        return result;
    }

    private Dictionary<string, string> LoadDeploymentOverrides()
    {
        var section = _configuration.GetSection("ProviderConfigurationOverrides");
        var result = new Dictionary<string, string>(StringComparer.OrdinalIgnoreCase);

        foreach (var child in section.GetChildren())
        {
            if (!string.IsNullOrWhiteSpace(child.Value))
            {
                result[child.Key] = child.Value!;
            }
        }

        return result;
    }

    private Dictionary<string, string> LoadRuntimeValues()
    {
        var runtimeFilePath = GetRuntimeFilePath();
        var runtimeDirectory = Path.GetDirectoryName(runtimeFilePath)!;
        Directory.CreateDirectory(runtimeDirectory);

        if (!File.Exists(runtimeFilePath))
        {
            // Initialize empty runtime configuration source on first run.
            File.WriteAllText(runtimeFilePath, "{\n  \"ProviderConfigurations\": {}\n}\n");
        }

        using var doc = JsonDocument.Parse(File.ReadAllText(runtimeFilePath));
        if (!doc.RootElement.TryGetProperty("ProviderConfigurations", out var providerConfigurations)
            || providerConfigurations.ValueKind != JsonValueKind.Object)
        {
            _logger.LogWarning("Runtime provider configuration file is malformed. Reinitializing: {Path}", runtimeFilePath);
            File.WriteAllText(runtimeFilePath, "{\n  \"ProviderConfigurations\": {}\n}\n");
            return new Dictionary<string, string>(StringComparer.OrdinalIgnoreCase);
        }

        var result = new Dictionary<string, string>(StringComparer.OrdinalIgnoreCase);
        foreach (var property in providerConfigurations.EnumerateObject())
        {
            if (property.Value.ValueKind == JsonValueKind.String)
            {
                var value = property.Value.GetString();
                if (!string.IsNullOrWhiteSpace(value))
                {
                    result[property.Name] = value;
                }
            }
        }

        return result;
    }

    private void PersistResolvedDefaultsToRuntimeSource()
    {
        var runtimeFilePath = GetRuntimeFilePath();
        var changed = false;

        // Per BuildAndDeploymentSpec §9(4): if runtime and deployment are missing,
        // load spec default and persist it to runtime source.
        foreach (var key in _defaults.Keys)
        {
            if (_runtimeValues.ContainsKey(key) || _deploymentOverrides.ContainsKey(key))
            {
                continue;
            }

            _runtimeValues[key] = _defaults[key];
            changed = true;
        }

        if (!changed)
        {
            return;
        }

        var wrapper = new Dictionary<string, Dictionary<string, string>>(StringComparer.OrdinalIgnoreCase)
        {
            ["ProviderConfigurations"] = _runtimeValues
        };

        var json = JsonSerializer.Serialize(wrapper, new JsonSerializerOptions
        {
            WriteIndented = true
        });

        File.WriteAllText(runtimeFilePath, json + Environment.NewLine);
        _logger.LogInformation("Persisted default provider configuration values to runtime source: {Path}", runtimeFilePath);
    }

    private static string GetRuntimeFilePath()
    {
        var appDataRoot = Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData);
        return Path.Combine(appDataRoot, RuntimeDirectoryName, RuntimeConfigSubdirectory, RuntimeConfigFileName);
    }
}
