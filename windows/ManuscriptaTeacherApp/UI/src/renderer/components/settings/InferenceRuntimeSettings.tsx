import React, { useState, useEffect } from "react";
import { Card } from "../common/Card";
import signalRService from "../../services/signalr/SignalRService";
import { SETTINGS_SECTION_MAX_WIDTH } from "../../constants/ui";

export const InferenceRuntimeSettings: React.FC = () => {
    const [runtime, setRuntime] = useState<string>("STANDARD");
    const [isLoading, setIsLoading] = useState(true);
    const [isSaving, setIsSaving] = useState(false);
    const [error, setError] = useState<string | null>(null);
    const [successMessage, setSuccessMessage] = useState<string | null>(null);

    useEffect(() => {
        const loadRuntime = async () => {
            try {
                setIsLoading(true);
                const activeRuntime = await signalRService.getActiveInferenceRuntime();
                setRuntime(activeRuntime || "STANDARD");
            } catch (err) {
                console.error("Failed to load active inference runtime:", err);
                setError("Failed to load runtime setting");
            } finally {
                setIsLoading(false);
            }
        };

        loadRuntime();
    }, []);

    const handleSave = async () => {
        setIsSaving(true);
        setError(null);
        setSuccessMessage(null);

        try {
            const success = await signalRService.switchInferenceRuntime(runtime);
            if (success) {
                setSuccessMessage("Inference runtime updated successfully.");
            } else {
                setError("Runtime switch failed (NPU may be missing).");
                // Revert to fetched state on failure
                const activeRuntime = await signalRService.getActiveInferenceRuntime();
                setRuntime(activeRuntime || "STANDARD");
            }
        } catch (err) {
            console.error("Failed to switch inference runtime:", err);
            setError("Failed to update runtime setting");
        } finally {
            setIsSaving(false);
        }
    };

    return (
        <Card className={`p-8 ${SETTINGS_SECTION_MAX_WIDTH}`}>
            <h2 className="text-2xl font-serif text-text-heading mb-6 border-b border-gray-100 pb-4">
                Inference Runtime
            </h2>
            {isLoading ? (
                <div className="animate-pulse flex space-x-4">
                    <div className="flex-1 space-y-4 py-1">
                        <div className="h-4 bg-gray-200 rounded w-3/4"></div>
                        <div className="h-4 bg-gray-200 rounded w-1/2"></div>
                    </div>
                </div>
            ) : (
                <div className="space-y-6">
                    <p className="text-text-body font-sans text-sm">
                        Select the underlying runtime environment for local AI execution. OpenVINO is optimized for Intel NPUs.
                    </p>

                    {error && (
                        <div className="p-4 bg-red-50 text-red-700 rounded-lg border border-red-200">
                            {error}
                        </div>
                    )}

                    <div>
                        <label className="font-sans font-medium text-text-heading text-sm mb-2 block">
                            Active Runtime
                        </label>
                        <select
                            value={runtime}
                            onChange={(e) => {
                                setRuntime(e.target.value);
                                setSuccessMessage(null);
                            }}
                            className="w-full max-w-xs p-3 bg-white text-text-body font-sans rounded-lg border border-gray-200 focus:border-brand-orange focus:ring-1 focus:ring-brand-orange focus:outline-none"
                        >
                            <option value="STANDARD">Standard Ollama</option>
                            <option value="OPENVINO">OpenVINO for NPU</option>
                        </select>
                    </div>

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
                                    Save Runtime
                                </>
                            )}
                        </button>
                    </div>
                </div>
            )}
        </Card>
    );
};
