import os
import tempfile
from typing import List, Optional
from langchain_community.document_loaders import PyPDFLoader, TextLoader
from langchain_text_splitters import RecursiveCharacterTextSplitter
from langchain_chroma import Chroma
from langchain_ollama import OllamaEmbeddings
from langchain_core.documents import Document

class RAGEngine:
    def __init__(self, embedding_model: str = "nomic-embed-text"):
        self.embeddings = OllamaEmbeddings(model=embedding_model)
        self.vector_store: Optional[Chroma] = None

    def set_embedding_model(self, model_name: str):
        self.embeddings = OllamaEmbeddings(model=model_name)

    def ingest_file(self, file_obj, chunk_size: int = 1000, chunk_overlap: int = 200) -> List[Document]:
        """
        Ingests a file (PDF or TXT) from a Streamlit UploadedFile object.
        """
        # Create a temporary file to save the uploaded content
        suffix = ".pdf" if file_obj.name.endswith(".pdf") else ".txt"
        with tempfile.NamedTemporaryFile(delete=False, suffix=suffix) as tmp_file:
            tmp_file.write(file_obj.read())
            tmp_file_path = tmp_file.name

        try:
            if suffix == ".pdf":
                loader = PyPDFLoader(tmp_file_path)
            else:
                loader = TextLoader(tmp_file_path)
            
            documents = loader.load()
            
            text_splitter = RecursiveCharacterTextSplitter(
                chunk_size=chunk_size,
                chunk_overlap=chunk_overlap
            )
            splits = text_splitter.split_documents(documents)
            
            # Create a new vector store for this session (ephemeral for the lab)
            self.vector_store = Chroma.from_documents(
                documents=splits,
                embedding=self.embeddings
            )
            
            return splits
        finally:
            if os.path.exists(tmp_file_path):
                os.remove(tmp_file_path)

    def retrieve(self, query: str, k: int = 4) -> List[Document]:
        if not self.vector_store:
            return []
        
        return self.vector_store.similarity_search(query, k=k)

    def clear(self):
        if self.vector_store:
            self.vector_store.delete_collection()
            self.vector_store = None
