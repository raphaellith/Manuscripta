/**
 * Classroom Page component.
 * Per WindowsAppStructureSpec §2B(1)(d)(i) (placed in pages/).
 * Replaces legacy ClassroomControl component.
 */

import React, { useState, useMemo, useEffect, useCallback } from 'react';
import { Card } from '../common/Card';
import { useAppContext } from '../../state/AppContext';
import signalRService from '../../services/signalr/SignalRService';
import type {
    PairedDeviceEntity,
    DeviceStatusEntity,
    DeviceStatus
} from '../../models';
import { RenameDeviceModal } from '../modals/RenameDeviceModal';

// Icons
const TabletIcon = () => (
    <svg xmlns="http://www.w3.org/2000/svg" className="h-8 w-8 text-gray-400" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={1.5}>
        <path strokeLinecap="round" strokeLinejoin="round" d="M12 18h.01M8 21h8a2 2 0 002-2V5a2 2 0 00-2-2H8a2 2 0 00-2 2v14a2 2 0 002 2z" />
    </svg>
);

const HandRaisedIcon = () => (
    <svg xmlns="http://www.w3.org/2000/svg" className="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
        <path strokeLinecap="round" strokeLinejoin="round" d="M7 11.5V14m0-2.5v-6a1.5 1.5 0 113 0m-3 6a1.5 1.5 0 00-3 0v2a7.5 7.5 0 0015 0v-5a1.5 1.5 0 00-3 0m-6-3V11m0-5.5v-1a1.5 1.5 0 013 0v1m0 0V11m0-5.5a1.5 1.5 0 013 0v3m0 0V11" />
    </svg>
);

// Toast notification component for help requests
interface ToastProps {
    message: string;
    deviceId: string;
    deviceName: string;
    onDismiss: () => void;
    onAcknowledge?: () => void;
}

