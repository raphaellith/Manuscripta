# Generative AI Component Specifications (Windows)

## Explanatory Note

This document defines the specifications for the application's lightweight `GenAI` component, which enables the `Main` component to access generative AI functionalities provided by IBM Granite and Ollama.



## Section 1 - General Principles

(1) This component must include the following files:

    (a) `ollama-granite.py`, a Python script that satisifies requirements listed in Section 2.

    (b) `OllamaSetup.exe`, the Windows installer officially provided by Ollama.

(2) It must be possible for the script `ollama-granite.py`, together with its dependencies, to be compiled via `pyinstaller` into a single directory that contains:

    (a) `ollama-granite.exe`, a Windows executable file.

    (b) all necessary supporting files and Python libraries.




## Section 2 - Requirements for `ollama-granite.py`

(1) When `ollama-granite.py` is run, it must: 

    (a) accept one additional command line argument `prompt`.
        
        (i) If no such additional argument is provided, it should raise an exception and terminate immediately.
        
        (ii) It should ignore other extraneous command line arguments, if present.

    (b) check whether Ollama is installed by running `ollama --version`. If not, it must run `ollama-granite.exe` to set up Ollama and add its executable to the user's PATH environment variable.

    (c) check whether the `granite4` model has been locally installed by running `ollama list`. If not, it must run `ollama pull granite4` to complete local installation of the model.

    (d) run the command `ollama serve` and allow it to continue running.

    (e) output to `stdout` the message content of the chat response created by feeding the `prompt` to the `granite4` model.
