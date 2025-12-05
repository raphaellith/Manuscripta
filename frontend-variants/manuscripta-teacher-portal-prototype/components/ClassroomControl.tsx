
import React, { useState, useMemo, useEffect } from 'react';
import { ContentHeader } from './ContentHeader';
import { Card } from './Card';
import type { ContentItem, Unit, LessonFolder, DeviceStatus, TabletAccessibilitySettings, Tablet } from '../types';

interface ClassroomControlProps {
    units: Unit[];
    lessonFolders: LessonFolder[];
    contentItems: ContentItem[];
}

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

// Toast notification component
interface ToastProps {
  message: string;
  tabletId: number;
  onDismiss: () => void;
  onAcknowledge: () => void;
}

const HelpToast: React.FC<ToastProps> = ({ message, tabletId, onDismiss, onAcknowledge }) => (
  <div className="fixed top-40 right-4 z-50 animate-slide-in">
    <div className="bg-white rounded-lg shadow-2xl border-l-4 border-brand-orange p-4 flex items-start gap-4 max-w-sm">
      <div className="bg-brand-orange-light p-2 rounded-full animate-pulse">
        <HandRaisedIcon />
      </div>
      <div className="flex-1">
        <p className="font-sans font-semibold text-text-heading">Help Requested</p>
        <p className="font-sans text-sm text-gray-600 mt-1">{message}</p>
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

// Per-tablet accessibility settings modal
interface TabletSettingsModalProps {
  tablet: Tablet;
  onClose: () => void;
  onSave: (tabletId: number, settings: TabletAccessibilitySettings) => void;
}

const TabletSettingsModal: React.FC<TabletSettingsModalProps> = ({ tablet, onClose, onSave }) => {
  const [settings, setSettings] = useState<TabletAccessibilitySettings>(tablet.accessibility);

  const handleToggle = (key: keyof TabletAccessibilitySettings) => {
    setSettings(prev => ({ ...prev, [key]: !prev[key] }));
  };

  const ToggleSwitch = ({ label, description, value, onToggle }: { label: string; description: string; value: boolean; onToggle: () => void }) => (
    <div className="flex justify-between items-center py-4 border-b border-gray-100 last:border-b-0">
      <div>
        <strong className="text-sm font-sans font-medium text-text-heading">{label}</strong>
        <p className="text-xs text-gray-500 mt-0.5 font-sans">{description}</p>
      </div>
      <button
        onClick={onToggle}
        className={`w-12 h-6 rounded-full p-0.5 transition-colors duration-300 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-brand-orange ${value ? 'bg-brand-orange' : 'bg-gray-300'}`}
      >
        <div className={`w-5 h-5 rounded-full bg-white shadow-sm transform transition-transform duration-300 ${value ? 'translate-x-6' : 'translate-x-0'}`} />
      </button>
    </div>
  );

  return (
    <div className="fixed inset-0 bg-text-heading/20 backdrop-blur-sm flex items-center justify-center z-50 p-4">
      <div className="bg-white rounded-lg shadow-2xl w-full max-w-md animate-fade-in-up">
        <div className="p-6 border-b border-gray-100">
          <h3 className="text-xl font-serif text-text-heading">
            Tablet {tablet.id} Settings
          </h3>
          <p className="text-sm text-gray-500 mt-1">Configure accessibility for this device</p>
        </div>
        
        <div className="p-6">
          <ToggleSwitch
            label="Text-to-Speech"
            description="Read content aloud to the student"
            value={settings.textToSpeech}
            onToggle={() => handleToggle('textToSpeech')}
          />
          <ToggleSwitch
            label="AI Summary"
            description="Show simplified AI-generated summaries"
            value={settings.aiSummary}
            onToggle={() => handleToggle('aiSummary')}
          />
          <ToggleSwitch
            label="Animated Avatar"
            description="Display an animated helper avatar"
            value={settings.animatedAvatar}
            onToggle={() => handleToggle('animatedAvatar')}
          />
        </div>
        
        <div className="flex gap-3 p-6 border-t border-gray-100 bg-gray-50 rounded-b-lg">
          <button 
            onClick={() => { onSave(tablet.id, settings); onClose(); }}
            className="px-5 py-2 bg-brand-orange text-white font-sans font-medium rounded-md hover:bg-brand-orange-dark transition-colors shadow-sm"
          >
            Save Settings
          </button>
          <button 
            onClick={onClose}
            className="px-5 py-2 bg-white text-gray-600 border border-gray-300 font-sans font-medium rounded-md hover:border-brand-orange hover:text-brand-orange transition-colors"
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
    </div>
  );
};

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
    badge: 'bg-brand-blue text-blue-900', 
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
  HAND_RAISED: { 
    cardBg: 'bg-orange-50 border-orange-200', 
    selectedCardBg: 'bg-orange-100 border-orange-300', 
    badge: 'bg-brand-orange text-white', 
    ringColor: 'ring-brand-orange',
    label: 'Needs Help'
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

const guaranteedStatuses: DeviceStatus[] = ['HAND_RAISED', 'ON_TASK', 'DISCONNECTED', 'IDLE', 'LOCKED'];

const defaultAccessibility: TabletAccessibilitySettings = {
  textToSpeech: false,
  aiSummary: false,
  animatedAvatar: false,
};

const createInitialTablets = (): Tablet[] => Array.from({ length: 30 }, (_, i) => {
  let status: DeviceStatus;

  // Ensure the first few tablets cover all statuses for demonstration
  if (i < guaranteedStatuses.length) {
    status = guaranteedStatuses[i];
  } else {
    // The rest are random
    const r = Math.random();
    if (r < 0.7) status = 'ON_TASK';
    else if (r < 0.85) status = 'HAND_RAISED';
    else if (r < 0.9) status = 'IDLE';
    else if (r < 0.95) status = 'LOCKED';
    else status = 'DISCONNECTED';
  }
  
  return { 
    id: i + 1, 
    status,
    accessibility: { ...defaultAccessibility },
    lastHelpRequestTime: status === 'HAND_RAISED' ? Date.now() - Math.random() * 60000 : undefined,
  };
});


export const ClassroomControl: React.FC<ClassroomControlProps> = ({ units, lessonFolders, contentItems }) => {
  const [selectedUnitId, setSelectedUnitId] = useState<string>('');
  const [selectedFolderId, setSelectedFolderId] = useState<string>('');
  const [selectedContentId, setSelectedContentId] = useState<string>('');
  const [selectedTablets, setSelectedTablets] = useState<number[]>([]);
  const [tablets, setTablets] = useState<Tablet[]>(createInitialTablets);
  const [settingsTablet, setSettingsTablet] = useState<Tablet | null>(null);
  const [helpToasts, setHelpToasts] = useState<Array<{ id: number; tabletId: number; message: string }>>([]);
  const [acknowledgedHelp, setAcknowledgedHelp] = useState<Set<number>>(new Set());

  // Count tablets needing help that haven't been acknowledged
  const unacknowledgedHelpCount = useMemo(() => {
    return tablets.filter(t => t.status === 'HAND_RAISED' && !acknowledgedHelp.has(t.id)).length;
  }, [tablets, acknowledgedHelp]);

  // Show toast for new help requests
  useEffect(() => {
    const newHelpRequests = tablets.filter(t => 
      t.status === 'HAND_RAISED' && 
      !acknowledgedHelp.has(t.id) &&
      !helpToasts.some(toast => toast.tabletId === t.id)
    );

    if (newHelpRequests.length > 0) {
      const newToasts = newHelpRequests.map(t => ({
        id: Date.now() + t.id,
        tabletId: t.id,
        message: `Tablet ${t.id} is requesting help`
      }));
      setHelpToasts(prev => [...prev, ...newToasts]);
    }
  }, [tablets, acknowledgedHelp, helpToasts]);

  const dismissToast = (toastId: number) => {
    setHelpToasts(prev => prev.filter(t => t.id !== toastId));
  };

  const acknowledgeHelp = (tabletId: number) => {
    setAcknowledgedHelp(prev => new Set([...prev, tabletId]));
    setHelpToasts(prev => prev.filter(t => t.tabletId !== tabletId));
  };

  const updateTabletAccessibility = (tabletId: number, settings: TabletAccessibilitySettings) => {
    setTablets(prev => prev.map(t => 
      t.id === tabletId ? { ...t, accessibility: settings } : t
    ));
  };

  const selectedUnit = useMemo(() => {
    return units.find(u => u.id === selectedUnitId);
  }, [selectedUnitId, units]);

  const foldersInSelectedUnit = useMemo(() => {
    if (!selectedUnit) return [];
    return lessonFolders.filter(f => f.unit === selectedUnit.title).sort((a,b) => a.number - b.number);
  }, [selectedUnit, lessonFolders]);
  
  const selectedFolder = useMemo(() => {
      return lessonFolders.find(f => f.id === selectedFolderId);
  }, [selectedFolderId, lessonFolders]);

  const contentInSelectedFolder = useMemo(() => {
    if (!selectedFolder) return [];
    const contentSortOrder: Record<string, number> = { 'Lesson': 1, 'Worksheet': 2, 'Quiz': 3, 'Reading': 4, 'PDF': 5 };
    return contentItems
        .filter(item => item.unit === selectedFolder.unit && item.lessonNumber === selectedFolder.number && item.status === 'Deployed')
        .sort((a, b) => (contentSortOrder[a.type] || 99) - (contentSortOrder[b.type] || 99));
  }, [selectedFolder, contentItems]);

  const handleAction = (action: string, tabletTarget: number | number[] | 'all') => {
    let targetDescription: string;
    if (tabletTarget === 'all') {
      targetDescription = 'all devices';
    } else if (Array.isArray(tabletTarget)) {
        targetDescription = `${tabletTarget.length} selected device(s)`;
    } else {
        targetDescription = `Tablet ${tabletTarget}`;
    }
    alert(`${action} for ${targetDescription}`);
  };

  const handleLaunchLesson = () => {
    const content = contentItems.find(l => l.id === selectedContentId);
    if (content) {
        alert(`Launching lesson "${content.title}" on all devices.`);
    }
  }

  const handleUnitChange = (e: React.ChangeEvent<HTMLSelectElement>) => {
    setSelectedUnitId(e.target.value);
    setSelectedFolderId('');
    setSelectedContentId('');
  };

  const handleFolderChange = (e: React.ChangeEvent<HTMLSelectElement>) => {
    setSelectedFolderId(e.target.value);
    setSelectedContentId('');
  }

  const toggleTabletSelection = (tabletId: number) => {
      setSelectedTablets(prev => 
        prev.includes(tabletId) 
        ? prev.filter(id => id !== tabletId)
        : [...prev, tabletId]
    );
  };
  
  const selectAll = () => setSelectedTablets(tablets.map(t => t.id));
  const deselectAll = () => setSelectedTablets([]);

  const hasSelection = selectedTablets.length > 0;
  const targetTabletsForAction = hasSelection ? selectedTablets : 'all';


  return (
    <div>
      {/* Toast notifications */}
      {helpToasts.slice(0, 1).map(toast => (
        <HelpToast
          key={toast.id}
          message={toast.message}
          tabletId={toast.tabletId}
          onDismiss={() => dismissToast(toast.id)}
          onAcknowledge={() => acknowledgeHelp(toast.tabletId)}
        />
      ))}

      {/* Tablet settings modal */}
      {settingsTablet && (
        <TabletSettingsModal
          tablet={settingsTablet}
          onClose={() => setSettingsTablet(null)}
          onSave={updateTabletAccessibility}
        />
      )}

      {/* Help alert banner when there are unacknowledged requests */}
      {unacknowledgedHelpCount > 0 && (
        <div className="mb-6 bg-brand-orange-light border border-brand-orange rounded-lg p-4 flex items-center gap-4 animate-pulse-slow">
          <div className="bg-brand-orange text-white p-2 rounded-full">
            <HandRaisedIcon />
          </div>
          <div className="flex-1">
            <p className="font-sans font-semibold text-brand-orange-dark">
              {unacknowledgedHelpCount} student{unacknowledgedHelpCount > 1 ? 's' : ''} requesting help
            </p>
            <p className="font-sans text-sm text-brand-orange">Click on a tablet card to view and acknowledge</p>
          </div>
          <button 
            onClick={() => {
              tablets.filter(t => t.status === 'HAND_RAISED').forEach(t => acknowledgeHelp(t.id));
            }}
            className="px-4 py-2 bg-brand-orange text-white text-sm font-medium rounded-md hover:bg-brand-orange-dark transition-colors"
          >
            Acknowledge All
          </button>
        </div>
      )}

      <div className="grid grid-cols-1 md:grid-cols-2 gap-8 mb-8">
        <Card className="border-t-4 border-t-brand-orange">
            <h4 className="font-sans font-semibold text-xl text-text-heading mb-6">Launch a Lesson</h4>
            <div className='flex flex-col space-y-5'>
                <div className="flex flex-col sm:flex-row gap-4">
                    <select 
                        value={selectedUnitId}
                        onChange={handleUnitChange}
                        className="w-full p-3 bg-white text-text-body font-sans rounded-lg border border-gray-200 focus:border-brand-orange focus:ring-1 focus:ring-brand-orange focus:outline-none flex-grow"
                    >
                        <option value="">-- Select Unit --</option>
                        {units.map(unit => (
                            <option key={unit.id} value={unit.id}>{unit.title}</option>
                        ))}
                    </select>

                    <select 
                        value={selectedFolderId}
                        onChange={handleFolderChange}
                        disabled={!selectedUnitId || foldersInSelectedUnit.length === 0}
                        className="w-full p-3 bg-white text-text-body font-sans rounded-lg border border-gray-200 focus:border-brand-orange focus:ring-1 focus:ring-brand-orange focus:outline-none flex-grow disabled:opacity-50 disabled:cursor-not-allowed"
                    >
                        <option value="">-- Select Lesson Folder --</option>
                        {foldersInSelectedUnit.map(folder => (
                            <option key={folder.id} value={folder.id}>{String(folder.number).padStart(2, '0')}. {folder.title}</option>
                        ))}
                    </select>
                </div>
                 <div className="flex flex-col sm:flex-row gap-4 items-center">
                    <select 
                        value={selectedContentId}
                        onChange={(e) => setSelectedContentId(e.target.value)}
                        disabled={!selectedFolderId || contentInSelectedFolder.length === 0}
                        className="w-full p-3 bg-white text-text-body font-sans rounded-lg border border-gray-200 focus:border-brand-orange focus:ring-1 focus:ring-brand-orange focus:outline-none flex-grow disabled:opacity-50 disabled:cursor-not-allowed"
                    >
                        <option value="">-- Select Deployed Content --</option>
                        {contentInSelectedFolder.map(item => (
                            <option key={item.id} value={item.id}>{item.title} ({item.type})</option>
                        ))}
                    </select>
                    
                    <button 
                        className="bg-brand-orange hover:bg-brand-orange-dark text-white font-sans font-medium py-3 px-6 rounded-md transition-colors shadow-sm disabled:bg-gray-300 disabled:cursor-not-allowed flex-shrink-0 w-full sm:w-auto" 
                        onClick={handleLaunchLesson}
                        disabled={!selectedContentId}
                    >
                        Launch
                    </button>
                 </div>
            </div>
        </Card>
        <Card className="border-t-4 border-t-brand-blue">
            <h4 className="font-sans font-semibold text-xl text-text-heading mb-6">Device Controls</h4>
            <div className='flex flex-wrap gap-4'>
                <button 
                    className="bg-brand-orange-light text-brand-orange hover:bg-orange-200 font-sans font-medium py-2 px-4 rounded-md transition-colors border border-transparent" 
                    onClick={() => handleAction('Locking screens', targetTabletsForAction)}>
                        {hasSelection ? `Lock Selected (${selectedTablets.length})` : 'Lock All Screens'}
                </button>
                <button 
                    className="bg-white border border-gray-300 text-text-body hover:border-brand-orange hover:text-brand-orange font-sans font-medium py-2 px-4 rounded-md transition-colors" 
                    onClick={() => handleAction('Ending session', targetTabletsForAction)}>
                        {hasSelection ? `End Session for Selected (${selectedTablets.length})` : 'End Session'}
                </button>
            </div>
        </Card>
      </div>

      <div className='mb-6 bg-white p-4 rounded-lg flex justify-between items-center border border-gray-200 shadow-soft'>
          <p className='font-sans font-medium text-text-heading'>
            {selectedTablets.length} / {tablets.length} devices selected
          </p>
          <div className='flex gap-4'>
            <button onClick={selectAll} className='text-sm font-medium text-brand-orange hover:text-brand-orange-dark transition-colors'>Select All</button>
            <button onClick={deselectAll} disabled={!hasSelection} className='text-sm font-medium text-gray-500 hover:text-gray-700 disabled:text-gray-300 disabled:cursor-not-allowed transition-colors'>Deselect All</button>
          </div>
      </div>
      
      <div role="grid" className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5 xl:grid-cols-6 gap-6">
        {tablets.map((tablet) => {
          const isSelected = selectedTablets.includes(tablet.id);
          const tabletStatusStyle = statusStyles[tablet.status];
          const backgroundClass = isSelected ? tabletStatusStyle.selectedCardBg : tabletStatusStyle.cardBg;
          const isNeedingHelp = tablet.status === 'HAND_RAISED' && !acknowledgedHelp.has(tablet.id);
          
          return (
            <div 
              key={tablet.id}
              role="gridcell"
              aria-selected={isSelected}
              tabIndex={0}
              onClick={() => toggleTabletSelection(tablet.id)}
              onKeyDown={(e) => {
                  if (e.key === ' ' || e.key === 'Enter') {
                      e.preventDefault();
                      toggleTabletSelection(tablet.id);
                  }
              }}
              className={`rounded-lg p-4 flex flex-col items-center justify-center text-center cursor-pointer transition-all duration-200 border
                ${backgroundClass} ${isSelected ? `ring-2 ${tabletStatusStyle.ringColor} ring-offset-2 ring-offset-brand-cream` : ''} 
                ${isNeedingHelp ? 'animate-pulse-border' : ''}
                focus:outline-none focus:ring-2 ${tabletStatusStyle.ringColor}`}
            >
              {isNeedingHelp ? (
                <div className="text-brand-orange animate-bounce-slow">
                  <HandRaisedIcon />
                </div>
              ) : (
                <TabletIcon />
              )}
              <p className="font-sans font-semibold text-text-heading mt-3 text-sm">
                {tablet.status === 'HAND_RAISED' ? 'Student' : `Tablet ${tablet.id}`}
              </p>
              <span className={`text-xs font-semibold px-2 py-1 rounded-full mt-2 uppercase tracking-wide ${tabletStatusStyle.badge}`}>
                {tabletStatusStyle.label}
              </span>
              
              {/* Per-tablet accessibility indicators */}
              {(tablet.accessibility.textToSpeech || tablet.accessibility.aiSummary || tablet.accessibility.animatedAvatar) && (
                <div className="flex gap-1 mt-2">
                  {tablet.accessibility.textToSpeech && (
                    <span className="w-5 h-5 bg-brand-blue/20 text-brand-blue rounded flex items-center justify-center" title="Text-to-Speech enabled">
                      <svg xmlns="http://www.w3.org/2000/svg" className="h-3 w-3" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15.536 8.464a5 5 0 010 7.072M18.364 5.636a9 9 0 010 12.728M7 17l-4-4m0 0l4-4m-4 4h18" />
                      </svg>
                    </span>
                  )}
                  {tablet.accessibility.aiSummary && (
                    <span className="w-5 h-5 bg-brand-green/20 text-brand-green rounded flex items-center justify-center" title="AI Summary enabled">
                      <svg xmlns="http://www.w3.org/2000/svg" className="h-3 w-3" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9.75 17L9 20l-1 1h8l-1-1-.75-3M3 13h18M5 17h14a2 2 0 002-2V5a2 2 0 00-2-2H5a2 2 0 00-2 2v10a2 2 0 002 2z" />
                      </svg>
                    </span>
                  )}
                  {tablet.accessibility.animatedAvatar && (
                    <span className="w-5 h-5 bg-brand-orange/20 text-brand-orange rounded flex items-center justify-center" title="Animated Avatar enabled">
                      <svg xmlns="http://www.w3.org/2000/svg" className="h-3 w-3" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5.121 17.804A13.937 13.937 0 0112 16c2.5 0 4.847.655 6.879 1.804M15 10a3 3 0 11-6 0 3 3 0 016 0zm6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
                      </svg>
                    </span>
                  )}
                </div>
              )}
              
              <div className='mt-3 flex flex-col w-full gap-1'>
                   <button 
                      className="text-xs w-full text-center py-1 text-gray-500 hover:text-brand-orange transition-colors font-medium" 
                      onClick={(e) => { e.stopPropagation(); handleAction('Ping', tablet.id); }}>
                        Ping Device
                   </button>
                   <button 
                      className="text-xs w-full text-center py-1 text-gray-500 hover:text-brand-blue transition-colors font-medium" 
                      onClick={(e) => { e.stopPropagation(); setSettingsTablet(tablet); }}>
                        Settings
                   </button>
              </div>
            </div>
          )
        })}
      </div>

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
