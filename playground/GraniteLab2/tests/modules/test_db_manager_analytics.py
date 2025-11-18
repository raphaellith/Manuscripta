import pytest
import sqlite3
import os
import json
from modules.db_manager import DBManager

# Use a temporary database for testing
TEST_DB_PATH = "tests/test_workbench_analytics.db"
TEST_SCHEMA_PATH = "db/schema.sql"

@pytest.fixture
def db_manager():
    if os.path.exists(TEST_DB_PATH):
        os.remove(TEST_DB_PATH)
    
    # Ensure schema exists or mock it? 
    # The schema path in DBManager defaults to db/schema.sql relative to CWD.
    # We should probably use the real schema.
    
    db = DBManager(db_path=TEST_DB_PATH, schema_path=TEST_SCHEMA_PATH)
    yield db
    
    if os.path.exists(TEST_DB_PATH):
        os.remove(TEST_DB_PATH)

def test_get_all_experiments(db_manager):
    # Create some experiments
    exp1_id = db_manager.create_experiment("Exp 1", "chat", {"temp": 0.7})
    exp2_id = db_manager.create_experiment("Exp 2", "rag", {"k": 5})
    
    experiments = db_manager.get_all_experiments()
    
    assert len(experiments) == 2
    # Ordered by created_at DESC, so Exp 2 should be first (or second depending on speed, but usually LIFO)
    # Actually created_at is timestamp, so if created fast, might be same second.
    # But let's check IDs or names.
    names = [e['name'] for e in experiments]
    assert "Exp 1" in names
    assert "Exp 2" in names
    
    # Check fields
    exp1 = next(e for e in experiments if e['id'] == exp1_id)
    assert exp1['experiment_type'] == "chat"
    assert json.loads(exp1['config']) == {"temp": 0.7}

def test_get_results_by_experiment_id(db_manager):
    # Create experiment
    exp_id = db_manager.create_experiment("Exp Results", "chat", {})
    
    # Add results
    db_manager.add_result(exp_id, "modelA", "prompt1", "output1", 100.0, 10.0, 50.0)
    db_manager.add_result(exp_id, "modelB", "prompt1", "output2", 200.0, 5.0, 100.0)
    
    # Add result for another experiment
    other_exp_id = db_manager.create_experiment("Other Exp", "chat", {})
    db_manager.add_result(other_exp_id, "modelA", "prompt1", "output3", 100.0, 10.0, 50.0)
    
    results = db_manager.get_results_by_experiment_id(exp_id)
    
    assert len(results) == 2
    models = [r['model'] for r in results]
    assert "modelA" in models
    assert "modelB" in models
    
    # Check fields
    res1 = next(r for r in results if r['model'] == "modelA")
    assert res1['output'] == "output1"
    assert res1['duration_ms'] == 100.0
    assert res1['tps'] == 10.0
    assert res1['ttft_ms'] == 50.0

def test_get_results_empty(db_manager):
    exp_id = db_manager.create_experiment("Empty Exp", "chat", {})
    results = db_manager.get_results_by_experiment_id(exp_id)
    assert len(results) == 0
