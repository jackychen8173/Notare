export interface Assignment {
  id: string;
  courseId: string;
  courseName: string;
  title: string;
  description: string | null;
  /** The due day in the course's time zone ("2026-11-20"), for views that show only a date. */
  dueDate: string;
  /**
   * Times carry the course time zone's offset ("2026-11-20T23:59:00-07:00"): an exact moment, and
   * the first 16 characters are the wall-clock time in the course's zone. See lib/dates.
   */
  dueAt: string;
  /** Students can't see it before this. */
  opensAt: string | null;
  /** No submissions after this; equal to dueAt means it locks at the due time. */
  closesAt: string | null;
  /** IANA zone the course's times are in, e.g. "America/Vancouver". */
  timeZone: string;
  /** null: not scored in points. */
  pointsPossible: number | null;
  /** Percent off per day late (any part of a day counts). */
  latePenaltyPercent: number;
  acceptingSubmissions: boolean;
  topicId: string | null;
  topicName: string | null;
  gradeCategoryId: string | null;
  gradeCategoryName: string | null;
  allowResubmission: boolean;
}
