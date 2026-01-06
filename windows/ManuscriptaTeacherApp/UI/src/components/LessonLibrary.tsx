
import React, { useState } from 'react';
import type { ContentItem, View, LessonFolder, Unit, Collection } from '../types';
import { ContentCreatorModal } from './ContentCreatorModal';
import { ContentEditorModal } from './ContentEditorModal';
import { ContentViewerModal } from './ContentViewerModal';
import { LibraryVariantTree } from './LibraryVariantTree';
import { LibraryVariantColumns } from './LibraryVariantColumns';
import { UnitSettingsModal } from './UnitSettingsModal';

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
                        className="w-full p-3 bg-white text-text-body font-sans rounded-lg border border-gray-200 focus:border-brand-orange focus:ring-1 focus:ring-brand-orange focus:outline-none"
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
  collections: Collection[];
  units: Unit[];
  lessonFolders: LessonFolder[];
  contentItems: ContentItem[];
  setContentItems: React.Dispatch<React.SetStateAction<ContentItem[]>>;
  setActiveView: (view: View) => void;
  onAddLessonFolder: (unit: string, title: string) => void;
  onUpdateContentItem: (item: ContentItem) => void;
  onUpdateUnit: (unit: Unit) => void;
}

// Only Tree and Columns variants
type ActiveVariant = 'tree' | 'columns';

// Variant Switcher Icons
const TreeViewIcon = ({ className = "h-4 w-4" }: { className?: string }) => (
  <svg xmlns="http://www.w3.org/2000/svg" className={className} fill="none" viewBox="0 0 24 24" stroke="currentColor">
    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 6h16M4 10h16M4 14h16M4 18h16" />
  </svg>
);

const ColumnsViewIcon = ({ className = "h-4 w-4" }: { className?: string }) => (
  <svg xmlns="http://www.w3.org/2000/svg" className={className} fill="none" viewBox="0 0 24 24" stroke="currentColor">
    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 17V7m0 10a2 2 0 01-2 2H5a2 2 0 01-2-2V7a2 2 0 012-2h2a2 2 0 012 2m0 10a2 2 0 002 2h2a2 2 0 002-2M9 7a2 2 0 012-2h2a2 2 0 012 2m0 10V7m0 10a2 2 0 002 2h2a2 2 0 002-2V7a2 2 0 00-2-2h-2a2 2 0 00-2 2" />
  </svg>
);

