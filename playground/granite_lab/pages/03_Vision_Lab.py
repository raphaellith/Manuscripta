"""
Vision Lab - Test multimodal capabilities (OCR/Vision)
"""

import streamlit as st
import sys
from pathlib import Path
from PIL import Image

# Add parent directory to path for imports
sys.path.append(str(Path(__file__).parent.parent))

from core.client import ModelEngine
from core.utils import encode_image_to_base64
from core.prompt_manager import PromptManager
from core.prompt_ui import render_prompt_selector

# Page configuration
st.set_page_config(
    page_title="Vision Lab - Granite Lab",
    page_icon="⚗",
    layout="wide"
)

st.title("Vision Lab")
st.markdown("Test multimodal models for OCR and image understanding")

# Initialize session state
if 'vision_engine' not in st.session_state:
    st.session_state.vision_engine = None
if 'uploaded_image' not in st.session_state:
    st.session_state.uploaded_image = None
if 'vision_results' not in st.session_state:
    st.session_state.vision_results = []
if 'vision_prompt_manager' not in st.session_state:
    st.session_state.vision_prompt_manager = PromptManager()

# Sidebar controls
st.sidebar.header("Configuration")

# Check Ollama connection
if not ModelEngine.check_ollama_connection():
    st.error("WARNING: Ollama is not running. Please launch it with `ollama serve`")
    st.stop()

# Model selector (filter for vision models)
available_models = ModelEngine.list_available_models()
if not available_models:
    st.error("No models available. Pull a vision model with `ollama pull llava` or `ollama pull granite-vision`")
    st.stop()

# Suggest vision models
vision_model_keywords = ['vision', 'llava', 'bakllava', 'llava-llama3', 'moondream']
vision_models = [m for m in available_models if any(keyword in m.lower() for keyword in vision_model_keywords)]

if vision_models:
    st.sidebar.success(f"[OK] {len(vision_models)} vision model(s) detected")
    default_model = vision_models[0]
else:
    st.sidebar.warning("WARNING: No vision models detected. Select any model or pull a vision model.")
    default_model = available_models[0]

selected_model = st.sidebar.selectbox(
    "Vision Model",
    available_models,
    index=available_models.index(default_model) if default_model in available_models else 0
)

st.sidebar.divider()

# System Prompt Management
st.sidebar.subheader("System Prompt")
system_prompt = render_prompt_selector(
    prompt_manager=st.session_state.vision_prompt_manager,
    session_key_prefix="vision",
    default_prompt="You are a helpful vision assistant.",
    height=120
)

st.sidebar.divider()

# Initialize engine
if st.session_state.vision_engine is None or st.session_state.vision_engine.model_name != selected_model:
    st.session_state.vision_engine = ModelEngine(model_name=selected_model)

# Clear results
if st.sidebar.button("Clear Results", use_container_width=True):
    st.session_state.vision_results = []
    st.rerun()

# Main interface
col1, col2 = st.columns([1, 1])

with col1:
    st.header("Image Upload")
    
    # Image upload
    uploaded_file = st.file_uploader(
        "Upload an image",
        type=["png", "jpg", "jpeg", "bmp", "gif"],
        help="Upload an image to analyze"
    )
    
    if uploaded_file:
        # Load and display image
        image = Image.open(uploaded_file)
        st.session_state.uploaded_image = image
        
        st.image(image, caption=f"Uploaded: {uploaded_file.name}", use_container_width=True)
        
        # Image info
        st.caption(f"Size: {image.size[0]} x {image.size[1]} pixels | Format: {image.format}")
    else:
        st.info("Upload an image to get started")
        st.session_state.uploaded_image = None

with col2:
    st.header("Prompt & Analysis")
    
    if st.session_state.uploaded_image is None:
        st.warning("WARNING: Upload an image first")
    else:
        # Prompt input
        prompt = st.text_area(
            "What would you like to know about this image?",
            value="Describe this image in detail.",
            height=100,
            placeholder="Ask a question about the image..."
        )
        
        # Analyze button
        if st.button("Analyze Image", type="primary", use_container_width=True, disabled=not prompt):
            with st.spinner("Analyzing image..."):
                try:
                    # Encode image to base64
                    image_b64 = encode_image_to_base64(st.session_state.uploaded_image)
                    
                    # Invoke multimodal model
                    response = st.session_state.vision_engine.invoke_multimodal(
                        prompt=prompt,
                        image_data=image_b64,
                        system_prompt=system_prompt if system_prompt else None
                    )
                    
                    # Get metrics
                    metrics = st.session_state.vision_engine.get_last_metrics()
                    
                    # Store result
                    st.session_state.vision_results.append({
                        "prompt": prompt,
                        "response": response,
                        "metrics": metrics,
                        "image": st.session_state.uploaded_image.copy()
                    })
                    
                    st.rerun()
                    
                except Exception as e:
                    st.error(f"Error: {str(e)}")
                    
                    if "not found" in str(e).lower():
                        st.info(f"TIP: This might not be a vision model. Try: `ollama pull llava`")
                    elif "multimodal" in str(e).lower() or "image" in str(e).lower():
                        st.info(f"TIP: Model '{selected_model}' may not support vision. Try a vision-capable model.")

# Display results
if st.session_state.vision_results:
    st.divider()
    st.header("Analysis Results")
    
    for idx, result in enumerate(reversed(st.session_state.vision_results)):
        with st.expander(f"Result {len(st.session_state.vision_results) - idx}: {result['prompt'][:50]}...", expanded=(idx==0)):
            col_a, col_b = st.columns([1, 1])
            
            with col_a:
                st.image(result['image'], caption="Analyzed Image", use_container_width=True)
            
            with col_b:
                st.markdown("**Model Response:**")
                st.markdown(result['response'])
                
                if result['metrics']:
                    st.divider()
                    st.caption(f"Processing time: {result['metrics']['total_duration']:.2f}s")

# Footer
st.divider()
st.caption(f"Using model: **{selected_model}**")
st.caption("TIP: Vision models work best with clear, high-contrast images")
