import streamlit as st
import time
import json
from modules.llm_client import LLMClient
from modules.ui_utils import init_session_state, render_sidebar
from modules.style_manager import load_custom_css
from modules.prompt_ui import render_prompt_selector

def init_chat_state():
    if "messages" not in st.session_state:
        st.session_state.messages = []
    if "json_mode" not in st.session_state:
        st.session_state.json_mode = False

def main():
    st.set_page_config(page_title="Granite Lab", layout="wide")
    load_custom_css()
    init_session_state()
    init_chat_state()
    
    client = LLMClient()
    render_sidebar(client)
    
    # Sidebar for Chat Lab specific settings
    with st.sidebar:
        st.divider()
        st.header("🧪 Lab Settings")
        
        # System Prompt Selector
        system_prompt = render_prompt_selector(key="chat_system_prompt")
        st.divider()
        
        st.session_state.json_mode = st.toggle("JSON Mode", value=st.session_state.json_mode, help="Enforce JSON output format.")
        
        if st.button("Clear History"):
            st.session_state.messages = []
            st.rerun()
            
        chat_json = json.dumps(st.session_state.messages, indent=2)
        st.download_button(
            label="Save Conversation",
            data=chat_json,
            file_name="conversation.json",
            mime="application/json"
        )

    st.title("💬 Chat Lab")
    
    # Display chat messages
    for message in st.session_state.messages:
        with st.chat_message(message["role"]):
            st.markdown(message["content"])

    # Chat input
    if prompt := st.chat_input("Type your message..."):
        # Add user message to history
        st.session_state.messages.append({"role": "user", "content": prompt})
        with st.chat_message("user"):
            st.markdown(prompt)

        # Generate response
        with st.chat_message("assistant"):
            message_placeholder = st.empty()
            full_response = ""
            
            # Prepare options
            options = {
                "temperature": st.session_state.get("temperature", 0.7),
                "top_k": st.session_state.get("top_k", 40),
                "num_ctx": st.session_state.get("context_window", 4096)
            }
            
            if st.session_state.json_mode:
                options["format"] = "json" # This might need to be passed differently depending on ollama version, but usually it's a top level param or in options? 
                # Actually ollama.chat has a 'format' parameter.
            
            # Prepare messages
            # If JSON mode is on, we might want to append a system instruction if not present
            messages_payload = [m for m in st.session_state.messages]
            
            # Prepend System Prompt
            if system_prompt:
                messages_payload.insert(0, {"role": "system", "content": system_prompt})
            
            if st.session_state.json_mode:
                 # Ensure the user knows JSON is expected, or rely on the 'format' param
                 pass

            start_time = time.time()
            ttft = 0
            first_token_received = False
            
            try:
                # Check if model is selected
                model = st.session_state.get("selected_model")
                if not model:
                    st.error("Please select a model in the sidebar.")
                    return

                # Call LLM
                # Note: format='json' is a parameter of chat(), not options.
                kwargs = {
                    "model": model,
                    "messages": messages_payload,
                    "options": options,
                    "stream": True
                }
                if st.session_state.json_mode:
                    kwargs["format"] = "json"

                stream = client.chat(**kwargs)
                
                for chunk in stream:
                    if not first_token_received:
                        ttft = time.time() - start_time
                        first_token_received = True
                    
                    content = chunk.get("message", {}).get("content", "")
                    full_response += content
                    message_placeholder.markdown(full_response + "▌")
                    
                    # Check for final metrics in the last chunk
                    if chunk.get("done"):
                        eval_count = chunk.get("eval_count", 0)
                        eval_duration = chunk.get("eval_duration", 0) # nanoseconds
                        
                        if eval_duration > 0:
                            tps = eval_count / (eval_duration / 1e9)
                            # st.caption(f"⏱️ TTFT: {ttft*1000:.2f}ms | ⚡ TPS: {tps:.2f} | 📏 Tokens: {eval_count}")
                            m1, m2, m3 = st.columns(3)
                            m1.metric("TTFT (ms)", f"{ttft*1000:.2f}")
                            m2.metric("TPS", f"{tps:.2f}")
                            m3.metric("Tokens", f"{eval_count}")

                message_placeholder.markdown(full_response)
                st.session_state.messages.append({"role": "assistant", "content": full_response})
                
            except Exception as e:
                st.error(f"An error occurred: {e}")

if __name__ == "__main__":
    main()
