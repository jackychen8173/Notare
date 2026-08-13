export type FeedbackStatus = "PENDING" | "APPROVED" | "REVISED";

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
}
