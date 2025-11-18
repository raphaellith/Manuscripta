import ollama
from ollama import Client
import os
from typing import List, Dict, Any

class LLMClient:
    def __init__(self, host: str = None):
        # Default to 127.0.0.1 which is often more reliable than localhost
        self.host = host or os.getenv('OLLAMA_HOST', 'http://127.0.0.1:11434')
        self.client = Client(host=self.host)

    def check_connection(self) -> bool:
        """Checks if the Ollama server is reachable."""
        try:
            self.client.list()
            return True
        except Exception as e:
            print(f"Connection check failed for {self.host}: {e}")
            return False

    def list_models(self) -> List[str]:
        """Lists available models from Ollama."""
        try:
            models_response = self.client.list()
            
            # Handle object response (newer ollama lib)
            if hasattr(models_response, 'models'):
                return [m.model for m in models_response.models]
            
            # Handle dict response (older ollama lib or raw api)
            if isinstance(models_response, dict) and 'models' in models_response:
                return [m.get('name', m.get('model')) for m in models_response['models']]
                
            return []
        except Exception:
            return []

    def chat(self, model: str, messages: List[Dict[str, str]], options: Dict[str, Any] = None, stream: bool = True, **kwargs):
        """
        Sends a chat request to the Ollama model.
        """
        try:
            return self.client.chat(model=model, messages=messages, options=options, stream=stream, **kwargs)
        except Exception as e:
            # In a real app, we might want to log this properly
            raise e

    def generate(self, model: str, prompt: str, images: List[bytes] = None, options: Dict[str, Any] = None, stream: bool = True, **kwargs):
        """
        Sends a generate request to the Ollama model (useful for vision tasks).
        """
        try:
            return self.client.generate(model=model, prompt=prompt, images=images, options=options, stream=stream, **kwargs)
        except Exception as e:
            raise e
