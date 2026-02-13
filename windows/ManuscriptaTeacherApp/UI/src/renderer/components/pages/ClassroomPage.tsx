/**
 * Classroom Page component.
 * Per WindowsAppStructureSpec §2B(1)(d)(i) (placed in pages/).
 * Replaces legacy ClassroomControl component.
 */

import React, { useState, useMemo, useEffect, useCallback, useRef } from 'react';
import { Card } from '../common/Card';
import { useAppContext } from '../../state/AppContext';
import { useAlertContext } from '../../state/AlertContext';
import signalRService from '../../services/signalr/SignalRService';
import type {
    PairedDeviceEntity,
    DeviceStatusEntity,
    DeviceStatus,
    ReMarkableDeviceEntity
} from '../../models';
import { RenameDeviceModal } from '../modals/RenameDeviceModal';
import { RmapiInstallModal } from '../modals/RmapiInstallModal';
import { ReMarkablePairingModal } from '../modals/ReMarkablePairingModal';

// Icons
const TabletIcon = () => (
    <svg xmlns="http://www.w3.org/2000/svg" className="h-8 w-8 text-gray-400" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={1.5}>
        <path strokeLinecap="round" strokeLinejoin="round" d="M12 18h.01M8 21h8a2 2 0 002-2V5a2 2 0 00-2-2H8a2 2 0 00-2 2v14a2 2 0 002 2z" />
    </svg>
);

// §5F(5): distinct visual indicator for reMarkable devices
const EReaderIcon = () => (
    <svg xmlns="http://www.w3.org/2000/svg" className="h-8 w-8 text-gray-500" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={1.5}>
        <path strokeLinecap="round" strokeLinejoin="round" d="M12 6.042A8.967 8.967 0 006 3.75c-1.052 0-2.062.18-3 .512v14.25A8.987 8.987 0 016 18c2.305 0 4.408.867 6 2.292m0-14.25a8.966 8.966 0 016-2.292c1.052 0 2.062.18 3 .512v14.25A8.987 8.987 0 0018 18a8.967 8.967 0 00-6 2.292m0-14.25v14.25" />
    </svg>
);

