/**
 * Component for configuring SMTP Email Credentials.
 * Per EmailHandlingSpecification §4.
 */

import React, { useState, useEffect } from 'react';
import signalRService from '../../services/signalr/SignalRService';
import type { EmailCredentialEntity } from '../../models';
import { useAlertContext } from '../../state/AlertContext';
import { SETTINGS_SECTION_MAX_WIDTH } from '../../constants/ui';

export const EmailCredentialSettings: React.FC = () => {
    const { addAlert } = useAlertContext();
    const [credentials, setCredentials] = useState<EmailCredentialEntity | null>(null);
    const [emailAddress, setEmailAddress] = useState('');
    const [smtpHost, setSmtpHost] = useState('');
    const [smtpPort, setSmtpPort] = useState(587);
    const [password, setPassword] = useState('');
    const [isLoading, setIsLoading] = useState(true);
    const [isSaving, setIsSaving] = useState(false);
    const [isDeleting, setIsDeleting] = useState(false);

    useEffect(() => {
        const loadCredentials = async () => {
            try {
                const creds = await signalRService.getEmailCredentials();
                if (creds) {
                    setCredentials(creds);
                    setEmailAddress(creds.emailAddress);
                    setSmtpHost(creds.smtpHost);
                    setSmtpPort(creds.smtpPort);
                    // Password is intentionally not populated here if it's '********' from backend,
                    // but we clear the local state to force the user to re-enter it if they want to change it,
                    // or we just leave it blank and the backend will not update it if it's empty.
                    // Actually, the API contract implies password is optional on update, but let's just 
                    // set it to empty locally. The backend returns '********' as a placeholder.
                    setPassword('');
                }
            } catch (error) {
                console.error('Failed to load email credentials:', error);
                addAlert('control_failed', undefined, 'Failed to load email credentials');
            } finally {
                setIsLoading(false);
            }
        };
        loadCredentials();
    }, [addAlert]);

    const handleSave = async (e: React.FormEvent) => {
        e.preventDefault();
        setIsSaving(true);
        try {
            const newCreds: EmailCredentialEntity = {
                id: credentials?.id || '00000000-0000-0000-0000-000000000000', // Backend handles empty GUID as new
                emailAddress,
                smtpHost,
                smtpPort,
                password: password.trim() ? password : (credentials ? '********' : '')
            };

            // §4B(3): save validates via TestConnectionAsync synchronously before saving
            await signalRService.saveEmailCredentials(newCreds);

            addAlert('success', undefined, 'Email credentials verified and saved successfully.');

            // Reload credentials to get the updated ID and placeholder password
            const reloadedCreds = await signalRService.getEmailCredentials();
            if (reloadedCreds) {
                setCredentials(reloadedCreds);
                setPassword('');
            }
        } catch (error) {
            console.error('Failed to save email credentials:', error);
            addAlert('control_failed', undefined, error instanceof Error ? error.message : 'Invalid credentials or SMTP service unreachable.');
        } finally {
            setIsSaving(false);
        }
    };

    const handleDelete = async () => {
        if (!confirm('Are you sure you want to delete these email credentials? You will not be able to send files to Kindle devices.')) {
            return;
        }
        setIsDeleting(true);
        try {
            await signalRService.deleteEmailCredentials();
            setCredentials(null);
            setEmailAddress('');
            setSmtpHost('');
            setSmtpPort(587);
            setPassword('');
            addAlert('success', undefined, 'Email credentials deleted.');
        } catch (error) {
            console.error('Failed to delete email credentials:', error);
            addAlert('control_failed', undefined, 'Failed to delete credentials.');
        } finally {
            setIsDeleting(false);
        }
    };

    if (isLoading) {
        return (
            <div className="flex items-center justify-center p-8">
                <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-brand-orange"></div>
            </div>
        );
    }

    return (
        <div className={`bg-white rounded-xl shadow-soft border border-gray-100 p-8 ${SETTINGS_SECTION_MAX_WIDTH}`}>
            <h2 className="text-2xl font-serif text-text-heading mb-6 border-b border-gray-100 pb-4">
                SMTP Email Credentials
            </h2>
            <p className="text-text-body font-sans text-sm mb-6 leading-relaxed">
                Configure your SMTP routing to allow Manuscripta to send Reading Materials as email attachments to external devices like Kindles. Ensure your email provider allows SMTP access (you may need an "App Password").
            </p>

            <form onSubmit={handleSave} className="space-y-5">
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-5">
                    <div className="sm:col-span-2">
                        <label className="font-sans font-medium text-text-heading text-sm mb-2 block">
                            Email Address <span className="text-red-500">*</span>
                        </label>
                        <input
                            type="email"
                            required
                            value={emailAddress}
                            onChange={(e) => setEmailAddress(e.target.value)}
                            className="w-full p-3 bg-white text-text-body font-sans rounded-lg border border-gray-200 focus:border-brand-orange focus:ring-1 focus:ring-brand-orange focus:outline-none transition-colors"
                            placeholder="e.g., sender@example.com"
                        />
                    </div>

                    <div>
                        <label className="font-sans font-medium text-text-heading text-sm mb-2 block">
                            SMTP Host <span className="text-red-500">*</span>
                        </label>
                        <input
                            type="text"
                            required
                            value={smtpHost}
                            onChange={(e) => setSmtpHost(e.target.value)}
                            className="w-full p-3 bg-white text-text-body font-sans rounded-lg border border-gray-200 focus:border-brand-orange focus:ring-1 focus:ring-brand-orange focus:outline-none transition-colors"
                            placeholder="e.g., smtp.gmail.com"
                        />
                    </div>

                    <div>
                        <label className="font-sans font-medium text-text-heading text-sm mb-2 block">
                            SMTP Port <span className="text-red-500">*</span>
                        </label>
                        <input
                            type="number"
                            required
                            value={smtpPort}
                            onChange={(e) => setSmtpPort(parseInt(e.target.value))}
                            className="w-full p-3 bg-white text-text-body font-sans rounded-lg border border-gray-200 focus:border-brand-orange focus:ring-1 focus:ring-brand-orange focus:outline-none transition-colors"
                        />
                    </div>

                    <div className="sm:col-span-2">
                        <label className="font-sans font-medium text-text-heading text-sm mb-2 block">
                            Password {credentials && <span className="text-gray-400 font-normal ml-2">(Leave blank to keep existing password)</span>} {!credentials && <span className="text-red-500">*</span>}
                        </label>
                        <input
                            type="password"
                            required={!credentials}
                            value={password}
                            onChange={(e) => setPassword(e.target.value)}
                            className="w-full p-3 bg-white text-text-body font-sans rounded-lg border border-gray-200 focus:border-brand-orange focus:ring-1 focus:ring-brand-orange focus:outline-none transition-colors"
                            placeholder={credentials ? '********' : 'Enter SMTP password'}
                        />
                    </div>
                </div>

                <div className="flex flex-wrap gap-4 pt-6 mt-6 border-t border-gray-100">
                    <button
                        type="submit"
                        disabled={isSaving}
                        className="px-8 py-3 bg-brand-orange text-white font-sans font-medium rounded-md hover:bg-brand-orange-dark transition-colors shadow-sm disabled:bg-gray-400 disabled:cursor-not-allowed flex items-center justify-center min-w-[140px]"
                    >
                        {isSaving ? (
                            <div className="animate-spin h-5 w-5 border-2 border-white border-t-transparent rounded-full"></div>
                        ) : credentials ? (
                            'Update credentials'
                        ) : (
                            'Test & Save'
                        )}
                    </button>

                    {credentials && (
                        <button
                            type="button"
                            onClick={handleDelete}
                            disabled={isDeleting || isSaving}
                            className="px-6 py-3 bg-white text-red-600 border border-red-200 font-sans font-medium rounded-md hover:bg-red-50 transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
                        >
                            {isDeleting ? 'Deleting...' : 'Delete'}
                        </button>
                    )}
                </div>
            </form>
        </div>
    );
};
