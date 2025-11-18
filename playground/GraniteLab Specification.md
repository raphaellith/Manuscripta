# Project Specification: Granite Lab (Local Ollama Playground)

## 1\. Executive Summary

**Granite Lab** is a modular, local-first testing environment for Large Language Models (LLMs), specifically optimized for the IBM Granite family running via Ollama. It provides a graphical interface to benchmark performance, inspect Retrieval-Augmented Generation (RAG) internals, test multimodal capabilities (OCR/Vision), and compare models side-by-side.

The application is built as a "zero-setup" Python tool: it requires only a running Ollama instance and a single `pip install` to boot.

-----

## 2\. System Architecture

### 2.1 Tech Stack

  * **Frontend:** Streamlit (Python-based UI, supports hot-reloading and sidebar navigation).
  * **Orchestration:** LangChain (Standardizes API calls across Chat, Vision, and RAG).
  * **Vector Database:** ChromaDB (running in `EphemeralClient` mode for in-memory experimentation).
  * **LLM Backend:** Ollama (via `langchain-ollama`).

### 2.2 Directory Structure

The project follows a multi-page Streamlit application structure.

```text
/granite_lab
│
├── main.py                  # Application Entry Point (Home/Landing)
├── requirements.txt         # Python Dependencies
├── README.md                # Setup Guide
│
├── /core                    # Backend Logic
│   ├── __init__.py
│   ├── client.py            # Wrapper for LLM interaction & Telemetry
│   ├── rag.py               # Vector Store & Retrieval Logic
│   └── utils.py             # Helpers (Base64 encoding, JSON parsing)
│
└── /pages                   # UI Modules (Streamlit Pages)
    ├── 01_Chat_Lounge.py
    ├── 02_RAG_Inspector.py
    ├── 03_Vision_Lab.py
    └── 04_Model_Arena.py
```

-----

## 3\. Core Module Specifications

### 3.1 `core/client.py` (The Engine)

This module manages the connection to Ollama and captures performance metrics.

  * **Class:** `ModelEngine`
  * **Responsibilities:**
      * Initialize `ChatOllama` with configurable parameters (`temperature`, `top_k`).
      * **Telemetry Capture:** Must capture and calculate:
          * *Time to First Token (TTFT)*: Latency measurement.
          * *Generation Speed*: Tokens per second (TPS).
          * *Total Tokens*: Input + Output counts.
      * **Metric Calculation:**
        $$\text{TPS} = \frac{\text{eval\_count}}{\text{eval\_duration (seconds)}}$$
        *(Note: Ollama returns duration in nanoseconds; convert accordingly.)*

### 3.2 `core/rag.py` (The Brain)

Handles document ingestion and retrieval transparency.

  * **Class:** `RAGExperiment`
  * **Dependencies:** `langchain_chroma`, `pypdf`
  * **Configuration:**
      * **Embedding Model:** Defaults to `granite-embedding` (if available) or `nomic-embed-text`.
      * **Chunking:** `RecursiveCharacterTextSplitter` (Default chunk size: 1000, Overlap: 200).
      * **Vector Store:** `Chroma(client=EphemeralClient())`. This ensures no junk data is left behind after the session closes.
  * **Key Method:** `retrieve_with_scores(query)`
      * Must return not just the answer, but the **raw chunks** and their **similarity scores** (L2 distance or Cosine similarity) to display in the UI.

-----

## 4\. UI Page Specifications

### 4.1 Page 1: Chat Lounge (`01_Chat_Lounge.py`)

  * **Purpose:** Standard chat interaction with deep telemetry.
  * **Sidebar Controls:**
      * **Model Selector:** Dynamic dropdown populated by `ollama list`.
      * **System Prompt:** Text Area (Default: "You are a helpful AI assistant.").
      * **Parameters:** Sliders for Temperature (0.0-1.0) and Context Window.
  * **Main View:**
      * Chat Interface: Standard `st.chat_message` history.
      * **Telemetry Dashboard:** A fixed or sticky status bar showing the TPS and Latency of the last message generated.

### 4.2 Page 2: RAG Inspector (`02_RAG_Inspector.py`)

  * **Purpose:** Debug retrieval quality.
  * **Workflow:**
    1.  **File Upload:** `st.file_uploader` (PDF/TXT support).
    2.  **Ingestion:** Process file $\rightarrow$ Chunk $\rightarrow$ Embed $\rightarrow$ Store.
    3.  **Query:** User asks a question.
    4.  **Visualization:**
          * **Top:** The generated answer.
          * **Bottom:** "Retrieved Context" Expanders.
          * *Requirement:* Show the exact text segment retrieved and its relevance score.

### 4.3 Page 3: Vision Lab (`03_Vision_Lab.py`)

  * **Purpose:** Test multimodal models (e.g., `granite-vision`, `llava`).
  * **Workflow:**
    1.  **Image Upload:** Drag & drop (PNG/JPG).
    2.  **Preview:** Display the uploaded image.
    3.  **Prompt:** Text input (Default: "Transcribe all text visible in this image.").
    4.  **Processing:** Convert image to Base64 $\rightarrow$ Send to `ChatOllama` using a Multimodal Message payload.
    5.  **Output:** Markdown rendering of the result (crucial for testing handwriting/table transcription).

### 4.4 Page 4: Model Arena (`04_Model_Arena.py`)

  * **Purpose:** A/B testing using the same prompt.
  * **Layout:** Two parallel columns (`col1`, `col2`).
  * **Controls:**
      * **Model A:** Dropdown.
      * **Model B:** Dropdown.
      * **Shared Prompt:** Single text input box.
  * **Execution:**
      * The "Run" button triggers **asynchronous** calls to both models.
      * Both outputs render simultaneously to allow real-time speed comparison.

-----

## 5\. Implementation Requirements

### 5.1 Prerequisites

The system assumes the following are installed on the host machine:

1.  **Ollama:** Running as a background service (`ollama serve`).
2.  **Python:** 3.10+.

### 5.2 `requirements.txt`

The project requires the following Python packages:

```text
streamlit>=1.35.0
langchain>=0.2.0
langchain-ollama>=0.1.0
langchain-chroma>=0.1.0
chromadb>=0.5.0
pypdf>=3.0.0
pillow>=10.0.0
```

### 5.3 Error Handling Guidelines

  * **Ollama Connection:** On application startup, the code must ping the Ollama endpoint (`localhost:11434`). If unreachable, display a friendly `st.error` banner: *"Ollama is not running. Please launch it with `ollama serve`."*
  * **Model Availability:** If a user selects a model (e.g., `granite-vision`) that is not pulled, the app should catch the `ModelNotFoundError` and offer a specific command to fix it: `ollama pull granite-vision`.

### 5.4 UX Polish

  * **Session State:** Chat history and uploaded documents must persist in `st.session_state` when switching between pages.
  * **Theming:** Enforce a clean, technical aesthetic using Streamlit's config (monospace fonts for code blocks).