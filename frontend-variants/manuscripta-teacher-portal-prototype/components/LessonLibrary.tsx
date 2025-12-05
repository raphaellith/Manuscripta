
import React, { useState, useMemo } from 'react';
import type { ContentItem, View, LessonFolder, Unit } from '../types';
import { ContentCreatorModal } from './ContentCreatorModal';
import { ContentEditorModal } from './ContentEditorModal';
import { ContentViewerModal } from './ContentViewerModal';
import { ContentHeader } from './ContentHeader';

interface LessonFolderCreatorModalProps {
    unit: string;
    onClose: () => void;
    onAdd: (unit: string, title: string) => void;
}

const LessonFolderCreatorModal: React.FC<LessonFolderCreatorModalProps> = ({ unit, onClose, onAdd }) => {
    const [title, setTitle] = useState('');

    const handleAdd = () => {
        if (!title.trim()) {
            alert('Please provide a title for the lesson folder.');
            return;
        }
        onAdd(unit, title);
        onClose();
    }

    return (
        <div className="fixed inset-0 bg-text-heading/20 backdrop-blur-sm flex items-center justify-center z-50 p-4">
            <div className="bg-white rounded-lg p-8 shadow-2xl w-full max-w-lg space-y-6">
                <h2 className="text-2xl font-serif text-text-heading">Add Lesson Folder to "{unit}"</h2>
                
                <div>
                    <label className="font-sans font-medium text-text-heading text-sm mb-2 block">Lesson Title</label>
                    <input
                        type="text"
                        value={title}
                        onChange={(e) => setTitle(e.target.value)}
                        className="w-full p-3 bg-brand-gray text-text-body font-sans rounded-lg border-2 border-transparent focus:border-brand-orange focus:outline-none"
                        placeholder="e.g., The Domesday Book"
                    />
                </div>
                
                <div className="flex flex-wrap gap-4 pt-4 border-t border-gray-100">
                    <button onClick={handleAdd} className="px-6 py-3 bg-brand-orange text-white font-sans font-medium rounded-md hover:bg-brand-orange-dark transition-colors shadow-sm">
                        Add Folder
                    </button>
                    <button onClick={onClose} className="px-6 py-3 bg-white text-gray-600 border border-gray-300 font-sans font-medium rounded-md hover:bg-gray-50 transition-colors">
                        Cancel
                    </button>
                </div>
            </div>
        </div>
    );
};

interface LessonLibraryProps {
  units: Unit[];
  lessonFolders: LessonFolder[];
  contentItems: ContentItem[];
  setContentItems: React.Dispatch<React.SetStateAction<ContentItem[]>>;
  setActiveView: (view: View) => void;
  onAddLessonFolder: (unit: string, title: string) => void;
  onUpdateContentItem: (item: ContentItem) => void;
}

interface ContentItemDisplayProps {
  contentItem: ContentItem;
  onEdit: (item: ContentItem) => void;
  onView: (item: ContentItem) => void;
}

const FolderIcon = () => (
    <svg xmlns="http://www.w3.org/2000/svg" className="h-6 w-6 text-brand-blue flex-shrink-0" fill="currentColor" viewBox="0 0 24 24">
       <path d="M10 4H4c-1.1 0-1.99.9-1.99 2L2 18c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V8c0-1.1-.9-2-2-2h-8l-2-2z"/>
    </svg>
);

const ChevronIcon = ({ isOpen }: { isOpen: boolean }) => (
    <svg 
        xmlns="http://www.w3.org/2000/svg" 
        className={`h-5 w-5 text-gray-400 transition-transform duration-200 ${isOpen ? 'rotate-180' : ''}`} 
        fill="none" 
        viewBox="0 0 24 24" 
        stroke="currentColor"
    >
        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 9l-7 7-7-7" />
    </svg>
);

const getTypeStyles = (type: string) => {
    switch(type) {
        case 'Lesson': return 'bg-brand-blue/20 text-blue-900 border-brand-blue';
        case 'Reading': return 'bg-brand-green/20 text-green-900 border-brand-green';
        case 'Worksheet': return 'bg-brand-yellow/30 text-yellow-900 border-brand-yellow';
        case 'Quiz': return 'bg-brand-orange-light text-brand-orange-dark border-brand-orange';
        case 'PDF': return 'bg-red-100 text-red-900 border-red-400';
        default: return 'bg-gray-100 text-gray-600 border-gray-300';
    }
}

