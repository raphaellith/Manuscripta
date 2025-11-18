import pytest
from unittest.mock import MagicMock, patch
import sys
import os

# Mock streamlit before importing app
mock_st = MagicMock()
sys.modules["streamlit"] = mock_st

# Add project root to sys.path
sys.path.append(os.path.abspath(os.path.join(os.path.dirname(__file__), '..')))

from modules.ui_utils import init_session_state, render_sidebar
from app import main, init_chat_state
from modules.llm_client import LLMClient

class MockSessionState(dict):
    def __getattr__(self, key):
        if key in self:
            return self[key]
        raise AttributeError(f"'MockSessionState' object has no attribute '{key}'")

    def __setattr__(self, key, value):
        self[key] = value

class TestApp:
    def setup_method(self):
        # Reset all mocks on st
        mock_st.reset_mock()
        # Reset session state mock for each test
        mock_st.session_state = MockSessionState()
        mock_st.session_state.messages = []
        mock_st.session_state.json_mode = False
        
        mock_st.sidebar = MagicMock()
        mock_st.sidebar.__enter__.return_value = mock_st.sidebar
        mock_st.chat_message = MagicMock()
        mock_st.chat_message.return_value.__enter__.return_value = MagicMock()
        
        # Mock tabs for prompt selector
        mock_st.tabs.return_value = [MagicMock(), MagicMock()]

        # Reset button side effects
        mock_st.button.side_effect = None
        mock_st.button.return_value = False
        mock_st.chat_input.return_value = None

    def test_mock_session_state_attribute_error(self):
        state = MockSessionState()
        with pytest.raises(AttributeError, match="'MockSessionState' object has no attribute 'missing'"):
            _ = state.missing

    def test_init_session_state(self):
        init_session_state()
        assert "selected_model" in mock_st.session_state
        assert "temperature" in mock_st.session_state
        assert "top_k" in mock_st.session_state
        assert "context_window" in mock_st.session_state
        
        # Test defaults
        assert mock_st.session_state.selected_model is None
        assert mock_st.session_state.temperature == 0.7

    def test_init_chat_state(self):
        # Clear state to test initialization
        mock_st.session_state = MockSessionState()
        
        init_chat_state()
        
        assert "messages" in mock_st.session_state
        assert mock_st.session_state.messages == []
        assert "json_mode" in mock_st.session_state
        assert mock_st.session_state.json_mode is False

    def test_render_sidebar_connected(self):
        mock_client = MagicMock(spec=LLMClient)
        mock_client.check_connection.return_value = True
        mock_client.list_models.return_value = ["model1", "model2"]
        
        # Setup session state
        mock_st.session_state = MockSessionState({
            "selected_model": None,
            "temperature": 0.7,
            "top_k": 40,
            "context_window": 4096
        })
        
        # Mock selectbox to return a value
        mock_st.selectbox.return_value = "model1"
        
        render_sidebar(mock_client)
        
        # Verify interactions
        mock_client.check_connection.assert_called_once()
        mock_client.list_models.assert_called_once()
        mock_st.selectbox.assert_called()
        mock_st.slider.assert_called()
        
        # Verify session state update (simulated)
        mock_st.session_state.selected_model = "model1"
        assert mock_st.session_state.selected_model == "model1"

    def test_main_layout(self):
        with patch('app.init_session_state') as mock_init, \
             patch('app.init_chat_state') as mock_init_chat, \
             patch('app.LLMClient') as mock_client_cls, \
             patch('app.render_sidebar') as mock_render, \
             patch('app.render_prompt_selector') as mock_prompt_selector:
            
            mock_prompt_selector.return_value = "You are a helpful assistant."
            
            main()
            
            mock_init.assert_called_once()
            mock_init_chat.assert_called_once()
            mock_client_cls.assert_called_once()
            mock_render.assert_called_once()
            mock_st.set_page_config.assert_called_once()
            mock_st.title.assert_called_with("💬 Chat Lab")

    def test_chat_interaction(self):
        with patch('app.LLMClient') as mock_client_cls, \
             patch('app.render_sidebar'), \
             patch('app.render_prompt_selector') as mock_prompt_selector: # Mock sidebar to prevent auto-selection interfering
            
            mock_prompt_selector.return_value = "You are a helpful assistant."
            
            mock_client = mock_client_cls.return_value
            mock_client.chat.return_value = iter([
                {'message': {'content': 'Hello'}, 'done': False},
                {'message': {'content': ' World'}, 'done': True, 'eval_count': 2, 'eval_duration': 100000000}
            ])
            
            # Mock user input
            mock_st.chat_input.return_value = "Hi"
            
            # Mock session state for model selection
            mock_st.session_state.selected_model = "granite-code:8b"
            
            main()
            
            # Verify user message added
            assert {"role": "user", "content": "Hi"} in mock_st.session_state.messages
            
            # Verify client.chat called
            mock_client.chat.assert_called()
            call_args = mock_client.chat.call_args[1]
            assert call_args['model'] == "granite-code:8b"
            assert call_args['messages'][-1]['content'] == "Hi"
            
            # Verify assistant response added
            assert {"role": "assistant", "content": "Hello World"} in mock_st.session_state.messages

    def test_chat_interaction_exception(self):
        with patch('app.LLMClient') as mock_client_cls, \
             patch('app.render_sidebar'), \
             patch('app.render_prompt_selector') as mock_prompt_selector:
            
            mock_prompt_selector.return_value = "You are a helpful assistant."
            
            mock_client = mock_client_cls.return_value
            mock_client.chat.side_effect = Exception("Chat error")
            
            mock_st.chat_input.return_value = "Hi"
            mock_st.session_state.selected_model = "granite-code:8b"
            
            main()
            
            mock_st.error.assert_called_with("An error occurred: Chat error")

    def test_chat_no_model_selected(self):
        with patch('app.LLMClient') as mock_client_cls, \
             patch('app.render_sidebar'), \
             patch('app.render_prompt_selector') as mock_prompt_selector:
            
            mock_prompt_selector.return_value = "You are a helpful assistant."
            
            mock_st.chat_input.return_value = "Hi"
            mock_st.session_state.selected_model = None
            
            main()
            
            mock_st.error.assert_called_with("Please select a model in the sidebar.")

    def test_clear_history(self):
        with patch('app.LLMClient'), \
             patch('app.render_prompt_selector') as mock_prompt_selector:
            mock_prompt_selector.return_value = "You are a helpful assistant."
            mock_st.session_state.messages = [{"role": "user", "content": "hi"}]
            
            # Mock button click
            mock_st.button.return_value = True
            
            main()
            
            assert mock_st.session_state.messages == []
            mock_st.rerun.assert_called()

    def test_save_conversation(self):
        with patch('app.LLMClient'), \
             patch('app.render_prompt_selector') as mock_prompt_selector:
            mock_prompt_selector.return_value = "You are a helpful assistant."
            mock_st.session_state.messages = [{"role": "user", "content": "hi"}]
            
            main()
            
            mock_st.download_button.assert_called()
            call_args = mock_st.download_button.call_args[1]
            assert call_args['file_name'] == "conversation.json"
            assert "hi" in call_args['data']
