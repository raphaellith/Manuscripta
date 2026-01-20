/**
 * DTO interfaces for create operations.
 * Aligned with Main/Models/Dtos per NetworkingAPISpec §1(1).
 */

import type { MaterialType, QuestionType } from './entities';

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

// Per NetworkingAPISpec §1(1)(d1)
export interface InternalCreateQuestionDto {
    materialId: string;
    questionType: QuestionType;
    questionText: string;
    options?: string[];           // Required for MULTIPLE_CHOICE
    correctAnswerIndex?: number;  // For MULTIPLE_CHOICE (null = auto-marking disabled)
    correctAnswer?: string;       // For WRITTEN_ANSWER (null = auto-marking disabled)
    markScheme?: string;          // Per §2E(1)(a) - for AI-marking
    maxScore?: number;
}

export interface InternalUpdateQuestionDto {
    id: string;
    materialId: string;
    questionType: QuestionType;
    questionText: string;
    options?: string[];
    correctAnswerIndex?: number;  // For MULTIPLE_CHOICE (null = auto-marking disabled)
    correctAnswer?: string;       // For WRITTEN_ANSWER (null = auto-marking disabled)
    markScheme?: string;          // Per §2E(1)(a) - for AI-marking
    maxScore?: number;
}

// Per NetworkingAPISpec §1(1)(l)
export interface InternalCreateAttachmentDto {
    materialId: string;
    fileBaseName: string;
    fileExtension: string;
}