const getTypeBorderColor = (type: string) => {
    switch(type) {
        case 'Lesson': return 'border-t-brand-blue';
        case 'Reading': return 'border-t-brand-green';
        case 'Worksheet': return 'border-t-brand-yellow';
        case 'Quiz': return 'border-t-brand-orange';
        case 'PDF': return 'border-t-red-400';
        default: return 'border-t-gray-300';
    }
}

const ContentItemDisplay: React.FC<ContentItemDisplayProps> = ({ contentItem, onEdit, onView }) => {
  const hasPdfOrImage = contentItem.type === 'PDF' || contentItem.imageUrl;
  
  return (
    <div className={`bg-white rounded-lg p-5 border border-gray-100 border-t-4 shadow-sm hover:shadow-md transition-all flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4 group ${getTypeBorderColor(contentItem.type)}`}>
      <div className="flex-1">
        <div className="flex items-center gap-3">
          {/* Show PDF icon for PDF type */}
          {contentItem.type === 'PDF' && (
            <svg xmlns="http://www.w3.org/2000/svg" className="h-5 w-5 text-red-500 flex-shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M7 21h10a2 2 0 002-2V9.414a1 1 0 00-.293-.707l-5.414-5.414A1 1 0 0012.586 3H7a2 2 0 00-2 2v14a2 2 0 002 2z" />
            </svg>
          )}
          {/* Show image icon for lessons with images */}
          {contentItem.imageUrl && contentItem.type !== 'PDF' && (
            <svg xmlns="http://www.w3.org/2000/svg" className="h-5 w-5 text-brand-blue flex-shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 16l4.586-4.586a2 2 0 012.828 0L16 16m-2-2l1.586-1.586a2 2 0 012.828 0L20 14m-6-6h.01M6 20h12a2 2 0 002-2V6a2 2 0 00-2-2H6a2 2 0 00-2 2v12a2 2 0 002 2z" />
            </svg>
          )}
          <h4 className="font-sans font-semibold text-text-heading text-lg group-hover:text-brand-orange transition-colors">{contentItem.title}</h4>
          <span className={`text-xs font-sans font-semibold px-2 py-1 rounded-md uppercase tracking-wide border ${getTypeStyles(contentItem.type)}`}>{contentItem.type}</span>
        </div>
        <p className="text-sm text-gray-500 mt-2 font-sans">{contentItem.subject} • Created {contentItem.created} • <span className={`font-medium ${contentItem.status === 'Deployed' ? 'text-brand-green' : 'text-brand-orange'}`}>{contentItem.status}</span></p>
      </div>
      <div className="flex gap-3 flex-shrink-0 opacity-0 group-hover:opacity-100 transition-opacity">
        <button 
          className="px-4 py-2 bg-brand-green text-white font-sans font-medium text-sm rounded-md hover:bg-green-800 transition-colors" 
          onClick={() => onView(contentItem)}
        >
          View
        </button>
        {contentItem.type !== 'PDF' && (
          <button className="px-4 py-2 bg-white text-text-heading border border-gray-300 font-sans font-medium text-sm rounded-md hover:border-brand-orange hover:text-brand-orange transition-colors" onClick={() => onEdit(contentItem)}>Edit</button>
        )}
        <button className="px-4 py-2 bg-white text-text-heading border border-gray-300 font-sans font-medium text-sm rounded-md hover:border-brand-orange hover:text-brand-orange transition-colors" onClick={() => alert(`Duplicating ${contentItem.title}`)}>Duplicate</button>
      </div>
    </div>
  );
};


