# 🚀 Granite Lab - Quick Start Guide

## One-Time Setup

### 1. Install Ollama
```bash
# Visit https://ollama.ai and download the installer
# Or use Homebrew on macOS:
brew install ollama
```

### 2. Start Ollama
```bash
ollama serve
```
Keep this terminal running in the background.

### 3. Pull Models
```bash
# Required for chat features
ollama pull granite3-dense:8b

# Required for RAG features
ollama pull nomic-embed-text

# Optional: For vision features
ollama pull llava
```

### 4. Install Python Dependencies
```bash
cd playground/granite_lab
./setup.sh
# Or manually:
pip install -r requirements.txt
```

## Running Granite Lab

### Quick Launch
```bash
cd playground/granite_lab
./run.sh
```

### Manual Launch
```bash
cd playground/granite_lab
streamlit run main.py
```

The app will open automatically in your browser at `http://localhost:8501`

## First Steps

### Try Chat Lounge First
1. Click "Chat Lounge" in the sidebar
2. Select a model (e.g., `granite3-dense:8b`)
3. Start chatting!
4. Watch the telemetry metrics update

### Experiment with RAG
1. Click "RAG Inspector" in the sidebar
2. Upload a PDF or paste some text
3. Click "Ingest Document"
4. Ask questions about your document
5. Inspect the retrieved chunks and scores

### Test Vision Capabilities
1. Click "Vision Lab" in the sidebar
2. Select a vision model (e.g., `llava`)
3. Upload an image
4. Use a quick prompt or write your own
5. See the model's analysis

### Compare Models
1. Click "Model Arena" in the sidebar
2. Select two different models
3. Enter a prompt
4. Click "Run Comparison"
5. Compare responses and performance

## Troubleshooting

### "Ollama is not running"
```bash
# Start Ollama in a terminal
ollama serve
```

### "Model not found"
```bash
# Pull the model first
ollama pull <model-name>
```

### Import errors
```bash
# Reinstall dependencies
pip install -r requirements.txt
```

## Tips

- **Performance**: Smaller models (7B-8B) are faster; larger models (70B+) are more capable
- **Temperature**: Lower (0.0-0.3) for factual tasks, higher (0.7-1.0) for creative tasks
- **RAG**: Adjust chunk size based on your document structure
- **Vision**: Use clear, high-contrast images for best OCR results

## Available Models

### Chat Models
- `granite3-dense:8b` - Balanced performance
- `llama3` - General purpose
- `mistral` - Fast and capable
- `mixtral` - High quality

### Embedding Models (for RAG)
- `nomic-embed-text` - Recommended
- `mxbai-embed-large` - Alternative
- `granite-embedding` - IBM Granite specific

### Vision Models
- `llava` - Recommended
- `bakllava` - Alternative
- `granite-vision` - IBM Granite specific

## Getting Help

- Check the main README.md for detailed documentation
- Visit the GraniteLab Specification.md for architecture details
- All features are local - your data never leaves your machine

---

**Happy experimenting! 🧪**
