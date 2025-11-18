#!/bin/bash

# Granite Lab Launcher

echo "🧪 Launching Granite Lab..."
echo ""

# Check if Ollama is running
if ! curl -s http://localhost:11434/api/tags > /dev/null 2>&1; then
    echo "⚠️  Ollama is not running!"
    echo "   Please start Ollama with: ollama serve"
    exit 1
fi

# Launch Streamlit
streamlit run main.py
