
import { AITask } from '../types';

// This is a mock service to simulate calls to the Gemini API.
// In a real application, you would import { GoogleGenAI } from "@google/genai"
// and make actual API calls here.

const MOCK_LATENCY = 0; // in milliseconds

const mockResponses: Record<AITask, string> = {
  [AITask.Simplify]: "Imagine two people wanting the same toy, which is the English crown. One person, Harold, got it first. But another person, William, said the toy was promised to him. So William came with his friends and they had a big fight. William won the fight, got the toy, and became the new king. That's the Battle of Hastings!",
  [AITask.Expand]: "The Battle of Hastings in 1066 was a pivotal event in British history. It occurred after the death of the childless King Edward the Confessor, leading to a succession crisis.\n\nThree main claimants to the throne emerged: Harold Godwinson, an influential English earl who was crowned king shortly after Edward's death; William, Duke of Normandy, who claimed Edward had previously promised him the throne; and Harald Hardrada, the King of Norway.\n\nHarold Godwinson first defeated an invasion by Harald Hardrada in the north of England at the Battle of Stamford Bridge. However, he then had to force-march his exhausted army south to face William's Norman forces, who had landed at Pevensey. The two armies clashed at Senlac Hill, near Hastings, in a battle that lasted nearly a full day—unusually long for the period. The English army's shield wall was formidable, but it was eventually broken by Norman cavalry and archers after they feigned retreat. King Harold's death, famously depicted as being from an arrow to the eye, marked the decisive moment that led to the English defeat and William's eventual coronation as King of England on Christmas Day, 1066.",
  [AITask.Summarise]: "• Date: 1066\n• Location: Near Hastings, England\n• Contenders: Harold Godwinson (King of England) vs. William, Duke of Normandy.\n• Reason: Dispute over the English throne after King Edward the Confessor's death.\n• Outcome: Norman victory. Harold Godwinson was killed, and William the Conqueror became King of England, starting the Norman era.",
};


export const generateContent = <T,>(task: AITask): Promise<string> => {
  return new Promise((resolve) => {
    setTimeout(() => {
      resolve(mockResponses[task]);
    }, MOCK_LATENCY);
  });
};