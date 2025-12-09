import React, { useState, useMemo } from 'react';
import type { ContentItem, LessonFolder, Unit, Collection } from '../types';

interface LibraryVariantTreeProps {
  collections: Collection[];
  units: Unit[];
  lessonFolders: LessonFolder[];
  contentItems: ContentItem[];
  onEditItem: (item: ContentItem) => void;
  onViewItem: (item: ContentItem) => void;
  onOpenContentModal: (unitTitle: string) => void;
  onOpenFolderModal: (unitTitle: string) => void;
  onOpenUnitSettings: (unit: Unit) => void;
  searchQuery: string;
}

// Icons
const ChevronRightIcon = ({ isOpen }: { isOpen: boolean }) => (
  <svg 
    xmlns="http://www.w3.org/2000/svg" 
    className={`h-4 w-4 text-gray-400 transition-transform duration-200 flex-shrink-0 ${isOpen ? 'rotate-90' : ''}`} 
    fill="none" 
    viewBox="0 0 24 24" 
    stroke="currentColor"
  >
    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5l7 7-7 7" />
  </svg>
);

// Collection: Stacked layers icon (represents grouping)
const CollectionIcon = () => (
  <svg xmlns="http://www.w3.org/2000/svg" className="h-4 w-4 text-brand-orange flex-shrink-0" viewBox="0 0 24 24" fill="currentColor">
    <path d="M12 2L2 7l10 5 10-5-10-5z"/>
    <path d="M2 12l10 5 10-5" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round"/>
    <path d="M2 17l10 5 10-5" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round"/>
  </svg>
);

// Unit: Book with bookmark icon (represents a unit of study)
const UnitIcon = () => (
  <svg xmlns="http://www.w3.org/2000/svg" className="h-4 w-4 text-brand-green flex-shrink-0" viewBox="0 0 24 24" fill="currentColor">
    <path d="M4 4a2 2 0 012-2h8a2 2 0 012 2v16l-5-3-5 3V4z"/>
    <path d="M16 2h2a2 2 0 012 2v16l-2-1.2V4a2 2 0 00-2-2z" opacity="0.6"/>
  </svg>
);

// Lesson Folder: Document stack icon (represents a lesson folder)
const LessonFolderIcon = () => (
  <svg xmlns="http://www.w3.org/2000/svg" className="h-4 w-4 text-brand-blue flex-shrink-0" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5">
    <rect x="4" y="4" width="14" height="16" rx="2" fill="currentColor" opacity="0.2"/>
    <path d="M8 2h8a2 2 0 012 2v16a2 2 0 01-2 2H8a2 2 0 01-2-2V4a2 2 0 012-2z" fill="#3B82F6" stroke="none"/>
    <path d="M9 8h6M9 12h6M9 16h4" strokeLinecap="round" stroke="white" strokeWidth="1.5"/>
  </svg>
);

const getContentIcon = (type: string) => {
  switch(type) {
    case 'Lesson': return (
      <svg xmlns="http://www.w3.org/2000/svg" className="h-4 w-4 text-brand-blue flex-shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor">
        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 6.253v13m0-13C10.832 5.477 9.246 5 7.5 5S4.168 5.477 3 6.253v13C4.168 18.477 5.754 18 7.5 18s3.332.477 4.5 1.253m0-13C13.168 5.477 14.754 5 16.5 5c1.747 0 3.332.477 4.5 1.253v13C19.832 18.477 18.247 18 16.5 18c-1.746 0-3.332.477-4.5 1.253" />
      </svg>
    );
    case 'Worksheet': return (
      <svg xmlns="http://www.w3.org/2000/svg" className="h-4 w-4 text-brand-yellow flex-shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor">
        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
      </svg>
    );
    case 'Quiz': return (
      <svg xmlns="http://www.w3.org/2000/svg" className="h-4 w-4 text-brand-orange flex-shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor">
        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M8.228 9c.549-1.165 2.03-2 3.772-2 2.21 0 4 1.343 4 3 0 1.4-1.278 2.575-3.006 2.907-.542.104-.994.54-.994 1.093m0 3h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
      </svg>
    );
    case 'PDF': return (
      <svg xmlns="http://www.w3.org/2000/svg" className="h-4 w-4 text-red-500 flex-shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor">
        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M7 21h10a2 2 0 002-2V9.414a1 1 0 00-.293-.707l-5.414-5.414A1 1 0 0012.586 3H7a2 2 0 00-2 2v14a2 2 0 002 2z" />
      </svg>
    );
    case 'Reading': return (
      <svg xmlns="http://www.w3.org/2000/svg" className="h-4 w-4 text-brand-green flex-shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor">
        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 6.253v13m0-13C10.832 5.477 9.246 5 7.5 5S4.168 5.477 3 6.253v13C4.168 18.477 5.754 18 7.5 18s3.332.477 4.5 1.253m0-13C13.168 5.477 14.754 5 16.5 5c1.747 0 3.332.477 4.5 1.253v13C19.832 18.477 18.247 18 16.5 18c-1.746 0-3.332.477-4.5 1.253" />
      </svg>
    );
    default: return (
      <svg xmlns="http://www.w3.org/2000/svg" className="h-4 w-4 text-gray-400 flex-shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor">
        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M7 21h10a2 2 0 002-2V9.414a1 1 0 00-.293-.707l-5.414-5.414A1 1 0 0012.586 3H7a2 2 0 00-2 2v14a2 2 0 002 2z" />
      </svg>
    );
  }
};

