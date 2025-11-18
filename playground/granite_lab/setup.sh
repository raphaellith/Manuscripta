#!/bin/bash

# Granite Lab Setup Script

echo "🧪 Granite Lab Setup"
echo "===================="
echo ""

# Check if Python is installed
if ! command -v python3 &> /dev/null; then
    echo "❌ Python 3 is not installed. Please install Python 3.10 or higher."
    exit 1
fi

echo "✅ Python 3 found"

# Check if Ollama is running
if ! curl -s http://localhost:11434/api/tags > /dev/null 2>&1; then
    echo "⚠️  Ollama is not running."
    echo "   Please start Ollama with: ollama serve"
    echo ""
    read -p "Do you want to continue anyway? (y/n) " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        exit 1
    fi
else
    echo "✅ Ollama is running"
fi

# Install dependencies
echo ""
echo "📦 Installing dependencies..."
pip3 install -r requirements.txt

if [ $? -eq 0 ]; then
    echo "✅ Dependencies installed successfully"
else
    echo "❌ Failed to install dependencies"
    exit 1
fi

echo ""
echo "🎉 Setup complete!"
echo ""
echo "To launch Granite Lab:"
echo "  streamlit run main.py"
echo ""
echo "Recommended models to pull:"
echo "  ollama pull granite3-dense:8b    # General chat"
echo "  ollama pull nomic-embed-text     # For RAG"
echo "  ollama pull llava               # For vision"
echo ""