const HelpToast: React.FC<ToastProps> = ({ message, deviceName, onDismiss, onAcknowledge }) => (
    <div className="fixed top-40 right-4 z-50 animate-slide-in">
        <div className="bg-white rounded-lg shadow-2xl border-l-4 border-brand-orange p-4 flex items-start gap-4 max-w-sm">
            <div className="bg-brand-orange-light p-2 rounded-full animate-pulse">
                <HandRaisedIcon />
            </div>
            <div className="flex-1">
                <p className="font-sans font-semibold text-text-heading">Help Requested</p>
                <p className="font-sans text-sm text-gray-600 mt-1">{deviceName}: {message}</p>
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

const CheckCircleIcon = () => (
    <svg xmlns="http://www.w3.org/2000/svg" className="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
        <path strokeLinecap="round" strokeLinejoin="round" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
    </svg>
);

const SuccessToast: React.FC<ToastProps> = ({ message, onDismiss }) => (
    <div className="fixed top-40 right-4 z-50 animate-slide-in">
        <div className="bg-white rounded-lg shadow-2xl border-l-4 border-green-500 p-4 flex items-start gap-4 max-w-sm">
            <div className="bg-green-100 text-green-600 p-2 rounded-full">
                <CheckCircleIcon />
            </div>
            <div className="flex-1">
                <p className="font-sans font-semibold text-text-heading">Success</p>
                <p className="font-sans text-sm text-gray-600 mt-1">{message}</p>
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

// Status styling per device status
const statusStyles: Record<DeviceStatus, {
    cardBg: string;
    selectedCardBg: string;
    badge: string;
    ringColor: string;
    label: string;
}> = {
    ON_TASK: {
        cardBg: 'bg-blue-50 border-blue-100',
        selectedCardBg: 'bg-blue-100 border-blue-200',
        badge: 'bg-brand-blue text-white',
        ringColor: 'ring-brand-blue',
        label: 'On Task'
    },
    IDLE: {
        cardBg: 'bg-green-50 border-green-200',
        selectedCardBg: 'bg-green-100 border-green-300',
        badge: 'bg-brand-green text-white',
        ringColor: 'ring-brand-green',
        label: 'Idle'
    },

    LOCKED: {
        cardBg: 'bg-yellow-50 border-yellow-200',
        selectedCardBg: 'bg-yellow-100 border-yellow-300',
        badge: 'bg-yellow-600 text-white',
        ringColor: 'ring-yellow-500',
        label: 'Locked'
    },
    DISCONNECTED: {
        cardBg: 'bg-gray-100 border-gray-200',
        selectedCardBg: 'bg-gray-200 border-gray-300',
        badge: 'bg-gray-600 text-white',
        ringColor: 'ring-gray-400',
        label: 'Disconnected'
    },
};

const helpStatusStyle = {
    cardBg: 'bg-orange-50 border-orange-200',
    selectedCardBg: 'bg-orange-100 border-orange-300',
    badge: 'bg-brand-orange text-white',
    ringColor: 'ring-brand-orange',
    label: 'Needs Help'
};

// Alert types for various events
interface Alert {
    id: string;
    type: 'help' | 'disconnection' | 'distribution_failed' | 'control_failed' | 'feedback_failed' | 'success';
    deviceId?: string;
    deviceName?: string;
    message: string;
    timestamp: number;
}

export const ClassroomPage: React.FC = () => {
    // Data for materials - from AppContext
    const { unitCollections, units, getUnitsForCollection, getLessonsForUnit, getMaterialsForLesson } = useAppContext();

    // Material selection state
    const [selectedUnitCollectionId, setSelectedUnitCollectionId] = useState<string>('');
    const [selectedUnitId, setSelectedUnitId] = useState<string>('');
    const [selectedLessonId, setSelectedLessonId] = useState<string>('');
    const [selectedMaterialId, setSelectedMaterialId] = useState<string>('');

    // Device state - from SignalR backend
    const [devices, setDevices] = useState<PairedDeviceEntity[]>([]);
    const devicesRef = React.useRef(devices); // Ref to hold latest devices without triggering re-effects
    const [deviceStatuses, setDeviceStatuses] = useState<Map<string, DeviceStatusEntity>>(new Map());
    const [selectedDeviceIds, setSelectedDeviceIds] = useState<string[]>([]);

    useEffect(() => {
        devicesRef.current = devices;
    }, [devices]);

    // Pairing state
    const [isPairing, setIsPairing] = useState(false);

    // Rename Modal State
    const [isRenameModalOpen, setIsRenameModalOpen] = useState(false);

    // Deployment state - per device progress
    const [deployingDevices, setDeployingDevices] = useState<Set<string>>(new Set());

    // Alert/notification state
    const [alerts, setAlerts] = useState<Alert[]>([]);
    const [handRaisedDeviceIds, setHandRaisedDeviceIds] = useState<Set<string>>(new Set());

    // Loading state
    const [isLoading, setIsLoading] = useState(true);

    // Helper to add alerts - uses Ref to avoid dependency cycles
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
    }, []); // No dependencies needed thanks to ref

    // Load initial device data and register handlers
    useEffect(() => {
        const loadInitialData = async () => {
            try {
                const [pairedDevices, statuses] = await Promise.all([
                    signalRService.getAllPairedDevices(),
                    signalRService.getAllDeviceStatuses()
                ]);
                setDevices(pairedDevices);
                setDeviceStatuses(new Map(statuses.map(s => [s.deviceId, s])));
            } catch (error) {
                console.error('Failed to load classroom data:', error);
                addAlert('control_failed', undefined, 'Failed to load device data');
            } finally {
                setIsLoading(false);
            }
        };

        loadInitialData();

        // Register SignalR handlers
        const unsubStatus = signalRService.onDeviceStatusUpdate((status) => {
            setDeviceStatuses(prev => new Map(prev).set(status.deviceId, status));

            if (status.status === 'DISCONNECTED') {
                const device = devicesRef.current.find(d => d.deviceId === status.deviceId);
                addAlert('disconnection', status.deviceId, `${device?.name || 'Device'} disconnected`);
            }
        });

        const unsubPaired = signalRService.onDevicePaired((device) => {
            setDevices(prev => [...prev, device]);
            setDeviceStatuses(prev => new Map(prev).set(device.deviceId, {
                deviceId: device.deviceId,
                status: 'IDLE',
                batteryLevel: 100,
                currentMaterialId: '',
                studentView: '',
                timestamp: Date.now()
            }));
        });

        const unsubHandRaised = signalRService.onHandRaised((deviceId) => {
            setHandRaisedDeviceIds(prev => new Set(prev).add(deviceId));
            const device = devicesRef.current.find(d => d.deviceId === deviceId);
            addAlert('help', deviceId, `${device?.name || 'Device'} is requesting help`);
        });

        const unsubDistributionFailed = signalRService.onDistributionFailed((deviceId) => {
            setDeployingDevices(prev => {
                const next = new Set(prev);
                next.delete(deviceId);
                return next;
            });
            const device = devicesRef.current.find(d => d.deviceId === deviceId);
            addAlert('distribution_failed', deviceId, `Failed to deploy material to ${device?.name || 'device'}`);
        });

        const unsubControlFailed = signalRService.onRemoteControlFailed((payload) => {
            if (payload && payload.deviceId) {
                const device = devicesRef.current.find(d => d.deviceId === payload.deviceId);
                const name = device ? device.name : payload.deviceId.substring(0, 8);
                addAlert('control_failed', payload.deviceId, `Remote control command '${payload.command}' failed for ${name} (Timeout)`);
            } else {
                addAlert('control_failed', undefined, 'Remote control command failed');
            }
        });

        const unsubFeedbackFailed = signalRService.onFeedbackDeliveryFailed((deviceId) => {
            const device = devicesRef.current.find(d => d.deviceId === deviceId);
            addAlert('feedback_failed', deviceId, `Failed to deliver feedback to ${device?.name || 'device'}`);
        });

        return () => {
            unsubStatus();
            unsubPaired();
            unsubHandRaised();
            unsubDistributionFailed();
            unsubControlFailed();
            unsubFeedbackFailed();
        };
    }, [addAlert]); // Only run once (addAlert is stable)

    // Get status for a device
    const getDeviceStatus = useCallback((deviceId: string): DeviceStatus => {
        return deviceStatuses.get(deviceId)?.status || 'DISCONNECTED';
    }, [deviceStatuses]);

    // Count unacknowledged help
    const unacknowledgedHelpCount = useMemo(() => {
        return handRaisedDeviceIds.size;
    }, [handRaisedDeviceIds]);

    // Pairing toggle
    const handleTogglePairing = async () => {
        try {
            if (isPairing) {
                await signalRService.stopPairing();
                setIsPairing(false);
            } else {
                await signalRService.pairDevices();
                setIsPairing(true);
            }
        } catch (error) {
            console.error('Pairing toggle failed:', error);
            addAlert('control_failed', undefined, 'Failed to toggle pairing mode');
        }
    };

    // Device Selection
    const toggleDeviceSelection = (deviceId: string) => {
        setSelectedDeviceIds(prev =>
            prev.includes(deviceId)
                ? prev.filter(id => id !== deviceId)
                : [...prev, deviceId]
        );
    };

    const selectAll = () => setSelectedDeviceIds(devices.map(d => d.deviceId));
    const deselectAll = () => setSelectedDeviceIds([]);

    // Device Actions
    const handleLockDevices = async () => {
        const targetIds = selectedDeviceIds.length > 0 ? selectedDeviceIds : devices.map(d => d.deviceId);
        try {
            await signalRService.lockDevices(targetIds);
        } catch (error) {
            console.error('Lock failed:', error);
            addAlert('control_failed', undefined, 'Failed to lock devices');
        }
    };

    const handleUnlockDevices = async () => {
        const targetIds = selectedDeviceIds.length > 0 ? selectedDeviceIds : devices.map(d => d.deviceId);
        try {
            await signalRService.unlockDevices(targetIds);
        } catch (error) {
            console.error('Unlock failed:', error);
            addAlert('control_failed', undefined, 'Failed to unlock devices');
        }
    };

    const handleDeployMaterial = async () => {
        if (!selectedMaterialId || selectedDeviceIds.length === 0) return;
        const targetIds = selectedDeviceIds;
        setDeployingDevices(new Set(targetIds));
        try {
            await signalRService.deployMaterial(selectedMaterialId, targetIds);
        } catch (error) {
            console.error('Deploy failed:', error);
            addAlert('distribution_failed', undefined, 'Failed to deploy material');
            setDeployingDevices(new Set());
        }
    };

    const handleUnpairDevices = async () => {
        if (selectedDeviceIds.length === 0) return;
        try {
            await signalRService.unpairDevices(selectedDeviceIds);
            setDevices(prev => prev.filter(d => !selectedDeviceIds.includes(d.deviceId)));
            setSelectedDeviceIds([]);
        } catch (error) {
            console.error('Unpair failed:', error);
            addAlert('control_failed', undefined, 'Failed to unpair devices');
        }
    };

    // Help Acknowledgement
    const acknowledgeHelp = (deviceId: string) => {
        setHandRaisedDeviceIds(prev => {
            const next = new Set(prev);
            next.delete(deviceId);
            return next;
        });
        setAlerts(prev => prev.filter(a => !(a.type === 'help' && a.deviceId === deviceId)));
    };

    const dismissAlert = (alertId: string) => {
        setAlerts(prev => prev.filter(a => a.id !== alertId));
    };

    // Rename Handler
    const handleRenameDevice = async (newName: string) => {
        if (selectedDeviceIds.length !== 1) return;
        const deviceId = selectedDeviceIds[0];
        const device = devices.find(d => d.deviceId === deviceId);
        if (!device) return;

        try {
            await signalRService.updatePairedDevice({
                ...device,
                name: newName
            });
            // Update local state immediately for responsiveness
            setDevices(prev => prev.map(d => d.deviceId === deviceId ? { ...d, name: newName } : d));
            addAlert('success', undefined, `Device renamed to ${newName}`);
        } catch (error) {
            console.error('Rename failed:', error);
            throw error; // Let modal handle error display
        }
    };

    // Material Selection Logic
    const unitsInSelectedCollection = useMemo(() => {
        if (!selectedUnitCollectionId) return [];
        return getUnitsForCollection(selectedUnitCollectionId);
    }, [selectedUnitCollectionId, getUnitsForCollection]);

    const foldersInSelectedUnit = useMemo(() => {
        if (!selectedUnitId) return [];
        return getLessonsForUnit(selectedUnitId);
    }, [selectedUnitId, getLessonsForUnit]);

    const contentInSelectedFolder = useMemo(() => {
        if (!selectedLessonId) return [];
        // Only show deployed materials? Spec doesn't strictly implementing status check for deployment, 
        // assuming all materials in library can be deployed.
        // The previous implementation filtered by 'Deployed' status which might be legacy logic.
        // We allow deploying any material.
        return getMaterialsForLesson(selectedLessonId);
    }, [selectedLessonId, getMaterialsForLesson]);

    const handleCollectionChange = (e: React.ChangeEvent<HTMLSelectElement>) => {
        setSelectedUnitCollectionId(e.target.value);
        setSelectedUnitId('');
        setSelectedLessonId('');
        setSelectedMaterialId('');
    };

    const handleUnitChange = (e: React.ChangeEvent<HTMLSelectElement>) => {
        setSelectedUnitId(e.target.value);
        setSelectedLessonId('');
        setSelectedMaterialId('');
    };

    const handleFolderChange = (e: React.ChangeEvent<HTMLSelectElement>) => {
        setSelectedLessonId(e.target.value);
        setSelectedMaterialId('');
    };

    const hasSelection = selectedDeviceIds.length > 0;

    if (isLoading) {
        return (
            <div className="flex items-center justify-center h-64">
                <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-brand-orange"></div>
            </div>
        );
    }

    return (
        <div>
            {/* Alert toasts */}
            {alerts.filter(a => a.type === 'help').slice(0, 1).map(alert => (
                <HelpToast
                    key={alert.id}
                    message={alert.message}
                    deviceId={alert.deviceId || ''}
                    deviceName={alert.deviceName || 'Unknown Device'}
                    onDismiss={() => dismissAlert(alert.id)}
                    onAcknowledge={() => alert.deviceId && acknowledgeHelp(alert.deviceId)}
                />
            ))}

            {/* Success toasts */}
            {alerts.filter(a => a.type === 'success').slice(0, 1).map(alert => (
                <SuccessToast
                    key={alert.id}
                    message={alert.message}
                    deviceId={alert.deviceId || ''}
                    deviceName={alert.deviceName || 'System'}
                    onDismiss={() => dismissAlert(alert.id)}
                />
            ))}

            {/* Rename Modal */}
            {isRenameModalOpen && selectedDeviceIds.length === 1 && (
                <RenameDeviceModal
                    currentName={devices.find(d => d.deviceId === selectedDeviceIds[0])?.name || ''}
                    onClose={() => setIsRenameModalOpen(false)}
                    onRename={handleRenameDevice}
                />
            )}

            {/* Help banner */}
            {unacknowledgedHelpCount > 0 && (
                <div className="mb-6 bg-brand-orange-light border border-brand-orange rounded-lg p-4 flex items-center gap-4 animate-pulse-slow">
                    <div className="bg-brand-orange text-white p-2 rounded-full">
                        <HandRaisedIcon />
                    </div>
                    <div className="flex-1">
                        <p className="font-sans font-semibold text-brand-orange-dark">
                            {unacknowledgedHelpCount} student{unacknowledgedHelpCount > 1 ? 's' : ''} requesting help
                        </p>
                    </div>
                    <button
                        onClick={() => {
                            Array.from(handRaisedDeviceIds).forEach(id => acknowledgeHelp(id));
                        }}
                        className="px-4 py-2 bg-brand-orange text-white text-sm font-medium rounded-md hover:bg-brand-orange-dark transition-colors"
                    >
                        Acknowledge All
                    </button>
                </div>
            )}

            {/* Error alerts */}
            {alerts.filter(a => a.type !== 'help').slice(0, 3).map(alert => (
                <div key={alert.id} className="mb-4 bg-red-50 border border-red-200 rounded-lg p-3 flex items-center gap-3">
                    <span className="text-red-600">⚠️</span>
                    <span className="flex-1 text-sm text-red-800">{alert.message}</span>
                    <button onClick={() => dismissAlert(alert.id)} className="text-red-600 hover:text-red-800">✕</button>
                </div>
            ))}

            <div className="grid grid-cols-1 md:grid-cols-2 gap-8 mb-8">
                {/* Deploy Material Card */}
                <Card className="border-t-4 border-t-brand-orange">
                    <h4 className="font-sans font-semibold text-xl text-text-heading mb-6">Deploy Material</h4>
                    <div className='flex flex-col space-y-5'>

                        <div className="flex flex-col sm:flex-row gap-4">
                            <select
                                value={selectedUnitCollectionId}
                                onChange={handleCollectionChange}
                                className="w-full p-3 bg-white text-text-body font-sans rounded-lg border border-gray-200 focus:border-brand-orange focus:ring-1 focus:ring-brand-orange focus:outline-none flex-grow"
                            >
                                <option value="">-- Select Collection --</option>
                                {unitCollections.map(collection => (
                                    <option key={collection.id} value={collection.id}>{collection.title}</option>
                                ))}
                            </select>

                            <select
                                value={selectedUnitId}
                                onChange={handleUnitChange}
                                disabled={!selectedUnitCollectionId || unitsInSelectedCollection.length === 0}
                                className="w-full p-3 bg-white text-text-body font-sans rounded-lg border border-gray-200 focus:border-brand-orange focus:ring-1 focus:ring-brand-orange focus:outline-none flex-grow disabled:opacity-50 disabled:cursor-not-allowed"
                            >
                                <option value="">-- Select Unit --</option>
                                {unitsInSelectedCollection.map(unit => (
                                    <option key={unit.id} value={unit.id}>{unit.title}</option>
                                ))}
                            </select>
                        </div>

                        <div className="flex flex-col sm:flex-row gap-4 items-center">
                            <select
                                value={selectedLessonId}
                                onChange={handleFolderChange}
                                disabled={!selectedUnitId || foldersInSelectedUnit.length === 0}
                                className="w-full p-3 bg-white text-text-body font-sans rounded-lg border border-gray-200 focus:border-brand-orange focus:ring-1 focus:ring-brand-orange focus:outline-none flex-grow disabled:opacity-50 disabled:cursor-not-allowed"
                            >
                                <option value="">-- Select Lesson --</option>
                                {foldersInSelectedUnit.map(folder => (
                                    <option key={folder.id} value={folder.id}>{folder.title}</option>
                                ))}
                            </select>

                            <select
                                value={selectedMaterialId}
                                onChange={(e) => setSelectedMaterialId(e.target.value)}
                                disabled={!selectedLessonId || contentInSelectedFolder.length === 0}
                                className="w-full p-3 bg-white text-text-body font-sans rounded-lg border border-gray-200 focus:border-brand-orange focus:ring-1 focus:ring-brand-orange focus:outline-none flex-grow disabled:opacity-50 disabled:cursor-not-allowed"
                            >
                                <option value="">-- Select Content --</option>
                                {contentInSelectedFolder.map(item => (
                                    <option key={item.id} value={item.id}>{item.title} ({item.materialType})</option>
                                ))}
                            </select>

                            <button
                                className="bg-brand-orange hover:bg-brand-orange-dark text-white font-sans font-medium py-3 px-6 rounded-md transition-colors shadow-sm disabled:bg-gray-300 disabled:cursor-not-allowed flex-shrink-0 w-full sm:w-auto"
                                onClick={handleDeployMaterial}
                                disabled={!selectedMaterialId || devices.length === 0 || !hasSelection}
                            >
                                Deploy to Selected Devices {hasSelection ? `(${selectedDeviceIds.length})` : ''}
                            </button>
                        </div>
                    </div>
                </Card>

                {/* Device Controls Card */}
                <Card className="border-t-4 border-t-brand-blue">
                    <h4 className="font-sans font-semibold text-xl text-text-heading mb-6">Device Controls</h4>
                    <div className='flex flex-wrap gap-4'>
                        <button
                            className={`${isPairing ? 'bg-red-500 hover:bg-red-600' : 'bg-brand-green hover:bg-brand-green-dark'} text-white font-sans font-medium py-2 px-4 rounded-md transition-colors shadow-sm`}
                            onClick={handleTogglePairing}
                        >
                            {isPairing ? '⏹ Stop Pairing' : '📱 Start Pairing'}
                        </button>

                        <button
                            className="bg-brand-orange text-white hover:bg-brand-orange-dark font-sans font-medium py-2 px-4 rounded-md transition-colors shadow-sm"
                            onClick={handleLockDevices}
                            disabled={devices.length === 0}
                        >
                            {hasSelection ? `Lock (${selectedDeviceIds.length})` : 'Lock All'}
                        </button>


                        <button
                            className="bg-white border border-gray-300 text-text-body hover:border-brand-orange hover:text-brand-orange font-sans font-medium py-2 px-4 rounded-md transition-colors"
                            onClick={handleUnlockDevices}
                            disabled={devices.length === 0}
                        >
                            {hasSelection ? `Unlock (${selectedDeviceIds.length})` : 'Unlock All'}
                        </button>

                        {selectedDeviceIds.length === 1 && (
                            <button
                                className="bg-white border border-gray-300 text-text-body hover:border-brand-orange hover:text-brand-orange font-sans font-medium py-2 px-4 rounded-md transition-colors"
                                onClick={() => setIsRenameModalOpen(true)}
                            >
                                Rename
                            </button>
                        )}

                        {hasSelection && (
                            <button
                                className="bg-red-100 text-red-600 hover:bg-red-200 font-sans font-medium py-2 px-4 rounded-md transition-colors"
                                onClick={handleUnpairDevices}
                            >
                                Unpair ({selectedDeviceIds.length})
                            </button>
                        )}
                    </div>

                    {isPairing && (
                        <div className="mt-4 flex items-center gap-2 text-brand-green">
                            <div className="animate-pulse h-3 w-3 bg-brand-green rounded-full"></div>
                            <span className="text-sm font-medium">Waiting for devices to connect...</span>
                        </div>
                    )}
                </Card>
            </div>

            {/* Grid Selection Info */}
            <div className='mb-6 bg-white p-4 rounded-lg flex justify-between items-center border border-gray-200 shadow-soft'>
                <p className='font-sans font-medium text-text-heading'>
                    {selectedDeviceIds.length} / {devices.length} devices selected
                </p>
                <div className='flex gap-4'>
                    <button onClick={selectAll} disabled={devices.length === 0} className='text-sm font-medium text-brand-orange hover:text-brand-orange-dark transition-colors disabled:text-gray-300'>Select All</button>
                    <button onClick={deselectAll} disabled={!hasSelection} className='text-sm font-medium text-gray-500 hover:text-gray-700 disabled:text-gray-300 disabled:cursor-not-allowed transition-colors'>Deselect All</button>
                </div>
            </div>

            {/* Device Grid */}
            {devices.length === 0 ? (
                <div className="text-center py-16 text-gray-500">
                    <TabletIcon />
                    <p className="mt-4 text-lg">No devices paired yet</p>
                </div>
            ) : (
                <div role="grid" className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5 xl:grid-cols-6 gap-6">
                    {devices.map((device) => {
                        const status = getDeviceStatus(device.deviceId);
                        const isSelected = selectedDeviceIds.includes(device.deviceId);
                        const isNeedingHelp = handRaisedDeviceIds.has(device.deviceId);
                        const effectiveStyle = isNeedingHelp ? helpStatusStyle : statusStyles[status];
                        const backgroundClass = isSelected ? effectiveStyle.selectedCardBg : effectiveStyle.cardBg;
                        const isDeploying = deployingDevices.has(device.deviceId);

                        return (
                            <div
                                key={device.deviceId}
                                role="gridcell"
                                aria-selected={isSelected}
                                tabIndex={0}
                                onClick={() => toggleDeviceSelection(device.deviceId)}
                                onKeyDown={(e) => {
                                    if (e.key === ' ' || e.key === 'Enter') {
                                        e.preventDefault();
                                        toggleDeviceSelection(device.deviceId);
                                    }
                                }}
                                className={`rounded-lg p-4 flex flex-col items-center justify-center text-center cursor-pointer transition-all duration-200 border
                  ${backgroundClass} ${isSelected ? `ring-2 ${effectiveStyle.ringColor} ring-offset-2 ring-offset-brand-cream` : ''} 
                  ${isNeedingHelp ? 'animate-pulse-border' : ''}
                  focus:outline-none focus:ring-2 ${effectiveStyle.ringColor}`}
                            >
                                {/* Deploying indicator */}
                                {isDeploying && (
                                    <div className="absolute top-2 right-2">
                                        <div className="animate-spin h-4 w-4 border-2 border-brand-orange border-t-transparent rounded-full"></div>
                                    </div>
                                )}

                                {isNeedingHelp ? (
                                    <div className="text-brand-orange animate-bounce-slow">
                                        <HandRaisedIcon />
                                    </div>
                                ) : (
                                    <TabletIcon />
                                )}

                                <p className="font-sans font-semibold text-text-heading mt-3 text-sm truncate max-w-full">
                                    {device.name}
                                </p>

                                <span className={`text-xs font-semibold px-2 py-1 rounded-full mt-2 uppercase tracking-wide ${effectiveStyle.badge}`}>
                                    {effectiveStyle.label}
                                </span>

                                {isNeedingHelp && (
                                    <button
                                        className="mt-2 text-xs text-brand-orange hover:text-brand-orange-dark font-medium"
                                        onClick={(e) => { e.stopPropagation(); acknowledgeHelp(device.deviceId); }}
                                    >
                                        Acknowledge
                                    </button>
                                )}
                            </div>
                        );
                    })}
                </div>
            )}

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
        
        @keyframes bounce-slow {
          0%, 100% { transform: translateY(0); }
          50% { transform: translateY(-4px); }
        }
        .animate-bounce-slow { animation: bounce-slow 1s ease-in-out infinite; }
        
        @keyframes pulse-border {
          0%, 100% { box-shadow: 0 0 0 0 rgba(237, 135, 51, 0.4); }
          50% { box-shadow: 0 0 0 8px rgba(237, 135, 51, 0); }
        }
        .animate-pulse-border { animation: pulse-border 2s ease-in-out infinite; }
      `}</style>
        </div>
    );
};
