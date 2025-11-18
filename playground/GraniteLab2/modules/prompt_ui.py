import streamlit as st
from modules.db_manager import DBManager

def render_prompt_selector(label: str = "System Prompt", key: str = "system_prompt_selector") -> str:
    """
    Renders a UI component to select a system prompt from the database or enter a custom one.
    Returns the content of the selected prompt.
    """
    db = DBManager()
    system_prompts = db.get_all_system_prompts()
    
    # Initialize session state for this component if not exists
    if f"{key}_mode" not in st.session_state:
        st.session_state[f"{key}_mode"] = "Saved Personas"
    
    tab1, tab2 = st.tabs(["Saved Personas", "Custom Input"])
    
    selected_content = ""
    
    with tab1:
        if not system_prompts:
            st.info("No saved system prompts found. Add one in the Prompt Library.")
            prompt_options = ["Default (Helpful Assistant)"]
            prompt_map = {"Default (Helpful Assistant)": "You are a helpful assistant."}
        else:
            prompt_options = ["Default (Helpful Assistant)"] + [p["alias"] for p in system_prompts]
            prompt_map = {p["alias"]: p["content"] for p in system_prompts}
            prompt_map["Default (Helpful Assistant)"] = "You are a helpful assistant."
            
        selected_alias = st.selectbox(
            f"Select {label}", 
            options=prompt_options, 
            key=f"{key}_select"
        )
        
        selected_content = prompt_map.get(selected_alias, "")
        
        # Show preview
        with st.expander("Preview Prompt"):
            st.text(selected_content)

    with tab2:
        custom_content = st.text_area(
            f"Enter Custom {label}", 
            value="You are a helpful assistant.", 
            key=f"{key}_custom",
            height=150
        )
        # If the user is in this tab, we want to use the custom content
        # We can track which tab is active by checking which input was last interacted with, 
        # but Streamlit tabs don't inherently tell us which is active.
        # A common pattern is to use a radio button or just prioritize one.
        # However, since we return a value, we need to know which one the user intends.
        # Let's add a radio button above tabs or just assume if they type in custom it overrides?
        # Better: Use a radio button to toggle mode explicitly if tabs are confusing for state.
        # OR, just return based on a "Mode" selector.
        
    # Let's use a mode selector above the tabs for clarity, or just rely on the fact that 
    # we need to return *something*.
    # If we use tabs, we can't easily know which tab is "active".
    # So let's add a radio button to select source.
    
    mode = st.radio("Source", ["Saved", "Custom"], horizontal=True, label_visibility="collapsed", key=f"{key}_mode_radio")
    
    if mode == "Saved":
        return selected_content
    else:
        return st.session_state.get(f"{key}_custom", "")
