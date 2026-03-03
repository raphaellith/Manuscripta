using System.ComponentModel.DataAnnotations;

namespace Main.Models.Entities;

/// <summary>
/// Singleton entity persisting the user's preferred inference runtime.
/// Per AdditionalValidationRules §3F and PersistenceAndCascadingRules §1(1)(k).
/// </summary>
public class InferencePreferenceEntity
{
    [Key]
    public Guid Id { get; set; }

    /// <summary>
    /// The preferred runtime variant (STANDARD or OPENVINO).
    /// </summary>
    public InferenceRuntime PreferredRuntime { get; set; }

    public InferencePreferenceEntity() { }

    public InferencePreferenceEntity(Guid id, InferenceRuntime preferredRuntime)
    {
        Id = id;
        PreferredRuntime = preferredRuntime;
    }
}
