import streamlit as st
import time
import json
from modules.db_manager import DBManager
from modules.llm_client import LLMClient

st.set_page_config(page_title="The Matrix", layout="wide")

st.title("The Matrix: Batch Experiment Engine")
st.markdown("Define and execute large-scale validation runs.")

db = DBManager()
llm = LLMClient()

# --- Configuration Wizard ---
st.header("1. Configuration")

col1, col2 = st.columns(2)

with col1:
    available_models = llm.list_models()
    selected_models = st.multiselect("Select Models", available_models)

    iterations = st.number_input("Iterations per Prompt", min_value=1, value=1)

with col2:
    all_prompts = db.get_all_prompts()
    prompt_options = {f"{p['alias']} (ID: {p['id']})": p for p in all_prompts}
    selected_prompt_names = st.multiselect("Select Prompts", list(prompt_options.keys()))
    selected_prompts = [prompt_options[name] for name in selected_prompt_names]

experiment_name = st.text_input("Experiment Name", value=f"Run {time.strftime('%Y-%m-%d %H:%M')}")

# --- Execution Engine ---
if st.button("Start Matrix Run", type="primary"):
    if not selected_models or not selected_prompts:
        st.error("Please select at least one model and one prompt.")
    else:
        st.header("2. Execution Log")
        
        # Create Experiment
        config = {
            "models": selected_models,
            "prompts": [p['alias'] for p in selected_prompts],
            "iterations": iterations
        }
        experiment_id = db.create_experiment(experiment_name, "matrix", config)
        st.success(f"Experiment '{experiment_name}' initialized (ID: {experiment_id})")

        # Progress Bar
        total_runs = len(selected_models) * len(selected_prompts) * iterations
        progress_bar = st.progress(0)
        status_text = st.empty()
        log_area = st.empty()
        logs = []

        completed_runs = 0

        for model in selected_models:
            for prompt_data in selected_prompts:
                for i in range(iterations):
                    run_index = completed_runs + 1
                    status_msg = f"Processing Run {run_index}/{total_runs}: {model} on '{prompt_data['alias']}' (Iter {i+1})"
                    status_text.text(status_msg)
                    
                    start_time = time.time()
                    first_token_time = None
                    full_response = ""
                    
                    try:
                        # We use chat for consistency, assuming prompts are user messages
                        messages = [{"role": "user", "content": prompt_data['content']}]
                        
                        # Stream response to calculate metrics
                        stream = llm.chat(model=model, messages=messages, stream=True)
                        
                        token_count = 0
                        for chunk in stream:
                            if first_token_time is None:
                                first_token_time = time.time()
                            
                            content = chunk['message']['content']
                            full_response += content
                            token_count += 1 # Approximation, ideally use tokenizer
                        
                        end_time = time.time()
                        duration_ms = (end_time - start_time) * 1000
                        ttft_ms = (first_token_time - start_time) * 1000 if first_token_time else 0
                        tps = token_count / (duration_ms / 1000) if duration_ms > 0 else 0
                        
                        # Save Result
                        db.add_result(
                            experiment_id=experiment_id,
                            model=model,
                            prompt_content=prompt_data['content'],
                            output=full_response,
                            duration_ms=duration_ms,
                            tps=tps,
                            ttft_ms=ttft_ms
                        )
                        
                        logs.append(f"✅ [SUCCESS] {model} | {prompt_data['alias']} | {duration_ms:.2f}ms | {tps:.2f} TPS")

                    except Exception as e:
                        logs.append(f"❌ [ERROR] {model} | {prompt_data['alias']} | {str(e)}")
                        # Log failure in DB as well? The requirement says "log 'ERROR' and continue". 
                        # We can save a result with error message in output or similar.
                        db.add_result(
                            experiment_id=experiment_id,
                            model=model,
                            prompt_content=prompt_data['content'],
                            output=f"ERROR: {str(e)}",
                            duration_ms=0,
                            tps=0,
                            ttft_ms=0
                        )

                    # Update UI
                    completed_runs += 1
                    progress_bar.progress(completed_runs / total_runs)
                    log_area.code("\n".join(logs[-10:])) # Show last 10 logs

        st.success("Matrix Run Completed!")
