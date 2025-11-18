import pytest
from unittest.mock import MagicMock, patch
from modules.llm_client import LLMClient

def test_generate_success():
    client = LLMClient()
    mock_response = [{'response': 'Hello'}, {'response': ' World'}]
    
    with patch('modules.llm_client.ollama.generate', return_value=mock_response) as mock_generate:
        result = client.generate(model="test-model", prompt="test prompt", images=[b'fake_image_bytes'])
        
        assert result == mock_response
        mock_generate.assert_called_once_with(
            model="test-model",
            prompt="test prompt",
            images=[b'fake_image_bytes'],
            options=None,
            stream=True
        )

def test_generate_exception():
    client = LLMClient()
    
    with patch('modules.llm_client.ollama.generate', side_effect=Exception("Ollama error")):
        with pytest.raises(Exception) as excinfo:
            client.generate(model="test-model", prompt="test prompt")
        
        assert "Ollama error" in str(excinfo.value)
