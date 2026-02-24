/**
 * Modal for pairing an external device (reMarkable or Kindle).
 * Replaces the legacy ReMarkablePairingModal.
 */

import React, { useState } from 'react';
import type { ExternalDeviceType } from '../../models';

interface ExternalDevicePairingModalProps {
    onClose: () => void;
    onPaired: () => void;
    pairDevice: (name: string, type: ExternalDeviceType, configurationData: string) => Promise<string>;
    onDependencyMissing?: (dependencyIds: string[]) => void;
}

type PairingStep = 'type-and-name' | 'auth' | 'pairing';

export const ExternalDevicePairingModal: React.FC<ExternalDevicePairingModalProps> = ({
    onClose,
    onPaired,
    pairDevice,
    onDependencyMissing,
}) => {
    const [step, setStep] = useState<PairingStep>('type-and-name');
    const [deviceType, setDeviceType] = useState<ExternalDeviceType>('REMARKABLE');
    const [deviceName, setDeviceName] = useState('');
    const [configurationData, setConfigurationData] = useState(''); // Holds rmapi code OR kindle email
    const [error, setError] = useState<string | null>(null);

    const remarkableUrl = 'https://my.remarkable.com/device/desktop/connect';

    const handleNameSubmit = () => {
        if (!deviceName.trim()) {
            setError('Device name is required');
            return;
        }
        setError(null);

        if (deviceType === 'REMARKABLE') {
            window.open(remarkableUrl, '_blank');
        }

        setStep('auth');
    };

    const handleAuthSubmit = async () => {
        if (!configurationData.trim()) {
            setError(deviceType === 'REMARKABLE' ? 'One-time code is required' : 'Kindle email address is required');
            return;
        }

        // Basic validation for Kindle email
        if (deviceType === 'KINDLE' && !configurationData.includes('@')) {
            setError('Please enter a valid Kindle email address');
            return;
        }

        setError(null);
        setStep('pairing');

        try {
            await pairDevice(deviceName.trim(), deviceType, configurationData.trim());
            onPaired();
        } catch (err) {
            const errorMessage = err instanceof Error ? err.message : 'Pairing failed. Please try again.';

            // DEPENDENCY_MISSING is a special signal from the backend that the dependency install popup
            // should be triggered. The backend also emits a RuntimeDependencyNotInstalled event which ClassroomPage catches.
            // We just need to close this modal silently so the dependency modal can be shown.
            if (errorMessage.includes('DEPENDENCY_MISSING')) {
                onClose();
                // We optionally pass 'rmapi' up to be safe, though ClassroomPage usually gets it via SignalR broadcast
                if (onDependencyMissing) {
                    onDependencyMissing(['rmapi']);
                }
                return;
            }

            setError(errorMessage);
            setStep('auth');
        }
    };

    return (
        <div className="fixed inset-0 bg-text-heading/20 backdrop-blur-sm flex items-center justify-center z-50 p-4">
            <div className="bg-white rounded-lg p-8 shadow-2xl w-full max-w-md space-y-6 animate-fade-in-up border border-gray-100">
                <h2 className="text-2xl font-serif text-text-heading">
                    Pair External Device
                </h2>

                {/* Step indicator */}
                <div className="flex items-center gap-2 text-xs font-sans text-gray-400">
                    <span className={step === 'type-and-name' ? 'text-brand-orange font-semibold' : ''}>
                        1. Type & Name
                    </span>
                    <span>→</span>
                    <span className={step === 'auth' || step === 'pairing' ? 'text-brand-orange font-semibold' : ''}>
                        2. Configuration
                    </span>
                </div>

                {step === 'pairing' ? (
                    <div className="text-center py-4 space-y-4">
                        <div className="animate-spin w-8 h-8 border-4 border-brand-orange border-t-transparent rounded-full mx-auto" />
                        <p className="text-text-body font-sans">
                            Pairing "{deviceName}"…
                        </p>
                    </div>
                ) : step === 'type-and-name' ? (
                    <>
                        <div>
                            <label className="font-sans font-medium text-text-heading text-sm mb-2 block">
                                Device Type <span className="text-red-500">*</span>
                            </label>
                            <div className="grid grid-cols-2 gap-4 mb-4">
                                <button
                                    className={`py-3 px-4 rounded-lg border text-center font-sans font-medium transition-colors ${deviceType === 'REMARKABLE'
                                        ? 'border-brand-orange bg-orange-50 text-brand-orange'
                                        : 'border-gray-200 bg-white text-gray-600 hover:border-brand-orange hover:text-brand-orange'
                                        }`}
                                    onClick={() => { setDeviceType('REMARKABLE'); setError(null); }}
                                >
                                    reMarkable
                                </button>
                                <button
                                    className={`py-3 px-4 rounded-lg border text-center font-sans font-medium transition-colors ${deviceType === 'KINDLE'
                                        ? 'border-brand-orange bg-orange-50 text-brand-orange'
                                        : 'border-gray-200 bg-white text-gray-600 hover:border-brand-orange hover:text-brand-orange'
                                        }`}
                                    onClick={() => { setDeviceType('KINDLE'); setError(null); }}
                                >
                                    Kindle
                                </button>
                            </div>

                            <label className="font-sans font-medium text-text-heading text-sm mb-2 block">
                                Device Name <span className="text-red-500">*</span>
                            </label>
                            <input
                                type="text"
                                value={deviceName}
                                onChange={(e) => { setDeviceName(e.target.value); setError(null); }}
                                className="w-full p-3 bg-white text-text-body font-sans rounded-lg border border-gray-200 focus:border-brand-orange focus:ring-1 focus:ring-brand-orange focus:outline-none"
                                placeholder={deviceType === 'REMARKABLE' ? "e.g., My reMarkable 2" : "e.g., John's Kindle Scribe"}
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
                    /* step === 'auth' */
                    <>
                        {deviceType === 'REMARKABLE' ? (
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
                        ) : (
                            <div className="space-y-3">
                                <p className="text-text-body font-sans text-sm leading-relaxed">
                                    Enter your Send-to-Kindle email address.
                                </p>
                                <p className="text-text-body font-sans text-sm text-gray-500">
                                    You can find this on your Amazon account under "Manage Your Content and Devices" &gt; "Preferences" &gt; "Personal Document Settings". Ensure this application's sending email address is also approved there.
                                </p>
                            </div>
                        )}

                        <div>
                            <label className="font-sans font-medium text-text-heading text-sm mb-2 block">
                                {deviceType === 'REMARKABLE' ? 'One-Time Code' : 'Kindle Email Address'} <span className="text-red-500">*</span>
                            </label>
                            <input
                                type={deviceType === 'KINDLE' ? 'email' : 'text'}
                                value={configurationData}
                                onChange={(e) => { setConfigurationData(e.target.value); setError(null); }}
                                className={`w-full p-3 bg-white text-text-body font-sans rounded-lg border border-gray-200 focus:border-brand-orange focus:ring-1 focus:ring-brand-orange focus:outline-none ${deviceType === 'REMARKABLE' ? 'font-mono text-center text-lg tracking-widest uppercase' : ''}`}
                                placeholder={deviceType === 'REMARKABLE' ? "e.g., abcdefgh" : "e.g., mykindle@kindle.com"}
                                autoFocus
                            />
                            {error && <p className="text-red-500 text-sm mt-2">{error}</p>}
                        </div>

                        <div className="flex flex-wrap gap-4 pt-4 border-t border-gray-100">
                            <button
                                onClick={handleAuthSubmit}
                                className="px-6 py-3 bg-brand-orange text-white font-sans font-medium rounded-md hover:bg-brand-orange-dark transition-colors shadow-sm"
                            >
                                Pair Device
                            </button>
                            <div className="flex-grow"></div>
                            <button
                                onClick={() => setStep('type-and-name')}
                                className="px-6 py-3 bg-white text-gray-600 border border-gray-300 font-sans font-medium rounded-md hover:border-gray-400 transition-colors"
                            >
                                Back
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
