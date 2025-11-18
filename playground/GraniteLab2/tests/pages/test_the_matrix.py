import unittest
from unittest.mock import MagicMock, patch
import sys
import os
import runpy

class TestTheMatrix(unittest.TestCase):
    def setUp(self):
        self.page_path = os.path.join(os.path.dirname(__file__), '../../pages/06_The_Matrix.py')
        
        # Patch streamlit in sys.modules
        self.mock_st = MagicMock()
        self.st_patcher = patch.dict(sys.modules, {'streamlit': self.mock_st})
        self.st_patcher.start()
        
        # Configure columns to always return 2 items to avoid ValueError
        self.mock_st.columns.return_value = [MagicMock(), MagicMock()]
        
    def tearDown(self):
        self.st_patcher.stop()
        
    def test_page_render(self):
        """Test that the page renders the basic UI elements."""
        with patch('modules.db_manager.DBManager') as MockDB, \
             patch('modules.llm_client.LLMClient') as MockLLM:
            
            # Setup mocks
            mock_db = MockDB.return_value
            mock_db.get_all_prompts.return_value = [
                {'id': 1, 'alias': 'Test Prompt', 'content': 'Hello', 'tags': 'test'}
            ]
            
            mock_llm = MockLLM.return_value
            mock_llm.list_models.return_value = ['model1', 'model2']
            
            # Run the page
            runpy.run_path(self.page_path, run_name='__main__')
            
            # Verify UI elements
            self.mock_st.set_page_config.assert_called_with(page_title="The Matrix", layout="wide")
            self.mock_st.title.assert_called_with("The Matrix: Batch Experiment Engine")
            self.mock_st.multiselect.assert_any_call("Select Models", ['model1', 'model2'])
            self.mock_st.multiselect.assert_any_call("Select Prompts", ['Test Prompt (ID: 1)'])
            self.mock_st.button.assert_called_with("Start Matrix Run", type="primary")

    def test_execution_logic(self):
        """Test the execution logic when button is clicked."""
        with patch('modules.db_manager.DBManager') as MockDB, \
             patch('modules.llm_client.LLMClient') as MockLLM:
            
            # Setup mocks
            mock_db = MockDB.return_value
            mock_db.get_all_prompts.return_value = [
                {'id': 1, 'alias': 'Test Prompt', 'content': 'Hello', 'tags': 'test'}
            ]
            mock_db.create_experiment.return_value = 123
            
            mock_llm = MockLLM.return_value
            mock_llm.list_models.return_value = ['model1']
            # Mock chat stream
            mock_chunk = {'message': {'content': 'World'}}
            mock_llm.chat.return_value = [mock_chunk]
            
            # Mock Streamlit inputs
            # st.multiselect is called twice: Models, Prompts
            self.mock_st.multiselect.side_effect = [['model1'], ['Test Prompt (ID: 1)']]
            
            # st.number_input is called once: Iterations
            self.mock_st.number_input.return_value = 1
            
            # st.text_input is called once: Experiment Name
            self.mock_st.text_input.return_value = "Test Run"
            
            # st.button is called once: Start
            self.mock_st.button.return_value = True
            
            # Run the page
            runpy.run_path(self.page_path, run_name='__main__')
            
            # Verify execution
            mock_db.create_experiment.assert_called()
            mock_llm.chat.assert_called()
            mock_db.add_result.assert_called()
            
            # Verify result was saved
            args, kwargs = mock_db.add_result.call_args
            self.assertEqual(kwargs['experiment_id'], 123)
            self.assertEqual(kwargs['model'], 'model1')
            self.assertEqual(kwargs['output'], 'World')

    def test_execution_error_handling(self):
        """Test that errors during execution are handled gracefully."""
        with patch('modules.db_manager.DBManager') as MockDB, \
             patch('modules.llm_client.LLMClient') as MockLLM:
            
            # Setup mocks
            mock_db = MockDB.return_value
            mock_db.get_all_prompts.return_value = [
                {'id': 1, 'alias': 'Test Prompt', 'content': 'Hello', 'tags': 'test'}
            ]
            mock_db.create_experiment.return_value = 123
            
            mock_llm = MockLLM.return_value
            mock_llm.list_models.return_value = ['model1']
            
            # Mock chat to raise exception
            mock_llm.chat.side_effect = Exception("API Error")
            
            # Mock Streamlit inputs
            self.mock_st.multiselect.side_effect = [['model1'], ['Test Prompt (ID: 1)']]
            self.mock_st.number_input.return_value = 1
            self.mock_st.text_input.return_value = "Test Run"
            self.mock_st.button.return_value = True
            
            # Run the page
            runpy.run_path(self.page_path, run_name='__main__')
            
            # Verify error was logged (we log to DB with error message)
            mock_db.add_result.assert_called()
            args, kwargs = mock_db.add_result.call_args
            self.assertIn("ERROR: API Error", kwargs['output'])

