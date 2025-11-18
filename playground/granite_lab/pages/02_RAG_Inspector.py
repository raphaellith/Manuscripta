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
from core.prompt_manager import PromptManager

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
if 'rag_prompt_manager' not in st.session_state:
    st.session_state.rag_prompt_manager = PromptManager()
if 'rag_system_prompt' not in st.session_state:
    st.session_state.rag_system_prompt = "You are a helpful AI assistant. Answer questions based on the provided context."

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

st.sidebar.divider()

# System Prompt Management for RAG
st.sidebar.subheader("System Prompt")

# Prompt selector: default, saved prompts, or custom
rag_prompt_mode = st.sidebar.radio(
    "RAG Prompt Mode",
    ["Default", "Saved Prompts", "Custom"],
    label_visibility="collapsed",
    key="rag_prompt_mode"
)

if rag_prompt_mode == "Default":
    st.session_state.rag_system_prompt = "You are a helpful AI assistant. Answer questions based on the provided context."
    rag_system_prompt = st.session_state.rag_system_prompt
    st.sidebar.text_area(
        "Current Prompt",
        value=rag_system_prompt,
        height=80,
        disabled=True,
        key="rag_default_prompt_display"
    )

elif rag_prompt_mode == "Saved Prompts":
    saved_prompts = st.session_state.rag_prompt_manager.list_prompts()
    
    if saved_prompts:
        # Display saved prompts with descriptions
        prompt_options = ["Select a prompt..."] + [p["name"] for p in saved_prompts]
        selected_prompt_name = st.sidebar.selectbox(
            "Choose Prompt",
            prompt_options,
            key="rag_saved_prompt_selector"
        )
        
        if selected_prompt_name != "Select a prompt...":
            # Load the selected prompt
            loaded_prompt = st.session_state.rag_prompt_manager.load_prompt(selected_prompt_name)
            if loaded_prompt:
                st.session_state.rag_system_prompt = loaded_prompt
                
                # Show description if available
                prompt_meta = next((p for p in saved_prompts if p["name"] == selected_prompt_name), None)
                if prompt_meta and prompt_meta["description"]:
                    st.sidebar.caption(f"ℹ️ {prompt_meta['description']}")
                
                # Display the prompt
                st.sidebar.text_area(
                    "Current Prompt",
                    value=loaded_prompt,
                    height=120,
                    disabled=True,
                    key="rag_loaded_prompt_display"
                )
                
                # Delete button
                if st.sidebar.button("🗑️ Delete This Prompt", key="rag_delete_saved_prompt"):
                    if st.session_state.rag_prompt_manager.delete_prompt(selected_prompt_name):
                        st.sidebar.success(f"Deleted '{selected_prompt_name}'")
                        st.rerun()
                    else:
                        st.sidebar.error("Failed to delete prompt")
        else:
            st.sidebar.info("Select a saved prompt from the dropdown")
    else:
        st.sidebar.info("No saved prompts yet. Use 'Custom' mode to create one.")
    
    rag_system_prompt = st.session_state.rag_system_prompt

elif rag_prompt_mode == "Custom":
    # Custom prompt input
    rag_system_prompt = st.sidebar.text_area(
        "Custom System Prompt",
        value=st.session_state.rag_system_prompt,
        height=120,
        key="rag_custom_prompt_input"
    )
    st.session_state.rag_system_prompt = rag_system_prompt
    
    # Save prompt interface
    with st.sidebar.expander("💾 Save This Prompt"):
        prompt_name = st.text_input("Prompt Name", key="rag_new_prompt_name")
        prompt_description = st.text_input("Description (optional)", key="rag_new_prompt_desc")
        
        if st.button("Save Prompt", key="rag_save_prompt_btn"):
            if not prompt_name:
                st.error("Please enter a name")
            elif st.session_state.rag_prompt_manager.prompt_exists(prompt_name):
                st.warning(f"Prompt '{prompt_name}' already exists")
            elif not rag_system_prompt.strip():
                st.error("Prompt content cannot be empty")
            else:
                if st.session_state.rag_prompt_manager.save_prompt(
                    prompt_name, 
                    rag_system_prompt, 
                    prompt_description
                ):
                    st.success(f"✅ Saved '{prompt_name}'")
                    st.rerun()
                else:
                    st.error("Failed to save prompt")

st.sidebar.divider()

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
                        k=k_retrieval,
                        system_prompt=st.session_state.rag_system_prompt
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
