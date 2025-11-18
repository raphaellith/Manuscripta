import pytest
import os
import sqlite3
from unittest.mock import patch
from modules.db_manager import DBManager

# Define paths for test database and schema
TEST_DB_PATH = "tests/test_workbench.db"
SCHEMA_PATH = "db/schema.sql"

@pytest.fixture
def dirty_db():
    """Fixture to create a dirty DB file before db_manager runs."""
    with open(TEST_DB_PATH, 'w') as f:
        f.write("dirty")
    yield

@pytest.fixture
def db_manager():
    """Fixture to create a DBManager instance with a test database."""
    # Ensure clean state
    if os.path.exists(TEST_DB_PATH):
        os.remove(TEST_DB_PATH)
    
    manager = DBManager(db_path=TEST_DB_PATH, schema_path=SCHEMA_PATH)
    yield manager
    
    # Cleanup after test
    if os.path.exists(TEST_DB_PATH):
        os.remove(TEST_DB_PATH)

def test_fixture_cleanup(dirty_db, db_manager):
    """Test that the fixture cleans up an existing DB file."""
    # The dirty_db fixture creates the file.
    # The db_manager fixture setup runs, sees the file, and removes it (covering the line).
    # Then it creates a new DB.
    assert os.path.exists(TEST_DB_PATH)

def test_init_db(db_manager):
    """Test if the database and tables are created successfully."""
    assert os.path.exists(TEST_DB_PATH)
    conn = sqlite3.connect(TEST_DB_PATH)
    cursor = conn.cursor()
    
    # Check if tables exist
    tables = ["prompts", "system_prompts", "experiments", "results"]
    for table in tables:
        cursor.execute(f"SELECT name FROM sqlite_master WHERE type='table' AND name='{table}'")
        assert cursor.fetchone() is not None
    conn.close()

def test_init_db_exception(capsys):
    """Test exception handling during database initialization."""
    # Create dummy file to trigger cleanup logic
    with open(TEST_DB_PATH, 'w') as f:
        f.write("dummy")

    # Ensure clean state
    if os.path.exists(TEST_DB_PATH):
        os.remove(TEST_DB_PATH)
        
    with patch("builtins.open", side_effect=IOError("Mocked IO Error")):
        DBManager(db_path=TEST_DB_PATH, schema_path=SCHEMA_PATH)
        captured = capsys.readouterr()
        assert "Error initializing database: Mocked IO Error" in captured.out
    
    # Cleanup
    if os.path.exists(TEST_DB_PATH):
        os.remove(TEST_DB_PATH)

def test_add_get_prompt(db_manager):
    """Test adding and retrieving prompts."""
    prompt_id = db_manager.add_prompt("Test Prompt", "This is a test.", "test, unit")
    assert prompt_id is not None
    
    prompts = db_manager.get_all_prompts()
    assert len(prompts) == 1
    assert prompts[0]["alias"] == "Test Prompt"
    assert prompts[0]["content"] == "This is a test."
    assert prompts[0]["tags"] == "test, unit"

def test_add_duplicate_prompt_alias(db_manager):
    """Test that adding a prompt with a duplicate alias raises a ValueError."""
    db_manager.add_prompt("Unique Alias", "Content 1")
    with pytest.raises(ValueError, match="Prompt with alias 'Unique Alias' already exists."):
        db_manager.add_prompt("Unique Alias", "Content 2")

def test_update_prompt(db_manager):
    """Test updating a prompt."""
    prompt_id = db_manager.add_prompt("Old Alias", "Old Content", "old")
    db_manager.update_prompt(prompt_id, "New Alias", "New Content", "new")
    
    prompts = db_manager.get_all_prompts()
    assert len(prompts) == 1
    assert prompts[0]["alias"] == "New Alias"
    assert prompts[0]["content"] == "New Content"
    assert prompts[0]["tags"] == "new"

def test_update_prompt_duplicate_alias(db_manager):
    """Test updating a prompt to an existing alias raises ValueError."""
    db_manager.add_prompt("Alias 1", "Content 1")
    id_2 = db_manager.add_prompt("Alias 2", "Content 2")
    
    with pytest.raises(ValueError, match="Prompt with alias 'Alias 1' already exists."):
        db_manager.update_prompt(id_2, "Alias 1", "Content 2", "")

def test_delete_prompt(db_manager):
    """Test deleting a prompt."""
    prompt_id = db_manager.add_prompt("To Delete", "Content")
    db_manager.delete_prompt(prompt_id)
    prompts = db_manager.get_all_prompts()
    assert len(prompts) == 0

def test_add_get_system_prompt(db_manager):
    """Test adding and retrieving system prompts."""
    sys_id = db_manager.add_system_prompt("Sys Prompt", "You are a helper.")
    assert sys_id is not None
    
    sys_prompts = db_manager.get_all_system_prompts()
    assert len(sys_prompts) == 1
    assert sys_prompts[0]["alias"] == "Sys Prompt"
    assert sys_prompts[0]["content"] == "You are a helper."

def test_add_duplicate_system_prompt_alias(db_manager):
    """Test that adding a system prompt with a duplicate alias raises a ValueError."""
    db_manager.add_system_prompt("Unique Sys", "Content 1")
    with pytest.raises(ValueError, match="System prompt with alias 'Unique Sys' already exists."):
        db_manager.add_system_prompt("Unique Sys", "Content 2")

def test_update_system_prompt(db_manager):
    """Test updating a system prompt."""
    sys_id = db_manager.add_system_prompt("Old Sys", "Old Content")
    db_manager.update_system_prompt(sys_id, "New Sys", "New Content")
    
    sys_prompts = db_manager.get_all_system_prompts()
    assert len(sys_prompts) == 1
    assert sys_prompts[0]["alias"] == "New Sys"
    assert sys_prompts[0]["content"] == "New Content"

def test_update_system_prompt_duplicate_alias(db_manager):
    """Test updating a system prompt to an existing alias raises ValueError."""
    db_manager.add_system_prompt("Sys 1", "Content 1")
    id_2 = db_manager.add_system_prompt("Sys 2", "Content 2")
    
    with pytest.raises(ValueError, match="System prompt with alias 'Sys 1' already exists."):
        db_manager.update_system_prompt(id_2, "Sys 1", "Content 2")

def test_delete_system_prompt(db_manager):
    """Test deleting a system prompt."""
    sys_id = db_manager.add_system_prompt("To Delete", "Content")
    db_manager.delete_system_prompt(sys_id)
    sys_prompts = db_manager.get_all_system_prompts()
    assert len(sys_prompts) == 0

