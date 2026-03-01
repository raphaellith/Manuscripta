/**
 * Settings Page component.
 * Per FrontendWorkflowSpecifications §7.
 */

import React, { useState, useEffect } from 'react';
import { Card } from '../common/Card';
import signalRService from '../../services/signalr/SignalRService';
import type { ConfigurationEntity, FeedbackStyle, MascotSelection } from '../../models';
import { RuntimeDependencyInstallModal } from '../modals/RuntimeDependencyInstallModal';

// Per FrontendWorkflowSpecifications §3A(4): Runtime dependencies that can be managed
const RUNTIME_DEPENDENCIES = [
    { id: 'ollama', name: 'Ollama', description: 'Local LLM runtime for AI features' },
    { id: 'chroma', name: 'ChromaDB', description: 'Vector database for semantic search' },
    { id: 'qwen3:8b', name: 'Qwen3 8B Model', description: 'Primary AI model for generation' },
    { id: 'granite4', name: 'IBM Granite 4.0 Model', description: 'Fallback AI model' },
    { id: 'nomic-embed-text', name: 'Nomic Embed Text Model', description: 'Embedding model for semantic search' },
    { id: 'rmapi', name: 'rmapi', description: 'reMarkable tablet integration' },
];

export const SettingsPage: React.FC = () => {
    const [baseConfig, setBaseConfig] = useState<ConfigurationEntity | null>(null);
    const [isLoading, setIsLoading] = useState(true);
    const [isSaving, setIsSaving] = useState(false);
    const [error, setError] = useState<string | null>(null);
    const [successMessage, setSuccessMessage] = useState<string | null>(null);
    // Per §7(2): Check if there are paired Android devices
    const [hasPairedAndroidDevices, setHasPairedAndroidDevices] = useState(false);

    // Per §3A(4): Runtime dependency management state
    const [dependencyStatuses, setDependencyStatuses] = useState<Record<string, boolean | null>>({});
    const [checkingDependency, setCheckingDependency] = useState<string | null>(null);
    const [reinstallDependencyId, setReinstallDependencyId] = useState<string | null>(null);

    // Per §7(1): On entry, call GetBaseConfiguration()
    // Per §7(2): Also check for paired Android devices
    // Per §3A(4)(a): Check all runtime dependencies on entry
    useEffect(() => {
        const loadBaseConfiguration = async () => {
            try {
                setIsLoading(true);
                const [config, androidDevices] = await Promise.all([
                    signalRService.getBaseConfiguration(),
                    signalRService.getAllPairedDevices()
                ]);
                setBaseConfig(config);
                // Per §7(2)(b): prevent modification when there are paired Android devices
                setHasPairedAndroidDevices(androidDevices.length > 0);

                // Per §3A(4)(a): Check availability of all runtime dependencies
                const statuses: Record<string, boolean | null> = {};
                await Promise.all(
                    RUNTIME_DEPENDENCIES.map(async (dep) => {
                        try {
                            const available = await signalRService.checkRuntimeDependencyAvailability(dep.id);
                            statuses[dep.id] = available;
                        } catch {
                            statuses[dep.id] = null; // null indicates error checking
                        }
                    })
                );
                setDependencyStatuses(statuses);
            } catch (err) {
                console.error('Failed to load base configuration:', err);
                setError('Failed to load configuration');
            } finally {
                setIsLoading(false);
            }
        };

        loadBaseConfiguration();
    }, []);

    // Per §3A(4)(a): Handler to re-check a single dependency
    const handleRecheckDependency = async (dependencyId: string) => {
        setCheckingDependency(dependencyId);
        try {
            const available = await signalRService.checkRuntimeDependencyAvailability(dependencyId);
            setDependencyStatuses(prev => ({ ...prev, [dependencyId]: available }));
        } catch (err) {
            console.error(`Failed to check ${dependencyId}:`, err);
            setDependencyStatuses(prev => ({ ...prev, [dependencyId]: null }));
        } finally {
            setCheckingDependency(null);
        }
    };

    // Per §3A(4)(b): Handler to reinstall a dependency
    const handleReinstallDependency = (dependencyId: string) => {
        setReinstallDependencyId(dependencyId);
    };

    const handleInstallComplete = () => {
        setReinstallDependencyId(null);
        // Re-check the dependency after installation
        if (reinstallDependencyId) {
            handleRecheckDependency(reinstallDependencyId);
        }
    };

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
            {/* Per FrontendWorkflowSpecifications §3A(4): Runtime Dependency Management */}
            <Card>
                <div className="border-b border-gray-100 pb-4 mb-6">
                    <h2 className="text-2xl font-serif text-text-heading">Runtime Dependencies</h2>
                    <p className="text-sm text-gray-500 mt-2">
                        Manage runtime dependencies required for AI features and device integration.
                    </p>
                </div>

                <div className="space-y-4">
                    {RUNTIME_DEPENDENCIES.map((dep) => {
                        const status = dependencyStatuses[dep.id];
                        const isChecking = checkingDependency === dep.id;

                        return (
                            <div key={dep.id} className="flex items-center justify-between p-4 border border-gray-200 rounded-lg">
                                <div className="flex-1">
                                    <div className="flex items-center gap-2">
                                        <h4 className="font-sans font-medium text-text-heading">{dep.name}</h4>
                                        {status === true && (
                                            <span className="px-2 py-0.5 text-xs font-medium bg-green-100 text-green-800 rounded">
                                                Installed
                                            </span>
                                        )}
                                        {status === false && (
                                            <span className="px-2 py-0.5 text-xs font-medium bg-red-100 text-red-800 rounded">
                                                Not Installed
                                            </span>
                                        )}
                                        {status === null && (
                                            <span className="px-2 py-0.5 text-xs font-medium bg-yellow-100 text-yellow-800 rounded">
                                                Error
                                            </span>
                                        )}
                                    </div>
                                    <p className="text-sm text-gray-500 mt-1">{dep.description}</p>
                                </div>
                                <div className="flex gap-2">
                                    {/* Per §3A(4)(a): Re-check button */}
                                    <button
                                        onClick={() => handleRecheckDependency(dep.id)}
                                        disabled={isChecking}
                                        className="px-4 py-2 text-sm font-sans font-medium text-brand-orange border border-brand-orange rounded-md hover:bg-brand-orange hover:text-white transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
                                    >
                                        {isChecking ? 'Checking…' : 'Re-check'}
                                    </button>
                                    {/* Per §3A(4)(b): Reinstall button */}
                                    <button
                                        onClick={() => handleReinstallDependency(dep.id)}
                                        className="px-4 py-2 text-sm font-sans font-medium bg-brand-orange text-white rounded-md hover:bg-brand-orange-dark transition-colors shadow-sm"
                                    >
                                        Reinstall
                                    </button>
                                </div>
                            </div>
                        );
                    })}
                </div>
            </Card>

            <Card>
                <div className="border-b border-gray-100 pb-4 mb-6">
                    <h2 className="text-2xl font-serif text-text-heading">Base Configuration for Android Devices</h2>
                    <p className="text-sm text-gray-500 mt-2">
                        These settings serve as the default configuration for all Android devices. Individual Android device configurations can override these defaults.
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

                {/* Per §7(2)(b): Show warning when devices are paired */}
                {hasPairedAndroidDevices && (
                    <div className="bg-yellow-50 border border-yellow-200 text-yellow-800 px-4 py-3 rounded mb-6">
                        <p className="font-medium">Configuration is locked</p>
                        <p className="text-sm mt-1">
                            The base configuration cannot be modified while Android devices are paired.
                            Please unpair all Android devices before making changes.
                        </p>
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
                                onChange={(e) => {
                                    const newValue = parseInt(e.target.value, 10);
                                    if (!isNaN(newValue) && newValue >= 5 && newValue <= 50) {
                                        updateConfig('textSize', newValue);
                                    }
                                }}
                                disabled={hasPairedAndroidDevices}
                                className="w-full max-w-xs p-3 bg-white text-text-body font-sans rounded-lg border border-gray-200 focus:border-brand-orange focus:ring-1 focus:ring-brand-orange focus:outline-none disabled:opacity-50 disabled:cursor-not-allowed"
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
                                disabled={hasPairedAndroidDevices}
                                className="w-full max-w-xs p-3 bg-white text-text-body font-sans rounded-lg border border-gray-200 focus:border-brand-orange focus:ring-1 focus:ring-brand-orange focus:outline-none disabled:opacity-50 disabled:cursor-not-allowed"
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
                                disabled={hasPairedAndroidDevices}
                                className="mt-1 h-5 w-5 text-brand-orange border-gray-300 rounded focus:ring-brand-orange disabled:opacity-50 disabled:cursor-not-allowed"
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
                                disabled={hasPairedAndroidDevices}
                                className="mt-1 h-5 w-5 text-brand-orange border-gray-300 rounded focus:ring-brand-orange disabled:opacity-50 disabled:cursor-not-allowed"
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
                                disabled={hasPairedAndroidDevices}
                                className="mt-1 h-5 w-5 text-brand-orange border-gray-300 rounded focus:ring-brand-orange disabled:opacity-50 disabled:cursor-not-allowed"
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
                                disabled={hasPairedAndroidDevices}
                                className="w-full max-w-xs p-3 bg-white text-text-body font-sans rounded-lg border border-gray-200 focus:border-brand-orange focus:ring-1 focus:ring-brand-orange focus:outline-none disabled:opacity-50 disabled:cursor-not-allowed"
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
                        disabled={isSaving || !baseConfig || hasPairedAndroidDevices}
                        className="px-8 py-3 bg-brand-orange text-white font-sans font-medium rounded-md hover:bg-brand-orange-dark transition-colors shadow-sm disabled:opacity-50 disabled:cursor-not-allowed"
                    >
                        {isSaving ? 'Saving...' : 'Save Changes'}
                    </button>
                </div>
            </Card>

            {/* Per §3A(4)(b): RuntimeDependencyInstallModal for reinstallation */}
            {reinstallDependencyId && (
                <RuntimeDependencyInstallModal
                    dependencyIds={[reinstallDependencyId]}
                    onClose={() => setReinstallDependencyId(null)}
                    onInstallComplete={handleInstallComplete}
                />
            )}
        </div>
    );
};
