namespace Main.Models.Dtos;

/// <summary>
/// Represents a single chunk of streaming AI generation output.
/// Per GenAISpec.md §3H(3).
/// </summary>
/// <param name="Token">The text token for this chunk.</param>
/// <param name="IsThinking">True if this token is part of chain-of-thought reasoning (&lt;think&gt; block).</param>
/// <param name="Done">True if this is the final chunk of the generation.</param>
public record StreamingGenerationChunk(string Token, bool IsThinking, bool Done);
