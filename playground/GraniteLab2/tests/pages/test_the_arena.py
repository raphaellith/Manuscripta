import unittest
from unittest.mock import MagicMock, patch
import sys
import os

# Add the project root to the path so we can import modules
sys.path.append(os.path.abspath(os.path.join(os.path.dirname(__file__), '../../')))

class TestTheArena(unittest.TestCase):

    def setUp(self):
        self.mock_st = MagicMock()
        self.mock_llm_client = MagicMock()
        
        # Patch streamlit
        self.st_patcher = patch.dict('sys.modules', {'streamlit': self.mock_st})
        self.st_patcher.start()
        
        # Patch LLMClient
        self.llm_patcher = patch('modules.llm_client.LLMClient', return_value=self.mock_llm_client)
        self.mock_llm_class = self.llm_patcher.start()

    def tearDown(self):
        self.st_patcher.stop()
        self.llm_patcher.stop()

    def test_import_and_run(self):
        # Because the file is named "02_The_Arena.py", we can't import it with standard syntax easily.
        # We will use importlib to load it.
        import importlib.util
        
        file_path = os.path.abspath(os.path.join(os.path.dirname(__file__), '../../pages/02_The_Arena.py'))
        spec = importlib.util.spec_from_file_location("pages.02_The_Arena", file_path)
        the_arena = importlib.util.module_from_spec(spec)
        
        # We need to make sure 'modules' is available to the imported module
        with patch.dict('sys.modules', {'streamlit': self.mock_st}):
             spec.loader.exec_module(the_arena)
             
             # Test: No connection
             self.mock_llm_client.check_connection.return_value = False
             the_arena.main()
             self.mock_st.error.assert_called_with("🔴 Ollama not connected. Is it running?")

             # Test: No models
             self.mock_llm_client.check_connection.return_value = True
             self.mock_llm_client.list_models.return_value = []
             the_arena.main()
             self.mock_st.warning.assert_called_with("No models found. Please pull a model using `ollama pull <model>`.")

             # Test: Happy path
             self.mock_llm_client.list_models.return_value = ['model1', 'model2']
             self.mock_st.columns.return_value = [MagicMock(), MagicMock()]
             self.mock_st.chat_input.return_value = "Test Prompt"
             
             # Mock chat stream
             mock_stream = [{'message': {'content': 'chunk1'}}, {'message': {'content': 'chunk2'}}]
             self.mock_llm_client.chat.return_value = mock_stream
             
             the_arena.main()
             
             # Verify chat was called twice (once for each model)
             self.assertEqual(self.mock_llm_client.chat.call_count, 2)
             self.mock_st.code.assert_called_with("Test Prompt", language="text")

             # Test: Exception handling
             self.mock_llm_client.chat.side_effect = Exception("Test Error")
             the_arena.main()
             # We expect error to be called for both models
             # Since we are reusing the mock, call_count will increase
             # We can check if st.empty().error was called.
             # But st.empty() returns a mock, so we need to check that mock.
             
             # Let's reset mocks to be cleaner
             self.mock_st.reset_mock()
             self.mock_llm_client.reset_mock()
             self.mock_llm_client.check_connection.return_value = True
             self.mock_llm_client.list_models.return_value = ['model1', 'model2']
             self.mock_st.columns.return_value = [MagicMock(), MagicMock()]
             self.mock_st.chat_input.return_value = "Test Prompt"
             self.mock_llm_client.chat.side_effect = Exception("Test Error")
             
             # We need to mock the containers and empty placeholders to verify error calls
             mock_container = MagicMock()
             self.mock_st.container.return_value = mock_container
             mock_empty = MagicMock()
             self.mock_st.empty.return_value = mock_empty
             
             the_arena.main()
             
             # Verify error was called
             # Since we have two models, and both fail, we expect error to be called twice on the empty placeholder
             # or on the status placeholder.
             # In the code: status_a = st.empty(); status_a.error(...)
             self.assertTrue(mock_empty.error.called)
             self.assertEqual(mock_empty.error.call_count, 2)

if __name__ == '__main__':
    unittest.main()