export const LessonLibrary: React.FC<LessonLibraryProps> = ({ 
  collections,
  units, 
  lessonFolders, 
  contentItems, 
  setContentItems, 
  setActiveView, 
  onAddLessonFolder, 
  onUpdateContentItem,
  onUpdateUnit,
}) => {
  const [contentModalUnit, setContentModalUnit] = useState<string | null>(null);
  const [contentModalLesson, setContentModalLesson] = useState<LessonFolder | null>(null);
  const [folderModalUnit, setFolderModalUnit] = useState<string | null>(null);
  const [editingContentItem, setEditingContentItem] = useState<ContentItem | null>(null);
  const [viewingContentItem, setViewingContentItem] = useState<ContentItem | null>(null);
  const [selectedUnit, setSelectedUnit] = useState<Unit | null>(null);
  const [searchQuery, setSearchQuery] = useState('');
  const [activeVariant, setActiveVariant] = useState<ActiveVariant>('tree');
  const [isViewSwitcherOpen, setIsViewSwitcherOpen] = useState(false);

  const handleOpenContentModal = (unitTitle: string, lesson?: LessonFolder) => {
    setContentModalUnit(unitTitle);
    setContentModalLesson(lesson || null);
  };
  const handleOpenFolderModal = (unitTitle: string) => setFolderModalUnit(unitTitle);
  const handleOpenUnitSettings = (unit: Unit) => setSelectedUnit(unit);
  const handleCloseModals = () => {
      setContentModalUnit(null);
      setContentModalLesson(null);
      setFolderModalUnit(null);
      setEditingContentItem(null);
      setViewingContentItem(null);
      setSelectedUnit(null);
  };

  const handleSaveUnit = (updatedUnit: Unit) => {
    onUpdateUnit(updatedUnit);
    handleCloseModals();
  };

  const handleSaveEditedContent = (updatedItem: ContentItem) => {
    onUpdateContentItem(updatedItem);
    handleCloseModals();
  };

  const handleAddContent = (content: { title: string; type: 'Reading' | 'Worksheet' | 'Quiz', lessonNumber: number, lessonTitle: string }) => {
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

  // Shared props for all variants
  const variantProps = {
    collections,
    units,
    lessonFolders,
    contentItems,
    onEditItem: setEditingContentItem,
    onViewItem: setViewingContentItem,
    onOpenContentModal: handleOpenContentModal,
    onOpenFolderModal: handleOpenFolderModal,
    onOpenUnitSettings: handleOpenUnitSettings,
    searchQuery,
  };

  const renderVariant = () => {
    switch (activeVariant) {
      case 'tree':
        return <LibraryVariantTree {...variantProps} />;
      case 'columns':
        return <LibraryVariantColumns {...variantProps} />;
      default:
        return <LibraryVariantTree {...variantProps} />;
    }
  };

  return (
    <div>
      {contentModalUnit && (
        <ContentCreatorModal 
          unit={contentModalUnit}
          existingLessonFolders={lessonFolders.filter(f => f.unit === contentModalUnit).sort((a,b) => a.number - b.number)}
          preSelectedLesson={contentModalLesson}
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
      {selectedUnit && (
        <UnitSettingsModal
            unit={selectedUnit}
            onClose={handleCloseModals}
            onSave={handleSaveUnit}
        />
      )}
      
      {/* Floating View Switcher - positioned like ThemeSwitcher but on the left */}
      <div className="fixed bottom-4 left-4 z-50">
        {!isViewSwitcherOpen && (
          <button
            onClick={() => setIsViewSwitcherOpen(true)}
            className="bg-white p-3 rounded-full shadow-lg border border-gray-200 hover:shadow-xl transition-all flex items-center gap-2"
            title="Switch View"
          >
            {activeVariant === 'tree' ? <TreeViewIcon /> : <ColumnsViewIcon />}
            <svg xmlns="http://www.w3.org/2000/svg" className="h-4 w-4 text-gray-500" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 5a1 1 0 011-1h14a1 1 0 011 1v2a1 1 0 01-1 1H5a1 1 0 01-1-1V5zM4 13a1 1 0 011-1h6a1 1 0 011 1v6a1 1 0 01-1 1H5a1 1 0 01-1-1v-6zM16 13a1 1 0 011-1h2a1 1 0 011 1v6a1 1 0 01-1 1h-2a1 1 0 01-1-1v-6z" />
            </svg>
          </button>
        )}

        {isViewSwitcherOpen && (
          <div className="bg-white rounded-lg shadow-xl border border-gray-200 w-48 overflow-hidden">
            <div className="flex items-center justify-between p-3 border-b border-gray-100">
              <span className="text-sm font-medium text-gray-700">View Layout</span>
              <button
                onClick={() => setIsViewSwitcherOpen(false)}
                className="p-1 hover:bg-gray-100 rounded transition-colors"
              >
                <svg xmlns="http://www.w3.org/2000/svg" className="h-4 w-4 text-gray-500" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
                </svg>
              </button>
            </div>
            <div className="p-2 space-y-1">
              <button
                onClick={() => { setActiveVariant('tree'); setIsViewSwitcherOpen(false); }}
                className={`w-full flex items-center gap-3 px-3 py-2 rounded-md text-left transition-all ${
                  activeVariant === 'tree'
                    ? 'bg-brand-orange/10 text-brand-orange'
                    : 'hover:bg-gray-50 text-gray-600'
                }`}
              >
                <TreeViewIcon />
                <span className="text-sm font-medium">Tree View</span>
              </button>
              <button
                onClick={() => { setActiveVariant('columns'); setIsViewSwitcherOpen(false); }}
                className={`w-full flex items-center gap-3 px-3 py-2 rounded-md text-left transition-all ${
                  activeVariant === 'columns'
                    ? 'bg-brand-orange/10 text-brand-orange'
                    : 'hover:bg-gray-50 text-gray-600'
                }`}
              >
                <ColumnsViewIcon />
                <span className="text-sm font-medium">Column View</span>
              </button>
            </div>
          </div>
        )}
      </div>
      
      <div className="mb-6 flex flex-col md:flex-row gap-4 sticky top-0 z-40 pb-4 bg-gradient-to-b from-brand-cream to-transparent">
        {/* Search Input */}
        <div className="relative flex-grow">
            <span className="absolute inset-y-0 left-0 flex items-center pl-4 pointer-events-none">
                <svg xmlns="http://www.w3.org/2000/svg" className="h-5 w-5 text-gray-400" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
                    <path strokeLinecap="round" strokeLinejoin="round" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
                </svg>
            </span>
            <input
                type="text"
                placeholder="Search collections, units, lessons, or content..."
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                className="w-full p-4 pl-12 bg-white text-text-body font-sans rounded-xl border border-gray-200 focus:border-brand-orange focus:ring-1 focus:ring-brand-orange focus:outline-none transition-all shadow-soft"
                aria-label="Search lesson library"
            />
        </div>

        {/* Create Unit Button */}
        <button 
            onClick={() => setActiveView('lesson-creator')}
            className="px-8 py-4 bg-brand-orange text-white font-sans font-medium rounded-xl hover:bg-brand-orange-dark transition-colors shadow-soft hover:shadow-md whitespace-nowrap flex-shrink-0 flex items-center gap-2">
            <span>+</span> Create Unit
        </button>
      </div>

      {/* Active Variant Display */}
      {renderVariant()}
    </div>
  );
};
