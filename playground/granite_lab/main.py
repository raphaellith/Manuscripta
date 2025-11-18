"""
Granite Lab - Main Entry Point
A local-first testing environment for Large Language Models via Ollama.
"""

import streamlit as st
from core.client import ModelEngine

# Page configuration
st.set_page_config(
    page_title="Granite Lab",
    page_icon="⚗",
    layout="wide",
    initial_sidebar_state="expanded"
)

# Custom CSS for technical aesthetic
st.markdown("""
<style>
    .main-header {
        font-size: 3rem;
        font-weight: bold;
        text-align: center;
        margin-bottom: 1rem;
    }
    .sub-header {
        font-size: 1.2rem;
        text-align: center;
        color: #666;
        margin-bottom: 2rem;
    }
    .feature-card {
        padding: 1.5rem;
        border-radius: 0.5rem;
        background-color: #f0f2f6;
        margin-bottom: 1rem;
    }
    .status-indicator {
        display: inline-block;
        width: 10px;
        height: 10px;
        border-radius: 50%;
        margin-right: 8px;
    }
    .status-online {
        background-color: #00ff00;
    }
    .status-offline {
        background-color: #ff0000;
    }
</style>
""", unsafe_allow_html=True)

# Check Ollama connection
ollama_online = ModelEngine.check_ollama_connection()

# Header
st.markdown('<div class="main-header">Granite Lab</div>', unsafe_allow_html=True)
st.markdown('<div class="sub-header">Local-first testing environment for Large Language Models</div>', unsafe_allow_html=True)

# Connection status
if ollama_online:
    st.markdown(
        '<span class="status-indicator status-online"></span> Ollama Connected',
        unsafe_allow_html=True
    )
    models = ModelEngine.list_available_models()
    if models:
        st.success(f"[OK] {len(models)} model(s) available: {', '.join(models[:5])}{'...' if len(models) > 5 else ''}")
else:
    st.markdown(
        '<span class="status-indicator status-offline"></span> Ollama Offline',
        unsafe_allow_html=True
    )
    st.error("WARNING: Ollama is not running. Please launch it with `ollama serve`")
    st.stop()

st.divider()

# Feature cards
st.markdown("## Features")

col1, col2 = st.columns(2)

with col1:
    st.markdown("""
    <div class="feature-card">
        <h3>Chat Lounge</h3>
        <p>Standard chat interaction with deep telemetry:</p>
        <ul>
            <li>Time to First Token (TTFT)</li>
            <li>Tokens per second (TPS)</li>
            <li>Configurable temperature & context</li>
        </ul>
    </div>
    """, unsafe_allow_html=True)
    
    st.markdown("""
    <div class="feature-card">
        <h3>RAG Inspector</h3>
        <p>Debug retrieval quality with transparency:</p>
        <ul>
            <li>PDF/TXT document ingestion</li>
            <li>View retrieved chunks & scores</li>
            <li>ChromaDB ephemeral vector store</li>
        </ul>
    </div>
    """, unsafe_allow_html=True)

with col2:
    st.markdown("""
    <div class="feature-card">
        <h3>Vision Lab</h3>
        <p>Test multimodal capabilities:</p>
        <ul>
            <li>Image upload & preview</li>
            <li>OCR & transcription</li>
            <li>Support for granite-vision, llava</li>
        </ul>
    </div>
    """, unsafe_allow_html=True)
    
    st.markdown("""
    <div class="feature-card">
        <h3>Model Arena</h3>
        <p>A/B testing side-by-side:</p>
        <ul>
            <li>Compare two models simultaneously</li>
            <li>Shared prompt input</li>
            <li>Real-time speed comparison</li>
        </ul>
    </div>
    """, unsafe_allow_html=True)

st.divider()

# Getting started
st.markdown("## Getting Started")

st.markdown("""
1. **Select a feature** from the sidebar navigation
2. **Choose your model** from the available Ollama models
3. **Start experimenting!** All data is ephemeral and local

### Prerequisites
- Python 3.10+
- Ollama running (`ollama serve`)
- Models pulled (e.g., `ollama pull granite3-dense:8b`)
""")

st.divider()

# Footer
st.markdown("""
<div style="text-align: center; color: #666; padding: 2rem;">
    <p>Built with Streamlit + LangChain + Ollama</p>
    <p>Zero-setup • Local-first • Privacy-focused</p>
</div>
""", unsafe_allow_html=True)
