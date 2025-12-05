export type View = 'dashboard' | 'lesson-library' | 'lesson-creator' | 'classroom-control' | 'responses' | 'ai-assistant' | 'settings';

export interface Message {
  id: number;
  text: string;
  sender: 'user' | 'ai';
}

export type ContentType = 'Lesson' | 'Worksheet' | 'Quiz' | 'PDF' | 'Reading';

export interface Unit {
  id: string;
  title: string;
  subject: string;
  ageRange: string;
  description: string;
  sourceMaterials?: SourceMaterial[];  // Textbooks/resources for RAG-based lesson generation
}

// Source materials for units (textbooks, resources for RAG)
export interface SourceMaterial {
  id: string;
  name: string;
  type: 'textbook' | 'pdf' | 'document' | 'notes';
  size?: string;        // e.g., "2.4 MB"
  addedDate: string;    // e.g., "Dec 1, 2025"
  pages?: number;       // For textbooks/PDFs
}

export interface Collection {
  id: string;
  name: string;           // e.g., "Year 7 History", "GCSE Cohort 2025"
  description?: string;
  color?: string;         // Accent color for visual distinction
  unitIds: string[];      // Units belonging to this collection
}

export type LibraryVariant = 'tree' | 'columns' | 'cards';

export interface VocabularyTerm {
  term: string;
  definition: string;
}

export interface ContentItem {
  id: string;
  title: string;
  subject: string;
  created: string;
  status: 'Draft' | 'Deployed';
  unit: string;
  type: ContentType;
  lessonNumber?: number;
  lessonTitle?: string;
  content?: string;
  pdfPath?: string;           // Path to PDF file for PDF type
  imageUrl?: string;          // Embedded image URL for lessons with images
  vocabularyTerms?: VocabularyTerm[];  // Keywords with definitions
  readingAge?: number;        // Target reading age (e.g., 8 for "reading age of 8 years")
}

export interface LessonFolder {
  id: string;
  unit: string;
  number: number;
  title: string;
}

// === Data Model Alignment with Validation Rules ===

// Content type enum alignment
export type MaterialType = 'READING' | 'WORKSHEET' | 'POLL' | 'QUIZ' | 'LESSON';

// Question types for quizzes
export type QuestionType = 'MULTIPLE_CHOICE' | 'TRUE_FALSE' | 'WRITTEN_ANSWER';

// Device status alignment
export type DeviceStatus = 'ON_TASK' | 'IDLE' | 'HAND_RAISED' | 'LOCKED' | 'DISCONNECTED';

// Session status for tracking
export type SessionStatus = 'ACTIVE' | 'PAUSED' | 'COMPLETED' | 'CANCELLED';

// Per-tablet accessibility settings
export interface TabletAccessibilitySettings {
  textToSpeech: boolean;
  aiSummary: boolean;
  animatedAvatar: boolean;
}

// Tablet/Device interface with accessibility settings
export interface Tablet {
  id: number;
  status: DeviceStatus;
  studentName?: string;
  batteryLevel?: number;
  accessibility: TabletAccessibilitySettings;
  lastHelpRequestTime?: number; // Timestamp of last help request for alerts
}

// Global app settings
export interface AppSettings {
  helpSoundEnabled: boolean;
  helpNotificationsEnabled: boolean;
  autoLockOnDisconnect: boolean;
}

// Question entity for quizzes and polls
export interface Question {
  id: string;
  materialId: string;
  text: string;
  type: QuestionType;
  options?: string[];
  correctAnswer?: string;
}

// Student response entity
export interface StudentResponse {
  id: string;
  questionId: string;
  answer: string;
  isCorrect: boolean;
  timestamp: number;
  deviceId: string;
  synced: boolean;
  markedCorrect?: boolean; // Manual override by teacher (MAT18)
}