const HandRaisedIcon = () => (
    <svg xmlns="http://www.w3.org/2000/svg" className="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
        <path strokeLinecap="round" strokeLinejoin="round" d="M7 11.5V14m0-2.5v-6a1.5 1.5 0 113 0m-3 6a1.5 1.5 0 00-3 0v2a7.5 7.5 0 0015 0v-5a1.5 1.5 0 00-3 0m-6-3V11m0-5.5v-1a1.5 1.5 0 013 0v1m0 0V11m0-5.5a1.5 1.5 0 013 0v3m0 0V11" />
    </svg>
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

export const ClassroomPage: React.FC = () => {
    // Data for materials - from AppContext
    const { unitCollections, getUnitsForCollection, getLessonsForUnit, getMaterialsForLesson } = useAppContext();

    // Alert context - global alert state per FrontendWorkflowSpec §5D(1)
    const {
        addAlert,
        acknowledgeHelp,
        setPairedDevices,
        handRaisedDeviceIds
    } = useAlertContext();

    // Material selection state
    const [selectedUnitCollectionId, setSelectedUnitCollectionId] = useState<string>('');
    const [selectedUnitId, setSelectedUnitId] = useState<string>('');
    const [selectedLessonId, setSelectedLessonId] = useState<string>('');
    const [selectedMaterialId, setSelectedMaterialId] = useState<string>('');

    // Device state - from SignalR backend
    const [devices, setDevices] = useState<PairedDeviceEntity[]>([]);
    const [deviceStatuses, setDeviceStatuses] = useState<Map<string, DeviceStatusEntity>>(new Map());
    const [selectedDeviceIds, setSelectedDeviceIds] = useState<string[]>([]);

    // reMarkable device state - per §5F
    const [remarkableDevices, setRemarkableDevices] = useState<ReMarkableDeviceEntity[]>([]);
    const [selectedRemarkableIds, setSelectedRemarkableIds] = useState<string[]>([]);

    // Stable ref for remarkableDevices — avoids circular useEffect dependency
    const remarkableDevicesRef = useRef(remarkableDevices);
    remarkableDevicesRef.current = remarkableDevices;

    // Pairing state
    const [isPairing, setIsPairing] = useState(false);

    // Modal states
    const [isRenameModalOpen, setIsRenameModalOpen] = useState(false);
    const [isRmapiInstallModalOpen, setIsRmapiInstallModalOpen] = useState(false);
    const [isRemarkablePairingModalOpen, setIsRemarkablePairingModalOpen] = useState(false);

    // Deployment state - per device progress
    const [deployingDevices, setDeployingDevices] = useState<Set<string>>(new Set());

    // Loading state
    const [isLoading, setIsLoading] = useState(true);

    // Update alert context with devices for name resolution
    useEffect(() => {
        setPairedDevices(devices);
    }, [devices, setPairedDevices]);

    // Refresh device grid - per FrontendWorkflowSpec §5A(3A)
    const refreshDeviceGrid = useCallback(async () => {
        try {
            const [pairedDevices, statuses, rmDevices] = await Promise.all([
                signalRService.getAllPairedDevices(),
                signalRService.getAllDeviceStatuses(),
                signalRService.getAllReMarkableDevices()
            ]);
            setDevices(pairedDevices);
            setDeviceStatuses(new Map(statuses.map(s => [s.deviceId, s])));
            setRemarkableDevices(rmDevices);
        } catch (error) {
            console.error('Failed to refresh device grid:', error);
            addAlert('control_failed', undefined, 'Failed to refresh device data');
        }
    }, [addAlert]);

    // Debounced refresh to avoid rapid successive API calls
    const debounceTimeoutRef = useRef<NodeJS.Timeout | null>(null);
    const debouncedRefresh = useCallback(() => {
        if (debounceTimeoutRef.current) {
            clearTimeout(debounceTimeoutRef.current);
        }
        debounceTimeoutRef.current = setTimeout(() => {
            refreshDeviceGrid();
        }, 150);
    }, [refreshDeviceGrid]);

    // Load initial device data and register handlers
    useEffect(() => {
        const loadInitialData = async () => {
            try {
                await refreshDeviceGrid();
            } finally {
                setIsLoading(false);
            }
        };

        loadInitialData();

        // Register SignalR handlers for device status updates (non-alert)
        const unsubStatus = signalRService.onDeviceStatusUpdate((status) => {
            setDeviceStatuses(prev => new Map(prev).set(status.deviceId, status));
            // Note: Alert for disconnection is handled by AlertContext
        });

        // Per FrontendWorkflowSpec §5A(3A)(c)(i) and NetworkingAPISpec §2(1)(a)(iii):
        // Use notification as signal to refresh, do not directly use payload
        const unsubPaired = signalRService.onDevicePaired(() => {
            debouncedRefresh();
        });

        // Handler for distribution completion - clear deploying state
        // Payload includes deviceId and materialId per API Contract §3.6.2
        const unsubDistributionFailed = signalRService.onDistributionFailed((payload: { deviceId: string; materialId: string }) => {
            setDeployingDevices(prev => {
                const next = new Set(prev);
                next.delete(payload.deviceId);
                return next;
            });
            // Note: Alert for distribution failure is handled by AlertContext
        });

        // §5F(3): subscribe to reMarkable auth invalid events
        const unsubRemarkableAuth = signalRService.onReMarkableAuthInvalid((deviceId) => {
            const device = remarkableDevicesRef.current.find(d => d.deviceId === deviceId);
            addAlert('control_failed', undefined,
                `reMarkable device "${device?.name ?? deviceId}" requires re-authentication. Please re-pair the device.`);
        });

        return () => {
            unsubStatus();
            unsubPaired();
            unsubDistributionFailed();
            unsubRemarkableAuth();
            if (debounceTimeoutRef.current) {
                clearTimeout(debounceTimeoutRef.current);
            }
        };
    }, [addAlert, refreshDeviceGrid, debouncedRefresh]);

    // Get status for a device
    const getDeviceStatus = useCallback((deviceId: string): DeviceStatus => {
        return deviceStatuses.get(deviceId)?.status || 'DISCONNECTED';
    }, [deviceStatuses]);

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

    // Device Selection (Android)
    const toggleDeviceSelection = (deviceId: string) => {
        setSelectedDeviceIds(prev =>
            prev.includes(deviceId)
                ? prev.filter(id => id !== deviceId)
                : [...prev, deviceId]
        );
    };

    // reMarkable device selection
    const toggleRemarkableSelection = (deviceId: string) => {
        setSelectedRemarkableIds(prev =>
            prev.includes(deviceId)
                ? prev.filter(id => id !== deviceId)
                : [...prev, deviceId]
        );
    };

    const totalDeviceCount = devices.length + remarkableDevices.length;
    const totalSelectedCount = selectedDeviceIds.length + selectedRemarkableIds.length;
    const selectAll = () => {
        setSelectedDeviceIds(devices.map(d => d.deviceId));
        setSelectedRemarkableIds(remarkableDevices.map(d => d.deviceId));
    };
    const deselectAll = () => {
        setSelectedDeviceIds([]);
        setSelectedRemarkableIds([]);
    };

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

    // §5G(4): mixed deployment — Android + reMarkable
    const handleDeployMaterial = async () => {
        if (!selectedMaterialId || totalSelectedCount === 0) return;

        const hasRemarkableTargets = selectedRemarkableIds.length > 0;
        let rmapiAvailable = true;
        if (hasRemarkableTargets) {
            rmapiAvailable = await ensureRmapiAvailable();
            if (!rmapiAvailable && selectedDeviceIds.length === 0) return;
        }

        const allTargetIds = rmapiAvailable
            ? [...selectedDeviceIds, ...selectedRemarkableIds]
            : [...selectedDeviceIds];
        setDeployingDevices(new Set(allTargetIds));

        try {
            const promises: Promise<void>[] = [];

            // Deploy to Android devices
            if (selectedDeviceIds.length > 0) {
                promises.push(signalRService.deployMaterial(selectedMaterialId, selectedDeviceIds));
            }

            // Deploy to reMarkable devices per §5G(2)
            if (rmapiAvailable && selectedRemarkableIds.length > 0) {
                promises.push(signalRService.deployMaterialToReMarkable(selectedMaterialId, selectedRemarkableIds));
            }

            await Promise.all(promises);

            if (rmapiAvailable && selectedRemarkableIds.length > 0) {
                // §5G(2)(d): inform user about sync
                addAlert('success', undefined, 'Material uploaded to reMarkable cloud. Devices will receive it on next sync.');
            }
        } catch (error) {
            console.error('Deploy failed:', error);
            addAlert('distribution_failed', undefined, 'Failed to deploy material');
        } finally {
            setDeployingDevices(new Set());
        }
    };

    const handleUnpairDevices = async () => {
        if (selectedDeviceIds.length === 0 && selectedRemarkableIds.length === 0) return;
        try {
            // Unpair Android devices
            if (selectedDeviceIds.length > 0) {
                await signalRService.unpairDevices(selectedDeviceIds);
            }
            // Unpair reMarkable devices per §5F(4)
            for (const rmId of selectedRemarkableIds) {
                await signalRService.unpairReMarkableDevice(rmId);
            }
            // Per FrontendWorkflowSpec §5A(3A)(c)(ii): refresh after unpair
            await refreshDeviceGrid();
            setSelectedDeviceIds([]);
            setSelectedRemarkableIds([]);
        } catch (error) {
            console.error('Unpair failed:', error);
            addAlert('control_failed', undefined, 'Failed to unpair devices');
        }
    };

    // Rename Handler — handles both Android and reMarkable
    const handleRenameDevice = async (newName: string) => {
        // Android device rename
        if (selectedDeviceIds.length === 1 && selectedRemarkableIds.length === 0) {
            const deviceId = selectedDeviceIds[0];
            const device = devices.find(d => d.deviceId === deviceId);
            if (!device) return;

            try {
                await signalRService.updatePairedDevice({ ...device, name: newName });
                setDevices(prev => prev.map(d => d.deviceId === deviceId ? { ...d, name: newName } : d));
                addAlert('success', undefined, `Device renamed to ${newName}`);
            } catch (error) {
                console.error('Rename failed:', error);
                throw error;
            }
        }
        // reMarkable device rename
        else if (selectedRemarkableIds.length === 1 && selectedDeviceIds.length === 0) {
            const deviceId = selectedRemarkableIds[0];
            const device = remarkableDevices.find(d => d.deviceId === deviceId);
            if (!device) return;

            try {
                await signalRService.updateReMarkableDevice({ ...device, name: newName });
                setRemarkableDevices(prev => prev.map(d => d.deviceId === deviceId ? { ...d, name: newName } : d));
                addAlert('success', undefined, `Device renamed to ${newName}`);
            } catch (error) {
                console.error('Rename failed:', error);
                throw error;
            }
        }
    };

    // §5E: rmapi availability check before reMarkable operations
    const ensureRmapiAvailable = async (): Promise<boolean> => {
        try {
            const available = await signalRService.checkRmapiAvailability();
            if (!available) {
                setIsRmapiInstallModalOpen(true);
                return false;
            }
            return true;
        } catch {
            addAlert('control_failed', undefined, 'Failed to check rmapi availability');
            return false;
        }
    };

    // §5F(1): initiate reMarkable pairing
    const handleRemarkablePairing = async () => {
        const available = await ensureRmapiAvailable();
        if (available) {
            setIsRemarkablePairingModalOpen(true);
        }
    };

    // Determine if only one device total is selected (for rename)
    const singleDeviceSelected = (selectedDeviceIds.length === 1 && selectedRemarkableIds.length === 0)
        || (selectedRemarkableIds.length === 1 && selectedDeviceIds.length === 0);

    // Get the name of the single selected device for rename modal
    const singleSelectedDeviceName = singleDeviceSelected
        ? (selectedDeviceIds.length === 1
            ? devices.find(d => d.deviceId === selectedDeviceIds[0])?.name
            : remarkableDevices.find(d => d.deviceId === selectedRemarkableIds[0])?.name)
        ?? ''
        : '';

    // §5F(6): check if any reMarkable devices are selected (to disable Android-only controls)
    const hasRemarkableInSelection = selectedRemarkableIds.length > 0;

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

    const hasSelection = totalSelectedCount > 0;

    if (isLoading) {
        return (
            <div className="flex items-center justify-center h-64">
                <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-brand-orange"></div>
            </div>
        );
    }

    return (
        <div>
            {/* Rename Modal */}
            {isRenameModalOpen && singleDeviceSelected && (
                <RenameDeviceModal
                    currentName={singleSelectedDeviceName}
                    onClose={() => setIsRenameModalOpen(false)}
                    onRename={handleRenameDevice}
                />
            )}

            {/* rmapi Install Modal — per §5E */}
            {isRmapiInstallModalOpen && (
                <RmapiInstallModal
                    onClose={() => setIsRmapiInstallModalOpen(false)}
                    onInstallComplete={() => {
                        setIsRmapiInstallModalOpen(false);
                        addAlert('success', undefined, 'rmapi installed successfully');
                    }}
                    installRmapi={() => signalRService.installRmapi()}
                />
            )}

            {/* reMarkable Pairing Modal — per §5F(2) */}
            {isRemarkablePairingModalOpen && (
                <ReMarkablePairingModal
                    onClose={() => setIsRemarkablePairingModalOpen(false)}
                    onPaired={() => {
                        setIsRemarkablePairingModalOpen(false);
                        refreshDeviceGrid();
                    }}
                    pairDevice={(name, code) => signalRService.pairReMarkableDevice(name, code)}
                />
            )}

            {/* Note: Alert UI (toasts, banners) is now rendered globally by GlobalAlerts component */}

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
                                disabled={!selectedMaterialId || totalDeviceCount === 0 || !hasSelection}
                            >
                                Deploy to Selected Devices {hasSelection ? `(${totalSelectedCount})` : ''}
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
                            {isPairing ? '⏹ Stop Pairing' : '📱 Pair Android'}
                        </button>

                        {/* §5F(1): distinct reMarkable pairing button */}
                        <button
                            className="bg-gray-700 hover:bg-gray-800 text-white font-sans font-medium py-2 px-4 rounded-md transition-colors shadow-sm"
                            onClick={handleRemarkablePairing}
                        >
                            📖 Pair reMarkable
                        </button>

                        {/* §5F(6): lock/unlock disabled when reMarkable devices selected */}
                        <div className="relative group">
                            <button
                                className="bg-brand-orange text-white hover:bg-brand-orange-dark font-sans font-medium py-2 px-4 rounded-md transition-colors shadow-sm disabled:bg-gray-300 disabled:cursor-not-allowed"
                                onClick={handleLockDevices}
                                disabled={devices.length === 0 || hasRemarkableInSelection}
                            >
                                {selectedDeviceIds.length > 0 ? `Lock (${selectedDeviceIds.length})` : 'Lock All'}
                            </button>
                            {hasRemarkableInSelection && (
                                <span className="hidden group-hover:block absolute -bottom-8 left-0 text-xs text-gray-500 whitespace-nowrap bg-white px-2 py-1 rounded shadow">
                                    Lock is not available for reMarkable devices
                                </span>
                            )}
                        </div>

                        <div className="relative group">
                            <button
                                className="bg-white border border-gray-300 text-text-body hover:border-brand-orange hover:text-brand-orange font-sans font-medium py-2 px-4 rounded-md transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
                                onClick={handleUnlockDevices}
                                disabled={devices.length === 0 || hasRemarkableInSelection}
                            >
                                {selectedDeviceIds.length > 0 ? `Unlock (${selectedDeviceIds.length})` : 'Unlock All'}
                            </button>
                            {hasRemarkableInSelection && (
                                <span className="hidden group-hover:block absolute -bottom-8 left-0 text-xs text-gray-500 whitespace-nowrap bg-white px-2 py-1 rounded shadow">
                                    Unlock is not available for reMarkable devices
                                </span>
                            )}
                        </div>

                        {singleDeviceSelected && (
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
                                Unpair ({totalSelectedCount})
                            </button>
                        )}
                    </div>

                    {isPairing && (
                        <div className="mt-4 flex items-center gap-2 text-brand-green">
                            <div className="animate-pulse h-3 w-3 bg-brand-green rounded-full"></div>
                            <span className="text-sm font-medium">Waiting for Android devices to connect...</span>
                        </div>
                    )}
                </Card>
            </div>

            {/* Grid Selection Info */}
            <div className='mb-6 bg-white p-4 rounded-lg flex justify-between items-center border border-gray-200 shadow-soft'>
                <p className='font-sans font-medium text-text-heading'>
                    {totalSelectedCount} / {totalDeviceCount} devices selected
                    {totalSelectedCount > 0 && (
                        <span className='text-sm text-gray-400 ml-2'>
                            ({selectedDeviceIds.length} Android, {selectedRemarkableIds.length} reMarkable)
                        </span>
                    )}
                </p>
                <div className='flex gap-4'>
                    <button onClick={selectAll} disabled={totalDeviceCount === 0} className='text-sm font-medium text-brand-orange hover:text-brand-orange-dark transition-colors disabled:text-gray-300'>Select All</button>
                    <button onClick={deselectAll} disabled={!hasSelection} className='text-sm font-medium text-gray-500 hover:text-gray-700 disabled:text-gray-300 disabled:cursor-not-allowed transition-colors'>Deselect All</button>
                </div>
            </div>

            {/* Device Grid */}
            {totalDeviceCount === 0 ? (
                <div className="text-center py-16 text-gray-500">
                    <TabletIcon />
                    <p className="mt-4 text-lg">No devices paired yet</p>
                </div>
            ) : (
                <div role="grid" className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5 xl:grid-cols-6 gap-6">
                    {/* Android devices */}
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

                    {/* §5F(5): reMarkable devices with distinct visual indicator */}
                    {remarkableDevices.map((device) => {
                        const isSelected = selectedRemarkableIds.includes(device.deviceId);
                        const isDeploying = deployingDevices.has(device.deviceId);

                        return (
                            <div
                                key={`rm-${device.deviceId}`}
                                role="gridcell"
                                aria-selected={isSelected}
                                tabIndex={0}
                                onClick={() => toggleRemarkableSelection(device.deviceId)}
                                onKeyDown={(e) => {
                                    if (e.key === ' ' || e.key === 'Enter') {
                                        e.preventDefault();
                                        toggleRemarkableSelection(device.deviceId);
                                    }
                                }}
                                className={`rounded-lg p-4 flex flex-col items-center justify-center text-center cursor-pointer transition-all duration-200 border relative
                  ${isSelected ? 'bg-gray-200 border-gray-400 ring-2 ring-gray-500 ring-offset-2 ring-offset-brand-cream' : 'bg-gray-50 border-gray-200'}
                  focus:outline-none focus:ring-2 ring-gray-500`}
                            >
                                {/* Deploying indicator */}
                                {isDeploying && (
                                    <div className="absolute top-2 right-2">
                                        <div className="animate-spin h-4 w-4 border-2 border-brand-orange border-t-transparent rounded-full"></div>
                                    </div>
                                )}

                                <EReaderIcon />

                                <p className="font-sans font-semibold text-text-heading mt-3 text-sm truncate max-w-full">
                                    {device.name}
                                </p>

                                <span className="text-xs font-semibold px-2 py-1 rounded-full mt-2 uppercase tracking-wide bg-gray-600 text-white">
                                    reMarkable
                                </span>
                            </div>
                        );
                    })}
                </div>
            )}

            <style>{`
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
