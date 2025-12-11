import React, { useState, useMemo } from 'react';
import type { ContentItem, LessonFolder, Unit, Collection } from '../types';

interface LibraryVariantColumnsProps {
  collections: Collection[];
  units: Unit[];
  lessonFolders: LessonFolder[];
  contentItems: ContentItem[];
  onEditItem: (item: ContentItem) => void;
  onViewItem: (item: ContentItem) => void;
  onOpenContentModal: (unitTitle: string, lesson?: LessonFolder) => void;
  onOpenFolderModal: (unitTitle: string) => void;
  onOpenUnitSettings: (unit: Unit) => void;
  searchQuery: string;
}

const ChevronRightIcon = () => (
  <svg xmlns="http://www.w3.org/2000/svg" className="h-4 w-4 text-gray-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5l7 7-7 7" />
  </svg>
);

const getTypeStyles = (type: string) => {
  switch(type) {
    case 'Reading': return 'bg-brand-green text-white border-brand-green';
    case 'Worksheet': return 'bg-brand-yellow text-text-on-yellow border-brand-yellow';
    case 'Quiz': return 'bg-brand-orange text-white border-brand-orange';
    case 'PDF': return 'bg-brand-orange-dark text-white border-brand-orange-dark';
    default: return 'bg-gray-200 text-text-body border-gray-300';
  }
};

const getContentIcon = (type: string) => {
  switch(type) {
    case 'Worksheet': return (
      <svg xmlns="http://www.w3.org/2000/svg" className="h-5 w-5 text-brand-yellow flex-shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor">
        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
      </svg>
    );
    case 'Quiz': return (
      <svg xmlns="http://www.w3.org/2000/svg" className="h-5 w-5 text-brand-orange flex-shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor">
        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M8.228 9c.549-1.165 2.03-2 3.772-2 2.21 0 4 1.343 4 3 0 1.4-1.278 2.575-3.006 2.907-.542.104-.994.54-.994 1.093m0 3h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
      </svg>
    );
    case 'PDF': return (
      <svg xmlns="http://www.w3.org/2000/svg" className="h-5 w-5 text-red-500 flex-shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor">
        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M7 21h10a2 2 0 002-2V9.414a1 1 0 00-.293-.707l-5.414-5.414A1 1 0 0012.586 3H7a2 2 0 00-2 2v14a2 2 0 002 2z" />
      </svg>
    );
    case 'Reading': return (
      <svg xmlns="http://www.w3.org/2000/svg" className="h-5 w-5 text-brand-green flex-shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor">
        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 6.253v13m0-13C10.832 5.477 9.246 5 7.5 5S4.168 5.477 3 6.253v13C4.168 18.477 5.754 18 7.5 18s3.332.477 4.5 1.253m0-13C13.168 5.477 14.754 5 16.5 5c1.747 0 3.332.477 4.5 1.253v13C19.832 18.477 18.247 18 16.5 18c-1.746 0-3.332.477-4.5 1.253" />
      </svg>
    );
    default: return (
      <svg xmlns="http://www.w3.org/2000/svg" className="h-5 w-5 text-gray-400 flex-shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor">
        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M7 21h10a2 2 0 002-2V9.414a1 1 0 00-.293-.707l-5.414-5.414A1 1 0 0012.586 3H7a2 2 0 00-2 2v14a2 2 0 002 2z" />
      </svg>
    );
  }
};

