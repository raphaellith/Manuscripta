/**
 * Lesson Library Page component.
 * Uses tree view UI from prototype LibraryVariantTree.tsx.
 * Per WindowsAppStructureSpec §2B(1)(d)(i).
 */

import React, { useMemo, useState } from 'react';
import { useAppContext } from '../../state/AppContext';
import { CreateCollectionModal } from '../modals/CreateCollectionModal';
import { CreateUnitModal } from '../modals/CreateUnitModal';
import { CreateLessonModal } from '../modals/CreateLessonModal';
import { CreateMaterialModal } from '../modals/CreateMaterialModal';
import { EditorModal } from '../editor/EditorModal';
import { markdownToHtml } from '../../utils/markdownConversion';
import type { UnitCollectionEntity, UnitEntity, LessonEntity, MaterialEntity, MaterialType, GenerationRequest, GenerationResult } from '../../models';

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
    | { type: 'createMaterial'; lesson: LessonEntity }
    | { type: 'editMaterial'; material: MaterialEntity };

export const LessonLibraryPage: React.FC = () => {
    const {
        unitCollections,
        units,
        getUnitsForCollection,
        getLessonsForUnit,
        getMaterialsForLesson,
        getQuestionsForMaterial,
        createUnitCollection,
        deleteUnitCollection,
        createUnit,
        deleteUnit,
        createLesson,
        deleteLesson,
        createMaterial,
        updateMaterial,
        deleteMaterial,
        generateReading,
        generateWorksheet,
    } = useAppContext();

    const [expandedCollections, setExpandedCollections] = useState<Set<string>>(new Set());
    const [expandedUnits, setExpandedUnits] = useState<Set<string>>(new Set());
    const [expandedLessons, setExpandedLessons] = useState<Set<string>>(new Set());
    const [modal, setModal] = useState<ModalState>({ type: 'none' });
    const [searchQuery, setSearchQuery] = useState('');

    const searchKeywords = useMemo(
        () => searchQuery.trim().toLowerCase().split(/\s+/).filter(Boolean),
        [searchQuery]
    );
    const isSearching = searchKeywords.length > 0;

    const getVisibleTextFromContent = (content: string) => {
        if (!content.trim()) return '';
        const html = markdownToHtml(content);
        const text = html
            .replace(/<[^>]*>/g, ' ')
            .replace(/\[Question:\s*[^\]]+\]/gi, ' ')
            .replace(/\[PDF:\s*[^\]]+\]/gi, ' ')
            .replace(/\s+/g, ' ')
            .trim();
        return text;
    };

    const filteredMaterialsByLesson = useMemo(() => {
        const result = new Map<string, MaterialEntity[]>();
        const materialMatchesSearch = (material: MaterialEntity) => {
            if (!isSearching) return true;
            const questionText = getQuestionsForMaterial(material.id)
                .map(question => {
                    const options = question.options?.join(' ') ?? '';
                    return `${question.questionText} ${options}`;
                })
                .join(' ');
            const visibleContent = getVisibleTextFromContent(material.content || '');
            const haystack = `${material.title} ${visibleContent} ${questionText}`.toLowerCase();
            return searchKeywords.every(keyword => haystack.includes(keyword));
        };

        unitCollections.forEach(collection => {
            getUnitsForCollection(collection.id).forEach(unit => {
                getLessonsForUnit(unit.id).forEach(lesson => {
                    const materials = getMaterialsForLesson(lesson.id);
                    const filtered = isSearching
                        ? materials.filter(materialMatchesSearch)
                        : materials;
                    result.set(lesson.id, filtered);
                });
            });
        });

        return result;
    }, [
        getLessonsForUnit,
        getMaterialsForLesson,
        getQuestionsForMaterial,
        getUnitsForCollection,
        isSearching,
        searchKeywords,
        unitCollections,
    ]);

    const getFilteredMaterialsForLesson = (lessonId: string) =>
        filteredMaterialsByLesson.get(lessonId) ?? [];

    const filteredLessonsByUnit = useMemo(() => {
        const result = new Map<string, LessonEntity[]>();
        unitCollections.forEach(collection => {
            getUnitsForCollection(collection.id).forEach(unit => {
                const lessons = getLessonsForUnit(unit.id);
                const filtered = isSearching
                    ? lessons.filter(lesson => (filteredMaterialsByLesson.get(lesson.id)?.length ?? 0) > 0)
                    : lessons;
                result.set(unit.id, filtered);
            });
        });
        return result;
    }, [
        filteredMaterialsByLesson,
        getLessonsForUnit,
        getUnitsForCollection,
        isSearching,
        unitCollections,
    ]);

    const getFilteredLessonsForUnit = (unitId: string) =>
        filteredLessonsByUnit.get(unitId) ?? [];

    const filteredUnitsByCollection = useMemo(() => {
        const result = new Map<string, UnitEntity[]>();
        unitCollections.forEach(collection => {
            const units = getUnitsForCollection(collection.id);
            const filtered = isSearching
                ? units.filter(unit => (filteredLessonsByUnit.get(unit.id)?.length ?? 0) > 0)
                : units;
            result.set(collection.id, filtered);
        });
        return result;
    }, [getUnitsForCollection, isSearching, filteredLessonsByUnit, unitCollections]);

    const getFilteredUnitsForCollection = (collectionId: string) =>
        filteredUnitsByCollection.get(collectionId) ?? [];

    const visibleCollections = useMemo(
        () => (isSearching
            ? unitCollections.filter(collection => (filteredUnitsByCollection.get(collection.id)?.length ?? 0) > 0)
            : unitCollections),
        [filteredUnitsByCollection, isSearching, unitCollections]
    );

    const hasSearchResults = !isSearching || visibleCollections.length > 0;

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

    // Per FrontendWorkflowSpec §4B: AI generation handler
    const handleCreateMaterialWithAI = async (
        lessonId: string,
        title: string,
        materialType: MaterialType,
        generationRequest: GenerationRequest,
        readingAge: number,
        actualAge: number
    ) => {
        // Per §4B(2): First create material with empty content
        const createdMaterial = await createMaterial({
            lessonId,
            title,
            content: '',
            materialType,
        });

        try {
            // Per §4B(2)(a): Invoke GenerateReading or GenerateWorksheet
            const generationResult: GenerationResult = materialType === 'READING'
                ? await generateReading(generationRequest)
                : await generateWorksheet(generationRequest);

            // Update material with generated content
            await updateMaterial({
                ...createdMaterial,
                content: generationResult.content,
                readingAge,
                actualAge,
            });

            // Per §4B(2)(b): Display content in editor modal
            // Note: Validation warnings will be displayed by the editor modal if present
            setModal({ type: 'editMaterial', material: {
                ...createdMaterial,
                content: generationResult.content,
                readingAge,
                actualAge,
            }});
        } catch (error) {
            console.error('AI generation failed:', error);
            // Roll back material creation
            await deleteMaterial(createdMaterial.id);
            throw error;
        }
    };

    const handleDeleteMaterial = async (materialId: string) => {
        await deleteMaterial(materialId);
    };

    return (
        <div className="flex h-[calc(100vh-220px)] min-h-[500px] bg-white rounded-xl border border-gray-200 overflow-hidden shadow-sm">
            {/* Tree View */}
            <div className="flex-1 flex flex-col bg-gray-50/50">
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

                <div className="p-3 border-b border-gray-200 bg-white">
                    <input
                        type="text"
                        value={searchQuery}
                        onChange={(event) => setSearchQuery(event.target.value)}
                        placeholder="Search materials"
                        className="w-full rounded-md border border-gray-200 bg-white px-3 py-2 text-sm text-text-body focus:border-brand-orange focus:ring-1 focus:ring-brand-orange focus:outline-none"
                    />
                </div>

                <div className="flex-1 overflow-y-auto p-2 space-y-1">
                    {isSearching && !hasSearchResults && (
                        <div className="text-center py-8">
                            <p className="text-sm text-gray-400">No materials match your search.</p>
                        </div>
                    )}
                    {!isSearching && unitCollections.length === 0 && (
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

                    {visibleCollections.map(collection => {
                        const unitsForCollection = getFilteredUnitsForCollection(collection.id);

                        return (
                        <div key={collection.id}>
                            {/* Collection Row - Orange left border (from prototype) */}
                            <div
                                className="flex items-center gap-2 px-2 py-1.5 rounded-r-md cursor-pointer hover:bg-brand-orange/5 transition-colors group border-l-3 border-l-brand-orange bg-brand-orange/[0.02]"
                                onClick={() => toggleSet(collection.id, expandedCollections, setExpandedCollections)}
                            >
                                <ChevronRightIcon isOpen={expandedCollections.has(collection.id)} />
                                <span className="text-sm font-medium text-text-heading truncate flex-1">{collection.title}</span>
                                <span className="text-xs text-gray-400">{unitsForCollection.length}</span>
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
                                    {unitsForCollection.map(unit => {
                                        const lessonsForUnit = getFilteredLessonsForUnit(unit.id);

                                        return (
                                        <div key={unit.id}>
                                            {/* Unit Row - Green left border (from prototype) */}
                                            <div
                                                className="flex items-center gap-2 px-2 py-1.5 rounded-r-md cursor-pointer hover:bg-brand-green/5 transition-colors group border-l-3 border-l-brand-green bg-brand-green/[0.02]"
                                                onClick={() => toggleSet(unit.id, expandedUnits, setExpandedUnits)}
                                            >
                                                <ChevronRightIcon isOpen={expandedUnits.has(unit.id)} />
                                                <span className="text-sm text-text-heading truncate flex-1">{unit.title}</span>
                                                <span className="text-xs text-gray-400">{lessonsForUnit.length}</span>
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
                                                    {lessonsForUnit.map(lesson => {
                                                        const materialsForLesson = getFilteredMaterialsForLesson(lesson.id);

                                                        return (
                                                            <div key={lesson.id}>
                                                                {/* Lesson Row - Blue left border (from prototype) */}
                                                                <div
                                                                    className="flex items-center gap-2 px-2 py-1 rounded-r-md cursor-pointer hover:bg-brand-blue/5 transition-colors border-l-3 border-l-brand-blue bg-brand-blue/[0.02] group"
                                                                    onClick={() => toggleSet(lesson.id, expandedLessons, setExpandedLessons)}
                                                                >
                                                                    <ChevronRightIcon isOpen={expandedLessons.has(lesson.id)} />
                                                                    <span className="text-sm text-text-body truncate flex-1">{lesson.title}</span>
                                                                    <span className="text-xs text-gray-400">({materialsForLesson.length})</span>
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
                                                                        {materialsForLesson.length === 0 ? (
                                                                            <p className="text-xs text-gray-400 py-2">No materials yet</p>
                                                                        ) : (
                                                                            materialsForLesson.map(material => (
                                                                                <div
                                                                                    key={material.id}
                                                                                    className="flex items-center gap-2 px-2 py-1 rounded-md cursor-pointer transition-colors border-l-2 border-l-gray-200 group hover:bg-gray-50"
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
                                                        );
                                                    })}
                                                    {lessonsForUnit.length === 0 && (
                                                        <p className="text-xs text-gray-400 py-2">No lessons yet</p>
                                                    )}
                                                </div>
                                            )}
                                                </div>
                                            );
                                            })}
                                            {unitsForCollection.length === 0 && (
                                        <p className="text-xs text-gray-400 py-2">No units yet</p>
                                    )}
                                </div>
                            )}
                        </div>
                            );
                            })}
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
            {modal.type === 'createMaterial' && (() => {
                // Find unit collection ID for AI generation context
                const lesson = modal.lesson;
                const unit = units.find(u => u.id === lesson.unitId);
                const unitCollectionId = unit?.unitCollectionId;

                return (
                    <CreateMaterialModal
                        lessonId={lesson.id}
                        lessonTitle={lesson.title}
                        unitCollectionId={unitCollectionId}
                        onClose={() => setModal({ type: 'none' })}
                        onCreate={(title, materialType) => handleCreateMaterial(lesson.id, title, materialType)}
                        onCreateWithAI={(title, materialType, generationRequest, readingAge, actualAge) =>
                            handleCreateMaterialWithAI(
                                lesson.id,
                                title,
                                materialType,
                                generationRequest,
                                readingAge,
                                actualAge
                            )
                        }
                    />
                );
            })()}
            {modal.type === 'editMaterial' && (
                <EditorModal
                    material={modal.material}
                    onClose={() => setModal({ type: 'none' })}
                />
            )}
        </div>
    );
};
