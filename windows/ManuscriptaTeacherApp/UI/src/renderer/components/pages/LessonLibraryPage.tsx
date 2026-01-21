/**
 * Lesson Library Page component.
 * Uses tree view UI from prototype LibraryVariantTree.tsx.
 * Per WindowsAppStructureSpec §2B(1)(d)(i).
 * 
 * Implements:
 * - s4A(3)(a): Source document attachment UI
 * - s4A(3)(b): Material search by title and content
 */

import React, { useState, useMemo } from 'react';
import { useAppContext } from '../../state/AppContext';
import { CreateCollectionModal } from '../modals/CreateCollectionModal';
import { CreateUnitModal } from '../modals/CreateUnitModal';
import { CreateLessonModal } from '../modals/CreateLessonModal';
import { CreateMaterialModal } from '../modals/CreateMaterialModal';
import { SourceDocumentModal } from '../modals/SourceDocumentModal';
import { EditorModal } from '../editor/EditorModal';
import type { UnitCollectionEntity, UnitEntity, LessonEntity, MaterialEntity, MaterialType } from '../../models';

// Icons from prototype LibraryVariantTree.tsx
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

const getContentIcon = (type: MaterialType) => {
    switch (type) {
        case 'WORKSHEET': return (
            <svg xmlns="http://www.w3.org/2000/svg" className="h-4 w-4 text-brand-yellow flex-shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
            </svg>
        );
        case 'READING': return (
            <svg xmlns="http://www.w3.org/2000/svg" className="h-4 w-4 text-brand-green flex-shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 6.253v13m0-13C10.832 5.477 9.246 5 7.5 5S4.168 5.477 3 6.253v13C4.168 18.477 5.754 18 7.5 18s3.332.477 4.5 1.253m0-13C13.168 5.477 14.754 5 16.5 5c1.747 0 3.332.477 4.5 1.253v13C19.832 18.477 18.247 18 16.5 18c-1.746 0-3.332.477-4.5 1.253" />
            </svg>
        );
        default: return (
            <svg xmlns="http://www.w3.org/2000/svg" className="h-4 w-4 text-purple-500 flex-shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M7 21h10a2 2 0 002-2V9.414a1 1 0 00-.293-.707l-5.414-5.414A1 1 0 0012.586 3H7a2 2 0 00-2 2v14a2 2 0 002 2z" />
            </svg>
        );
    }
};

const PlusIcon = () => (
    <svg xmlns="http://www.w3.org/2000/svg" className="h-3.5 w-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth="2">
        <path strokeLinecap="round" strokeLinejoin="round" d="M12 4v16m8-8H4" />
    </svg>
);

const TrashIcon = () => (
    <svg xmlns="http://www.w3.org/2000/svg" className="h-3.5 w-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth="2">
        <path strokeLinecap="round" strokeLinejoin="round" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
    </svg>
);

const SearchIcon = () => (
    <svg xmlns="http://www.w3.org/2000/svg" className="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth="2">
        <path strokeLinecap="round" strokeLinejoin="round" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
    </svg>
);

const DocumentIcon = () => (
    <svg xmlns="http://www.w3.org/2000/svg" className="h-3.5 w-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth="2">
        <path strokeLinecap="round" strokeLinejoin="round" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
    </svg>
);

// Modal state types
type ModalState =
    | { type: 'none' }
    | { type: 'createCollection' }
    | { type: 'createUnit'; collection: UnitCollectionEntity }
    | { type: 'createLesson'; unit: UnitEntity }
    | { type: 'createMaterial'; lesson: LessonEntity }
    | { type: 'editMaterial'; material: MaterialEntity }
    | { type: 'sourceDocuments'; collection: UnitCollectionEntity };

