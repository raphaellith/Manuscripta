"""
Model Arena - A/B testing for side-by-side comparison
"""

import streamlit as st
import sys
from pathlib import Path
import asyncio
from concurrent.futures import ThreadPoolExecutor

# Add parent directory to path for imports
sys.path.append(str(Path(__file__).parent.parent))

from core.client import ModelEngine
from core.utils import format_metrics
from core.prompt_manager import PromptManager
from core.prompt_ui import render_prompt_selector

# Page configuration
st.set_page_config(
    page_title="Model Arena - Granite Lab",
    page_icon="⚗",
    layout="wide"
)

st.title("Model Arena")
st.markdown("A/B testing: Compare two models side-by-side")

# Initialize session state
if 'arena_results' not in st.session_state:
    st.session_state.arena_results = []
if 'arena_prompt_manager' not in st.session_state:
    st.session_state.arena_prompt_manager = PromptManager()

# Sidebar controls
st.sidebar.header("Configuration")

# Check Ollama connection
if not ModelEngine.check_ollama_connection():
    st.error("WARNING: Ollama is not running. Please launch it with `ollama serve`")
    st.stop()

# Get available models
available_models = ModelEngine.list_available_models()
if not available_models:
    st.error("No models available. Pull models with `ollama pull <model-name>`")
    st.stop()

if len(available_models) < 2:
    st.warning("WARNING: Only one model available. Pull another model to compare.")

# Temperature slider (shared)
temperature = st.sidebar.slider(
    "Temperature (Shared)",
    min_value=0.0,
    max_value=1.0,
    value=0.7,
    step=0.1
)

st.sidebar.divider()

# System Prompt Management
st.sidebar.subheader("System Prompt (Shared)")
system_prompt = render_prompt_selector(
    prompt_manager=st.session_state.arena_prompt_manager,
    session_key_prefix="arena",
    default_prompt="You are a helpful AI assistant.",
    height=120
)

st.sidebar.divider()

# Clear results
if st.sidebar.button("Clear Results", use_container_width=True):
    st.session_state.arena_results = []
    st.rerun()

st.sidebar.divider()
st.sidebar.caption(f"{len(st.session_state.arena_results)} comparison(s) performed")

# Main interface
st.header("Select Models")

col1, col2 = st.columns(2)

with col1:
    st.subheader("Model A")
    model_a = st.selectbox(
        "Choose Model A",
        available_models,
        index=0,
        key="model_a"
    )

with col2:
    st.subheader("Model B")
    model_b = st.selectbox(
        "Choose Model B",
        available_models,
        index=min(1, len(available_models) - 1),
        key="model_b"
    )

if model_a == model_b:
    st.warning("WARNING: You selected the same model for both sides. Consider choosing different models for comparison.")

st.divider()

# Shared prompt
st.header("Shared Prompt")
prompt = st.text_area(
    "Enter a prompt to send to both models:",
    height=150,
    placeholder="Write a haiku about AI...",
    key="shared_prompt"
)

# Run button
if st.button("▶️ Run Comparison", type="primary", use_container_width=True, disabled=not prompt):
    col_left, col_right = st.columns(2)
    
    # Containers for results
    with col_left:
        st.subheader("Model A Response")
        placeholder_a = st.empty()
        metrics_a = st.empty()
    
    with col_right:
        st.subheader("Model B Response")
        placeholder_b = st.empty()
        metrics_b = st.empty()
    
    # Function to invoke model
    def invoke_model(model_name, prompt_text, sys_prompt):
        engine = ModelEngine(
            model_name=model_name,
            temperature=temperature
        )
        response = engine.invoke(prompt_text, sys_prompt)
        metrics = engine.get_last_metrics()
        return response, metrics
    
    # Run both models in parallel
    with st.spinner("Running both models..."):
        try:
            with ThreadPoolExecutor(max_workers=2) as executor:
                # Submit both tasks
                future_a = executor.submit(invoke_model, model_a, prompt, system_prompt)
                future_b = executor.submit(invoke_model, model_b, prompt, system_prompt)
                
                # Get results
                response_a, metrics_a_data = future_a.result()
                response_b, metrics_b_data = future_b.result()
            
            # Display results
            with placeholder_a:
                st.markdown(response_a)
            
            with placeholder_b:
                st.markdown(response_b)
            
            with metrics_a:
                st.info(format_metrics(metrics_a_data))
            
            with metrics_b:
                st.info(format_metrics(metrics_b_data))
            
            st.success("[OK] Comparison complete!")
            st.session_state.arena_results.append({
                "prompt": prompt,
                "model_a": model_a,
                "response_a": response_a,
                "metrics_a": metrics_a_data,
                "model_b": model_b,
                "response_b": response_b,
                "metrics_b": metrics_b_data
            })
            
            st.success("✅ Comparison complete!")
            
        except Exception as e:
            st.error(f"Error: {str(e)}")

# Display history
if st.session_state.arena_results:
    st.divider()
    st.header("Comparison History")
    
    for idx, result in enumerate(reversed(st.session_state.arena_results)):
        with st.expander(f"Comparison {len(st.session_state.arena_results) - idx}: {result['prompt'][:60]}...", expanded=(idx==0)):
            st.markdown(f"**Prompt:** {result['prompt']}")
            
            st.divider()
            
            col_hist_a, col_hist_b = st.columns(2)
            
            with col_hist_a:
                st.markdown(f"### Model A: {result['model_a']}")
                st.markdown(result['response_a'])
                st.caption(format_metrics(result['metrics_a']))
            
            with col_hist_b:
                st.markdown(f"### Model B: {result['model_b']}")
                st.markdown(result['response_b'])
                st.caption(format_metrics(result['metrics_b']))
            
            # Comparison summary
            st.divider()
            st.markdown("**Performance Comparison:**")
            
            comp_col1, comp_col2, comp_col3 = st.columns(3)
            
            with comp_col1:
                if result['metrics_a']['ttft'] < result['metrics_b']['ttft']:
                    st.metric("Faster TTFT", result['model_a'], 
                             f"{result['metrics_a']['ttft']:.3f}s vs {result['metrics_b']['ttft']:.3f}s")
                else:
                    st.metric("Faster TTFT", result['model_b'],
                             f"{result['metrics_b']['ttft']:.3f}s vs {result['metrics_a']['ttft']:.3f}s")
            
            with comp_col2:
                if result['metrics_a']['tps'] > result['metrics_b']['tps']:
                    st.metric("Higher TPS", result['model_a'],
                             f"{result['metrics_a']['tps']:.1f} vs {result['metrics_b']['tps']:.1f}")
                else:
                    st.metric("Higher TPS", result['model_b'],
                             f"{result['metrics_b']['tps']:.1f} vs {result['metrics_a']['tps']:.1f}")
            
            with comp_col3:
                if result['metrics_a']['total_duration'] < result['metrics_b']['total_duration']:
                    st.metric("Faster Overall", result['model_a'],
                             f"{result['metrics_a']['total_duration']:.2f}s vs {result['metrics_b']['total_duration']:.2f}s")
                else:
                    st.metric("Faster Overall", result['model_b'],
                             f"{result['metrics_b']['total_duration']:.2f}s vs {result['metrics_a']['total_duration']:.2f}s")

# Footer
st.divider()
st.caption(f"Comparing: **{model_a}** vs **{model_b}** | Temperature: **{temperature}**")
st.caption("TIP: Run the same prompt multiple times to see consistency variations")
