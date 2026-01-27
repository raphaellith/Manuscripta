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

// Per Validation Rules §2B and AdditionalValidationRules §2E
export type QuestionType = 'MULTIPLE_CHOICE' | 'WRITTEN_ANSWER';

export interface QuestionEntity {
    id: string;
    materialId: string;
    questionType: QuestionType;
    questionText: string;
    options?: string[];         // Required for MULTIPLE_CHOICE
    correctAnswer?: string | number;  // Index for MC, string for WRITTEN (exact match)
    maxScore?: number;
    markScheme?: string;        // Per s2E(1)(a) - for AI-marking of WRITTEN_ANSWER
}

// Per AdditionalValidationRules §3B
export interface AttachmentEntity {
    id: string;
    materialId: string;
    fileBaseName: string;
    fileExtension: string;
}
