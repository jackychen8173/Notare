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
  /** 1 for the first submission; each resubmission is the next number. */
  attemptNumber: number;
  sageFeedback: string | null;
  tutorFeedback: string | null;
  feedbackStatus: FeedbackStatus;
  grade: string | null;
  submittedAt: string;
  releasedAt: string | null;
  rubricScores: RubricScoreItem[];
  rubricTotalAwarded: number | null;
  rubricTotalPossible: number | null;
  /** Before any late penalty. Hidden from the student until released. */
  pointsEarned: number | null;
  pointsPossible: number | null;
  /** What counts after the late penalty. Hidden from the student until released. */
  pointsAfterPenalty: number | null;
  late: boolean;
  daysLate: number;
  latePenaltyPercent: number;
}

/** SUGGESTED: a Sage draft only the tutor sees. PUBLISHED: shown to the student once feedback is released. */
export type LineCommentStatus = "SUGGESTED" | "PUBLISHED";

export interface LineComment {
  id: string;
  submissionId: string;
  lineNumber: number;
  body: string;
  source: "TUTOR" | "SAGE";
  status: LineCommentStatus;
  createdAt: string;
}
