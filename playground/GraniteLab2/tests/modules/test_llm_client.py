import pytest
from unittest.mock import patch, MagicMock
from modules.llm_client import LLMClient
import ollama

class TestLLMClient:
    @pytest.fixture
    def client(self):
        with patch('modules.llm_client.Client') as MockClient:
            client_instance = LLMClient()
            # We need to set the mock client on the instance because __init__ creates it
            # But since we patched the class, client.client is already a mock (the return value of the class constructor)
            return client_instance

    def test_check_connection_success(self, client):
        client.client.list.return_value = {'models': []}
        assert client.check_connection() is True

    def test_check_connection_failure(self, client):
        client.client.list.side_effect = Exception("Connection refused")
        assert client.check_connection() is False

    def test_list_models_success(self, client):
        client.client.list.return_value = {
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
        client.client.list.return_value = {} # Unexpected format or empty
        assert client.list_models() == []

    def test_list_models_exception(self, client):
        client.client.list.side_effect = Exception("Error")
        assert client.list_models() == []

    def test_chat_success(self, client):
        client.client.chat.return_value = iter([{'message': {'content': 'Hello'}}])
        response = client.chat(model='test-model', messages=[{'role': 'user', 'content': 'Hi'}])
        assert next(response) == {'message': {'content': 'Hello'}}
        client.client.chat.assert_called_once_with(
            model='test-model',
            messages=[{'role': 'user', 'content': 'Hi'}],
            options=None,
            stream=True
        )

    def test_chat_failure(self, client):
        client.client.chat.side_effect = Exception("Chat error")
        with pytest.raises(Exception) as excinfo:
            client.chat(model='test-model', messages=[])
        assert "Chat error" in str(excinfo.value)
