"""
System Prompt Manager for Granite Lab.
Handles loading, saving, and managing system prompts.
"""

import os
from pathlib import Path
from typing import List, Dict, Optional
import json


class PromptManager:
    """Manages system prompts stored in the resources folder."""
    
    def __init__(self, prompts_dir: Optional[str] = None):
        """
        Initialize the prompt manager.
        
        Args:
            prompts_dir: Path to the prompts directory. If None, uses default location.
        """
        if prompts_dir is None:
            # Default to resources/system_prompts in the project root
            self.prompts_dir = Path(__file__).parent.parent / "resources" / "system_prompts"
        else:
            self.prompts_dir = Path(prompts_dir)
        
        # Create directory if it doesn't exist
        self.prompts_dir.mkdir(parents=True, exist_ok=True)
        
        # Create a README if it doesn't exist
        readme_path = self.prompts_dir / "README.md"
        if not readme_path.exists():
            readme_path.write_text(
                "# System Prompts\n\n"
                "This directory contains user-uploaded system prompts.\n"
                "Files in this directory are ignored by git to keep your prompts private.\n\n"
                "Each prompt is stored as a .txt file with metadata in a .json file.\n"
            )
    
    def save_prompt(self, name: str, content: str, description: str = "", overwrite: bool = False) -> bool:
        """
        Save a system prompt.
        
        Args:
            name: Name of the prompt (used as filename)
            content: The prompt content
            description: Optional description of the prompt
            overwrite: If True, allows overwriting existing prompts
            
        Returns:
            True if successful, False otherwise
        """
        try:
            # Sanitize filename
            safe_name = "".join(c for c in name if c.isalnum() or c in (' ', '-', '_')).strip()
            safe_name = safe_name.replace(' ', '_')
            
            if not safe_name:
                raise ValueError("Invalid prompt name")
            
            # Check if exists and overwrite is not allowed
            if not overwrite and self.prompt_exists(name):
                raise ValueError(f"Prompt '{name}' already exists")
            
            # Save prompt content
            prompt_path = self.prompts_dir / f"{safe_name}.txt"
            prompt_path.write_text(content, encoding='utf-8')
            
            # Save metadata
            metadata = {
                "name": name,
                "description": description,
                "filename": f"{safe_name}.txt"
            }
            metadata_path = self.prompts_dir / f"{safe_name}.json"
            metadata_path.write_text(json.dumps(metadata, indent=2), encoding='utf-8')
            
            return True
        except Exception as e:
            print(f"Error saving prompt: {e}")
            return False
    
    def update_prompt(self, name: str, content: str, description: str = "") -> bool:
        """
        Update an existing system prompt.
        
        Args:
            name: Name of the prompt to update
            content: The new prompt content
            description: Optional new description
            
        Returns:
            True if successful, False otherwise
        """
        return self.save_prompt(name, content, description, overwrite=True)
    
    def load_prompt(self, name: str) -> Optional[str]:
        """
        Load a system prompt by name.
        
        Args:
            name: Name of the prompt
            
        Returns:
            Prompt content or None if not found
        """
        try:
            # Try exact match first
            safe_name = "".join(c for c in name if c.isalnum() or c in (' ', '-', '_')).strip()
            safe_name = safe_name.replace(' ', '_')
            
            prompt_path = self.prompts_dir / f"{safe_name}.txt"
            if prompt_path.exists():
                return prompt_path.read_text(encoding='utf-8')
            
            return None
        except Exception as e:
            print(f"Error loading prompt: {e}")
            return None
    
    def list_prompts(self) -> List[Dict[str, str]]:
        """
        List all available prompts.
        
        Returns:
            List of prompt metadata dictionaries
        """
        prompts = []
        
        try:
            for metadata_file in self.prompts_dir.glob("*.json"):
                try:
                    metadata = json.loads(metadata_file.read_text(encoding='utf-8'))
                    prompts.append({
                        "name": metadata.get("name", metadata_file.stem),
                        "description": metadata.get("description", ""),
                        "filename": metadata.get("filename", f"{metadata_file.stem}.txt")
                    })
                except Exception as e:
                    print(f"Error reading metadata {metadata_file}: {e}")
                    continue
        except Exception as e:
            print(f"Error listing prompts: {e}")
        
        return sorted(prompts, key=lambda x: x["name"])
    
    def delete_prompt(self, name: str) -> bool:
        """
        Delete a system prompt.
        
        Args:
            name: Name of the prompt
            
        Returns:
            True if successful, False otherwise
        """
        try:
            safe_name = "".join(c for c in name if c.isalnum() or c in (' ', '-', '_')).strip()
            safe_name = safe_name.replace(' ', '_')
            
            prompt_path = self.prompts_dir / f"{safe_name}.txt"
            metadata_path = self.prompts_dir / f"{safe_name}.json"
            
            deleted = False
            if prompt_path.exists():
                prompt_path.unlink()
                deleted = True
            if metadata_path.exists():
                metadata_path.unlink()
                deleted = True
            
            return deleted
        except Exception as e:
            print(f"Error deleting prompt: {e}")
            return False
    
    def get_prompt_names(self) -> List[str]:
        """
        Get a list of prompt names.
        
        Returns:
            List of prompt names
        """
        return [p["name"] for p in self.list_prompts()]
    
    def prompt_exists(self, name: str) -> bool:
        """
        Check if a prompt exists.
        
        Args:
            name: Name of the prompt
            
        Returns:
            True if prompt exists, False otherwise
        """
        safe_name = "".join(c for c in name if c.isalnum() or c in (' ', '-', '_')).strip()
        safe_name = safe_name.replace(' ', '_')
        prompt_path = self.prompts_dir / f"{safe_name}.txt"
        return prompt_path.exists()
