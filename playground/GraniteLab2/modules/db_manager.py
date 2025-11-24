import sqlite3
import os
from typing import List, Dict, Optional, Any
import json

class DBManager:
    def __init__(self, db_path: str = None, schema_path: str = None):
        base_dir = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
        self.db_path = db_path or os.path.join(base_dir, "db", "workbench.db")
        self.schema_path = schema_path or os.path.join(base_dir, "db", "schema.sql")
        self._init_db()

    def _get_connection(self) -> sqlite3.Connection:
        """Establishes a connection to the SQLite database."""
        conn = sqlite3.connect(self.db_path)
        conn.row_factory = sqlite3.Row
        return conn

    def _init_db(self):
        """Initializes the database with the schema if it doesn't exist."""
        # Ensure the directory exists
        os.makedirs(os.path.dirname(self.db_path), exist_ok=True)
        
        conn = self._get_connection()
        try:
            with open(self.schema_path, 'r') as f:
                schema = f.read()
            conn.executescript(schema)
            conn.commit()
        except Exception as e:
            print(f"Error initializing database: {e}")
        finally:
            conn.close()

    # --- Prompts ---

    def add_prompt(self, alias: str, content: str, tags: str = "") -> int:
        """Adds a new prompt to the database."""
        conn = self._get_connection()
        try:
            cursor = conn.cursor()
            cursor.execute(
                "INSERT INTO prompts (alias, content, tags) VALUES (?, ?, ?)",
                (alias, content, tags)
            )
            conn.commit()
            return cursor.lastrowid
        except sqlite3.IntegrityError:
            raise ValueError(f"Prompt with alias '{alias}' already exists.")
        finally:
            conn.close()

    def get_all_prompts(self) -> List[Dict[str, Any]]:
        """Retrieves all prompts."""
        conn = self._get_connection()
        try:
            cursor = conn.cursor()
            cursor.execute("SELECT * FROM prompts ORDER BY created_at DESC")
            rows = cursor.fetchall()
            return [dict(row) for row in rows]
        finally:
            conn.close()

    def update_prompt(self, prompt_id: int, alias: str, content: str, tags: str):
        """Updates an existing prompt."""
        conn = self._get_connection()
        try:
            cursor = conn.cursor()
            cursor.execute(
                "UPDATE prompts SET alias = ?, content = ?, tags = ? WHERE id = ?",
                (alias, content, tags, prompt_id)
            )
            conn.commit()
        except sqlite3.IntegrityError:
             raise ValueError(f"Prompt with alias '{alias}' already exists.")
        finally:
            conn.close()

    def delete_prompt(self, prompt_id: int):
        """Deletes a prompt by ID."""
        conn = self._get_connection()
        try:
            cursor = conn.cursor()
            cursor.execute("DELETE FROM prompts WHERE id = ?", (prompt_id,))
            conn.commit()
        finally:
            conn.close()

    # --- System Prompts ---

    def add_system_prompt(self, alias: str, content: str) -> int:
        """Adds a new system prompt."""
        conn = self._get_connection()
        try:
            cursor = conn.cursor()
            cursor.execute(
                "INSERT INTO system_prompts (alias, content) VALUES (?, ?)",
                (alias, content)
            )
            conn.commit()
            return cursor.lastrowid
        except sqlite3.IntegrityError:
            raise ValueError(f"System prompt with alias '{alias}' already exists.")
        finally:
            conn.close()

    def get_all_system_prompts(self) -> List[Dict[str, Any]]:
        """Retrieves all system prompts."""
        conn = self._get_connection()
        try:
            cursor = conn.cursor()
            cursor.execute("SELECT * FROM system_prompts ORDER BY created_at DESC")
            rows = cursor.fetchall()
            return [dict(row) for row in rows]
        finally:
            conn.close()
            
    def update_system_prompt(self, prompt_id: int, alias: str, content: str):
        """Updates an existing system prompt."""
        conn = self._get_connection()
        try:
            cursor = conn.cursor()
            cursor.execute(
                "UPDATE system_prompts SET alias = ?, content = ? WHERE id = ?",
                (alias, content, prompt_id)
            )
            conn.commit()
        except sqlite3.IntegrityError:
             raise ValueError(f"System prompt with alias '{alias}' already exists.")
        finally:
            conn.close()

    def delete_system_prompt(self, prompt_id: int):
        """Deletes a system prompt by ID."""
        conn = self._get_connection()
        try:
            cursor = conn.cursor()
            cursor.execute("DELETE FROM system_prompts WHERE id = ?", (prompt_id,))
            conn.commit()
        finally:
            conn.close()

    # --- Experiments ---

    def create_experiment(self, name: str, experiment_type: str, config: Dict[str, Any]) -> int:
        """Creates a new experiment record."""
        conn = self._get_connection()
        try:
            cursor = conn.cursor()
            cursor.execute(
                "INSERT INTO experiments (name, experiment_type, config) VALUES (?, ?, ?)",
                (name, experiment_type, json.dumps(config))
            )
            conn.commit()
            return cursor.lastrowid
        finally:
            conn.close()

    def get_all_experiments(self) -> List[Dict[str, Any]]:
        """Retrieves all experiments."""
        conn = self._get_connection()
        try:
            cursor = conn.cursor()
            cursor.execute("SELECT * FROM experiments ORDER BY created_at DESC")
            rows = cursor.fetchall()
            return [dict(row) for row in rows]
        finally:
            conn.close()

    def add_result(self, experiment_id: int, model: str, prompt_content: str, output: str, 
                   duration_ms: float, tps: float, ttft_ms: float):
        """Adds a result record for an experiment."""
        conn = self._get_connection()
        try:
            cursor = conn.cursor()
            cursor.execute(
                """
                INSERT INTO results 
                (experiment_id, model, prompt_content, output, duration_ms, tps, ttft_ms) 
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """,
                (experiment_id, model, prompt_content, output, duration_ms, tps, ttft_ms)
            )
            conn.commit()
        finally:
            conn.close()

    def get_results_by_experiment_id(self, experiment_id: int) -> List[Dict[str, Any]]:
        """Retrieves all results for a specific experiment."""
        conn = self._get_connection()
        try:
            cursor = conn.cursor()
            cursor.execute("SELECT * FROM results WHERE experiment_id = ?", (experiment_id,))
            rows = cursor.fetchall()
            return [dict(row) for row in rows]
        finally:
            conn.close()
