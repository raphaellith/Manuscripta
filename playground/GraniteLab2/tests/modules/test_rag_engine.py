import pytest
from unittest.mock import MagicMock, patch
import sys
import os

# Add the parent directory to sys.path to import modules
sys.path.append(os.path.abspath(os.path.join(os.path.dirname(__file__), '../../')))

from modules.rag_engine import RAGEngine
from langchain_core.documents import Document

@pytest.fixture
def mock_embeddings():
    with patch('modules.rag_engine.OllamaEmbeddings') as mock:
        yield mock

@pytest.fixture
def mock_chroma():
    with patch('modules.rag_engine.Chroma') as mock:
        yield mock

@pytest.fixture
def mock_loaders():
    with patch('modules.rag_engine.PyPDFLoader') as pdf_mock, \
         patch('modules.rag_engine.TextLoader') as txt_mock:
        yield pdf_mock, txt_mock

@pytest.fixture
def mock_splitter():
    with patch('modules.rag_engine.RecursiveCharacterTextSplitter') as mock:
        yield mock

def test_rag_engine_initialization(mock_embeddings):
    engine = RAGEngine()
    mock_embeddings.assert_called_with(model="nomic-embed-text")
    assert engine.vector_store is None

def test_set_embedding_model(mock_embeddings):
    engine = RAGEngine()
    engine.set_embedding_model("llama3")
    mock_embeddings.assert_called_with(model="llama3")

def test_ingest_file_txt(mock_embeddings, mock_chroma, mock_loaders, mock_splitter):
    mock_pdf_loader, mock_txt_loader = mock_loaders
    
    # Mock file object
    mock_file = MagicMock()
    mock_file.name = "test.txt"
    mock_file.read.return_value = b"test content"
    
    # Mock loader return
    mock_loader_instance = mock_txt_loader.return_value
    mock_loader_instance.load.return_value = [Document(page_content="test content")]
    
    # Mock splitter return
    mock_splitter_instance = mock_splitter.return_value
    mock_splitter_instance.split_documents.return_value = [Document(page_content="test content")]
    
    engine = RAGEngine()
    splits = engine.ingest_file(mock_file)
    
    mock_txt_loader.assert_called()
    mock_chroma.from_documents.assert_called()
    assert len(splits) == 1
    assert engine.vector_store is not None

def test_ingest_file_pdf(mock_embeddings, mock_chroma, mock_loaders, mock_splitter):
    mock_pdf_loader, mock_txt_loader = mock_loaders
    
    # Mock file object
    mock_file = MagicMock()
    mock_file.name = "test.pdf"
    mock_file.read.return_value = b"pdf content"
    
    # Mock loader return
    mock_loader_instance = mock_pdf_loader.return_value
    mock_loader_instance.load.return_value = [Document(page_content="pdf content")]
    
    # Mock splitter return
    mock_splitter_instance = mock_splitter.return_value
    mock_splitter_instance.split_documents.return_value = [Document(page_content="pdf content")]
    
    engine = RAGEngine()
    splits = engine.ingest_file(mock_file)
    
    mock_pdf_loader.assert_called()
    mock_chroma.from_documents.assert_called()
    assert len(splits) == 1

def test_retrieve(mock_embeddings, mock_chroma):
    engine = RAGEngine()
    
    # Test retrieve without vector store
    assert engine.retrieve("query") == []
    
    # Test retrieve with vector store
    mock_store = MagicMock()
    mock_store.similarity_search.return_value = [Document(page_content="result")]
    engine.vector_store = mock_store
    
    results = engine.retrieve("query", k=2)
    mock_store.similarity_search.assert_called_with("query", k=2)
    assert len(results) == 1

def test_clear(mock_embeddings):
    engine = RAGEngine()
    mock_store = MagicMock()
    engine.vector_store = mock_store
    
    engine.clear()
    
    mock_store.delete_collection.assert_called_once()
    assert engine.vector_store is None
