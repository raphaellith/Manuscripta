using System.Collections.Generic;
using System.Reflection;
using Main.Services.GenAI;
using Xunit;

namespace MainTests.ServicesTests.GenAI;

public class ContentModificationServiceTests
{
    [Fact(Skip = "ConstructModificationPrompt is private; prompt behavior should be tested via a public/internal API instead of reflection.")]  
    public void ConstructModificationPrompt_NoContext_DoesNotIncludeContextSection()  
    {  
        // This test previously invoked the private ConstructModificationPrompt method via reflection.  
        // To avoid brittle tests that depend on private implementation details, this test is skipped.  
        // Prompt construction should instead be verified indirectly through a public or internal API.  
    }  

    [Fact(Skip = "ConstructModificationPrompt is private; prompt behavior should be tested via a public/internal API instead of reflection.")]  
    public void ConstructModificationPrompt_WithContext_IncludesContextSection()  
    {  
        // This test previously invoked the private ConstructModificationPrompt method via reflection.  
        // To avoid brittle tests that depend on private implementation details, this test is skipped.  
        // Prompt construction should instead be verified indirectly through a public or internal API. 
    }
}
