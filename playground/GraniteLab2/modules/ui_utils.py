import streamlit as st
from modules.llm_client import LLMClient

def init_session_state():
    if "selected_model" not in st.session_state:
        st.session_state.selected_model = None
    if "temperature" not in st.session_state:
        st.session_state.temperature = 0.7
    if "top_k" not in st.session_state:
        st.session_state.top_k = 40
    if "context_window" not in st.session_state:
        st.session_state.context_window = 4096

def render_sidebar_header():
    st.sidebar.markdown('<h1 class="sidebar-header">GRANITE LABS 2</h1>', unsafe_allow_html=True)
    st.sidebar.divider()

def render_sidebar(llm_client: LLMClient):
    with st.sidebar:
        render_sidebar_header()

        st.header("⚙️ Configuration")
        
        # Model Selector
        if llm_client.check_connection():
            models = llm_client.list_models()
            if models:
                index = 0
                if st.session_state.selected_model in models:
                    index = models.index(st.session_state.selected_model)
                
                st.session_state.selected_model = st.selectbox(
                    "Select Model", 
                    models, 
                    index=index
                )
            else:
                st.warning("No models found. Please pull a model using `ollama pull <model>`.")
        else:
            st.error("🔴 Ollama not connected. Is it running?")

        st.divider()
        
        # Hyperparameters
        st.subheader("Hyperparameters")
        st.session_state.temperature = st.slider(
            "Temperature", 
            min_value=0.0, max_value=1.0, value=st.session_state.temperature, step=0.1,
            help="Controls randomness. Higher = more creative."
        )
        
        st.session_state.top_k = st.slider(
            "Top K", 
            min_value=1, max_value=100, value=st.session_state.top_k, step=1,
            help="Limits the next token selection to the K most likely tokens."
        )
        
        st.session_state.context_window = st.slider(
            "Context Window", 
            min_value=2048, max_value=32768, value=st.session_state.context_window, step=1024,
            help="Size of the context window in tokens."
        )
