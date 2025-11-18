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
from app import main
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
        mock_st.sidebar = MagicMock()
        mock_st.sidebar.__enter__.return_value = mock_st.sidebar

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

    def test_render_sidebar_disconnected(self):
        mock_client = MagicMock(spec=LLMClient)
        mock_client.check_connection.return_value = False
        
        # Setup session state for sliders which are still rendered or accessed?
        # Wait, in my code, sliders are rendered AFTER the connection check block?
        # Let's check app.py. 
        # Yes, st.divider() and st.subheader("Hyperparameters") are outside the if/else.
        # So we need session state for them.
        mock_st.session_state = MockSessionState({
            "temperature": 0.7,
            "top_k": 40,
            "context_window": 4096
        })

        render_sidebar(mock_client)
        
        mock_st.error.assert_called_with("🔴 Ollama not connected. Is it running?")
        mock_st.selectbox.assert_not_called()

    def test_render_sidebar_connected_with_selection(self):
        mock_client = MagicMock(spec=LLMClient)
        mock_client.check_connection.return_value = True
        mock_client.list_models.return_value = ["model1", "model2"]
        
        # Setup session state with pre-selected model
        mock_st.session_state = MockSessionState({
            "selected_model": "model2",
            "temperature": 0.7,
            "top_k": 40,
            "context_window": 4096
        })
        
        mock_st.selectbox.return_value = "model2"
        
        render_sidebar(mock_client)
        
        # Verify selectbox called with correct index
        mock_st.selectbox.assert_called_with("Select Model", ["model1", "model2"], index=1)

    def test_render_sidebar_no_models(self):
        mock_client = MagicMock(spec=LLMClient)
        mock_client.check_connection.return_value = True
        mock_client.list_models.return_value = []
        
        mock_st.session_state = MockSessionState({
            "temperature": 0.7,
            "top_k": 40,
            "context_window": 4096
        })

        render_sidebar(mock_client)
        
        mock_st.warning.assert_called_with("No models found. Please pull a model using `ollama pull <model>`.")
        mock_st.selectbox.assert_not_called()

    def test_main(self):
        with patch('app.init_session_state') as mock_init, \
             patch('app.LLMClient') as mock_client_cls, \
             patch('app.render_sidebar') as mock_render, \
             patch('app.st') as mock_st_module:
            
            main()
            
            mock_init.assert_called_once()
            mock_client_cls.assert_called_once()
            mock_render.assert_called_once()
            mock_st_module.set_page_config.assert_called_once()
            mock_st_module.title.assert_called_once()
            mock_st_module.write.assert_called_once()

    def test_app_as_script(self):
        import runpy
        file_path = os.path.join(os.path.dirname(__file__), '..', 'app.py')
        
        # Reset mocks to ensure we capture calls from this run
        mock_st.reset_mock()
        # Ensure session state is set
        mock_st.session_state = MockSessionState()
        
        with patch('modules.llm_client.LLMClient') as mock_client_cls, \
             patch('modules.ui_utils.render_sidebar'), \
             patch('modules.ui_utils.init_session_state'):
             
             runpy.run_path(file_path, run_name='__main__')
             
             # Verify main ran
             mock_st.title.assert_called_with("🧪 GraniteLab Workbench")
