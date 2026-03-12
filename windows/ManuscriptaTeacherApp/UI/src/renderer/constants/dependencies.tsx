import React from 'react';

export interface RuntimeDependencyMetadata {
    name: string;
    description: string;
    manualUrl: string;
    manualPathInfo: React.ReactNode;
}

export const DEPENDENCY_METADATA: Record<string, RuntimeDependencyMetadata> = {
    'rmapi': {
        name: 'rmapi',
        description: 'Required to communicate with reMarkable devices through the reMarkable cloud.',
        manualUrl: 'https://github.com/ddvk/rmapi/releases',
        manualPathInfo: (
            <>
                Download <span className="font-mono">rmapi</span> from GitHub, rename the binary to <span className="font-mono">rmapi.exe</span>, and place it at{' '}
                <span className="font-mono">%AppData%\ManuscriptaTeacherApp\bin\rmapi.exe</span>
            </>
        )
    },
    'ollama': {
        name: 'Ollama',
        description: 'Ollama is required for AI-powered material generation and feedback. It provides local large language models.',
        manualUrl: 'https://ollama.ai/download',
        manualPathInfo: (
            <>
                Download Ollama from the official website and install it to your system.
            </>
        )
    },
    'chroma': {
        name: 'ChromaDB',
        description: 'ChromaDB is required for semantic search and retrieval-augmented generation in AI features.',
        manualUrl: 'https://docs.trychroma.com/docs/overview/getting-started',
        manualPathInfo: (
            <>
                Follow the ChromaDB installation guide to install it on your system.
            </>
        )
    },
    'qwen3:8b': {
        name: 'Qwen3 8B Model',
        description: 'The Qwen3 8B language model is required for AI-powered material generation and feedback.',
        manualUrl: 'https://ollama.ai/library/qwen3',
        manualPathInfo: (
            <>
                Ensure Ollama is installed, then run{' '}
                <span className="font-mono">ollama pull qwen3:8b</span> in a terminal.
            </>
        )
    },
    'granite4': {
        name: 'IBM Granite 4.0 Model',
        description: 'The IBM Granite 4.0 language model serves as a fallback for AI-powered features.',
        manualUrl: 'https://ollama.ai/library/granite4',
        manualPathInfo: (
            <>
                Ensure Ollama is installed, then run{' '}
                <span className="font-mono">ollama pull granite4</span> in a terminal.
            </>
        )
    },
    'nomic-embed-text': {
        name: 'Nomic Embed Text Model',
        description: 'The Nomic Embed Text model is required for semantic search and document embedding.',
        manualUrl: 'https://ollama.ai/library/nomic-embed-text',
        manualPathInfo: (
            <>
                Ensure Ollama is installed, then run{' '}
                <span className="font-mono">ollama pull nomic-embed-text</span> in a terminal.
            </>
        )
    }
};
