/**
 * Alert Context for global alert state management.
 * Per FrontendWorkflowSpecifications §5D(1): "Alerts shall be displayed regardless of the tab the user is currently on."
 */

import React, { createContext, useContext, useState, useEffect, useCallback, ReactNode, useRef } from 'react';
import signalRService from '../services/signalr/SignalRService';
import type { PairedDeviceEntity, DeviceStatusEntity } from '../models';

// Alert types for various events
export interface Alert {
    id: string;
    type: 'help' | 'disconnection' | 'distribution_failed' | 'control_failed' | 'config_refresh_failed' | 'feedback_failed' | 'embedding_failed' | 'success';
    deviceId?: string;
    deviceName?: string;
    message: string;
    timestamp: number;
}

interface AlertContextValue {
    alerts: Alert[];
    handRaisedDeviceIds: Set<string>;
    addAlert: (type: Alert['type'], deviceId: string | undefined, message: string) => void;
    dismissAlert: (alertId: string) => void;
    acknowledgeHelp: (deviceId: string) => void;
    acknowledgeAllHelp: () => void;
    setPairedDevices: (devices: PairedDeviceEntity[]) => void;
    unacknowledgedHelpCount: number;
}

const AlertContext = createContext<AlertContextValue | null>(null);

export function useAlertContext(): AlertContextValue {
    const context = useContext(AlertContext);
    if (!context) {
        throw new Error('useAlertContext must be used within an AlertProvider');
    }
    return context;
}

interface AlertProviderProps {
    children: ReactNode;
}

export function AlertProvider({ children }: AlertProviderProps): React.ReactElement {
    const [alerts, setAlerts] = useState<Alert[]>([]);
    const [handRaisedDeviceIds, setHandRaisedDeviceIds] = useState<Set<string>>(new Set());

    // Ref to hold latest devices without triggering re-effects
    const devicesRef = useRef<PairedDeviceEntity[]>([]);

    // Update devices ref for name resolution
    const setPairedDevices = useCallback((devices: PairedDeviceEntity[]) => {
        devicesRef.current = devices;
    }, []);

    // Helper to add alerts - uses ref to avoid dependency cycles
    const addAlert = useCallback((type: Alert['type'], deviceId: string | undefined, message: string) => {
        const device = deviceId ? devicesRef.current.find(d => d.deviceId === deviceId) : undefined;
        setAlerts(prev => [...prev, {
            id: `${Date.now()}-${deviceId || 'global'}`,
            type,
            deviceId,
            deviceName: device?.name,
            message,
            timestamp: Date.now()
        }]);
    }, []);

    // Dismiss a specific alert
    const dismissAlert = useCallback((alertId: string) => {
        setAlerts(prev => prev.filter(a => a.id !== alertId));
    }, []);

    // Acknowledge help request for a device
    const acknowledgeHelp = useCallback((deviceId: string) => {
        setHandRaisedDeviceIds(prev => {
            const next = new Set(prev);
            next.delete(deviceId);
            return next;
        });
        setAlerts(prev => prev.filter(a => !(a.type === 'help' && a.deviceId === deviceId)));
    }, []);

    // Acknowledge all help requests
    const acknowledgeAllHelp = useCallback(() => {
        setAlerts(prev => prev.filter(a => a.type !== 'help'));
        setHandRaisedDeviceIds(new Set());
    }, []);

    // Register SignalR handlers for alerts (runs once on mount)
    useEffect(() => {
        // Handler: Device disconnection alerts (per §5D(3))
        const unsubStatus = signalRService.onDeviceStatusUpdate((status: DeviceStatusEntity) => {
            if (status.status === 'DISCONNECTED') {
                const device = devicesRef.current.find(d => d.deviceId === status.deviceId);
                addAlert('disconnection', status.deviceId, `${device?.name || 'Device'} disconnected`);
            }
        });

        // Handler: Hand raised alerts (per §5D(2))
        const unsubHandRaised = signalRService.onHandRaised((deviceId: string) => {
            setHandRaisedDeviceIds(prev => new Set(prev).add(deviceId));
            const device = devicesRef.current.find(d => d.deviceId === deviceId);
            addAlert('help', deviceId, `${device?.name || 'Device'} is requesting help`);
        });

        // Handler: Distribution failure alerts (per §5D(4))
        // Payload includes deviceId and materialId per API Contract §3.6.2
        const unsubDistributionFailed = signalRService.onDistributionFailed((payload: { deviceId: string; materialId: string }) => {
            const device = devicesRef.current.find(d => d.deviceId === payload.deviceId);
            // Entity IDs are internal - only show device name to user
            addAlert('distribution_failed', payload.deviceId, `Failed to deploy material to ${device?.name || 'device'}`);
        });

        // Handler: Remote control failure alerts (per §5D(5))
        const unsubControlFailed = signalRService.onRemoteControlFailed((payload: { deviceId: string; command: string }) => {
            if (payload && payload.deviceId) {
                const device = devicesRef.current.find(d => d.deviceId === payload.deviceId);
                const name = device ? device.name : payload.deviceId.substring(0, 8);
                addAlert('control_failed', payload.deviceId, `Remote control command '${payload.command}' failed for ${name} (Timeout)`);
            } else {
                addAlert('control_failed', undefined, 'Remote control command failed');
            }
        });

        // Handler: Feedback delivery failure alerts (per §5D(7))
        // Payload includes deviceId and feedbackId per API Contract §3.6.2
        const unsubFeedbackFailed = signalRService.onFeedbackDeliveryFailed((payload: { deviceId: string; feedbackId: string }) => {
            const device = devicesRef.current.find(d => d.deviceId === payload.deviceId);
            // Entity IDs are internal - only show device name to user
            addAlert('feedback_failed', payload.deviceId, `Failed to deliver feedback to ${device?.name || 'device'}`);
        });

        // Handler: Embedding failure alerts (per FrontendWorkflowSpec §4AA(6))
        const unsubEmbeddingFailed = signalRService.onEmbeddingFailed((sourceDocumentId: string, error: string) => {
            addAlert('embedding_failed', undefined, `Source document indexing failed: ${error}`);
        });

        return () => {
            unsubStatus();
            unsubHandRaised();
            unsubDistributionFailed();
            unsubControlFailed();
            unsubFeedbackFailed();
            unsubEmbeddingFailed();
        };
    }, [addAlert]);

    const unacknowledgedHelpCount = handRaisedDeviceIds.size;

    const value: AlertContextValue = {
        alerts,
        handRaisedDeviceIds,
        addAlert,
        dismissAlert,
        acknowledgeHelp,
        acknowledgeAllHelp,
        setPairedDevices,
        unacknowledgedHelpCount
    };

    return (
        <AlertContext.Provider value={value}>
            {children}
        </AlertContext.Provider>
    );
}
