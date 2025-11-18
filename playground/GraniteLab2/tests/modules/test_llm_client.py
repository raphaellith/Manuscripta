import pytest
from unittest.mock import patch, MagicMock
from modules.llm_client import LLMClient
import ollama

class TestLLMClient:
    @pytest.fixture
    def client(self):
        return LLMClient()

    def test_check_connection_success(self, client):
        with patch('ollama.list') as mock_list:
            mock_list.return_value = {'models': []}
            assert client.check_connection() is True

    def test_check_connection_failure(self, client):
        with patch('ollama.list') as mock_list:
            mock_list.side_effect = Exception("Connection refused")
            assert client.check_connection() is False

    def test_list_models_success(self, client):
        with patch('ollama.list') as mock_list:
            mock_list.return_value = {
                'models': [
                    {'name': 'granite-code:8b'},
                    {'name': 'llama3:latest'}
                ]
            }
            models = client.list_models()
            assert len(models) == 2
            assert 'granite-code:8b' in models
            assert 'llama3:latest' in models

    def test_list_models_empty(self, client):
        with patch('ollama.list') as mock_list:
            mock_list.return_value = {} # Unexpected format or empty
            assert client.list_models() == []

    def test_list_models_exception(self, client):
        with patch('ollama.list') as mock_list:
            mock_list.side_effect = Exception("Error")
            assert client.list_models() == []
