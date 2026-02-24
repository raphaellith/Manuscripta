/**
 * Application Settings Page.
 */

import React from 'react';
import { EmailCredentialSettings } from '../settings/EmailCredentialSettings';
import { RuntimeDependencySettings } from '../settings/RuntimeDependencySettings';

export const SettingsPage: React.FC = () => {
    return (
        <div className="space-y-8 animate-fade-in-up">
            <header className="mb-6">
                <h1 className="text-3xl font-serif text-text-heading mb-2">Application Settings</h1>
                <p className="text-text-body font-sans text-lg">
                    Manage your preferences and external integrations.
                </p>
            </header>

            <div className="space-y-12">
                <section>
                    <RuntimeDependencySettings />
                </section>
                <section>
                    <EmailCredentialSettings />
                </section>
                {/* Future settings sections can be added here */}
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
