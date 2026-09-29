import type { CourseColor } from "@/types/course";

export interface HomeCourse {
  id: string;
  name: string;
  color: CourseColor;
}

export interface HomeThread {
  threadId: string;
  title: string;
  course: HomeCourse;
  visibility: "PUBLIC" | "PRIVATE";
  lastActivityAt: string;
}

export interface TutorHome {
  submissions: {
    submissionId: string;
    assignmentId: string;
    assignmentTitle: string;
    course: HomeCourse;
    studentName: string;
    attemptNumber: number;
    submittedAt: string;
    sageDraftReady: boolean;
  }[];
  quizAttempts: {
    attemptId: string;
    quizId: string;
    quizTitle: string;
    course: HomeCourse;
    studentName: string;
    submittedAt: string | null;
    needsGrading: boolean;
  }[];
  unreadDiscussions: HomeThread[];
}

export interface StudentHome {
  assignments: { assignmentId: string; title: string; course: HomeCourse; dueDate: string }[];
  quizzes: {
    quizId: string;
    title: string;
    course: HomeCourse;
    timeLimitMinutes: number | null;
    attemptId: string | null;
    deadlineAt: string | null;
  }[];
  returned: {
    kind: "ASSIGNMENT" | "QUIZ";
    id: string;
    title: string;
    course: HomeCourse;
    grade: string | null;
    releasedAt: string;
  }[];
  unreadDiscussions: HomeThread[];
}
