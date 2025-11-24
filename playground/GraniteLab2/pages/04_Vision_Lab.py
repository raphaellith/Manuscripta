import streamlit as st
from modules.llm_client import LLMClient
from modules.ui_utils import init_session_state, render_sidebar
from modules.prompt_ui import render_prompt_selector
from modules.style_manager import load_custom_css
from PIL import Image
import io

def main():
    st.set_page_config(page_title="Vision Lab", layout="wide")
    load_custom_css()
    init_session_state()
    
    client = LLMClient()
    render_sidebar(client)
    
    with st.sidebar:
        from modules.ui_utils import render_sidebar_header
        render_sidebar_header()
        st.header("Configuration")
        st.header("System Prompt")
        system_prompt = render_prompt_selector(key="vision_system_prompt")

    st.title("Vision Lab")
    st.markdown("Experiment with image-to-text capabilities (OCR, Description, Extraction).")

    col1, col2 = st.columns([1, 1])

    with col1:
        st.subheader("Input Image")
        uploaded_file = st.file_uploader("Upload an image", type=["png", "jpg", "jpeg"])
        
        image_bytes = None
        if uploaded_file is not None:
            image = Image.open(uploaded_file)
            st.image(image, caption="Uploaded Image", use_column_width=True)
            
            # Convert to bytes
            img_byte_arr = io.BytesIO()
            image.save(img_byte_arr, format=image.format)
            image_bytes = img_byte_arr.getvalue()

    with col2:
        st.subheader("Analysis")
        
        if uploaded_file is None:
            st.info("Please upload an image to start.")
        else:
            if "vision_prompt" not in st.session_state:
                st.session_state.vision_prompt = ""

            st.markdown("**Quick Prompts:**")
            c1, c2, c3 = st.columns(3)
            if c1.button("Transcribe Handwriting"):
                st.session_state.vision_prompt = "Transcribe the handwriting in this image exactly as it appears."
                st.rerun()
            if c2.button("Describe in JSON"):
                st.session_state.vision_prompt = "Describe this image in detail. Output the result as a valid JSON object."
                st.rerun()
            if c3.button("Extract Data Table"):
                st.session_state.vision_prompt = "Extract the data from the table in this image and format it as CSV."
                st.rerun()

            prompt = st.text_area("Enter your prompt:", key="vision_prompt", height=100)

            if st.button("Generate", type="primary"):
                if not st.session_state.selected_model:
                    st.error("Please select a model from the sidebar.")
                elif not prompt:
                    st.error("Please enter a prompt.")
                else:
                    with st.spinner("Analyzing image..."):
                        try:
                            options = {
                                "temperature": st.session_state.temperature,
                                "top_k": st.session_state.top_k,
                                "num_ctx": st.session_state.context_window
                            }
                            
                            response_container = st.empty()
                            full_response = ""
                            
                            # Use generate for vision tasks as per spec
                            # Note: ollama.generate takes images as list of bytes
                            kwargs = {
                                "model": st.session_state.selected_model,
                                "prompt": prompt,
                                "images": [image_bytes],
                                "options": options,
                                "stream": True
                            }
                            if system_prompt:
                                kwargs["system"] = system_prompt

                            for chunk in client.generate(**kwargs):
                                if 'response' in chunk:
                                    full_response += chunk['response']
                                    response_container.markdown(full_response)
                            
                            # Final render to ensure formatting
                            response_container.markdown(full_response)
                            
                        except Exception as e:
                            st.error(f"An error occurred: {str(e)}")

if __name__ == "__main__":
    main()
