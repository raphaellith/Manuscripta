/**
 * Lesson Library Page component.
 * Uses tree view UI from prototype LibraryVariantTree.tsx.
 * Per WindowsAppStructureSpec §2B(1)(d)(i).
 */

import React, { useState } from 'react';
import { useAppContext } from '../../state/AppContext';
import { CreateCollectionModal } from '../modals/CreateCollectionModal';
import { CreateUnitModal } from '../modals/CreateUnitModal';
import { CreateLessonModal } from '../modals/CreateLessonModal';
import { CreateMaterialModal } from '../modals/CreateMaterialModal';
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

// Modal state types
type ModalState =
    | { type: 'none' }
    | { type: 'createCollection' }
    | { type: 'createUnit'; collection: UnitCollectionEntity }
    | { type: 'createLesson'; unit: UnitEntity }
    | { type: 'createMaterial'; lesson: LessonEntity };

export const LessonLibraryPage: React.FC = () => {
    const {
        unitCollections,
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
    const [selectedMaterial, setSelectedMaterial] = useState<MaterialEntity | null>(null);
    const [modal, setModal] = useState<ModalState>({ type: 'none' });

    const toggleSet = (id: string, set: Set<string>, setter: React.Dispatch<React.SetStateAction<Set<string>>>) => {
        const next = new Set(set);
        if (next.has(id)) next.delete(id);
        else next.add(id);
        setter(next);
    };

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
        // Clear detail panel if deleting the currently selected material
        if (selectedMaterial?.id === materialId) {
            setSelectedMaterial(null);
        }
        await deleteMaterial(materialId);
    };

    return (
        <div className="flex h-[calc(100vh-220px)] min-h-[500px] bg-white rounded-xl border border-gray-200 overflow-hidden shadow-sm">
            {/* Tree Sidebar - from prototype */}
            <div className="w-80 border-r border-gray-200 flex flex-col bg-gray-50/50">
                <div className="p-3 border-b border-gray-200 bg-white flex justify-between items-center">
                    <span className="text-xs font-semibold text-gray-500 uppercase tracking-wider">Explorer</span>
                    <button
                        onClick={() => setModal({ type: 'createCollection' })}
                        className="p-1.5 hover:bg-brand-orange/10 rounded transition-all text-brand-orange"
                        title="Add collection"
                    >
                        <PlusIcon />
                    </button>
                </div>

                <div className="flex-1 overflow-y-auto p-2 space-y-1">
                    {unitCollections.length === 0 && (
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

                    {unitCollections.map(collection => (
                        <div key={collection.id}>
                            {/* Collection Row - Orange left border (from prototype) */}
                            <div
                                className="flex items-center gap-2 px-2 py-1.5 rounded-r-md cursor-pointer hover:bg-brand-orange/5 transition-colors group border-l-3 border-l-brand-orange bg-brand-orange/[0.02]"
                                onClick={() => toggleSet(collection.id, expandedCollections, setExpandedCollections)}
                            >
                                <ChevronRightIcon isOpen={expandedCollections.has(collection.id)} />
                                <span className="text-sm font-medium text-text-heading truncate flex-1">{collection.title}</span>
                                <span className="text-xs text-gray-400">{getUnitsForCollection(collection.id).length}</span>
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
                                    {getUnitsForCollection(collection.id).map(unit => (
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
                                                    {getLessonsForUnit(unit.id).map(lesson => (
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
                                                                        getMaterialsForLesson(lesson.id).map(material => (
                                                                            <div
                                                                                key={material.id}
                                                                                className={`flex items-center gap-2 px-2 py-1 rounded-md cursor-pointer transition-colors border-l-2 border-l-gray-200 group ${selectedMaterial?.id === material.id ? 'bg-brand-orange/10 border-l-brand-orange' : 'hover:bg-gray-50'
                                                                                    }`}
                                                                                onClick={() => setSelectedMaterial(material)}
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

            {/* Detail Panel - from prototype */}
            <div className="flex-1 flex flex-col">
                {selectedMaterial ? (
                    <>
                        <div className="p-6 border-b border-gray-200 bg-gradient-to-r from-gray-50 to-white">
                            <div className="flex items-center gap-3 mb-3">
                                <h2 className="text-2xl font-semibold text-text-heading">{selectedMaterial.title}</h2>
                                <span className={`text-xs font-semibold px-2 py-1 rounded-md uppercase tracking-wide ${selectedMaterial.materialType === 'READING' ? 'bg-brand-green text-white' :
                                    selectedMaterial.materialType === 'WORKSHEET' ? 'bg-brand-yellow text-gray-900' :
                                        'bg-purple-500 text-white'
                                    }`}>
                                    {selectedMaterial.materialType}
                                </span>
                            </div>
                            <p className="text-sm text-gray-500">
                                Created {new Date(selectedMaterial.timestamp).toLocaleDateString()}
                            </p>
                        </div>
                        <div className="flex-1 p-6 overflow-y-auto">
                            {selectedMaterial.content ? (
                                <div className="prose prose-sm max-w-none" dangerouslySetInnerHTML={{ __html: selectedMaterial.content }} />
                            ) : (
                                <p className="text-gray-400 italic">No content yet. Click Edit to add content.</p>
                            )}
                        </div>
                        <div className="p-4 border-t border-gray-200 bg-white flex gap-3">
                            <button className="px-4 py-2 bg-brand-green text-white font-medium text-sm rounded-md hover:bg-green-800 transition-colors">
                                Edit Content
                            </button>
                            <button
                                onClick={() => handleDeleteMaterial(selectedMaterial.id)}
                                className="px-4 py-2 bg-white text-red-600 border border-red-300 font-medium text-sm rounded-md hover:bg-red-50 transition-colors"
                            >
                                Delete
                            </button>
                        </div>
                    </>
                ) : (
                    <div className="flex-1 flex items-center justify-center text-gray-400">
                        <div className="text-center">
                            <svg xmlns="http://www.w3.org/2000/svg" className="h-16 w-16 mx-auto mb-4 opacity-30" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1} d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
                            </svg>
                            <p className="text-lg">Select a material to view details</p>
                        </div>
                    </div>
                )}
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
        </div>
    );
};
