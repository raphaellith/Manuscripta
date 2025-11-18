# GraniteLab 2.0

GraniteLab is a local-first, Python-based Laboratory Environment designed for the rigorous testing, benchmarking, and experimentation of Large Language Models (LLMs). Optimised for IBM Granite but agnostic to any Ollama model, it prioritises **metrics**, **reproducibility**, and **persistence**.

## Features

*   **The Sandbox (Chat Lab):** Quick interactions with JSON mode and code highlighting.
*   **The Arena:** Side-by-side model comparison with diff views.
*   **RAG Lab:** Retrieval Augmented Generation with "X-Ray" view to debug retrieved context.
*   **Vision Lab:** Multimodal testing for OCR and image description.
*   **Prompt Library:** Manage and version control your prompts and system personas.
*   **The Matrix:** Batch experiment engine to run N Models × N Prompts × N Iterations.
*   **Analytics:** Visualise performance metrics (TPS, Latency) and qualitative results.

## Development Process

This project was built using an AI-assisted development process. The `GraniteLab2 Issues.md` file served as the central coordination document.

**Workflow:**
1.  **Issue Definition:** Tasks were broken down into specific issues (e.g., "Issue #1: Project Skeleton").
2.  **AI Prompting:** An AI agent (Gemini 3 Pro) was prompted with the specific issue and context.
3.  **Implementation:** The agent wrote code, tests, and updated the issue checklist.
4.  **Verification:** Unit tests with 100% coverage were required for each issue.

**Key Documents:**
*   `GraniteLab2 Specification.md`: Detailed system architecture and requirements.
*   `GraniteLab2 Issues.md`: The log of tasks and their completion status.

## Architecture

*   **Frontend:** Streamlit
*   **Backend:** Python
*   **Inference:** Ollama (Local)
*   **Database:** SQLite (Prompts, Results)
*   **Vector Store:** ChromaDB (RAG)

