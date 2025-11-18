# 🧪 Granite Lab

**A local-first testing environment for Large Language Models via Ollama**

Granite Lab is a modular, zero-setup Python tool for benchmarking performance, inspecting RAG internals, testing multimodal capabilities (OCR/Vision), and comparing models side-by-side.

---

## ✨ Features

### 💬 Chat Lounge
Standard chat interaction with deep telemetry:
- Time to First Token (TTFT) measurement
- Tokens per second (TPS) tracking
- Configurable temperature & top-k parameters
- Full chat history with metrics

### 🔍 RAG Inspector
Debug retrieval quality with full transparency:
- PDF/TXT document ingestion
- ChromaDB ephemeral vector store (no persistent data)
- View retrieved chunks with similarity scores
- Customizable chunking strategy

### 👁️ Vision Lab
Test multimodal capabilities:
- Image upload & preview
- OCR and transcription
- Support for vision models (llava, granite-vision)
- Multiple prompt templates

### ⚔️ Model Arena
A/B testing side-by-side:
- Compare two models simultaneously
- Shared prompt input
- Real-time performance comparison
- Detailed metrics breakdown

---

## 🚀 Quick Start

### Prerequisites

1. **Python 3.10+**
2. **Ollama** - Install from [ollama.ai](https://ollama.ai)

### Installation

1. **Start Ollama:**
   ```bash
   ollama serve
   ```

2. **Pull some models:**
   ```bash
   # General chat models
   ollama pull granite3-dense:8b
   ollama pull llama3
   
   # Embedding model (for RAG)
   ollama pull nomic-embed-text
   
   # Vision model (optional)
   ollama pull llava
   ```

3. **Install dependencies:**
   ```bash
   cd playground/granite_lab
   pip install -r requirements.txt
   ```

4. **Launch Granite Lab:**
   ```bash
   streamlit run main.py
   ```

5. **Open your browser:**
   The app will automatically open at `http://localhost:8501`

---

## 📖 Usage Guide

### Chat Lounge
1. Select your model from the sidebar
2. Adjust temperature and other parameters
3. Start chatting!
4. View telemetry metrics for each response

### RAG Inspector
1. Upload a PDF or paste text
2. Wait for document ingestion
3. Ask questions about your documents
4. Inspect retrieved chunks and their relevance scores

### Vision Lab
1. Select a vision-capable model (e.g., `llava`)
2. Upload an image (PNG/JPG)
3. Enter a prompt or use a quick template
4. View the model's analysis

### Model Arena
1. Select two different models
2. Enter a shared prompt
3. Click "Run Comparison"
4. Compare responses and performance side-by-side

---

## 🏗️ Architecture

```
granite_lab/
├── main.py              # Landing page
├── requirements.txt     # Python dependencies
├── core/
│   ├── client.py       # LLM interaction & telemetry
│   ├── rag.py          # Vector store & retrieval
│   └── utils.py        # Helper functions
└── pages/
    ├── 01_Chat_Lounge.py
    ├── 02_RAG_Inspector.py
    ├── 03_Vision_Lab.py
    └── 04_Model_Arena.py
```

### Tech Stack
- **Frontend:** Streamlit
- **LLM Framework:** LangChain
- **Vector Database:** ChromaDB (ephemeral)
- **LLM Backend:** Ollama

---

## 🔧 Configuration

### Model Selection
All pages allow you to select from available Ollama models. If a model is not listed:
```bash
ollama pull <model-name>
```

### RAG Configuration
In the RAG Inspector sidebar:
- **Chunk Size:** Size of text segments (default: 1000)
- **Chunk Overlap:** Overlap between chunks (default: 200)
- **Retrieval K:** Number of chunks to retrieve (default: 3)

### Chat Parameters
- **Temperature:** Controls randomness (0.0 = deterministic, 1.0 = creative)
- **Top K:** Number of highest probability tokens to consider

---

## 📊 Telemetry Metrics

Granite Lab captures detailed performance metrics:

- **TTFT (Time to First Token):** Latency before response begins
- **TPS (Tokens Per Second):** Generation speed
- **Total Duration:** End-to-end response time
- **Token Counts:** Input and output tokens

These metrics help you understand model performance and make informed comparisons.

---

## 🎯 Best Practices

### For RAG Testing
- Use clear, well-structured documents
- Experiment with different chunk sizes for your use case
- Check similarity scores to ensure relevant retrieval

### For Vision Models
- Use high-resolution images for better OCR
- Try different prompt formulations
- Some models work better for specific tasks (text vs. scene understanding)

### For Model Comparison
- Use the same prompt across different models
- Run multiple times to assess consistency
- Consider both speed and quality in your evaluation

---

## 🐛 Troubleshooting

### "Ollama is not running"
Start Ollama in a terminal:
```bash
ollama serve
```

### "Model not found"
Pull the model first:
```bash
ollama pull <model-name>
```

### Vision models not working
Ensure you're using a multimodal model:
```bash
ollama pull llava
# or
ollama pull granite-vision
```

### Embedding model errors (RAG)
Pull an embedding model:
```bash
ollama pull nomic-embed-text
```

---

## 🔒 Privacy & Data

- **100% Local:** All processing happens on your machine
- **Ephemeral Storage:** Vector stores use in-memory storage
- **No Telemetry:** No data sent to external services
- **Session-Based:** Data cleared when you close the app

---

## 🤝 Contributing

This is a playground project for testing and experimentation. Feel free to:
- Add new pages for additional experiments
- Enhance existing features
- Optimize performance metrics
- Improve error handling

---

## 📝 License

See the main project LICENSE file.

---

## 🙏 Acknowledgments

- Built for the Manuscripta project
- Optimized for IBM Granite models
- Powered by Ollama, LangChain, and Streamlit

---

**Happy experimenting! 🧪✨**
