"""
Granite Lab Core Module
Handles LLM interaction, RAG, and utility functions.
"""

from .client import ModelEngine
from .rag import RAGExperiment
from .utils import encode_image_to_base64, parse_json_response

__all__ = ['ModelEngine', 'RAGExperiment', 'encode_image_to_base64', 'parse_json_response']
