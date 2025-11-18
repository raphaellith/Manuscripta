"""
Utility functions for Granite Lab.
"""

import base64
import json
from typing import Any, Optional
from PIL import Image
import io


def encode_image_to_base64(image_file) -> str:
    """
    Encode an image to base64 string.
    
    Args:
        image_file: File-like object or PIL Image
        
    Returns:
        Base64-encoded string
    """
    if isinstance(image_file, Image.Image):
        # PIL Image object
        buffered = io.BytesIO()
        image_file.save(buffered, format="PNG")
        img_bytes = buffered.getvalue()
    else:
        # File-like object
        img_bytes = image_file.read()
    
    return base64.b64encode(img_bytes).decode('utf-8')


def parse_json_response(response: str) -> Optional[dict]:
    """
    Try to parse a JSON response from a string.
    
    Args:
        response: Response string that may contain JSON
        
    Returns:
        Parsed JSON dict or None if parsing fails
    """
    try:
        return json.loads(response)
    except json.JSONDecodeError:
        # Try to extract JSON from markdown code blocks
        if "```json" in response:
            start = response.find("```json") + 7
            end = response.find("```", start)
            if end != -1:
                try:
                    return json.loads(response[start:end].strip())
                except json.JSONDecodeError:
                    pass
        return None


def format_metrics(metrics: dict) -> str:
    """
    Format metrics dictionary into a readable string.
    
    Args:
        metrics: Metrics dictionary
        
    Returns:
        Formatted string
    """
    if not metrics:
        return "No metrics available"
    
    lines = []
    
    if 'ttft' in metrics:
        lines.append(f"TTFT: {metrics['ttft']:.3f}s")
    
    if 'tps' in metrics:
        lines.append(f"Speed: {metrics['tps']:.1f} tok/s")
    
    if 'total_duration' in metrics:
        lines.append(f"Total: {metrics['total_duration']:.2f}s")
    
    if 'total_tokens' in metrics:
        lines.append(f"Tokens: {metrics['total_tokens']}")
    
    return " | ".join(lines)


def format_file_size(size_bytes: int) -> str:
    """
    Format file size in human-readable format.
    
    Args:
        size_bytes: Size in bytes
        
    Returns:
        Formatted string (e.g., "1.5 MB")
    """
    for unit in ['B', 'KB', 'MB', 'GB']:
        if size_bytes < 1024.0:
            return f"{size_bytes:.1f} {unit}"
        size_bytes /= 1024.0
    return f"{size_bytes:.1f} TB"
