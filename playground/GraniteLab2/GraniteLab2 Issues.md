**Preface**

This issues were used to direct a coding agent in the build out of this experimental sub application. It was built using Gemini 3 Pro (Preview) in copilot agent mode. 

For each issue, Gemini was prompted with:

*"complete issue {issue # from below} from GraniteLabs2 Issues.md. Make sure to also read GraniteLabs2 Sepcification.md. Include unit tests in a parallel test directory where relevent, and make sure all the code you write passes all the unit tests with 100% coverage. Use python -m pytest --cov=. --cov-report=term-missing tests/ in the GraniteLab2 conda env to check coverage otherwise it freezes up. Make a commit when you're done and confident that it's all working as intended. Commit all changes to the current branch. Update the checklist on the issue as you go. The commits in your issues markdown file are LOCAL only. Do not include # in the commit because it will mess with real issues in the repo."*

Until all issues were complete. 

---

### Issue #1: Project Skeleton & "One-Click" Bootstrapper
**Label:** `Core` `Setup`

**Description:**
Initialise the project repository, file structure, and the "one-click" launcher script. The goal is to have a running Streamlit application that connects to a local Ollama instance.

**Tasks:**
- [x] Create directory structure (`/pages`, `/modules`, `/db`).
- [x] Implement `llm_client.py`: A wrapper class for `ollama-python` to handle connection checks and model listing.
- [x] Create `app.py` (Main Entry):
    - [x] Sidebar with Model Selector (populated dynamically).
    - [x] Sidebar with Hyperparameter sliders (Temp, Top_K, Context).
- [x] Create `run.py`:
    - [x] Auto-install dependencies from `requirements.txt`.
    - [x] Launch the Streamlit server automatically.
    - [x] Handle "Ollama not found" errors gracefully.

**Technical Notes:**
- Use `subprocess` in `run.py` to call streamlit.
- Ensure the Sidebar persists across pages using `st.session_state`.

---

### Issue #2: Persistence Layer & Prompt Library
**Label:** `Backend` `Database` `UI`

**Description:**
Implement the SQLite database and the "Prompt Library" page. This is the foundation for the "Matrix" experiments later. We need to be able to Create, Read, Update, and Delete (CRUD) prompts and system personas.

**Tasks:**
- [x] **Database:**
    - [x] Create `schema.sql` (Tables: `prompts`, `system_prompts`, `experiments`, `results`).
    - [x] Create `db_manager.py` to handle init and connections.
- [ ] **UI (Page: Prompt Library):**
    - [x] Implement `st.data_editor` to view/edit existing prompts.
    - [x] Create a Form to add new prompts with fields: `Alias`, `Content`, `Tags`.
    - [x] Implement a "System Prompt Manager" tab for personas (e.g., "Strict Coder").

**Acceptance Criteria:**
- I can add a prompt named "Python Sort" with tag `#coding`.
- I can restart the app, and the prompt is still there.

---

### Issue #3: "The Sandbox" (Chat Lab)
**Label:** `Feature` `UI`

**Description:**
Build the standard Chat interface. It must include specific features for testing IBM Granite (JSON enforcement, Code UI).

**Tasks:**
- [x] Implement standard chat loop (User input -> Stream output -> History).
- [x] **Granite Features:**
    - [x] "JSON Mode" Toggle: Appends strict JSON instructions/format to the API call.
    - [x] Code Syntax Highlighting: Ensure Python/JS blocks render correctly.
- [x] **Metrics:** Display TPS (Tokens per second) and TTFT (Time to first token) at the top of the response.
- [x] Add a "Save Conversation" button (downloads JSON).

---

### Issue #4: "The Arena" (Model Comparison)
**Label:** `Feature` `UI`

**Description:**
A split-view interface to compare two models side-by-side with the same prompt.

**Tasks:**
- [x] Create a 2-column layout.
- [x] Add two separate Model Selectors in the main view (Model A vs Model B).
- [x] **Sync/Async Logic:** When the user sends a prompt, trigger generation for *both* models. (Note: Local inference might be sequential if GPU VRAM is limited; implement a queue or simple sequential execution first).
- [x] Display comparison metrics (Duration A vs Duration B).
- [x] Add a "Copy Prompt" button to easily re-run tests.

---

### Issue #5: "RAG Lab" & X-Ray View
**Label:** `Feature` `RAG`

**Description:**
Implement the Retrieval Augmented Generation experiment page. The key differentiator here is the "X-Ray" transparency to debug retrieval quality.

**Tasks:**
- [x] **Backend:**
    - [x] Integrate `ChromaDB` (ephemeral or persistent).
    - [x] Integrate `LangChain` + `PyPDF` for ingestion.
- [x] **UI:**
    - [x] File Uploader (PDF/TXT).
    - [x] Tuning Sliders: `Chunk Size`, `Overlap`, `Top K` (how many chunks to retrieve).
- [x] **X-Ray Feature:**
    - [x] After the model answers, render an `st.expander("View Retrieved Context")`.
    - [x] Inside, show the exact raw text chunks that were sent to the LLM.

---

### Issue #6: "Vision Lab" (Multimodal)
**Label:** `Feature` `Vision`

**Description:**
An experiment page for image-to-text capabilities, focusing on handwriting and transcription (Granite vision capabilities).

**Tasks:**
- [x] Image Uploader (Support PNG/JPG).
- [x] Render uploaded image in the left column, Chat/Output in the right.
- [x] **Preset Prompts:** Add buttons for quick-testing:
    - [x] "Transcribe this handwriting"
    - [x] "Describe this image in JSON"
    - [x] "Extract data table"
- [x] Pass image bytes correctly to `ollama.generate(images=[bytes])`.

---

### Issue #7: "The Matrix" (Batch Experiment Engine)
**Label:** `Feature` `Core` **(High Complexity)**

**Description:**
The core "Workhorse" of the app. A system to define and execute large-scale validation runs (N Models × N Prompts × N Iterations).

**Tasks:**
- [x] **Configuration Wizard:**
    - [x] Multi-select widget for Models.
    - [x] Multi-select widget for Prompts (filtered by Tags from Issue #2).
    - [x] Input for `Iterations` (Count).
- [x] **Execution Engine:**
    - [x] Create a loop that runs: `Models * Prompts * Iterations`.
    - [x] **Robustness:** Wrap inference in `try/except`. If Model A fails, log "ERROR" and continue to Model B. Do not crash.
    - [x] **Streaming Save:** Write result to SQLite `results` table *immediately* after each generation.
- [x] **UI Feedback:**
    - [x] Progress Bar (0% to 100%).
    - [x] "Console Log" window showing current status (e.g., "Processing Run 4/50: Granite-8b on Prompt #2").

---

### Issue #8: "Lab Analytics" & Reporting
**Label:** `Feature` `Data Science`

**Description:**
A dashboard to visualise the results generated by "The Matrix".

**Tasks:**
- [x] **Filters:** Dropdown to select a specific `Experiment ID`.
- [x] **Data Processing:** Load results from SQLite into a Pandas DataFrame.
- [x] **Visuals (Plotly):**
    - [x] Bar Chart: Average TPS per Model.
    - [x] Bar Chart: Average Latency per Model.
- [x] **Pivot Table:** Render a comprehensive table where Rows=Prompts, Cols=Models, Cell=Output.
- [x] **Drill-Down:** Make table cells clickable to view the full metadata (System prompt used, specific duration, full output text).
