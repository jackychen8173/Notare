export type ProgressItemStatus = "NOT_STARTED" | "IN_PROGRESS" | "SUBMITTED" | "RETURNED";

export interface StudentProgressUnit {
  /** null for the "No unit" bucket. */
  topicId: string | null;
  name: string;
  items: {
    id: string;
    kind: "ASSIGNMENT" | "QUIZ";
    title: string;
    dueDate: string | null;
    status: ProgressItemStatus;
  }[];
  done: number;
  total: number;
}

export interface StudentProgress {
  units: StudentProgressUnit[];
}

export interface CourseProgress {
  units: { topicId: string | null; name: string; assignmentCount: number; quizCount: number }[];
  /** done[i] is how many of units[i]'s items the student has submitted. */
  students: { studentId: string; name: string; done: number[] }[];
}
