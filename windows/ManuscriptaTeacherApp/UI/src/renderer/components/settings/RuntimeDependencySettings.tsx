import React, { useState, useEffect } from 'react';
import signalRService from '../../services/signalr/SignalRService';
import { DEPENDENCY_METADATA } from '../../constants/dependencies';

type InstallState = 'idle' | 'checking' | 'installing' | 'failed' | 'success';

interface DependencyStatus {
    id: string;
    isAvailable: boolean | null;
    state: InstallState;
    phase: string;
    progressPercentage: number | null;
    errorMessage: string | null;
}

const DependencyItem: React.FC<{ id: string }> = ({ id }) => {
    const [dep, setDep] = useState<DependencyStatus>({
        id,
        isAvailable: null,
        state: 'idle',
        phase: '',
        progressPercentage: null,
        errorMessage: null,
    });

    // Check availability on mount
    useEffect(() => {
        checkAvailability();
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [id]);

    // Subscribe to install progress
    useEffect(() => {
        const unsubscribe = signalRService.onRuntimeDependencyInstallProgress(
            (progressId, currentPhase, percentage, errorMsg) => {
                if (progressId !== id) return;

                if (currentPhase === 'Completed') {
                    setDep(prev => ({
                        ...prev,
                        state: 'success',
                        phase: 'Installation complete',
                        progressPercentage: 100,
                        isAvailable: true,
                        errorMessage: null
                    }));
                } else if (currentPhase === 'Failed') {
                    setDep(prev => ({
                        ...prev,
                        state: 'failed',
                        phase: '',
                        errorMessage: errorMsg || 'Installation failed.'
                    }));
                } else {
                    setDep(prev => ({
                        ...prev,
                        state: 'installing',
                        phase: currentPhase,
                        progressPercentage: percentage
                    }));
                }
            }
        );

        return () => {
            unsubscribe();
        };
    }, [id]);

    const checkAvailability = async () => {
        setDep(prev => ({ ...prev, state: 'checking', errorMessage: null, phase: '' }));
        try {
            const available = await signalRService.checkRuntimeDependencyAvailability(id);
            setDep(prev => ({
                ...prev,
                isAvailable: available,
                state: 'idle',
            }));
        } catch (e: any) {
            setDep(prev => ({
                ...prev,
                state: 'failed',
                errorMessage: e.message || 'Failed to check status.'
            }));
        }
    };

    const handleInstall = async () => {
        setDep(prev => ({
            ...prev,
            state: 'installing',
            phase: 'Starting',
            progressPercentage: null,
            errorMessage: null
        }));

        try {
            const success = await signalRService.installRuntimeDependency(id);
            if (!success) {
                setDep(prev => ({
                    ...prev,
                    state: 'failed',
                    errorMessage: 'Installation failed to start.'
                }));
            }
        } catch (e: any) {
            setDep(prev => ({
                ...prev,
                state: 'failed',
                errorMessage: e.message || 'An error occurred during installation.'
            }));
        }
    };

    return (
        <div className="flex flex-col sm:flex-row sm:items-center justify-between p-4 border border-gray-200 rounded-lg">
            <div className="mb-4 sm:mb-0">
                <h3 className="text-text-heading font-sans font-medium text-lg capitalize">{dep.id}</h3>
                <p className="text-text-body font-sans text-sm mt-1">
                    {DEPENDENCY_METADATA[dep.id]?.description || 'Runtime dependency required for external device integration.'}
                </p>
            </div>

            <div className="flex flex-col items-end shrink-0 space-y-2">
                <div className="flex items-center space-x-2">
                    {dep.isAvailable === true && (
                        <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-green-100 text-green-800">
                            Available
                        </span>
                    )}
                    {dep.isAvailable === false && (
                        <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-red-100 text-red-800">
                            Missing
                        </span>
                    )}

                    <button
                        onClick={checkAvailability}
                        disabled={dep.state === 'checking' || dep.state === 'installing'}
                        className="px-3 py-1.5 text-sm font-sans font-medium text-gray-700 bg-white border border-gray-300 rounded-md hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-brand-orange disabled:opacity-50 transition-colors"
                    >
                        {dep.state === 'checking' ? 'Checking...' : 'Check Status'}
                    </button>

                    <button
                        onClick={handleInstall}
                        disabled={dep.state === 'installing'}
                        className="px-3 py-1.5 text-sm font-sans font-medium text-white bg-brand-orange rounded-md hover:bg-brand-orange-dark focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-brand-orange disabled:opacity-50 transition-colors"
                    >
                        {dep.state === 'installing' ? 'Installing...' : dep.isAvailable ? 'Reinstall' : 'Install'}
                    </button>
                </div>

                {dep.state === 'installing' && (
                    <div className="w-full max-w-[200px] mt-2">
                        <div className="flex justify-between text-xs text-text-body font-sans mb-1">
                            <span>{dep.phase}</span>
                            {dep.progressPercentage !== null && <span>{dep.progressPercentage}%</span>}
                        </div>
                        {dep.progressPercentage !== null ? (
                            <div className="w-full bg-gray-200 rounded-full h-1.5">
                                <div
                                    className="bg-brand-orange h-1.5 rounded-full transition-all duration-300"
                                    style={{ width: `${dep.progressPercentage}%` }}
                                ></div>
                            </div>
                        ) : (
                            <div className="w-full bg-gray-200 rounded-full h-1.5 overflow-hidden">
                                <div className="bg-brand-orange h-1.5 rounded-full w-1/3 animate-pulse"></div>
                            </div>
                        )}
                    </div>
                )}

                {dep.state === 'failed' && dep.errorMessage && (
                    <div className="text-sm text-red-600 font-sans mt-2 max-w-[250px] text-right">
                        {dep.errorMessage}
                    </div>
                )}

                {dep.state === 'success' && (
                    <div className="text-sm text-green-600 font-sans mt-2 max-w-[250px] text-right">
                        Installed successfully!
                    </div>
                )}
            </div>
        </div>
    );
};

export const RuntimeDependencySettings: React.FC = () => {
    const dependencyIds = Object.keys(DEPENDENCY_METADATA);

    return (
        <div className="bg-white rounded-xl shadow-soft border border-gray-100 p-8 max-w-2xl mx-auto">
            <h2 className="text-2xl font-serif text-text-heading mb-6 border-b border-gray-100 pb-4">
                Runtime Dependencies
            </h2>

            <div className="space-y-4">
                {dependencyIds.length === 0 ? (
                    <p className="text-text-body font-sans text-sm">No runtime dependencies configured.</p>
                ) : (
                    dependencyIds.map(id => (
                        <DependencyItem key={id} id={id} />
                    ))
                )}
            </div>
        </div>
    );
};
