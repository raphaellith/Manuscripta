using Main.Models.Enums;

namespace Main.Models.Entities;

/// <summary>
/// Represents per-device configuration overrides.
/// Per ConfigurationManagementSpecification §2(2)(a): stores only fields that deviate from defaults.
/// Null fields indicate "use default value".
/// Short-term persisted per PersistenceAndCascadingRules §1(2).
/// </summary>
public class ConfigurationOverride
{
    /// <summary>
    /// Overridden text size, or null to use default.
    /// Per §2G(1)(a): if set, must be between 5 and 50 (inclusive).
    /// </summary>
    public int? TextSize { get; set; }

    /// <summary>
    /// Overridden feedback style, or null to use default.
    /// Per §2G(1)(b).
    /// </summary>
    public FeedbackStyle? FeedbackStyle { get; set; }

    /// <summary>
    /// Overridden TTS setting, or null to use default.
    /// Per §2G(1)(c).
    /// </summary>
    public bool? TtsEnabled { get; set; }

    /// <summary>
    /// Overridden AI scaffolding setting, or null to use default.
    /// Per §2G(1)(d).
    /// </summary>
    public bool? AiScaffoldingEnabled { get; set; }

    /// <summary>
    /// Overridden summarisation setting, or null to use default.
    /// Per §2G(1)(e).
    /// </summary>
    public bool? SummarisationEnabled { get; set; }

    /// <summary>
    /// Overridden mascot selection, or null to use default.
    /// Per §2G(1)(f).
    /// </summary>
    public MascotSelection? MascotSelection { get; set; }

    /// <summary>
    /// Returns true if all fields are null (no overrides set).
    /// </summary>
    public bool IsEmpty =>
        TextSize == null &&
        FeedbackStyle == null &&
        TtsEnabled == null &&
        AiScaffoldingEnabled == null &&
        SummarisationEnabled == null &&
        MascotSelection == null;
}
