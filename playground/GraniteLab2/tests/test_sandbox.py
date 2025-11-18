import pytest
from unittest.mock import MagicMock, patch
import sys
import os
import importlib

# Mock streamlit before importing the page
sys.modules['streamlit'] = MagicMock()
import streamlit as st

# Add the parent directory to sys.path to import pages
sys.path.append(os.path.abspath(os.path.join(os.path.dirname(__file__), '..')))

# Import the module using importlib because of the filename
sandbox = importlib.import_module("pages.01_Sandbox")

class MockSessionState(dict):
    def __getattr__(self, key):
        if key in self:
            return self[key]
        raise AttributeError(f"'MockSessionState' object has no attribute '{key}'")

    def __setattr__(self, key, value):
        self[key] = value

class TestSandbox:
    def setup_method(self):
        # Reset session state mock
        st.session_state = MockSessionState()
        st.session_state.messages = []
        st.session_state.json_mode = False
        
        # Reset other mocks
        st.reset_mock()
        st.sidebar = MagicMock()
        st.sidebar.__enter__.return_value = st.sidebar
        st.chat_message = MagicMock()
        st.chat_message.return_value.__enter__.return_value = MagicMock()
        
        # Reset button side effects
        st.button.side_effect = None
        st.button.return_value = False
        st.chat_input.return_value = None

    def test_init_chat_state(self):
        # Clear state to test initialization
        st.session_state = MockSessionState()
        
        sandbox.init_chat_state()
        
        assert "messages" in st.session_state
        assert st.session_state.messages == []
        assert "json_mode" in st.session_state
        assert st.session_state.json_mode is False

    def test_init_chat_state_preserves_existing(self):
        st.session_state.messages = [{"role": "user", "content": "hi"}]
        st.session_state.json_mode = True
        
        sandbox.init_chat_state()
        
        assert st.session_state.messages == [{"role": "user", "content": "hi"}]
        assert st.session_state.json_mode is True

    def test_main_layout(self):
        with patch.object(sandbox, 'LLMClient') as mock_client_cls:
            sandbox.main()
            
            st.set_page_config.assert_called_with(page_title="Chat Lab", layout="wide")
            st.title.assert_called_with("💬 Chat Lab")
            # st.header is called in sidebar, but it's st.header
            st.header.assert_any_call("🧪 Lab Settings")

    def test_chat_interaction(self):
        with patch.object(sandbox, 'LLMClient') as mock_client_cls:
            mock_client = mock_client_cls.return_value
            mock_client.chat.return_value = iter([
                {'message': {'content': 'Hello'}, 'done': False},
                {'message': {'content': ' World'}, 'done': True, 'eval_count': 2, 'eval_duration': 100000000}
            ])
            
            # Mock user input
            st.chat_input.return_value = "Hi"
            
            # Mock session state for model selection
            st.session_state.selected_model = "granite-code:8b"
            
            sandbox.main()
            
            # Check for errors
            if st.error.called:
                print(f"st.error called with: {st.error.call_args}")
            
            # Verify user message added
            assert {"role": "user", "content": "Hi"} in st.session_state.messages
            
            # Verify client.chat called
            mock_client.chat.assert_called()
            call_args = mock_client.chat.call_args[1]
            assert call_args['model'] == "granite-code:8b"
            assert call_args['messages'][-1]['content'] == "Hi"
            
            # Verify assistant response added
            assert {"role": "assistant", "content": "Hello World"} in st.session_state.messages

    def test_chat_interaction_exception(self):
        with patch.object(sandbox, 'LLMClient') as mock_client_cls:
            mock_client = mock_client_cls.return_value
            mock_client.chat.side_effect = Exception("Chat error")
            
            st.chat_input.return_value = "Hi"
            st.session_state.selected_model = "granite-code:8b"
            
            sandbox.main()
            
            st.error.assert_called_with("An error occurred: Chat error")

    def test_chat_no_model_selected(self):
        with patch.object(sandbox, 'LLMClient') as mock_client_cls:
            st.chat_input.return_value = "Hi"
            st.session_state.selected_model = None
            
            sandbox.main()
            
            st.error.assert_called_with("Please select a model in the sidebar.")

    def test_clear_history(self):
        with patch.object(sandbox, 'LLMClient'):
            st.session_state.messages = [{"role": "user", "content": "hi"}]
            
            # Mock button click
            st.button.return_value = True
            
            sandbox.main()
            
            assert st.session_state.messages == []
            st.rerun.assert_called()

    def test_save_conversation(self):
        with patch.object(sandbox, 'LLMClient'):
            st.session_state.messages = [{"role": "user", "content": "hi"}]
            
            sandbox.main()
            
            st.download_button.assert_called()
            call_args = st.download_button.call_args[1]
            assert call_args['file_name'] == "conversation.json"
            assert "hi" in call_args['data']
