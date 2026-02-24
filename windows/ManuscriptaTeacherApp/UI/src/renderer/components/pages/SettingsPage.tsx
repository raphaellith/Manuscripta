/**
 * Settings Page component.
 * Per FrontendWorkflowSpecifications §7.
 */

import React, { useState, useEffect } from 'react';
import { Card } from '../common/Card';
import signalRService from '../../services/signalr/SignalRService';
import type { ConfigurationEntity, FeedbackStyle, MascotSelection } from '../../models';

export const SettingsPage: React.FC = () => {
    const [baseConfig, setBaseConfig] = useState<ConfigurationEntity | null>(null);
    const [isLoading, setIsLoading] = useState(true);
    const [isSaving, setIsSaving] = useState(false);
    const [error, setError] = useState<string | null>(null);
    const [successMessage, setSuccessMessage] = useState<string | null>(null);

    // Per §7(1): On entry, call GetBaseConfiguration()
    useEffect(() => {
        const loadBaseConfiguration = async () => {
            try {
                setIsLoading(true);
                const config = await signalRService.getBaseConfiguration();
                setBaseConfig(config);
            } catch (err) {
                console.error('Failed to load base configuration:', err);
                setError('Failed to load configuration');
            } finally {
                setIsLoading(false);
            }
        };

        loadBaseConfiguration();
    }, []);

    // Per §7(2): Provide means to modify any value
    const updateConfig = (field: keyof ConfigurationEntity, value: unknown) => {
        if (!baseConfig) return;
        setBaseConfig({ ...baseConfig, [field]: value });
        setSuccessMessage(null); // Clear success message when editing
    };

    const handleSave = async () => {
        if (!baseConfig) return;

        setIsSaving(true);
        setError(null);
        setSuccessMessage(null);

        try {
            await signalRService.updateBaseConfiguration(baseConfig);
            setSuccessMessage('Base configuration saved successfully');
        } catch (err) {
            console.error('Failed to save base configuration:', err);
            setError('Failed to save configuration');
        } finally {
            setIsSaving(false);
        }
    };

    if (isLoading) {
        return (
            <div className="flex items-center justify-center h-64">
                <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-brand-orange"></div>
            </div>
        );
    }

    return (
        <div className="space-y-8">
            <Card>
                <div className="border-b border-gray-100 pb-4 mb-6">
                    <h2 className="text-2xl font-serif text-text-heading">Base Device Configuration</h2>
                    <p className="text-sm text-gray-500 mt-2">
                        These settings serve as the default configuration for all devices. Individual device configurations can override these defaults.
                    </p>
                </div>

                {error && (
                    <div className="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded mb-6">
                        {error}
                    </div>
                )}

                {successMessage && (
                    <div className="bg-green-50 border border-green-200 text-green-700 px-4 py-3 rounded mb-6">
                        {successMessage}
                    </div>
                )}

                {baseConfig && (
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
                                value={baseConfig.textSize}
                                onChange={(e) => updateConfig('textSize', parseInt(e.target.value, 10))}
                                className="w-full max-w-xs p-3 bg-white text-text-body font-sans rounded-lg border border-gray-200 focus:border-brand-orange focus:ring-1 focus:ring-brand-orange focus:outline-none"
                            />
                            <p className="text-sm text-gray-500 mt-1">
                                Default text size for reading materials on student devices
                            </p>
                        </div>

                        {/* Feedback Style - Per Validation Rules §2G(1)(b) */}
                        <div>
                            <label className="font-sans font-medium text-text-heading text-sm mb-2 block">
                                Feedback Style
                            </label>
                            <select
                                value={baseConfig.feedbackStyle}
                                onChange={(e) => updateConfig('feedbackStyle', e.target.value as FeedbackStyle)}
                                className="w-full max-w-xs p-3 bg-white text-text-body font-sans rounded-lg border border-gray-200 focus:border-brand-orange focus:ring-1 focus:ring-brand-orange focus:outline-none"
                            >
                                <option value="IMMEDIATE">Immediate</option>
                                <option value="NEUTRAL">Neutral</option>
                            </select>
                            <p className="text-sm text-gray-500 mt-1">
                                {baseConfig.feedbackStyle === 'IMMEDIATE'
                                    ? 'Correct/Incorrect feedback is shown immediately'
                                    : 'Only "Response Submitted" confirmation is shown'}
                            </p>
                        </div>

                        {/* TTS Enabled - Per Validation Rules §2G(1)(c) */}
                        <div className="flex items-start gap-3">
                            <input
                                type="checkbox"
                                id="baseTtsEnabled"
                                checked={baseConfig.ttsEnabled}
                                onChange={(e) => updateConfig('ttsEnabled', e.target.checked)}
                                className="mt-1 h-5 w-5 text-brand-orange border-gray-300 rounded focus:ring-brand-orange"
                            />
                            <div className="flex-1">
                                <label htmlFor="baseTtsEnabled" className="font-sans font-medium text-text-heading text-sm block cursor-pointer">
                                    Text-to-Speech Enabled
                                </label>
                                <p className="text-sm text-gray-500 mt-1">
                                    Enable audio narration for text content by default
                                </p>
                            </div>
                        </div>

                        {/* AI Scaffolding Enabled - Per Validation Rules §2G(1)(d) */}
                        <div className="flex items-start gap-3">
                            <input
                                type="checkbox"
                                id="baseAiScaffoldingEnabled"
                                checked={baseConfig.aiScaffoldingEnabled}
                                onChange={(e) => updateConfig('aiScaffoldingEnabled', e.target.checked)}
                                className="mt-1 h-5 w-5 text-brand-orange border-gray-300 rounded focus:ring-brand-orange"
                            />
                            <div className="flex-1">
                                <label htmlFor="baseAiScaffoldingEnabled" className="font-sans font-medium text-text-heading text-sm block cursor-pointer">
                                    AI Scaffolding Enabled
                                </label>
                                <p className="text-sm text-gray-500 mt-1">
                                    Enable AI-powered learning assistance by default
                                </p>
                            </div>
                        </div>

                        {/* Summarisation Enabled - Per Validation Rules §2G(1)(e) */}
                        <div className="flex items-start gap-3">
                            <input
                                type="checkbox"
                                id="baseSummarisationEnabled"
                                checked={baseConfig.summarisationEnabled}
                                onChange={(e) => updateConfig('summarisationEnabled', e.target.checked)}
                                className="mt-1 h-5 w-5 text-brand-orange border-gray-300 rounded focus:ring-brand-orange"
                            />
                            <div className="flex-1">
                                <label htmlFor="baseSummarisationEnabled" className="font-sans font-medium text-text-heading text-sm block cursor-pointer">
                                    Summarisation Enabled
                                </label>
                                <p className="text-sm text-gray-500 mt-1">
                                    Enable content summarisation features by default
                                </p>
                            </div>
                        </div>

                        {/* Mascot Selection - Per Validation Rules §2G(1)(f) */}
                        <div>
                            <label className="font-sans font-medium text-text-heading text-sm mb-2 block">
                                Mascot Selection
                            </label>
                            <select
                                value={baseConfig.mascotSelection}
                                onChange={(e) => updateConfig('mascotSelection', e.target.value as MascotSelection)}
                                className="w-full max-w-xs p-3 bg-white text-text-body font-sans rounded-lg border border-gray-200 focus:border-brand-orange focus:ring-1 focus:ring-brand-orange focus:outline-none"
                            >
                                <option value="NONE">None</option>
                                <option value="MASCOT1">Mascot 1</option>
                                <option value="MASCOT2">Mascot 2</option>
                                <option value="MASCOT3">Mascot 3</option>
                                <option value="MASCOT4">Mascot 4</option>
                                <option value="MASCOT5">Mascot 5</option>
                            </select>
                            <p className="text-sm text-gray-500 mt-1">
                                Default mascot character for student devices
                            </p>
                        </div>
                    </div>
                )}

                <div className="pt-6 border-t border-gray-100 mt-8">
                    <button
                        onClick={handleSave}
                        disabled={isSaving || !baseConfig}
                        className="px-8 py-3 bg-brand-orange text-white font-sans font-medium rounded-md hover:bg-brand-orange-dark transition-colors shadow-sm disabled:opacity-50"
                    >
                        {isSaving ? 'Saving...' : 'Save Changes'}
                    </button>
                </div>
            </Card>
        </div>
    );
};
