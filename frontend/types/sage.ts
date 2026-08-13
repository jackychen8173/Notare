export interface SageFeedback {
  correctness: string;
  style: string;
  suggestions: string;
  encouragement: string;
}

export interface ProgressSummary {
  studentId: string;
  summary: string;
}

export interface PendingReviews {
  count: number;
}
