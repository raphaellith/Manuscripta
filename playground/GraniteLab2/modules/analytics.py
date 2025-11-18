import pandas as pd
from typing import List, Dict, Any

def process_results_to_dataframe(results: List[Dict[str, Any]]) -> pd.DataFrame:
    """
    Converts a list of result dictionaries to a Pandas DataFrame
    and ensures numeric columns are correctly typed.
    """
    if not results:
        return pd.DataFrame()
        
    df = pd.DataFrame(results)
    
    # Ensure numeric columns are numeric
    numeric_cols = ['tps', 'duration_ms', 'ttft_ms']
    for col in numeric_cols:
        if col in df.columns:
            df[col] = pd.to_numeric(df[col], errors='coerce')
            
    # Create short prompt for display
    if 'prompt_content' in df.columns:
        df['prompt_short'] = df['prompt_content'].str.slice(0, 50) + "..."
        
    return df

def get_average_metrics_by_model(df: pd.DataFrame) -> pd.DataFrame:
    """
    Calculates average TPS and Duration per model.
    """
    if df.empty:
        return pd.DataFrame()
        
    # Group by model and calculate means
    metrics = df.groupby('model')[['tps', 'duration_ms']].mean().reset_index()
    return metrics

def create_pivot_table(df: pd.DataFrame) -> pd.DataFrame:
    """
    Creates a pivot table for qualitative comparison.
    Rows=Prompt, Cols=Model, Values=Output
    """
    if df.empty:
        return pd.DataFrame()
        
    pivot_df = df.pivot_table(
        index='prompt_short', 
        columns='model', 
        values='output', 
        aggfunc='first' 
    )
    return pivot_df
