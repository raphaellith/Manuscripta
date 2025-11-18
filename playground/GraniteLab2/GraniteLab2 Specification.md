
# Project Specification: GraniteLab
**Version:** 2.0
**Target Models:** Optimised for IBM Granite (Code/Instruct) but agnostic to any Ollama model.

## 1. Executive Summary
The Ollama Workbench is a local-first, Python-based Laboratory Environment designed for the rigorous testing, benchmarking, and experimentation of Large Language Models (LLMs). Unlike standard chat interfaces, this system prioritises **metrics**, **reproducibility**, and **persistence**. It enables users to curate prompt libraries, execute large-scale "Matrix Experiments" (batch processing), and analyse historical performance data using a clean, modular GUI.

## 2. System Architecture

### 2.1 Technology Stack
*   **Runtime:** Python 3.10+
*   **User Interface:** `Streamlit` (chosen for native hot-reloading, state management, and data visualisation).
*   **Inference Engine:** `Ollama` (via `ollama` Python library).
*   **Data Persistence:** `SQLite` (Single-file relational database for prompts/results).
*   **Vector Storage (RAG):** `ChromaDB` (Local, ephemeral or persistent vector store).
*   **Data Analysis:** `Pandas` (Data manipulation) + `Plotly` (Interactive charting).
*   **Document Processing:** `LangChain` + `PyPDF` + `Pillow`.

### 2.2 Data Architecture (SQLite Schema)
The application relies on a `workbench.db` file to ensure all data is saved locally and permanently.

**Table: `prompts`**
*   `id` (PK): Integer
*   `alias`: String (Short name, e.g., "Python Sort Test")
*   `content`: Text (The actual prompt text with `{{variables}}`)
*   `tags`: String (Comma-separated, e.g., "coding, granite, v1")
*   `created_at`: Timestamp

**Table: `system_prompts`**
*   `id` (PK): Integer
*   `alias`: String (e.g., "Senior Dev Persona")
*   `content`: Text

**Table: `experiments`**
*   `id` (PK): Integer
*   `name`: String (e.g., "Granite 8B vs Llama 3 Benchmark")
*   `experiment_type`: String (chat, rag, vision)
*   `config`: JSON (Stores hyperparameters used: temp, top_k, etc.)
*   `created_at`: Timestamp

**Table: `results`**
*   `id` (PK): Integer
*   `experiment_id` (FK): Link to `experiments`
*   `model`: String (Model name)
*   `prompt_content`: Text (The exact prompt sent)
*   `output`: Text (The model response)
*   `duration_ms`: Float (Total generation time)
*   `tps`: Float (Tokens per second)
*   `ttft_ms`: Float (Time to first token)
*   `timestamp`: Timestamp

## 3. Global UI Components
These elements persist across all pages.

*   **Sidebar Controls:**
    *   **Model Selector:** Dynamic dropdown populated by `ollama list`.
    *   **Hyperparameters:** Sliders for `Temperature`, `Top P`, `Context Window`.
    *   **System Prompt:** Dropdown to select from `system_prompts` DB or custom input.
    *   **Hardware Stats:** (Optional) Simple RAM/VRAM usage indicator.

## 4. Functional Modules (Pages)

The application is structured as a Multi-Page Streamlit App.

### 4.1 Page: Chat Lab (The Sandbox)
*   **Goal:** Quick, one-off interactions and sanity checks.
*   **Features:**
    *   Standard chat interface with history.
    *   **JSON Mode Toggle:** Forces valid JSON output (critical for Granite enterprise tests).
    *   **Code View:** Syntax highlighting with "Copy" button.
    *   **Metrics Header:** Shows TPS and Duration for the last message generated.

### 4.2 Page: The Arena (Comparison)
*   **Goal:** Side-by-side qualitative evaluation.
*   **Layout:** Two-column split view.
*   **Workflow:**
    *   Select Model A (Left) and Model B (Right).
    *   Single input field sends prompt to both asynchronously.
*   **Analysis:**
    *   Diff View: Highlight textual differences.
    *   Winner Selection: User can click "A is better", "B is better", or "Tie" (saved to session log).

