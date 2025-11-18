"""
RAGExperiment: Handles document ingestion and retrieval with transparency.
"""

from typing import List, Tuple, Optional
from langchain_chroma import Chroma
from langchain_ollama import OllamaEmbeddings
from langchain.text_splitter import RecursiveCharacterTextSplitter
from langchain_core.documents import Document
from chromadb import EphemeralClient
import pypdf


class RAGExperiment:
    """
    Manages document ingestion and retrieval with full transparency.
    
    Uses:
    - ChromaDB in EphemeralClient mode (in-memory, no persistent data)
    - RecursiveCharacterTextSplitter for chunking
    - Ollama embeddings (granite-embedding or nomic-embed-text)
    """
    
    def __init__(
        self,
        embedding_model: str = "nomic-embed-text",
        chunk_size: int = 1000,
        chunk_overlap: int = 200,
        base_url: str = "http://localhost:11434"
    ):
        """
        Initialize the RAG experiment.
        
        Args:
            embedding_model: Name of the embedding model
            chunk_size: Size of text chunks
            chunk_overlap: Overlap between chunks
            base_url: Ollama server URL
        """
        self.embedding_model = embedding_model
        self.chunk_size = chunk_size
        self.chunk_overlap = chunk_overlap
        
        # Initialize embeddings
        self.embeddings = OllamaEmbeddings(
            model=embedding_model,
            base_url=base_url
        )
        
        # Initialize text splitter
        self.text_splitter = RecursiveCharacterTextSplitter(
            chunk_size=chunk_size,
            chunk_overlap=chunk_overlap,
            length_function=len,
        )
        
        # Initialize vector store with ephemeral client
        self.vector_store: Optional[Chroma] = None
        self.documents: List[Document] = []
    
    def ingest_pdf(self, file_path: str) -> int:
        """
        Ingest a PDF file.
        
        Args:
            file_path: Path to the PDF file
            
        Returns:
            Number of chunks created
        """
        # Read PDF
        pdf_reader = pypdf.PdfReader(file_path)
        
        # Extract text from all pages
        text = ""
        for page in pdf_reader.pages:
            text += page.extract_text() + "\n"
        
        # Create document
        doc = Document(page_content=text, metadata={"source": file_path})
        
        # Split into chunks
        chunks = self.text_splitter.split_documents([doc])
        self.documents.extend(chunks)
        
        # Create or update vector store
        if self.vector_store is None:
            self.vector_store = Chroma.from_documents(
                documents=chunks,
                embedding=self.embeddings,
                client=EphemeralClient()
            )
        else:
            self.vector_store.add_documents(chunks)
        
        return len(chunks)
    
    def ingest_text(self, text: str, source: str = "text_input") -> int:
        """
        Ingest raw text.
        
        Args:
            text: Raw text content
            source: Source identifier
            
        Returns:
            Number of chunks created
        """
        # Create document
        doc = Document(page_content=text, metadata={"source": source})
        
        # Split into chunks
        chunks = self.text_splitter.split_documents([doc])
        self.documents.extend(chunks)
        
        # Create or update vector store
        if self.vector_store is None:
            self.vector_store = Chroma.from_documents(
                documents=chunks,
                embedding=self.embeddings,
                client=EphemeralClient()
            )
        else:
            self.vector_store.add_documents(chunks)
        
        return len(chunks)
    
    def retrieve_with_scores(
        self,
        query: str,
        k: int = 3
    ) -> List[Tuple[Document, float]]:
        """
        Retrieve documents with similarity scores.
        
        Args:
            query: Query text
            k: Number of documents to retrieve
            
        Returns:
            List of (document, score) tuples
        """
        if self.vector_store is None:
            return []
        
        # Retrieve with scores
        results = self.vector_store.similarity_search_with_score(query, k=k)
        
        return results
    
    def generate_answer(
        self,
        query: str,
        model_engine,
        k: int = 3
    ) -> Tuple[str, List[Tuple[Document, float]]]:
        """
        Generate an answer using RAG.
        
        Args:
            query: User query
            model_engine: ModelEngine instance
            k: Number of documents to retrieve
            
        Returns:
            Tuple of (answer, retrieved_documents_with_scores)
        """
        # Retrieve relevant documents
        retrieved_docs = self.retrieve_with_scores(query, k=k)
        
        if not retrieved_docs:
            return "No documents have been ingested yet.", []
        
        # Build context from retrieved documents
        context = "\n\n".join([
            f"[Document {i+1}]:\n{doc.page_content}"
            for i, (doc, _score) in enumerate(retrieved_docs)
        ])
        
        # Build prompt
        prompt = f"""Based on the following context, answer the question.

Context:
{context}

Question: {query}

Answer:"""
        
        # Generate answer
        answer = model_engine.invoke(prompt)
        
        return answer, retrieved_docs
    
    def clear(self):
        """Clear all documents and reset the vector store."""
        self.documents = []
        self.vector_store = None
    
    def get_document_count(self) -> int:
        """Get the number of ingested document chunks."""
        return len(self.documents)
