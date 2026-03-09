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

// Per MaterialConversionSpecification §1(5)(e-g)
export type LinePatternType = 'RULED' | 'SQUARE' | 'ISOMETRIC' | 'NONE';
export type LineSpacingPreset = 'SMALL' | 'MEDIUM' | 'LARGE' | 'EXTRA_LARGE';
export type FontSizePreset = 'SMALL' | 'MEDIUM' | 'LARGE' | 'EXTRA_LARGE';

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
    linePatternType?: LinePatternType | null;
    lineSpacingPreset?: LineSpacingPreset | null;
    fontSizePreset?: FontSizePreset | null;
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

// ==========================================
// Classroom Entities - FrontendWorkflowSpec §5
// ==========================================

/**
 * Paired device entity per Pairing Process §2(2)(c).
 * Contains device ID and user-friendly name.
 */
export interface PairedDeviceEntity {
    deviceId: string;
    name: string;
}

/**
 * Device status values per Validation Rules §2E(1)(b).
 */
export type DeviceStatus = 'ON_TASK' | 'IDLE' | 'LOCKED' | 'DISCONNECTED';

/**
 * Device status entity per Session Interaction §4(2).
 * Contains current status and metadata for a paired device.
 */
export interface DeviceStatusEntity {
    deviceId: string;
    status: DeviceStatus;
    batteryLevel: number;
    currentMaterialId: string;
    studentView: string;
    timestamp: number;
}

// ==========================================
// Response Entities - FrontendWorkflowSpec §6
// ==========================================

/**
 * Response entity per Validation Rules §2C(3).
 */
export interface ResponseEntity {
    id: string;
    questionId: string;
    deviceId: string;
    responseContent: string;
    timestamp: string;
    isCorrect?: boolean;
}

/**
 * Feedback status per AdditionalValidationRules §3AE.
 */
export type FeedbackStatus = 'PROVISIONAL' | 'READY' | 'DELIVERED';

/**
 * Feedback entity per AdditionalValidationRules §3AE.
 */
export interface FeedbackEntity {
    id: string;
    responseId: string;
    text: string | null;
    marks: number | null;
    status: FeedbackStatus;
}

// ==========================================
// External Device Entities
// ==========================================

export type ExternalDeviceType = 'REMARKABLE' | 'KINDLE';

/**
 * Unified external device entity representing paired e-ink reader.
 */
export interface ExternalDeviceEntity {
    deviceId: string;
    name: string;
    type: ExternalDeviceType;
    configurationData?: string;
    /** Per AdditionalValidationRules §3D(1)(e): per-device line pattern override. */
    linePatternType?: LinePatternType | null;
    /** Per AdditionalValidationRules §3D(1)(f): per-device line spacing override. */
    lineSpacingPreset?: LineSpacingPreset | null;
    /** Per AdditionalValidationRules §3D(1)(g): per-device font size override. */
    fontSizePreset?: FontSizePreset | null;
}

/**
 * Email credentials for dispatching materials via SMTP.
 */
export interface EmailCredentialEntity {
    id: string;
    emailAddress: string;
    smtpHost: string;
    smtpPort: number;
    password?: string; // May be returned as '********' by backend
}

// ==========================================
// Configuration Entities - FrontendWorkflowSpec §5H and §7
// ==========================================

/**
 * Feedback style values per Validation Rules §2G(1)(b).
 */
export type FeedbackStyle = 'IMMEDIATE' | 'NEUTRAL';

/**
 * Mascot selection values per Validation Rules §2G(1)(f).
 */
export type MascotSelection = 'NONE' | 'MASCOT1' | 'MASCOT2' | 'MASCOT3' | 'MASCOT4' | 'MASCOT5';

/**
 * Configuration entity per Validation Rules §2G(1) and ConfigurationManagementSpecification §1(2).
 * Represents device configuration settings.
 */
export interface ConfigurationEntity {
    id: string;
    textSize: number;
    feedbackStyle: FeedbackStyle;
    ttsEnabled: boolean;
    aiScaffoldingEnabled: boolean;
    summarisationEnabled: boolean;
    mascotSelection: MascotSelection;
}

// ==========================================
// Source Document Entities - FrontendWorkflowSpec §4AA
// ==========================================

/**
 * Embedding status per AdditionalValidationRules §3A(1)(c).
 */
export type EmbeddingStatus = 'PENDING' | 'INDEXED' | 'FAILED';

/**
 * Source document entity per AdditionalValidationRules §3A(1).
 * Represents a source document imported into a unit collection.
 */
export interface SourceDocumentEntity {
    id: string;
    unitCollectionId: string;
    transcript: string;
    embeddingStatus?: EmbeddingStatus;
}

// ==========================================
// PDF Export Settings - FrontendWorkflowSpec §7(5)
// ==========================================

/**
 * PDF export settings entity per AdditionalValidationRules §3F.
 * Represents global default PDF export settings.
 */
export interface PdfExportSettingsEntity {
    id: string;
    linePatternType: LinePatternType;
    lineSpacingPreset: LineSpacingPreset;
    fontSizePreset: FontSizePreset;
}

