/**
 * DTO interfaces for create operations.
 * Aligned with Main/Models/Dtos per NetworkingAPISpec §1(1).
 */

import type { MaterialType } from './entities';

export interface InternalCreateUnitCollectionDto {
    title: string;
}

export interface InternalCreateUnitDto {
    unitCollectionId: string;
    title: string;
    sourceDocuments?: string[];
}

export interface InternalCreateLessonDto {
    unitId: string;
    title: string;
    description: string;
}

export interface InternalCreateMaterialDto {
    lessonId: string;
    title: string;
    content: string;
    materialType: MaterialType;
    metadata?: string;
    vocabularyTerms?: unknown[];
    readingAge?: number;
    actualAge?: number;
}
