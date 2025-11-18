import pytest
import os
import sqlite3
import json
from modules.db_manager import DBManager

DB_PATH = "tests/test_db/workbench_test.db"
SCHEMA_PATH = "db/schema.sql"

@pytest.fixture
def db_manager():
    if os.path.exists(DB_PATH):
        os.remove(DB_PATH)
    
    # Ensure schema exists or point to the real one
    # The real schema is at playground/GraniteLab2/db/schema.sql
    # We are running tests from playground/GraniteLab2 usually
    
    manager = DBManager(db_path=DB_PATH, schema_path=SCHEMA_PATH)
    yield manager
    
    if os.path.exists(DB_PATH):
        os.remove(DB_PATH)
    if os.path.exists("tests/test_db"):
        os.rmdir("tests/test_db")

def test_create_experiment(db_manager):
    config = {"models": ["model1"], "prompts": ["prompt1"], "iterations": 1}
    exp_id = db_manager.create_experiment("Test Experiment", "matrix", config)
    assert exp_id is not None
    
    conn = sqlite3.connect(DB_PATH)
    conn.row_factory = sqlite3.Row
    cursor = conn.cursor()
    cursor.execute("SELECT * FROM experiments WHERE id = ?", (exp_id,))
    row = cursor.fetchone()
    conn.close()
    
    assert row['name'] == "Test Experiment"
    assert row['experiment_type'] == "matrix"
    assert json.loads(row['config']) == config

def test_add_result(db_manager):
    config = {"models": ["model1"], "prompts": ["prompt1"], "iterations": 1}
    exp_id = db_manager.create_experiment("Test Experiment", "matrix", config)
    
    db_manager.add_result(
        experiment_id=exp_id,
        model="model1",
        prompt_content="test prompt",
        output="test output",
        duration_ms=100.0,
        tps=10.0,
        ttft_ms=50.0
    )
    
    conn = sqlite3.connect(DB_PATH)
    conn.row_factory = sqlite3.Row
    cursor = conn.cursor()
    cursor.execute("SELECT * FROM results WHERE experiment_id = ?", (exp_id,))
    row = cursor.fetchone()
    conn.close()
    
    assert row['model'] == "model1"
    assert row['prompt_content'] == "test prompt"
    assert row['output'] == "test output"
    assert row['duration_ms'] == 100.0
    assert row['tps'] == 10.0
    assert row['ttft_ms'] == 50.0
