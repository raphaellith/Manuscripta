/**
 * Application Settings Page.
 * Merges Application Config (from windows-app) and external integration settings.
 */

import React, { useState, useEffect } from "react";
import { Card } from "../common/Card";
import signalRService from "../../services/signalr/SignalRService";
import type { ConfigurationEntity, FeedbackStyle, MascotSelection } from "../../models";

import { EmailCredentialSettings } from "../settings/EmailCredentialSettings";
import { RuntimeDependencySettings } from "../settings/RuntimeDependencySettings";
import { InferenceRuntimeSettings } from "../settings/InferenceRuntimeSettings";
import { SETTINGS_SECTION_MAX_WIDTH } from "../../constants/ui";

export const SettingsPage: React.FC = () => {
    const [baseConfig, setBaseConfig] = useState<ConfigurationEntity | null>(null);
    const [isLoading, setIsLoading] = useState(true);
    const [isSaving, setIsSaving] = useState(false);
    const [error, setError] = useState<string | null>(null);
    const [successMessage, setSuccessMessage] = useState<string | null>(null);
    // Per §7(2): Check if there are paired Android devices
    const [hasPairedAndroidDevices, setHasPairedAndroidDevices] = useState(false);

    // Per §7(1): On entry, call GetBaseConfiguration()
    // Per §7(2): Also check for paired Android devices
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
            } catch (err) {
                console.error("Failed to load base configuration:", err);
                setError("Failed to load configuration");
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
            setSuccessMessage("Base configuration saved successfully");
        } catch (err) {
            console.error("Failed to save base configuration:", err);
            setError("Failed to save configuration");
        } finally {
            setIsSaving(false);
        }
    };

    return (
        <div className="space-y-8 animate-fade-in-up pb-12">
            <header className="mb-6">
                <h1 className="text-3xl font-serif text-text-heading mb-2">Application Settings</h1>
                <p className="text-text-body font-sans text-lg">
                    Manage your preferences and external integrations.
                </p>
            </header>

            <div className="space-y-12">
                <section>
                    <Card className={`p-8 ${SETTINGS_SECTION_MAX_WIDTH}`}>
                        <h2 className="text-2xl font-serif text-text-heading mb-6 border-b border-gray-100 pb-4">Device Base Configuration</h2>
                        {isLoading ? (
                            <div className="animate-pulse flex space-x-4">
                                <div className="flex-1 space-y-4 py-1">
                                    <div className="h-4 bg-gray-200 rounded w-3/4"></div>
                                    <div className="space-y-2">
                                        <div className="h-4 bg-gray-200 rounded"></div>
                                        <div className="h-4 bg-gray-200 rounded w-5/6"></div>
                                    </div>
                                </div>
                            </div>
                        ) : error ? (
                            <div className="p-4 bg-red-50 text-red-700 rounded-lg border border-red-200">
                                {error}
                            </div>
                        ) : baseConfig ? (
                            <>
                                {hasPairedAndroidDevices && (
                                    <div className="mb-6 p-4 bg-yellow-50 text-yellow-800 rounded-lg border border-yellow-200">
                                        <p className="font-medium flex items-center gap-2">
                                            <span className="material-symbols-outlined text-xl">warning</span>
                                            Cannot modify active configuration
                                        </p>
                                        <p className="text-sm mt-1">
                                            Base configuration modifications are restricted while Android devices are paired.
                                        </p>
                                    </div>
                                )}

                                <div className="space-y-6">
                                    {/* Text Size */}
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
                                                    updateConfig("textSize", newValue);
                                                }
                                            }}
                                            disabled={hasPairedAndroidDevices}
                                            className="w-full max-w-xs p-3 bg-white text-text-body font-sans rounded-lg border border-gray-200 focus:border-brand-orange focus:ring-1 focus:ring-brand-orange focus:outline-none disabled:opacity-50 disabled:cursor-not-allowed"
                                        />
                                        <p className="text-sm text-gray-500 mt-1">
                                            Default text size for reading materials on student devices
                                        </p>
                                    </div>

                                    {/* Feedback Style */}
                                    <div>
                                        <label className="font-sans font-medium text-text-heading text-sm mb-2 block">
                                            Feedback Style
                                        </label>
                                        <select
                                            value={baseConfig.feedbackStyle}
                                            onChange={(e) => updateConfig("feedbackStyle", e.target.value)}
                                            disabled={hasPairedAndroidDevices}
                                            className="w-full max-w-xs p-3 bg-white text-text-body font-sans rounded-lg border border-gray-200 focus:border-brand-orange focus:ring-1 focus:ring-brand-orange focus:outline-none disabled:opacity-50 disabled:cursor-not-allowed"
                                        >
                                            <option value="IMMEDIATE">Immediate</option>
                                            <option value="NEUTRAL">Neutral</option>
                                        </select>
                                    </div>

                                    {/* TTS Enabled */}
                                    <div className="flex items-start gap-3">
                                        <input
                                            type="checkbox"
                                            id="baseTtsEnabled"
                                            checked={baseConfig.ttsEnabled}
                                            onChange={(e) => updateConfig("ttsEnabled", e.target.checked)}
                                            disabled={hasPairedAndroidDevices}
                                            className="mt-1 h-5 w-5 text-brand-orange border-gray-300 rounded focus:ring-brand-orange disabled:opacity-50 disabled:cursor-not-allowed"
                                        />
                                        <div className="flex-1">
                                            <label htmlFor="baseTtsEnabled" className="font-sans font-medium text-text-heading text-sm block cursor-pointer">
                                                Text-to-Speech Enabled
                                            </label>
                                        </div>
                                    </div>

                                    {/* AI Scaffolding Enabled */}
                                    <div className="flex items-start gap-3">
                                        <input
                                            type="checkbox"
                                            id="baseAiScaffoldingEnabled"
                                            checked={baseConfig.aiScaffoldingEnabled}
                                            onChange={(e) => updateConfig("aiScaffoldingEnabled", e.target.checked)}
                                            disabled={hasPairedAndroidDevices}
                                            className="mt-1 h-5 w-5 text-brand-orange border-gray-300 rounded focus:ring-brand-orange disabled:opacity-50 disabled:cursor-not-allowed"
                                        />
                                        <div className="flex-1">
                                            <label htmlFor="baseAiScaffoldingEnabled" className="font-sans font-medium text-text-heading text-sm block cursor-pointer">
                                                AI Scaffolding Enabled
                                            </label>
                                        </div>
                                    </div>

                                    {/* Summarisation Enabled */}
                                    <div className="flex items-start gap-3">
                                        <input
                                            type="checkbox"
                                            id="baseSummarisationEnabled"
                                            checked={baseConfig.summarisationEnabled}
                                            onChange={(e) => updateConfig("summarisationEnabled", e.target.checked)}
                                            disabled={hasPairedAndroidDevices}
                                            className="mt-1 h-5 w-5 text-brand-orange border-gray-300 rounded focus:ring-brand-orange disabled:opacity-50 disabled:cursor-not-allowed"
                                        />
                                        <div className="flex-1">
                                            <label htmlFor="baseSummarisationEnabled" className="font-sans font-medium text-text-heading text-sm block cursor-pointer">
                                                Summarisation Enabled
                                            </label>
                                        </div>
                                    </div>

                                    {/* Mascot Selection */}
                                    <div>
                                        <label className="font-sans font-medium text-text-heading text-sm mb-2 block">
                                            Mascot Selection
                                        </label>
                                        <select
                                            value={baseConfig.mascotSelection}
                                            onChange={(e) => updateConfig("mascotSelection", e.target.value as MascotSelection)}
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
                                    </div>

                                    {/* Save Actions */}
                                    {!hasPairedAndroidDevices && (
                                        <div className="pt-6 border-t border-gray-100 flex items-center justify-between">
                                            {successMessage && (
                                                <span className="text-green-600 font-medium flex items-center gap-1">
                                                    <span className="material-symbols-outlined text-sm">check_circle</span>
                                                    {successMessage}
                                                </span>
                                            )}
                                            <button
                                                onClick={handleSave}
                                                disabled={isSaving}
                                                className="ml-auto px-6 py-2 bg-brand-orange text-white font-serif rounded-lg hover:bg-brand-orange-dark transition-colors disabled:opacity-50 disabled:cursor-not-allowed flex items-center gap-2"
                                            >
                                                {isSaving ? (
                                                    <>
                                                        <span className="material-symbols-outlined text-xl animate-spin">progress_activity</span>
                                                        Saving...
                                                    </>
                                                ) : (
                                                    <>
                                                        <span className="material-symbols-outlined text-xl">save</span>
                                                        Save Configuration
                                                    </>
                                                )}
                                            </button>
                                        </div>
                                    )}
                                </div>
                            </>
                        ) : null}
                    </Card>
                </section>

                <section>
                    <RuntimeDependencySettings />
                </section>
                <section>
                    <InferenceRuntimeSettings />
                </section>
                <section>
                    <EmailCredentialSettings />
                </section>
            </div>

            <style>{`
                @keyframes fade-in-up {
                    from { opacity: 0; transform: translateY(10px); }
                    to { opacity: 1; transform: translateY(0); }
                }
                .animate-fade-in-up { animation: fade-in-up 0.4s ease-out forwards; }
            `}</style>
        </div>
    );
};
