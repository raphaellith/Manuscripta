import pytest
from unittest.mock import MagicMock, patch
import sys
import os
import importlib.util

# Helper to import the module
def import_vision_lab():
    # Adjust path to point to pages/04_Vision_Lab.py
    file_path = os.path.abspath(os.path.join(os.path.dirname(__file__), '../../pages/04_Vision_Lab.py'))
    spec = importlib.util.spec_from_file_location("vision_lab", file_path)
    module = importlib.util.module_from_spec(spec)
    sys.modules["vision_lab"] = module
    spec.loader.exec_module(module)
    return module

# Helper class for session state
class MockSessionState(dict):
    def __init__(self, *args, **kwargs):
        super().__init__(*args, **kwargs)
        self.__dict__ = self

def test_vision_lab_render_no_upload():
    vision_lab = import_vision_lab()
    
    with patch.object(vision_lab, 'st') as mock_st, \
         patch.object(vision_lab, 'LLMClient') as mock_client_cls, \
         patch.object(vision_lab, 'render_sidebar') as mock_render_sidebar, \
         patch.object(vision_lab, 'init_session_state') as mock_init_session_state:
        
        # Mock session state
        mock_st.session_state = MockSessionState({
            'selected_model': 'granite-vision',
            'temperature': 0.7,
            'top_k': 40,
            'context_window': 4096,
            'vision_prompt': ''
        })
        
        # Mock file uploader to return None
        mock_st.file_uploader.return_value = None
        
        # Mock columns
        mock_col1 = MagicMock()
        mock_col2 = MagicMock()
        mock_st.columns.return_value = [mock_col1, mock_col2]
        
        # Context managers for columns
        mock_col1.__enter__.return_value = mock_col1
        mock_col1.__exit__.return_value = None
        mock_col2.__enter__.return_value = mock_col2
        mock_col2.__exit__.return_value = None
        
        vision_lab.main()
        
        mock_st.set_page_config.assert_called_once()
        mock_st.title.assert_called_once_with("👁️ Vision Lab")
        mock_st.info.assert_called_once_with("Please upload an image to start.")

def test_vision_lab_with_image_and_generate():
    vision_lab = import_vision_lab()
    
    with patch.object(vision_lab, 'st') as mock_st, \
         patch.object(vision_lab, 'LLMClient') as mock_client_cls, \
         patch.object(vision_lab, 'render_sidebar') as mock_render_sidebar, \
         patch.object(vision_lab, 'init_session_state') as mock_init_session_state, \
         patch.object(vision_lab, 'Image') as mock_image_cls, \
         patch.object(vision_lab, 'io') as mock_io:
        
        # Mock session state
        mock_st.session_state = MockSessionState({
            'selected_model': 'granite-vision', 
            'temperature': 0.7, 
            'top_k': 40, 
            'context_window': 4096,
            'vision_prompt': ''
        })
        
        # Mock file uploader
        mock_file = MagicMock()
        mock_st.file_uploader.return_value = mock_file
        
        # Mock Image.open
        mock_image = MagicMock()
        mock_image.format = 'PNG'
        mock_image_cls.open.return_value = mock_image
        
        # Mock io.BytesIO
        mock_bytes_io = MagicMock()
        mock_bytes_io.getvalue.return_value = b'fake_image_bytes'
        mock_io.BytesIO.return_value = mock_bytes_io
        
        # Mock columns
        mock_col1 = MagicMock()
        mock_col2 = MagicMock()
        mock_c1 = MagicMock()
        mock_c2 = MagicMock()
        mock_c3 = MagicMock()
        
        # st.columns is called twice.
        # 1. col1, col2 = st.columns([1, 1])
        # 2. c1, c2, c3 = st.columns(3)
        mock_st.columns.side_effect = [[mock_col1, mock_col2], [mock_c1, mock_c2, mock_c3]]
        
        # Context managers for columns
        mock_col1.__enter__.return_value = mock_col1
        mock_col1.__exit__.return_value = None
        mock_col2.__enter__.return_value = mock_col2
        mock_col2.__exit__.return_value = None
        
        # Mock button clicks
        # We want "Generate" to be True, others False
        # st.button is called multiple times.
        # 1. Transcribe (in col2) -> c1.button
        # 2. Describe (in col2) -> c2.button
        # 3. Extract (in col2) -> c3.button
        # 4. Generate (in col2) -> st.button
        
        mock_c1.button.return_value = False
        mock_c2.button.return_value = False
        mock_c3.button.return_value = False
        mock_st.button.return_value = True
        
        # Mock text_area
        mock_st.text_area.return_value = "Describe this image"
        
        # Mock LLMClient instance
        mock_client_instance = mock_client_cls.return_value
        mock_client_instance.generate.return_value = [{'response': 'A description'}]
        
        vision_lab.main()
        
        # Verify generate was called
        mock_client_instance.generate.assert_called_once()
        args, kwargs = mock_client_instance.generate.call_args
        assert kwargs['model'] == 'granite-vision'
        assert kwargs['prompt'] == "Describe this image"
        assert kwargs['images'] == [b'fake_image_bytes']

