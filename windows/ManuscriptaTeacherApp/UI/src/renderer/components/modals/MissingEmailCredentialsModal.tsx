import React from 'react';
import { ModalOverlay } from './ModalOverlay';

interface Props {
    isOpen: boolean;
    onClose: () => void;
    onGoToSettings: () => void;
}

export const MissingEmailCredentialsModal: React.FC<Props> = ({
    isOpen,
    onClose,
    onGoToSettings,
}) => {
    if (!isOpen) return null;

    return (
        <ModalOverlay priority="moderate" onClick={onClose}>
            <div
                className="bg-white rounded-xl shadow-card w-[480px] overflow-hidden"
                onClick={e => e.stopPropagation()}
            >
                <div className="p-6">
                    <div className="flex items-center gap-3 mb-4 text-brand-orange">
                        <span className="material-symbols-outlined text-3xl">mail</span>
                        <h2 className="text-xl font-serif text-text-heading">Email Setup Required</h2>
                    </div>

                    <p className="text-text-body font-sans mb-6">
                        This operation requires SMTP Email Credentials to send files to email-enabled devices (like Kindles). Would you like to configure these settings now?
                    </p>

                    <div className="flex justify-end gap-3 pt-6 border-t border-gray-100">
                        <button
                            onClick={onClose}
                            className="px-5 py-2 text-text-body font-sans hover:bg-gray-50 rounded-lg transition-colors"
                        >
                            Cancel
                        </button>
                        <button
                            onClick={onGoToSettings}
                            className="px-5 py-2 bg-brand-orange text-white font-sans rounded-lg hover:bg-brand-orange-dark transition-colors flex items-center justify-center min-w-[120px] gap-2"
                        >
                            <span className="material-symbols-outlined text-sm">settings</span>
                            Go to Settings
                        </button>
                    </div>
                </div>
            </div>
        </ModalOverlay>
    );
};
