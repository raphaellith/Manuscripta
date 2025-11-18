import ollama
from typing import List, Dict, Any

class LLMClient:
    def __init__(self, host: str = None):
        # ollama python client uses OLLAMA_HOST env var or default localhost:11434
        # We can allow passing a host if needed, but usually it's env var based.
        pass

    def check_connection(self) -> bool:
        """Checks if the Ollama server is reachable."""
        try:
            ollama.list()
            return True
        except Exception:
            return False

    def list_models(self) -> List[str]:
        """Lists available models from Ollama."""
        try:
            models_response = ollama.list()
            # The response structure from ollama.list() is usually {'models': [{'name': '...', ...}]}
            if 'models' in models_response:
                return [m['name'] for m in models_response['models']]
            return []
        except Exception:
            return []

    def chat(self, model: str, messages: List[Dict[str, str]], options: Dict[str, Any] = None, stream: bool = True, **kwargs):
        """
        Sends a chat request to the Ollama model.
        """
        try:
            return ollama.chat(model=model, messages=messages, options=options, stream=stream, **kwargs)
        except Exception as e:
            # In a real app, we might want to log this properly
            raise e
