import streamlit as st
from modules.llm_client import LLMClient
from modules.ui_utils import init_session_state, render_sidebar

def main():
    st.set_page_config(page_title="GraniteLab", layout="wide")
    init_session_state()
    
    client = LLMClient()
    render_sidebar(client)
    
    st.title("🧪 GraniteLab Workbench")
    st.write("Welcome to the GraniteLab. Select a tool from the sidebar or the pages menu.")

if __name__ == "__main__":
    main()
