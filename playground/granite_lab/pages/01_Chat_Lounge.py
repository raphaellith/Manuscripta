"""
Chat Lounge - Standard chat with deep telemetry
"""

import streamlit as st
import sys
from pathlib import Path

# Add parent directory to path for imports
sys.path.append(str(Path(__file__).parent.parent))

from core.client import ModelEngine
from core.utils import format_metrics

# Page configuration
st.set_page_config(
    page_title="Chat Lounge - Granite Lab",
    page_icon="⚗",
    layout="wide"
)

st.title("Chat Lounge")
st.markdown("Standard chat interaction with deep telemetry")

# Initialize session state
if 'chat_history' not in st.session_state:
    st.session_state.chat_history = []
if 'chat_engine' not in st.session_state:
    st.session_state.chat_engine = None

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
    "Model",
    available_models,
    index=0
)

# System prompt
system_prompt = st.sidebar.text_area(
    "System Prompt",
    value="You are a helpful AI assistant.",
    height=100
)

# Parameters
temperature = st.sidebar.slider(
    "Temperature",
    min_value=0.0,
    max_value=1.0,
    value=0.7,
    step=0.1,
    help="Higher values make output more random"
)

top_k = st.sidebar.slider(
    "Top K",
    min_value=1,
    max_value=100,
    value=40,
    help="Number of highest probability tokens to consider"
)

# Initialize or update engine
if (st.session_state.chat_engine is None or 
    st.session_state.chat_engine.model_name != selected_model or
    st.session_state.chat_engine.temperature != temperature or
    st.session_state.chat_engine.top_k != top_k):
    
    st.session_state.chat_engine = ModelEngine(
        model_name=selected_model,
        temperature=temperature,
        top_k=top_k
    )

# Clear chat button
if st.sidebar.button("Clear Chat", use_container_width=True):
    st.session_state.chat_history = []
    st.rerun()

st.sidebar.divider()

# Telemetry display
st.sidebar.header("Last Response Telemetry")
if st.session_state.chat_engine and st.session_state.chat_engine.get_last_metrics():
    metrics = st.session_state.chat_engine.get_last_metrics()
    
    col1, col2 = st.sidebar.columns(2)
    with col1:
        st.metric("TTFT", f"{metrics['ttft']:.3f}s")
        st.metric("TPS", f"{metrics['tps']:.1f}")
    with col2:
        st.metric("Duration", f"{metrics['total_duration']:.2f}s")
        st.metric("Tokens", metrics['total_tokens'])
else:
    st.sidebar.info("No metrics yet. Send a message to see telemetry.")

# Main chat interface
st.divider()

# Display chat history
for message in st.session_state.chat_history:
    with st.chat_message(message["role"]):
        st.markdown(message["content"])
        
        # Show metrics for assistant messages
        if message["role"] == "assistant" and "metrics" in message:
            with st.expander("Response Metrics"):
                st.text(format_metrics(message["metrics"]))

# Chat input
if prompt := st.chat_input("Type your message..."):
    # Add user message to history
    st.session_state.chat_history.append({
        "role": "user",
        "content": prompt
    })
    
    # Display user message
    with st.chat_message("user"):
        st.markdown(prompt)
    
    # Generate response
    with st.chat_message("assistant"):
        with st.spinner("Thinking..."):
            try:
                response = st.session_state.chat_engine.invoke(
                    prompt=prompt,
                    system_prompt=system_prompt
                )
                
                st.markdown(response)
                
                # Get metrics
                metrics = st.session_state.chat_engine.get_last_metrics()
                
                # Add assistant message to history
                st.session_state.chat_history.append({
                    "role": "assistant",
                    "content": response,
                    "metrics": metrics
                })
                
                # Display metrics
                if metrics:
                    with st.expander("Response Metrics"):
                        st.text(format_metrics(metrics))
                
                st.rerun()
                
            except Exception as e:
                st.error(f"Error: {str(e)}")
                
                # Check if model is available
                if "not found" in str(e).lower():
                    st.info(f"TIP: Try pulling the model: `ollama pull {selected_model}`")

# Footer
st.divider()
st.caption(f"Using model: **{selected_model}** | Temperature: **{temperature}** | Top K: **{top_k}**")