const getTypeStyles = (type: string) => {
  switch(type) {
    case 'Lesson': return 'bg-brand-blue text-text-on-blue border-brand-blue';
    case 'Reading': return 'bg-brand-green text-white border-brand-green';
    case 'Worksheet': return 'bg-brand-yellow text-text-on-yellow border-brand-yellow';
    case 'Quiz': return 'bg-brand-orange text-white border-brand-orange';
    case 'PDF': return 'bg-brand-orange-dark text-white border-brand-orange-dark';
    default: return 'bg-gray-200 text-text-body border-gray-300';
  }
};

export const LibraryVariantTree: React.FC<LibraryVariantTreeProps> = ({
  collections,
  units,
  lessonFolders,
  contentItems,
  onEditItem,
  onViewItem,
  onOpenContentModal,
  onOpenFolderModal,
  onOpenUnitSettings,
  searchQuery,
}) => {
  const [expandedCollections, setExpandedCollections] = useState<Set<string>>(new Set(collections.map(c => c.id)));
  const [expandedUnits, setExpandedUnits] = useState<Set<string>>(new Set(units.map(u => u.id)));
  const [expandedFolders, setExpandedFolders] = useState<Set<string>>(new Set());
  const [selectedItem, setSelectedItem] = useState<ContentItem | null>(null);

  const toggleCollection = (id: string) => {
    setExpandedCollections(prev => {
      const next = new Set(prev);
      if (next.has(id)) next.delete(id);
      else next.add(id);
      return next;
    });
  };

  const toggleUnit = (id: string) => {
    setExpandedUnits(prev => {
      const next = new Set(prev);
      if (next.has(id)) next.delete(id);
      else next.add(id);
      return next;
    });
  };

  const toggleFolder = (id: string) => {
    setExpandedFolders(prev => {
      const next = new Set(prev);
      if (next.has(id)) next.delete(id);
      else next.add(id);
      return next;
    });
  };

  // Filter data based on search
  const filteredData = useMemo(() => {
    const query = searchQuery.toLowerCase().trim();
    if (!query) return { collections, units, lessonFolders, contentItems };
    
    const matchedItems = contentItems.filter(item => 
      item.title.toLowerCase().includes(query)
    );
    const matchedFolders = lessonFolders.filter(folder =>
      folder.title.toLowerCase().includes(query) ||
      matchedItems.some(item => item.lessonNumber === folder.number && item.unit === folder.unit)
    );
    const matchedUnits = units.filter(unit =>
      unit.title.toLowerCase().includes(query) ||
      matchedFolders.some(f => f.unit === unit.title) ||
      matchedItems.some(item => item.unit === unit.title)
    );
    const matchedCollections = collections.filter(col =>
      col.name.toLowerCase().includes(query) ||
      matchedUnits.some(u => col.unitIds.includes(u.id))
    );
    
    return {
      collections: matchedCollections,
      units: matchedUnits,
      lessonFolders: matchedFolders,
      contentItems: matchedItems,
    };
  }, [searchQuery, collections, units, lessonFolders, contentItems]);

  const getUnitsForCollection = (collection: Collection) => 
    filteredData.units.filter(u => collection.unitIds.includes(u.id));

  const getFoldersForUnit = (unit: Unit) =>
    filteredData.lessonFolders.filter(f => f.unit === unit.title).sort((a, b) => a.number - b.number);

  const getItemsForFolder = (unit: Unit, folder: LessonFolder) =>
    filteredData.contentItems.filter(item => item.unit === unit.title && item.lessonNumber === folder.number);

  const getStandaloneItems = (unit: Unit) =>
    filteredData.contentItems.filter(item => item.unit === unit.title && !item.lessonNumber);

  return (
    <div className="flex h-[calc(100vh-220px)] min-h-[500px] bg-white rounded-xl border border-gray-200 overflow-hidden shadow-sm">
      {/* Tree Sidebar */}
      <div className="w-80 border-r border-gray-200 flex flex-col bg-gray-50/50">
        <div className="p-3 border-b border-gray-200 bg-white">
          <span className="text-xs font-semibold text-gray-500 uppercase tracking-wider">Explorer</span>
        </div>
        <div className="flex-1 overflow-y-auto p-2 space-y-1">
          {filteredData.collections.length === 0 && searchQuery && (
            <p className="text-sm text-gray-500 italic p-3">No results found.</p>
          )}
          {filteredData.collections.map(collection => (
            <div key={collection.id}>
              {/* Collection Row - Orange left border */}
              <div
                className="flex items-center gap-2 px-2 py-1.5 rounded-r-md cursor-pointer hover:bg-brand-orange/5 transition-colors group border-l-3 border-l-brand-orange bg-brand-orange/[0.02]"
                onClick={() => toggleCollection(collection.id)}
              >
                <ChevronRightIcon isOpen={expandedCollections.has(collection.id)} />
                <span className="text-sm font-medium text-text-heading truncate flex-1">{collection.name}</span>
                <span className="text-xs text-gray-400">{getUnitsForCollection(collection).length}</span>
              </div>
              
              {/* Units under Collection - Orange indent line */}
              {expandedCollections.has(collection.id) && (
                <div className="ml-4 pl-2 space-y-1 mt-1" style={{ borderLeft: '2px solid var(--color-brand-orange)' }}>
                  {getUnitsForCollection(collection).map(unit => (
                    <div key={unit.id}>
                      {/* Unit Row - Green left border */}
                      <div
                        className="flex items-center gap-2 px-2 py-1.5 rounded-r-md cursor-pointer hover:bg-brand-green/5 transition-colors group border-l-3 border-l-brand-green bg-brand-green/[0.02]"
                        onClick={() => toggleUnit(unit.id)}
                      >
                        <ChevronRightIcon isOpen={expandedUnits.has(unit.id)} />
                        <span className="text-sm text-text-heading truncate flex-1">{unit.title}</span>
                        <button
                          onClick={(e) => { e.stopPropagation(); onOpenUnitSettings(unit); }}
                          className="opacity-0 group-hover:opacity-100 p-1 hover:bg-gray-200 rounded transition-all"
                          title="Unit settings"
                        >
                          <svg xmlns="http://www.w3.org/2000/svg" className="h-3.5 w-3.5 text-gray-500" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 002.573 1.066c1.543-.94 3.31.826 2.37 2.37a1.724 1.724 0 001.065 2.572c1.756.426 1.756 2.924 0 3.35a1.724 1.724 0 00-1.066 2.573c.94 1.543-.826 3.31-2.37 2.37a1.724 1.724 0 00-2.572 1.065c-.426 1.756-2.924 1.756-3.35 0a1.724 1.724 0 00-2.573-1.066c-1.543.94-3.31-.826-2.37-2.37a1.724 1.724 0 00-1.065-2.572c-1.756-.426-1.756-2.924 0-3.35a1.724 1.724 0 001.066-2.573c-.94-1.543.826-3.31 2.37-2.37.996.608 2.296.07 2.572-1.065z" />
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
                          </svg>
                        </button>
                        <button
                          onClick={(e) => { e.stopPropagation(); onOpenContentModal(unit.title); }}
                          className="opacity-0 group-hover:opacity-100 text-xs text-brand-orange hover:text-brand-orange-dark transition-opacity"
                          title="Add content"
                        >
                          +
                        </button>
                      </div>
                      
                      {/* Folders and Items under Unit - Green indent line */}
                      {expandedUnits.has(unit.id) && (
                        <div className="ml-4 pl-2 space-y-0.5 mt-1" style={{ borderLeft: '2px solid var(--color-brand-green)' }}>
                          {getFoldersForUnit(unit).map(folder => (
                            <div key={folder.id}>
                              {/* Lesson Folder Row - Blue left border */}
                              <div
                                className="flex items-center gap-2 px-2 py-1 rounded-r-md cursor-pointer hover:bg-brand-blue/5 transition-colors border-l-3 border-l-brand-blue bg-brand-blue/[0.02]"
                                onClick={() => toggleFolder(folder.id)}
                              >
                                <ChevronRightIcon isOpen={expandedFolders.has(folder.id)} />
                                <span className="text-sm text-text-body truncate flex-1">L{folder.number} {folder.title}</span>
                                <span className="text-xs text-gray-400">({getItemsForFolder(unit, folder).length})</span>
                              </div>
                              
                              {/* Items in Folder - Blue indent line */}
                              {expandedFolders.has(folder.id) && (
                                <div className="ml-4 pl-2 space-y-0.5 mt-0.5" style={{ borderLeft: '2px solid var(--color-brand-blue)' }}>
                                  {getItemsForFolder(unit, folder).map(item => (
                                    <div
                                      key={item.id}
                                      className={`flex items-center gap-2 px-2 py-1 rounded-md cursor-pointer transition-colors border-l-2 border-l-gray-200 ${
                                        selectedItem?.id === item.id ? 'bg-brand-orange/10 border-l-brand-orange' : 'hover:bg-gray-50'
                                      }`}
                                      onClick={() => setSelectedItem(item)}
                                    >
                                      {getContentIcon(item.type)}
                                      <span className="text-sm text-text-body truncate">{item.title}</span>
                                    </div>
                                  ))}
                                </div>
                              )}
                            </div>
                          ))}
                          
                          {/* Standalone Items */}
                          {getStandaloneItems(unit).map(item => (
                            <div
                              key={item.id}
                              className={`flex items-center gap-2 px-2 py-1 rounded-md cursor-pointer transition-colors border-l-2 border-l-gray-200 ${
                                selectedItem?.id === item.id ? 'bg-brand-orange/10 border-l-brand-orange' : 'hover:bg-gray-50'
                              }`}
                              onClick={() => setSelectedItem(item)}
                            >
                              {getContentIcon(item.type)}
                              <span className="text-sm text-text-body truncate">{item.title}</span>
                            </div>
                          ))}
                        </div>
                      )}
                    </div>
                  ))}
                </div>
              )}
            </div>
          ))}
        </div>
      </div>
      
      {/* Detail Panel */}
      <div className="flex-1 flex flex-col">
        {selectedItem ? (
          <>
            <div className="p-6 border-b border-gray-200 bg-gradient-to-r from-gray-50 to-white">
              <div className="flex items-center gap-3 mb-3">
                <h2 className="text-2xl font-semibold text-text-heading">{selectedItem.title}</h2>
                <span className={`text-xs font-semibold px-2 py-1 rounded-md uppercase tracking-wide border ${getTypeStyles(selectedItem.type)}`}>
                  {selectedItem.type}
                </span>
              </div>
              <p className="text-sm text-gray-500">
                {selectedItem.subject} • Created {selectedItem.created} • 
                <span className={`font-medium ml-1 ${selectedItem.status === 'Deployed' ? 'text-brand-green' : 'text-brand-orange'}`}>
                  {selectedItem.status}
                </span>
              </p>
            </div>
            <div className="flex-1 p-6 overflow-y-auto">
              {/* PDF Preview */}
              {selectedItem.type === 'PDF' && selectedItem.pdfPath ? (
                <div className="h-full min-h-[400px] bg-gray-100 rounded-lg overflow-hidden shadow-inner">
                  <iframe
                    src={selectedItem.pdfPath}
                    className="w-full h-full min-h-[400px]"
                    title={selectedItem.title}
                  />
                </div>
              ) : (
                <>
                  {selectedItem.content && (
                    <div 
                      className="prose prose-sm max-w-none"
                      dangerouslySetInnerHTML={{ __html: selectedItem.content }}
                    />
                  )}
                  {selectedItem.imageUrl && (
                    <img src={selectedItem.imageUrl} alt={selectedItem.title} className="max-w-full rounded-lg shadow-md mt-4" />
                  )}
                  {!selectedItem.content && !selectedItem.imageUrl && (
                    <p className="text-gray-400 italic">No preview available for this content.</p>
                  )}
                </>
              )}
            </div>
            <div className="p-4 border-t border-gray-200 bg-white flex gap-3">
              <button
                onClick={() => onViewItem(selectedItem)}
                className="px-4 py-2 bg-brand-green text-white font-medium text-sm rounded-md hover:bg-green-800 transition-colors"
              >
                View Full
              </button>
              {selectedItem.type !== 'PDF' && (
                <button
                  onClick={() => onEditItem(selectedItem)}
                  className="px-4 py-2 bg-white text-text-heading border border-gray-300 font-medium text-sm rounded-md hover:border-brand-orange hover:text-brand-orange transition-colors"
                >
                  Edit
                </button>
              )}
              <button
                onClick={() => alert(`Duplicating ${selectedItem.title}`)}
                className="px-4 py-2 bg-white text-text-heading border border-gray-300 font-medium text-sm rounded-md hover:border-brand-orange hover:text-brand-orange transition-colors"
              >
                Duplicate
              </button>
            </div>
          </>
        ) : (
          <div className="flex-1 flex items-center justify-center text-gray-400">
            <div className="text-center">
              <svg xmlns="http://www.w3.org/2000/svg" className="h-16 w-16 mx-auto mb-4 opacity-30" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1} d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
              </svg>
              <p className="text-lg">Select an item to view details</p>
            </div>
          </div>
        )}
      </div>
    </div>
  );
};
