import streamlit as st
import pandas as pd
from modules.db_manager import DBManager
from modules.style_manager import load_custom_css

def render_prompt_library():
    load_custom_css()
    st.header("Prompt Library")
    
    db = DBManager()
    
    with st.sidebar:
        from modules.ui_utils import render_sidebar_header
        render_sidebar_header()
        st.header("Settings")
    
    tab1, tab2 = st.tabs(["User Prompts", "System Prompts"])
    
    with tab1:
        st.subheader("Manage User Prompts")
        
        # Add New Prompt Form
        with st.expander("Add New Prompt"):
            with st.form("add_prompt_form"):
                new_alias = st.text_input("Alias (Unique Name)")
                new_content = st.text_area("Prompt Content")
                new_tags = st.text_input("Tags (comma-separated)")
                submitted = st.form_submit_button("Save Prompt")
                
                if submitted:
                    if new_alias and new_content:
                        try:
                            db.add_prompt(new_alias, new_content, new_tags)
                            st.success(f"Prompt '{new_alias}' added!")
                            st.rerun()
                        except ValueError as e:
                            st.error(str(e))
                    else:
                        st.error("Alias and Content are required.")

        # View/Edit Prompts
        prompts = db.get_all_prompts()
        if prompts:
            df = pd.DataFrame(prompts)
            # Reorder columns for better view
            df = df[['id', 'alias', 'tags', 'content', 'created_at']]
            
            st.data_editor(
                df,
                key="prompt_editor",
                num_rows="dynamic",
                column_config={
                    "id": st.column_config.NumberColumn(disabled=True),
                    "created_at": st.column_config.DatetimeColumn(disabled=True),
                    "content": st.column_config.TextColumn(width="large"),
                },
                use_container_width=True,
                hide_index=True
            )
            
            if "prompt_editor" in st.session_state:
                changes = st.session_state["prompt_editor"]
                needs_rerun = False
                
                # Handle Edits
                if changes["edited_rows"]:
                    for index, changes_dict in changes["edited_rows"].items():
                        row_id = df.iloc[index]["id"]
                        current_alias = df.iloc[index]["alias"]
                        current_content = df.iloc[index]["content"]
                        current_tags = df.iloc[index]["tags"]
                        
                        new_alias = changes_dict.get("alias", current_alias)
                        new_content = changes_dict.get("content", current_content)
                        new_tags = changes_dict.get("tags", current_tags)
                        
                        try:
                            db.update_prompt(int(row_id), new_alias, new_content, new_tags)
                            needs_rerun = True
                        except ValueError as e:
                            st.error(str(e))
                            
                # Handle Deletes
                if changes["deleted_rows"]:
                    for index in changes["deleted_rows"]:
                        row_id = df.iloc[index]["id"]
                        db.delete_prompt(int(row_id))
                        needs_rerun = True
                
                # Handle Adds
                if changes["added_rows"]:
                    for new_row in changes["added_rows"]:
                        if "alias" in new_row and "content" in new_row:
                            try:
                                db.add_prompt(new_row["alias"], new_row["content"], new_row.get("tags", ""))
                                needs_rerun = True
                            except ValueError as e:
                                st.error(str(e))
                
                if needs_rerun:
                    st.rerun()
            
        else:
            st.info("No prompts found. Add one above!")

    with tab2:
        st.subheader("Manage System Prompts (Personas)")
        
        # Add New System Prompt Form
        with st.expander("Add New Persona"):
            with st.form("add_sys_prompt_form"):
                sys_alias = st.text_input("Alias (e.g., 'Strict Coder')")
                sys_content = st.text_area("System Prompt Content")
                sys_submitted = st.form_submit_button("Save Persona")
                
                if sys_submitted:
                    if sys_alias and sys_content:
                        try:
                            db.add_system_prompt(sys_alias, sys_content)
                            st.success(f"Persona '{sys_alias}' added!")
                            st.rerun()
                        except ValueError as e:
                            st.error(str(e))
                    else:
                        st.error("Alias and Content are required.")

        # View/Edit System Prompts
        sys_prompts = db.get_all_system_prompts()
        if sys_prompts:
            sys_df = pd.DataFrame(sys_prompts)
            sys_df = sys_df[['id', 'alias', 'content', 'created_at']]
            
            st.data_editor(
                sys_df,
                key="sys_prompt_editor",
                num_rows="dynamic",
                column_config={
                    "id": st.column_config.NumberColumn(disabled=True),
                    "created_at": st.column_config.DatetimeColumn(disabled=True),
                    "content": st.column_config.TextColumn(width="large"),
                },
                use_container_width=True,
                hide_index=True
            )
            
            if "sys_prompt_editor" in st.session_state:
                sys_changes = st.session_state["sys_prompt_editor"]
                needs_rerun = False
                
                # Handle Edits
                if sys_changes["edited_rows"]:
                    for index, changes_dict in sys_changes["edited_rows"].items():
                        row_id = sys_df.iloc[index]["id"]
                        current_alias = sys_df.iloc[index]["alias"]
                        current_content = sys_df.iloc[index]["content"]
                        
                        new_alias = changes_dict.get("alias", current_alias)
                        new_content = changes_dict.get("content", current_content)
                        
                        try:
                            db.update_system_prompt(int(row_id), new_alias, new_content)
                            needs_rerun = True
                        except ValueError as e:
                            st.error(str(e))

                # Handle Deletes
                if sys_changes["deleted_rows"]:
                    for index in sys_changes["deleted_rows"]:
                        row_id = sys_df.iloc[index]["id"]
                        db.delete_system_prompt(int(row_id))
                        needs_rerun = True

                # Handle Adds
                if sys_changes["added_rows"]:
                    for new_row in sys_changes["added_rows"]:
                        if "alias" in new_row and "content" in new_row:
                            try:
                                db.add_system_prompt(new_row["alias"], new_row["content"])
                                needs_rerun = True
                            except ValueError as e:
                                st.error(str(e))
                
                if needs_rerun:
                    st.rerun()

        else:
            st.info("No system prompts found.")

if __name__ == "__main__":
    render_prompt_library()
