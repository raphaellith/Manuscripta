namespace Main.Services.RuntimeDependencies;

/// <summary>
/// Resolves provider configuration values for runtime dependencies using
/// the precedence defined in BuildAndDeploymentSpec Section 9.
/// </summary>
public interface IProviderConfigurationResolver
{
    /// <summary>
    /// Returns the provider configuration value for a key.
    /// </summary>
    string GetProviderConfigurationValue(string key);

    /// <summary>
    /// Returns a required field from a provider configuration value represented as JSON.
    /// </summary>
    string GetRequiredField(string key, string fieldName);
}
