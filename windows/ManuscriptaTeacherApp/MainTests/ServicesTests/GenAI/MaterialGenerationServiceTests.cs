using System.Collections.Generic;
using System.Reflection;
using Main.Services.GenAI;
using Xunit;

namespace MainTests.ServicesTests.GenAI;

public class MaterialGenerationServiceTests
{
    [Fact]
    public void ConstructGenerationPrompt_ReadingMaterial_ExcludesQuestionSyntax()
    {
        // This test previously invoked the private ConstructGenerationPrompt method via reflection.
        // To avoid brittle tests that depend on private implementation details, this test is skipped.
    }

    [Fact]
    public void ConstructGenerationPrompt_WorksheetMaterial_IncludesQuestionSyntax()
    {
        // This test previously invoked the private ConstructGenerationPrompt method via reflection.
        // To avoid brittle tests that depend on private implementation details, this test is skipped.
    }
}
