import streamlit as st
import time
from modules.llm_client import LLMClient

def main():
    st.set_page_config(page_title="The Arena", layout="wide")
    
    st.title("⚔️ The Arena")
    st.markdown("Compare two models side-by-side.")

    client = LLMClient()
    
    if not client.check_connection():
        st.error("🔴 Ollama not connected. Is it running?")
        return

    models = client.list_models()
    if not models:
        st.warning("No models found. Please pull a model using `ollama pull <model>`.")
        return

    col1, col2 = st.columns(2)

    with col1:
        st.subheader("Model A")
        model_a = st.selectbox("Select Model A", models, key="model_a", index=0)
        container_a = st.container()

    with col2:
        st.subheader("Model B")
        # Try to select a different model for B if available
        index_b = 1 if len(models) > 1 else 0
        model_b = st.selectbox("Select Model B", models, key="model_b", index=index_b)
        container_b = st.container()

    prompt = st.chat_input("Enter your prompt here...")

    if prompt:
        # Display the prompt
        st.write(f"**Prompt:** {prompt}")
        
        # Copy Prompt Button (using code block for easy copying)
        st.code(prompt, language="text")

        # Run Model A
        with container_a:
            st.markdown(f"**{model_a}**")
            status_a = st.empty()
            status_a.write("Generating...")
            
            start_time_a = time.time()
            response_a_text = ""
            placeholder_a = st.empty()
            
            try:
                # We use stream=True to show progress, though for side-by-side 
                # strictly sequential might be better if we want to measure pure generation time without UI overhead
                # But streaming is better UX.
                stream_a = client.chat(
                    model=model_a,
                    messages=[{'role': 'user', 'content': prompt}],
                    stream=True
                )
                
                for chunk in stream_a:
                    content = chunk['message']['content']
                    response_a_text += content
                    placeholder_a.markdown(response_a_text)
                
                end_time_a = time.time()
                duration_a = end_time_a - start_time_a
                status_a.success(f"Done in {duration_a:.2f}s")
                
            except Exception as e:
                status_a.error(f"Error: {e}")

        # Run Model B
        with container_b:
            st.markdown(f"**{model_b}**")
            status_b = st.empty()
            status_b.write("Generating...")
            
            start_time_b = time.time()
            response_b_text = ""
            placeholder_b = st.empty()
            
            try:
                stream_b = client.chat(
                    model=model_b,
                    messages=[{'role': 'user', 'content': prompt}],
                    stream=True
                )
                
                for chunk in stream_b:
                    content = chunk['message']['content']
                    response_b_text += content
                    placeholder_b.markdown(response_b_text)
                
                end_time_b = time.time()
                duration_b = end_time_b - start_time_b
                status_b.success(f"Done in {duration_b:.2f}s")
                
            except Exception as e:
                status_b.error(f"Error: {e}")

if __name__ == "__main__":
    main()
