export type View = 'dashboard' | 'lesson-library' | 'ai-assistant' | 'classroom-control' | 'settings' | 'lesson-creator';

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
}

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