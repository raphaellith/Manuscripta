/**
 * Entity interfaces aligned with Main/Models/Entities.
 * Per WindowsAppStructure §2B(1)(d)(iii).
 */

export interface UnitCollectionEntity {
    id: string;  // GUID as string
    title: string;
}

export interface UnitEntity {
    id: string;
    unitCollectionId: string;
    title: string;
    sourceDocuments: string[];
}

export interface LessonEntity {
    id: string;
    unitId: string;
    title: string;
    description: string;
}

export type MaterialType = 'READING' | 'WORKSHEET' | 'POLL';

export interface MaterialEntity {
    id: string;
    lessonId: string;
    title: string;
    content: string;
    materialType: MaterialType;
    metadata?: string;
    vocabularyTerms?: unknown[];
    timestamp: string;
    readingAge?: number;
    actualAge?: number;
}
