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
from core.prompt_manager import PromptManager

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
if 'prompt_manager' not in st.session_state:
    st.session_state.prompt_manager = PromptManager()
if 'current_system_prompt' not in st.session_state:
    st.session_state.current_system_prompt = "You are a helpful AI assistant."

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

# System Prompt Management
st.sidebar.subheader("System Prompt")

# Prompt selector: default, saved prompts, or custom
prompt_mode = st.sidebar.radio(
    "Prompt Mode",
    ["Default", "Saved Prompts", "Custom"],
    label_visibility="collapsed"
)

if prompt_mode == "Default":
    st.session_state.current_system_prompt = "You are a helpful AI assistant."
    system_prompt = st.session_state.current_system_prompt
    st.sidebar.text_area(
        "Current Prompt",
        value=system_prompt,
        height=100,
        disabled=True,
        key="default_prompt_display"
    )

elif prompt_mode == "Saved Prompts":
    saved_prompts = st.session_state.prompt_manager.list_prompts()
    
    if saved_prompts:
        # Display saved prompts with descriptions
        prompt_options = ["Select a prompt..."] + [p["name"] for p in saved_prompts]
        selected_prompt_name = st.sidebar.selectbox(
            "Choose Prompt",
            prompt_options,
            key="saved_prompt_selector"
        )
        
        if selected_prompt_name != "Select a prompt...":
            # Load the selected prompt
            loaded_prompt = st.session_state.prompt_manager.load_prompt(selected_prompt_name)
            if loaded_prompt:
                st.session_state.current_system_prompt = loaded_prompt
                
                # Show description if available
                prompt_meta = next((p for p in saved_prompts if p["name"] == selected_prompt_name), None)
                if prompt_meta and prompt_meta["description"]:
                    st.sidebar.caption(f"ℹ️ {prompt_meta['description']}")
                
                # Display the prompt
                st.sidebar.text_area(
                    "Current Prompt",
                    value=loaded_prompt,
                    height=150,
                    disabled=True,
                    key="loaded_prompt_display"
                )
                
                # Delete button
                if st.sidebar.button("🗑️ Delete This Prompt", key="delete_saved_prompt"):
                    if st.session_state.prompt_manager.delete_prompt(selected_prompt_name):
                        st.sidebar.success(f"Deleted '{selected_prompt_name}'")
                        st.rerun()
                    else:
                        st.sidebar.error("Failed to delete prompt")
        else:
            st.sidebar.info("Select a saved prompt from the dropdown")
    else:
        st.sidebar.info("No saved prompts yet. Use 'Custom' mode to create one.")
    
    system_prompt = st.session_state.current_system_prompt

elif prompt_mode == "Custom":
    # Custom prompt input
    system_prompt = st.sidebar.text_area(
        "Custom System Prompt",
        value=st.session_state.current_system_prompt,
        height=150,
        key="custom_prompt_input"
    )
    st.session_state.current_system_prompt = system_prompt
    
    # Save prompt interface
    with st.sidebar.expander("💾 Save This Prompt"):
        prompt_name = st.text_input("Prompt Name", key="new_prompt_name")
        prompt_description = st.text_input("Description (optional)", key="new_prompt_desc")
        
        if st.button("Save Prompt", key="save_prompt_btn"):
            if not prompt_name:
                st.error("Please enter a name")
            elif st.session_state.prompt_manager.prompt_exists(prompt_name):
                st.warning(f"Prompt '{prompt_name}' already exists")
            elif not system_prompt.strip():
                st.error("Prompt content cannot be empty")
            else:
                if st.session_state.prompt_manager.save_prompt(
                    prompt_name, 
                    system_prompt, 
                    prompt_description
                ):
                    st.success(f"✅ Saved '{prompt_name}'")
                    st.rerun()
                else:
                    st.error("Failed to save prompt")

st.sidebar.divider()

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
