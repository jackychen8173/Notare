// Keys match the backend's CourseColor enum; see the [data-course-color] rules in globals.css.
export type CourseColor = "TEAL" | "BLUE" | "INDIGO" | "VIOLET" | "ROSE" | "ORANGE" | "AMBER" | "SLATE";

export const COURSE_COLORS: CourseColor[] = ["TEAL", "BLUE", "INDIGO", "VIOLET", "ROSE", "ORANGE", "AMBER", "SLATE"];

export interface Course {
  id: string;
  tutorId: string;
  tutorName: string;
  name: string;
  subject: string;
  description: string | null;
  joinCode: string | null;
  color: CourseColor;
  archivedAt: string | null;
}
