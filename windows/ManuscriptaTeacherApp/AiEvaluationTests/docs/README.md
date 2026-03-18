# AI Evaluation Tests Overview

This directory contains the automated evaluation test suites used to validate the Generative AI capabilities of the Manuscripta Windows Application. These tests are functionally isolated from the `MainTests` suite as they intentionally trigger heavily compute-bound inference processes (Ollama, ChromaDB) and execute using statistical repetition.

## Architecture & Framework
- **Framework**: xUnit
- **Context Generation**: `AiEvaluationWebApplicationFactory` sets up an in-memory SQL configuration but intentionally preserves connections to ChromaDB and Ollama so that real AI inferences run during tests.
- **Non-Deterministic Aggregation**: Because Generative AI is non-deterministic, tests inherently loop workflows iteratively ($N \ge 10$) instead of simply running once. Asserts are evaluated mathematically on the aggregated results to avoid intermittent test failures caused by expected outlier hallmarks of LLM generation.

## Evaluated Test Suites

### 1. `RAGLatencyTests.cs`
- **Purpose**: Evaluates the latency of embedding and storing highly dense chunks into the vector store.
- **Mechanism**: Instructs `DocumentEmbeddingService` to index a dataset spanning thousands of tokens, forcing ChromaDB to execute rapid storage ops.
- **Dependencies**: For realistic stress-testing, place a generic dense baseline transcript in `AiEvaluationTests/Data/HeavyTranscript.txt`. A fallback repeating string will be generated if missing.

### 2. `MaterialGenerationTests.cs`
- **Purpose**: Evaluates the model's ability to ingest RAG context and stream curriculum-aligned markdown out.
- **Mechanism**: Tests `IMaterialGenerationService` with strict reading age bounds and requires the system to generate a functional worksheet format, parsing whether `!!! question-draft` syntax blocks were correctly emitted.

### 3. `ContentModificationTests.cs`
- **Purpose**: Evaluates the AI assistant's accuracy in modifying localized texts according to pedagogical instructions without hallucinating.
- **Mechanism**: Prompts the `IContentModificationService` to simplify complex terminology (e.g. cellular biology notes) for an 8-year-old.
- **Validation**:
  - Uses string matching to verify formatting fidelity (ensuring core entity names like "mitochondria" remain intact).
  - Asserts that the Flesch-Kincaid readability score mathematically falls on average compared to the baseline string.

### 4. `FeedbackGenerationTests.cs`
- **Purpose**: Evaluates the grading strictness and validation compliance of automatic assessment logic.
- **Mechanism**: Mimics a CAIE Example Candidate Response via `WrittenAnswerResponseEntity` and queries the `IFeedbackGenerationService`.
- **Validation**: Ensures that across repeated generational runs, grading assigns the mathematically accurate expected numeric marks. Also tests format strictness to ensure internal syntax tokens (`MARK: X`) are cleanly parsed out and hidden from the text strings.
