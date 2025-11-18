"""
RAG Inspector - Debug retrieval quality with transparency
"""

import streamlit as st
import sys
from pathlib import Path
import tempfile
import os

# Add parent directory to path for imports
sys.path.append(str(Path(__file__).parent.parent))

from core.client import ModelEngine
from core.rag import RAGExperiment
from core.utils import format_file_size

# Page configuration
st.set_page_config(
    page_title="RAG Inspector - Granite Lab",
    page_icon="⚗",
    layout="wide"
)

st.title("RAG Inspector")
st.markdown("Debug retrieval quality with full transparency")

# Initialize session state
if 'rag_experiment' not in st.session_state:
    st.session_state.rag_experiment = None
if 'rag_engine' not in st.session_state:
    st.session_state.rag_engine = None
if 'rag_history' not in st.session_state:
    st.session_state.rag_history = []

# Sidebar controls
st.sidebar.header("Configuration")

# Check Ollama connection
if not ModelEngine.check_ollama_connection():
    st.error("WARNING: Ollama is not running. Please launch it with `ollama serve`")
    st.stop()

# Model selector
available_models = ModelEngine.list_available_models()
if not available_models:
    st.error("No models available. Pull a model with `ollama pull <model-name>`")
    st.stop()

selected_model = st.sidebar.selectbox(
    "LLM Model",
    available_models,
    index=0
)

# Embedding model
embedding_models = ["nomic-embed-text", "granite-embedding", "mxbai-embed-large"]
selected_embedding = st.sidebar.selectbox(
    "Embedding Model",
    embedding_models,
    index=0
)

# RAG parameters
chunk_size = st.sidebar.slider(
    "Chunk Size",
    min_value=200,
    max_value=2000,
    value=1000,
    step=100
)

chunk_overlap = st.sidebar.slider(
    "Chunk Overlap",
    min_value=0,
    max_value=500,
    value=200,
    step=50
)

k_retrieval = st.sidebar.slider(
    "Retrieval K",
    min_value=1,
    max_value=10,
    value=3,
    help="Number of chunks to retrieve"
)

# Initialize RAG experiment
if st.session_state.rag_experiment is None:
    with st.spinner("Initializing RAG experiment..."):
        try:
            st.session_state.rag_experiment = RAGExperiment(
                embedding_model=selected_embedding,
                chunk_size=chunk_size,
                chunk_overlap=chunk_overlap
            )
            st.sidebar.success("[OK] RAG initialized")
        except Exception as e:
            st.sidebar.error(f"Failed to initialize: {str(e)}")
            if "not found" in str(e).lower():
                st.sidebar.info(f"TIP: Try pulling the embedding model: `ollama pull {selected_embedding}`")
            st.stop()

# Initialize LLM engine
if st.session_state.rag_engine is None or st.session_state.rag_engine.model_name != selected_model:
    st.session_state.rag_engine = ModelEngine(model_name=selected_model)

# Document count
doc_count = st.session_state.rag_experiment.get_document_count()
st.sidebar.info(f"**{doc_count}** document chunks loaded")

# Clear button
if st.sidebar.button("Clear All", use_container_width=True):
    st.session_state.rag_experiment.clear()
    st.session_state.rag_history = []
    st.rerun()

st.sidebar.divider()

# Main interface
col1, col2 = st.columns([1, 1])

with col1:
    st.header("Document Ingestion")
    
    # File upload
    uploaded_file = st.file_uploader(
        "Upload PDF or TXT",
        type=["pdf", "txt"],
        help="Upload a document to ingest into the vector store"
    )
    
    if uploaded_file:
        file_details = f"**{uploaded_file.name}** ({format_file_size(uploaded_file.size)})"
        st.info(f"File: {file_details}")
        
        if st.button("Ingest Document", use_container_width=True):
            with st.spinner("Processing document..."):
                try:
                    # Save to temp file
                    with tempfile.NamedTemporaryFile(delete=False, suffix=Path(uploaded_file.name).suffix) as tmp_file:
                        tmp_file.write(uploaded_file.getvalue())
                        tmp_path = tmp_file.name
                    
                    # Ingest based on file type
                    if uploaded_file.name.endswith('.pdf'):
                        chunks = st.session_state.rag_experiment.ingest_pdf(tmp_path)
                    else:
                        text = uploaded_file.getvalue().decode('utf-8')
                        chunks = st.session_state.rag_experiment.ingest_text(text, uploaded_file.name)
                    
                    # Clean up temp file
                    os.unlink(tmp_path)
                    
                    st.success(f"[OK] Ingested {chunks} chunks")
                    st.rerun()
                    
                except Exception as e:
                    st.error(f"Error ingesting document: {str(e)}")
    
    # Text input option
    st.divider()
    st.subheader("Or paste text directly:")
    
    text_input = st.text_area(
        "Text Content",
        height=200,
        placeholder="Paste your text here..."
    )
    
    if st.button("Ingest Text", use_container_width=True, disabled=not text_input):
        with st.spinner("Processing text..."):
            try:
                chunks = st.session_state.rag_experiment.ingest_text(text_input)
                st.success(f"[OK] Ingested {chunks} chunks")
                st.rerun()
            except Exception as e:
                st.error(f"Error ingesting text: {str(e)}")

with col2:
    st.header("Query & Retrieval")
    
    if doc_count == 0:
        st.warning("WARNING: No documents ingested yet. Upload a document to get started.")
    else:
        query = st.text_input(
            "Ask a question:",
            placeholder="What is this document about?"
        )
        
        if st.button("Search", use_container_width=True, disabled=not query):
            with st.spinner("Retrieving and generating answer..."):
                try:
                    answer, retrieved_docs = st.session_state.rag_experiment.generate_answer(
                        query=query,
                        model_engine=st.session_state.rag_engine,
                        k=k_retrieval
                    )
                    
                    # Store in history
                    st.session_state.rag_history.append({
                        "query": query,
                        "answer": answer,
                        "retrieved_docs": retrieved_docs
                    })
                    
                    st.rerun()
                    
                except Exception as e:
                    st.error(f"Error: {str(e)}")

# Display history
if st.session_state.rag_history:
    st.divider()
    st.header("Query History")
    
    for idx, item in enumerate(reversed(st.session_state.rag_history)):
        with st.container():
            st.subheader(f"Query {len(st.session_state.rag_history) - idx}")
            
            st.markdown(f"**Question:** {item['query']}")
            
            st.markdown("**Answer:**")
            st.info(item['answer'])
            
            st.markdown("**Retrieved Context:**")
            
            for i, (doc, score) in enumerate(item['retrieved_docs']):
                with st.expander(f"Chunk {i+1} (Score: {score:.4f}):"):
                    st.text(doc.page_content)
                    if doc.metadata:
                        st.caption(f"Source: {doc.metadata.get('source', 'Unknown')}")
            
            st.divider()

# Footer
st.caption(f"Using: **{selected_model}** (LLM) | **{selected_embedding}** (Embeddings)")
