"""
ModelEngine: Wrapper for LLM interaction with telemetry capture.
"""

import time
from typing import Dict, Optional, Any
from langchain_ollama import ChatOllama


class ModelEngine:
    """
    Manages connection to Ollama and captures performance metrics.
    
    Captures:
    - Time to First Token (TTFT)
    - Generation Speed (Tokens per second)
    - Total Tokens (Input + Output)
    """
    
    def __init__(
        self,
        model_name: str,
        temperature: float = 0.7,
        top_k: int = 40,
        base_url: str = "http://localhost:11434"
    ):
        """
        Initialize the ModelEngine.
        
        Args:
            model_name: Name of the Ollama model
            temperature: Sampling temperature (0.0-1.0)
            top_k: Top-k sampling parameter
            base_url: Ollama server URL
        """
        self.model_name = model_name
        self.temperature = temperature
        self.top_k = top_k
        self.base_url = base_url
        
        self.llm = ChatOllama(
            model=model_name,
            temperature=temperature,
            top_k=top_k,
            base_url=base_url
        )
        
        self.last_metrics: Optional[Dict[str, Any]] = None
    
    def invoke(self, prompt: str, system_prompt: Optional[str] = None) -> str:
        """
        Invoke the model and capture telemetry.
        
        Args:
            prompt: User prompt
            system_prompt: Optional system prompt
            
        Returns:
            Generated response text
        """
        messages = []
        if system_prompt:
            messages.append(("system", system_prompt))
        messages.append(("human", prompt))
        
        start_time = time.time()
        first_token_time = None
        
        try:
            # Stream the response to capture TTFT
            full_response = ""
            for chunk in self.llm.stream(messages):
                if first_token_time is None:
                    first_token_time = time.time()
                if hasattr(chunk, 'content'):
                    full_response += chunk.content
                else:
                    full_response += str(chunk)
            
            end_time = time.time()
            
            # Calculate metrics
            total_duration = end_time - start_time
            ttft = (first_token_time - start_time) if first_token_time else 0
            
            # Get additional metadata if available
            # Note: Ollama metadata might be in response_metadata
            try:
                # Try to get the last response with metadata
                response = self.llm.invoke(messages)
                metadata = getattr(response, 'response_metadata', {})
                
                # Extract token counts and timing from metadata
                eval_count = metadata.get('eval_count', 0)
                eval_duration = metadata.get('eval_duration', 0)
                prompt_eval_count = metadata.get('prompt_eval_count', 0)
                
                # Convert nanoseconds to seconds
                eval_duration_sec = eval_duration / 1_000_000_000 if eval_duration else total_duration
                
                # Calculate TPS
                tps = eval_count / eval_duration_sec if eval_duration_sec > 0 else 0
                
                self.last_metrics = {
                    'ttft': ttft,
                    'total_duration': total_duration,
                    'tps': tps,
                    'input_tokens': prompt_eval_count,
                    'output_tokens': eval_count,
                    'total_tokens': prompt_eval_count + eval_count
                }
            except Exception:
                # Fallback metrics if metadata is unavailable
                estimated_tokens = len(full_response.split())
                estimated_tps = estimated_tokens / total_duration if total_duration > 0 else 0
                
                self.last_metrics = {
                    'ttft': ttft,
                    'total_duration': total_duration,
                    'tps': estimated_tps,
                    'input_tokens': 0,
                    'output_tokens': estimated_tokens,
                    'total_tokens': estimated_tokens
                }
            
            return full_response
            
        except Exception as e:
            raise Exception(f"Error invoking model: {str(e)}")
    
    def invoke_multimodal(
        self,
        prompt: str,
        image_data: str,
        system_prompt: Optional[str] = None
    ) -> str:
        """
        Invoke the model with image input (for vision models).
        
        Args:
            prompt: Text prompt
            image_data: Base64-encoded image data
            system_prompt: Optional system prompt
            
        Returns:
            Generated response text
        """
        from langchain_core.messages import HumanMessage
        
        messages = []
        if system_prompt:
            messages.append(("system", system_prompt))
        
        # Create multimodal message
        message_content = [
            {"type": "text", "text": prompt},
            {"type": "image_url", "image_url": f"data:image/jpeg;base64,{image_data}"}
        ]
        
        messages.append(HumanMessage(content=message_content))
        
        start_time = time.time()
        
        try:
            response = self.llm.invoke(messages)
            end_time = time.time()
            
            total_duration = end_time - start_time
            
            self.last_metrics = {
                'ttft': 0,  # Streaming not used for multimodal
                'total_duration': total_duration,
                'tps': 0,
                'input_tokens': 0,
                'output_tokens': 0,
                'total_tokens': 0
            }
            
            return response.content if hasattr(response, 'content') else str(response)
            
        except Exception as e:
            raise Exception(f"Error invoking multimodal model: {str(e)}")
    
    def get_last_metrics(self) -> Optional[Dict[str, Any]]:
        """
        Get metrics from the last invocation.
        
        Returns:
            Dictionary containing metrics or None
        """
        return self.last_metrics
    
    @staticmethod
    def check_ollama_connection(base_url: str = "http://localhost:11434") -> bool:
        """
        Check if Ollama is running and accessible.
        
        Args:
            base_url: Ollama server URL
            
        Returns:
            True if connection successful, False otherwise
        """
        import requests
        
        try:
            response = requests.get(f"{base_url}/api/tags", timeout=5)
            return response.status_code == 200
        except Exception:
            return False
    
    @staticmethod
    def list_available_models(base_url: str = "http://localhost:11434") -> list:
        """
        List all available models in Ollama.
        
        Args:
            base_url: Ollama server URL
            
        Returns:
            List of model names
        """
        import requests
        
        try:
            response = requests.get(f"{base_url}/api/tags", timeout=5)
            if response.status_code == 200:
                data = response.json()
                return [model['name'] for model in data.get('models', [])]
            return []
        except Exception:
            return []
