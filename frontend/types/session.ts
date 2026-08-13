export type SessionStatus = "SCHEDULED" | "COMPLETED" | "CANCELLED";

export interface Session {
  id: string;
  tutorId: string;
  studentId: string;
  studentName: string;
  courseId: string | null;
  courseName: string | null;
  date: string;
  subject: string;
  duration: number;
  status: SessionStatus;
}

export interface SessionNote {
  id: string;
  sessionId: string;
  rawNotes: string | null;
  formattedNotes: string | null;
  createdAt: string;
}