export const LessonLibraryPage: React.FC = () => {
    const {
        unitCollections,
        materials,
        getUnitsForCollection,
        getLessonsForUnit,
        getMaterialsForLesson,
        createUnitCollection,
        deleteUnitCollection,
        createUnit,
        deleteUnit,
        createLesson,
        deleteLesson,
        createMaterial,
        deleteMaterial,
    } = useAppContext();

    const [expandedCollections, setExpandedCollections] = useState<Set<string>>(new Set());
    const [expandedUnits, setExpandedUnits] = useState<Set<string>>(new Set());
    const [expandedLessons, setExpandedLessons] = useState<Set<string>>(new Set());
    const [modal, setModal] = useState<ModalState>({ type: 'none' });

    // Search state (s4A(3)(b))
    const [searchQuery, setSearchQuery] = useState('');

    const toggleSet = (id: string, set: Set<string>, setter: React.Dispatch<React.SetStateAction<Set<string>>>) => {
        const next = new Set(set);
        if (next.has(id)) next.delete(id);
        else next.add(id);
        setter(next);
    };

    // Search logic: filter materials by title and content
    const matchingMaterialIds = useMemo(() => {
        if (!searchQuery.trim()) return null; // null means show all
        const query = searchQuery.toLowerCase();
        return new Set(
            materials
                .filter(m =>
                    m.title.toLowerCase().includes(query) ||
                    m.content.toLowerCase().includes(query)
                )
                .map(m => m.id)
        );
    }, [searchQuery, materials]);

    // Determine which hierarchy items to show based on search
    const { visibleCollections, visibleUnits, visibleLessons } = useMemo(() => {
        if (!matchingMaterialIds) {
            // No search active - show all
            return {
                visibleCollections: new Set(unitCollections.map(c => c.id)),
                visibleUnits: new Set(getUnitsForCollection('').map(u => u.id)), // Will be filtered per collection
                visibleLessons: new Set(), // Will be filtered per unit
            };
        }

        // Filter to only show items containing matching materials
        const lessonIds = new Set<string>();
        const unitIds = new Set<string>();
        const collectionIds = new Set<string>();

        materials.forEach(m => {
            if (matchingMaterialIds.has(m.id)) {
                lessonIds.add(m.lessonId);
            }
        });

        // Get all lessons and find their units
        unitCollections.forEach(c => {
            getUnitsForCollection(c.id).forEach(u => {
                getLessonsForUnit(u.id).forEach(l => {
                    if (lessonIds.has(l.id)) {
                        unitIds.add(u.id);
                        collectionIds.add(c.id);
                    }
                });
            });
        });

        return {
            visibleCollections: collectionIds,
            visibleUnits: unitIds,
            visibleLessons: lessonIds,
        };
    }, [matchingMaterialIds, unitCollections, materials, getUnitsForCollection, getLessonsForUnit]);

    const handleCreateCollection = async (title: string) => {
        await createUnitCollection({ title });
    };

    const handleCreateUnit = async (collectionId: string, title: string) => {
        await createUnit({ unitCollectionId: collectionId, title });
    };

    const handleCreateLesson = async (unitId: string, title: string, description: string) => {
        await createLesson({ unitId, title, description });
    };

    const handleCreateMaterial = async (lessonId: string, title: string, materialType: MaterialType) => {
        await createMaterial({ lessonId, title, content: '', materialType });
    };

    const handleDeleteMaterial = async (materialId: string) => {
        await deleteMaterial(materialId);
    };

    // Filter collections based on search
    const filteredCollections = matchingMaterialIds
        ? unitCollections.filter(c => visibleCollections.has(c.id))
        : unitCollections;

    return (
        <div className="flex h-[calc(100vh-220px)] min-h-[500px] bg-white rounded-xl border border-gray-200 overflow-hidden shadow-sm">
            {/* Tree View */}
            <div className="flex-1 flex flex-col bg-gray-50/50">
                <div className="p-3 border-b border-gray-200 bg-white">
                    {/* Header Row */}
                    <div className="flex justify-between items-center mb-2">
                        <span className="text-xs font-semibold text-gray-500 uppercase tracking-wider">Explorer</span>
                        <button
                            onClick={() => setModal({ type: 'createCollection' })}
                            className="p-1.5 hover:bg-brand-orange/10 rounded transition-all text-brand-orange"
                            title="Add collection"
                        >
                            <PlusIcon />
                        </button>
                    </div>

                    {/* Search Bar (s4A(3)(b)) */}
                    <div className="relative">
                        <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none text-gray-400">
                            <SearchIcon />
                        </div>
                        <input
                            type="text"
                            value={searchQuery}
                            onChange={(e) => setSearchQuery(e.target.value)}
                            placeholder="Search materials..."
                            className="w-full pl-9 pr-3 py-1.5 text-sm bg-gray-50 text-text-body rounded-md border border-gray-200 focus:border-brand-orange focus:ring-1 focus:ring-brand-orange focus:outline-none"
                        />
                        {searchQuery && (
                            <button
                                onClick={() => setSearchQuery('')}
                                className="absolute inset-y-0 right-0 pr-3 flex items-center text-gray-400 hover:text-gray-600"
                            >
                                <svg xmlns="http://www.w3.org/2000/svg" className="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
                                </svg>
                            </button>
                        )}
                    </div>
                </div>

                <div className="flex-1 overflow-y-auto p-2 space-y-1">
                    {filteredCollections.length === 0 && !searchQuery && (
                        <div className="text-center py-8">
                            <p className="text-sm text-gray-400 mb-2">No collections yet</p>
                            <button
                                onClick={() => setModal({ type: 'createCollection' })}
                                className="text-sm text-brand-orange hover:underline"
                            >
                                Create your first collection
                            </button>
                        </div>
                    )}

                    {filteredCollections.length === 0 && searchQuery && (
                        <div className="text-center py-8">
                            <p className="text-sm text-gray-400">No materials match "{searchQuery}"</p>
                        </div>
                    )}

                    {filteredCollections.map(collection => (
                        <div key={collection.id}>
                            {/* Collection Row - Orange left border (from prototype) */}
                            <div
                                className="flex items-center gap-2 px-2 py-1.5 rounded-r-md cursor-pointer hover:bg-brand-orange/5 transition-colors group border-l-3 border-l-brand-orange bg-brand-orange/[0.02]"
                                onClick={() => toggleSet(collection.id, expandedCollections, setExpandedCollections)}
                            >
                                <ChevronRightIcon isOpen={expandedCollections.has(collection.id)} />
                                <span className="text-sm font-medium text-text-heading truncate flex-1">{collection.title}</span>
                                <span className="text-xs text-gray-400">{getUnitsForCollection(collection.id).length}</span>
                                {/* Source Documents Button (s4A(3)(a)) */}
                                <button
                                    onClick={(e) => { e.stopPropagation(); setModal({ type: 'sourceDocuments', collection }); }}
                                    className="opacity-0 group-hover:opacity-100 p-1 hover:bg-brand-blue/10 rounded transition-all text-brand-blue"
                                    title="Source documents"
                                >
                                    <DocumentIcon />
                                </button>
                                <button
                                    onClick={(e) => { e.stopPropagation(); setModal({ type: 'createUnit', collection }); }}
                                    className="opacity-0 group-hover:opacity-100 p-1 hover:bg-brand-orange/10 rounded transition-all text-brand-orange"
                                    title="Add unit"
                                >
                                    <PlusIcon />
                                </button>
                                <button
                                    onClick={(e) => { e.stopPropagation(); deleteUnitCollection(collection.id); }}
                                    className="opacity-0 group-hover:opacity-100 p-1 hover:bg-red-100 rounded transition-all text-red-500"
                                    title="Delete collection"
                                >
                                    <TrashIcon />
                                </button>
                            </div>

                            {/* Units under Collection - Orange indent line (from prototype) */}
                            {expandedCollections.has(collection.id) && (
                                <div className="ml-4 pl-2 space-y-1 mt-1" style={{ borderLeft: '2px solid var(--color-brand-orange)' }}>
                                    {getUnitsForCollection(collection.id)
                                        .filter(u => !matchingMaterialIds || visibleUnits.has(u.id))
                                        .map(unit => (
                                            <div key={unit.id}>
                                                {/* Unit Row - Green left border (from prototype) */}
                                                <div
                                                    className="flex items-center gap-2 px-2 py-1.5 rounded-r-md cursor-pointer hover:bg-brand-green/5 transition-colors group border-l-3 border-l-brand-green bg-brand-green/[0.02]"
                                                    onClick={() => toggleSet(unit.id, expandedUnits, setExpandedUnits)}
                                                >
                                                    <ChevronRightIcon isOpen={expandedUnits.has(unit.id)} />
                                                    <span className="text-sm text-text-heading truncate flex-1">{unit.title}</span>
                                                    <span className="text-xs text-gray-400">{getLessonsForUnit(unit.id).length}</span>
                                                    <button
                                                        onClick={(e) => { e.stopPropagation(); setModal({ type: 'createLesson', unit }); }}
                                                        className="opacity-0 group-hover:opacity-100 p-1 hover:bg-brand-orange/10 rounded transition-all text-brand-orange"
                                                        title="Add lesson"
                                                    >
                                                        <PlusIcon />
                                                    </button>
                                                    <button
                                                        onClick={(e) => { e.stopPropagation(); deleteUnit(unit.id); }}
                                                        className="opacity-0 group-hover:opacity-100 p-1 hover:bg-red-100 rounded transition-all text-red-500"
                                                        title="Delete unit"
                                                    >
                                                        <TrashIcon />
                                                    </button>
                                                </div>

                                                {/* Lessons under Unit - Green indent line (from prototype) */}
                                                {expandedUnits.has(unit.id) && (
                                                    <div className="ml-4 pl-2 space-y-0.5 mt-1" style={{ borderLeft: '2px solid var(--color-brand-green)' }}>
                                                        {getLessonsForUnit(unit.id)
                                                            .filter(l => !matchingMaterialIds || visibleLessons.has(l.id))
                                                            .map(lesson => (
                                                                <div key={lesson.id}>
                                                                    {/* Lesson Row - Blue left border (from prototype) */}
                                                                    <div
                                                                        className="flex items-center gap-2 px-2 py-1 rounded-r-md cursor-pointer hover:bg-brand-blue/5 transition-colors border-l-3 border-l-brand-blue bg-brand-blue/[0.02] group"
                                                                        onClick={() => toggleSet(lesson.id, expandedLessons, setExpandedLessons)}
                                                                    >
                                                                        <ChevronRightIcon isOpen={expandedLessons.has(lesson.id)} />
                                                                        <span className="text-sm text-text-body truncate flex-1">{lesson.title}</span>
                                                                        <span className="text-xs text-gray-400">({getMaterialsForLesson(lesson.id).length})</span>
                                                                        <button
                                                                            onClick={(e) => { e.stopPropagation(); setModal({ type: 'createMaterial', lesson }); }}
                                                                            className="opacity-0 group-hover:opacity-100 p-1 hover:bg-brand-orange/10 rounded transition-all text-brand-orange"
                                                                            title="Add material"
                                                                        >
                                                                            <PlusIcon />
                                                                        </button>
                                                                        <button
                                                                            onClick={(e) => { e.stopPropagation(); deleteLesson(lesson.id); }}
                                                                            className="opacity-0 group-hover:opacity-100 p-1 hover:bg-red-100 rounded transition-all text-red-500"
                                                                            title="Delete lesson"
                                                                        >
                                                                            <TrashIcon />
                                                                        </button>
                                                                    </div>

                                                                    {/* Materials in Lesson - Blue indent line (from prototype) */}
                                                                    {expandedLessons.has(lesson.id) && (
                                                                        <div className="ml-4 pl-2 space-y-0.5 mt-0.5" style={{ borderLeft: '2px solid var(--color-brand-blue)' }}>
                                                                            {getMaterialsForLesson(lesson.id).length === 0 ? (
                                                                                <p className="text-xs text-gray-400 py-2">No materials yet</p>
                                                                            ) : (
                                                                                getMaterialsForLesson(lesson.id)
                                                                                    .filter(m => !matchingMaterialIds || matchingMaterialIds.has(m.id))
                                                                                    .map(material => (
                                                                                        <div
                                                                                            key={material.id}
                                                                                            className={`flex items-center gap-2 px-2 py-1 rounded-md cursor-pointer transition-colors border-l-2 border-l-gray-200 group hover:bg-gray-50 ${matchingMaterialIds?.has(material.id) ? 'bg-yellow-50 border-l-yellow-400' : ''
                                                                                                }`}
                                                                                            onClick={() => setModal({ type: 'editMaterial', material })}
                                                                                        >
                                                                                            {getContentIcon(material.materialType)}
                                                                                            <span className="text-sm text-text-body truncate flex-1">{material.title}</span>
                                                                                            <button
                                                                                                onClick={(e) => { e.stopPropagation(); handleDeleteMaterial(material.id); }}
                                                                                                className="opacity-0 group-hover:opacity-100 p-1 hover:bg-red-100 rounded transition-all text-red-500"
                                                                                                title="Delete material"
                                                                                            >
                                                                                                <TrashIcon />
                                                                                            </button>
                                                                                        </div>
                                                                                    ))
                                                                            )}
                                                                        </div>
                                                                    )}
                                                                </div>
                                                            ))}
                                                        {getLessonsForUnit(unit.id).length === 0 && (
                                                            <p className="text-xs text-gray-400 py-2">No lessons yet</p>
                                                        )}
                                                    </div>
                                                )}
                                            </div>
                                        ))}
                                    {getUnitsForCollection(collection.id).length === 0 && (
                                        <p className="text-xs text-gray-400 py-2">No units yet</p>
                                    )}
                                </div>
                            )}
                        </div>
                    ))}
                </div>
            </div>

            {/* Modals */}
            {modal.type === 'createCollection' && (
                <CreateCollectionModal
                    onClose={() => setModal({ type: 'none' })}
                    onCreate={handleCreateCollection}
                />
            )}
            {modal.type === 'createUnit' && (
                <CreateUnitModal
                    collectionId={modal.collection.id}
                    collectionTitle={modal.collection.title}
                    onClose={() => setModal({ type: 'none' })}
                    onCreate={(title) => handleCreateUnit(modal.collection.id, title)}
                />
            )}
            {modal.type === 'createLesson' && (
                <CreateLessonModal
                    unitId={modal.unit.id}
                    unitTitle={modal.unit.title}
                    onClose={() => setModal({ type: 'none' })}
                    onCreate={(title, description) => handleCreateLesson(modal.unit.id, title, description)}
                />
            )}
            {modal.type === 'createMaterial' && (
                <CreateMaterialModal
                    lessonId={modal.lesson.id}
                    lessonTitle={modal.lesson.title}
                    onClose={() => setModal({ type: 'none' })}
                    onCreate={(title, materialType) => handleCreateMaterial(modal.lesson.id, title, materialType)}
                />
            )}
            {modal.type === 'sourceDocuments' && (
                <SourceDocumentModal
                    collectionId={modal.collection.id}
                    collectionTitle={modal.collection.title}
                    onClose={() => setModal({ type: 'none' })}
                />
            )}
            {modal.type === 'editMaterial' && (
                <EditorModal
                    material={modal.material}
                    onClose={() => setModal({ type: 'none' })}
                />
            )}
        </div>
    );
};
