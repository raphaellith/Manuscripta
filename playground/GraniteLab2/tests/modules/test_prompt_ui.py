from unittest.mock import MagicMock, patch
import sys
import pytest
from modules import prompt_ui

def test_render_prompt_selector_saved():
    with patch("modules.prompt_ui.st") as mock_st, \
         patch("modules.prompt_ui.DBManager") as MockDBManager:
        
        mock_db = MockDBManager.return_value
        mock_db.get_all_system_prompts.return_value = [
            {"alias": "Test Persona", "content": "You are a test."}
        ]
        
        # Mock session state
        mock_st.session_state = {}
        
        # Mock UI interactions
        mock_st.radio.return_value = "Saved"
        mock_st.selectbox.return_value = "Test Persona"
        mock_st.tabs.return_value = [MagicMock(), MagicMock()]
        
        # Execute
        result = prompt_ui.render_prompt_selector(key="test")
        
        # Assert
        assert result == "You are a test."
        mock_st.selectbox.assert_called()

def test_render_prompt_selector_custom():
    with patch("modules.prompt_ui.st") as mock_st, \
         patch("modules.prompt_ui.DBManager") as MockDBManager:
        
        mock_db = MockDBManager.return_value
        mock_db.get_all_system_prompts.return_value = []
        
        mock_st.session_state = {"test_custom": "Custom prompt content"}
        
        # Mock UI interactions
        mock_st.radio.return_value = "Custom"
        mock_st.tabs.return_value = [MagicMock(), MagicMock()]
        
        # Execute
        result = prompt_ui.render_prompt_selector(key="test")
        
        # Assert
        assert result == "Custom prompt content"

