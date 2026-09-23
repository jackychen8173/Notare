import type { QuestionType, QuizOption } from "@/types/quiz";

export type AttemptStatus = "IN_PROGRESS" | "SUBMITTED";

export interface QuizAnswer {
  questionId: string;
  questionPrompt: string;
  questionType: QuestionType;
  pointsPossible: number;
  options: QuizOption[];
  selectedOptionId: string | null;
  textResponse: string | null;
  // correct/pointsAwarded/tutorFeedback/sageSuggestion are all withheld until the attempt is
  // released - see the backend's QuizAnswerResponse.forStudent gating.
  correct: boolean | null;
  pointsAwarded: number | null;
  tutorFeedback: string | null;
  sageSuggestion: string | null;
}

export interface QuizAttempt {
  id: string;
  quizId: string;
  quizTitle: string;
  studentId: string;
  studentName: string;
  status: AttemptStatus;
  startedAt: string;
  deadlineAt: string | null;
  submittedAt: string | null;
  autoSubmitted: boolean;
  releasedAt: string | null;
  answers: QuizAnswer[];
  totalPointsAwarded: number | null;
  totalPointsPossible: number | null;
}
