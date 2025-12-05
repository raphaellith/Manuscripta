import React, { useState, useMemo } from 'react';
import type { ContentItem, LessonFolder, Unit, Collection } from '../types';

interface LibraryVariantCardsProps {
  collections: Collection[];
  units: Unit[];
  lessonFolders: LessonFolder[];
  contentItems: ContentItem[];
  onEditItem: (item: ContentItem) => void;
  onViewItem: (item: ContentItem) => void;
  onOpenContentModal: (unitTitle: string) => void;
  onOpenFolderModal: (unitTitle: string) => void;
  searchQuery: string;
}

type NavigationLevel = 'collections' | 'units' | 'folders' | 'items';

interface NavigationState {
  level: NavigationLevel;
  collection?: Collection;
  unit?: Unit;
  folder?: LessonFolder;
}

const getTypeStyles = (type: string) => {
  switch(type) {
    case 'Lesson': return { bg: 'bg-brand-blue/10', border: 'border-brand-blue', text: 'text-brand-blue' };
    case 'Reading': return { bg: 'bg-brand-green/10', border: 'border-brand-green', text: 'text-brand-green' };
    case 'Worksheet': return { bg: 'bg-brand-yellow/20', border: 'border-brand-yellow', text: 'text-yellow-700' };
    case 'Quiz': return { bg: 'bg-brand-orange-light', border: 'border-brand-orange', text: 'text-brand-orange' };
    case 'PDF': return { bg: 'bg-red-50', border: 'border-red-400', text: 'text-red-600' };
    default: return { bg: 'bg-gray-50', border: 'border-gray-300', text: 'text-gray-600' };
  }
};

const getContentIcon = (type: string, size: string = 'h-8 w-8') => {
  const baseClass = `${size} flex-shrink-0`;
  switch(type) {
    case 'Lesson': return (
      <svg xmlns="http://www.w3.org/2000/svg" className={`${baseClass} text-brand-blue`} fill="none" viewBox="0 0 24 24" stroke="currentColor">
        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M12 6.253v13m0-13C10.832 5.477 9.246 5 7.5 5S4.168 5.477 3 6.253v13C4.168 18.477 5.754 18 7.5 18s3.332.477 4.5 1.253m0-13C13.168 5.477 14.754 5 16.5 5c1.747 0 3.332.477 4.5 1.253v13C19.832 18.477 18.247 18 16.5 18c-1.746 0-3.332.477-4.5 1.253" />
      </svg>
    );
    case 'Worksheet': return (
      <svg xmlns="http://www.w3.org/2000/svg" className={`${baseClass} text-brand-yellow`} fill="none" viewBox="0 0 24 24" stroke="currentColor">
        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
      </svg>
    );
    case 'Quiz': return (
      <svg xmlns="http://www.w3.org/2000/svg" className={`${baseClass} text-brand-orange`} fill="none" viewBox="0 0 24 24" stroke="currentColor">
        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M8.228 9c.549-1.165 2.03-2 3.772-2 2.21 0 4 1.343 4 3 0 1.4-1.278 2.575-3.006 2.907-.542.104-.994.54-.994 1.093m0 3h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
      </svg>
    );
    case 'PDF': return (
      <svg xmlns="http://www.w3.org/2000/svg" className={`${baseClass} text-red-500`} fill="none" viewBox="0 0 24 24" stroke="currentColor">
        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M7 21h10a2 2 0 002-2V9.414a1 1 0 00-.293-.707l-5.414-5.414A1 1 0 0012.586 3H7a2 2 0 00-2 2v14a2 2 0 002 2z" />
      </svg>
    );
    case 'Reading': return (
      <svg xmlns="http://www.w3.org/2000/svg" className={`${baseClass} text-brand-green`} fill="none" viewBox="0 0 24 24" stroke="currentColor">
        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M12 6.253v13m0-13C10.832 5.477 9.246 5 7.5 5S4.168 5.477 3 6.253v13C4.168 18.477 5.754 18 7.5 18s3.332.477 4.5 1.253m0-13C13.168 5.477 14.754 5 16.5 5c1.747 0 3.332.477 4.5 1.253v13C19.832 18.477 18.247 18 16.5 18c-1.746 0-3.332.477-4.5 1.253" />
      </svg>
    );
    default: return (
      <svg xmlns="http://www.w3.org/2000/svg" className={`${baseClass} text-gray-400`} fill="none" viewBox="0 0 24 24" stroke="currentColor">
        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M7 21h10a2 2 0 002-2V9.414a1 1 0 00-.293-.707l-5.414-5.414A1 1 0 0012.586 3H7a2 2 0 00-2 2v14a2 2 0 002 2z" />
      </svg>
    );
  }
};

