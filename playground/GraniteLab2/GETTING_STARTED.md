# Getting Started with GraniteLab

It is incredibly easy to get GraniteLab up and running.

## Prerequisites

1.  **Python 3.10+**: Ensure you have Python installed.
2.  **Ollama**: Download and install [Ollama](https://ollama.com/).
    *   Make sure Ollama is running (`ollama serve` or via the desktop app).
    *   Pull a model to test with, e.g., `ollama pull granite-code:8b`.

## One-Command Launch

We have provided a "one-click" bootstrapper script that handles dependency installation and launches the app.

Open your terminal, navigate to the `playground/GraniteLab2` directory, and run:

```bash
python run.py
```

**That's it!**

The script will:
1.  Install all required Python packages from `requirements.txt`.
2.  Check if Ollama is reachable.
3.  Launch the Streamlit interface in your default browser.

## Manual Installation (Optional)

If you prefer to manage your environment manually:

1.  Create a virtual environment:
    ```bash
    python -m venv venv
    source venv/bin/activate  # On Windows: venv\Scripts\activate
    ```
2.  Install dependencies:
    ```bash
    pip install -r requirements.txt
    ```
3.  Run the app:
    ```bash
    streamlit run app.py
    ```

## Troubleshooting

*   **"Ollama not found":** Ensure the Ollama application is running in the background.
*   **Database Errors:** The app creates a `db/workbench.db` file automatically. If you encounter schema errors, try deleting this file to let the app regenerate it (warning: this deletes your saved prompts/results).
