import pytest
from unittest.mock import patch, MagicMock
import sys
import os
import subprocess
from run import install_dependencies, check_ollama, run_streamlit, main

class TestRun:
    def test_install_dependencies_success(self):
        with patch('subprocess.check_call') as mock_call:
            install_dependencies()
            mock_call.assert_called_once()

    def test_install_dependencies_failure(self):
        with patch('subprocess.check_call') as mock_call:
            mock_call.side_effect = subprocess.CalledProcessError(1, 'cmd')
            with pytest.raises(SystemExit):
                install_dependencies()

    def test_check_ollama_success(self):
        with patch.dict(sys.modules, {'ollama': MagicMock()}):
            sys.modules['ollama'].list.return_value = {}
            assert check_ollama() is True

    def test_check_ollama_failure(self):
        with patch.dict(sys.modules, {'ollama': MagicMock()}):
            sys.modules['ollama'].list.side_effect = Exception("Connection refused")
            assert check_ollama() is False

    def test_check_ollama_import_error_simulated(self):
        # Remove ollama from sys.modules if present
        if 'ollama' in sys.modules:
            del sys.modules['ollama']
            
        original_import = __import__
        def mock_import(name, *args, **kwargs):
            if name == 'ollama':
                raise ImportError("No module named 'ollama'")
            return original_import(name, *args, **kwargs)
        
        with patch('builtins.__import__', side_effect=mock_import):
            assert check_ollama() is False

    def test_run_streamlit(self):
        with patch('subprocess.run') as mock_run:
            run_streamlit()
            mock_run.assert_called_once()
            args = mock_run.call_args[0][0]
            assert "streamlit" in args
            assert "run" in args

    def test_run_streamlit_keyboard_interrupt(self):
        with patch('subprocess.run') as mock_run:
            mock_run.side_effect = KeyboardInterrupt
            run_streamlit() # Should not raise exception

    def test_main_flow(self):
        with (
            patch('os.path.exists') as mock_exists,
            patch('run.install_dependencies') as mock_install,
            patch('run.check_ollama') as mock_check,
            patch('run.run_streamlit') as mock_run
        ):
            mock_exists.return_value = True
            mock_check.return_value = True
            
            main()
            
            mock_install.assert_called_once()
            mock_check.assert_called_once()
            mock_run.assert_called_once()

    def test_main_ollama_check_fails(self):
        with (
            patch('os.path.exists') as mock_exists,
            patch('run.install_dependencies') as mock_install,
            patch('run.check_ollama') as mock_check,
            patch('run.run_streamlit') as mock_run
        ):
            mock_exists.return_value = True
            mock_check.return_value = False # Fail check
            
            main()
            
            mock_install.assert_called_once()
            mock_check.assert_called_once()
            mock_run.assert_called_once()

    def test_main_no_requirements(self):
        with patch('os.path.exists') as mock_exists:
            mock_exists.return_value = False
            with pytest.raises(SystemExit):
                main()

    def test_run_as_script(self):
        # Test running the file as a script
        # We need to mock subprocess to prevent actual execution
        with patch('subprocess.run') as mock_run, \
             patch('subprocess.check_call') as mock_check_call, \
             patch('sys.exit') as mock_exit, \
             patch('os.path.exists') as mock_exists:
            
            mock_exists.return_value = True
            
            # Mock ollama to avoid network calls or import errors
            with patch.dict(sys.modules, {'ollama': MagicMock()}):
                import runpy
                file_path = os.path.join(os.path.dirname(__file__), '..', 'run.py')
                runpy.run_path(file_path, run_name='__main__')
            
            # Verify main logic ran
            # install_dependencies calls check_call
            mock_check_call.assert_called()
            # run_streamlit calls run
            mock_run.assert_called()

