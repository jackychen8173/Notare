// Keys match the backend's CourseColor enum; see the [data-course-color] rules in globals.css.
export type CourseColor = "TEAL" | "BLUE" | "INDIGO" | "VIOLET" | "ROSE" | "ORANGE" | "AMBER" | "SLATE";

export const COURSE_COLORS: CourseColor[] = ["TEAL", "BLUE", "INDIGO", "VIOLET", "ROSE", "ORANGE", "AMBER", "SLATE"];

// java.time.DayOfWeek names, as the backend serializes them.
export type Weekday = "MONDAY" | "TUESDAY" | "WEDNESDAY" | "THURSDAY" | "FRIDAY" | "SATURDAY" | "SUNDAY";

export const WEEKDAYS: Weekday[] = ["MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY"];

/**
 * Weekly meeting schedule. Times are "HH:mm" (or "HH:mm:ss") wall-clock times and dates are
 * date-only strings; both are shown as-is in the viewer's time zone.
 */
export interface CourseSchedule {
  days: Weekday[];
  startTime: string | null;
  endTime: string | null;
  termStart: string | null;
  termEnd: string | null;
}

export interface Course {
  id: string;
  tutorId: string;
  tutorName: string;
  name: string;
  subject: string;
  description: string | null;
  joinCode: string | null;
  color: CourseColor;
  schedule: CourseSchedule | null;
  archivedAt: string | null;
}