export const LibraryVariantCards: React.FC<LibraryVariantCardsProps> = ({
  collections,
  units,
  lessonFolders,
  contentItems,
  onEditItem,
  onViewItem,
  onOpenContentModal,
  onOpenFolderModal,
  searchQuery,
}) => {
  const [nav, setNav] = useState<NavigationState>({ level: 'collections' });

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

  const navigateToCollection = (collection: Collection) => {
    setNav({ level: 'units', collection });
  };

  const navigateToUnit = (unit: Unit) => {
    setNav({ ...nav, level: 'folders', unit });
  };

  const navigateToFolder = (folder: LessonFolder) => {
    setNav({ ...nav, level: 'items', folder });
  };

  const goBack = (toLevel: NavigationLevel) => {
    switch (toLevel) {
      case 'collections':
        setNav({ level: 'collections' });
        break;
      case 'units':
        setNav({ level: 'units', collection: nav.collection });
        break;
      case 'folders':
        setNav({ level: 'folders', collection: nav.collection, unit: nav.unit });
        break;
    }
  };

  // Render breadcrumbs
  const renderBreadcrumbs = () => (
    <div className="flex items-center gap-2 text-sm mb-6 flex-wrap">
      <button
        onClick={() => goBack('collections')}
        className={`font-medium transition-colors ${nav.level === 'collections' ? 'text-text-heading' : 'text-brand-orange hover:text-brand-orange-dark'}`}
      >
        All Collections
      </button>
      {nav.collection && (
        <>
          <span className="text-gray-400">›</span>
          <button
            onClick={() => goBack('units')}
            className={`font-medium transition-colors ${nav.level === 'units' ? 'text-text-heading' : 'text-brand-orange hover:text-brand-orange-dark'}`}
          >
            {nav.collection.name}
          </button>
        </>
      )}
      {nav.unit && (
        <>
          <span className="text-gray-400">›</span>
          <button
            onClick={() => goBack('folders')}
            className={`font-medium transition-colors ${nav.level === 'folders' ? 'text-text-heading' : 'text-brand-orange hover:text-brand-orange-dark'}`}
          >
            {nav.unit.title}
          </button>
        </>
      )}
      {nav.folder && (
        <>
          <span className="text-gray-400">›</span>
          <span className="font-medium text-text-heading">{nav.folder.title}</span>
        </>
      )}
    </div>
  );

  // Render collection cards
  const renderCollections = () => (
    <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6">
      {filteredData.collections.length === 0 && searchQuery && (
        <p className="col-span-full text-gray-500 italic text-center py-10">No results found for "{searchQuery}"</p>
      )}
      {filteredData.collections.map(col => {
        const unitCount = getUnitsForCollection(col).length;
        return (
          <div
            key={col.id}
            onClick={() => navigateToCollection(col)}
            className="group bg-white rounded-xl border-2 border-gray-100 p-6 cursor-pointer hover:border-brand-orange hover:shadow-lg transition-all"
          >
            <div className="w-14 h-14 rounded-lg bg-brand-orange/10 flex items-center justify-center mb-4 group-hover:bg-brand-orange/20 transition-colors">
              <svg xmlns="http://www.w3.org/2000/svg" className="h-8 w-8 text-brand-orange" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M19 11H5m14 0a2 2 0 012 2v6a2 2 0 01-2 2H5a2 2 0 01-2-2v-6a2 2 0 012-2m14 0V9a2 2 0 00-2-2M5 11V9a2 2 0 012-2m0 0V5a2 2 0 012-2h6a2 2 0 012 2v2M7 7h10" />
              </svg>
            </div>
            <h3 className="text-lg font-semibold text-text-heading mb-1 group-hover:text-brand-orange transition-colors">{col.name}</h3>
            <p className="text-sm text-gray-500">{unitCount} unit{unitCount !== 1 ? 's' : ''}</p>
            {col.description && (
              <p className="text-sm text-gray-400 mt-2 line-clamp-2">{col.description}</p>
            )}
          </div>
        );
      })}
    </div>
  );

  // Render unit cards
  const renderUnits = () => {
    if (!nav.collection) return null;
    const unitsInCollection = getUnitsForCollection(nav.collection);
    
    return (
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6">
        {unitsInCollection.length === 0 ? (
          <p className="col-span-full text-gray-500 italic text-center py-10">No units in this collection</p>
        ) : (
          unitsInCollection.map(unit => {
            const folderCount = getFoldersForUnit(unit).length;
            return (
              <div
                key={unit.id}
                onClick={() => navigateToUnit(unit)}
                className="group bg-white rounded-xl border-2 border-gray-100 p-6 cursor-pointer hover:border-brand-green hover:shadow-lg transition-all"
              >
                <div className="w-14 h-14 rounded-lg bg-brand-green/10 flex items-center justify-center mb-4 group-hover:bg-brand-green/20 transition-colors">
                  <svg xmlns="http://www.w3.org/2000/svg" className="h-8 w-8 text-brand-green" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M12 6.253v13m0-13C10.832 5.477 9.246 5 7.5 5S4.168 5.477 3 6.253v13C4.168 18.477 5.754 18 7.5 18s3.332.477 4.5 1.253m0-13C13.168 5.477 14.754 5 16.5 5c1.747 0 3.332.477 4.5 1.253v13C19.832 18.477 18.247 18 16.5 18c-1.746 0-3.332.477-4.5 1.253" />
                  </svg>
                </div>
                <h3 className="text-lg font-semibold text-text-heading mb-1 group-hover:text-brand-green transition-colors">{unit.title}</h3>
                <p className="text-sm text-gray-500">{folderCount} lesson{folderCount !== 1 ? 's' : ''}</p>
                <p className="text-xs text-gray-400 mt-2">{unit.subject} • {unit.ageRange}</p>
              </div>
            );
          })
        )}
      </div>
    );
  };

  // Render folder cards
  const renderFolders = () => {
    if (!nav.unit) return null;
    const foldersInUnit = getFoldersForUnit(nav.unit);
    const standaloneItems = getStandaloneItems(nav.unit);
    
    return (
      <>
        <div className="flex items-center justify-between mb-4">
          <h2 className="text-lg font-medium text-gray-600">Lessons</h2>
          <button
            onClick={() => onOpenFolderModal(nav.unit!.title)}
            className="text-sm text-brand-blue hover:text-blue-800 font-medium"
          >
            + Add Lesson Folder
          </button>
        </div>
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6 mb-8">
          {foldersInUnit.map(folder => {
            const itemCount = getItemsForFolder(nav.unit!, folder).length;
            return (
              <div
                key={folder.id}
                onClick={() => navigateToFolder(folder)}
                className="group bg-white rounded-xl border-2 border-gray-100 p-6 cursor-pointer hover:border-brand-blue hover:shadow-lg transition-all"
              >
                <div className="w-14 h-14 rounded-lg bg-brand-blue/10 flex items-center justify-center mb-4 group-hover:bg-brand-blue/20 transition-colors">
                  <svg xmlns="http://www.w3.org/2000/svg" className="h-8 w-8 text-brand-blue" fill="currentColor" viewBox="0 0 24 24">
                    <path d="M10 4H4c-1.1 0-1.99.9-1.99 2L2 18c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V8c0-1.1-.9-2-2-2h-8l-2-2z"/>
                  </svg>
                </div>
                <div className="text-xs text-gray-400 mb-1">Lesson #{folder.number}</div>
                <h3 className="text-lg font-semibold text-text-heading mb-1 group-hover:text-brand-blue transition-colors">{folder.title}</h3>
                <p className="text-sm text-gray-500">{itemCount} item{itemCount !== 1 ? 's' : ''}</p>
              </div>
            );
          })}
        </div>
        
        {standaloneItems.length > 0 && (
          <>
            <h2 className="text-lg font-medium text-gray-600 mb-4">Standalone Content</h2>
            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6">
              {standaloneItems.map(item => {
                const styles = getTypeStyles(item.type);
                return (
                  <div
                    key={item.id}
                    className={`group bg-white rounded-xl border-2 ${styles.border} p-6 hover:shadow-lg transition-all`}
                  >
                    <div className={`w-14 h-14 rounded-lg ${styles.bg} flex items-center justify-center mb-4`}>
                      {getContentIcon(item.type)}
                    </div>
                    <div className={`text-xs font-semibold ${styles.text} uppercase tracking-wide mb-1`}>{item.type}</div>
                    <h3 className="text-lg font-semibold text-text-heading mb-2">{item.title}</h3>
                    <div className="flex items-center gap-2 text-sm text-gray-500 mb-4">
                      <span className={`font-medium ${item.status === 'Deployed' ? 'text-brand-green' : 'text-brand-orange'}`}>
                        {item.status}
                      </span>
                    </div>
                    <div className="flex gap-2 opacity-0 group-hover:opacity-100 transition-opacity">
                      <button
                        onClick={() => onViewItem(item)}
                        className="flex-1 px-3 py-1.5 bg-brand-green text-white text-sm font-medium rounded-md hover:bg-green-800"
                      >
                        View
                      </button>
                      {item.type !== 'PDF' && (
                        <button
                          onClick={() => onEditItem(item)}
                          className="flex-1 px-3 py-1.5 bg-white text-text-heading border border-gray-300 text-sm font-medium rounded-md hover:border-brand-orange hover:text-brand-orange"
                        >
                          Edit
                        </button>
                      )}
                    </div>
                  </div>
                );
              })}
            </div>
          </>
        )}
        
        {foldersInUnit.length === 0 && standaloneItems.length === 0 && (
          <p className="text-gray-500 italic text-center py-10">No lessons or content in this unit. Add a lesson folder to get started.</p>
        )}
      </>
    );
  };

  // Render content item cards
  const renderItems = () => {
    if (!nav.unit || !nav.folder) return null;
    const itemsInFolder = getItemsForFolder(nav.unit, nav.folder);
    
    return (
      <>
        <div className="flex items-center justify-between mb-4">
          <h2 className="text-lg font-medium text-gray-600">Content</h2>
          <button
            onClick={() => onOpenContentModal(nav.unit!.title)}
            className="text-sm text-brand-orange hover:text-brand-orange-dark font-medium"
          >
            + Add Content
          </button>
        </div>
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6">
          {itemsInFolder.length === 0 ? (
            <p className="col-span-full text-gray-500 italic text-center py-10">No content in this lesson. Click "+ Add Content" to create some.</p>
          ) : (
            itemsInFolder.map(item => {
              const styles = getTypeStyles(item.type);
              return (
                <div
                  key={item.id}
                  className={`group bg-white rounded-xl border-2 ${styles.border} p-6 hover:shadow-lg transition-all`}
                >
                  <div className={`w-14 h-14 rounded-lg ${styles.bg} flex items-center justify-center mb-4`}>
                    {getContentIcon(item.type)}
                  </div>
                  <div className={`text-xs font-semibold ${styles.text} uppercase tracking-wide mb-1`}>{item.type}</div>
                  <h3 className="text-lg font-semibold text-text-heading mb-2">{item.title}</h3>
                  <p className="text-xs text-gray-400 mb-1">Created {item.created}</p>
                  <div className="flex items-center gap-2 text-sm text-gray-500 mb-4">
                    <span className={`font-medium ${item.status === 'Deployed' ? 'text-brand-green' : 'text-brand-orange'}`}>
                      {item.status}
                    </span>
                  </div>
                  <div className="flex gap-2 opacity-0 group-hover:opacity-100 transition-opacity">
                    <button
                      onClick={() => onViewItem(item)}
                      className="flex-1 px-3 py-1.5 bg-brand-green text-white text-sm font-medium rounded-md hover:bg-green-800"
                    >
                      View
                    </button>
                    {item.type !== 'PDF' && (
                      <button
                        onClick={() => onEditItem(item)}
                        className="flex-1 px-3 py-1.5 bg-white text-text-heading border border-gray-300 text-sm font-medium rounded-md hover:border-brand-orange hover:text-brand-orange"
                      >
                        Edit
                      </button>
                    )}
                  </div>
                </div>
              );
            })
          )}
        </div>
      </>
    );
  };

  return (
    <div className="bg-white rounded-xl border border-gray-200 p-6 shadow-sm min-h-[500px]">
      {renderBreadcrumbs()}
      
      {nav.level === 'collections' && renderCollections()}
      {nav.level === 'units' && renderUnits()}
      {nav.level === 'folders' && renderFolders()}
      {nav.level === 'items' && renderItems()}
    </div>
  );
};
