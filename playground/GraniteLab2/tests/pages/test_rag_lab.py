import pytest
from unittest.mock import MagicMock, patch
import sys
import os
import importlib.util

# Mock streamlit before importing page
mock_st = MagicMock()
sys.modules["streamlit"] = mock_st

# Add parent dir to path
sys.path.append(os.path.abspath(os.path.join(os.path.dirname(__file__), '../../')))

# Load the page module dynamically
rag_lab_path = os.path.join(os.path.dirname(__file__), '../../pages/03_RAG_Lab.py')
spec = importlib.util.spec_from_file_location("pages.rag_lab", rag_lab_path)
rag_lab = importlib.util.module_from_spec(spec)
sys.modules["pages.rag_lab"] = rag_lab
spec.loader.exec_module(rag_lab)

class MockSessionState(dict):
    def __getattr__(self, key):
        if key in self:
            return self[key]
        raise AttributeError(f"'MockSessionState' object has no attribute '{key}'")

    def __setattr__(self, key, value):
        self[key] = value

class TestRAGLab:
    def setup_method(self):
        mock_st.reset_mock()
        mock_st.session_state = MockSessionState()
        mock_st.sidebar = MagicMock()
        mock_st.sidebar.__enter__.return_value = mock_st.sidebar
        mock_st.spinner = MagicMock()
        mock_st.spinner.__enter__.return_value = None
        mock_st.expander = MagicMock()
        mock_st.expander.__enter__.return_value = None
        mock_st.chat_message = MagicMock()
        mock_st.chat_message.__enter__.return_value = None
        mock_st.empty = MagicMock()
        mock_st.empty.return_value = MagicMock()

    def test_init_session_state(self):
        with patch('pages.rag_lab.RAGEngine') as mock_engine_cls:
            rag_lab.init_session_state()
            assert "rag_engine" in mock_st.session_state
            assert "rag_messages" in mock_st.session_state
            assert "selected_model" in mock_st.session_state
            mock_engine_cls.assert_called_once()

    def test_main_no_models(self):
        with patch('pages.rag_lab.LLMClient') as mock_client_cls:
            mock_client = mock_client_cls.return_value
            mock_client.check_connection.return_value = True
            mock_client.list_models.return_value = []
            
            # Mock file uploader to return None
            mock_st.file_uploader.return_value = None
            
            rag_lab.main()
            
            mock_st.warning.assert_called_with("No models found. Please pull a model using `ollama pull <model>`.")

    def test_main_with_models(self):
        with patch('pages.rag_lab.LLMClient') as mock_client_cls:
            mock_client = mock_client_cls.return_value
            mock_client.check_connection.return_value = True
            mock_client.list_models.return_value = ["model1"]
            
            # Mock file uploader to return None
            mock_st.file_uploader.return_value = None
            # Mock chat input to return None
            mock_st.chat_input.return_value = None
            
            # Mock selectbox return value
            mock_st.selectbox.return_value = "model1"
            
            rag_lab.main()
            
            mock_st.selectbox.assert_called()
            assert mock_st.session_state.selected_model == "model1"

    def test_main_with_preselected_model(self):
        with patch('pages.rag_lab.LLMClient') as mock_client_cls:
            mock_client = mock_client_cls.return_value
            mock_client.check_connection.return_value = True
            mock_client.list_models.return_value = ["model1", "model2"]
            
            mock_st.session_state.selected_model = "model2"
            
            # Mock file uploader to return None
            mock_st.file_uploader.return_value = None
            # Mock chat input to return None
            mock_st.chat_input.return_value = None
            
            rag_lab.main()
            
            mock_st.selectbox.assert_called()
            # Check if index was 1
            args, kwargs = mock_st.selectbox.call_args
            assert kwargs['index'] == 1

    def test_file_upload(self):
        with patch('pages.rag_lab.LLMClient') as mock_client_cls:
            mock_client = mock_client_cls.return_value
            mock_client.check_connection.return_value = True
            mock_client.list_models.return_value = ["model1"]
            
            # Mock file uploader
            mock_file = MagicMock()
            mock_file.name = "test.pdf"
            mock_file.read.return_value = b"content"
            mock_st.file_uploader.return_value = mock_file
            
            # Mock RAGEngine in session state
            mock_engine = MagicMock()
            mock_engine.ingest_file.return_value = ["chunk1", "chunk2"]
            mock_st.session_state.rag_engine = mock_engine
            
            # Ensure current_file is not set so ingestion happens
            if "current_file" in mock_st.session_state:
                del mock_st.session_state["current_file"]
            
            rag_lab.main()
            
            mock_engine.clear.assert_called_once()
            mock_engine.ingest_file.assert_called_once()
            mock_st.success.assert_called()
            assert mock_st.session_state.current_file == "test.pdf"

    def test_file_upload_already_processed(self):
        with patch('pages.rag_lab.LLMClient') as mock_client_cls:
            mock_client = mock_client_cls.return_value
            mock_client.check_connection.return_value = True
            mock_client.list_models.return_value = ["model1"]
            
            # Mock file uploader
            mock_file = MagicMock()
            mock_file.name = "test.pdf"
            mock_st.file_uploader.return_value = mock_file
            
            # Mock RAGEngine in session state
            mock_engine = MagicMock()
            mock_st.session_state.rag_engine = mock_engine
            
            # Set current_file to simulate already processed
            mock_st.session_state.current_file = "test.pdf"
            
            rag_lab.main()
            
            mock_engine.ingest_file.assert_not_called()

    def test_chat_interaction(self):
        with patch('pages.rag_lab.LLMClient') as mock_client_cls:
            mock_client = mock_client_cls.return_value
            mock_client.check_connection.return_value = True
            mock_client.list_models.return_value = ["model1"]
            
            mock_st.session_state.selected_model = "model1"
            mock_st.session_state.rag_engine = MagicMock()
            mock_st.session_state.rag_engine.retrieve.return_value = [MagicMock(page_content="context")]
            
            # Mock chat input
            mock_st.chat_input.return_value = "Hello"
            
            # Mock streaming response
            mock_client.chat.return_value = [
                {'message': {'content': 'Hi '}},
                {'message': {'content': 'there'}}
            ]
            
            rag_lab.main()
            
            mock_st.session_state.rag_engine.retrieve.assert_called()
            mock_client.chat.assert_called()
            
            # Check history updated
            assert len(mock_st.session_state.rag_messages) == 2 # User + Assistant
            assert mock_st.session_state.rag_messages[-1]["content"] == "Hi there"

    def test_history_rendering_with_context(self):
        with patch('pages.rag_lab.LLMClient') as mock_client_cls:
            mock_client = mock_client_cls.return_value
            mock_client.check_connection.return_value = True
            mock_client.list_models.return_value = ["model1"]
            
            # Mock file uploader to return None
            mock_st.file_uploader.return_value = None
            
            # Setup history with context
            mock_st.session_state.rag_messages = [
                {"role": "user", "content": "Hi"},
                {"role": "assistant", "content": "Hello", "retrieved_context": [MagicMock(page_content="ctx")]}
            ]
            
            rag_lab.main()
            
            # Verify expander called for context
            mock_st.expander.assert_called_with("View Retrieved Context (X-Ray)")

    def test_chat_interaction_no_context(self):
        with patch('pages.rag_lab.LLMClient') as mock_client_cls:
            mock_client = mock_client_cls.return_value
            mock_client.check_connection.return_value = True
            mock_client.list_models.return_value = ["model1"]
            
            mock_st.session_state.selected_model = "model1"
            mock_st.session_state.rag_engine = MagicMock()
            mock_st.session_state.rag_engine.retrieve.return_value = []
            
            # Mock chat input
            mock_st.chat_input.return_value = "Hello"
            
            # Mock streaming response
            mock_client.chat.return_value = [
                {'message': {'content': 'Hi'}}
            ]
            
            rag_lab.main()
            
            mock_st.session_state.rag_engine.retrieve.assert_called()
            # Verify system prompt fallback
            call_args = mock_client.chat.call_args
            assert call_args is not None
            messages = call_args[1]['messages']
            assert messages[0]['content'] == "You are a helpful assistant."

    def test_chat_interaction_error(self):
        with patch('pages.rag_lab.LLMClient') as mock_client_cls:
            mock_client = mock_client_cls.return_value
            mock_client.check_connection.return_value = True
            mock_client.list_models.return_value = ["model1"]
            
            mock_st.session_state.selected_model = "model1"
            mock_st.session_state.rag_engine = MagicMock()
            mock_st.session_state.rag_engine.retrieve.return_value = []
            
            # Mock chat input
            mock_st.chat_input.return_value = "Hello"
            
            # Mock error
            mock_client.chat.side_effect = Exception("API Error")
            
            rag_lab.main()
            
            mock_st.error.assert_called_with("Error generating response: API Error")
