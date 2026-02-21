import React, { useState, useEffect } from 'react';
import signalRService from '../../services/signalr/SignalRService';

interface RuntimeDependencyInstallModalProps {
    dependencyId: string;
    onClose: () => void;
    onInstallComplete: () => void;
}

type InstallState = 'prompt' | 'installing' | 'failed';

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
    }
};

/**
 * Modal component for handling the installation of runtime dependencies.
 * Per FrontendWorkflowSpecifications §3A(2).
 */
export const RuntimeDependencyInstallModal: React.FC<RuntimeDependencyInstallModalProps> = ({
    dependencyId,
    onClose,
    onInstallComplete,
}) => {
    const [state, setState] = useState<InstallState>('prompt');
    const [phase, setPhase] = useState<string>('');
    const [progressPercentage, setProgressPercentage] = useState<number | null>(null);
    const [errorMessage, setErrorMessage] = useState<string | null>(null);

    const meta = DEPENDENCY_METADATA[dependencyId] || {
        name: dependencyId,
        description: `The ${dependencyId} dependency is required to continue.`,
        manualUrl: '',
        manualPathInfo: `Please install ${dependencyId} manually and try again.`
    };

    useEffect(() => {
        const unsubscribe = signalRService.onRuntimeDependencyInstallProgress(
            (id, currentPhase, percentage, errorMsg) => {
                if (id !== dependencyId) return;

                if (currentPhase === 'Completed') {
                    onInstallComplete();
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
    }, [dependencyId, onInstallComplete]);

    const handleAutoInstall = async () => {
        setState('installing');
        setPhase('Starting');
        setProgressPercentage(null);
        setErrorMessage(null);

        try {
            const success = await signalRService.installRuntimeDependency(dependencyId);
            // If the hub method returns false synchronously (which shouldn't happen much since progress handles it)
            if (!success) {
                setState('failed');
                setErrorMessage('Installation failed to start.');
            }
        } catch (e: any) {
            setState('failed');
            setErrorMessage(e.message || 'An error occurred during installation.');
        }
    };

    const handleManualInstall = () => {
        if (meta.manualUrl) {
            window.open(meta.manualUrl, '_blank');
        }
    };

    return (
        <div className="fixed inset-0 bg-text-heading/20 backdrop-blur-sm flex items-center justify-center z-50 p-4">
            <div className="bg-white rounded-lg p-8 shadow-2xl w-full max-w-md space-y-6 animate-fade-in-up border border-gray-100">
                <h2 className="text-2xl font-serif text-text-heading">
                    {meta.name} Required
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
                ) : (
                    <>
                        <p className="text-text-body font-sans text-sm leading-relaxed">
                            {state === 'failed'
                                ? errorMessage || 'Automatic installation failed. You can try installing manually or cancel.'
                                : meta.description + ' How would you like to proceed?'}
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
        </div>
    );
};
