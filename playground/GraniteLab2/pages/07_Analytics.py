import streamlit as st
import pandas as pd
import plotly.express as px
from modules.db_manager import DBManager
from modules.analytics import process_results_to_dataframe, get_average_metrics_by_model, create_pivot_table
from modules.style_manager import load_custom_css

st.set_page_config(page_title="Lab Analytics", layout="wide")
load_custom_css()

st.title("Lab Analytics")

db = DBManager()

# --- 1. Experiment Selection ---
experiments = db.get_all_experiments()

if not experiments:
    st.warning("No experiments found. Go to 'The Matrix' to run some tests!")
    st.stop()

experiment_options = {f"{e['id']} - {e['name']} ({e['created_at']})": e['id'] for e in experiments}
selected_experiment_label = st.selectbox("Select Experiment", list(experiment_options.keys()))
selected_experiment_id = experiment_options[selected_experiment_label]

# --- 2. Data Processing ---
results = db.get_results_by_experiment_id(selected_experiment_id)

if not results:
    st.info("No results found for this experiment.")
    st.stop()

df = process_results_to_dataframe(results)

# --- 3. Visuals (Plotly) ---
st.subheader("Performance Metrics")

col1, col2 = st.columns(2)

avg_metrics = get_average_metrics_by_model(df)

with col1:
    st.markdown("#### Average TPS per Model")
    fig_tps = px.bar(avg_metrics, x='model', y='tps', color='model', title="Tokens Per Second (Higher is Better)")
    st.plotly_chart(fig_tps, use_container_width=True)

with col2:
    st.markdown("#### Average Latency per Model")
    fig_latency = px.bar(avg_metrics, x='model', y='duration_ms', color='model', title="Total Duration (ms) (Lower is Better)")
    st.plotly_chart(fig_latency, use_container_width=True)

# --- 4. Pivot Table ---
st.subheader("Qualitative Comparison")

pivot_df = create_pivot_table(df)

st.dataframe(pivot_df, use_container_width=True)

# --- 5. Drill-Down / Detail Inspector ---
st.subheader("Drill-Down Inspector")

st.markdown("Select a specific result to view full details.")

# Filter controls for drill down
c1, c2 = st.columns(2)
with c1:
    selected_model = st.selectbox("Filter by Model", ["All"] + list(df['model'].unique()))
with c2:
    # Create a mapping for prompts to select
    unique_prompts = df['prompt_content'].unique()
    prompt_map = {p[:50] + "...": p for p in unique_prompts}
    selected_prompt_short = st.selectbox("Filter by Prompt", ["All"] + list(prompt_map.keys()))

filtered_df = df.copy()
if selected_model != "All":
    filtered_df = filtered_df[filtered_df['model'] == selected_model]
if selected_prompt_short != "All":
    filtered_df = filtered_df[filtered_df['prompt_content'] == prompt_map[selected_prompt_short]]

# Display the filtered list to select from
st.markdown(f"Found {len(filtered_df)} results.")

# Use selection mode to pick a row
event = st.dataframe(
    filtered_df[['id', 'model', 'prompt_short', 'tps', 'duration_ms']],
    selection_mode="single-row",
    on_select="rerun",
    use_container_width=True,
    hide_index=True
)

if event.selection.rows:
    selected_index = event.selection.rows[0]
    # Get the actual row from the filtered dataframe
    # st.dataframe selection index corresponds to the displayed dataframe index (0 to N-1)
    # We need to access the row by integer location in the filtered_df
    selected_row = filtered_df.iloc[selected_index]
    with st.sidebar:
        from modules.ui_utils import render_sidebar_header
        render_sidebar_header()
        st.header("Filters")
    st.divider()
    st.markdown(f"### Result Details (ID: {selected_row['id']})")
    
    m1, m2, m3 = st.columns(3)
    m1.metric("Model", selected_row['model'])
    m2.metric("TPS", f"{selected_row['tps']:.2f}")
    m3.metric("Duration", f"{selected_row['duration_ms']:.2f} ms")
    
    st.markdown("#### Prompt")
    st.code(selected_row['prompt_content'], language="text")
    
    st.markdown("#### Output")
    st.code(selected_row['output'], language="markdown")
    
    st.markdown("#### Full Metadata")
    st.json(selected_row.to_dict())

