/**
 * Modal for viewing and modifying external device PDF export setting overrides.
 * Per FrontendWorkflowSpecifications §5H(2).
 */

import React, { useState, useEffect } from 'react';
import signalRService from '../../services/signalr/SignalRService';
import type {
    ExternalDeviceEntity,
    PdfExportSettingsEntity,
    LinePatternType,
    LineSpacingPreset,
    FontSizePreset
} from '../../models';
import { ModalOverlay } from './ModalOverlay';

interface ExternalDeviceConfigurationModalProps {
    device: ExternalDeviceEntity;
    onClose: () => void;
    onSaved: () => void;
}

/** Human-readable labels for enum values. */
const LINE_PATTERN_LABELS: Record<LinePatternType, string> = {
    RULED: 'Ruled',
    SQUARE: 'Square',
    ISOMETRIC: 'Isometric',
    NONE: 'None',
};

const LINE_SPACING_LABELS: Record<LineSpacingPreset, string> = {
    SMALL: 'Small (6mm)',
    MEDIUM: 'Medium (8mm)',
    LARGE: 'Large (10mm)',
    EXTRA_LARGE: 'Extra Large (14mm)',
};

const FONT_SIZE_LABELS: Record<FontSizePreset, string> = {
    SMALL: 'Small (10pt)',
    MEDIUM: 'Medium (12pt)',
    LARGE: 'Large (14pt)',
    EXTRA_LARGE: 'Extra Large (16pt)',
};

export const ExternalDeviceConfigurationModal: React.FC<ExternalDeviceConfigurationModalProps> = ({
    device,
    onClose,
    onSaved
}) => {
    const [linePatternType, setLinePatternType] = useState<LinePatternType | null>(device.linePatternType ?? null);
    const [lineSpacingPreset, setLineSpacingPreset] = useState<LineSpacingPreset | null>(device.lineSpacingPreset ?? null);
    const [fontSizePreset, setFontSizePreset] = useState<FontSizePreset | null>(device.fontSizePreset ?? null);

    const [globalDefaults, setGlobalDefaults] = useState<PdfExportSettingsEntity | null>(null);
    const [isLoading, setIsLoading] = useState(true);
    const [isSubmitting, setIsSubmitting] = useState(false);
    const [error, setError] = useState<string | null>(null);

    // Load global defaults so we can show "Default (Ruled)" etc.
    useEffect(() => {
        const load = async () => {
            try {
                setIsLoading(true);
                const defaults = await signalRService.getPdfExportSettings();
                setGlobalDefaults(defaults);
            } catch (err) {
                console.error('Failed to load global PDF export settings:', err);
                setError('Failed to load settings');
            } finally {
                setIsLoading(false);
            }
        };
        load();
    }, []);

    // Per §5H(2)(c): Save changes via UpdateExternalDevice
    const handleSubmit = async () => {
        setIsSubmitting(true);
        setError(null);
        try {
            const updated: ExternalDeviceEntity = {
                ...device,
                linePatternType: linePatternType,
                lineSpacingPreset: lineSpacingPreset,
                fontSizePreset: fontSizePreset,
            };
            await signalRService.updateExternalDevice(updated);
            onSaved();
        } catch (err) {
            console.error('Failed to update external device settings:', err);
            setError('Failed to save settings');
        } finally {
            setIsSubmitting(false);
        }
    };

    /** Render a dropdown with a "Default (...)" option per §5H(2)(b). */
    function renderDropdown<T extends string>(
        label: string,
        value: T | null,
        onChange: (v: T | null) => void,
        options: Record<T, string>,
        globalDefault: T | undefined
    ) {
        const defaultLabel = globalDefault ? options[globalDefault] : '...';
        return (
            <div>
                <label className="font-sans font-medium text-text-heading text-sm mb-2 block">
                    {label}
                </label>
                <select
                    value={value ?? ''}
                    onChange={(e) => {
                        const val = e.target.value;
                        onChange(val === '' ? null : val as T);
                    }}
                    className="w-full p-3 bg-white text-text-body font-sans rounded-lg border border-gray-200 focus:border-brand-orange focus:ring-1 focus:ring-brand-orange focus:outline-none"
                >
                    {/* Per §5H(2)(b)(i): Default option maps to null */}
                    <option value="">Default ({defaultLabel})</option>
                    {/* Per §5H(2)(b)(ii): All enum values */}
                    {(Object.keys(options) as T[]).map((key) => (
                        <option key={key} value={key}>{options[key]}</option>
                    ))}
                </select>
            </div>
        );
    }

    return (
        <ModalOverlay priority="low">
            <div className="bg-white rounded-lg p-8 shadow-2xl w-full max-w-2xl space-y-6 animate-fade-in-up border border-gray-100 max-h-[90vh] overflow-y-auto">
                <h2 className="text-2xl font-serif text-text-heading">
                    PDF Settings: {device.name}
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

                {globalDefaults && !isLoading && (
                    <div className="space-y-6">
                        <p className="text-sm text-gray-500">
                            Override the global PDF export defaults for this device. Select &ldquo;Default&rdquo; to use the global setting.
                        </p>

                        {/* Per §5H(2)(b): Three dropdown controls */}
                        {renderDropdown<LinePatternType>(
                            'Line Pattern Type',
                            linePatternType,
                            setLinePatternType,
                            LINE_PATTERN_LABELS,
                            globalDefaults.linePatternType
                        )}

                        {renderDropdown<LineSpacingPreset>(
                            'Line Spacing Preset',
                            lineSpacingPreset,
                            setLineSpacingPreset,
                            LINE_SPACING_LABELS,
                            globalDefaults.lineSpacingPreset
                        )}

                        {renderDropdown<FontSizePreset>(
                            'Font Size Preset',
                            fontSizePreset,
                            setFontSizePreset,
                            FONT_SIZE_LABELS,
                            globalDefaults.fontSizePreset
                        )}
                    </div>
                )}

                {/* Per §5H(2)(c): Save button */}
                <div className="flex flex-wrap gap-4 pt-4 border-t border-gray-100">
                    <button
                        onClick={handleSubmit}
                        disabled={isSubmitting || isLoading || !globalDefaults}
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
