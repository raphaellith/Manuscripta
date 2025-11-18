import sqlite3
import os
from typing import List, Dict, Optional, Any

class DBManager:
    def __init__(self, db_path: str = "db/workbench.db", schema_path: str = "db/schema.sql"):
        self.db_path = db_path
        self.schema_path = schema_path
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
