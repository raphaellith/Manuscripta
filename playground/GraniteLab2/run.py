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
    """Checks if Ollama is running, and starts it if not."""
    print("🔍 Checking Ollama connection...")
    try:
        import ollama
        ollama.list()
        print("✅ Ollama is running.")
        return True
    except ImportError:
        print("⚠️ Ollama library not found (will be installed).")
        return False
    except Exception:
        print("❌ Ollama is NOT running. Attempting to start it...")
        try:
            # Attempt to start Ollama in the background
            subprocess.Popen(["ollama", "serve"], stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
            print("⏳ Waiting for Ollama to start...")
            
            # Wait up to 10 seconds for it to become responsive
            for _ in range(10):
                time.sleep(1)
                try:
                    ollama.list()
                    print("✅ Ollama started successfully.")
                    return True
                except Exception:
                    pass
            
            print("❌ Failed to start Ollama automatically. Please run 'ollama serve' manually.")
            return False
        except FileNotFoundError:
            print("❌ 'ollama' command not found. Please install Ollama from https://ollama.com/")
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
