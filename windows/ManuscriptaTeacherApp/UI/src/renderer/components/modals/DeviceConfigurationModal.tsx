/**
 * Modal for viewing and modifying device configuration.
 * Per FrontendWorkflowSpecifications §5H.
 */

import React, { useState, useEffect } from 'react';
import signalRService from '../../services/signalr/SignalRService';
import type { ConfigurationEntity, FeedbackStyle, MascotSelection } from '../../models';
import { ModalOverlay } from './ModalOverlay';

interface DeviceConfigurationModalProps {
    deviceId: string;
    deviceName: string;
    onClose: () => void;
}

export const DeviceConfigurationModal: React.FC<DeviceConfigurationModalProps> = ({
    deviceId,
    deviceName,
    onClose
}) => {
    const [config, setConfig] = useState<ConfigurationEntity | null>(null);
    const [isLoading, setIsLoading] = useState(true);
    const [isSubmitting, setIsSubmitting] = useState(false);
    const [error, setError] = useState<string | null>(null);

    // Load device configuration on mount
    // Per §5H(1)(a)
    useEffect(() => {
        const loadConfiguration = async () => {
            try {
                setIsLoading(true);
                const deviceConfig = await signalRService.getDeviceConfiguration(deviceId);
                setConfig(deviceConfig);
            } catch (err) {
                console.error('Failed to load device configuration:', err);
                setError('Failed to load configuration');
            } finally {
                setIsLoading(false);
            }
        };

        loadConfiguration();
    }, [deviceId]);

    // Per §5H(1)(c): Save changes
    const handleSubmit = async () => {
        if (!config) return;

        setIsSubmitting(true);
        setError(null);
        try {
            await signalRService.updateDeviceConfiguration(deviceId, config);
            onClose();
        } catch (err) {
            console.error('Failed to update device configuration:', err);
            setError('Failed to save configuration');
        } finally {
            setIsSubmitting(false);
        }
    };

    // Per §5H(1)(b): Allow modification of any value
    const updateConfig = (field: keyof ConfigurationEntity, value: unknown) => {
        if (!config) return;
        setConfig({ ...config, [field]: value });
    };

    return (
        <ModalOverlay priority="low">
            <div className="bg-white rounded-lg p-8 shadow-2xl w-full max-w-2xl space-y-6 animate-fade-in-up border border-gray-100 max-h-[90vh] overflow-y-auto">
                <h2 className="text-2xl font-serif text-text-heading">
                    Device Configuration: {deviceName}
                </h2>

                {isLoading && (
                    <div className="flex items-center justify-center py-8">
                        <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-brand-orange"></div>
                    </div>
                )}

                {error && (
                    <div className="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded">
                        {error}
                    </div>
                )}

                {config && !isLoading && (
                    <div className="space-y-6">
                        {/* Text Size - Per Validation Rules §2G(1)(a) */}
                        <div>
                            <label className="font-sans font-medium text-text-heading text-sm mb-2 block">
                                Text Size (5-50)
                            </label>
                            <input
                                type="number"
                                min={5}
                                max={50}
                                value={config.textSize}
                                onChange={(e) => {
                                    const parsed = parseInt(e.target.value, 10);
                                    if (!Number.isNaN(parsed) && parsed >= 5 && parsed <= 50) {
                                        updateConfig('textSize', parsed);
                                    }
                                }}
                                className="w-full p-3 bg-white text-text-body font-sans rounded-lg border border-gray-200 focus:border-brand-orange focus:ring-1 focus:ring-brand-orange focus:outline-none"
                            />
                        </div>

                        {/* Feedback Style - Per Validation Rules §2G(1)(b) */}
                        <div>
                            <label className="font-sans font-medium text-text-heading text-sm mb-2 block">
                                Feedback Style
                            </label>
                            <select
                                value={config.feedbackStyle}
                                onChange={(e) => updateConfig('feedbackStyle', e.target.value as FeedbackStyle)}
                                className="w-full p-3 bg-white text-text-body font-sans rounded-lg border border-gray-200 focus:border-brand-orange focus:ring-1 focus:ring-brand-orange focus:outline-none"
                            >
                                <option value="IMMEDIATE">Immediate</option>
                                <option value="NEUTRAL">Neutral</option>
                            </select>
                            <p className="text-sm text-gray-500 mt-1">
                                {config.feedbackStyle === 'IMMEDIATE'
                                    ? 'Correct/Incorrect feedback is shown immediately'
                                    : 'Only "Response Submitted" confirmation is shown'}
                            </p>
                        </div>

                        {/* TTS Enabled - Per Validation Rules §2G(1)(c) */}
                        <div className="flex items-start gap-3">
                            <input
                                type="checkbox"
                                id="ttsEnabled"
                                checked={config.ttsEnabled}
                                onChange={(e) => updateConfig('ttsEnabled', e.target.checked)}
                                className="mt-1 h-5 w-5 text-brand-orange border-gray-300 rounded focus:ring-brand-orange"
                            />
                            <div className="flex-1">
                                <label htmlFor="ttsEnabled" className="font-sans font-medium text-text-heading text-sm block cursor-pointer">
                                    Text-to-Speech Enabled
                                </label>
                                <p className="text-sm text-gray-500 mt-1">
                                    Enable audio narration for text content
                                </p>
                            </div>
                        </div>

                        {/* AI Scaffolding Enabled - Per Validation Rules §2G(1)(d) */}
                        <div className="flex items-start gap-3">
                            <input
                                type="checkbox"
                                id="aiScaffoldingEnabled"
                                checked={config.aiScaffoldingEnabled}
                                onChange={(e) => updateConfig('aiScaffoldingEnabled', e.target.checked)}
                                className="mt-1 h-5 w-5 text-brand-orange border-gray-300 rounded focus:ring-brand-orange"
                            />
                            <div className="flex-1">
                                <label htmlFor="aiScaffoldingEnabled" className="font-sans font-medium text-text-heading text-sm block cursor-pointer">
                                    AI Scaffolding Enabled
                                </label>
                                <p className="text-sm text-gray-500 mt-1">
                                    Enable AI-powered learning assistance
                                </p>
                            </div>
                        </div>

                        {/* Summarisation Enabled - Per Validation Rules §2G(1)(e) */}
                        <div className="flex items-start gap-3">
                            <input
                                type="checkbox"
                                id="summarisationEnabled"
                                checked={config.summarisationEnabled}
                                onChange={(e) => updateConfig('summarisationEnabled', e.target.checked)}
                                className="mt-1 h-5 w-5 text-brand-orange border-gray-300 rounded focus:ring-brand-orange"
                            />
                            <div className="flex-1">
                                <label htmlFor="summarisationEnabled" className="font-sans font-medium text-text-heading text-sm block cursor-pointer">
                                    Summarisation Enabled
                                </label>
                                <p className="text-sm text-gray-500 mt-1">
                                    Enable content summarisation features
                                </p>
                            </div>
                        </div>

                        {/* Mascot Selection - Per Validation Rules §2G(1)(f) */}
                        <div>
                            <label className="font-sans font-medium text-text-heading text-sm mb-2 block">
                                Mascot Selection
                            </label>
                            <select
                                value={config.mascotSelection}
                                onChange={(e) => updateConfig('mascotSelection', e.target.value as MascotSelection)}
                                className="w-full p-3 bg-white text-text-body font-sans rounded-lg border border-gray-200 focus:border-brand-orange focus:ring-1 focus:ring-brand-orange focus:outline-none"
                            >
                                <option value="NONE">None</option>
                                <option value="MASCOT1">Mascot 1</option>
                                <option value="MASCOT2">Mascot 2</option>
                                <option value="MASCOT3">Mascot 3</option>
                                <option value="MASCOT4">Mascot 4</option>
                                <option value="MASCOT5">Mascot 5</option>
                            </select>
                        </div>
                    </div>
                )}

                {/* Per §5H(1)(c): Save button */}
                <div className="flex flex-wrap gap-4 pt-4 border-t border-gray-100">
                    <button
                        onClick={handleSubmit}
                        disabled={isSubmitting || isLoading || !config}
                        className="px-6 py-3 bg-brand-orange text-white font-sans font-medium rounded-md hover:bg-brand-orange-dark transition-colors shadow-sm disabled:opacity-50"
                    >
                        {isSubmitting ? 'Saving...' : 'Save'}
                    </button>
                    <button
                        onClick={onClose}
                        disabled={isSubmitting}
                        className="px-6 py-3 bg-white text-gray-600 border border-gray-300 font-sans font-medium rounded-md hover:border-brand-orange hover:text-brand-orange transition-colors disabled:opacity-50"
                    >
                        Cancel
                    </button>
                </div>
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
