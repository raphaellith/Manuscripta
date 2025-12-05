
export enum View {
  Lesson,
  Quiz,
  Worksheet,
  AIAssist,
  FeedbackCorrect,
  FeedbackIncorrect,
}

export enum AITask {
  Simplify = 'Simplify',
  Expand = 'Expand',
  Summarise = 'Summarise',
}

export type QuizQuestion = {
  question: string;
  options: string[];
  correctAnswerIndex: number;
};

export type WorksheetItem = {
  type: 'fill-in-the-blank';
  sentence: string;
  blankWord: string;
};