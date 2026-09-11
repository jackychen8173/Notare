export type FeedbackStatus = "PENDING" | "APPROVED" | "REVISED";

export interface RubricScoreItem {
  criterionId: string;
  criterionName: string;
  pointsAwarded: number;
  pointsPossible: number;
}

export interface Submission {
  id: string;
  assignmentId: string;
  studentId: string;
  studentName: string;
  content: string;
  sageFeedback: string | null;
  tutorFeedback: string | null;
  feedbackStatus: FeedbackStatus;
  grade: string | null;
  submittedAt: string;
  releasedAt: string | null;
  rubricScores: RubricScoreItem[];
  rubricTotalAwarded: number | null;
  rubricTotalPossible: number | null;
}
