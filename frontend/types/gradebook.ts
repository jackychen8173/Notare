export type GradeCellStatus = "NOT_SUBMITTED" | "MISSING" | "IN_PROGRESS" | "SUBMITTED" | "RETURNED";

export interface GradeCategorySummary {
  id: string;
  name: string;
  weightPercent: number;
}

export interface GradebookColumn {
  id: string;
  kind: "ASSIGNMENT" | "QUIZ";
  title: string;
  /** With the course zone's offset; null when there's no due time (quizzes can have none). */
  dueAt: string | null;
  opensAt: string | null;
  /** null when the work has no points: it shows a status only and never counts. */
  pointsPossible: number | null;
  categoryId: string | null;
  countsTowardGrade: boolean;
  /** Class average of returned scores, as a percent (tutor view only). */
  averagePercent: number | null;
}

export interface GradebookCell {
  status: GradeCellStatus;
  /** After any late penalty. From the latest returned version, which can be older than the latest one handed in. */
  points: number | null;
  pointsBeforePenalty: number | null;
  /** The old free-text grade, for work graded before points existed. */
  grade: string | null;
  late: boolean;
  daysLate: number;
  latePenaltyPercent: number;
  /** The latest submission, or the quiz attempt. */
  workId: string | null;
}

export interface Gradebook {
  timeZone: string;
  categories: GradeCategorySummary[];
  columns: GradebookColumn[];
  students: {
    studentId: string;
    name: string;
    email: string;
    /** Aligned with columns. */
    cells: GradebookCell[];
    /** Aligned with categories; null where nothing in that category is graded yet. */
    categoryPercents: (number | null)[];
    coursePercent: number | null;
  }[];
}

export interface StudentGrades {
  timeZone: string;
  categories: (GradeCategorySummary & { percent: number | null })[];
  items: { column: GradebookColumn; cell: GradebookCell }[];
  coursePercent: number | null;
}
