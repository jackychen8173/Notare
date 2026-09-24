export type QuestionType = "MULTIPLE_CHOICE" | "TRUE_FALSE" | "SHORT_ANSWER" | "ESSAY";

export interface QuizOption {
  id: string;
  text: string;
  // Always null on the take page / before an attempt is released - see the backend's
  // OptionResponse.forStudent gating.
  correct: boolean | null;
  position: number;
}

export interface QuizQuestion {
  id: string;
  type: QuestionType;
  prompt: string;
  pointsPossible: number;
  // Tutor's grading guide for SHORT_ANSWER/ESSAY - null in the student-facing view.
  referenceAnswer: string | null;
  position: number;
  options: QuizOption[];
}

export interface Quiz {
  id: string;
  courseId: string;
  courseName: string;
  topicId: string | null;
  topicName: string | null;
  gradeCategoryId: string | null;
  gradeCategoryName: string | null;
  title: string;
  description: string | null;
  timeLimitMinutes: number | null;
  publishedAt: string | null;
  totalPointsPossible: number;
  questions: QuizQuestion[];
}