def test_vision_lab_quick_prompts():
    vision_lab = import_vision_lab()
    
    with patch.object(vision_lab, 'st') as mock_st, \
         patch.object(vision_lab, 'LLMClient') as mock_client_cls, \
         patch.object(vision_lab, 'render_sidebar') as mock_render_sidebar, \
         patch.object(vision_lab, 'init_session_state') as mock_init_session_state, \
         patch.object(vision_lab, 'Image') as mock_image_cls, \
         patch.object(vision_lab, 'io') as mock_io:
        
        # Mock session state
        mock_st.session_state = MockSessionState({
            'selected_model': 'granite-vision', 
            'vision_prompt': ''
        })
        
        # Mock file uploader
        mock_st.file_uploader.return_value = MagicMock()
        
        # Mock Image.open
        mock_image_cls.open.return_value = MagicMock()
        
        # Mock columns
        mock_col1 = MagicMock()
        mock_col2 = MagicMock()
        mock_c1 = MagicMock()
        mock_c2 = MagicMock()
        mock_c3 = MagicMock()
        
        mock_st.columns.side_effect = [[mock_col1, mock_col2], [mock_c1, mock_c2, mock_c3]]
        
        # Context managers for columns
        mock_col1.__enter__.return_value = mock_col1
        mock_col1.__exit__.return_value = None
        mock_col2.__enter__.return_value = mock_col2
        mock_col2.__exit__.return_value = None
        
        # Mock button clicks on columns
        mock_c1.button.return_value = True # Transcribe
        mock_c2.button.return_value = False
        mock_c3.button.return_value = False
        
        mock_st.button.return_value = False # Generate button
        
        vision_lab.main()
        
        assert mock_st.session_state.vision_prompt == "Transcribe the handwriting in this image exactly as it appears."
        mock_st.rerun.assert_called_once()

def test_vision_lab_validation_and_error():
    vision_lab = import_vision_lab()
    
    with patch.object(vision_lab, 'st') as mock_st, \
         patch.object(vision_lab, 'LLMClient') as mock_client_cls, \
         patch.object(vision_lab, 'render_sidebar') as mock_render_sidebar, \
         patch.object(vision_lab, 'init_session_state') as mock_init_session_state, \
         patch.object(vision_lab, 'Image') as mock_image_cls, \
         patch.object(vision_lab, 'io') as mock_io:
        
        # Mock file uploader
        mock_st.file_uploader.return_value = MagicMock()
        mock_image_cls.open.return_value = MagicMock()
        
        # Mock columns
        mock_col1 = MagicMock()
        mock_col2 = MagicMock()
        
        def columns_side_effect(spec):
            if spec == [1, 1]:
                return [mock_col1, mock_col2]
            elif spec == 3:
                return [MagicMock(), MagicMock(), MagicMock()]
            return [MagicMock()] * spec
            
        mock_st.columns.side_effect = columns_side_effect
        
        mock_col1.__enter__.return_value = mock_col1
        mock_col1.__exit__.return_value = None
        mock_col2.__enter__.return_value = mock_col2
        mock_col2.__exit__.return_value = None
        
        # Case 1: No model selected
        mock_st.session_state = MockSessionState({
            'selected_model': None, 
            'vision_prompt': 'prompt'
        })
        mock_st.button.return_value = True # Generate
        mock_st.text_area.return_value = "prompt"
        
        vision_lab.main()
        mock_st.error.assert_called_with("Please select a model from the sidebar.")
        
        # Case 2: No prompt
        mock_st.session_state = MockSessionState({
            'selected_model': 'model', 
            'vision_prompt': ''
        })
        mock_st.text_area.return_value = ""
        
        vision_lab.main()
        mock_st.error.assert_called_with("Please enter a prompt.")
        
        # Case 3: Exception during generation
        mock_st.session_state = MockSessionState({
            'selected_model': 'model', 
            'temperature': 0.7, 
            'top_k': 40, 
            'context_window': 4096,
            'vision_prompt': 'prompt'
        })
        mock_st.text_area.return_value = "prompt"
        
        mock_client_instance = mock_client_cls.return_value
        mock_client_instance.generate.side_effect = Exception("Test Error")
        
        vision_lab.main()
        mock_st.error.assert_called_with("An error occurred: Test Error")

def test_vision_lab_as_script():
    import runpy
    
    # Add project root to sys.path so modules can be imported
    project_root = os.path.abspath(os.path.join(os.path.dirname(__file__), '../../'))
    if project_root not in sys.path:
        sys.path.append(project_root)
        
    file_path = os.path.join(project_root, 'pages/04_Vision_Lab.py')
    
    # We need to patch the imports in the script
    # Since we can't easily patch 'modules.llm_client' if it's already imported,
    # we rely on sys.modules mocking or patching where it's used.
    
    # Mock streamlit in sys.modules if not already
    if "streamlit" not in sys.modules or not isinstance(sys.modules["streamlit"], MagicMock):
        mock_st = MagicMock()
        sys.modules["streamlit"] = mock_st
    else:
        mock_st = sys.modules["streamlit"]
        mock_st.reset_mock()
        
    mock_st.session_state = MockSessionState({
        'selected_model': 'granite-vision',
        'temperature': 0.7,
        'top_k': 40,
        'context_window': 4096,
        'vision_prompt': ''
    })
    
    # We also need to mock modules.llm_client.LLMClient
    with patch('modules.llm_client.LLMClient') as mock_client_cls, \
         patch('modules.ui_utils.render_sidebar'), \
         patch('modules.ui_utils.init_session_state'), \
         patch('PIL.Image.open') as mock_image_open:
         
         # Mock columns
         mock_col = MagicMock()
         mock_col.__enter__.return_value = mock_col
         mock_col.__exit__.return_value = None
         
         def columns_side_effect(spec):
            if spec == [1, 1]:
                return [mock_col, mock_col]
            elif spec == 3:
                return [mock_col, mock_col, mock_col]
            return [mock_col] * (spec if isinstance(spec, int) else len(spec))
            
         mock_st.columns.side_effect = columns_side_effect
         
         runpy.run_path(file_path, run_name='__main__')
         
         mock_st.title.assert_called_with("👁️ Vision Lab")
