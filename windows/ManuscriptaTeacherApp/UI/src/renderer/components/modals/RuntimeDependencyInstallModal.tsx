import React, { useState, useEffect } from 'react';
import signalRService from '../../services/signalr/SignalRService';
import { ModalOverlay } from './ModalOverlay';

interface RuntimeDependencyInstallModalProps {
    dependencyIds: string[];
    onClose: () => void;
    onInstallComplete: () => void;
}

type InstallState = 'prompt' | 'installing' | 'failed' | 'manual';

const DEPENDENCY_METADATA: Record<string, {
    name: string;
    description: string;
    manualUrl: string;
    manualPathInfo: React.ReactNode;
}> = {
    'rmapi': {
        name: 'rmapi',
        description: 'The rmapi tool is required to communicate with reMarkable devices.',
        manualUrl: 'https://github.com/ddvk/rmapi/releases',
        manualPathInfo: (
            <>
                Download <span className="font-mono">rmapi</span> from GitHub and place it at{' '}
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

/**
 * Modal component for handling the installation of runtime dependencies.
 * Per FrontendWorkflowSpecifications §3A(2).
 */
export const RuntimeDependencyInstallModal: React.FC<RuntimeDependencyInstallModalProps> = ({
    dependencyIds,
    onClose,
    onInstallComplete,
}) => {
    const [state, setState] = useState<InstallState>('prompt');
    const [phase, setPhase] = useState<string>('');
    const [progressPercentage, setProgressPercentage] = useState<number | null>(null);
    const [errorMessage, setErrorMessage] = useState<string | null>(null);
    const [currentIndex, setCurrentIndex] = useState(0);

    const dependencyId = dependencyIds[currentIndex];

    const meta = DEPENDENCY_METADATA[dependencyId] || {
        name: dependencyId,
        description: `The ${dependencyId} dependency is required to continue.`,
        manualUrl: '',
        manualPathInfo: `Please install ${dependencyId} manually and try again.`
    };

    const handleAutoInstallForId = async (id: string) => {
        setState('installing');
        setPhase('Starting');
        setProgressPercentage(null);
        setErrorMessage(null);

        try {
            const success = await signalRService.installRuntimeDependency(id);
            if (!success) {
                setState('failed');
                setErrorMessage('Installation failed to start.');
            }
        } catch (e: any) {
            setState('failed');
            setErrorMessage(e.message || 'An error occurred during installation.');
        }
    };

    useEffect(() => {
        if (!dependencyId) return;

        const unsubscribe = signalRService.onRuntimeDependencyInstallProgress(
            (id, currentPhase, percentage, errorMsg) => {
                if (id !== dependencyId) return;

                if (currentPhase === 'Completed') {
                    if (currentIndex < dependencyIds.length - 1) {
                        const nextId = dependencyIds[currentIndex + 1];
                        setCurrentIndex(prev => prev + 1);
                        handleAutoInstallForId(nextId);
                    } else {
                        onInstallComplete();
                    }
                } else if (currentPhase === 'Failed') {
                    setState('failed');
                    setErrorMessage(errorMsg || 'Installation failed.');
                } else {
                    setState('installing');
                    setPhase(currentPhase);
                    setProgressPercentage(percentage);
                }
            }
        );

        return () => {
            unsubscribe();
        };
    }, [dependencyId, currentIndex, dependencyIds, onInstallComplete]);

    const handleAutoInstall = () => {
        setCurrentIndex(0);
        handleAutoInstallForId(dependencyIds[0]);
    };

    const handleManualInstall = () => {
        // Per FrontendWorkflowSpecifications §3A(3)(a): open browser to download page
        if (meta.manualUrl) {
            window.open(meta.manualUrl, '_blank');
        }
        // Per FrontendWorkflowSpecifications §3A(3)(b): display instructions (handled by state change)
        setState('manual');
    };

    // Per FrontendWorkflowSpecifications §3A(3)(c): provide re-check button
    const handleRecheck = async () => {
        setState('installing');
        setPhase('Checking availability');
        setProgressPercentage(null);
        setErrorMessage(null);

        try {
            const available = await signalRService.checkRuntimeDependencyAvailability(dependencyId);
            if (available) {
                // Dependency is now available, proceed to next or complete
                if (currentIndex < dependencyIds.length - 1) {
                    setCurrentIndex(prev => prev + 1);
                    setState('prompt');
                } else {
                    onInstallComplete();
                }
            } else {
                setState('manual');
                setErrorMessage('Dependency is still not available. Please ensure it is correctly installed.');
            }
        } catch (e: any) {
            setState('manual');
            setErrorMessage(e.message || 'Failed to check dependency availability.');
        }
    };

    return (
        <ModalOverlay priority="high">
            <div className="bg-white rounded-lg p-8 shadow-2xl w-full max-w-md space-y-6 animate-fade-in-up border border-gray-100">
                <h2 className="text-2xl font-serif text-text-heading">
                    Missing Dependencies Required
                </h2>

                {state === 'installing' ? (
                    <div className="text-center py-4 space-y-4">
                        <div className="relative pt-1">
                            {progressPercentage !== null ? (
                                <>
                                    <div className="overflow-hidden h-2 mb-4 text-xs flex rounded bg-brand-orange/20">
                                        <div style={{ width: `${progressPercentage}%` }} className="shadow-none flex flex-col text-center whitespace-nowrap text-white justify-center bg-brand-orange transition-all duration-500 ease-in-out"></div>
                                    </div>
                                    <p className="text-text-body font-sans text-sm font-medium">{phase}… {progressPercentage}%</p>
                                </>
                            ) : (
                                <>
                                    <div className="animate-spin w-8 h-8 border-4 border-brand-orange border-t-transparent rounded-full mx-auto mb-4" />
                                    <p className="text-text-body font-sans text-sm font-medium">{phase}…</p>
                                </>
                            )}
                        </div>
                    </div>
                ) : state === 'manual' ? (
                    /* Per FrontendWorkflowSpecifications §3A(3)(b) and (c): Display instructions and re-check button */
                    <>
                        <div className="bg-blue-50 border border-blue-200 text-blue-800 px-4 py-3 rounded mb-4">
                            <p className="font-medium mb-2">Manual Installation Required</p>
                            <p className="text-sm">{meta.manualPathInfo}</p>
                        </div>

                        {errorMessage && (
                            <div className="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded mb-4 text-sm">
                                {errorMessage}
                            </div>
                        )}

                        <p className="text-text-body font-sans text-sm leading-relaxed mb-4">
                            After installing {meta.name} manually, click the button below to verify the installation.
                        </p>

                        <div className="flex flex-col gap-3 pt-4 border-t border-gray-100">
                            <button
                                onClick={handleRecheck}
                                className="w-full px-6 py-3 bg-brand-orange text-white font-sans font-medium rounded-md hover:bg-brand-orange-dark transition-colors shadow-sm"
                            >
                                Re-check Availability
                            </button>
                            <button
                                onClick={onClose}
                                className="w-full px-6 py-3 bg-white text-gray-600 border border-gray-300 font-sans font-medium rounded-md hover:border-brand-orange hover:text-brand-orange transition-colors"
                            >
                                Cancel
                            </button>
                        </div>
                    </>
                ) : (
                    <>
                        <p className="text-text-body font-sans text-sm leading-relaxed">
                            {state === 'failed'
                                ? errorMessage || 'Automatic installation failed. You can try installing manually or cancel.'
                                : dependencyIds.length === 1
                                    ? meta.description + ' How would you like to proceed?'
                                    : `The following dependencies are required to continue: ${dependencyIds.map(id => DEPENDENCY_METADATA[id]?.name ?? id).join(', ')}. How would you like to proceed?`}
                        </p>

                        <div className="flex flex-col gap-3 pt-4 border-t border-gray-100">
                            {state === 'prompt' && (
                                <button
                                    onClick={handleAutoInstall}
                                    className="w-full px-6 py-3 bg-brand-orange text-white font-sans font-medium rounded-md hover:bg-brand-orange-dark transition-colors shadow-sm"
                                >
                                    Install Automatically
                                </button>
                            )}
                            <button
                                onClick={handleManualInstall}
                                className="w-full px-6 py-3 bg-white text-text-heading border border-gray-300 font-sans font-medium rounded-md hover:border-brand-orange hover:text-brand-orange transition-colors"
                            >
                                Install Manually
                            </button>
                            {meta.manualPathInfo && (
                                <p className="text-xs text-gray-400 font-sans px-1">
                                    {meta.manualPathInfo}
                                </p>
                            )}
                            <button
                                onClick={onClose}
                                className="w-full px-6 py-3 bg-white text-gray-600 border border-gray-300 font-sans font-medium rounded-md hover:border-brand-orange hover:text-brand-orange transition-colors"
                            >
                                Cancel
                            </button>
                        </div>
                    </>
                )}
            </div>
            <style>{`
                @keyframes fade-in-up {
                    from { opacity: 0; transform: translateY(20px); }
                    to { opacity: 1; transform: translateY(0); }
                }
                .animate-fade-in-up { animation: fade-in-up 0.3s ease-out forwards; }
            `}</style>
        </ModalOverlay>
    );
};
