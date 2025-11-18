import pytest
import pandas as pd
from modules.analytics import process_results_to_dataframe, get_average_metrics_by_model, create_pivot_table

@pytest.fixture
def sample_results():
    return [
        {
            "id": 1,
            "model": "modelA",
            "prompt_content": "This is a long prompt content that should be truncated",
            "output": "Output A",
            "duration_ms": 100.0,
            "tps": 10.0,
            "ttft_ms": 50.0
        },
        {
            "id": 2,
            "model": "modelB",
            "prompt_content": "This is a long prompt content that should be truncated",
            "output": "Output B",
            "duration_ms": 200.0,
            "tps": 5.0,
            "ttft_ms": 100.0
        },
        {
            "id": 3,
            "model": "modelA",
            "prompt_content": "Short prompt",
            "output": "Output A2",
            "duration_ms": 150.0,
            "tps": 12.0,
            "ttft_ms": 60.0
        }
    ]

def test_process_results_to_dataframe(sample_results):
    df = process_results_to_dataframe(sample_results)
    
    assert not df.empty
    assert len(df) == 3
    assert 'prompt_short' in df.columns
    assert df['prompt_short'].iloc[0].endswith("...")
    assert df['tps'].dtype.kind in 'fi' # float or int
    assert df['duration_ms'].dtype.kind in 'fi'

def test_process_results_empty():
    df = process_results_to_dataframe([])
    assert df.empty

def test_get_average_metrics_by_model(sample_results):
    df = process_results_to_dataframe(sample_results)
    metrics = get_average_metrics_by_model(df)
    
    assert len(metrics) == 2 # modelA and modelB
    
    modelA_metrics = metrics[metrics['model'] == 'modelA'].iloc[0]
    assert modelA_metrics['tps'] == 11.0 # (10 + 12) / 2
    assert modelA_metrics['duration_ms'] == 125.0 # (100 + 150) / 2
    
    modelB_metrics = metrics[metrics['model'] == 'modelB'].iloc[0]
    assert modelB_metrics['tps'] == 5.0
    assert modelB_metrics['duration_ms'] == 200.0

def test_get_average_metrics_empty():
    metrics = get_average_metrics_by_model(pd.DataFrame())
    assert metrics.empty

def test_create_pivot_table(sample_results):
    df = process_results_to_dataframe(sample_results)
    pivot = create_pivot_table(df)
    
    assert not pivot.empty
    assert 'modelA' in pivot.columns
    assert 'modelB' in pivot.columns
    
    # Check values
    # prompt_short for first item
    prompt_short = df['prompt_short'].iloc[0]
    assert pivot.loc[prompt_short, 'modelA'] == "Output A"
    assert pivot.loc[prompt_short, 'modelB'] == "Output B"

def test_create_pivot_table_empty():
    pivot = create_pivot_table(pd.DataFrame())
    assert pivot.empty
