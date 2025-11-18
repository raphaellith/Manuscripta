import subprocess
import sys
import os
import time

def install_dependencies():
    """Installs dependencies from requirements.txt."""
    print("📦 Checking dependencies...")
    try:
        subprocess.check_call([sys.executable, "-m", "pip", "install", "-r", "requirements.txt"])
        print("✅ Dependencies installed.")
    except subprocess.CalledProcessError as e:
        print(f"❌ Failed to install dependencies: {e}")
        sys.exit(1)

def check_ollama():
    """Checks if Ollama is running."""
    print("🔍 Checking Ollama connection...")
    try:
        # We can try to import ollama here since dependencies should be installed
        import ollama
        ollama.list()
        print("✅ Ollama is running.")
        return True
    except ImportError:
        print("⚠️ Ollama library not found (will be installed).")
        return False # Should be installed in next step if not present, but logic flow handles install first
    except Exception:
        print("❌ Ollama is NOT running. Please start Ollama app first.")
        return False

def run_streamlit():
    """Launches the Streamlit app."""
    print("🚀 Launching GraniteLab...")
    app_path = os.path.join(os.path.dirname(__file__), "app.py")
    try:
        subprocess.run([sys.executable, "-m", "streamlit", "run", app_path], check=True)
    except KeyboardInterrupt:
        print("\n👋 GraniteLab stopped.")

def main():
    if not os.path.exists("requirements.txt"):
        print("❌ requirements.txt not found!")
        sys.exit(1)

    install_dependencies()
    
    # Check Ollama after dependencies are installed
    if not check_ollama():
        print("⚠️ Warning: Ollama check failed. You can still run the app, but models won't load.")
        # We don't exit here, we let the app handle the UI for disconnected state as per requirements
    
    run_streamlit()

if __name__ == "__main__":
    main()
