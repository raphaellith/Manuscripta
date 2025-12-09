import { QuizQuestion, WorksheetItem } from './types';

// Global text size for content
export const CONTENT_TEXT_SIZE = "text-3xl"; // consistent across all content views

export const LESSON_TITLE = "The Battle of Hastings";

export const LESSON_CONTENT = [
  "In 1066, a power struggle for the English throne culminated in the Battle of Hastings. After the death of King Edward the Confessor, Harold Godwinson was crowned king, but his claim was challenged by William, Duke of Normandy. The two forces met near Hastings, where Harold's English army was defeated and he was killed, leading to William's conquest of England and the beginning of Norman rule."
];

export const QUIZ_QUESTIONS: QuizQuestion[] = [
  {
    question: "Who won the Battle of Hastings?",
    options: ["King Harold", "William of Normandy", "The Vikings"],
    correctAnswerIndex: 1,
  },
];

export const WORKSHEET_ITEMS: WorksheetItem[] = [
    {
        type: 'fill-in-the-blank',
        sentence: "The Battle of Hastings was fought in the year ______.",
        blankWord: "1066"
    },
    {
        type: 'fill-in-the-blank',
        sentence: "King Harold was the leader of the ______ army.",
        blankWord: "English"
    },
    {
        type: 'fill-in-the-blank',
        sentence: "The Norman army was led by ______ of Normandy.",
        blankWord: "William"
    }
];