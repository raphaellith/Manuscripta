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

export interface InternalCreateFeedbackDto {
    responseId: string;
    text: string | null;
    marks: number | null;
}

// Per NetworkingAPISpec §1(1)(k) - Source Document
export interface InternalCreateSourceDocumentDto {
    unitCollectionId: string;
    transcript: string;
}

// Per AdditionalValidationRules §3AB - Generation Request
export interface GenerationRequest {
    description: string;           // Teacher's description of desired content
    readingAge: number;            // Target reading age
    actualAge: number;             // Actual age of the audience
    durationInMinutes: number;     // Approximate completion time
    unitCollectionId: string;      // The unit collection containing source documents
    title: string;                 // The title of the material to be generated
    sourceDocumentIds?: string[];  // Optional: specific source documents for context
}

// Per AdditionalValidationRules §3AC - Generation Result
export interface GenerationResult {
    content: string;               // The generated or modified content
    warnings?: ValidationWarning[];// Validation issues that could not be automatically resolved
    createdQuestionIds?: string[]; // UUIDs for QuestionEntity objects created during worksheet generation
}

// Per AdditionalValidationRules §3AD - Validation Warning
export interface ValidationWarning {
    errorType: string;             // Code identifying the error type (e.g., MALFORMED_MARKER)
    description: string;           // Human-readable description of the issue
    lineNumber?: number;           // Optional: line number where the issue occurs
}
