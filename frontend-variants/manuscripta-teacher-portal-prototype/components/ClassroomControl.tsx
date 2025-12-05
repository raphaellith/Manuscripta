
import React, { useState, useMemo } from 'react';
import { ContentHeader } from './ContentHeader';
import { Card } from './Card';
import type { ContentItem, Unit, LessonFolder } from '../types';

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

const statusStyles = {
  connected: { 
      cardBg: 'bg-green-50 border-green-200', 
      selectedCardBg: 'bg-green-100 border-green-300', 
      badge: 'bg-brand-green text-white', 
      ringColor: 'ring-brand-green' 
  },
  on_task: { 
      cardBg: 'bg-blue-50 border-blue-100', 
      selectedCardBg: 'bg-blue-100 border-blue-200', 
      badge: 'bg-brand-blue text-blue-900', 
      ringColor: 'ring-brand-blue' 
  },
  needs_help: { 
      cardBg: 'bg-orange-50 border-orange-200', 
      selectedCardBg: 'bg-orange-100 border-orange-300', 
      badge: 'bg-brand-orange text-white', 
      ringColor: 'ring-brand-orange' 
  },
  disconnected: { 
      cardBg: 'bg-gray-100 border-gray-200', 
      selectedCardBg: 'bg-gray-200 border-gray-300', 
      badge: 'bg-gray-600 text-white', 
      ringColor: 'ring-gray-400' 
  },
};

const guaranteedStatuses: (keyof typeof statusStyles)[] = ['needs_help', 'on_task', 'disconnected', 'connected'];

const tablets = Array.from({ length: 30 }, (_, i) => {
    let status: keyof typeof statusStyles;

    // Ensure the first few tablets cover all statuses for demonstration
    if (i < guaranteedStatuses.length) {
        status = guaranteedStatuses[i];
    } else {
        // The rest are random
        const r = Math.random();
        if (r < 0.8) status = 'on_task';
        else if (r < 0.9) status = 'needs_help';
        else if (r < 0.95) status = 'connected';
        else status = 'disconnected';
    }
    
    return { id: i + 1, status };
});


export const ClassroomControl: React.FC<ClassroomControlProps> = ({ units, lessonFolders, contentItems }) => {
  const [selectedUnitId, setSelectedUnitId] = useState<string>('');
  const [selectedFolderId, setSelectedFolderId] = useState<string>('');
  const [selectedContentId, setSelectedContentId] = useState<string>('');
  const [selectedTablets, setSelectedTablets] = useState<number[]>([]);

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
    const contentSortOrder = { 'Lesson': 1, 'Worksheet': 2, 'Quiz': 3 };
    return contentItems
        .filter(item => item.unit === selectedFolder.unit && item.lessonNumber === selectedFolder.number && item.status === 'Deployed')
        .sort((a, b) => contentSortOrder[a.type] - contentSortOrder[b.type]);
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
      <div className="grid grid-cols-1 md:grid-cols-2 gap-8 mb-8">
        <Card className="border-t-4 border-t-brand-orange">
            <h4 className="font-sans font-semibold text-xl text-text-heading mb-6">Launch a Lesson</h4>
            <div className='flex flex-col space-y-5'>
                <div className="flex flex-col sm:flex-row gap-4">
                    <select 
                        value={selectedUnitId}
                        onChange={handleUnitChange}
                        className="w-full p-3 bg-brand-gray text-text-body font-sans rounded-lg border-2 border-transparent focus:border-brand-orange focus:outline-none flex-grow"
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
                        className="w-full p-3 bg-brand-gray text-text-body font-sans rounded-lg border-2 border-transparent focus:border-brand-orange focus:outline-none flex-grow disabled:opacity-50 disabled:cursor-not-allowed"
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
                        className="w-full p-3 bg-brand-gray text-text-body font-sans rounded-lg border-2 border-transparent focus:border-brand-orange focus:outline-none flex-grow disabled:opacity-50 disabled:cursor-not-allowed"
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
                ${backgroundClass} ${isSelected ? `ring-2 ${tabletStatusStyle.ringColor} ring-offset-2 ring-offset-brand-cream` : ''} focus:outline-none focus:ring-2 ${tabletStatusStyle.ringColor}`}
            >
              <TabletIcon />
              <p className="font-sans font-semibold text-text-heading mt-3 text-sm">{tablet.status === 'needs_help' ? 'Student' : `Tablet ${tablet.id}`}</p>
              <span className={`text-xs font-semibold px-2 py-1 rounded-full mt-2 uppercase tracking-wide ${tabletStatusStyle.badge}`}>
                {tablet.status.replace('_', ' ')}
              </span>
              <div className='mt-3 flex flex-col w-full'>
                   <button 
                      className="text-xs w-full text-center py-1 text-gray-500 hover:text-brand-orange transition-colors font-medium" 
                      onClick={(e) => { e.stopPropagation(); handleAction('Ping', tablet.id); }}>
                        Ping Device
                   </button>
              </div>
            </div>
          )
        })}
      </div>
    </div>
  );
};
