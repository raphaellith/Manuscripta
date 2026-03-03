namespace Main.Models.Entities;

/// <summary>
/// Identifies the inference runtime variant.
/// Per AdditionalValidationRules §3F(1).
/// </summary>
public enum InferenceRuntime
{
    STANDARD,
    OPENVINO
}
