using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;
using Main.Models.Enums;

namespace Main.Models.Entities;

/// <summary>
/// Represents the configuration for a student device.
/// Per Validation Rules.md §2G(1).
/// Long-term persisted for default values per PersistenceAndCascadingRules §1(1)(h).
/// Per ConfigurationManagementSpecification §1(2).
/// </summary>
[Table("Configurations")]
public class ConfigurationEntity
{
    /// <summary>
    /// Well-known singleton ID for the global defaults row.
    /// </summary>
    public static readonly Guid DefaultId = new("00000000-0000-0000-0000-000000000001");

    [Key]
    [DatabaseGenerated(DatabaseGeneratedOption.None)]
    public Guid Id { get; set; }

    /// <summary>
    /// The text size for the student device.
    /// Per §2G(1)(a): Value must be between 5 and 50 (inclusive).
    /// </summary>
    public int TextSize { get; set; }

    /// <summary>
    /// The feedback style for the student device.
    /// Per §2G(1)(b).
    /// </summary>
    public FeedbackStyle FeedbackStyle { get; set; }

    /// <summary>
    /// Whether text-to-speech is enabled.
    /// Per §2G(1)(c).
    /// </summary>
    public bool TtsEnabled { get; set; }

    /// <summary>
    /// Whether AI scaffolding is enabled.
    /// Per §2G(1)(d).
    /// </summary>
    public bool AiScaffoldingEnabled { get; set; }

    /// <summary>
    /// Whether summarisation is enabled.
    /// Per §2G(1)(e).
    /// </summary>
    public bool SummarisationEnabled { get; set; }

    /// <summary>
    /// The mascot selection for the student device.
    /// Per §2G(1)(f).
    /// </summary>
    public MascotSelection MascotSelection { get; set; }

    public ConfigurationEntity() { }

    public ConfigurationEntity(
        Guid id,
        int textSize,
        FeedbackStyle feedbackStyle,
        bool ttsEnabled,
        bool aiScaffoldingEnabled,
        bool summarisationEnabled,
        MascotSelection mascotSelection)
    {
        Id = id;
        TextSize = textSize;
        FeedbackStyle = feedbackStyle;
        TtsEnabled = ttsEnabled;
        AiScaffoldingEnabled = aiScaffoldingEnabled;
        SummarisationEnabled = summarisationEnabled;
        MascotSelection = mascotSelection;
    }

    /// <summary>
    /// Creates a default configuration entity per ConfigurationManagementSpecification Appendix 1.
    /// </summary>
    public static ConfigurationEntity CreateDefault()
    {
        return new ConfigurationEntity(
            id: DefaultId,
            textSize: 6,
            feedbackStyle: FeedbackStyle.IMMEDIATE,
            ttsEnabled: false,
            aiScaffoldingEnabled: false,
            summarisationEnabled: false,
            mascotSelection: MascotSelection.NONE);
    }
}