export const LessonLibrary: React.FC<LessonLibraryProps> = ({ units, lessonFolders, contentItems, setContentItems, setActiveView, onAddLessonFolder, onUpdateContentItem }) => {
  const [contentModalUnit, setContentModalUnit] = useState<string | null>(null);
  const [folderModalUnit, setFolderModalUnit] = useState<string | null>(null);
  const [editingContentItem, setEditingContentItem] = useState<ContentItem | null>(null);
  const [viewingContentItem, setViewingContentItem] = useState<ContentItem | null>(null);
  const [searchQuery, setSearchQuery] = useState('');
  const [collapsedUnits, setCollapsedUnits] = useState<Set<string>>(new Set());
  const [collapsedFolders, setCollapsedFolders] = useState<Set<string>>(new Set());

  const toggleUnit = (unitId: string) => {
    setCollapsedUnits(prev => {
      const next = new Set(prev);
      if (next.has(unitId)) {
        next.delete(unitId);
      } else {
        next.add(unitId);
      }
      return next;
    });
  };

  const toggleFolder = (folderId: string) => {
    setCollapsedFolders(prev => {
      const next = new Set(prev);
      if (next.has(folderId)) {
        next.delete(folderId);
      } else {
        next.add(folderId);
      }
      return next;
    });
  };

  const handleOpenContentModal = (unitTitle: string) => setContentModalUnit(unitTitle);
  const handleOpenFolderModal = (unitTitle: string) => setFolderModalUnit(unitTitle);
  const handleCloseModals = () => {
      setContentModalUnit(null);
      setFolderModalUnit(null);
      setEditingContentItem(null);
      setViewingContentItem(null);
  };

  const handleSaveEditedContent = (updatedItem: ContentItem) => {
    onUpdateContentItem(updatedItem);
    handleCloseModals();
  };

  const handleAddContent = (content: { title: string; type: 'Lesson' | 'Worksheet' | 'Quiz', lessonNumber: number, lessonTitle: string }) => {
      if (!contentModalUnit) return;
      
      const newContentItem: ContentItem = {
          id: `lesson-${Date.now()}`,
          unit: contentModalUnit,
          title: content.title,
          type: content.type,
          lessonNumber: content.lessonNumber,
          lessonTitle: content.lessonTitle,
          subject: 'History - Year 7-9',
          created: new Date().toLocaleDateString('en-GB', { day:'numeric', month: 'short', year: 'numeric' }),
          status: 'Draft',
          content: `<h1>${content.title}</h1><p>This is a newly generated ${content.type}. Start editing to add your content!</p>`,
      };
      
      setContentItems(prev => [...prev, newContentItem].sort((a,b) => a.unit.localeCompare(b.unit) || (a.lessonNumber ?? 0) - (b.lessonNumber ?? 0) ));
      alert(`"${content.title}" (${content.type}) has been generated and added to the "${contentModalUnit}" unit as a draft.`);
      handleCloseModals();
  }

  const displayData = useMemo(() => {
    const lowercasedQuery = searchQuery.toLowerCase().trim();

    if (!lowercasedQuery) {
        return units.map(unit => ({
            ...unit,
            folders: lessonFolders
                .filter(f => f.unit === unit.title)
                .sort((a, b) => a.number - b.number)
                .map(folder => ({
                    ...folder,
                    items: contentItems.filter(item => item.unit === unit.title && item.lessonNumber === folder.number),
                })),
            standaloneItems: contentItems.filter(item => item.unit === unit.title && !item.lessonNumber)
        }));
    }

    const filteredUnits = [];
    for (const unit of units) {
        const unitTitleMatches = unit.title.toLowerCase().includes(lowercasedQuery);

        const filteredStandaloneItems = contentItems.filter(item =>
            item.unit === unit.title &&
            !item.lessonNumber &&
            item.title.toLowerCase().includes(lowercasedQuery)
        );

        const filteredFolders = lessonFolders
            .filter(folder => folder.unit === unit.title)
            .map(folder => {
                const folderTitleMatches = folder.title.toLowerCase().includes(lowercasedQuery);
                const itemsInFolder = contentItems.filter(item => item.unit === unit.title && item.lessonNumber === folder.number);
                
                const filteredItems = folderTitleMatches
                    ? itemsInFolder
                    : itemsInFolder.filter(item => item.title.toLowerCase().includes(lowercasedQuery));
                
                if (folderTitleMatches || filteredItems.length > 0) {
                    return { ...folder, items: filteredItems };
                }
                return null;
            })
            .filter((f): f is NonNullable<typeof f> => f !== null)
            .sort((a, b) => a.number - b.number);
        
        if (unitTitleMatches || filteredFolders.length > 0 || filteredStandaloneItems.length > 0) {
            if (unitTitleMatches) {
                filteredUnits.push({
                    ...unit,
                    folders: lessonFolders
                        .filter(f => f.unit === unit.title)
                        .sort((a,b) => a.number - b.number)
                        .map(folder => ({
                            ...folder,
                            items: contentItems.filter(item => item.unit === unit.title && item.lessonNumber === folder.number),
                        })),
                    standaloneItems: contentItems.filter(item => item.unit === unit.title && !item.lessonNumber)
                });
            } else {
                filteredUnits.push({
                    ...unit,
                    folders: filteredFolders,
                    standaloneItems: filteredStandaloneItems,
                });
            }
        }
    }
    return filteredUnits;
  }, [searchQuery, units, lessonFolders, contentItems]);


  return (
    <div>
      {contentModalUnit && (
        <ContentCreatorModal 
          unit={contentModalUnit}
          existingLessonFolders={lessonFolders.filter(f => f.unit === contentModalUnit).sort((a,b) => a.number - b.number)}
          onClose={handleCloseModals}
          onAddContent={handleAddContent}
        />
      )}
      {folderModalUnit && (
        <LessonFolderCreatorModal 
          unit={folderModalUnit}
          onClose={handleCloseModals}
          onAdd={onAddLessonFolder}
        />
      )}
      {editingContentItem && (
        <ContentEditorModal
            contentItem={editingContentItem}
            onClose={handleCloseModals}
            onSave={handleSaveEditedContent}
        />
      )}
      {viewingContentItem && (
        <ContentViewerModal
            contentItem={viewingContentItem}
            onClose={handleCloseModals}
        />
      )}
      
      
      <div className="mb-10 flex flex-col md:flex-row gap-4 sticky top-0 z-40 pb-4 bg-gradient-to-b from-brand-cream to-transparent">
        <div className="relative flex-grow">
            <span className="absolute inset-y-0 left-0 flex items-center pl-4 pointer-events-none">
                <svg xmlns="http://www.w3.org/2000/svg" className="h-5 w-5 text-gray-400" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
                    <path strokeLinecap="round" strokeLinejoin="round" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
                </svg>
            </span>
            <input
                type="text"
                placeholder="Search units, lessons, or content..."
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                className="w-full p-4 pl-12 bg-white text-text-body font-sans rounded-xl border border-gray-200 focus:border-brand-orange focus:ring-1 focus:ring-brand-orange focus:outline-none transition-all shadow-soft"
                aria-label="Search lesson library"
            />
        </div>
        <button 
            onClick={() => setActiveView('lesson-creator')}
            className="px-8 py-4 bg-brand-orange text-white font-sans font-medium rounded-xl hover:bg-brand-orange-dark transition-colors shadow-soft hover:shadow-md whitespace-nowrap flex-shrink-0 flex items-center gap-2">
            <span>+</span> Create Unit
        </button>
      </div>

      <div className="space-y-12">
        {displayData.length === 0 && searchQuery && (
            <div className="text-center py-10">
                <p className="text-gray-500 italic font-serif text-lg">No results found for "{searchQuery}".</p>
            </div>
        )}
        {displayData.map(unit => {
            const contentSortOrder: Record<string, number> = { 'Lesson': 1, 'Reading': 2, 'Worksheet': 3, 'Quiz': 4, 'PDF': 5 };
            const originalFoldersForUnit = lessonFolders.filter(f => f.unit === unit.title);
            const originalStandaloneItems = contentItems.filter(item => item.unit === unit.title && !item.lessonNumber);
            const isUnitCollapsed = collapsedUnits.has(unit.id);
            const folderCount = unit.folders.length;
            const itemCount = unit.folders.reduce((acc, f) => acc + f.items.length, 0) + unit.standaloneItems.length;

            return(
                <div key={unit.id} className="bg-white rounded-xl shadow-sm border border-gray-100 overflow-hidden border-t-8 border-t-brand-orange">
                    <div 
                        className="flex justify-between items-center p-8 pb-6 border-b border-gray-100 bg-white cursor-pointer hover:bg-gray-50 transition-colors"
                        onClick={() => toggleUnit(unit.id)}
                    >
                        <div className="flex items-center gap-4">
                            <ChevronIcon isOpen={!isUnitCollapsed} />
                            <div>
                                <h3 className="text-2xl font-sans font-semibold text-text-heading">{unit.title}</h3>
                                <p className="text-gray-500 mt-1 font-sans text-sm">
                                    {folderCount} lesson{folderCount !== 1 ? 's' : ''} • {itemCount} item{itemCount !== 1 ? 's' : ''}
                                </p>
                            </div>
                        </div>
                        <div className="flex gap-3" onClick={(e) => e.stopPropagation()}>
                           <button 
                                onClick={() => handleOpenFolderModal(unit.title)}
                                className="px-4 py-2 bg-brand-blue/10 text-brand-blue border border-transparent font-sans font-medium text-sm rounded-md hover:bg-brand-blue hover:text-white transition-all"
                            >
                                + Add Lesson Folder
                            </button>
                            <button 
                                onClick={() => handleOpenContentModal(unit.title)}
                                className="px-4 py-2 bg-white border border-brand-orange text-brand-orange font-sans font-medium text-sm rounded-md hover:bg-brand-orange hover:text-white transition-colors"
                            >
                                + Add Content
                            </button>
                        </div>
                    </div>
                    
                    {!isUnitCollapsed && (
                        <div className="p-8 space-y-6 bg-gray-50/50">
                            {originalFoldersForUnit.length === 0 && originalStandaloneItems.length === 0 && (
                                <div className="p-12 bg-gray-50 rounded-lg text-center border-2 border-dashed border-gray-200">
                                    <p className="text-gray-400 italic font-sans">This unit is empty. Add a lesson folder or content to get started.</p>
                                </div>
                            )}
                            {unit.folders.map(folder => {
                                const itemsInFolder = folder.items;
                                const isFolderCollapsed = collapsedFolders.has(folder.id);
                                
                                return (
                                    <div key={folder.id} className="bg-white border border-gray-200 rounded-lg shadow-sm border-t-4 border-t-brand-blue overflow-hidden">
                                        <div 
                                            className="p-6 flex items-center justify-between cursor-pointer hover:bg-gray-50 transition-colors"
                                            onClick={() => toggleFolder(folder.id)}
                                        >
                                            <h4 className="font-sans font-medium text-text-heading text-lg flex items-center gap-3">
                                                <ChevronIcon isOpen={!isFolderCollapsed} />
                                                <div className="p-2 bg-brand-blue/10 rounded-md shadow-sm">
                                                    <FolderIcon />
                                                </div>
                                                <span className="text-gray-400 font-sans text-base font-normal">#{folder.number}</span>
                                                {folder.title}
                                                <span className="text-sm text-gray-400 font-normal ml-2">
                                                    ({itemsInFolder.length} item{itemsInFolder.length !== 1 ? 's' : ''})
                                                </span>
                                            </h4>
                                        </div>
                                        
                                        {!isFolderCollapsed && (
                                            <div className="px-6 pb-6">
                                                <div className="space-y-3 pl-4 border-l-2 border-brand-blue/20 ml-4">
                                                    {itemsInFolder.length > 0 ? (
                                                        itemsInFolder.sort((a, b) => contentSortOrder[a.type] - contentSortOrder[b.type]).map(item => <ContentItemDisplay key={item.id} contentItem={item} onEdit={setEditingContentItem} onView={setViewingContentItem} />)
                                                    ) : (
                                                      <p className="text-sm text-gray-500 italic pl-2">No content in this lesson yet.</p>
                                                    )}
                                                </div>
                                            </div>
                                        )}
                                    </div>
                                )
                            })}
                            {unit.standaloneItems.map(item => <ContentItemDisplay key={item.id} contentItem={item} onEdit={setEditingContentItem} onView={setViewingContentItem} />)}
                        </div>
                    )}
                </div>
            )
        })}
      </div>
    </div>
  );
};
