"""
Reusable UI components for system prompt management.
Provides a consistent prompt management interface across all labs.
"""

import streamlit as st
from typing import Optional
from core.prompt_manager import PromptManager


def render_prompt_selector(
    prompt_manager: PromptManager,
    session_key_prefix: str,
    default_prompt: str = "You are a helpful AI assistant.",
    height: int = 120
) -> str:
    """
    Render a universal prompt selector UI component.
    
    Args:
        prompt_manager: PromptManager instance to use
        session_key_prefix: Unique prefix for session state keys (e.g., 'chat', 'rag', 'vision')
        default_prompt: Default system prompt text
        height: Height of text areas in pixels
        
    Returns:
        The currently selected system prompt content
    """
    # Initialize session state for this instance
    system_prompt_key = f"{session_key_prefix}_system_prompt"
    if system_prompt_key not in st.session_state:
        st.session_state[system_prompt_key] = default_prompt
    
    # Prompt mode selector
    prompt_mode = st.sidebar.radio(
        "Prompt Mode",
        ["Default", "Saved Prompts", "Custom"],
        label_visibility="collapsed",
        key=f"{session_key_prefix}_prompt_mode"
    )
    
    if prompt_mode == "Default":
        st.session_state[system_prompt_key] = default_prompt
        system_prompt = st.session_state[system_prompt_key]
        st.sidebar.text_area(
            "Current Prompt",
            value=system_prompt,
            height=min(80, height),
            disabled=True,
            key=f"{session_key_prefix}_default_prompt_display"
        )
    
    elif prompt_mode == "Saved Prompts":
        system_prompt = _render_saved_prompts_ui(
            prompt_manager,
            session_key_prefix,
            system_prompt_key,
            height
        )
    
    elif prompt_mode == "Custom":
        system_prompt = _render_custom_prompt_ui(
            prompt_manager,
            session_key_prefix,
            system_prompt_key,
            height
        )
    
    return system_prompt


def _render_saved_prompts_ui(
    prompt_manager: PromptManager,
    prefix: str,
    system_prompt_key: str,
    height: int
) -> str:
    """Render the saved prompts selection UI."""
    saved_prompts = prompt_manager.list_prompts()
    
    if saved_prompts:
        # Display saved prompts with descriptions
        prompt_options = ["Select a prompt..."] + [p["name"] for p in saved_prompts]
        selected_prompt_name = st.sidebar.selectbox(
            "Choose Prompt",
            prompt_options,
            key=f"{prefix}_saved_prompt_selector"
        )
        
        if selected_prompt_name != "Select a prompt...":
            # Load the selected prompt
            loaded_prompt = prompt_manager.load_prompt(selected_prompt_name)
            if loaded_prompt:
                st.session_state[system_prompt_key] = loaded_prompt
                
                # Show description if available
                prompt_meta = next((p for p in saved_prompts if p["name"] == selected_prompt_name), None)
                if prompt_meta and prompt_meta["description"]:
                    st.sidebar.caption(f"ℹ️ {prompt_meta['description']}")
                
                # Display the prompt
                st.sidebar.text_area(
                    "Current Prompt",
                    value=loaded_prompt,
                    height=height,
                    disabled=True,
                    key=f"{prefix}_loaded_prompt_display"
                )
                
                # Edit and Delete buttons
                col_edit, col_delete = st.sidebar.columns(2)
                with col_edit:
                    if st.button("✏️ Edit", key=f"{prefix}_edit_btn", use_container_width=True):
                        st.session_state[f"{prefix}_editing_prompt"] = selected_prompt_name
                        st.session_state[f"{prefix}_editing_content"] = loaded_prompt
                        st.session_state[f"{prefix}_editing_description"] = prompt_meta.get("description", "")
                        st.rerun()
                
                with col_delete:
                    if st.button("🗑️ Delete", key=f"{prefix}_delete_btn", use_container_width=True):
                        if prompt_manager.delete_prompt(selected_prompt_name):
                            st.sidebar.success(f"Deleted '{selected_prompt_name}'")
                            st.rerun()
                        else:
                            st.sidebar.error("Failed to delete prompt")
                
                # Edit form (if editing)
                editing_key = f"{prefix}_editing_prompt"
                if editing_key in st.session_state and st.session_state[editing_key] == selected_prompt_name:
                    _render_edit_form(prompt_manager, prefix, selected_prompt_name, height)
        else:
            st.sidebar.info("Select a saved prompt from the dropdown")
    else:
        st.sidebar.info("No saved prompts yet. Use 'Custom' mode to create one.")
    
    return st.session_state[system_prompt_key]


def _render_edit_form(
    prompt_manager: PromptManager,
    prefix: str,
    prompt_name: str,
    height: int
) -> None:
    """Render the edit form for a saved prompt."""
    with st.sidebar.expander("✏️ Edit Prompt", expanded=True):
        edited_content = st.text_area(
            "Prompt Content",
            value=st.session_state[f"{prefix}_editing_content"],
            height=height,
            key=f"{prefix}_edit_content"
        )
        edited_description = st.text_input(
            "Description (optional)",
            value=st.session_state[f"{prefix}_editing_description"],
            key=f"{prefix}_edit_desc"
        )
        
        col_save, col_cancel = st.columns(2)
        with col_save:
            if st.button("💾 Save", key=f"{prefix}_save_edit", use_container_width=True):
                if prompt_manager.update_prompt(
                    prompt_name,
                    edited_content,
                    edited_description
                ):
                    st.success(f"✅ Updated '{prompt_name}'")
                    del st.session_state[f"{prefix}_editing_prompt"]
                    st.rerun()
                else:
                    st.error("Failed to update prompt")
        
        with col_cancel:
            if st.button("❌ Cancel", key=f"{prefix}_cancel_edit", use_container_width=True):
                del st.session_state[f"{prefix}_editing_prompt"]
                st.rerun()


def _render_custom_prompt_ui(
    prompt_manager: PromptManager,
    prefix: str,
    system_prompt_key: str,
    height: int
) -> str:
    """Render the custom prompt input UI."""
    # Custom prompt input
    system_prompt = st.sidebar.text_area(
        "Custom System Prompt",
        value=st.session_state[system_prompt_key],
        height=height,
        key=f"{prefix}_custom_prompt_input"
    )
    st.session_state[system_prompt_key] = system_prompt
    
    # Save prompt interface
    with st.sidebar.expander("💾 Save This Prompt"):
        prompt_name = st.text_input("Prompt Name", key=f"{prefix}_new_prompt_name")
        prompt_description = st.text_input("Description (optional)", key=f"{prefix}_new_prompt_desc")
        
        if st.button("Save Prompt", key=f"{prefix}_save_prompt_btn"):
            if not prompt_name:
                st.error("Please enter a name")
            elif prompt_manager.prompt_exists(prompt_name):
                st.warning(f"Prompt '{prompt_name}' already exists")
            elif not system_prompt.strip():
                st.error("Prompt content cannot be empty")
            else:
                if prompt_manager.save_prompt(
                    prompt_name, 
                    system_prompt, 
                    prompt_description
                ):
                    st.success(f"✅ Saved '{prompt_name}'")
                    st.rerun()
                else:
                    st.error("Failed to save prompt")
    
    return system_prompt
