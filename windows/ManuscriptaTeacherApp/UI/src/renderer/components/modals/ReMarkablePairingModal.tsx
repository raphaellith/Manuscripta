/**
 * Modal for pairing a reMarkable device.
 * Per Frontend Workflow Spec §5F(2).
 * Multi-step: name → open browser → enter code → submit.
 */

import React, { useState } from 'react';

interface ReMarkablePairingModalProps {
    onClose: () => void;
    onPaired: () => void;
    pairDevice: (name: string, oneTimeCode: string) => Promise<string>;
}

type PairingStep = 'name' | 'code' | 'pairing';

export const ReMarkablePairingModal: React.FC<ReMarkablePairingModalProps> = ({
    onClose,
    onPaired,
    pairDevice,
}) => {
    const [step, setStep] = useState<PairingStep>('name');
    const [deviceName, setDeviceName] = useState('');
    const [oneTimeCode, setOneTimeCode] = useState('');
    const [error, setError] = useState<string | null>(null);

    const remarkableUrl = 'https://my.remarkable.com/device/desktop/connect';

    const handleNameSubmit = () => {
        if (!deviceName.trim()) {
            setError('Device name is required');
            return;
        }
        setError(null);

        // §5F(2)(b): open browser to reMarkable connect page
        window.open(remarkableUrl, '_blank');
        setStep('code');
    };

    const handleCodeSubmit = async () => {
        if (!oneTimeCode.trim()) {
            setError('One-time code is required');
            return;
        }
        setError(null);
        setStep('pairing');

        try {
            await pairDevice(deviceName.trim(), oneTimeCode.trim());
            onPaired();
        } catch (err) {
            setError(err instanceof Error ? err.message : 'Pairing failed. Please try again.');
            setStep('code');
        }
    };

    return (
        <div className="fixed inset-0 bg-text-heading/20 backdrop-blur-sm flex items-center justify-center z-[999] p-4">
            <div className="bg-white rounded-lg p-8 shadow-2xl w-full max-w-md space-y-6 animate-fade-in-up border border-gray-100">
                <h2 className="text-2xl font-serif text-text-heading">
                    Pair reMarkable Device
                </h2>

                {/* Step indicator */}
                <div className="flex items-center gap-2 text-xs font-sans text-gray-400">
                    <span className={step === 'name' ? 'text-brand-orange font-semibold' : ''}>
                        1. Name
                    </span>
                    <span>→</span>
                    <span className={step === 'code' || step === 'pairing' ? 'text-brand-orange font-semibold' : ''}>
                        2. Authenticate
                    </span>
                </div>

                {step === 'pairing' ? (
                    <div className="text-center py-4 space-y-4">
                        <div className="animate-spin w-8 h-8 border-4 border-brand-orange border-t-transparent rounded-full mx-auto" />
                        <p className="text-text-body font-sans">
                            Pairing "{deviceName}"…
                        </p>
                    </div>
                ) : step === 'name' ? (
                    <>
                        <div>
                            <label className="font-sans font-medium text-text-heading text-sm mb-2 block">
                                Device Name <span className="text-red-500">*</span>
                            </label>
                            <input
                                type="text"
                                value={deviceName}
                                onChange={(e) => { setDeviceName(e.target.value); setError(null); }}
                                className="w-full p-3 bg-white text-text-body font-sans rounded-lg border border-gray-200 focus:border-brand-orange focus:ring-1 focus:ring-brand-orange focus:outline-none"
                                placeholder="e.g., My reMarkable 2"
                                autoFocus
                            />
                            {error && <p className="text-red-500 text-sm mt-2">{error}</p>}
                        </div>

                        <div className="flex flex-wrap gap-4 pt-4 border-t border-gray-100">
                            <button
                                onClick={handleNameSubmit}
                                className="px-6 py-3 bg-brand-orange text-white font-sans font-medium rounded-md hover:bg-brand-orange-dark transition-colors shadow-sm"
                            >
                                Next
                            </button>
                            <button
                                onClick={onClose}
                                className="px-6 py-3 bg-white text-gray-600 border border-gray-300 font-sans font-medium rounded-md hover:border-brand-orange hover:text-brand-orange transition-colors"
                            >
                                Cancel
                            </button>
                        </div>
                    </>
                ) : (
                    /* step === 'code' */
                    <>
                        <div className="space-y-3">
                            <p className="text-text-body font-sans text-sm leading-relaxed">
                                A browser window should have opened. If it didn't, visit:
                            </p>
                            <a
                                href={remarkableUrl}
                                target="_blank"
                                rel="noopener noreferrer"
                                className="text-brand-orange hover:underline text-sm font-mono break-all block"
                            >
                                {remarkableUrl}
                            </a>
                            <p className="text-text-body font-sans text-sm">
                                Enter the one-time code shown on the page below:
                            </p>
                        </div>

                        <div>
                            <label className="font-sans font-medium text-text-heading text-sm mb-2 block">
                                One-Time Code <span className="text-red-500">*</span>
                            </label>
                            <input
                                type="text"
                                value={oneTimeCode}
                                onChange={(e) => { setOneTimeCode(e.target.value); setError(null); }}
                                className="w-full p-3 bg-white text-text-body font-sans font-mono text-center text-lg tracking-widest rounded-lg border border-gray-200 focus:border-brand-orange focus:ring-1 focus:ring-brand-orange focus:outline-none uppercase"
                                placeholder="e.g., abcdefgh"
                                autoFocus
                            />
                            {error && <p className="text-red-500 text-sm mt-2">{error}</p>}
                        </div>

                        <div className="flex flex-wrap gap-4 pt-4 border-t border-gray-100">
                            <button
                                onClick={handleCodeSubmit}
                                className="px-6 py-3 bg-brand-orange text-white font-sans font-medium rounded-md hover:bg-brand-orange-dark transition-colors shadow-sm"
                            >
                                Pair Device
                            </button>
                            <button
                                onClick={onClose}
                                className="px-6 py-3 bg-white text-gray-600 border border-gray-300 font-sans font-medium rounded-md hover:border-brand-orange hover:text-brand-orange transition-colors"
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
