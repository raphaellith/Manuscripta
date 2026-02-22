/**
 * Modal for installing rmapi.
 * Per Frontend Workflow Spec §5E(2).
 */

import React, { useState } from 'react';

interface RmapiInstallModalProps {
    onClose: () => void;
    onInstallComplete: () => void;
    installRmapi: () => Promise<boolean>;
}

type InstallState = 'prompt' | 'installing' | 'failed';

export const RmapiInstallModal: React.FC<RmapiInstallModalProps> = ({
    onClose,
    onInstallComplete,
    installRmapi,
}) => {
    const [state, setState] = useState<InstallState>('prompt');

    const handleAutoInstall = async () => {
        setState('installing');
        try {
            const success = await installRmapi();
            if (success) {
                onInstallComplete();
            } else {
                setState('failed');
            }
        } catch {
            setState('failed');
        }
    };

    const handleManualInstall = () => {
        window.open('https://github.com/ddvk/rmapi/releases', '_blank');
    };

    return (
        <div className="fixed inset-0 bg-text-heading/20 backdrop-blur-sm flex items-center justify-center z-50 p-4">
            <div className="bg-white rounded-lg p-8 shadow-2xl w-full max-w-md space-y-6 animate-fade-in-up border border-gray-100">
                <h2 className="text-2xl font-serif text-text-heading">
                    rmapi Required
                </h2>

                {state === 'installing' ? (
                    <div className="text-center py-4 space-y-4">
                        <div className="animate-spin w-8 h-8 border-4 border-brand-orange border-t-transparent rounded-full mx-auto" />
                        <p className="text-text-body font-sans">Installing rmapi…</p>
                    </div>
                ) : (
                    <>
                        <p className="text-text-body font-sans text-sm leading-relaxed">
                            {state === 'failed'
                                ? 'Automatic installation failed. You can try installing manually or cancel.'
                                : 'The rmapi tool is required to communicate with reMarkable devices. How would you like to proceed?'}
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
                            <p className="text-xs text-gray-400 font-sans px-1">
                                Download <span className="font-mono">rmapi</span> from GitHub and place it at{' '}
                                <span className="font-mono">%AppData%\ManuscriptaTeacherApp\bin\rmapi.exe</span>
                            </p>
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
