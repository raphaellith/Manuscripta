/**
 * Global Alerts component for cross-tab alert display.
 * Per FrontendWorkflowSpecifications §5D(1): "Alerts shall be displayed regardless of the tab the user is currently on."
 */

import React from 'react';
import { useAlertContext, Alert } from '../../state/AlertContext';

// Icons
const HandRaisedIcon = () => (
    <svg xmlns="http://www.w3.org/2000/svg" className="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
        <path strokeLinecap="round" strokeLinejoin="round" d="M7 11.5V14m0-2.5v-6a1.5 1.5 0 113 0m-3 6a1.5 1.5 0 00-3 0v2a7.5 7.5 0 0015 0v-5a1.5 1.5 0 00-3 0m-6-3V11m0-5.5v-1a1.5 1.5 0 013 0v1m0 0V11m0-5.5a1.5 1.5 0 013 0v3m0 0V11" />
    </svg>
);

const CheckCircleIcon = () => (
    <svg xmlns="http://www.w3.org/2000/svg" className="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
        <path strokeLinecap="round" strokeLinejoin="round" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
    </svg>
);

// Toast notification component for help requests
interface HelpToastProps {
    alert: Alert;
    onDismiss: () => void;
    onAcknowledge: () => void;
}

const HelpToast: React.FC<HelpToastProps> = ({ alert, onDismiss, onAcknowledge }) => (
    <div className="fixed top-32 right-4 z-[60] animate-slide-in">
        <div className="bg-white rounded-lg shadow-2xl border-l-4 border-brand-orange p-4 flex items-start gap-4 max-w-sm">
            <div className="bg-brand-orange-light p-2 rounded-full animate-pulse">
                <HandRaisedIcon />
            </div>
            <div className="flex-1">
                <p className="font-sans font-semibold text-text-heading">Help Requested</p>
                <p className="font-sans text-sm text-gray-600 mt-1">{alert.deviceName || 'Unknown Device'}: {alert.message}</p>
                <div className="flex gap-2 mt-3">
                    <button
                        onClick={onAcknowledge}
                        className="px-3 py-1 bg-brand-orange text-white text-xs font-medium rounded hover:bg-brand-orange-dark transition-colors"
                    >
                        Acknowledge
                    </button>
                    <button
                        onClick={onDismiss}
                        className="px-3 py-1 bg-gray-100 text-gray-600 text-xs font-medium rounded hover:bg-gray-200 transition-colors"
                    >
                        Dismiss
                    </button>
                </div>
            </div>
        </div>
    </div>
);

// Success toast component
interface SuccessToastProps {
    alert: Alert;
    onDismiss: () => void;
}

const SuccessToast: React.FC<SuccessToastProps> = ({ alert, onDismiss }) => (
    <div className="fixed top-32 right-4 z-[60] animate-slide-in">
        <div className="bg-white rounded-lg shadow-2xl border-l-4 border-green-500 p-4 flex items-start gap-4 max-w-sm">
            <div className="bg-green-100 text-green-600 p-2 rounded-full">
                <CheckCircleIcon />
            </div>
            <div className="flex-1">
                <p className="font-sans font-semibold text-text-heading">Success</p>
                <p className="font-sans text-sm text-gray-600 mt-1">{alert.message}</p>
                <button
                    onClick={onDismiss}
                    className="mt-2 text-xs font-medium text-gray-500 hover:text-gray-700"
                >
                    Dismiss
                </button>
            </div>
        </div>
    </div>
);

/**
 * GlobalAlerts component - renders all alert banners and toasts.
 * Placed at the App level to ensure alerts persist across tab navigation.
 */
export const GlobalAlerts: React.FC = () => {
    const {
        alerts,
        unacknowledgedHelpCount,
        dismissAlert,
        acknowledgeHelp,
        acknowledgeAllHelp
    } = useAlertContext();

    // Filter alerts by type
    const helpAlerts = alerts.filter(a => a.type === 'help');
    const successAlerts = alerts.filter(a => a.type === 'success');
    const errorAlerts = alerts.filter(a => a.type !== 'help' && a.type !== 'success');

    return (
        <>
            {/* CSS Animations */}
            <style>{`
                @keyframes slide-in {
                    from { opacity: 0; transform: translateX(100px); }
                    to { opacity: 1; transform: translateX(0); }
                }
                .animate-slide-in { animation: slide-in 0.3s ease-out forwards; }
                
                @keyframes pulse-slow {
                    0%, 100% { opacity: 1; }
                    50% { opacity: 0.7; }
                }
                .animate-pulse-slow { animation: pulse-slow 2s ease-in-out infinite; }
            `}</style>

            {/* Help request toast (show latest one) */}
            {helpAlerts.slice(0, 1).map(alert => (
                <HelpToast
                    key={alert.id}
                    alert={alert}
                    onDismiss={() => dismissAlert(alert.id)}
                    onAcknowledge={() => alert.deviceId && acknowledgeHelp(alert.deviceId)}
                />
            ))}

            {/* Success toast (show latest one) */}
            {successAlerts.slice(0, 1).map(alert => (
                <SuccessToast
                    key={alert.id}
                    alert={alert}
                    onDismiss={() => dismissAlert(alert.id)}
                />
            ))}

            {/* Persistent help request banner - per §5D(2)(b) */}
            {unacknowledgedHelpCount > 0 && (
                <div className="fixed top-32 left-1/2 transform -translate-x-1/2 z-[60] w-full max-w-xl px-4">
                    <div className="bg-brand-orange-light border border-brand-orange rounded-lg p-4 flex items-center gap-4 shadow-lg animate-pulse-slow">
                        <div className="bg-brand-orange text-white p-2 rounded-full">
                            <HandRaisedIcon />
                        </div>
                        <div className="flex-1">
                            <p className="font-sans font-semibold text-brand-orange-dark">
                                {unacknowledgedHelpCount} student{unacknowledgedHelpCount > 1 ? 's' : ''} requesting help
                            </p>
                        </div>
                        <button
                            onClick={acknowledgeAllHelp}
                            className="px-4 py-2 bg-brand-orange text-white text-sm font-medium rounded-md hover:bg-brand-orange-dark transition-colors"
                        >
                            Acknowledge All
                        </button>
                    </div>
                </div>
            )}

            {/* Error/warning alerts banners - per §5D(3-7) */}
            {errorAlerts.length > 0 && (
                <div className="fixed top-32 right-4 z-[60] w-96 space-y-2">
                    {errorAlerts.slice(0, 3).map(alert => (
                        <div key={alert.id} className="bg-red-50 border border-red-200 rounded-lg p-3 flex items-center gap-3 shadow-md">
                            <span className="text-red-600">⚠️</span>
                            <span className="flex-1 text-sm text-red-800">{alert.message}</span>
                            <button onClick={() => dismissAlert(alert.id)} className="text-red-600 hover:text-red-800">✕</button>
                        </div>
                    ))}
                </div>
            )}
        </>
    );
};
