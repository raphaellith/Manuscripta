import streamlit as st
import os
from modules.llm_client import LLMClient
from modules.rag_engine import RAGEngine
from modules.prompt_ui import render_prompt_selector
from modules.style_manager import load_custom_css

st.set_page_config(page_title="RAG Lab", layout="wide")
load_custom_css()

def init_session_state():
    if "rag_engine" not in st.session_state:
        # Initialize with a default embedding model, can be made configurable if needed
        st.session_state.rag_engine = RAGEngine()
    if "rag_messages" not in st.session_state:
        st.session_state.rag_messages = []
    if "selected_model" not in st.session_state:
        st.session_state.selected_model = None

def main():
    init_session_state()
    client = LLMClient()
    
    # Sidebar Configuration
    with st.sidebar:
        from modules.ui_utils import render_sidebar_header
        render_sidebar_header()
        st.header("⚙️ Configuration")
        
        # Model Selector
        if client.check_connection():
            models = client.list_models()
            if models:
                index = 0
                if st.session_state.selected_model in models:
                    index = models.index(st.session_state.selected_model)
                elif models:
                    st.session_state.selected_model = models[0]
                
                st.session_state.selected_model = st.selectbox(
                    "Select Model", 
                    models, 
                    index=index if st.session_state.selected_model in models else 0
                )
            else:
                st.warning("No models found. Please pull a model using `ollama pull <model>`.")
        else:
            st.error("🔴 Ollama not connected. Is it running?")
            
        st.divider()
        st.subheader("System Prompt")
        base_system_prompt = render_prompt_selector(key="rag_system_prompt")

        st.divider()
        st.subheader("Hyperparameters")
        temperature = st.slider("Temperature", 0.0, 1.0, 0.7, 0.1)
        
        st.divider()
        st.subheader("RAG Settings")
        chunk_size = st.slider("Chunk Size", 100, 2000, 1000, 100)
        chunk_overlap = st.slider("Chunk Overlap", 0, 500, 200, 50)
        top_k = st.slider("Retrieval Top K", 1, 10, 4, 1)

    st.title("RAG Lab & X-Ray")
    st.markdown("Upload a document and chat with it using Retrieval Augmented Generation.")

    # File Uploader
    uploaded_file = st.file_uploader("Upload Document (PDF/TXT)", type=["pdf", "txt"])
    
    if uploaded_file:
        # Check if file is already processed to avoid re-processing on every rerun
        # We use a simple check on filename. In a real app, we might hash the content.
        if "current_file" not in st.session_state or st.session_state.current_file != uploaded_file.name:
            with st.spinner("Ingesting document..."):
                st.session_state.rag_engine.clear()
                splits = st.session_state.rag_engine.ingest_file(uploaded_file, chunk_size, chunk_overlap)
                st.session_state.current_file = uploaded_file.name
                st.success(f"Ingested {len(splits)} chunks from {uploaded_file.name}.")
    
    # Chat Interface
    for msg in st.session_state.rag_messages:
        with st.chat_message(msg["role"]):
            st.markdown(msg["content"])
            if "retrieved_context" in msg:
                with st.expander("View Retrieved Context (X-Ray)"):
                    for i, doc in enumerate(msg["retrieved_context"]):
                        st.markdown(f"**Chunk {i+1}:**")
                        st.code(doc.page_content)
                        st.markdown("---")

    if prompt := st.chat_input("Ask a question about the document..."):
        st.session_state.rag_messages.append({"role": "user", "content": prompt})
        with st.chat_message("user"):
            st.markdown(prompt)

        with st.chat_message("assistant"):
            if not st.session_state.selected_model:
                st.error("Please select a model.")
            else:
                # Retrieval
                retrieved_docs = st.session_state.rag_engine.retrieve(prompt, k=top_k)
                
                # Construct Prompt with Context
                if retrieved_docs:
                    context_text = "\n\n".join([doc.page_content for doc in retrieved_docs])
                    system_prompt = f"{base_system_prompt}\n\nUse the following context to answer the user's question.\n\nContext:\n{context_text}"
                else:
                    system_prompt = base_system_prompt
                    context_text = "No context retrieved."
                
                messages = [
                    {"role": "system", "content": system_prompt},
                    {"role": "user", "content": prompt}
                ]
                
                # Stream Response
                response_placeholder = st.empty()
                full_response = ""
                
                try:
                    stream = client.chat(
                        model=st.session_state.selected_model,
                        messages=messages,
                        options={"temperature": temperature},
                        stream=True
                    )
                    
                    for chunk in stream:
                        if 'message' in chunk:
                            content = chunk['message']['content']
                            full_response += content
                            response_placeholder.markdown(full_response + "▌")
                    
                    response_placeholder.markdown(full_response)
                    
                    # Save to history with context
                    st.session_state.rag_messages.append({
                        "role": "assistant", 
                        "content": full_response,
                        "retrieved_context": retrieved_docs
                    })
                    
                    # X-Ray for the new message
                    if retrieved_docs:
                        with st.expander("View Retrieved Context (X-Ray)"):
                            for i, doc in enumerate(retrieved_docs):
                                st.markdown(f"**Chunk {i+1}:**")
                                st.code(doc.page_content)
                                st.markdown("---")
                except Exception as e:
                    st.error(f"Error generating response: {e}")

if __name__ == "__main__":
    main()