### 4.3 Page: RAG Lab (Retrieval)
*   **Goal:** Test model performance on external data.
*   **Workflow:**
    1.  **Ingest:** Upload PDF/TXT. System chunks and embeds into ChromaDB.
    2.  **Tuning:** Slider for `k` (number of chunks to retrieve) and `chunk_size`.
    3.  **Query:** Chat with the document.
*   **Verification:**
    *   **"X-Ray" Panel:** An expandable section under the answer displaying the exact raw text chunks retrieved from the database.
    *   **Relevance Score:** Display the vector distance score (if available via Chroma).

### 4.4 Page: Vision Lab (Multimodal)
*   **Goal:** Test OCR and Image Description.
*   **Workflow:** Upload Image -> Select Task (Transcribe/Describe) -> Generate.
*   **Granite Focus:** Specific prompt templates for "Handwriting Transcription" and "Table to JSON extraction."

### 4.5 Page: Prompt Library (Management)
*   **Goal:** Manage the assets for experiments.
*   **Features:**
    *   **Data Editor:** A spreadsheet-like view of the `prompts` table.
    *   **Add New:** Form to create prompts/system prompts with auto-tagging.
    *   **Import/Export:** CSV upload/download for prompt sets.

### 4.6 Page: The Matrix (Batch Experiments)
*   **Goal:** High-volume, automated testing.
*   **Configuration Wizard:**
    1.  **Select Models:** Multi-select (e.g., Granite-8b, Granite-3b, Mistral).
    2.  **Select Prompts:** Multi-select from Library OR specific Tags (e.g., run all `#python` prompts).
    3.  **Iterations:** Integer (e.g., run each combination 5 times).
*   **Execution:**
    *   Iterates through: `Models * Prompts * Iterations`.
    *   **Real-time Dashboard:** Progress bar and a "Latest Logs" terminal view.
    *   **Auto-Save:** Writes to `results` table immediately after every inference.

### 4.7 Page: Analytics (Results & Insights)
*   **Goal:** Review data from "The Matrix."
*   **Features:**
    *   **Experiment Selector:** Dropdown to choose which run to analyze.
    *   **Leaderboard:** Bar charts for Average TPS and Average Latency.
    *   **Qualitative Table:** A Pivot Table (Rows=Prompts, Cols=Models) showing the outputs.
    *   **Drill Down:** Clicking a result cell opens a modal with full details (system prompt used, exact duration, full output).
    *   **Export:** Download specific experiment results as CSV/Excel.

## 5. Implementation Roadmap & File Structure

### 5.1 Directory Structure
```text
ollama-workbench/
├── run.py                 # The "One Script" bootstrapper
├── app.py                 # Main Streamlit Entry Point
├── requirements.txt       # Dependencies
├── db/
│   ├── schema.sql         # Database initialization
│   └── workbench.db       # Created on runtime
├── modules/               # Core Logic (Separated from UI)
│   ├── db_manager.py      # SQLite context manager
│   ├── llm_client.py      # Ollama API wrapper
│   ├── rag_engine.py      # Vector store logic
│   └── analytics.py       # Pandas data processing
└── pages/                 # Streamlit Interface Pages
    ├── 01_Sandbox.py
    ├── 02_The_Arena.py
    ├── 03_RAG_Lab.py
    ├── 04_Vision_Lab.py
    ├── 05_Prompt_Library.py
    ├── 06_The_Matrix.py
    └── 07_Analytics.py
```

### 5.2 The "One Script" Bootstrapper (`run.py`)
To meet the requirement of ease of use, `run.py` will:
1.  Check if a generic virtual environment exists (optional).
2.  Install/Update `requirements.txt`.
3.  Initialize the SQLite DB if missing.
4.  Launch `streamlit run app.py`.

## 6. Non-Functional Requirements
*   **Error Handling:** If a model crashes during a Batch Experiment (The Matrix), the loop must catch the exception, log the error in the DB as "FAILED", and continue to the next item. It must not crash the app.
*   **Theme:** The app will enforce a clean layout using `st.set_page_config(layout="wide")` and respect the user's system Light/Dark mode.
*   **Granite Optimization:** Default system prompts provided in the seed database will be optimized for IBM Granite (e.g., specific formatting instructions).