export const LibraryVariantColumns: React.FC<LibraryVariantColumnsProps> = ({
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
  const [selectedCollection, setSelectedCollection] = useState<Collection | null>(null);
  const [selectedUnit, setSelectedUnit] = useState<Unit | null>(null);
  const [selectedFolder, setSelectedFolder] = useState<LessonFolder | null>(null);
  const [selectedItem, setSelectedItem] = useState<ContentItem | null>(null);

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

  const handleSelectCollection = (col: Collection) => {
    setSelectedCollection(col);
    setSelectedUnit(null);
    setSelectedFolder(null);
    setSelectedItem(null);
  };

  const handleSelectUnit = (unit: Unit) => {
    setSelectedUnit(unit);
    setSelectedFolder(null);
    setSelectedItem(null);
  };

  const handleSelectFolder = (folder: LessonFolder) => {
    setSelectedFolder(folder);
    setSelectedItem(null);
  };

  // Build breadcrumb
  const breadcrumbs = [
    selectedCollection?.name,
    selectedUnit?.title,
    selectedFolder?.title,
  ].filter(Boolean);

  return (
    <div className="bg-white rounded-xl border border-gray-200 overflow-hidden shadow-sm">
      {/* Breadcrumb */}
      <div className="px-6 py-3 bg-gray-50 border-b border-gray-200 flex items-center gap-2 text-sm">
        <button 
          onClick={() => { setSelectedCollection(null); setSelectedUnit(null); setSelectedFolder(null); setSelectedItem(null); }}
          className="text-brand-orange hover:underline font-medium"
        >
          Library
        </button>
        {breadcrumbs.map((crumb, i) => (
          <React.Fragment key={i}>
            <span className="text-gray-400">›</span>
            <span className={i === breadcrumbs.length - 1 ? 'text-text-heading font-medium' : 'text-gray-500'}>
              {crumb}
            </span>
          </React.Fragment>
        ))}
      </div>

      {/* Columns */}
      <div className="flex h-[400px] border-b border-gray-200">
        {/* Column 1: Collections */}
        <div className="w-1/4 border-r border-gray-200 flex flex-col">
          <div className="px-3 py-2 bg-gray-50 border-b border-gray-200">
            <span className="text-xs font-semibold text-gray-500 uppercase tracking-wider">Collections</span>
          </div>
          <div className="flex-1 overflow-y-auto">
            {filteredData.collections.length === 0 && searchQuery && (
              <p className="text-sm text-gray-400 italic p-3">No results</p>
            )}
            {filteredData.collections.map(col => (
              <div
                key={col.id}
                onClick={() => handleSelectCollection(col)}
                className={`flex items-center justify-between px-4 py-3 cursor-pointer transition-colors border-l-3 ${
                  selectedCollection?.id === col.id
                    ? 'bg-brand-orange/5 border-l-brand-orange'
                    : 'hover:bg-gray-50 border-l-transparent'
                }`}
              >
                <div>
                  <p className="font-medium text-text-heading">{col.name}</p>
                  <p className="text-xs text-gray-400">{getUnitsForCollection(col).length} units</p>
                </div>
                <ChevronRightIcon />
              </div>
            ))}
          </div>
        </div>

        {/* Column 2: Units */}
        <div className="w-1/4 border-r border-gray-200 flex flex-col">
          <div className="px-3 py-2 bg-gray-50 border-b border-gray-200 flex items-center justify-between">
            <span className="text-xs font-semibold text-gray-500 uppercase tracking-wider">Units</span>
            {selectedUnit && (
              <button
                onClick={() => onOpenContentModal(selectedUnit.title)}
                className="text-xs text-brand-orange hover:text-brand-orange-dark"
              >
                + Add
              </button>
            )}
          </div>
          <div className="flex-1 overflow-y-auto">
            {selectedCollection ? (
              getUnitsForCollection(selectedCollection).length > 0 ? (
                getUnitsForCollection(selectedCollection).map(unit => (
                  <div
                    key={unit.id}
                    className={`flex items-center justify-between px-4 py-3 cursor-pointer transition-colors border-l-3 group ${
                      selectedUnit?.id === unit.id
                        ? 'bg-brand-orange/5 border-l-brand-orange'
                        : 'hover:bg-gray-50 border-l-transparent'
                    }`}
                  >
                    <div onClick={() => handleSelectUnit(unit)} className="flex-1">
                      <p className="font-medium text-text-heading">{unit.title}</p>
                      <p className="text-xs text-gray-400">{getFoldersForUnit(unit).length} lessons</p>
                    </div>
                    <div className="flex items-center gap-1">
                      <button
                        onClick={(e) => { e.stopPropagation(); onOpenUnitSettings(unit); }}
                        className="opacity-0 group-hover:opacity-100 p-1.5 hover:bg-gray-200 rounded transition-all"
                        title="Unit settings"
                      >
                        <svg xmlns="http://www.w3.org/2000/svg" className="h-4 w-4 text-gray-500" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 002.573 1.066c1.543-.94 3.31.826 2.37 2.37a1.724 1.724 0 001.065 2.572c1.756.426 1.756 2.924 0 3.35a1.724 1.724 0 00-1.066 2.573c.94 1.543-.826 3.31-2.37 2.37a1.724 1.724 0 00-2.572 1.065c-.426 1.756-2.924 1.756-3.35 0a1.724 1.724 0 00-2.573-1.066c-1.543.94-3.31-.826-2.37-2.37a1.724 1.724 0 00-1.065-2.572c-1.756-.426-1.756-2.924 0-3.35a1.724 1.724 0 001.066-2.573c-.94-1.543.826-3.31 2.37-2.37.996.608 2.296.07 2.572-1.065z" />
                          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
                        </svg>
                      </button>
                      <ChevronRightIcon />
                    </div>
                  </div>
                ))
              ) : (
                <p className="text-sm text-gray-400 italic p-4">No units in this collection</p>
              )
            ) : (
              <p className="text-sm text-gray-400 italic p-4">Select a collection</p>
            )}
          </div>
        </div>

        {/* Column 3: Lessons/Folders */}
        <div className="w-1/4 border-r border-gray-200 flex flex-col">
          <div className="px-3 py-2 bg-gray-50 border-b border-gray-200 flex items-center justify-between">
            <span className="text-xs font-semibold text-gray-500 uppercase tracking-wider">Lessons</span>
            {selectedUnit && (
              <button
                onClick={() => onOpenFolderModal(selectedUnit.title)}
                className="text-xs text-brand-blue hover:text-blue-800"
              >
                + Folder
              </button>
            )}
          </div>
          <div className="flex-1 overflow-y-auto">
            {selectedUnit ? (
              <>
                {getFoldersForUnit(selectedUnit).map(folder => (
                  <div
                    key={folder.id}
                    onClick={() => handleSelectFolder(folder)}
                    className={`flex items-center justify-between px-4 py-3 cursor-pointer transition-colors border-l-3 ${
                      selectedFolder?.id === folder.id
                        ? 'bg-brand-orange/5 border-l-brand-orange'
                        : 'hover:bg-gray-50 border-l-transparent'
                    }`}
                  >
                    <div className="flex items-center gap-2">
                      <svg xmlns="http://www.w3.org/2000/svg" className="h-5 w-5 text-brand-blue" fill="currentColor" viewBox="0 0 24 24">
                        <path d="M10 4H4c-1.1 0-1.99.9-1.99 2L2 18c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V8c0-1.1-.9-2-2-2h-8l-2-2z"/>
                      </svg>
                      <div>
                        <p className="font-medium text-text-heading">#{folder.number} {folder.title}</p>
                        <p className="text-xs text-gray-400">{getItemsForFolder(selectedUnit, folder).length} items</p>
                      </div>
                    </div>
                    <ChevronRightIcon />
                  </div>
                ))}
                {getStandaloneItems(selectedUnit).map(item => (
                  <div
                    key={item.id}
                    onClick={() => setSelectedItem(item)}
                    className={`flex items-center gap-3 px-4 py-3 cursor-pointer transition-colors border-l-3 ${
                      selectedItem?.id === item.id
                        ? 'bg-brand-orange/5 border-l-brand-orange'
                        : 'hover:bg-gray-50 border-l-transparent'
                    }`}
                  >
                    {getContentIcon(item.type)}
                    <p className="font-medium text-text-heading truncate">{item.title}</p>
                  </div>
                ))}
                {getFoldersForUnit(selectedUnit).length === 0 && getStandaloneItems(selectedUnit).length === 0 && (
                  <p className="text-sm text-gray-400 italic p-4">No lessons in this unit</p>
                )}
              </>
            ) : (
              <p className="text-sm text-gray-400 italic p-4">Select a unit</p>
            )}
          </div>
        </div>

        {/* Column 4: Content Items */}
        <div className="w-1/4 flex flex-col">
          <div className="px-3 py-2 bg-gray-50 border-b border-gray-200">
            <span className="text-xs font-semibold text-gray-500 uppercase tracking-wider">Content</span>
          </div>
          <div className="flex-1 overflow-y-auto">
            {selectedFolder && selectedUnit ? (
              getItemsForFolder(selectedUnit, selectedFolder).length > 0 ? (
                getItemsForFolder(selectedUnit, selectedFolder).map(item => (
                  <div
                    key={item.id}
                    onClick={() => setSelectedItem(item)}
                    className={`flex items-center gap-3 px-4 py-3 cursor-pointer transition-colors border-l-3 ${
                      selectedItem?.id === item.id
                        ? 'bg-brand-orange/5 border-l-brand-orange'
                        : 'hover:bg-gray-50 border-l-transparent'
                    }`}
                  >
                    {getContentIcon(item.type)}
                    <div className="flex-1 min-w-0">
                      <p className="font-medium text-text-heading truncate">{item.title}</p>
                      <p className="text-xs text-gray-400">
                        <span className={`font-medium ${item.status === 'Deployed' ? 'text-brand-green' : 'text-brand-orange'}`}>
                          {item.status}
                        </span>
                      </p>
                    </div>
                  </div>
                ))
              ) : (
                <p className="text-sm text-gray-400 italic p-4">No content in this lesson</p>
              )
            ) : (
              <p className="text-sm text-gray-400 italic p-4">Select a lesson</p>
            )}
          </div>
        </div>
      </div>

      {/* Detail Panel */}
      {selectedItem && (
        <div className="p-6 bg-gradient-to-r from-gray-50 to-white">
          <div className="flex items-center justify-between mb-4">
            <div className="flex items-center gap-3">
              {getContentIcon(selectedItem.type)}
              <div>
                <h3 className="text-xl font-semibold text-text-heading">{selectedItem.title}</h3>
                <p className="text-sm text-gray-500">
                  {selectedItem.subject} • Created {selectedItem.created} • 
                  <span className={`font-medium ml-1 ${selectedItem.status === 'Deployed' ? 'text-brand-green' : 'text-brand-orange'}`}>
                    {selectedItem.status}
                  </span>
                </p>
              </div>
            </div>
            <div className="flex gap-3">
              <button
                onClick={() => onViewItem(selectedItem)}
                className="px-4 py-2 bg-brand-green text-white font-medium text-sm rounded-md hover:bg-green-800 transition-colors"
              >
                View
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
          </div>
          {selectedItem.content && (
            <>
              <div 
                className="prose prose-sm max-w-none bg-white p-4 rounded-lg border border-gray-100"
                dangerouslySetInnerHTML={{ __html: selectedItem.content }}
              />
              <style>{`
                .prose { color: #14201E; }
                .prose h1 { font-family: 'Fraunces', serif; font-size: 1.5rem; font-weight: 500; margin-bottom: 0.75rem; color: #212631; }
                .prose h2 { font-family: 'Fraunces', serif; font-size: 1.25rem; font-weight: 500; margin-bottom: 0.5rem; color: #212631; margin-top: 1rem; }
                .prose h3 { font-family: 'Fraunces', serif; font-size: 1.1rem; font-weight: 500; margin-bottom: 0.375rem; color: #212631; margin-top: 0.875rem; }
                .prose p { margin-bottom: 0.75rem; line-height: 1.6; }
                .prose ul, .prose ol { margin-left: 1.25rem; margin-bottom: 0.75rem; }
                .prose ul { list-style-type: disc; }
                .prose ol { list-style-type: decimal; }
                .prose li { margin-bottom: 0.2rem; line-height: 1.5; }
                .prose strong, .prose b { font-weight: 600; }
                .prose em, .prose i { font-style: italic; }
              `}</style>
            </>
          )}
        </div>
      )}
    </div>
  );
};
